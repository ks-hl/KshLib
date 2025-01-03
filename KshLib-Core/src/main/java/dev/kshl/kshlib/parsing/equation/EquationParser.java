package dev.kshl.kshlib.parsing.equation;

import dev.kshl.kshlib.misc.GenericParser;
import dev.kshl.kshlib.parsing.equation.exception.ParseException;
import dev.kshl.kshlib.parsing.equation.node.BinaryNode;
import dev.kshl.kshlib.parsing.equation.node.ConstantNode;
import dev.kshl.kshlib.parsing.equation.node.GreaterLessThanNode;
import dev.kshl.kshlib.parsing.equation.node.LogicNode;
import dev.kshl.kshlib.parsing.equation.node.NullaryNode;
import dev.kshl.kshlib.parsing.equation.node.NumberNode;
import dev.kshl.kshlib.parsing.equation.node.OperatorNode;
import dev.kshl.kshlib.parsing.equation.node.SummationNode;
import dev.kshl.kshlib.parsing.equation.node.TrinaryNode;
import dev.kshl.kshlib.parsing.equation.node.UnaryMinusNode;
import dev.kshl.kshlib.parsing.equation.node.UnaryNode;
import dev.kshl.kshlib.parsing.equation.node.VariableNode;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Converts a String to an equation which can be programmatically evaluated.
 * <a href="https://stackoverflow.com/questions/3422673/how-to-evaluate-a-math-expression-given-in-string-form">Partial Credit</a>
 */
public class EquationParser extends GenericParser {
    static final Map<String, Double> CONSTANTS = Map.of( //
            "pi", Math.PI, //
            "e", Math.E, //
            "true", 1D, //
            "false", 0D //
    );
    private final EquationNode.ConstructorParams params;

    private EquationParser(String equation, TimeoutManager timeoutManager, boolean insideSummation, @Nullable EquationNode.EvaluationParams reduceEvaluationParameters) {
        super(equation);
        this.params = new EquationNode.ConstructorParams(timeoutManager, insideSummation, reduceEvaluationParameters);
    }

    public static EquationNode parse(String equation) {
        return builder().parse(equation);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TimeoutManager timeoutManager = new TimeoutManager(5, TimeUnit.SECONDS);
        private boolean insideSummation;
        private boolean reduce = true;
        private int recursiveVariableLimit = 1000;
        private int summationLoopLimit = 100000;

        public Builder timeout(TimeoutManager timeoutManager) {
            this.timeoutManager = timeoutManager;
            return this;
        }

        public Builder setInsideSummation(boolean insideSummation) {
            this.insideSummation = insideSummation;
            return this;
        }

        public Builder setReduce(boolean reduce) {
            this.reduce = reduce;
            return this;
        }

        public Builder setRecursiveVariableLimit(int recursiveVariableLimit) {
            VariableNode.validateRecursiveLimit(recursiveVariableLimit);
            this.recursiveVariableLimit = recursiveVariableLimit;
            return this;
        }

        public Builder setSummationLoopLimit(int summationLoopLimit) {
            this.summationLoopLimit = summationLoopLimit;
            return this;
        }

        public Builder setReduceEvaluationParams(EquationNode.EvaluationParams params) {
            setReduce(params != null);
            if (params != null) {
                setRecursiveVariableLimit(params.recursiveVariableLimit());
                setSummationLoopLimit(params.summationLoopLimit());
            }
            return this;
        }

        public EquationNode parse(String equation) {
            EquationNode.EvaluationParams evaluationParams = null;
            if (reduce) evaluationParams = new EquationNode.EvaluationParams(null, null, timeoutManager, recursiveVariableLimit, summationLoopLimit);
            EquationParser parser = new EquationParser(equation, timeoutManager, insideSummation, evaluationParams);
            return parser.parse();
        }
    }

    @Override
    protected void nextChar() {
        if (params != null && params.timeoutManager() != null) params.timeoutManager().checkIn();
        super.nextChar();
    }

    protected EquationNode parse() {
        init();
        EquationNode node = parseExpression();
        node = new UnaryNode(UnaryNode::checkFinite, node, null, params);
        if (pos < text.length()) throw new ParseException(getUnexpectedCharacterMessage(), pos);
        return node;
    }

    protected EquationNode parseExpression() {
        EquationNode x = parseTerm();
        for (; ; ) {
            if (eat("||")) {
                x = new LogicNode((a, b) -> a || b, x, parseTerm(), "||", params);
            } else if (eat('=')) {
                eat('='); // Can be `=` of `==`
                x = new GreaterLessThanNode(false, true, false, x, parseTerm(), "==", params);
            } else if (eat("!=")) {
                x = new GreaterLessThanNode(true, false, true, x, parseTerm(), "!=", params);
            } else if (eat('>')) {
                boolean orEqual = eat('=');
                x = new GreaterLessThanNode(false, orEqual, true, x, parseTerm(), ">" + (orEqual ? "=" : ""), params);
            } else if (eat('<')) {
                if (eat('>')) {
                    x = new GreaterLessThanNode(true, false, true, x, parseTerm(), "<>", params);
                } else {
                    boolean orEqual = eat('=');
                    x = new GreaterLessThanNode(true, orEqual, false, x, parseTerm(), "<" + (orEqual ? "=" : ""), params);
                }
            } else if (eat('+')) x = new OperatorNode(Double::sum, x, parseTerm(), "+", params); // addition
            else if (eat('-')) x = new OperatorNode((a, b) -> a - b, x, parseTerm(), "-", params); // subtraction
            else return x;
        }
    }

    protected EquationNode parseTerm() {
        EquationNode x = parseFactor();
        for (; ; ) {
            if (eat('*') || ch == '(' || isLetterOrUnderscore())
                x = new OperatorNode((a, b) -> a * b, x, parseFactor(), "*", params); // multiplication
            else if (eat('/'))
                x = new OperatorNode(Operations.DIVISION, x, parseFactor(), "/", params); // division
            else if (eat('%')) x = new OperatorNode((a, b) -> a % b, x, parseFactor(), "%", params); // modulo
            else if (eat("&&")) {
                x = new LogicNode((a, b) -> a && b, x, parseTerm(), "&&", params);
            } else return x;
        }
    }

    protected EquationNode parseFunction(String functionName) {
        if (eat('(')) {
            int startPos = this.pos;
            int depth = 1;
            while (true) {
                if (ch < 0) {
                    throw new ParseException("Missing closing ')' after arguments for " + functionName + "(" + this.text.substring(startPos, this.pos), this.pos);
                } else if (eat(')')) {
                    if (--depth < 1) break;
                } else if (eat('(')) {
                    depth++;
                } else nextChar();
            }
            int endPos = this.pos - 1;
            String[] parts;
            if (endPos > startPos) {
                String eqPart = text.substring(startPos, endPos);
                if (eqPart.contains(",") && eqPart.contains("(")) {
                    parts = eqPart.split("((?<=^[^()]{0,}),)|(,(?=[^()]*$))");
                } else {
                    parts = eqPart.split(",");
                }
            } else {
                parts = new String[0];
            }

            List<Supplier<EquationNode>> parameters = Arrays.stream(parts).map(part -> (Supplier<EquationNode>) () ->
                    builder().timeout(params.timeoutManager()).setInsideSummation(true).setReduceEvaluationParams(params.reduceEvaluationParams()).parse(part)
            ).toList();
            Supplier<ParseException> unknownFunction = () -> new ParseException("Unknown function: " + functionName + "(" + parameters.size() + ")", this.pos);
            return switch (parameters.size()) {
                case 0 -> switch (functionName) {
                    case "rand", "random" -> new NullaryNode(Math::random, functionName, params);
                    default -> throw unknownFunction.get();
                };
                case 1 -> {
                    EquationNode x = parameters.get(0).get();
                    yield switch (functionName) {
                        case "sqrt" -> new UnaryNode(Math::sqrt, x, functionName, params);
                        case "sin" -> new UnaryNode(Math::sin, new UnaryNode(Math::toRadians, x, null, params), functionName, params);
                        case "cos" -> new UnaryNode(Math::cos, new UnaryNode(Math::toRadians, x, null, params), functionName, params);
                        case "tan" -> new UnaryNode(Math::tan, new UnaryNode(Math::toRadians, x, null, params), functionName, params);
                        case "log" -> new UnaryNode(Math::log10, x, functionName, params);
                        case "ln" -> new UnaryNode(Math::log, x, functionName, params);
                        case "floor" -> new UnaryNode(Math::floor, x, functionName, params);
                        case "ceil", "ceiling" -> new UnaryNode(Math::ceil, x, functionName, params);
                        case "round" -> new UnaryNode(d -> (double) Math.round(d), x, functionName, params);
                        case "abs", "absolute" -> new UnaryNode(Math::abs, x, null, params);
                        default -> throw unknownFunction.get();
                    };
                }
                case 2 -> {
                    EquationNode x1 = parameters.get(0).get();
                    EquationNode x2 = parameters.get(1).get();
                    yield switch (functionName) {
                        case "log", "log10", "log_10" -> new BinaryNode((l, r) -> Math.log(l) / Math.log(r), x1, x2, functionName, params);
                        case "floor" -> new BinaryNode(Operations.FLOOR, x1, x2, functionName, params);
                        case "ceil", "ceiling" -> new BinaryNode(Operations.CEILING, x1, x2, functionName, params);
                        case "round" -> new BinaryNode(Operations.ROUND, x1, x2, functionName, params);
                        default -> throw unknownFunction.get();
                    };
                }
                case 3 -> {
                    EquationNode x1 = parameters.get(0).get();
                    EquationNode x2 = parameters.get(1).get();
                    Supplier<EquationNode> x3 = parameters.get(2);

                    yield switch (functionName) {
                        case "if" -> new TrinaryNode((a, b, c) -> EquationNode.toBoolean(a) ? b : c, x1, x2, x3.get(), functionName, params);
                        case "sum", "summation" -> (TrinaryNode) new SummationNode(x1, x2,
                                builder().timeout(params.timeoutManager()).setInsideSummation(true).setReduceEvaluationParams(params.reduceEvaluationParams()).parse(parts[2])
                                , params);
                        default -> throw unknownFunction.get();
                    };
                }
                default -> throw unknownFunction.get();
            };
        } else {
            Double constant = CONSTANTS.get(functionName);
            if (constant != null) return new ConstantNode(constant, () -> functionName, params);

            return new VariableNode(functionName, params);
        }
    }

    protected EquationNode parseFactor() {
        if (eat('-')) return new UnaryMinusNode(d -> -d, parseFactor(), params); // unary minus

        EquationNode x;
        int startPos = this.pos;
        if (eat('(')) { // parentheses
            x = new UnaryNode(d -> d, parseExpression(), "", params); // Wrapping is only necessary for toString operation
            if (!eat(')')) throw new ParseException("Missing ')'", pos);
        } else if (isDigit() || ch == '.') { // numbers
            do nextChar();
            while (isDigit() || ch == '.');
            x = new NumberNode(Double.parseDouble(text.substring(startPos, this.pos)), params);
        } else if (isLetter()) { // functions
            // Must be [a-zA-Z] to start variable, but can have numbers after starting.
            do nextChar();
            while (isLetterOrUnderscore() || isDigit());
            String func = text.substring(startPos, this.pos);
            x = parseFunction(func);
        } else {
            throw new ParseException("Unexpected: '" + (char) ch + "' (" + ch + ") in equation '" + text + "' index " + pos, pos);
        }

        if (eat('^')) x = new OperatorNode(Math::pow, x, parseFactor(), "^", params); // exponentiation

        return x;
    }
}
