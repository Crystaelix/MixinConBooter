package com.crystaelix.mixinconbooter;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("MixinConBooter")
@IFMLLoadingPlugin.SortingIndex(MixinConBooterLoadingPlugin.SORT_ORDER)
public class MixinConBooterLoadingPlugin implements IFMLLoadingPlugin {

	// Need to be before Mixin tweaker, usually has sort index 0
	public static final int SORT_ORDER = Short.MIN_VALUE;
	public static final String CALL_HOOK_CLASS = "com.crystaelix.mixinconbooter.MixinConBooterCallHook";
	public static final String MIXIN_BOOTSTRAP_CLASS = "org.spongepowered.asm.launch.MixinBootstrap";

	@Override
	public String[] getASMTransformerClass() {
		return new String[0];
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {}

	@Override
	public String getSetupClass() {
		try {
			Class.forName(MIXIN_BOOTSTRAP_CLASS);
		}
		catch(Exception e) {
			throw new RuntimeException("Mixin is not present, please install a Mixin loader", e);
		}
		return CALL_HOOK_CLASS;
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
