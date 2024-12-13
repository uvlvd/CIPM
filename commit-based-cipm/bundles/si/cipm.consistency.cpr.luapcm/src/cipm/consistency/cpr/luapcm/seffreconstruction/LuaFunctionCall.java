package cipm.consistency.cpr.luapcm.seffreconstruction;

import org.apache.log4j.Logger;
import org.eclipse.xtext.EcoreUtil2;

import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NamedFeature;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.utils.FeatureUtil;

public class LuaFunctionCall {
	private static final Logger LOGGER = Logger.getLogger(LuaFunctionCall.class);

	/**
	 * The name of the called function;
	 */
	private String name;
	/**
	 * The called function.
	 */
	private LuaFunctionDeclaration calledFunction;
	/**
	 * The feature calling the function;
	 */
	private Feature callingFeature;
	
	// set on init, contract: calledFunction = null <=> isMocked = true
	private boolean isMocked = false; 
	// set on first access
	private Boolean isExternal = null;
	
	private LuaFunctionCall() { }
	
	/**
	 * Returns the name of the <i>called</i> function.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the called function, or null if the reference to the called function {@link #isMocked()}.
	 */
	public LuaFunctionDeclaration getCalledFunction() {
		return calledFunction;
	}
	
	public Feature getCallingFeature() {
		return callingFeature;
	}

	/**
	 * Returns true if the reference to the called function is mocked, i.e. {@link #getCalledFunction()} returns null.
	 */
	public boolean isMocked() {
		return isMocked;
	}
	
	/**
	 * Returns true if this is a function call to an external function, i.e. the component
	 * containing the function call differs from the component containing the called function.</br>
	 * 
	 * This can only be determined if this {@link LuaFunctionCall} is not mocked, and will
	 * throw a {@link RuntimeException} otherwise.
	 */
	public boolean isExternal() {
		if (isExternal == null) {
			if (calledFunction == null) {
				throw new RuntimeException("Attempting to check if " + this + " is external, but calledFunction is null!");
			}
			// note: we are comparing optionals here
			final var callingComponent = LuaUtil.getComponent(this);
			final var calledComponent = LuaUtil.getComponent(calledFunction);
			isExternal = !(callingComponent.equals(calledComponent));
		}
		
		return isExternal;
	}
	
	public boolean isInternal() {
		return !isExternal();
	}

	public static LuaFunctionCall of(final FunctionCallStat functionCallStat) {
		var result = new LuaFunctionCall();
		
		final var featureRoot = (Feature) functionCallStat.getPrefix();
		final var featurePathLeaf = FeatureUtil.getFeaturePathLeaf(featureRoot);
		final var featurePathNamedLeafOpt = FeatureUtil.findFeaturePathNamedLeaf(featureRoot);
		if (featurePathNamedLeafOpt.isPresent()) {
			result.initFromNamedFeature(featurePathLeaf, featurePathNamedLeafOpt.get());
		}
		
		if (!result.validateConstruction()) {
			return null;
		}
		
		return result;
	}
	
	public static LuaFunctionCall of(final FunctionCall functionCall) {
		var result = new LuaFunctionCall();
		
		var parent = EcoreUtil2.getContainerOfType(functionCall, Feature.class);
		while (parent != null) {
			if (parent instanceof NamedFeature named) {
				result.initFromNamedFeature(functionCall, named);
				break;
			}
			parent = EcoreUtil2.getContainerOfType(parent, Feature.class);
		}
		
		if (!result.validateConstruction()) {
			return null;
		}
		
		return result;
	}
	
	public static LuaFunctionCall of(final MethodCall methodCall) {
		var result = new LuaFunctionCall();
		
		result.initFromNamedFeature(methodCall, methodCall);
		if (!result.validateConstruction()) {
			return null;
		}
		
		return result;
	}
	
	
	/**
	 * The Lua CM parser contains different kinds of features that form feature paths, e.g. the feature path </br>
	 *  {@code var.func()} </br>
	 * contains the Features {@code var}, {@code func} and {@code ()} (a function call). Thus, the {@link callingFeature} is not always the Feature
	 * referencing the called function, e.g. for a {@link FunctionCall} in a path like {@code func()} the Feature actually containing the reference to the called function
	 * is {@code func}, not {@code ()}. </br>
	 * This initializes the {@link LuaFunctionCall} based on the given named feature, which is</br>
	 * 	- the last named feature of the feature path for {@link FunctionCallStat}s</br>
	 * 	- the first named feature prefix for {@link FunctionCall} Features</br>
	 *  - the {@link MethodCall} for {@link MethodCall}s</br>
	 * 
	 * @param named the NamedFeature this functionCall calls (i.e. the feature referencing the called function).</br>
	 */
	private void initFromNamedFeature(Feature callingFeature, NamedFeature named) {
		this.callingFeature = callingFeature;
		this.name = named.getName();
		this.calledFunction = getCalledFunction(named);
		if (calledFunction == null) {
			this.isMocked = true;
		}
	}
	
	private boolean validateConstruction() {
		if (name == null) {
			LOGGER.error("Expected function call statment to contain a named feature leaf.");
			return false;
		}
		
		return true;
	}
	
	private LuaFunctionDeclaration getCalledFunction(NamedFeature named) {
		var ref = named.getRef();
		return LuaUtil.getReferencedFunction(ref, 0, 1000);
	}
	
	

}
