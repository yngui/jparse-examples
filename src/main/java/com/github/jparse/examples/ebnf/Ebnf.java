package com.github.jparse.examples.ebnf;

import com.github.jparse.FluentParser;
import com.github.jparse.Function;
import com.github.jparse.LogParser;
import com.github.jparse.MemoParser;
import com.github.jparse.Pair;
import com.github.jparse.ParseResult;
import com.github.jparse.Parser;
import com.github.jparse.Sequence;
import com.github.jparse.Sequences;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import static com.github.jparse.CharParsers.literal;
import static com.github.jparse.CharParsers.pattern;
import static com.github.jparse.Parsers.phrase;

public final class Ebnf implements Parser<Character, Grammar> {

    private static final Function<String, Identifier> newIdent = new Function<String, Identifier>() {
        @Override
        public Identifier apply(String arg) {
            return new Identifier(arg);
        }
    };
    private static final Function<String, TerminalExpression> newTermExpr = new Function<String, TerminalExpression>() {
        @Override
        public TerminalExpression apply(String arg) {
            return new TerminalExpression(arg);
        }
    };
    private static final Function<Identifier, IdentifierExpression> newIdentExpr = new Function<Identifier,
            IdentifierExpression>() {
        @Override
        public IdentifierExpression apply(Identifier arg) {
            return new IdentifierExpression(arg);
        }
    };
    private static final Function<Expression, OptionExpression> newOptExpr = new Function<Expression,
            OptionExpression>() {
        @Override
        public OptionExpression apply(Expression arg) {
            return new OptionExpression(arg);
        }
    };
    private static final Function<Expression, RepetitionExpression> newRepExpr = new Function<Expression,
            RepetitionExpression>() {
        @Override
        public RepetitionExpression apply(Expression arg) {
            return new RepetitionExpression(arg);
        }
    };
    private static final Function<Expression, Repetition1Expression> newRep1Expr = new Function<Expression,
            Repetition1Expression>() {
        @Override
        public Repetition1Expression apply(Expression arg) {
            return new Repetition1Expression(arg);
        }
    };
    private static final Function<Pair<Expression, Expression>, AlternationExpression> newAltExpr = new
            Function<Pair<Expression, Expression>, AlternationExpression>() {
        @Override
        public AlternationExpression apply(Pair<Expression, Expression> arg) {
            return new AlternationExpression(arg.getLeft(), arg.getRight());
        }
    };
    private static final Function<Pair<Expression, Expression>, ConcatenationExpression> newConcatExpr = new
            Function<Pair<Expression, Expression>, ConcatenationExpression>() {
        @Override
        public ConcatenationExpression apply(Pair<Expression, Expression> arg) {
            return new ConcatenationExpression(arg.getLeft(), arg.getRight());
        }
    };
    private static final Function<Pair<Identifier, Expression>, Rule> newRule = new Function<Pair<Identifier,
            Expression>, Rule>() {
        @Override
        public Rule apply(Pair<Identifier, Expression> arg) {
            return new Rule(arg.getLeft(), arg.getRight());
        }
    };
    private static final Function<Collection<Rule>, Grammar> newGrammar = new Function<Collection<Rule>, Grammar>() {
        @Override
        public Grammar apply(Collection<Rule> arg) {
            return new Grammar(arg);
        }
    };

    private final Context context;
    private final FluentParser<Character, Expression> quantExpr;
    private final FluentParser<Character, Expression> concatExpr;
    private final FluentParser<Character, Expression> altExpr;
    private final FluentParser<Character, Grammar> grammar;

    public Ebnf() {
        this(new Context());
    }

    public Ebnf(Context context) {
        this.context = context;
        MemoParser.Context<Character> memoContext = context.memoContext;
        LogParser.Context logContext = context.logContext;

        FluentParser<Character, Expression> quantExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence) {
                return quantExpr.parse(sequence);
            }
        };

        FluentParser<Character, Expression> concatExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence) {
                return concatExpr.parse(sequence);
            }
        };

        FluentParser<Character, Expression> altExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence) {
                return altExpr.parse(sequence);
            }
        };

        FluentParser<Character, ?> comments = pattern("/\\*.*?\\*/").rep().log("comments", logContext);

        FluentParser<Character, Identifier> ident = comments.thenRight(pattern("^[A-Za-z][0-9A-Za-z_]*"))
                .map(newIdent)
                .log("ident", logContext);

        FluentParser<Character, TerminalExpression> termExpr = comments.thenRight(
                literal("'").thenRight(pattern("[^']*"))
                        .thenLeft(literal("'").asError())
                        .orelse(literal("\"").thenRight(pattern("[^\"]*")).thenLeft(literal("\"").asError()))
                        .<String>cast()).map(newTermExpr).log("termExpr", logContext);

        FluentParser<Character, IdentifierExpression> identExpr = ident.map(newIdentExpr).log("identExpr", logContext);

        FluentParser<Character, Expression> groupExpr = comments.thenRight(literal("("))
                .thenRight(altExprRef)
                .thenLeft(comments)
                .thenLeft(literal(")").asError())
                .log("groupExpr", logContext);

        FluentParser<Character, OptionExpression> optExpr = quantExprRef.thenLeft(comments)
                .thenLeft(literal("?"))
                .map(newOptExpr)
                .log("optExpr", logContext);

        FluentParser<Character, RepetitionExpression> repExpr = quantExprRef.thenLeft(comments)
                .thenLeft(literal("*"))
                .map(newRepExpr)
                .log("repExpr", logContext);

        FluentParser<Character, Repetition1Expression> rep1Expr = quantExprRef.thenLeft(comments)
                .thenLeft(literal("+"))
                .map(newRep1Expr)
                .log("rep1Expr", logContext);

        quantExpr = optExpr.orelse(repExpr)
                .orelse(rep1Expr)
                .orelse(termExpr.orelse(identExpr).orelse(groupExpr))
                .<Expression>cast()
                .memo(memoContext)
                .log("quantExpr", logContext);

        concatExpr = concatExprRef.thenLeft(comments)
                .then(quantExpr)
                .map(newConcatExpr)
                .orelse(quantExpr)
                .<Expression>cast()
                .memo(memoContext)
                .log("concatExpr", logContext);

        altExpr = altExprRef.thenLeft(comments)
                .thenLeft(literal("|"))
                .then(concatExpr)
                .map(newAltExpr)
                .orelse(concatExpr)
                .<Expression>cast()
                .memo(memoContext)
                .log("altExpr", logContext);

        FluentParser<Character, Rule> rule = ident.thenLeft(comments)
                .thenLeft(literal(":").asError())
                .then(altExpr)
                .thenLeft(comments)
                .thenLeft(literal(";").asError())
                .map(newRule)
                .log("rule", logContext);

        grammar = rule.rep1()
                .thenLeft(comments)
                .thenLeft(pattern("\\s*"))
                .map(newGrammar)
                .asFailure()
                .log("grammar", logContext);
    }

    public static void main(String[] args) throws IOException {
        String sequence = readFully(new InputStreamReader(Ebnf.class.getResourceAsStream("grammar")));
        ParseResult<Character, Grammar> result = new Ebnf().parse(Sequences.forCharSequence(sequence));
        if (result.isSuccess()) {
            Grammar grammar = result.getResult();
            System.out.println(grammar);
        } else {
            System.out.println(result.getMessage() + " at " + (sequence.length() - result.getRest().length()));
        }
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
        return sb.toString();
    }

    public Context getContext() {
        return context;
    }

    @Override
    public ParseResult<Character, Grammar> parse(Sequence<Character> sequence) {
        return phrase(grammar).parse(sequence);
    }

    public static final class Context {

        final MemoParser.Context<Character> memoContext = new MemoParser.Context<>();
        final LogParser.Context logContext = new LogParser.Context();

        public void reset() {
            memoContext.reset();
            logContext.reset();
        }
    }
}
