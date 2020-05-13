package mdoc.interfaces;

import java.util.List;
import java.nio.file.Path;
import java.io.PrintStream;

public abstract class Mdoc {

  public abstract EvaluatedWorksheet evaluateWorksheet(String filename, String text);
  public abstract Mdoc withWorkingDirectory(Path workingDirectory);
  public abstract Mdoc withClasspath(List<Path> classpath);
  public abstract Mdoc withScalacOptions(List<String> options);
  public abstract Mdoc withSettings(List<String> settings);
  public abstract Mdoc withConsoleReporter(PrintStream out);
  public abstract Mdoc withScreenHeight(int screenHeight);
  public abstract Mdoc withScreenWidth(int screenWidth);
  public abstract Mdoc withCoursierLogger(coursierapi.Logger logger);
  public abstract void shutdown();

}
