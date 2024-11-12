package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.xtext.lua.component_extension.Application;
import org.xtext.lua.component_extension.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import cipm.consistency.commitintegration.lang.lua.appspace.AppSpaceSemantics;
import lua.Block;
import lua.FuncBody;
import lua.IfThenElse;
import lua.Referenceable;
import lua.Stat;

/**
 * This class contains information about a ComponentSet and its contents which is needed during SEFF
 * reconstruction
 * 
 * @author Lukas Burgey
 */
public class ComponentSetInfo {

    private static final Logger LOGGER = Logger.getLogger(ComponentSetInfo.class.getName());

    //TODO: juanj comment: this seems to map some kind of function name encoded in the last argument
    // of the functionCall to that function call (?)
    //private Map<String, Expression_Functioncall_Direct> functionNameToServeCall;
    
    /**
     * Mapping between function declarations and their external calls, i.e. calls from outside the 
     * function declaration's component.
     */
    // TODO: shouldn't this be able to hold multiple function calls? Seems plausible that 
    // multiple componenets inside a component set call the same function of one of the components...
    private Map<LuaFunctionDeclaration, LuaFunctionCall> functionToExternalFunctionCall;

    // we track which Statement_Function_Declaration are called in an external call action
    private ListMultimap<LuaFunctionDeclaration, AbstractAction> declarationToCallingActions;

    // map a component to components it depends upon (because it has external calls to it)
    private ListMultimap<Component, Component> componentToRequiredComponents;

    private Set<Block> blocksRequiringActionReconstruction;

    private Set<Stat> refreshedStatements;
    
    private boolean emulatedInstrumentationRan = false;
    

    /**
     * Initialize the component set info.
     * 
     * This causes some calculation (served function names).
     * 
     * @param application
     *            The component set for which this class will contain infos
     */
    public ComponentSetInfo(Application application) {
        LOGGER.debug("Initializing ComponentSetInfo for " + application.toString());

        declarationToCallingActions = ArrayListMultimap.create();
        componentToRequiredComponents = ArrayListMultimap.create();
        blocksRequiringActionReconstruction = new HashSet<>();
        refreshedStatements = new HashSet<>();

//        functionNameToServeCall = generateServedFunctionNamesFor(componentSet);
        functionToExternalFunctionCall = getFunctionToExternalFunctionCallMapFor(application);
        scanFunctionsForActionReconstruction(application);
    }

    /**
     * Creates and returns a map from function declarations to their external calls, 
     * i.e. calls from from outside the component containing the function declaration.
     */
    //TODO: comment juanj: previously: private static Map<String, Expression_Functioncall_Direct> generateServedFunctionNames(EObject root) {
    private Map<LuaFunctionDeclaration, LuaFunctionCall> getFunctionToExternalFunctionCallMapFor(EObject root) {
    	Map<LuaFunctionDeclaration, LuaFunctionCall> result = new HashMap<>();
    	
        final var functionCalls = LuaUtil.getExternalFunctionCallsContainedIn(root);
        for (var functionCall : functionCalls) {
        	result.put(functionCall.getCalledFunction(), functionCall);	
        }
        
        return result;
        
        // does this handle one component set?
		//  - "appspace apps" i think are component sets
		// In this function:
		//  - EcoreUtil2 finds all function calls in the given set
		//  - AppSpaceSemantics.isServingFunctionCall(functionCall) checks if the CALLED function is a served function (?)
		//  - the last parameter of the function call is the called function's name?
		// => so basically, it seems this function iterates over all calls in a component set and
		//    populates the map depending on some abstruse, non-documented criteria regarding the called function's
		//    name and argument count to find calls to external services. (?)
    	
//    	Map<String, Expression_Functioncall_Direct> servedNamesToServeCalls = new HashMap<>();
//
//        var functionCalls = EcoreUtil2.getAllContentsOfType(root, Expression_Functioncall_Direct.class);
//        for (var functionCall : functionCalls) {
//            if (!AppSpaceSemantics.isServingFunctionCall(functionCall)) {
//                continue;
//            }
//
//            var args = functionCall.getCalledFunctionArgs()
//                .getArguments();
//            if (args.size() == 2 || args.size() == 3) {
//                var nameIndex = args.size() - 1;
//                var funcName = args.get(nameIndex);
//                if (funcName instanceof Expression_String funcNameExpString) {
//                    servedNamesToServeCalls.put(funcNameExpString.getValue(), functionCall);
//                } else if (funcName instanceof Expression_VariableName funcNameExpVar) {
//                    servedNamesToServeCalls.put(funcNameExpVar.getRef()
//                        .getName(), functionCall);
//                } else {
//                    throw new IllegalStateException("Invalid Script.serveFunction call: Arguments are of invalid type");
//                }
//            } else {
//                throw new IllegalStateException("Invalid Script.serveFunction call: Must have 2 or 3 arguments");
//            }
//        }
//        return servedNamesToServeCalls;
    }

    /**
     * Determines if a function declaration requires SEFF reconstruction
     * 
     * 
     * @param functionDeclaration
     * @return boolean
     */
    public boolean needsSeffReconstruction(LuaFunctionDeclaration functionDeclaration) {
        return functionToExternalFunctionCall.containsKey(functionDeclaration);
    }

    /**
     * Returns the serve call that causes a function to be served
     * 
     * @param functionDeclaration
     * @return
     */
    // TODO: serve calls are specific to SICK appspace apps, this needs to be fixed for generic lua apps
    public LuaFunctionCall getServeCallForDeclaration(LuaFunctionDeclaration functionDeclaration) {
        return functionToExternalFunctionCall.get(functionDeclaration);
    }

    // TODO: maps should not be modified from outside of this class, implement something like
    // addActionForDeclaration(LuaFunctionDeclaration, Abstractaction) and use that...
    public ListMultimap<LuaFunctionDeclaration, AbstractAction> getDeclarationToCallingActions() {
        return declarationToCallingActions;
    }
    
    // TODO: same here, why is this direct access to the map provided?
    public ListMultimap<Component, Component> getComponentToRequiredComponents() {
        return componentToRequiredComponents;
    }

    /**
     * Determine if we need to reconstruct actions for a given eObject. This is only the case for The
     * content of a function declaration which needs a SEFF. In addition only external calls and
     * objects above them in the tree need action recovery.
     * 
     * @param eObj
     * @return
     */
    public boolean needsActionReconstruction(final EObject eObj) {
    	// handle if-then-else blocks
    	if (eObj instanceof IfThenElse ifThenElse) {
    		return LuaUtil.getBlocksFromIfStatement(ifThenElse)
    				.stream()
    				.anyMatch(blocksRequiringActionReconstruction::contains);
    	}
    	
    	// handle contents of function body
    	final var funcBodyOpt = findContainingFuncBody(eObj);
    	if (funcBodyOpt.isPresent()) {
    		final var funcBody = funcBodyOpt.get();
    		final var funcBlock = funcBody.getFuncBlock();
    		
    		 if (blocksRequiringActionReconstruction.contains(funcBlock)) {
    			 return true;
    	     }
    	}
    	return false;
    	
    	
    	
//        var parentDeclaration = EcoreUtil2.getContainerOfType(eObj, Statement_Function_Declaration.class);
//        if (parentDeclaration == null || !needsSeffReconstruction(parentDeclaration)) {
//            return false;
//        }

//        var parentBlock = EcoreUtil2.getContainerOfType(eObj, Block.class);
//        if (blocksRequiringActionReconstruction.contains(parentBlock)) {
//            return true;
//        }
//
//        if (eObj instanceof Statement_If_Then_Else ifStatement) {
//            for (var block : getBlocksFromIfStatement(ifStatement)) {
//                if (blocksRequiringActionReconstruction.contains(block)) {
//                    return true;
//                }
//            }
//        }
//        return false;
    }
    
    private Optional<FuncBody> findContainingFuncBody(final EObject eObj) {
    	final var funcBody = EcoreUtil2.getContainerOfType(eObj, FuncBody.class);
    	return Optional.of(funcBody);
    }

    // TODO move this to a utility class
//    public static List<Block> getBlocksFromIfStatement(Statement_If_Then_Else ifStatement) {
//        List<Block> blocks = new ArrayList<>();
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
//    }

    private void scanFunctionsForActionReconstruction(Application application) {
        var functionDecls = LuaUtil.getAllFunctionDeclarationsContainedIn(application);
        for (var functionDecl : functionDecls) {
            if (needsSeffReconstruction(functionDecl)) {
                scanSeffFunctionForActionReconstruction(functionDecl);
            }
        }
    }

    private void scanSeffFunctionForActionReconstruction(LuaFunctionDeclaration functionDeclaration) {
        /*
         * We always mark the root block of a seff function for reconstruction. The SEFF may only
         * contain an internal action in which case the marking algorithm will not catch the root
         * block.
         */
        final var block = functionDeclaration.getBlock();
        markBlockForActionReconstruction(block);


        /*
         * Mark architecturally relevant calls and all the blocks from them to the top level of
         * their containing declaration
         */
        var statements = EcoreUtil2.getAllContentsOfType(functionDeclaration.getRoot(), Stat.class);
        for (var statement : statements) {
            if (!needsActionReconstruction(statement)
                    && ActionReconstruction.doesStatementContainArchitecturallyRelevantCall(statement, this)) {
                // mark objects and its parent towards the declaration for action reconstruction
                LOGGER.debug("Scan found cause for action reconstruction: " + statement.toString());
                markObjectAndParentsForActionReconstruction(statement, functionDeclaration);
            }
        }
        
//        /*
//         * We always mark the root block of a seff function for reconstruction. The SEFF may only
//         * contain an internal action in which case the marking algorithm will not catch the root
//         * block.
//         */
//        var func = functionDeclaration.getFunction();
//        if (func != null) {
//            var rootBlock = func.getBlock();
//            if (rootBlock != null) {
//                markBlockForActionReconstruction(rootBlock);
//            }
//        }
//
//        /*
//         * Mark architecturally relevant calls and all the blocks from them to the top level of
//         * their containing declaration
//         */
//        var statements = EcoreUtil2.getAllContentsOfType(functionDeclaration, Statement.class);
//        for (var statement : statements) {
//            if (!needsActionReconstruction(statement)
//                    && ActionReconstruction.doesStatementContainArchitecturallyRelevantCall(statement, this)) {
//                // mark objects and its parent towards the declaration for action reconstruction
//                LOGGER.debug("Scan found cause for action reconstruction: " + statement.toString());
//                markObjectAndParentsForActionReconstruction(statement, functionDeclaration);
//            }
//        }
    }

    /*
     * Mark all e objects on the path from statement (inclusive) to root (exclusive) for action
     * reconstruction.
     * 
     * Also marks all other statements in blocks which are traversed.
     */
    private void markObjectAndParentsForActionReconstruction(Stat statement, LuaFunctionDeclaration functionDeclaration) {
        
    	EObject current = statement;
        do {
            if (current instanceof Block block) {
                markBlockForActionReconstruction(block);
            } else if (current instanceof IfThenElse ifStatement) {
                // also mark other child block when marking branch actions
                // mark other branches
                for (var block : LuaUtil.getBlocksFromIfStatement(ifStatement)) {
                    markBlockForActionReconstruction(block);
                }
            }

            // continue traversal
            current = current.eContainer();
        } while (!current.equals(functionDeclaration.getContainingStat()));
    }

    private void markBlockForActionReconstruction(Block block) {
        LOGGER.trace("Block marked for action reconstruction: " + block.toString());
        blocksRequiringActionReconstruction.add(block);
    }

    public boolean isEmulatedInstrumentationRan() {
        return emulatedInstrumentationRan;
    }

    public void setEmulatedInstrumentationRan(boolean emulatedInstrumentationRan) {
        this.emulatedInstrumentationRan = emulatedInstrumentationRan;
    }

    public Set<Stat> getRefreshedStatements() {
        return refreshedStatements;
    }
}
