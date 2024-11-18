package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.component_extension.Component;
import org.xtext.lua.linking.SyntheticVar;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.ElseIf;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.FunctionDeclaration;
import org.xtext.lua.lua.GenericFor;
import org.xtext.lua.lua.IfThenElse;
import org.xtext.lua.lua.LocalFunctionDeclaration;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NumericFor;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.RepeatLoop;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.WhileLoop;

public class LuaUtil {
	
	private LuaUtil() { }
    
    /**
     * Returns true if the given Referenceable is mocked. The Lua CM is not able to resolve all
     * references (e.g. because of the dynamic nature of some references) and will insert 
     * mocks for these references.
     */
    public static boolean isMocked(final Referenceable ref) {
    	// TODO: ensure that this functions correctly: SyntheticVar is only available via the org.xtext.lua package,
    	// not via the org.xtext.lua.component_extension, which should be used by the Lua CM parser... (?)
    	return ref instanceof SyntheticVar;
    }
    
	/**
	 * Returns a list containing all the Blocks found in the given IfThenElse, in the following order:</br>
	 * 1. The ThenBlock of the IfThenElse</br>
	 * 2. (Optional) all ElseIf Blocks</br>
	 * 3. (Optional) the Else Block.
	 */
    public static List<Block> getBlocksFromIfStatement(final IfThenElse ifStatement) {
        List<Block> blocks = new ArrayList<>();
        
        blocks.add(ifStatement.getThenBlock());
        blocks.add(ifStatement.getElseBlock());
        ifStatement.getElseIfs()
        			.stream()
        			.map(ElseIf::getBlock)
        			.forEach(blocks::add);
        					
        return blocks.stream().filter(Objects::nonNull).toList();

//        if (ifStatement.getBlock() != null) {
//            blocks.add(ifStatement.getBlock());
//        }
//        for (var elseIf : ifStatement.getElseIf()) {
//            blocks.add(elseIf.getBlock());
//        }
//        if (ifStatement.getElseBlock() != null) {
//            blocks.add(ifStatement.getElseBlock());
//        }
//        return blocks;
    }
    
    /**
     * Returns true if the given statement is a control-flow statement, i.e.
     * a conditional (if-then-(elseif)-else) or a loop statement.
     */
    public static boolean isControlFlowStatement(final Stat stat) {
        return stat instanceof IfThenElse 
        		|| isLoopStat(stat);
    }
    
    /**
     * Returns an Optional containing the Block for the given statement, if the statement is a loop-statement (see {@link #isLoopStat(Stat)}.
     */
    public static Optional<Block> findBlockForLoopStat(final Stat stat) {
    	if (isLoopStat(stat)) {
    		if (stat instanceof NumericFor numericFor) {
    			return Optional.of(numericFor.getBlock());
    		}
    		if (stat instanceof GenericFor genericFor) {
    			return Optional.of(genericFor.getBlock());
    		}
    		if (stat instanceof WhileLoop whileLoop) {
    			return Optional.of(whileLoop.getBlock());
    		}
    		if (stat instanceof RepeatLoop repeatLoop) {
    			return Optional.of(repeatLoop.getBlock());
    		}
    	}
    	return Optional.empty();
    }
    
    /**
     * Returns true if the given statement is a loop statement.
     */
    public static boolean isLoopStat(final Stat stat) {
        //return (eObj instanceof Statement_If_Then_Else || eObj instanceof Statement_For
        //        || eObj instanceof Statement_While || eObj instanceof Statement_Repeat);
        return stat instanceof NumericFor 
        		|| stat instanceof GenericFor
        		|| stat instanceof WhileLoop 
        		|| stat instanceof RepeatLoop;
    }
    

    
    public static Component getComponent(final LuaFunctionCall functionCall) {
    	return getComponent(functionCall.getCallingFeature());
    }
    
    public static Component getComponent(final LuaFunctionDeclaration functionDeclaration) {
    	return getComponent(functionDeclaration.getRoot());
    }
    
    public static Component getComponent(final EObject eObj) {
    	return EcoreUtil2.getContainerOfType(eObj, Component.class);
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
    		final var functionCall = LuaFunctionCall.from(functionCallStat);
    		if (functionCall != null) {
    			result.add(functionCall);
    		}
    	} else { // handle other statements containing function/method calls
	    	EcoreUtil2.getAllContentsOfType(stat, FunctionCall.class)
	    			.stream()
	    			.map(LuaFunctionCall::from)
	    			.filter(Objects::nonNull)
	    			.forEach(result::add);
	    	EcoreUtil2.getAllContentsOfType(stat, MethodCall.class)
	    			.stream()
					.map(LuaFunctionCall::from)
					.filter(Objects::nonNull)
					.forEach(result::add);
    	}
    	return result;
    } 
    
    /**
     * Returns all non-mocked external function calls contained in the given object.
     */
    public static List<LuaFunctionCall> getExternalFunctionCallsContainedIn(final EObject root) {
    	return getFunctionCallsContainedIn(root)
    			.stream()
    			.filter(fc -> !fc.isMocked())
    			.filter(LuaFunctionCall::isExternal)
    			.toList();
    }
    
    
    // TODO: create superclass for Lua FunctionCallStat,FunctionCall,MethodCall s.t. they can be handled
    // together.. either in org.xtext.lua (maybe in lua.xtext?) or in this package as a helper class
    public static List<LuaFunctionCall> getFunctionCallsContainedIn(final EObject root) {
    	var result = new ArrayList<LuaFunctionCall>();
    	EcoreUtil2.getAllContentsOfType(root, FunctionCallStat.class)
    		.stream()
    		.map(LuaFunctionCall::from)
    		.filter(Objects::nonNull)
    		.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, FunctionCall.class)
			.stream()
			.map(LuaFunctionCall::from)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, MethodCall.class)
			.stream()
			.map(LuaFunctionCall::from)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	return result;
    }

    // TODO: comment copied from getFunctionCallscontainedIn: create superclass for Lua FunctionCallStat,FunctionCall,MethodCall s.t. they can be handled
    // together.. either in org.xtext.lua (maybe in lua.xtext?) or in this package as a helper class 
    public static List<LuaFunctionDeclaration> getAllFunctionDeclarationsContainedIn(final EObject root) {
    	var result = new ArrayList<LuaFunctionDeclaration>();
    	EcoreUtil2.getAllContentsOfType(root, FunctionDeclaration.class)
    		.stream()
    		.map(LuaFunctionDeclaration::from)
    		.filter(Objects::nonNull)
    		.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, LocalFunctionDeclaration.class)
			.stream()
			.map(LuaFunctionDeclaration::from)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	EcoreUtil2.getAllContentsOfType(root, ExpFunctionDeclaration.class)
			.stream()
			.map(LuaFunctionDeclaration::from)
			.filter(Objects::nonNull)
			.forEach(result::add);
    	return result;
    }
    
    public static Block getBlockFromCalledFunction(LuaFunctionCall functionCall) {
    	if (functionCall.getCalledFunction() == null) {
    		return null;
    	}
    	return functionCall.getCalledFunction().getBlock();
    }
    

    


}
