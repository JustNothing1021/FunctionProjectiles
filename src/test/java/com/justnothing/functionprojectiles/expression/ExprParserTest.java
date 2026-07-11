package com.justnothing.functionprojectiles.expression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExprParserTest {

    private static final double DELTA = 1e-9;

    private double eval(String input) throws ExprParseException {
        return ExprParser.parse(input).evaluate(0);
    }

    // ---------------------------------------------------------------
    //  1. Basic arithmetic
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Basic arithmetic")
    class BasicArithmetic {
        @Test
        void addition() throws ExprParseException {
            assertEquals(5, eval("2+3"), DELTA);
        }

        @Test
        void subtraction() throws ExprParseException {
            assertEquals(6, eval("10-4"), DELTA);
        }

        @Test
        void multiplication() throws ExprParseException {
            assertEquals(12, eval("3*4"), DELTA);
        }

        @Test
        void division() throws ExprParseException {
            assertEquals(5, eval("15/3"), DELTA);
        }

        @Test
        void power() throws ExprParseException {
            assertEquals(8, eval("2^3"), DELTA);
        }

        @Test
        void powerLarge() throws ExprParseException {
            assertEquals(1024, eval("2^10"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  2. Operator precedence
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Operator precedence")
    class OperatorPrecedence {
        @Test
        void addBeforeMul() throws ExprParseException {
            assertEquals(14, eval("2+3*4"), DELTA);
        }

        @Test
        void mulBeforeAdd() throws ExprParseException {
            assertEquals(10, eval("2*3+4"), DELTA);
        }

        @Test
        void addBeforePow() throws ExprParseException {
            assertEquals(11, eval("2+3^2"), DELTA);
        }

        @Test
        void mulBeforePow() throws ExprParseException {
            assertEquals(18, eval("2*3^2"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  3. Right-associative power
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Right-associative power")
    class RightAssociativePower {
        @Test
        void twoPowerThreePowerTwo() throws ExprParseException {
            // 2^(3^2) = 2^9 = 512
            assertEquals(512, eval("2^3^2"), DELTA);
        }

        @Test
        void threePowerTwoPowerThree() throws ExprParseException {
            // 3^(2^3) = 3^8 = 6561
            assertEquals(6561, eval("3^2^3"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  4. Unary operators
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Unary operators")
    class UnaryOperators {
        @Test
        void unaryMinus() throws ExprParseException {
            assertEquals(-5, eval("-5"), DELTA);
        }

        @Test
        void unaryPlus() throws ExprParseException {
            assertEquals(5, eval("+5"), DELTA);
        }

        @Test
        void doubleNegation() throws ExprParseException {
            assertEquals(5, eval("--5"), DELTA);
        }

        @Test
        void negatedParenthesizedNegative() throws ExprParseException {
            assertEquals(3, eval("-(-3)"), DELTA);
        }

        @Test
        void unaryMinusLowerPrecedenceThanPow() throws ExprParseException {
            // -2^2 = -(2^2) = -4
            assertEquals(-4, eval("-2^2"), DELTA);
        }

        @Test
        void powerOfNegativeExponent() throws ExprParseException {
            // 2^-2 = 2^(-2) = 0.25
            assertEquals(0.25, eval("2^-2"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  5. Parentheses
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Parentheses")
    class Parentheses {
        @Test
        void parenthesizedAddThenMul() throws ExprParseException {
            assertEquals(20, eval("(2+3)*4"), DELTA);
        }

        @Test
        void mulParenthesizedAdd() throws ExprParseException {
            assertEquals(14, eval("2*(3+4)"), DELTA);
        }

        @Test
        void doubleParentheses() throws ExprParseException {
            assertEquals(5, eval("((2+3))"), DELTA);
        }

        @Test
        void nestedParentheses() throws ExprParseException {
            assertEquals(19, eval("(1+(2+(3+4))*2)"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  6. Decimal numbers
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Decimal numbers")
    class DecimalNumbers {
        @Test
        void piApprox() throws ExprParseException {
            assertEquals(3.14, eval("3.14"), DELTA);
        }

        @Test
        void zeroPointFive() throws ExprParseException {
            assertEquals(0.5, eval("0.5"), DELTA);
        }

        @Test
        void dotFive() throws ExprParseException {
            assertEquals(0.5, eval(".5"), DELTA);
        }

        @Test
        void decimalMul() throws ExprParseException {
            assertEquals(5.0, eval("2.5*2"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  7. Functions
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Functions")
    class Functions {
        @Test
        void sinZero() throws ExprParseException {
            assertEquals(0, eval("sin(0)"), DELTA);
        }

        @Test
        void cosZero() throws ExprParseException {
            assertEquals(1, eval("cos(0)"), DELTA);
        }

        @Test
        void tanZero() throws ExprParseException {
            assertEquals(0, eval("tan(0)"), DELTA);
        }

        @Test
        void sqrtFour() throws ExprParseException {
            assertEquals(2, eval("sqrt(4)"), DELTA);
        }

        @Test
        void absNegativeFive() throws ExprParseException {
            assertEquals(5, eval("abs(-5)"), DELTA);
        }

        @Test
        void logOne() throws ExprParseException {
            assertEquals(0, eval("log(1)"), DELTA);
        }

        @Test
        void log10Hundred() throws ExprParseException {
            assertEquals(2, eval("log10(100)"), DELTA);
        }

        @Test
        void expZero() throws ExprParseException {
            assertEquals(1, eval("exp(0)"), DELTA);
        }

        @Test
        void ceil() throws ExprParseException {
            assertEquals(2, eval("ceil(1.5)"), DELTA);
        }

        @Test
        void floor() throws ExprParseException {
            assertEquals(1, eval("floor(1.5)"), DELTA);
        }

        @Test
        void round() throws ExprParseException {
            assertEquals(2, eval("round(1.5)"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  8. Constants
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Constants")
    class Constants {
        @Test
        void pi() throws ExprParseException {
            assertEquals(Math.PI, eval("pi"), DELTA);
        }

        @Test
        void euler() throws ExprParseException {
            assertEquals(Math.E, eval("e"), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  9. Variable x
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Variable x")
    class VariableX {
        @Test
        void identity() throws ExprParseException {
            assertEquals(3, ExprParser.parse("x").evaluate(3), DELTA);
        }

        @Test
        void squared() throws ExprParseException {
            assertEquals(9, ExprParser.parse("x^2").evaluate(3), DELTA);
        }

        @Test
        void linear() throws ExprParseException {
            assertEquals(7, ExprParser.parse("2*x+1").evaluate(3), DELTA);
        }

        @Test
        void sinOfX() throws ExprParseException {
            assertEquals(1.0, ExprParser.parse("sin(x)").evaluate(Math.PI / 2), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  10. Variable t (parseForT)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Variable t (parseForT)")
    class VariableT {
        @Test
        void tSquared() throws ExprParseException {
            assertEquals(9, ExprParser.parseForT("t^2").evaluate(3), DELTA);
        }

        @Test
        void tLinear() throws ExprParseException {
            assertEquals(7, ExprParser.parseForT("2*t+1").evaluate(3), DELTA);
        }

        @Test
        void xIsUnknownInParseForT() {
            assertThrows(ExprParseException.class, () -> ExprParser.parseForT("x"));
        }
    }

    // ---------------------------------------------------------------
    //  11. Implicit multiplication
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Implicit multiplication")
    class ImplicitMultiplication {
        @Test
        void numberTimesVariable() throws ExprParseException {
            assertEquals(6, ExprParser.parse("2x").evaluate(3), DELTA);
        }

        @Test
        void numberTimesConstant() throws ExprParseException {
            assertEquals(2 * Math.PI, eval("2pi"), DELTA);
        }

        @Test
        void numberTimesParenthesized() throws ExprParseException {
            assertEquals(6, eval("2(3)"), DELTA);
        }

        @Test
        void parenthesizedTimesParenthesized() throws ExprParseException {
            assertEquals(6, eval("(2)(3)"), DELTA);
        }

        @Test
        void parenthesizedTimesNumber() throws ExprParseException {
            assertEquals(6, eval("(2)3"), DELTA);
        }

        @Test
        void parenthesizedTimesIdentifier() throws ExprParseException {
            assertEquals(2 * Math.PI, ExprParser.parse("(2)pi").evaluate(0), DELTA);
        }

        @Test
        void identifierTimesNumberNotSupported() {
            // x2 is tokenized as a single identifier "x2", which is unknown → error
            assertThrows(ExprParseException.class, () -> ExprParser.parse("x2"));
        }

        @Test
        void variableTimesParenthesized() throws ExprParseException {
            // x(3) → x*(3), evaluate at x=5 → 15
            assertEquals(15, ExprParser.parse("x(3)").evaluate(5), DELTA);
        }

        @Test
        void xsinXIsNotImplicitMultiplication() {
            // "xsin(x)" is tokenized as identifier "xsin" followed by (, which is an unknown function
            assertThrows(ExprParseException.class, () -> ExprParser.parse("xsin(x)"));
        }
    }

    // ---------------------------------------------------------------
    //  12. Complex expressions
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Complex expressions")
    class ComplexExpressions {
        @Test
        void quadraticAtThree() throws ExprParseException {
            // x^2 + 2*x + 1 at x=3 → 9+6+1 = 16
            assertEquals(16, ExprParser.parse("x^2 + 2*x + 1").evaluate(3), DELTA);
        }

        @Test
        void sinPiOverSix() throws ExprParseException {
            assertEquals(0.5, eval("sin(pi/6)"), DELTA);
        }

        @Test
        void ePowerOne() throws ExprParseException {
            assertEquals(Math.E, eval("e^1"), DELTA);
        }

        @Test
        void sqrtOfSumOfSquares() throws ExprParseException {
            // sqrt(x^2+1) at x=3 → sqrt(10)
            assertEquals(Math.sqrt(10), ExprParser.parse("sqrt(x^2+1)").evaluate(3), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  13. Whitespace handling
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Whitespace handling")
    class WhitespaceHandling {
        @Test
        void spacesAroundOperators() throws ExprParseException {
            assertEquals(5, eval("  2  +  3  "), DELTA);
        }

        @Test
        void spacesInFunctionCall() throws ExprParseException {
            assertEquals(Math.sin(1), ExprParser.parse("sin ( x )").evaluate(1), DELTA);
        }
    }

    // ---------------------------------------------------------------
    //  14. Error cases
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Error cases")
    class ErrorCases {
        @Test
        void emptyString() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse(""));
        }

        @Test
        void blankString() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("   "));
        }

        @Test
        void doublePlusIsActuallyUnaryPlus() throws ExprParseException {
            // "2++3" → 2 + (+3) = 5, not an error
            assertEquals(5, eval("2++3"), DELTA);
        }

        @Test
        void plusStarIsError() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("2+*3"));
        }

        @Test
        void unclosedFunctionParen() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("sin("));
        }

        @Test
        void sinClosingParenWithoutOpening() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("sin)"));
        }

        @Test
        void loneOpenParen() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("("));
        }

        @Test
        void loneCloseParen() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse(")"));
        }

        @Test
        void trailingOperator() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("2+"));
        }

        @Test
        void leadingPlusIsUnary() throws ExprParseException {
            assertEquals(2, eval("+2"), DELTA);
        }

        @Test
        void unknownFunction() {
            assertThrows(ExprParseException.class, () -> ExprParser.parse("unknown_func(1)"));
        }

        @Test
        void maxLengthExceeded() {
            String input = "1" + "+1".repeat(128); // 1 + 128*2 = 257 chars
            assertEquals(257, input.length());
            assertThrows(ExprParseException.class, () -> ExprParser.parse(input));
        }

        @Test
        void deeplyNestedParens() {
            // 21 levels of nesting exceeds MAX_DEPTH (20)
            String input = "(".repeat(21) + "1" + ")".repeat(21);
            assertThrows(ExprParseException.class, () -> ExprParser.parse(input));
        }
    }

    // ---------------------------------------------------------------
    //  15. Special float values
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Special float values")
    class SpecialFloatValues {
        @Test
        void divisionByZeroReturnsNaN() throws ExprParseException {
            // The parser explicitly returns NaN when denominator is 0
            double result = eval("1/0");
            assertTrue(Double.isNaN(result), "1/0 should be NaN");
        }

        @Test
        void negativeDivisionByZeroReturnsNaN() throws ExprParseException {
            double result = eval("-1/0");
            assertTrue(Double.isNaN(result), "-1/0 should be NaN");
        }

        @Test
        void zeroDividedByZeroReturnsNaN() throws ExprParseException {
            double result = eval("0/0");
            assertTrue(Double.isNaN(result), "0/0 should be NaN");
        }

        @Test
        void largePowerReturnsInfinity() throws ExprParseException {
            double result = eval("2^100000");
            assertTrue(Double.isInfinite(result) && result > 0, "2^100000 should be +Infinity");
        }
    }
}
