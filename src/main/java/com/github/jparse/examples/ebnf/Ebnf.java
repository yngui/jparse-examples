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

public class Ebnf {

    private static final FluentParser<Character, ?> COMMENTS = pattern("/\\*.*?\\*/").rep().log("comments");
    private static final FluentParser<Character, Expression> QUANT_EXPR;
    private static final FluentParser<Character, Expression> CONCAT_EXPR;
    private static final FluentParser<Character, Expression> ALT_EXPR;
    private static final FluentParser<Character, Grammar> GRAMMAR;

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

        Function<Pair<Expression, Expression>, AlternationExpression> newAltExpr =
                new Function<Pair<Expression, Expression>, AlternationExpression>() {

                    @Override
                    public AlternationExpression apply(Pair<Expression, Expression> arg) {
                        return new AlternationExpression(arg.getLeft(), arg.getRight());
                    }
                };

        Function<Pair<Expression, Expression>, ConcatenationExpression> newConcatExpr =
                new Function<Pair<Expression, Expression>, ConcatenationExpression>() {

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
                return QUANT_EXPR.parse(sequence, context);
            }
        };

        FluentParser<Character, Expression> concatExprRef = new FluentParser<Character, Expression>() {

            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence, ParseContext context) {
                return CONCAT_EXPR.parse(sequence, context);
            }
        };

        FluentParser<Character, Expression> altExprRef = new FluentParser<Character, Expression>() {

            @Override
            public ParseResult<Character, Expression> parse(Sequence<Character> sequence, ParseContext context) {
                return ALT_EXPR.parse(sequence, context);
            }
        };

        FluentParser<Character, Identifier> ident =
                COMMENTS.thenRight(pattern("^[A-Za-z][0-9A-Za-z_]*")).map(newIdent).log("ident");

        FluentParser<Character, TerminalExpression> termExpr = COMMENTS.thenRight(
                literal("'").thenRight(pattern("[^']*"))
                        .thenLeft(literal("'").asError())
                        .orelse(literal("\"").thenRight(pattern("[^\"]*")).thenLeft(literal("\"").asError()))
                        .<String>cast()).map(newTermExpr).log("termExpr");

        FluentParser<Character, IdentifierExpression> identExpr = ident.map(newIdentExpr).log("identExpr");

        FluentParser<Character, Expression> groupExpr = COMMENTS.thenRight(literal("("))
                .thenRight(altExprRef)
                .thenLeft(COMMENTS)
                .thenLeft(literal(")").asError())
                .log("groupExpr");

        FluentParser<Character, OptionExpression> optExpr =
                quantExprRef.thenLeft(COMMENTS).thenLeft(literal("?")).map(newOptExpr).log("optExpr");

        FluentParser<Character, RepetitionExpression> repExpr =
                quantExprRef.thenLeft(COMMENTS).thenLeft(literal("*")).map(newRepExpr).log("repExpr");

        FluentParser<Character, Repetition1Expression> rep1Expr =
                quantExprRef.thenLeft(COMMENTS).thenLeft(literal("+")).map(newRep1Expr).log("rep1Expr");

        QUANT_EXPR = optExpr.orelse(repExpr)
                .orelse(rep1Expr)
                .orelse(termExpr.orelse(identExpr).orelse(groupExpr))
                .<Expression>cast()
                .memo()
                .log("quantExpr");

        CONCAT_EXPR = concatExprRef.thenLeft(COMMENTS)
                .then(QUANT_EXPR)
                .map(newConcatExpr)
                .orelse(QUANT_EXPR)
                .<Expression>cast()
                .memo()
                .log("concatExpr");

        ALT_EXPR = altExprRef.thenLeft(COMMENTS)
                .thenLeft(literal("|"))
                .then(CONCAT_EXPR)
                .map(newAltExpr)
                .orelse(CONCAT_EXPR)
                .<Expression>cast()
                .memo()
                .log("altExpr");

        FluentParser<Character, Rule> rule = ident.thenLeft(COMMENTS)
                .thenLeft(literal(":").asError())
                .then(ALT_EXPR)
                .thenLeft(COMMENTS)
                .thenLeft(literal(";").asError())
                .map(newRule)
                .log("rule");

        GRAMMAR = rule.rep1().thenLeft(COMMENTS).thenLeft(pattern("\\s*")).map(newGrammar).asFailure().log("grammar");
    }

    public static void main(String[] args) throws IOException {
        String sequence = readFully(new InputStreamReader(Ebnf.class.getResourceAsStream("grammar")));
        ParseResult<Character, Grammar> result =
                phrase(GRAMMAR).parse(Sequences.forCharSequence(sequence), new ParseContext());
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