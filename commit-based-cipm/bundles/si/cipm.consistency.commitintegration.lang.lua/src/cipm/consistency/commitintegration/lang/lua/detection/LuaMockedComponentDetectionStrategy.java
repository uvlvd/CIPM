package cipm.consistency.commitintegration.lang.lua.detection;

import cipm.consistency.commitintegration.lang.detection.ComponentCandidates;
import cipm.consistency.commitintegration.lang.detection.ComponentState;
import cipm.consistency.commitintegration.lang.detection.strategy.ComponentDetectionStrategy;
import java.nio.file.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.xtext.lua.mocking.MockObjectCreator;
import org.xtext.lua.scoping.LuaGlobalScopeProvider;

public class LuaMockedComponentDetectionStrategy implements ComponentDetectionStrategy {

    @Override
    public void detectComponent(Resource res, Path projectRoot, ComponentCandidates candidates) {
        if (res.getURI().equals(MockObjectCreator.VAR_MOCK_URI)) {
            candidates.addModuleClassifier(ComponentState.REGULAR_COMPONENT, "Mocked Objects", res);
        }
    }

}
