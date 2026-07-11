package com.justnothing.functionprojectiles.expression;

public class ExprParseException extends Exception {
    private final int position;

    public ExprParseException(String message, int position) {
        super(message + " at position " + position);
        this.position = position;
    }

    public ExprParseException(String message) {
        super(message);
        this.position = -1;
    }

    public int getPosition() {
        return position;
    }
}
