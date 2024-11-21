package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.InternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.IfThenElse;
import org.xtext.lua.lua.Stat;

import cipm.consistency.cpr.luapcm.Config;
import cipm.consistency.cpr.luapcm.Config.ReconstructionType;
import tools.vitruv.change.correspondence.view.EditableCorrespondenceModelView;
import tools.vitruv.dsls.reactions.runtime.correspondence.ReactionsCorrespondence;

/**
 * This class contains the SEFF reconstruction for Lua AppSpace apps.
 * 
 * The class is called forward because the reconstruction works in a "forward" manner
 * 
 * We can not use tools.vitruv.applications.pcmjava.seffstatements.Code2SeffFactory as it is
 * specific to the JaMoPP meta-model.
 * 
 * @author Lukas Burgey
 *
 */
public final class ActionReconstruction {

    private static final Logger LOGGER = Logger.getLogger(ActionReconstruction.class.getName());

    private static final String LOOP_COUNT_SPECIFICATION = "10";

    private EditableCorrespondenceModelView<ReactionsCorrespondence> correspondenceModelView;

    // TODO: why does this create a new instance per statement? Because of the cmv?
    public static List<AbstractAction> getActionsForStatement(
    		final EObject statement,
            final EditableCorrespondenceModelView<ReactionsCorrespondence> cmv
            ) {
        var instance = new ActionReconstruction(cmv);
        return instance.reconstructActionsForStatement(statement);
    }
    
    private ActionReconstruction(final EditableCorrespondenceModelView<ReactionsCorrespondence> cmv) {
        this.correspondenceModelView = cmv;
    }

    private List<AbstractAction> reconstructActionsForStatement(EObject statement) {
    	if (statement instanceof Stat stat) {
    		if (LuaUtil.isControlFlowStatement(stat)) {
                var cfAction = reconstructControlFlowStatementToAction(stat);
                if (cfAction != null) {
                    return List.of(cfAction);
                }
            } else {
                // statements which contains function calls, but is NO control flow statement
               return reconstructFunctionCallStatementToActions(stat);
            }
    	}
        
        return Collections.emptyList();
    }
    
    private AbstractAction reconstructControlFlowStatementToAction(final Stat stat) {
        if (stat instanceof IfThenElse ifStatement) {
            return reconstructBranchAction(ifStatement);
        }  
//        else if (statement instanceof BlockWrapper blockWrappingStatement) {
//            return reconstructLoopAction(blockWrappingStatement);
//        }
        final var loopBlockOpt = LuaUtil.findBlockForLoopStat(stat);
        if (loopBlockOpt.isPresent()) {
        	var loopBlock = loopBlockOpt.get();
        	return reconstructLoopAction(loopBlock, stat);
        }

        LOGGER.error("Action reconstructon of control flow statement '" + stat + "' is not implemented");
        return null;
    }
    

    private AbstractAction reconstructBranchAction(final IfThenElse ifStatement) {

        // we only reconstruct the if statement as a branch if at least one brach was marked as
        // interesting
    	final var ifBlocks = LuaUtil.getBlocksFromIfStatement(ifStatement);
        var reconstructAsBranchAction = false;
        for (var block : ifBlocks) {
            if (needsActionReconstruction(block)) {
                reconstructAsBranchAction = true;
                break;
            }
        }

        if (reconstructAsBranchAction) {
            var branchAction = SeffFactory.eINSTANCE.createBranchAction();
            if (Config.descriptiveNames()) {
                branchAction.setEntityName("IF " + ifStatement.getCondition()
                    .eClass()
                    .getName());
            }
            return branchAction;
        }

        var action = SeffFactory.eINSTANCE.createInternalAction();
        if (Config.descriptiveNames()) {
            action.setEntityName("IF BLOCK " + ifStatement.getCondition()
                .eClass()
                .getName());
        }
        return action;
    }

    private AbstractAction reconstructLoopAction(final Block loopBlock, final Stat parentStat) {
        if (needsActionReconstruction(loopBlock)) {

            var loopAction = SeffFactory.eINSTANCE.createLoopAction();
            if (Config.descriptiveNames()) {
                loopAction.setEntityName(parentStat.toString());
            }

            var randomLoopCount = CoreFactory.eINSTANCE.createPCMRandomVariable();
            randomLoopCount.setSpecification(LOOP_COUNT_SPECIFICATION);
            loopAction.setIterationCount_LoopAction(randomLoopCount);

            return loopAction;
        }

        var action = SeffFactory.eINSTANCE.createInternalAction();
        if (Config.descriptiveNames()) {
            action.setEntityName("LOOP BLOCK " + parentStat.toString());
        }
        return action;
    }
    
    private List<AbstractAction> reconstructFunctionCallStatementToActions(final Stat statement) {
        List<AbstractAction> actions = new ArrayList<>();

        // all function calls in this statement (or the statement may be a call itself)
        final var functionCalls = LuaUtil.getFunctionCallsFromStat(statement)
        		.stream() // we can only reconstruct for non-mocked function calls
        		.filter(fc -> !fc.isMocked())
        		.toList();

        AbstractAction predecessor = null;
        AbstractAction action = null;
        for (var functionCall : functionCalls) {
            action = reconstructFunctionCallToAction(functionCall);
            if (action != null) {
                actions.add(action);

                ActionUtil.chainActions(predecessor, action);
                predecessor = action;
            }
        }

        return actions;
    }
    

//    private AbstractAction reconstructFunctionCallToAction(LuaFunctionCall call) {
//    	
//        if (call instanceof Expression_Functioncall_Direct directCall) {
//            return reconstructDirectFunctionCallToAction(directCall);
//        } else if (call instanceof Expression_Functioncall_Table tableCall) {
//            // currently all table calls are mapped to internal actions
//            LOGGER.trace("Call classification: Internal table call");
//            var action = SeffFactory.eINSTANCE.createInternalAction();
//            return action;
//        }
//
//        LOGGER.error("");
//        return null;
//    }
    
    // TODO: may remove this comment
    // comment jsaenz: previously called reconstructDirectFunctionCallToAction and called by reconstructFunctionCallToAction
    /**
     * 
     * @param functionCall: the function call, non-mocked.
     * @return
     */
    private AbstractAction reconstructFunctionCallToAction(final LuaFunctionCall functionCall) {

    	final var calledFunction = functionCall.getCalledFunction();

        // if we call another of our own seffs we use their step behaviour
        if (functionCall.isInternal()) {
            if (SeffHelper.needsSeffReconstruction(calledFunction)) {
            	// TODO: get fully qualified name for logging
                LOGGER.trace("Call classification: Internal call to SEFF " + calledFunction.getName());
                return reconstructInternalSeffCall(functionCall);
            } else {
            	// TODO: get fully qualified name for logging
                LOGGER.trace("Call classification: Internal call to non-SEFF " + calledFunction.getName());
                var action = SeffFactory.eINSTANCE.createInternalAction();
                if (Config.descriptiveNames()) {
                    action.setEntityName("CALL_TO_NON-SEFF_" + calledFunction.getName());
                }
                return action;
            }
        }

        // external or library call
//        var isCallToMockedFunction = calledComponent.getName()
//            .equals(LuaLinkingService.MOCK_URI.path());
//        if (isCallToMockedFunction) {
//            // library call
//            LOGGER.trace(String.format("Call classification: Library call %s ", calledFunction.getName()));
//            return reconstructExternalStdlibCrownCall(directCall);
//        }

        LOGGER.debug(String.format("Call classification: External call to SEFF %s ", calledFunction.getName()));
        return reconstructExternalSeffCall(functionCall);
    }
    
    /**
     * 
     * @param functionCall: the function call, non-mocked.
     * @return
     */
    private AbstractAction reconstructInternalSeffCall(final LuaFunctionCall functionCall) {
        final var calledFunction = functionCall.getCalledFunction();

        // we have two alternatives to modelling internal seff calls as internal actions:
        // Both are currently not working / fit our overall modelling
        switch (Config.getReconstructionTypeInternalSeffCall()) {
	        case ExternalCallAction:
	            return reconstructExternalSeffCall(functionCall);
	        case InternalCallAction:
	            return reconstructInternalSeffCallAsInternalCallAction(functionCall);
	        default:
        }
        
        // TODO: get fully qualified name for logging
        LOGGER.trace(String.format("Call classification: Internal call to non-SEFF %s ", calledFunction.getName()));
        var action = SeffFactory.eINSTANCE.createInternalAction();
        if (Config.descriptiveNames()) {
            action.setEntityName("CALL_TO_INTERNAL_NON_SEFF " + calledFunction.getName());
        }
        return action;
    }
    
    /*
     * TODO Using internal call actions to model calling SEFFs from the same component does not work
     * as we cannot reference the step behaviour of the called SEFF. We would have to duplicate it,
     * which would likely break the fine grained seff reconstruction.
     * 
     * TODO method does currently not work correctly
     * 
     * jsaenz comment: this method was only adapted to the new Lua CM, not fixed functionally (I am not sure what "method does currently not work correctly" implies)
     */
    /**
     * 
     * @param functionCall: the function call, non-mocked.
     * @return
     */
    private InternalCallAction reconstructInternalSeffCallAsInternalCallAction(final LuaFunctionCall functionCall) {
        final var calledFunction = functionCall.getCalledFunction();
        final var calledComponent = LuaUtil.getComponent(calledFunction);

        // internal call
        // TODO: get fully qualified name for logging
        LOGGER.trace(String.format("Call classification: Internal call to SEFF %s ", calledFunction.getName()));
        var internalCallAction = SeffFactory.eINSTANCE.createInternalCallAction();
        if (Config.descriptiveNames()) {
            internalCallAction.setEntityName("CALL_TO_INTERNAL_SEFF " + calledFunction.getName());
        }

        final var calledFunctionRootBlock = LuaUtil.getBlockFromCalledFunction(functionCall);
        
        final var rdSeff = CorrespondenceUtil.getCorrespondingEObjectByType(
			        			correspondenceModelView, 
			        			calledFunctionRootBlock,
			        			ResourceDemandingSEFF.class
		        			);
        
        if (rdSeff != null) {
            var internalBehaviour = SeffFactory.eINSTANCE.createResourceDemandingInternalBehaviour();
            internalBehaviour.setResourceDemandingSEFF_ResourceDemandingInternalBehaviour(rdSeff);
            internalCallAction.setCalledResourceDemandingInternalBehaviour(internalBehaviour);
        } else {
            // TODO implement this if needed
            LOGGER.warn("Cannot find rd seff for internal call");
        }

        ComponentSetInfoRegistry.getInfosForComponentSet(functionCall.getCallingFeature())
            .getDeclarationToCallingActions()
            .put(calledFunction, internalCallAction);

        LOGGER.trace("Adding correspondence for internal call " + internalCallAction.toString());
        correspondenceModelView.addCorrespondenceBetween(internalCallAction, calledComponent, null);
        return internalCallAction;
    }

//    private AbstractAction reconstructExternalStdlibCrownCall(Expression_Functioncall_Direct directCall) {
//        var calledFunction = (Statement_Function_Declaration) directCall.getCalledFunction();
//        var calledComponent = LuaUtil.getComponent(calledFunction);
//
//        var action = SeffFactory.eINSTANCE.createInternalAction();
//        if (Config.descriptiveNames()) {
//            action.setEntityName("CALL_TO_STDLIB/CROWN " + calledComponent.getName());
//        }
//        return action;
//    }
    /**
     * 
     * @param functionCall: the function call, non-mocked.
     * @return
     */
    private AbstractAction reconstructExternalSeffCall(final LuaFunctionCall functionCall) {
        var calledFunction = functionCall.getCalledFunction();
        var calledComponent = LuaUtil.getComponent(calledFunction);

        // external call
        var externalCallAction = SeffFactory.eINSTANCE.createExternalCallAction();
        if (Config.descriptiveNames()) {
            externalCallAction.setEntityName("CALL_TO_SEFF " + calledFunction.getName());
        }

        // determine the signature of the called function
        var calledSignature = CorrespondenceUtil.getCorrespondingEObjectByType(
					        		correspondenceModelView, 
					        		calledFunction.getRoot(),
					                OperationSignature.class
				              );
        if (calledSignature != null) {
            externalCallAction.setCalledService_ExternalService(calledSignature);
            correspondenceModelView.addCorrespondenceBetween(externalCallAction, calledSignature, null);
        } else {
            LOGGER.warn(calledFunction.getName() + ": Signature does not (yet) exist");
        }

        ComponentSetInfoRegistry.getInfosForComponentSet(functionCall.getCallingFeature())
            .getDeclarationToCallingActions()
            .put(calledFunction, externalCallAction);

        LOGGER.trace("Adding correspondence for external call " + externalCallAction.toString());
        correspondenceModelView.addCorrespondenceBetween(externalCallAction, calledComponent, null);

        return externalCallAction;
    }


    /**
     * Do we need to reconstruct actions for this object?
     */
    public static boolean needsActionReconstruction(EObject eObj) {
        if (eObj == null) {
            return false;
        }

        var infos = ComponentSetInfoRegistry.getInfosForComponentSet(eObj);
        if (infos == null) {
            // TODO is falling back to more reconstruction a good idea?
            return true;
        }
        return infos.needsActionReconstruction(eObj);
    }
    
    

    //public static Expression_Functioncall_Direct getServeCallForDeclaration(Statement_Function_Declaration eObj) {
    public static LuaFunctionCall getServeCallForDeclaration(LuaFunctionDeclaration declaration) {
        if (declaration != null) {
            var infos = ComponentSetInfoRegistry.getInfosForComponentSet(declaration.getRoot());
            if (infos != null) {
                return infos.getServeCallForDeclaration(declaration);
            }
        }

        return null;
    }





    
/*
    private static List<Expression_Functioncall> getFunctionCallsFromStatement(EObject statement) {
        if (statement instanceof Expression_Functioncall functionCall) {
            return List.of(functionCall);
        }
        return EcoreUtil2.getAllContentsOfType(statement, Expression_Functioncall.class);
    }
*/
    
    
    
    // TODO: this was not even used in the original code.... oh wait yes it was
    protected static boolean doesStatementContainArchitecturallyRelevantCall(
    		final Stat stat,
            final ComponentSetInfo info
            ) {
    	
    	final var calls = LuaUtil.getFunctionCallsFromStat(stat);
    	for (var call : calls) {
            if (isCallArchitecturallyRelevant(call, info)) {
            	// TODO: should probably print fully qualified name here...
            	// need to implement/get access to org.xtext.lua.linking.LuaQualifiedNameProvider to do that or
            	// implement some kind of utility method...
                LOGGER.debug("Scan found architecturally relevant function call to: " + call.getCalledFunction().getName());
                return true;
            }
        }
    	return false;
    	
//        var calls = getFunctionCallsFromStatement(statement);
//        for (var call : calls) {
//            if (isCallArchitecturallyRelevant(call, info)) {
//                if (call instanceof Expression_Functioncall_Direct directCall) {
//                    LOGGER
//                        .debug("Scan found architecturally relevant function call to: " + directCall.getCalledFunction()
//                            .getName());
//                }
//                return true;
//            }
//        }
//        return false;
        
    }

    private static boolean isCallArchitecturallyRelevant(final LuaFunctionCall functionCall, final ComponentSetInfo info) {
    	
    	
    	final var calledFunction = functionCall.getCalledFunction();
        final var calledFunctionHasSeff = info.needsSeffReconstruction(calledFunction);
        
        return !functionCall.isMocked()
                && (
                		functionCall.isExternal()
                        || (Config.getReconstructionTypeInternalSeffCall() == ReconstructionType.InternalCallAction
                                && functionCall.isInternal() 
                                && calledFunctionHasSeff)
                   );
        
//        if (call instanceof Expression_Functioncall_Direct directCall) {
//            var calledFunction = directCall.getCalledFunction();
//            var callingComponent = LuaUtil.getComponent(directCall);
//            var calledComponent = LuaUtil.getComponent(calledFunction);
//            
//            if (calledFunction == null || calledComponent == null) {
//                return false;
//            }
//
//            var isCallToFunction = calledFunction instanceof Statement_Function_Declaration;
//            var isCallToMockedFunction = calledComponent.getName()
//                .equals(LuaLinkingService.MOCK_URI.path());
//            var isInternalCall = calledComponent.equals(callingComponent);
//            var isExternalCall = !isInternalCall;
//
//            var calledFunctionHasSeff = info.needsSeffReconstruction(calledFunction);
//
//            return isCallToFunction && !isCallToMockedFunction
//                    && (isExternalCall
//                            || (Config.getReconstructionTypeInternalSeffCall() == ReconstructionType.InternalCallAction
//                                    && isInternalCall && calledFunctionHasSeff));
//        }
//
//        // currently table calls and others are never architecturally relevant
//        return false;
    }
    
}
