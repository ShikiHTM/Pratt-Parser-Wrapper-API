package org.shiki.prattparserrestfulapi.parser;

import java.util.List;
import java.lang.Math.*;

public abstract class LipsNotation {
    public abstract String toString();
    public abstract long eval();

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
        public long eval() {
            return Integer.parseInt(value);
        }
    }

    public static class Cons extends LipsNotation {
        private final String head;
        private final List<LipsNotation> rest;

        public Cons(String head, List<LipsNotation> rest) {
            this.head = head;
            this.rest = rest;
        }

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

        public long eval() {
            return switch (head) {
                case "+" -> rest.get(0).eval() + rest.get(1).eval();
                case "-" -> {
                    if (rest.size() == 1)
                        yield -rest.get(0).eval();
                    yield rest.get(0).eval() - rest.get(1).eval();
                }
                case "*" -> rest.get(0).eval() * rest.get(1).eval();
                case "/" -> {
                    if (rest.size() == 1 || rest.get(1).equals("0")) {
                        throw new RuntimeException("Cannot divide by 0.");
                    }
                    yield rest.get(0).eval() / rest.get(1).eval();
                }
                case "%" -> rest.get(0).eval() % rest.get(1).eval();
                case "!" -> (long) factorial(rest.get(0).eval());
                case "^" -> (long) Math.pow(rest.get(0).eval(), rest.get(1).eval());
                default -> throw new RuntimeException("Unknown operator: " + head);
            };
        }
    }

    private static long factorial(long n) {
        long result = 1;
        while(n > 1) {
            result *= n--;
        }
        return result;
    }
}