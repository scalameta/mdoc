package mdoc.js.api;

import java.nio.file.Path;

public interface ScalajsWorkerApi {

	public interface IRFile {
	}

	public interface OutputDirectory {
	}

	public OutputDirectory newFolder();

	public void cache(Path[] classPath);

	public java.util.Map<String, byte[]> link(IRFile[] in);
	public IRFile inMemory(String path, byte[] contents);

}
