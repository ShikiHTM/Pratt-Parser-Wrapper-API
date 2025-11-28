package org.shiki.prattparserrestfulapi.parser;

import java.util.List;
import org.apfloat.*;

public abstract class LipsNotation {
    public abstract String toString();
    public abstract Apfloat eval();

    public static class Atom extends LipsNotation {
        private final String value;

        public Atom(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public Apfloat eval() {
            return new Apfloat(value, 1000);
        }
    }

    public static class Cons extends LipsNotation {
        private final String head;
        private final List<LipsNotation> rest;

        public Cons(String head, List<LipsNotation> rest) {
            this.head = head;
            this.rest = rest;
        }

        // Accessors used by Parser for higher-level pattern detection (points etc.)
        public String head() { return head; }
        public List<LipsNotation> rest() { return rest; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(").append(head);
            for(LipsNotation ln: rest) {
                sb.append(" ").append(ln.toString());
            }

            sb.append(")");
            return sb.toString();
        }

        public Apfloat eval() {
            return switch (head) {
                case "+" -> rest.get(0).eval().add(rest.get(1).eval());
                case "-" -> {
                    if (rest.size() == 1)
                        yield rest.get(0).eval().negate();
                    yield rest.get(0).eval().subtract(rest.get(1).eval());
                }
                case "*" -> rest.get(0).eval().multiply(rest.get(1).eval());
                case "/" -> {
                    if (rest.size() == 1 || rest.get(1).toString().equals("0")) {
                        throw new RuntimeException("Cannot divide by 0.");
                    }
                    yield rest.get(0).eval().divide(rest.get(1).eval());
                }
                case "%" -> rest.get(0).eval().mod(rest.get(1).eval());
                case "!" -> factorial(rest.get(0).eval());
                case "^" -> ApfloatMath.pow(rest.get(0).eval(), rest.get(1).eval());
                case "sin" -> ApfloatMath.sin(rest.get(0).eval());
                case "cos" -> ApfloatMath.cos(rest.get(0).eval());
                case "tan" -> ApfloatMath.tan(rest.get(0).eval());
                case "log" -> {
                    // support log(x) -> natural log, and log(base, x) -> log base 'base' of x
                    if (rest.size() == 1) {
                        yield ApfloatMath.log(rest.get(0).eval());
                    } else if (rest.size() == 2) {
                        Apfloat base = rest.get(0).eval();
                        Apfloat val = rest.get(1).eval();
                        yield ApfloatMath.log(val).divide(ApfloatMath.log(base));
                    } else {
                        throw new RuntimeException("log expects 1 or 2 arguments");
                    }
                }
                case "ln" -> ApfloatMath.log(rest.get(0).eval(), ApfloatMath.e(1000));
                default -> throw new RuntimeException("Unknown operator: " + head);
            };
        }
    }

    private static Apfloat factorial(Apfloat x) {
        if(x.compareTo(Apfloat.ZERO) < 0 || !x.isInteger()) {
            throw new RuntimeException("Invalid factorial: " + x);
        }

        if (x.equals(Apfloat.ZERO) || x.equals(Apfloat.ONE)) {
            return Apfloat.ONE;
        }

        Apfloat result = Apfloat.ONE;
        Apfloat i = Apfloat.ONE;

        while(i.compareTo(x) <= 0) {
            result = result.multiply(i);
            i = i.add(Apfloat.ONE);
        }

        return result;
    }
}