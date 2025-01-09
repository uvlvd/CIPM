package cipm.consistency.commitintegration.lang.lua.detection;

import cipm.consistency.commitintegration.lang.detection.ComponentCandidates;
import cipm.consistency.commitintegration.lang.detection.ComponentState;
import cipm.consistency.commitintegration.lang.detection.strategy.ComponentDetectionStrategy;
import java.nio.file.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.xtext.lua.mocking.MockObjectCreator;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;

public class LuaStdLibComponentDetectionStrategy implements ComponentDetectionStrategy {
	private static final String LUA_STD_LIB_COMPONENT_NAME = "Lua Standard Library";

    @Override
    public void detectComponent(Resource res, Path projectRoot, ComponentCandidates candidates) {
        if (LuaGlobalScopeProvider.isImplicitResource(res)) {
            candidates.addModuleClassifier(ComponentState.REGULAR_COMPONENT, LUA_STD_LIB_COMPONENT_NAME, res);
        }
    }

}
