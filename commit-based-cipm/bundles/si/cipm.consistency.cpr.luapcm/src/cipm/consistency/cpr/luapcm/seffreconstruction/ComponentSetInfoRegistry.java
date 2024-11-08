package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.component_extension.Application;

import cipm.consistency.commitintegration.lang.lua.runtimedata.ChangedResources;

/**
 * This singleton class is used to store and retrieve infos about component sets.
 * 
 * @author Lukas Burgey
 *
 */
public final class ComponentSetInfoRegistry {

    // singleton info map
    private static Map<Application, ComponentSetInfo> uriToInfos = new HashMap<>();
    
    public static ComponentSetInfo getInfosForComponentSet(Application application) {
        if (ChangedResources.getAndResetResourcesWereChanged()) {
            uriToInfos = new HashMap<>();
        }
        
        if (uriToInfos.containsKey(application)) {
            return uriToInfos.get(application);
        }

        var newInfos = new ComponentSetInfo(application);
        uriToInfos.put(application, newInfos);
        return newInfos;
    }

    public static ComponentSetInfo getInfosForComponentSet(EObject eObj) {
        var componentSet = EcoreUtil2.getContainerOfType(eObj, Application.class);
        if (componentSet == null) {
            return null;
        }
        return getInfosForComponentSet(componentSet);
    }
}
