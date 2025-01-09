package cipm.consistency.commitintegration.lang.lua.detection;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

import org.eclipse.emf.ecore.resource.Resource;

import cipm.consistency.commitintegration.lang.detection.ComponentCandidates;
import cipm.consistency.commitintegration.lang.detection.ComponentState;
import cipm.consistency.commitintegration.lang.detection.strategy.ComponentDetectionStrategy;

public class DirectoryBasedDetectionStrategy implements ComponentDetectionStrategy {
	
	//TODO: should implement some kind of config class defining which files belong to a component
	//      and maybe also which componentes belong to a componentSet?
	private static Map<String, String[]> APISIX_COMPONENTS = Map.ofEntries(
			entry("Plugins", new String[] {"apisix/plugins/", "plugin.lua"}),
			entry("Plugin Runtime", new String[] {"*"}),
			entry("Core", new String[] {"core.lua", "apisix/core/"})
		);
	
	private static List<DirectoryComponentConfig> DIRECTORY_COMPONENT_CONFIGS = List.of(
			new DirectoryComponentConfig("Plugins", new String[] {"plugins"}, new String[] {"plugins.lua"}),
			new DirectoryComponentConfig("Plugin Runtime", new String[] {"apisix"}, new String[] {}),
			new DirectoryComponentConfig("Core", new String[] {"core"}, new String[] {"core.lua"})
			);
	
	
	public DirectoryBasedDetectionStrategy() {
		
	}
	
	@Override
	public void detectComponent(final Resource res, final Path projectRoot, final ComponentCandidates candidate) {
		// TODO Auto-generated method stub
		if (!res.getURI().isFile()) {
			return;
		}
        final var sourceFile = res.getURI().toFileString();
        final var sourceFilePath = Path.of(sourceFile).toAbsolutePath();
        
        // for all defined directory-component configs
        for (final var config : DIRECTORY_COMPONENT_CONFIGS) {
        	final var modName = config.getComponentName();
        	// we check if the current resource path ends with any file defined for the config
        	for (final var fileName : config.getFiles()) {
        		if (sourceFilePath.endsWith(fileName)) {
        			candidate.addModuleClassifier(ComponentState.REGULAR_COMPONENT, modName, res);
        			return;
        		}
        	}
        }

		// then we check if any parent of the resource path is part of a config dir
		var parent = sourceFilePath.getParent();
		// TODO: this works in the case of apisix because the root folder is apisix and therein is another
		//   folder called apisix, which contains the lua code. Will not work for other projects,
		//   since files in the root folder would not be assigned to any component
		while (projectRoot.compareTo(parent) != 0) {
			for (final var config : DIRECTORY_COMPONENT_CONFIGS) {
				final var modName = config.getComponentName();
				for (final var dirName : config.getDirs()) {
					if (parent.endsWith(dirName)) {
						candidate.addModuleClassifier(ComponentState.REGULAR_COMPONENT, modName, res);
						return;
					}
				}
			}
			parent = parent.getParent();
		}
	}

}
