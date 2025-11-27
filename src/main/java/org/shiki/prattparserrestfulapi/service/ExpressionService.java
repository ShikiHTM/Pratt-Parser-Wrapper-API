package org.shiki.prattparserrestfulapi.service;

import org.apfloat.Apfloat;
import org.springframework.stereotype.Service;
import org.shiki.prattparserrestfulapi.parser.Parser;

@Service
public class ExpressionService {

    public Apfloat evaluate(String expression) {
        return Parser.eval(expression);
    }

}
