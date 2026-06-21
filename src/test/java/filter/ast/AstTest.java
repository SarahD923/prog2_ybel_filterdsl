package filter.ast;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AstTest {

  @Test
  void buildsStringComparison() {
    var expected = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));

    assertBothBuilders("artist == \"Beatles\"", expected);
  }

  @Test
  void buildsNumberComparison() {
    var expected = new Expr.Comparison("year", CompOp.LE, new Value.Num(1990));

    assertBothBuilders("year <= 1990", expected);
  }

  @Test
  void buildsInListInOriginalOrder() {
    var expected = new Expr.InList("genre", List.of(new Value.Str("rock"), new Value.Str("jazz")));

    assertBothBuilders("genre in (\"rock\", \"jazz\")", expected);
  }

  @Test
  void respectsOperatorPrecedence() {
    var artist = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));
    var year = new Expr.Comparison("year", CompOp.LE, new Value.Num(1990));
    var genre = new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock"));
    var expected = new Expr.Or(artist, new Expr.And(year, new Expr.Not(genre)));

    assertBothBuilders("artist == \"Beatles\" or year <= 1990 and not genre == \"rock\"", expected);
  }

  @Test
  void respectsParentheses() {
    var artist = new Expr.Comparison("artist", CompOp.EQ, new Value.Str("Beatles"));
    var year = new Expr.Comparison("year", CompOp.LE, new Value.Num(1990));
    var genre = new Expr.Comparison("genre", CompOp.EQ, new Value.Str("rock"));
    var expected = new Expr.And(new Expr.Or(artist, year), genre);

    assertBothBuilders("(artist == \"Beatles\" or year <= 1990) and genre == \"rock\"", expected);
  }

  @Test
  void buildsLongAndExpressionFromLeftToRight() {
    var first = new Expr.Comparison("year", CompOp.GE, new Value.Num(1960));
    var second = new Expr.Comparison("year", CompOp.LE, new Value.Num(1990));
    var third = new Expr.Comparison("genre", CompOp.NE, new Value.Str("jazz"));
    var expected = new Expr.And(new Expr.And(first, second), third);

    assertBothBuilders("year >= 1960 and year <= 1990 and genre != \"jazz\"", expected);
  }

  @Test
  void removesDoubleNotRecursively() {
    var comparison = new Expr.Comparison("year", CompOp.EQ, new Value.Num(2000));
    var input = new Expr.Not(new Expr.Not(new Expr.Not(new Expr.Not(comparison))));

    assertEquals(comparison, AstBuilders.simplify(input));
  }

  private void assertBothBuilders(String query, Expr expected) {
    var patternResult = new AstBuilderPattern().translate(AstBuilders.parse(query));
    var visitorResult = new AstBuilderVisitor().translate(AstBuilders.parse(query));

    assertAll(
        () -> assertEquals(expected, patternResult), () -> assertEquals(expected, visitorResult));
  }
}
