package filter.ast;

import filter.ast.builder.AstBuilderPattern;
import filter.ast.builder.AstBuilderVisitor;
import filter.ast.builder.AstBuilders;
import filter.ast.printer.AstPrinter;
import java.util.List;
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

public class ApprovalTest {

  @Test
  void approvesRepresentativeQueries() {
    var queries =
        List.of(
            "artist == \"Beatles\"",
            "year <= 1990",
            "genre in (\"rock\", \"jazz\")",
            "artist == \"Beatles\" and year == 1965",
            "genre in (\"rock\", \"jazz\") or year <= 1990 and not artist == \"Beatles\"",
            "(genre == \"rock\" or genre == \"jazz\") and year >= 1970");

    var result = new StringBuilder();
    for (var query : queries) {
      var pattern = AstBuilders.fromQuery(query, new AstBuilderPattern()::translate);
      var visitor = AstBuilders.fromQuery(query, new AstBuilderVisitor()::translate);

      result.append("Query: ").append(query).append('\n');
      result.append("Pattern: ").append(AstPrinter.toString(pattern)).append('\n');
      result.append("Visitor: ").append(AstPrinter.toString(visitor)).append("\n\n");
    }

    Approvals.verify(result.toString().strip());
  }
}
