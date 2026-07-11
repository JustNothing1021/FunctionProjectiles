package com.justnothing.functionprojectiles.expression;

public record ParametricExpression(Expression x, Expression y, Expression z) {
    public record Point(double x, double y, double z) {}

    public Point evaluate(double t) {
        return new Point(x.evaluate(t), y.evaluate(t), z.evaluate(t));
    }
}
