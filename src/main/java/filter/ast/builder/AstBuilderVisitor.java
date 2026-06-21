package filter.ast.builder;

import filter.FilterBaseVisitor;
import filter.FilterParser;
import filter.ast.nodes.CompOp;
import filter.ast.nodes.Expr;
import filter.ast.nodes.Value;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AstBuilderVisitor extends FilterBaseVisitor<Void> {

  private final Deque<Expr> expressions = new ArrayDeque<>();
  private final Deque<Value> values = new ArrayDeque<>();
  private final Deque<List<Value>> valueLists = new ArrayDeque<>();

  // Public entry point
  public Expr translate(FilterParser.QueryContext ctx) {
    expressions.clear();
    values.clear();
    valueLists.clear();

    visit(ctx);
    if (expressions.size() != 1 || !values.isEmpty() || !valueLists.isEmpty()) {
      throw new IllegalStateException("Could not build exactly one expression");
    }
    return expressions.pop();
  }

  // query  : expr EOF
  @Override
  public Void visitQuery(FilterParser.QueryContext ctx) {
    visit(ctx.expr());
    return null;
  }

  // expr: orExpr
  @Override
  public Void visitExpr(FilterParser.ExprContext ctx) {
    visit(ctx.orExpr());
    return null;
  }

  // orExpr : andExpr (OR andExpr)*
  @Override
  public Void visitOrExpr(FilterParser.OrExprContext ctx) {
    var operands = ctx.andExpr();
    visit(operands.getFirst());
    for (int i = 1; i < operands.size(); i++) {
      visit(operands.get(i));
      var right = expressions.pop();
      var left = expressions.pop();
      expressions.push(new Expr.Or(left, right));
    }
    return null;
  }

  // andExpr: notExpr (AND notExpr)*
  @Override
  public Void visitAndExpr(FilterParser.AndExprContext ctx) {
    var operands = ctx.notExpr();
    visit(operands.getFirst());
    for (int i = 1; i < operands.size(); i++) {
      visit(operands.get(i));
      var right = expressions.pop();
      var left = expressions.pop();
      expressions.push(new Expr.And(left, right));
    }
    return null;
  }

  // notExpr: NOT notExpr | primary
  @Override
  public Void visitNotExpr(FilterParser.NotExprContext ctx) {
    if (ctx.NOT() != null) {
      visit(ctx.notExpr());
      expressions.push(new Expr.Not(expressions.pop()));
    } else {
      visit(ctx.primary());
    }
    return null;
  }

  // primary: comparison | '(' expr ')'
  @Override
  public Void visitPrimary(FilterParser.PrimaryContext ctx) {
    if (ctx.comparison() != null) {
      visit(ctx.comparison());
    } else {
      visit(ctx.expr());
    }
    return null;
  }

  // comparison
  //   : IDENTIFIER op=COMPOP value=literal
  //   | IDENTIFIER IN '(' literalList ')'
  @Override
  public Void visitComparison(FilterParser.ComparisonContext ctx) {
    var field = ctx.IDENTIFIER().getText();
    if (ctx.COMPOP() != null) {
      visit(ctx.literal());
      expressions.push(
          new Expr.Comparison(field, CompOp.fromSymbol(ctx.COMPOP().getText()), values.pop()));
    } else {
      visit(ctx.literalList());
      expressions.push(new Expr.InList(field, valueLists.pop()));
    }
    return null;
  }

  // literalList: literal (',' literal)*
  @Override
  public Void visitLiteralList(FilterParser.LiteralListContext ctx) {
    var list = new ArrayList<Value>();
    for (var literal : ctx.literal()) {
      visit(literal);
      list.add(values.pop());
    }
    valueLists.push(List.copyOf(list));
    return null;
  }

  // literal: STRING | NUMBER
  @Override
  public Void visitLiteral(FilterParser.LiteralContext ctx) {
    if (ctx.STRING() != null) {
      values.push(new Value.Str(readString(ctx.STRING().getText())));
    } else {
      values.push(new Value.Num(Integer.parseInt(ctx.NUMBER().getText())));
    }
    return null;
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
