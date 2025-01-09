package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.xtext.lua.component_extension.Component;
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
import org.xtext.lua.lua.Referencing;
import org.xtext.lua.lua.RepeatLoop;
import org.xtext.lua.lua.Stat;
import org.xtext.lua.lua.WhileLoop;
import org.xtext.lua.mocking.SyntheticVar;
import org.xtext.lua.wrappers.LuaFunctionCall;

public class LuaUtil {
	
	private LuaUtil() { }
    
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
    
}
