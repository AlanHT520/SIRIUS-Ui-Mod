package net.alan.gui.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ExpressionEvaluator {
    private static final Map<String, Expression> CACHE = new ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExpressionEvaluator.class);

    public static int eval(String expr, int screenWidth, int screenHeight,
                           int elementWidth, int elementHeight) {
        return eval(expr, screenWidth, screenHeight, elementWidth, elementHeight, null);
    }

    public static int eval(String expr, int screenWidth, int screenHeight,
                           int elementWidth, int elementHeight,
                           Map<String, Integer> customVars) {
        if (expr == null || expr.trim().isEmpty()) return 0;
        String trimmed = expr.trim();
        if (trimmed.equals("center")) {
            return (screenWidth - elementWidth) / 2;
        }
        if (trimmed.startsWith("bottom-")) {
            int offset = Integer.parseInt(trimmed.substring(7));
            return screenHeight - elementHeight - offset;
        }

        Map<String, Integer> vars = new HashMap<>();
        vars.put("screen.width", screenWidth);
        vars.put("screen.height", screenHeight);
        vars.put("this.width", elementWidth);
        vars.put("this.height", elementHeight);
        if (customVars != null) vars.putAll(customVars);

        Expression expression = CACHE.computeIfAbsent(trimmed, Expression::compile);
        return (int) Math.round(expression.eval(vars));
    }

    public static String evalString(String expr, int screenWidth, int screenHeight,
                                     int elementWidth, int elementHeight,
                                     Map<String, Integer> customVars) {
        if (expr == null || expr.trim().isEmpty()) return expr;
        String trimmed = expr.trim();
        if (!trimmed.contains("?")) return trimmed;

        int qIndex = trimmed.indexOf('?');
        int cIndex = trimmed.lastIndexOf(':');
        if (qIndex < 0 || cIndex < qIndex) return trimmed;

        String condition = trimmed.substring(0, qIndex).trim();
        String trueBranch = trimmed.substring(qIndex + 1, cIndex).trim();
        String falseBranch = trimmed.substring(cIndex + 1).trim();

        try {
            int result = eval(condition, screenWidth, screenHeight, elementWidth, elementHeight, customVars);
            return result != 0 ? trueBranch : falseBranch;
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to evaluate string expression '{}': {}", trimmed, e.getMessage());
            return falseBranch;
        }
    }

    private static class Expression {
        private final Function<Map<String, Integer>, Double> evaluator;
        private Expression(Function<Map<String, Integer>, Double> evaluator) { this.evaluator = evaluator; }
        public double eval(Map<String, Integer> vars) { return evaluator.apply(vars); }
        public static Expression compile(String expr) { return new Expression(new Parser(expr).parse()); }

        private static class Parser {
            private final String input;
            private int pos;
            Parser(String input) { this.input = input; this.pos = 0; }

            private void skipWhitespace() { while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++; }
            private char peek() { return pos < input.length() ? input.charAt(pos) : '\0'; }
            private char consume() { return input.charAt(pos++); }
            private boolean match(char expected) {
                skipWhitespace();
                if (peek() == expected) { pos++; return true; }
                return false;
            }

            private Function<Map<String, Integer>, Double> parse() {
                Function<Map<String, Integer>, Double> expr = parseTernary();
                if (pos < input.length()) throw new RuntimeException("Unexpected character: " + peek());
                return expr;
            }

            private Function<Map<String, Integer>, Double> parseTernary() {
                Function<Map<String, Integer>, Double> cond = parseLogicalOr();
                skipWhitespace();
                if (match('?')) {
                    Function<Map<String, Integer>, Double> trueVal = parseTernary();
                    skipWhitespace();
                    if (!match(':')) throw new RuntimeException("Expected ':' in ternary expression");
                    Function<Map<String, Integer>, Double> falseVal = parseTernary();
                    return vars -> cond.apply(vars) != 0 ? trueVal.apply(vars) : falseVal.apply(vars);
                }
                return cond;
            }

            private Function<Map<String, Integer>, Double> parseLogicalOr() {
                Function<Map<String, Integer>, Double> left = parseLogicalAnd();
                while (true) {
                    skipWhitespace();
                    if (match('|')) {
                        if (match('|')) {
                            Function<Map<String, Integer>, Double> right = parseLogicalAnd();
                            Function<Map<String, Integer>, Double> finalLeft = left;
                            left = vars -> (finalLeft.apply(vars) != 0 || right.apply(vars) != 0) ? 1.0 : 0.0;
                        }
                    } else break;
                }
                return left;
            }

            private Function<Map<String, Integer>, Double> parseLogicalAnd() {
                Function<Map<String, Integer>, Double> left = parseComparison();
                while (true) {
                    skipWhitespace();
                    if (match('&')) {
                        if (match('&')) {
                            Function<Map<String, Integer>, Double> right = parseComparison();
                            Function<Map<String, Integer>, Double> finalLeft = left;
                            left = vars -> (finalLeft.apply(vars) != 0 && right.apply(vars) != 0) ? 1.0 : 0.0;
                        } else {
                            Function<Map<String, Integer>, Double> right = parseComparison();
                            Function<Map<String, Integer>, Double> finalLeft = left;
                            left = vars -> (finalLeft.apply(vars) != 0 && right.apply(vars) != 0) ? 1.0 : 0.0;
                        }
                    } else break;
                }
                return left;
            }

            private Function<Map<String, Integer>, Double> parseComparison() {
                Function<Map<String, Integer>, Double> left = parseAddSub();
                skipWhitespace();
                if (match('<')) {
                    if (match('=')) {
                        Function<Map<String, Integer>, Double> right = parseAddSub();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        return vars -> finalLeft.apply(vars) <= right.apply(vars) ? 1.0 : 0.0;
                    }
                    Function<Map<String, Integer>, Double> right = parseAddSub();
                    Function<Map<String, Integer>, Double> finalLeft = left;
                    return vars -> finalLeft.apply(vars) < right.apply(vars) ? 1.0 : 0.0;
                } else if (match('>')) {
                    if (match('=')) {
                        Function<Map<String, Integer>, Double> right = parseAddSub();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        return vars -> finalLeft.apply(vars) >= right.apply(vars) ? 1.0 : 0.0;
                    }
                    Function<Map<String, Integer>, Double> right = parseAddSub();
                    Function<Map<String, Integer>, Double> finalLeft = left;
                    return vars -> finalLeft.apply(vars) > right.apply(vars) ? 1.0 : 0.0;
                } else if (match('=')) {
                    if (!match('=')) throw new RuntimeException("Expected '==' for equality");
                    Function<Map<String, Integer>, Double> right = parseAddSub();
                    Function<Map<String, Integer>, Double> finalLeft = left;
                    return vars -> finalLeft.apply(vars) == right.apply(vars) ? 1.0 : 0.0;
                } else if (match('!')) {
                    if (!match('=')) throw new RuntimeException("Expected '!=' for inequality");
                    Function<Map<String, Integer>, Double> right = parseAddSub();
                    Function<Map<String, Integer>, Double> finalLeft = left;
                    return vars -> finalLeft.apply(vars) != right.apply(vars) ? 1.0 : 0.0;
                }
                return left;
            }

            private Function<Map<String, Integer>, Double> parseAddSub() {
                Function<Map<String, Integer>, Double> left = parseMulDiv();
                while (true) {
                    skipWhitespace();
                    if (match('+')) {
                        Function<Map<String, Integer>, Double> right = parseMulDiv();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        left = vars -> finalLeft.apply(vars) + right.apply(vars);
                    } else if (match('-')) {
                        Function<Map<String, Integer>, Double> right = parseMulDiv();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        left = vars -> finalLeft.apply(vars) - right.apply(vars);
                    } else break;
                }
                return left;
            }

            private Function<Map<String, Integer>, Double> parseMulDiv() {
                Function<Map<String, Integer>, Double> left = parseUnary();
                while (true) {
                    skipWhitespace();
                    if (match('*')) {
                        Function<Map<String, Integer>, Double> right = parseUnary();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        left = vars -> finalLeft.apply(vars) * right.apply(vars);
                    } else if (match('/')) {
                        Function<Map<String, Integer>, Double> right = parseUnary();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        left = vars -> {
                            double r = right.apply(vars);
                            return r != 0 ? finalLeft.apply(vars) / r : 0;
                        };
                    } else if (match('%')) {
                        Function<Map<String, Integer>, Double> right = parseUnary();
                        Function<Map<String, Integer>, Double> finalLeft = left;
                        left = vars -> {
                            double r = right.apply(vars);
                            return r != 0 ? finalLeft.apply(vars) % r : 0;
                        };
                    } else break;
                }
                return left;
            }

            private Function<Map<String, Integer>, Double> parseUnary() {
                skipWhitespace();
                if (match('!')) {
                    Function<Map<String, Integer>, Double> inner = parseUnary();
                    return vars -> inner.apply(vars) == 0 ? 1.0 : 0.0;
                }
                return parsePrimary();
            }

            private Function<Map<String, Integer>, Double> parsePrimary() {
                skipWhitespace();
                if (match('(')) {
                    Function<Map<String, Integer>, Double> expr = parseLogicalOr();
                    if (!match(')')) throw new RuntimeException("Missing closing parenthesis");
                    return expr;
                }
                if (peek() == '0' && pos + 1 < input.length() &&
                        (input.charAt(pos+1) == 'x' || input.charAt(pos+1) == 'X')) {
                    return parseHexNumber();
                }
                if (Character.isDigit(peek()) || peek() == '.' || peek() == '-') {
                    return parseNumber();
                }
                if (peek() == '$') { return parsePlaceholder(); }
                if (Character.isLetter(peek())) { return parseVariable(); }
                throw new RuntimeException("Unexpected token: " + peek());
            }

            private Function<Map<String, Integer>, Double> parsePlaceholder() {
                consume();
                if (peek() == '{') {
                    consume();
                    int depth = 1;
                    while (pos < input.length() && depth > 0) {
                        char c = consume();
                        if (c == '{') depth++;
                        else if (c == '}') depth--;
                    }
                } else {
                    while (pos < input.length() && Character.isLetter(peek())) consume();
                }
                return vars -> 0.0;
            }

            private Function<Map<String, Integer>, Double> parseHexNumber() {
                int start = pos;
                consume(); consume();
                while (pos < input.length() && (Character.isDigit(peek()) ||
                        (peek() >= 'a' && peek() <= 'f') || (peek() >= 'A' && peek() <= 'F'))) consume();
                String hexStr = input.substring(start, pos);
                double value = (double) Long.parseLong(hexStr.substring(2), 16);
                return vars -> value;
            }

            private Function<Map<String, Integer>, Double> parseNumber() {
                int start = pos;
                boolean negative = false;
                if (peek() == '-') { negative = true; consume(); }
                while (pos < input.length() && Character.isDigit(peek())) consume();
                if (peek() == '.' && pos + 1 < input.length() && Character.isDigit(input.charAt(pos+1))) {
                    consume();
                    while (pos < input.length() && Character.isDigit(peek())) consume();
                }
                String numStr = input.substring(start, pos);
                double value = Double.parseDouble(numStr);
                if (negative) value = -value;
                final double finalValue = value;
                return vars -> finalValue;
            }

            private Function<Map<String, Integer>, Double> parseVariable() {
                int start = pos;
                if (!Character.isLetter(peek())) throw new RuntimeException("Variable must start with a letter");
                while (pos < input.length() && (Character.isLetterOrDigit(peek()) || peek() == '.' || peek() == '_')) consume();
                String varName = input.substring(start, pos);
                if (varName.startsWith(".") || varName.endsWith(".") || varName.contains(".."))
                    throw new RuntimeException("Invalid variable name: " + varName);
                final String finalVarName = varName;
                return vars -> {
                    Integer val = vars.get(finalVarName);
                    if (val == null) {
                        LOGGER.debug("Unknown variable '{}' in expression, using 0.", finalVarName);
                        return 0.0;
                    }
                    return val.doubleValue();
                };
            }
        }
    }
}