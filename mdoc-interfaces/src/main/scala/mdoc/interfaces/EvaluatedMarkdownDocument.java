package mdoc.interfaces;

import java.util.List;
import java.nio.file.Path;
import coursierapi.Dependency;
import coursierapi.Repository;

public abstract class EvaluatedMarkdownDocument {

	public abstract List<Diagnostic> diagnostics();

	public abstract String content();
}
