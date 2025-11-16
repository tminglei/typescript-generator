package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;

public class TsQuoteExpression extends TsExpression {
    private final TsExpression expr;

    public TsQuoteExpression(TsExpression expr) {
        this.expr = expr;
    }

    public TsExpression getExpr() {
        return expr;
    }

    @Override
    public String format(Settings settings) {
        return "(" + expr.format(settings) + ")";
    }
}
