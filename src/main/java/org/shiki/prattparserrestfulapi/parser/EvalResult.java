package org.shiki.prattparserrestfulapi.parser;

import org.apfloat.Apfloat;

/**
 * Wrapper for evaluation results. Either a numeric result (kind="number")
 * or a point (kind="point"). Fields not used for a kind may be null.
 */
public record EvalResult(String kind, Apfloat number, String label, Apfloat x, Apfloat y, String funcParam, String funcExpr) {

    public static EvalResult ofNumber(Apfloat n) {
        return new EvalResult("number", n, null, null, null, null, null);
    }

    public static EvalResult ofPoint(String label, Apfloat x, Apfloat y) {
        return new EvalResult("point", null, label, x, y, null, null);
    }

    public static EvalResult ofFunction(String funcName, String funcParam, String funcExpr) {
        // funcName: e.g., 'y' or 'f'; funcParam e.g., 'x'; funcExpr is RHS string
        return new EvalResult("function", null, funcName, null, null, funcParam, funcExpr);
    }
}
