package com.github.jparse.examples.ebnf;

import com.github.jparse.FluentParser;
import com.github.jparse.Function;
import com.github.jparse.Pair;
import com.github.jparse.ParseContext;
import com.github.jparse.ParseResult;
import com.github.jparse.Sequence;
import com.github.jparse.Sequences;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import static com.github.jparse.CharParsers.literal;
import static com.github.jparse.CharParsers.pattern;
import static com.github.jparse.Parsers.phrase;

public final class Ebnf {

    private static final FluentParser<Character, ?> comments = pattern("/\\*.*?\\*/").rep().log("comments");
    private static final FluentParser<Character, Expression> quantExpr;
    private static final FluentParser<Character, Expression> concatExpr;
    private static final FluentParser<Character, Expression> altExpr;
    private static final FluentParser<Character, Grammar> grammar;

    static {
        Function<String, Identifier> newIdent = new Function<String, Identifier>() {
            @Override
            public Identifier apply(String arg) {
                return new Identifier(arg);
            }
        };

        Function<String, TerminalExpression> newTermExpr = new Function<String, TerminalExpression>() {
            @Override
            public TerminalExpression apply(String arg) {
                return new TerminalExpression(arg);
            }
        };

        Function<Identifier, IdentifierExpression> newIdentExpr = new Function<Identifier, IdentifierExpression>() {
            @Override
            public IdentifierExpression apply(Identifier arg) {
                return new IdentifierExpression(arg);
            }
        };

        Function<Expression, OptionExpression> newOptExpr = new Function<Expression, OptionExpression>() {
            @Override
            public OptionExpression apply(Expression arg) {
                return new OptionExpression(arg);
            }
        };

        Function<Expression, RepetitionExpression> newRepExpr = new Function<Expression, RepetitionExpression>() {
            @Override
            public RepetitionExpression apply(Expression arg) {
                return new RepetitionExpression(arg);
            }
        };

        Function<Expression, Repetition1Expression> newRep1Expr = new Function<Expression, Repetition1Expression>() {
            @Override
            public Repetition1Expression apply(Expression arg) {
                return new Repetition1Expression(arg);
            }
        };

        Function<Pair<Expression, Expression>, AlternationExpression> newAltExpr = new Function<Pair<Expression,
                Expression>, AlternationExpression>() {
            @Override
            public AlternationExpression apply(Pair<Expression, Expression> arg) {
                return new AlternationExpression(arg.getLeft(), arg.getRight());
            }
        };

        Function<Pair<Expression, Expression>, ConcatenationExpression> newConcatExpr = new Function<Pair<Expression,
                Expression>, ConcatenationExpression>() {
            @Override
            public ConcatenationExpression apply(Pair<Expression, Expression> arg) {
                return new ConcatenationExpression(arg.getLeft(), arg.getRight());
            }
        };

        Function<Pair<Identifier, Expression>, Rule> newRule = new Function<Pair<Identifier, Expression>, Rule>() {
            @Override
            public Rule apply(Pair<Identifier, Expression> arg) {
                return new Rule(arg.getLeft(), arg.getRight());
            }
        };

        Function<Collection<Rule>, Grammar> newGrammar = new Function<Collection<Rule>, Grammar>() {
            @Override
            public Grammar apply(Collection<Rule> arg) {
                return new Grammar(arg);
            }
        };

        FluentParser<Character, Expression> quantExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence, ParseContext context) {
                return quantExpr.parse(sequence, context);
            }
        };

        FluentParser<Character, Expression> concatExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence, ParseContext context) {
                return concatExpr.parse(sequence, context);
            }
        };

        FluentParser<Character, Expression> altExprRef = new FluentParser<Character, Expression>() {
            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence, ParseContext context) {
                return altExpr.parse(sequence, context);
            }
        };

        FluentParser<Character, Identifier> ident = comments.thenRight(pattern("^[A-Za-z][0-9A-Za-z_]*"))
                .map(newIdent)
                .log("ident");

        FluentParser<Character, TerminalExpression> termExpr = comments.thenRight(
                literal("'").thenRight(pattern("[^']*"))
                        .thenLeft(literal("'").asError())
                        .orelse(literal("\"").thenRight(pattern("[^\"]*")).thenLeft(literal("\"").asError()))
                        .<String>cast()).map(newTermExpr).log("termExpr");

        FluentParser<Character, IdentifierExpression> identExpr = ident.map(newIdentExpr).log("identExpr");

        FluentParser<Character, Expression> groupExpr = comments.thenRight(literal("("))
                .thenRight(altExprRef)
                .thenLeft(comments)
                .thenLeft(literal(")").asError())
                .log("groupExpr");

        FluentParser<Character, OptionExpression> optExpr = quantExprRef.thenLeft(comments)
                .thenLeft(literal("?"))
                .map(newOptExpr)
                .log("optExpr");

        FluentParser<Character, RepetitionExpression> repExpr = quantExprRef.thenLeft(comments)
                .thenLeft(literal("*"))
                .map(newRepExpr)
                .log("repExpr");

        FluentParser<Character, Repetition1Expression> rep1Expr = quantExprRef.thenLeft(comments)
                .thenLeft(literal("+"))
                .map(newRep1Expr)
                .log("rep1Expr");

        quantExpr = optExpr.orelse(repExpr)
                .orelse(rep1Expr)
                .orelse(termExpr.orelse(identExpr).orelse(groupExpr))
                .<Expression>cast()
                .memo()
                .log("quantExpr");

        concatExpr = concatExprRef.thenLeft(comments)
                .then(quantExpr)
                .map(newConcatExpr)
                .orelse(quantExpr)
                .<Expression>cast()
                .memo()
                .log("concatExpr");

        altExpr = altExprRef.thenLeft(comments)
                .thenLeft(literal("|"))
                .then(concatExpr)
                .map(newAltExpr)
                .orelse(concatExpr)
                .<Expression>cast()
                .memo()
                .log("altExpr");

        FluentParser<Character, Rule> rule = ident.thenLeft(comments)
                .thenLeft(literal(":").asError())
                .then(altExpr)
                .thenLeft(comments)
                .thenLeft(literal(";").asError())
                .map(newRule)
                .log("rule");

        grammar = rule.rep1().thenLeft(comments).thenLeft(pattern("\\s*")).map(newGrammar).asFailure().log("grammar");
    }

    public static void main(String[] args) throws IOException {
        String sequence = readFully(new InputStreamReader(Ebnf.class.getResourceAsStream("grammar")));
        ParseResult<Character, Grammar> result = phrase(grammar).parse(Sequences.forCharSequence(sequence),
                new ParseContext());
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
}
