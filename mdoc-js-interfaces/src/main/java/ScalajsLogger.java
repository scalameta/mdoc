package mdoc.js.interfaces;

public interface ScalajsLogger{
  public void log(LogLevel level, String message);
  public void trace(Throwable ex);
}
