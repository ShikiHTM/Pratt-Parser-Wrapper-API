package org.shiki.prattparserrestfulapi.parser;

public class Token {
    enum Type {ATOM, OP, EoF}

    final Type type;
    final String value;

    public Token(Type type, String value) {
        this.type = type;
        this.value = value;
    }

    static Token eof() {
        return new Token(Type.EoF, "\0");
    }

    static Token atom(String c) {
        return new Token(Type.ATOM, c);
    }

    static Token op(String c) {
        return new Token(Type.OP, c);
    }

    @Override
    public String toString() {
        return "Token(" + type + ", '" + value + "')";
    }
}