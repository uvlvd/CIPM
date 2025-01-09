package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.component_extension.ComponentUtil;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.utils.FunctionUtil;
import org.xtext.lua.wrappers.LuaFunctionCall;

public class LuaFunctionCallUtil {
	
	private LuaFunctionCallUtil() { }

	public static boolean isInternal(LuaFunctionCall functionCall) {
		return !isExternal(functionCall);
	}

	/**
	 * Returns true if this is a function call to an external function, i.e. the component
	 * containing the function call differs from the component containing the called function.</br>
	 * 
	 * This can only be determined if this {@link LuaFunctionCall} is not mocked, and will
	 * throw a {@link RuntimeException} otherwise.
	 */
	public static boolean isExternal(LuaFunctionCall functionCall) {
		final var calledFunction = functionCall.getCalledFunction();
		if (calledFunction == null) {
			// TODO: if the calledFunction is null, we could not trace the function call to the respective function declaration.
			//  Although this does not necessarily mean that the functionCall is not external, we return false here as a workaround.
			return false;
		}
		// note: we are comparing optionals here
		final var callingComponent = ComponentUtil.getComponent(functionCall);
		final var calledComponent = ComponentUtil.getComponent(calledFunction);
		final var isExternal = !(callingComponent.equals(calledComponent));
		
		return isExternal;
	}
	

    /**
     * Returns all {@link LuaFunctionCall}s that can be extracted from the given statement.
     * @param stat the statment.
     * @return the {@link LuaFunctionCall}s.
     */
    public static List<LuaFunctionCall> getFunctionCallsFromStat(final Stat stat) {
    	var result = new ArrayList<LuaFunctionCall>();
    	// handle function call statements
    	if (stat instanceof FunctionCallStat functionCallStat) {
    		final var functionCall = LuaFunctionCall.of(functionCallStat);
    		if (functionCall != null) {
    			result.add(functionCall);
    		}
    	} else { // handle other statements containing function/method calls
	    	EcoreUtil2.getAllContentsOfType(stat, FunctionCall.class)
	    			.stream()
	    			.map(LuaFunctionCall::of)
	    			.filter(Objects::nonNull)
	    			.forEach(result::add);
	    	EcoreUtil2.getAllContentsOfType(stat, MethodCall.class)
	    			.stream()
					.map(LuaFunctionCall::of)
					.filter(Objects::nonNull)
					.forEach(result::add);
    	}
    	return result;
    } 

    public static Block getBlockFromCalledFunction(LuaFunctionCall functionCall) {
    	if (functionCall.getCalledFunction() == null) {
    		return null;
    	}
    	return functionCall.getCalledFunction().getBlock();
    }

    /**
     * Returns all non-mocked external function calls contained in the given object.
     */
    public static List<LuaFunctionCall> getExternalFunctionCallsContainedIn(final EObject root) {
    	return FunctionUtil.getFunctionCallsContainedIn(root)
    			.stream()
    			.filter(fc -> !fc.isMocked())
    			.filter(fc -> LuaFunctionCallUtil.isExternal(fc))
    			.toList();
    }
}
