package org.shiki.prattparserrestfulapi.parser;

import lombok.extern.slf4j.Slf4j;
import org.apfloat.Apfloat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Parser {
    // Use AtomicInteger so labels persist across requests and are thread-safe
    private static final AtomicInteger labelIndex = new AtomicInteger(0);

    private static String nextLabel() {
        int idx = labelIndex.getAndIncrement();
        if (idx < 26) return String.valueOf((char)('A' + idx));
        idx -= 26;
        if (idx < 26) return String.valueOf((char)('a' + idx));
        return "P" + idx; // fallback naming when beyond letters
    }

    public static EvalResult eval(String input) {
        log.info("Starting evaluation for expression: {}", input);
    // Do NOT reset the label sequence here — keep labels unique across requests

        // Quick check: detect a top-level assignment like `y = ...` or `f(x) = ...`
        // We scan characters and find an '=' that is not inside parentheses.
        int depth = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            else if (c == '=' && depth == 0) {
                String lhs = input.substring(0, i).trim();
                String rhs = input.substring(i + 1).trim();

                // function form: name(param)
                Pattern funcPat = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*)\\s*\\(\\s*([A-Za-z])\\s*\\)$");
                Matcher m = funcPat.matcher(lhs);
                if (m.find()) {
                    String name = m.group(1);
                    String param = m.group(2);
                    return EvalResult.ofFunction(name, param, rhs);
                }

                // variable assignment like `y = expr` -> treat as function of x
                Pattern varPat = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*)$");
                Matcher m2 = varPat.matcher(lhs);
                if (m2.find()) {
                    String name = m2.group(1);
                    return EvalResult.ofFunction(name, "x", rhs);
                }

                // otherwise break and fall back to normal parsing
                break;
            }
        }

        LipsNotation ln = expr(input);

        // If the expression is a tuple like (x,y) we return an auto-labelled point
        if (ln instanceof LipsNotation.Cons) {
            LipsNotation.Cons c = (LipsNotation.Cons) ln;

            // tuple case produced by parser (head == "tuple")
            if ("tuple".equals(c.head()) && c.rest().size() == 2) {
                Apfloat x = c.rest().get(0).eval();
                Apfloat y = c.rest().get(1).eval();
                String label = nextLabel();
                return EvalResult.ofPoint(label, x, y);
            }

            // Explicit labeled point: A(x,y)
            String head = c.head();
            if (head != null && head.matches("[A-Za-z]") && c.rest().size() == 2) {
                Apfloat x = c.rest().get(0).eval();
                Apfloat y = c.rest().get(1).eval();
                return EvalResult.ofPoint(head, x, y);
            }
        }

        // Fallback: numeric evaluation
        Apfloat numeric = ln.eval();
        return EvalResult.ofNumber(numeric);
    }

    private static LipsNotation expr(String input) {
        Lexer lexer = new Lexer(input);
        return exprBP(lexer, 0);
    }

    private static LipsNotation exprBP(Lexer lexer, int min_bp) {
        log.debug("Entering exprBP with min_bp: {}", min_bp);

        Token token = lexer.next();

        LipsNotation lhs;

        if(token.type == Token.Type.ATOM) {
            log.debug("Consumed ATOM: {}", token.value);
            lhs = new LipsNotation.Atom(token.value);
        }
        else if(token.type == Token.Type.OP && token.value.equals("(")) {
            // Decide whether this parentheses represent a grouping (single expression)
            // or a tuple (comma-separated elements). Use lexer helper to peek ahead.
            boolean looksLikeTuple = lexer.hasTopLevelCommaUntilClosingParen();

            if (!looksLikeTuple) {
                // grouping: parse single expression and expect a closing ')'
                lhs = exprBP(lexer, 0);
                Token close = lexer.next();
                if(close.type != Token.Type.OP || !close.value.equals(")")) {
                    throw new RuntimeException("Expected ')', got: " + close);
                }
            } else {
                // tuple: parse comma-separated elements
                java.util.List<LipsNotation> elems = new java.util.ArrayList<>();
                // parse first element
                elems.add(exprBP(lexer, 0));
                // parse additional comma-separated elements
                while(lexer.peek().type == Token.Type.OP && lexer.peek().value.equals(",")) {
                    lexer.next(); // consume ','
                    elems.add(exprBP(lexer, 0));
                }

                Token close = lexer.next();
                if(close.type != Token.Type.OP || !close.value.equals(")")) {
                    throw new RuntimeException("Expected ')', got: " + close);
                }

                if (elems.size() == 1) {
                    lhs = elems.get(0);
                } else {
                    lhs = new LipsNotation.Cons("tuple", elems);
                }
            }
        }
        else if(token.type == Token.Type.OP) {
            log.debug("Consumed PREFIX OP: {}", token.value);
            BindingPower bp = prefix_binding_power(token.value);
            LipsNotation rhs = exprBP(lexer, bp.r_bp);
            lhs = new LipsNotation.Cons(token.value, Arrays.asList(rhs));
        }
        else {
            throw new RuntimeException("Bad token: " + token);
        }       

        while(true) {
            Token next = lexer.peek();

            // If lhs is an Atom (identifier) we support function-call style parsing:
            // - func(arg1, arg2, ...)
            // - func_base(arg) represented as func _ base (e.g. log_2(8)) -> treated as log(base, arg)
            if(lhs instanceof LipsNotation.Atom) {
                String funcName = lhs.toString();

                // log_2(8) style: atom '_' base '(' args ')'
                if(next.type == Token.Type.OP && next.value.equals("_")) {
                        // consume '_'
                        lexer.next();
                        // parse base token only (do not allow base to consume following parentheses as a function call)
                        Token baseTok = lexer.next();
                        LipsNotation base;
                        if (baseTok.type == Token.Type.ATOM) {
                            base = new LipsNotation.Atom(baseTok.value);
                        } else if (baseTok.type == Token.Type.OP && baseTok.value.equals("(")) {
                            // parse grouped base expression
                            base = exprBP(lexer, 0);
                            Token closeBase = lexer.next();
                            if (closeBase.type != Token.Type.OP || !closeBase.value.equals(")")) {
                                throw new RuntimeException("Expected ')', got: " + closeBase);
                            }
                        } else {
                            throw new RuntimeException("Bad base for subscript: " + baseTok);
                        }

                        // after base, expect parentheses for function arguments (optional)
                        Token afterBase = lexer.peek();
                        if(afterBase.type == Token.Type.OP && afterBase.value.equals("(")) {
                            // consume '('
                            lexer.next();
                            List<LipsNotation> args = new ArrayList<>();
                            // parse comma-separated args
                            if(!(lexer.peek().type == Token.Type.OP && lexer.peek().value.equals(")"))) {
                                args.add(exprBP(lexer, 0));
                                while(lexer.peek().type == Token.Type.OP && lexer.peek().value.equals(",")) {
                                    lexer.next();
                                    args.add(exprBP(lexer, 0));
                                }
                            }

                            Token close = lexer.next();
                            if(close.type != Token.Type.OP || !close.value.equals(")")) {
                                throw new RuntimeException("Expected ')', got: " + close);
                            }

                            // Prepend base as first argument: log(base, value) semantics
                            List<LipsNotation> rest = new java.util.ArrayList<>();
                            rest.add(base);
                            rest.addAll(args);
                            lhs = new LipsNotation.Cons(funcName, rest);
                            continue;
                        } else {
                            // No parentheses: treat as a unary application with base as single argument
                            List<LipsNotation> rest = java.util.Arrays.asList(base);
                            lhs = new LipsNotation.Cons(funcName, rest);
                            continue;
                        }
                }

                // Standard function call: func(arg1, arg2, ...)
                if(next.type == Token.Type.OP && next.value.equals("(")) {
                    // consume '('
                    lexer.next();
                    java.util.List<LipsNotation> args = new java.util.ArrayList<>();
                    if(!(lexer.peek().type == Token.Type.OP && lexer.peek().value.equals(")"))) {
                        args.add(exprBP(lexer, 0));
                        while(lexer.peek().type == Token.Type.OP && lexer.peek().value.equals(",")) {
                            lexer.next();
                            args.add(exprBP(lexer, 0));
                        }
                    }

                    Token close = lexer.next();
                    if(close.type != Token.Type.OP || !close.value.equals(")")) {
                        throw new RuntimeException("Expected ')', got: " + close);
                    }

                    lhs = new LipsNotation.Cons(funcName, args);
                    continue;
                }
            }

            BindingPower postfix_bp = postfix_binding_power(next.value);

            if(next.type == Token.Type.EoF) {
                log.debug("Peeking at token: {} | Current min_bp: {}", next, min_bp);
                break;
            }

            else if(postfix_bp != null && postfix_bp.l_bp >= min_bp) {
                lexer.next();
                lhs = new LipsNotation.Cons(next.value, Arrays.asList(lhs));
                continue;
            }

            else if(next.type == Token.Type.OP) {
                String op = next.value;
                BindingPower infix_bp = infix_binding_power(op);

                if(infix_bp == null) break;

                if(infix_bp.l_bp < min_bp) {
                    log.debug("Breaking loop: Infix LBP ({}) < min_bp ({})", infix_bp.l_bp, min_bp);;
                    break;
                }

                log.debug("Processing INFIX OP: {} | LBP: {} | New RBP for recursion: {}", op, infix_bp.l_bp, infix_bp.r_bp);

                lexer.next();
                LipsNotation rhs = exprBP(lexer, infix_bp.r_bp);
                lhs = new LipsNotation.Cons(next.value, Arrays.asList(lhs, rhs));
            }
        }

        log.debug("Exiting exprBP, returning LHS: {}", lhs);
        return lhs;
    }

    private static BindingPower postfix_binding_power(String op) {
        return switch (op) {
            case "!"-> new BindingPower(7, 0);
            default -> null;
        };
    }

    private static BindingPower infix_binding_power(String op) {
        return switch (op) {
            case "+", "-" -> new BindingPower(1, 3);
            case "*", "/" -> new BindingPower(3, 4);
            case "%" -> new BindingPower(4, 5);
            case "^" -> new BindingPower(5, 6);
            default -> null;
        };
    }

    private static BindingPower prefix_binding_power(String op) {
        return switch (op) {
            case "+", "-" -> new BindingPower(0, 5);
            case "_" -> new BindingPower(0, 7);
            default -> throw new RuntimeException("Bad op: " + op);
        };
    }

    record BindingPower(int l_bp, int r_bp) {};
}
