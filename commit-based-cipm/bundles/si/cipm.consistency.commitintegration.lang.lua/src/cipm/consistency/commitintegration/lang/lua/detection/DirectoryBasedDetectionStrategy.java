package cipm.consistency.commitintegration.lang.lua.detection;

import java.nio.file.Path;
import java.util.Map;
import static java.util.Map.entry;

import org.eclipse.emf.ecore.resource.Resource;

import cipm.consistency.commitintegration.lang.detection.ComponentCandidates;
import cipm.consistency.commitintegration.lang.detection.strategy.ComponentDetectionStrategy;

public class DirectoryBasedDetectionStrategy implements ComponentDetectionStrategy {
	
	//TODO: should implement some kind of config class defining which files belong to a component
	//      and maybe also which componentes belong to a componentSet?
	private static Map<String, String[]> APISIX_COMPONENTS = Map.ofEntries(
			entry("Core", new String[] {}),
			entry("Plugin Runtime", new String[] {}),
			entry("Plugins", new String[] {})
		);
	
	
	public DirectoryBasedDetectionStrategy() {
		
	}
	
	@Override
	public void detectComponent(Resource res, Path projectRoot, ComponentCandidates candidate) {
		// TODO Auto-generated method stub
	}

}
