package org.shiki.prattparserrestfulapi.service;

import org.springframework.stereotype.Service;
import org.shiki.prattparserrestfulapi.parser.Parser;

@Service
public class ExpressionService {

    public double evaluate(String expression) {
        return Parser.eval(expression);
    }

}
