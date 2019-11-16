package mdoc.interfaces;

import java.util.List;

public abstract class EvaluatedWorksheet {

  public abstract List<Diagnostic> diagnostics();
  public abstract List<EvaluatedWorksheetStatement> statements();

}
