package mdoc.interfaces;

public abstract class Diagnostic {

  public abstract RangePosition position();
  public abstract String message();
  public abstract DiagnosticSeverity severity();

}