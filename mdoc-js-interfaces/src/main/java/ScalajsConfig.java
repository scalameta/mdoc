package mdoc.js.interfaces;

import java.util.Map;

public class ScalajsConfig {
	public ModuleType moduleType;
	public boolean fullOpt;
	public boolean sourceMap;
	public boolean batchMode;
	public boolean closureCompiler;
	public Map<String, String> importMap;

	public ScalajsConfig() {
	}

	public ScalajsConfig withModuleKind(ModuleType kind) {
		if (kind == ModuleType.ESModule)
			this.moduleType = ModuleType.ESModule;
		else if (kind == ModuleType.NoModule)
			this.moduleType = ModuleType.NoModule;
		else if (kind == ModuleType.CommonJSModule)
			this.moduleType = ModuleType.CommonJSModule;
		return this;
	}

	public ScalajsConfig withImportMap(Map<String, String> importMap) {
		this.importMap = importMap;
		return this;
	}

	public ScalajsConfig withOptimized(boolean enabled) {
		this.fullOpt = enabled;
		return this;
	}

	public ScalajsConfig withSourceMap(boolean enabled) {
		this.sourceMap = enabled;
		return this;
	}

	public ScalajsConfig withBatchMode(boolean enabled) {
		this.batchMode = enabled;
		return this;
	}

	public ScalajsConfig withClosureCompiler(boolean enabled) {
		this.closureCompiler = enabled;
		return this;
	}
}
