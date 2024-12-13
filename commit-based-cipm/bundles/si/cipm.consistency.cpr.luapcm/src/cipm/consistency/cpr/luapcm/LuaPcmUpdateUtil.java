package cipm.consistency.cpr.luapcm;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Exp;
import org.xtext.lua.lua.ExpField;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.Field;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.IndexExpField;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NameField;
import org.xtext.lua.lua.Return;

import cipm.consistency.cpr.luapcm.seffreconstruction.LuaFunctionCall;
import cipm.consistency.cpr.luapcm.seffreconstruction.LuaFunctionDeclaration;

public class LuaPcmUpdateUtil {
    /**
     * Returns the Lua code model function declaration EObject that is the parent of the given EObject
     * or null if no parent can be found.
     */
    public static EObject getParentFunctionDeclarationEObject(final EObject eObj) {
    	var functionDeclaration = buildParentFunctionDeclarationOf(eObj);
    	if (functionDeclaration != null) {
    		return functionDeclaration.getRoot();
    	}
    	return null;
    }
    
    public static LuaFunctionDeclaration buildParentFunctionDeclarationOf(final EObject eObj) {
    	
    	EObject parent = EcoreUtil2.getContainerOfType(eObj, FunctionDeclaration.class);
    	if (parent != null) {
    		return LuaFunctionDeclaration.of((FunctionDeclaration) parent);
    	}
    	parent = EcoreUtil2.getContainerOfType(eObj, LocalFunctionDeclaration.class);
    	if (parent != null) {
    		return LuaFunctionDeclaration.of((LocalFunctionDeclaration) parent);
    	}
    	parent = EcoreUtil2.getContainerOfType(eObj, ExpFunctionDeclaration.class);
    	if (parent != null) {
    		return LuaFunctionDeclaration.of((ExpFunctionDeclaration) parent);
    	}
    	return null;
    }
    
    /**
     * This is needed because "of" is a keyword in the Reactions DSL, thus the {@link LuaFuncitonDeclaration}'s {@code of}-methods cannot be called
     * from the {@code .reactions} files directly.
     * @param eObj
     * @return
     */
    public static LuaFunctionDeclaration buildFunctionDeclaration(final EObject eObj) {
    	if (eObj instanceof FunctionDeclaration decl) {
    		return LuaFunctionDeclaration.of(decl);
    	}
    	if (eObj instanceof LocalFunctionDeclaration decl) {
    		return LuaFunctionDeclaration.of(decl);
    	}
    	if (eObj instanceof ExpFunctionDeclaration decl) {
    		return LuaFunctionDeclaration.of(decl);
    	}
    	return null;
    }
    
    /**
     * This is needed because "of" is a keyword in the Reactions DSL, thus the {@link LuaFuncitonDeclaration}'s {@code of}-methods cannot be called
     * from the {@code .reactions} files directly.
     * @param eObj
     * @return
     */
    public static LuaFunctionCall buildFunctionCall(final EObject eObj) {
    	if (eObj instanceof FunctionCallStat call) {
    		return LuaFunctionCall.of(call);
    	}
    	if (eObj instanceof FunctionCall call) {
    		return LuaFunctionCall.of(call);
    	}
    	if (eObj instanceof MethodCall call) {
    		return LuaFunctionCall.of(call);
    	}
    	return null;
    }
    
    public static boolean hasReturnValues(final Block block) {
    	final var lastStat = block.getLastStat();
    	if (lastStat != null && lastStat instanceof Return returnStat) {
    		final var expList = returnStat.getExpList();
    		return expList != null;
    	}
    	return false;
    }
    
    public static ExpFunctionDeclaration getExpFunctionDeclarationFromField(final Field field) {
    	final var exp = field.getValueExp();
    	if (exp instanceof ExpFunctionDeclaration decl) {
    		return decl;
    	}
    	return null;
    }
    
    /**
     * Returns the EObject corresponding to the given Block's containing function declaration, i.e.
     * a {@link FunctionDeclaration}, {@link LocalFunctionDeclaration} or {@link ExpFunctionDeclaration} from the Lua code model.
     * @param block the block
     * @return the function declaration EObject from the Lua CM.
     */
    public static EObject getParentFunctiondeclarationAsEObject(final Block block) {
    	final var blockContainer = block.eContainer();
    	if (blockContainer instanceof FuncBody funcBody) {
    		final var bodyContainer = funcBody.eContainer();
    		if (!(bodyContainer instanceof FunctionDeclaration || bodyContainer instanceof LocalFunctionDeclaration || bodyContainer instanceof ExpFunctionDeclaration)) {
    			throw new RuntimeException("Expected EObject of type FunctionDeclaration, LocalFunctionDeclaration or ExpFunctionDeclaration, but got " + bodyContainer);
    		}
    		final var functionDeclaration = buildFunctionDeclaration(bodyContainer);
    		if (functionDeclaration != null) {
    			return functionDeclaration.getRoot();
    		}
    	}
    	return null;
    }

}
