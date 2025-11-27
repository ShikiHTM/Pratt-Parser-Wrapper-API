package org.shiki.prattparserrestfulapi.parser;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Vector;

@Slf4j
public class Lexer {
    protected List<Token> tokens;
    private int pos = 0;

    public Lexer(String input) {
        tokens = new Vector<>();
        log.info("Starting tokenization for input: \"{}\"", input);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) continue;

            // Group consecutive letters/digits into a single ATOM token (supports multi-digit numbers
            // and multi-letter identifiers)
            if (Character.isLetterOrDigit(c)) {
                StringBuilder sb = new StringBuilder();
                sb.append(c);
                while (i + 1 < input.length() && Character.isLetterOrDigit(input.charAt(i + 1))) {
                    i++;
                    sb.append(input.charAt(i));
                }
                tokens.add(Token.atom(sb.toString()));
            } else {
                // Single-character operators/parens/etc.
                tokens.add(Token.op(Character.toString(c)));
            }
        }
        log.info("Finished tokenization. Total tokens: {}", tokens.size());
    }

    public Lexer(Vector<Token> tokens) {
        this.tokens = tokens;
    }

    public Token next() {
        if(pos >= tokens.size()) {
            log.debug("Consumed EoF");
            return Token.eof();
        }
        Token token = tokens.get(pos++);
        log.debug("NEXT: Consumed token at pos {}: {}", pos - 1, token);
        return token;
    }

    public Token peek() {
        if(pos >= tokens.size()) return Token.eof();
        Token token = tokens.get(pos);
        log.trace("PEEK: Token at pos {}: {}", pos, token);
        return token;
    }
}
