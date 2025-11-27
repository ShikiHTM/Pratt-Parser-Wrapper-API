package org.shiki.prattparserrestfulapi.parser;

import lombok.extern.slf4j.Slf4j;
import org.apfloat.Apfloat;

import java.util.Arrays;

@Slf4j
public class Parser {
    public static Apfloat eval(String input) {
        log.info("Starting evaluation for expression: {}", input);

        return expr(input).eval();
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
            lhs = exprBP(lexer, 0);

            Token close = lexer.next();
            if(close.type != Token.Type.OP || !close.value.equals(")")) {
                throw new RuntimeException("Expected ')', got: " + close);
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
            default -> throw new RuntimeException("Bad op: " + op);
        };
    }

    record BindingPower(int l_bp, int r_bp) {};
}
