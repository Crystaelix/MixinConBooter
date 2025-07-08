package com.crystaelix.mixinconbooter;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.MainAttributes;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.util.Constants;

import com.google.common.collect.Sets;

import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.IFMLCallHook;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class MixinConBooterCallHook implements IFMLCallHook {

	public static final Logger LOGGER = LogManager.getLogger("MixinConBooter");

	public static final String MIXIN_TWEAKER_CLASS = "org.spongepowered.asm.launch.MixinTweaker";
	public static final String CONTAINER_HANDLE_CLASS = "org.spongepowered.asm.launch.platform.container.IContainerHandle";
	public static final String LIBRARY_MANAGER_CLASS = "net.minecraftforge.fml.relauncher.libraries.LibraryManager";
	public static final String MOD_LIST_HELPER_CLASS = "net.minecraftforge.fml.relauncher.ModListHelper";

	static LaunchClassLoader classLoader;
	static File mcLocation;

	@Override
	public void injectData(Map<String, Object> data) {
		classLoader = (LaunchClassLoader)data.get("classLoader");
		mcLocation = (File)data.get("mcLocation");
	}

	@Override
	public Void call() {
		init();
		return null;
	}

	void init() {
		// Assume Mixin is initialized
		MixinPlatformManager platform = MixinBootstrap.getPlatform();
		Set<File> candidates = Sets.union(getClasspath(), getModCandidates());
		for(File candidate : candidates) {
			try {
				LOGGER.debug("Scanning file {} for manifest mixin configs", new Object[] {candidate});
				MainAttributes attr = MainAttributes.of(candidate);
				if(MIXIN_TWEAKER_CLASS.equals(attr.get(Constants.ManifestAttributes.TWEAKER))) {
					LOGGER.debug("Skipping as mixin tweaker specified in manifest");
					continue;
				}
				if(attr.get(Constants.ManifestAttributes.MIXINCONFIGS) != null ||
						attr.get(Constants.ManifestAttributes.TOKENPROVIDERS) != null ||
						attr.get(Constants.ManifestAttributes.MIXINCONNECTOR) != null) {
					LOGGER.debug("Mixin configs specified in manifest, adding as mixin container");
					platform.addContainer(new ContainerHandleURI(candidate.toURI()));
					classLoader.addURL(candidate.toURI().toURL());
				}
			}
			catch(Exception e) {
				LOGGER.warn("Unable to scan file", e);
			}
		}
	}

	public static Set<File> getClasspath() {
		Deque<URL> toProcess = new LinkedList<>();
		Collections.addAll(toProcess, classLoader.getURLs());
		Set<File> classpath = new LinkedHashSet<>();
		// Expand classpath in case running in VSCode works to some degree
		while(!toProcess.isEmpty()) {
			URL url = toProcess.poll();
			try {
				File file = new File(url.toURI().getPath());
				classpath.add(file);
				if(!file.exists()) {
					continue;
				}
				MainAttributes attr = MainAttributes.of(file);
				String cp = attr.get(Attributes.Name.CLASS_PATH);
				if(cp != null) {
					for(String path : cp.split(" ")) {
						URL cpUrl = new URL(url, path);
						if(url.getProtocol().equals("file")) {
							toProcess.add(cpUrl);
						}
					}
				}
			}
			catch(Exception e) {}
		}
		return classpath;
	}

	public static Set<File> getModCandidates() {
		return getLegacyModCandidates();
	}

	public static Set<File> getLegacyModCandidates() {
		Set<File> candidates = new LinkedHashSet<>();
		File modsDir = new File(mcLocation, "mods");
		File versionedModsDir = new File(modsDir, (String)FMLInjectionData.data()[4]);
		FilenameFilter filter = (dir, name)->name.endsWith(".jar");
		Collections.addAll(candidates, modsDir.listFiles(filter));
		if(versionedModsDir.isDirectory()) {
			Collections.addAll(candidates, versionedModsDir.listFiles(filter));
		}
		return candidates;
	}
}
