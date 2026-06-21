package filter.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.eval.Evaluator;
import filter.ast.nodes.Expr;
import filter.ast.printer.AstPrinter;
import filter.model.Genre;
import filter.model.MediaItem;
import net.jqwik.api.*;

public class RoundtripPropertiesTest {

  @Property(tries = 100)
  void bothBuildersCreateSameAst(@ForAll("simpleQueries") String query) {
    assertEquals(buildWithPattern(query), buildWithVisitor(query));
  }

  @Property(tries = 100)
  void patternBuilderSupportsRoundtrip(@ForAll("simpleQueries") String query) {
    var firstAst = buildWithPattern(query);
    var printed = AstPrinter.toString(firstAst);

    assertEquals(firstAst, buildWithPattern(printed));
  }

  @Property(tries = 100)
  void visitorBuilderSupportsRoundtrip(@ForAll("simpleQueries") String query) {
    var firstAst = buildWithVisitor(query);
    var printed = AstPrinter.toString(firstAst);

    assertEquals(firstAst, buildWithVisitor(printed));
  }

  @Property(tries = 100)
  void roundtripCanSwitchBetweenBuilders(@ForAll("simpleQueries") String query) {
    var visitorAst = buildWithVisitor(query);
    var printed = AstPrinter.toString(visitorAst);

    assertEquals(visitorAst, buildWithPattern(printed));
  }

  @Property(tries = 100)
  void simplifyingTwiceHasSameResult(@ForAll("simpleQueries") String query) {
    var ast = buildWithPattern(query);

    assertEquals(AstBuilders.simplify(ast), AstBuilders.simplify(AstBuilders.simplify(ast)));
  }

  @Property(tries = 100)
  void andIsCommutative(
      @ForAll("simpleQueries") String leftQuery,
      @ForAll("simpleQueries") String rightQuery,
      @ForAll("mediaItems") MediaItem item) {
    var left = buildWithPattern(leftQuery);
    var right = buildWithPattern(rightQuery);

    var firstOrder = new Expr.And(left, right);
    var secondOrder = new Expr.And(right, left);

    assertEquals(Evaluator.matches(item, firstOrder), Evaluator.matches(item, secondOrder));
  }

  private Expr buildWithPattern(String query) {
    return AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);
  }

  private Expr buildWithVisitor(String query) {
    return AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);
  }

  // ---------- @Provide-Methods for Arbitraries ----------

  @Provide
  Arbitrary<String> fields() {
    return Arbitraries.of("title", "artist", "genre", "year");
  }

  @Provide
  Arbitrary<String> stringLiterals() {
    return Arbitraries.strings()
        .withChars("abcxyz")
        .ofMinLength(1)
        .ofMaxLength(5)
        .map(s -> "\"" + s + "\"");
  }

  @Provide
  Arbitrary<String> numberLiterals() {
    return Arbitraries.integers().between(1900, 2025).map(Object::toString);
  }

  @Provide
  Arbitrary<String> comparisons() {
    Arbitrary<String> ops = Arbitraries.of("==", "!=", "<", "<=", ">", ">=");

    Arbitrary<String> stringComp =
        Combinators.combine(fields(), ops, stringLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    Arbitrary<String> numberComp =
        Combinators.combine(Arbitraries.of("year"), ops, numberLiterals())
            .as((f, op, lit) -> f + " " + op + " " + lit);

    return Arbitraries.oneOf(stringComp, numberComp);
  }

  @Provide
  Arbitrary<String> simpleQueries() {
    return comparisons()
        .list()
        .ofMinSize(1)
        .ofMaxSize(3)
        .map(
            list -> {
              if (list.size() == 1) return list.getFirst();
              StringBuilder sb = new StringBuilder();
              for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                  String conn = Arbitraries.of(" and ", " or ").sample();
                  sb.append(conn);
                }
                sb.append(list.get(i));
              }
              return sb.toString();
            });
  }

  @Provide
  Arbitrary<MediaItem> mediaItems() {
    var words = Arbitraries.strings().withChars("abcxyz").ofMinLength(1).ofMaxLength(5);
    var genres = Arbitraries.of(Genre.ROCK, Genre.JAZZ, Genre.GRUNGE, Genre.UNKNOWN);
    var years = Arbitraries.integers().between(1900, 2025);

    return Combinators.combine(words, words, genres, years).as(MediaItem::new);
  }
}
