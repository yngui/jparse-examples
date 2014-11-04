package com.github.jparse.examples.calculator;

import com.github.jparse.FluentParser;
import com.github.jparse.Function;
import com.github.jparse.LogParser;
import com.github.jparse.MemoParser;
import com.github.jparse.Pair;
import com.github.jparse.ParseResult;
import com.github.jparse.Parser;
import com.github.jparse.Sequence;
import com.github.jparse.Sequences;

import java.math.BigDecimal;

import static com.github.jparse.CharParsers.literal;
import static com.github.jparse.CharParsers.pattern;

public final class Calculator implements Parser<Character, BigDecimal> {

    private static final Function<String, BigDecimal> newBigDecimal = new Function<String, BigDecimal>() {
        @Override
        public BigDecimal apply(String arg) {
            return new BigDecimal(arg);
        }
    };
    private static final Function<Pair<BigDecimal, BigDecimal>, BigDecimal> add = new Function<Pair<BigDecimal,
            BigDecimal>, BigDecimal>() {
        @Override
        public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
            return arg.getLeft().add(arg.getRight());
        }
    };
    private static final Function<Pair<BigDecimal, BigDecimal>, BigDecimal> subtract = new Function<Pair<BigDecimal,
            BigDecimal>, BigDecimal>() {
        @Override
        public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
            return arg.getLeft().subtract(arg.getRight());
        }
    };
    private static final Function<Pair<BigDecimal, BigDecimal>, BigDecimal> multiply = new Function<Pair<BigDecimal,
            BigDecimal>, BigDecimal>() {
        @Override
        public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
            return arg.getLeft().multiply(arg.getRight());
        }
    };
    private static final Function<Pair<BigDecimal, BigDecimal>, BigDecimal> divide = new Function<Pair<BigDecimal,
            BigDecimal>, BigDecimal>() {
        @Override
        public BigDecimal apply(Pair<BigDecimal, BigDecimal> arg) {
            return arg.getLeft().divide(arg.getRight());
        }
    };

    private final MemoParser.Context<Character> memoContext = new MemoParser.Context<>();
    private final LogParser.Context logContext = new LogParser.Context();
    private final FluentParser<Character, BigDecimal> multiplicationOrDivision;
    private final FluentParser<Character, BigDecimal> additionOrSubtraction;
    private final FluentParser<Character, BigDecimal> expr;

    public Calculator() {
        FluentParser<Character, BigDecimal> multiplicationOrDivisionRef = new FluentParser<Character, BigDecimal>() {
            @Override
            public ParseResult<Character, BigDecimal> parse(Sequence<Character> sequence) {
                return multiplicationOrDivision.parse(sequence);
            }
        };
        FluentParser<Character, BigDecimal> additionOrSubtractionRef = new FluentParser<Character, BigDecimal>() {
            @Override
            public ParseResult<Character, BigDecimal> parse(Sequence<Character> sequence) {
                return additionOrSubtraction.parse(sequence);
            }
        };

        FluentParser<Character, BigDecimal> number = pattern("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?").map(newBigDecimal)
                .memo(memoContext)
                .log("number", logContext);
        FluentParser<Character, BigDecimal> grouping = literal("(").asError()
                .thenRight(additionOrSubtractionRef)
                .thenLeft(literal(")").asError())
                .memo(memoContext)
                .log("grouping", logContext);
        FluentParser<Character, BigDecimal> numberOrGrouping = number.orelse(grouping)
                .memo(memoContext)
                .log("numberOrGrouping", logContext)
                .cast();

        FluentParser<Character, BigDecimal> multiplication = multiplicationOrDivisionRef.thenLeft(literal("*"))
                .then(numberOrGrouping)
                .map(multiply)
                .memo(memoContext)
                .log("multiplication", logContext);
        FluentParser<Character, BigDecimal> division = multiplicationOrDivisionRef.thenLeft(literal("/"))
                .then(numberOrGrouping)
                .map(divide)
                .memo(memoContext)
                .log("division", logContext);
        multiplicationOrDivision = multiplication.orelse(division)
                .orelse(numberOrGrouping)
                .memo(memoContext)
                .log("multiplicationOrDivision", logContext)
                .cast();

        FluentParser<Character, BigDecimal> addition = additionOrSubtractionRef.thenLeft(literal("+"))
                .then(multiplicationOrDivisionRef)
                .map(add)
                .memo(memoContext)
                .log("addition", logContext);
        FluentParser<Character, BigDecimal> subtraction = additionOrSubtractionRef.thenLeft(literal("-"))
                .then(multiplicationOrDivisionRef)
                .map(subtract)
                .memo(memoContext)
                .log("subtraction", logContext);
        additionOrSubtraction = addition.orelse(subtraction)
                .orelse(multiplicationOrDivisionRef)
                .memo(memoContext)
                .log("additionOrSubtraction", logContext)
                .cast();

        expr = additionOrSubtraction.asFailure();
    }

    public static void main(String[] args) {
        String sequence = "1+(2-3)*4";
        ParseResult<Character, BigDecimal> result = new Calculator().parse(Sequences.forCharSequence(sequence));
        if (result.isSuccess()) {
            System.out.println(result.getResult());
        } else {
            System.out.println(result.getMessage() + " at " + (sequence.length() - result.getRest().length()));
        }
    }

    @Override
    public ParseResult<Character, BigDecimal> parse(Sequence<Character> sequence) {
        try {
            return expr.phrase().parse(sequence);
        } finally {
            memoContext.reset();
            logContext.reset();
        }
    }
}
