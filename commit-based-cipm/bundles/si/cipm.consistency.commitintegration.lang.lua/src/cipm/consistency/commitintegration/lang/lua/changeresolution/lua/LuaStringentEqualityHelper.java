package cipm.consistency.commitintegration.lang.lua.changeresolution.lua;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

import com.google.common.cache.LoadingCache;

import lua.Assignment;

/**
 * A more stringent equality helper for the evaluation
 * The normal helper misses some differences intentionally, to facilitate the change resolution
 * using EMF compare.
 * 
 * @author Lukas Burgey
 *
 */
public class LuaStringentEqualityHelper extends LuaEqualityHelper {

    public LuaStringentEqualityHelper(LoadingCache<EObject, URI> uriCache) {
        super(uriCache);
    }

    @Override
    protected boolean match(Assignment left, Assignment right) {
    	return super.match(left, right)
    			&& matchEList(left.getVars(), right.getVars());
    }
}
