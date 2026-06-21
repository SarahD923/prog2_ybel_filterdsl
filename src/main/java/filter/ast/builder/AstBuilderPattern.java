package filter.ast.builder;

import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.TerminalNode;

public class AstBuilderPattern {

  // Public entry point
  // query  : expr EOF
  public Expr translate(FilterParser.QueryContext ctx) {
    return buildExpr(ctx.expr());
  }

  // expr: orExpr
  private Expr buildExpr(FilterParser.ExprContext ctx) {
    return buildOrExpr(ctx.orExpr());
  }

  // orExpr : andExpr (OR andExpr)*
  private Expr buildOrExpr(FilterParser.OrExprContext ctx) {
    var operands = ctx.andExpr();
    Expr result = buildAndExpr(operands.getFirst());
    for (int i = 1; i < operands.size(); i++) {
      result = new Expr.Or(result, buildAndExpr(operands.get(i)));
    }
    return result;
  }

  // andExpr: notExpr (AND notExpr)*
  private Expr buildAndExpr(FilterParser.AndExprContext ctx) {
    var operands = ctx.notExpr();
    Expr result = buildNotExpr(operands.getFirst());
    for (int i = 1; i < operands.size(); i++) {
      result = new Expr.And(result, buildNotExpr(operands.get(i)));
    }
    return result;
  }

  // notExpr: NOT notExpr | primary
  private Expr buildNotExpr(FilterParser.NotExprContext ctx) {
    return switch (ctx.getChild(0)) {
      case FilterParser.PrimaryContext primary -> buildPrimary(primary);
      case TerminalNode ignored -> new Expr.Not(buildNotExpr(ctx.notExpr()));
      default -> throw new IllegalArgumentException("Unknown not expression: " + ctx.getText());
    };
  }

  // primary: comparison | '(' expr ')'
  private Expr buildPrimary(FilterParser.PrimaryContext ctx) {
    return switch (ctx.getChild(0)) {
      case FilterParser.ComparisonContext comparison -> buildComparison(comparison);
      case TerminalNode ignored -> buildExpr(ctx.expr());
      default -> throw new IllegalArgumentException("Unknown primary expression: " + ctx.getText());
    };
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  private Expr buildComparison(FilterParser.ComparisonContext ctx) {
    var field = ctx.IDENTIFIER().getText();
    return switch (ctx.getChild(2)) {
      case FilterParser.LiteralContext literal ->
          new Expr.Comparison(
              field, CompOp.fromSymbol(ctx.COMPOP().getText()), buildLiteral(literal));
      case TerminalNode ignored -> new Expr.InList(field, buildLiteralList(ctx.literalList()));
      default -> throw new IllegalArgumentException("Unknown comparison: " + ctx.getText());
    };
  }

  // literalList: literal (',' literal)*
  private List<Value> buildLiteralList(FilterParser.LiteralListContext ctx) {
    var result = new ArrayList<Value>();
    for (var literal : ctx.literal()) {
      result.add(buildLiteral(literal));
    }
    return List.copyOf(result);
  }

  // literal: STRING | NUMBER
  private Value buildLiteral(FilterParser.LiteralContext ctx) {
    return switch (ctx.getStart().getType()) {
      case FilterParser.STRING -> new Value.Str(readString(ctx.getText()));
      case FilterParser.NUMBER -> new Value.Num(Integer.parseInt(ctx.getText()));
      default -> throw new IllegalArgumentException("Unknown literal: " + ctx.getText());
    };
  }

  private String readString(String text) {
    var result = new StringBuilder();
    var content = text.substring(1, text.length() - 1);
    boolean escaped = false;
    for (char c : content.toCharArray()) {
      if (escaped) {
        result.append(c);
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }
}
