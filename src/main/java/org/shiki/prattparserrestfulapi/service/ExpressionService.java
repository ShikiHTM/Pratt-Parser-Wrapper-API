package org.shiki.prattparserrestfulapi.service;

import org.shiki.prattparserrestfulapi.parser.EvalResult;
import org.springframework.stereotype.Service;
import org.shiki.prattparserrestfulapi.parser.Parser;

@Service
public class ExpressionService {

    public EvalResult evaluate(String expression) {
        return Parser.eval(expression);
    }

}
