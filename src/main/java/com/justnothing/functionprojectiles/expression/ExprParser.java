package com.justnothing.functionprojectiles.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public class ExprParser {

    private static final int MAX_LENGTH = 256;
    private static final int MAX_DEPTH = 20;

    private static final Map<String, Double> CONSTANTS = Map.of(
            "pi", Math.PI,
            "e", Math.E
    );

    private static final Map<String, DoubleUnaryOperator> FUNCTIONS = new HashMap<>();

    static {
        FUNCTIONS.put("sin", Math::sin);
        FUNCTIONS.put("cos", Math::cos);
        FUNCTIONS.put("tan", Math::tan);
        FUNCTIONS.put("asin", Math::asin);
        FUNCTIONS.put("acos", Math::acos);
        FUNCTIONS.put("atan", Math::atan);
        FUNCTIONS.put("sqrt", Math::sqrt);
        FUNCTIONS.put("abs", Math::abs);
        FUNCTIONS.put("log", Math::log);
        FUNCTIONS.put("log10", Math::log10);
        FUNCTIONS.put("exp", Math::exp);
        FUNCTIONS.put("ceil", Math::ceil);
        FUNCTIONS.put("floor", Math::floor);
        FUNCTIONS.put("round", Math::round);
    }

    private enum TokenType {
        NUMBER, IDENTIFIER, OPERATOR, LPAREN, RPAREN, COMMA, EOF
    }

    private static final class Token {
        final TokenType type;
        final String text;
        final int position;

        Token(TokenType type, String text, int position) {
            this.type = type;
            this.text = text;
            this.position = position;
        }
    }

    // --- Tokenizer ---

    private static List<Token> tokenize(String input, String variable) throws ExprParseException {
        if (input == null || input.isBlank()) {
            throw new ExprParseException("Empty expression");
        }
        if (input.length() > MAX_LENGTH) {
            throw new ExprParseException("Expression too long (max " + MAX_LENGTH + " characters)");
        }

        var tokens = new ArrayList<Token>();
        int pos = 0;
        int len = input.length();

        while (pos < len) {
            char ch = input.charAt(pos);

            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            }

            int startPos = pos;

            if (ch == '(') {
                tokens.add(new Token(TokenType.LPAREN, "(", startPos));
                pos++;
            } else if (ch == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")", startPos));
                pos++;
            } else if (ch == ',') {
                tokens.add(new Token(TokenType.COMMA, ",", startPos));
                pos++;
            } else if (isOperatorChar(ch)) {
                tokens.add(new Token(TokenType.OPERATOR, String.valueOf(ch), startPos));
                pos++;
            } else if (isNumberStart(ch)) {
                Token num = readNumber(input, startPos);
                tokens.add(num);
                pos += num.text.length();
            } else if (Character.isUnicodeIdentifierStart(ch)) {
                Token ident = readIdentifier(input, startPos);
                tokens.add(ident);
                pos += ident.text.length();
            } else {
                throw new ExprParseException("Unexpected character '" + ch + "'", startPos);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", len));
        return insertImplicitMultiplication(tokens);
    }

    private static boolean isOperatorChar(char ch) {
        return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^';
    }

    private static boolean isNumberStart(char ch) {
        return Character.isDigit(ch) || ch == '.';
    }

    private static Token readNumber(String input, int startPos) throws ExprParseException {
        boolean hasDot = false;
        int i = startPos;

        if (input.charAt(i) == '.') {
            hasDot = true;
            i++;
        }

        while (i < input.length() && Character.isDigit(input.charAt(i))) {
            i++;
        }

        if (!hasDot && i < input.length() && input.charAt(i) == '.') {
            hasDot = true;
            i++;
            while (i < input.length() && Character.isDigit(input.charAt(i))) {
                i++;
            }
        }

        if (i == startPos || (i == startPos + 1 && input.charAt(startPos) == '.')) {
            throw new ExprParseException("Invalid number", startPos);
        }

        return new Token(TokenType.NUMBER, input.substring(startPos, i), startPos);
    }

    private static Token readIdentifier(String input, int startPos) {
        int i = startPos;
        while (i < input.length() && Character.isUnicodeIdentifierPart(input.charAt(i))) {
            i++;
        }
        return new Token(TokenType.IDENTIFIER, input.substring(startPos, i), startPos);
    }

    private static List<Token> insertImplicitMultiplication(List<Token> tokens) {
        if (tokens.size() <= 1) {
            return tokens;
        }

        var result = new ArrayList<Token>(tokens.size() + tokens.size() / 2);
        Token prev = tokens.get(0);
        result.add(prev);

        for (int i = 1; i < tokens.size(); i++) {
            Token curr = tokens.get(i);

            if (needsImplicitMul(prev, curr)) {
                result.add(new Token(TokenType.OPERATOR, "*", prev.position + prev.text.length()));
            }

            result.add(curr);
            prev = curr;
        }

        return result;
    }

    private static boolean needsImplicitMul(Token prev, Token curr) {
        if (prev.type == TokenType.NUMBER && curr.type == TokenType.IDENTIFIER) {
            return true;
        }
        if (prev.type == TokenType.NUMBER && curr.type == TokenType.LPAREN) {
            return true;
        }
        if (prev.type == TokenType.RPAREN && curr.type == TokenType.NUMBER) {
            return true;
        }
        if (prev.type == TokenType.RPAREN && curr.type == TokenType.LPAREN) {
            return true;
        }
        if (prev.type == TokenType.RPAREN && curr.type == TokenType.IDENTIFIER) {
            return true;
        }
        if (prev.type == TokenType.IDENTIFIER && curr.type == TokenType.NUMBER) {
            return true;
        }
        if (prev.type == TokenType.IDENTIFIER && curr.type == TokenType.LPAREN) {
            return !FUNCTIONS.containsKey(prev.text);
        }
        return false;
    }

    // --- Parser ---

    private final List<Token> tokens;
    private final String variable;
    private int tokenIndex;

    private ExprParser(List<Token> tokens, String variable) {
        this.tokens = tokens;
        this.variable = variable;
        this.tokenIndex = 0;
    }

    private Token current() {
        return tokens.get(tokenIndex);
    }

    private void advance() {
        if (tokenIndex < tokens.size() - 1) {
            tokenIndex++;
        }
    }

    private Expression parseExpression(int depth) throws ExprParseException {
        return parseAddSub(depth);
    }

    private Expression parseAddSub(int depth) throws ExprParseException {
        checkDepth(depth);
        Expression left = parseMulDiv(depth + 1);

        while (current().type == TokenType.OPERATOR
                && (current().text.equals("+") || current().text.equals("-"))) {
            String op = current().text;
            advance();
            Expression right = parseMulDiv(depth + 1);
            if (op.equals("+")) {
                Expression l = left, r = right;
                left = x -> l.evaluate(x) + r.evaluate(x);
            } else {
                Expression l = left, r = right;
                left = x -> l.evaluate(x) - r.evaluate(x);
            }
        }
        return left;
    }

    private Expression parseMulDiv(int depth) throws ExprParseException {
        checkDepth(depth);
        Expression left = parseUnary(depth + 1);

        while (current().type == TokenType.OPERATOR
                && (current().text.equals("*") || current().text.equals("/"))) {
            String op = current().text;
            advance();
            Expression right = parseUnary(depth + 1);
            if (op.equals("*")) {
                Expression l = left, r = right;
                left = x -> l.evaluate(x) * r.evaluate(x);
            } else {
                Expression l = left, r = right;
                left = x -> {
                    double denominator = r.evaluate(x);
                    if (denominator == 0) {
                        return Double.NaN;
                    }
                    return l.evaluate(x) / denominator;
                };
            }
        }
        return left;
    }

    private Expression parseUnary(int depth) throws ExprParseException {
        checkDepth(depth);
        if (current().type == TokenType.OPERATOR
                && (current().text.equals("+") || current().text.equals("-"))) {
            String op = current().text;
            advance();
            Expression operand = parseUnary(depth + 1);
            if (op.equals("-")) {
                return x -> -operand.evaluate(x);
            }
            return operand;
        }
        return parsePower(depth + 1);
    }

    private Expression parsePower(int depth) throws ExprParseException {
        checkDepth(depth);
        Expression base = parsePrimary(depth + 1);

        if (current().type == TokenType.OPERATOR && current().text.equals("^")) {
            advance();
            Expression exponent = parseUnary(depth + 1);
            Expression b = base, e = exponent;
            return x -> Math.pow(b.evaluate(x), e.evaluate(x));
        }
        return base;
    }

    private Expression parsePrimary(int depth) throws ExprParseException {
        checkDepth(depth);
        Token cur = current();

        if (cur.type == TokenType.NUMBER) {
            double value = Double.parseDouble(cur.text);
            advance();
            return x -> value;
        }

        if (cur.type == TokenType.IDENTIFIER) {
            String name = cur.text;
            int namePos = cur.position;
            advance();

            if (current().type == TokenType.LPAREN && FUNCTIONS.containsKey(name)) {
                advance();
                Expression arg = parseExpression(depth + 1);
                expect(TokenType.RPAREN);
                DoubleUnaryOperator fn = FUNCTIONS.get(name);
                return x -> fn.applyAsDouble(arg.evaluate(x));
            }

            if (CONSTANTS.containsKey(name)) {
                double value = CONSTANTS.get(name);
                return x -> value;
            }

            if (name.equals(variable)) {
                return x -> x;
            }

            if (current().type == TokenType.LPAREN) {
                throw new ExprParseException("Unknown function '" + name + "'", namePos);
            }

            throw new ExprParseException("Unknown identifier '" + name + "'", namePos);
        }

        if (cur.type == TokenType.LPAREN) {
            advance();
            Expression expr = parseExpression(depth + 1);
            expect(TokenType.RPAREN);
            return expr;
        }

        throw new ExprParseException("Unexpected token '" + cur.text + "'", cur.position);
    }

    private void expect(TokenType expected) throws ExprParseException {
        if (current().type != expected) {
            String expectedStr = switch (expected) {
                case RPAREN -> "')'";
                case EOF -> "end of input";
                default -> expected.name();
            };
            throw new ExprParseException("Expected " + expectedStr, current().position);
        }
        advance();
    }

    private void checkDepth(int depth) throws ExprParseException {
        if (depth > MAX_DEPTH) {
            throw new ExprParseException("Expression too deeply nested (max " + MAX_DEPTH + ")");
        }
    }

    // --- Public API ---

    public static Expression parse(String input) throws ExprParseException {
        var tokenList = tokenize(input, "x");
        var parser = new ExprParser(tokenList, "x");
        Expression expr = parser.parseExpression(0);
        if (parser.current().type != TokenType.EOF) {
            throw new ExprParseException("Unexpected token '" + parser.current().text + "'", parser.current().position);
        }
        return expr;
    }

    public static Expression parseForT(String input) throws ExprParseException {
        var tokenList = tokenize(input, "t");
        var parser = new ExprParser(tokenList, "t");
        Expression expr = parser.parseExpression(0);
        if (parser.current().type != TokenType.EOF) {
            throw new ExprParseException("Unexpected token '" + parser.current().text + "'", parser.current().position);
        }
        return expr;
    }
}
