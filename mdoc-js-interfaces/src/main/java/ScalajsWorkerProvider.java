package mdoc.js.interfaces;

public interface ScalajsWorkerProvider {
	public ScalajsWorkerApi create(ScalajsConfig config, ScalajsLogger logger);
}
