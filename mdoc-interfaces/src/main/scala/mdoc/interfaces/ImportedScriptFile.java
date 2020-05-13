package mdoc.interfaces;

import java.util.List;
import java.nio.file.Path;

public abstract class ImportedScriptFile {

  public abstract Path path();
  public abstract String packageName();
  public abstract String objectName();
  public abstract String instrumentedSource();
  public abstract String originalSource();
  public abstract List<ImportedScriptFile> files();

}
