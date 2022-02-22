package mdoc.js.api;

public class ScalajsConfig {
	public ModuleType moduleType = ModuleType.NoModule;
	public boolean fullOpt;
	public boolean sourceMap;
	public boolean batchMode;
	public boolean closureCompiler;

	public ScalajsConfig() {
	}

	

	public ScalajsConfig withESModule() {
		this.moduleType = ModuleType.ESModule;
		return this;
	}
	
	public ScalajsConfig withModuleKind(ModuleType kind) {
		if(kind == ModuleType.ESModule)
			this.moduleType = ModuleType.ESModule;
		else if (kind == ModuleType.NoModule)
			this.moduleType = ModuleType.NoModule;
		else if (kind == ModuleType.CommonJSModule)
			this.moduleType = ModuleType.CommonJSModule;
		return this;
	}

	public ScalajsConfig withNoModule() {
		this.moduleType = ModuleType.NoModule;
		return this;
	}

	public ScalajsConfig withCommonJSModule() {
		this.moduleType = ModuleType.CommonJSModule;
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
