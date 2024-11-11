package cipm.consistency.cpr.luapcm.seffreconstruction;

import org.apache.log4j.Logger;

import lua.Feature;
import lua.FunctionCall;
import lua.MemberAccess;
import lua.MethodCall;
import lua.Referenceable;
import lua.TableAccess;
import lua.Var;

//TODO
/**
 * ATTENTION! This class contains copies of the methods from {@link org.xtext.lua.utils.LinkingAndScopingUtils}.
 * Since we used ecore model inheritance for {@link org.xtext.lua.component_extension} package, the packages
 * from org.xtext.lua.lua.* (containing the xText-generated lua ecore classes) and org.xtext.lua.component_extension.lua
 * differ, which is why we cannot use the methods from {@link org.xtext.lua.utils.LinkingAndScopingUtils} here.</br>
 * @author juanj
 *
 */
public class LuaLinkingAndScopingUtils {
		private static final Logger LOGGER = Logger.getLogger(LuaLinkingAndScopingUtils.class);

		public static Feature getFeaturePathNamedLeaf(Feature feature) {
			return getFeaturePathNamedLeaf(feature, feature);
		}
		
		private static Feature getFeaturePathNamedLeaf(final Feature feature, Feature lastMatch) {
			if (feature instanceof Referenceable referenceable) { // has name
				lastMatch = feature;
			}
			
			final var suffix = getSuffixExpFromFeature(feature);
			if (suffix == null) {
				return lastMatch; // might be null
			}

			return getFeaturePathNamedLeaf(suffix, lastMatch);

		}
		
		public static Feature getFeaturePathLeaf(Feature feature) {
			final var suffix = getSuffixExpFromFeature(feature);
			if (suffix == null) {
				return feature;
			}
			return getFeaturePathLeaf(suffix);
		}
		
		private static Feature getSuffixExpFromFeature(Feature feature) {
			if (feature instanceof Var var) {
				return (Feature) var.getSuffixExp();
			} else if (feature instanceof MemberAccess ma) {
				return (Feature) ma.getSuffixExp();
			} else if (feature instanceof TableAccess ta) {
				return (Feature) ta.getSuffixExp();
			} else if (feature instanceof FunctionCall fc) {
				return (Feature) fc.getSuffixExp();
			} else if (feature instanceof MethodCall mc) {
				return (Feature) mc.getSuffixExp();
			}
			LOGGER.error("Cannot find Suffix for feature" + feature);
			return null;
		}
}
