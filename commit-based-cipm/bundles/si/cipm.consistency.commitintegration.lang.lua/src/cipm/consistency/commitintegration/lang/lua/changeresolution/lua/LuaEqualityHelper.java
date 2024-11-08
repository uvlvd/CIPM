package cipm.consistency.commitintegration.lang.lua.changeresolution.lua;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.utils.EqualityHelper;
import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.lua.Block;

import lua.Args;
import lua.Assignment;
import lua.Chunk;
import lua.ElseIf;
import lua.ExpAdd;
import lua.ExpAnd;
import lua.ExpConcat;
import lua.ExpDiv;
import lua.ExpEq;
import lua.ExpExponentiation;
import lua.ExpFalse;
import lua.ExpGeq;
import lua.ExpGt;
import lua.ExpInvert;
import lua.ExpLength;
import lua.ExpLeq;
import lua.ExpLt;
import lua.ExpMod;
import lua.ExpMult;
import lua.ExpNegate;
import lua.ExpNeq;
import lua.ExpNil;
import lua.ExpNumberLiteral;
import lua.ExpOr;
import lua.ExpStringLiteral;
import lua.ExpSub;
import lua.ExpTrue;
import lua.ExpVarArgs;
import lua.Feature;
import lua.FuncBody;
import lua.FunctionCall;
import lua.GenericFor;
import lua.IfThenElse;
import lua.LiteralStringArg;
import lua.MemberAccess;
import lua.MethodCall;
import lua.NumericFor;
import lua.ParList;
import lua.ParamArgs;
import lua.Referenceable;
import lua.Return;
import lua.TableAccess;
import lua.TableConstructor;
import lua.Var;
import lua.WhileLoop;

import org.xtext.lua.component_extension.Component;
import org.xtext.lua.component_extension.NamedChunk;
import org.xtext.lua.component_extension.Application;
/*
import org.xtext.lua.lua.Expression_And;
import org.xtext.lua.lua.Expression_Concatenation;
import org.xtext.lua.lua.Expression_Division;
import org.xtext.lua.lua.Expression_Equal;
import org.xtext.lua.lua.Expression_Exponentiation;
import org.xtext.lua.lua.Expression_False;
import org.xtext.lua.lua.Expression_Functioncall;
import org.xtext.lua.lua.Expression_Functioncall_Direct;
import org.xtext.lua.lua.Expression_Functioncall_Table;
import org.xtext.lua.lua.Expression_Import;
import org.xtext.lua.lua.Expression_Invert;
import org.xtext.lua.lua.Expression_Larger;
import org.xtext.lua.lua.Expression_Larger_Equal;
import org.xtext.lua.lua.Expression_Length;
import org.xtext.lua.lua.Expression_Minus;
import org.xtext.lua.lua.Expression_Modulo;
import org.xtext.lua.lua.Expression_Multiplication;
import org.xtext.lua.lua.Expression_Negate;
import org.xtext.lua.lua.Expression_Nil;
import org.xtext.lua.lua.Expression_Not_Equal;
import org.xtext.lua.lua.Expression_Number;
import org.xtext.lua.lua.Expression_Or;
import org.xtext.lua.lua.Expression_Plus;
import org.xtext.lua.lua.Expression_Smaller;
import org.xtext.lua.lua.Expression_Smaller_Equal;
import org.xtext.lua.lua.Expression_String;
import org.xtext.lua.lua.Expression_TableAccess;
import org.xtext.lua.lua.Expression_TableConstructor;
import org.xtext.lua.lua.Expression_True;
import org.xtext.lua.lua.Expression_VarArgs;
import org.xtext.lua.lua.Expression_VariableName;
import org.xtext.lua.lua.Field_AppendEntryToTable;
import org.xtext.lua.lua.Function;
import org.xtext.lua.lua.Functioncall_Arguments;
import org.xtext.lua.lua.LastStatement_Return;
import org.xtext.lua.lua.LastStatement_Return_WithValue;
import org.xtext.lua.lua.NamedChunk;
import org.xtext.lua.lua.Refble;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.Statement_Assignment;
import org.xtext.lua.lua.Statement_For;
import org.xtext.lua.lua.Statement_If_Then_Else;
import org.xtext.lua.lua.Statement_If_Then_Else_ElseIfPart;
import org.xtext.lua.lua.Statement_While;
*/
import com.google.common.cache.LoadingCache;

/**
 * This class defines how the constructs of the Lua code model should be matched (which code model objects are "equal")
 * for the comparison of multiple Lua code models comparison via emfcompare. </br></br>
 * 
 * The {@link #LuaHierarchicalMatchEngineFactory} implements hierarchical matching, i.e. the
 * left and right EObjects matched by this EqualityHelper class are assumed to always be on the same level
 * in the "trees" of the compared models.
 * @author juanj
 *
 */
public class LuaEqualityHelper extends EqualityHelper {
	private static final Logger LOGGER = Logger.getLogger(LuaEqualityHelper.class); 

    /**
     * Constructor to initialize the required cache.
     *
     * @param uriCache
     *            The cache to use during the equality checks.
     */
    public LuaEqualityHelper(LoadingCache<EObject, org.eclipse.emf.common.util.URI> uriCache) {
        super(uriCache);
    }
    
    @Override
    protected boolean matchingEObjects(EObject left, EObject right) {
    	// TODO: may be better to extract null-check here, instead of performing it in every match(..) function
        return match(left, right);
    }
    
    /**
     * Do objects match based on lua attributes, like name
     * 
     * @param left
     * @param right
     * @return True if matching, false if not
     */
    public boolean match(EObject left, EObject right) {
        if (left == null || right == null) {
            return false;
        }

        if (!eClassMatch(left, right)) {
            return false;
        }
        
        // this "switch" is ugly, but i have no time to make it nice :/
        // terminals
        if (bothInstanceOfAny(left, right, List.of(ExpNil.class, ExpTrue.class, ExpFalse.class, ExpVarArgs.class))) {
        	 return true;
        } 
        
        // unary expressions
        else if (left instanceof ExpLength l && right instanceof ExpLength r) {
            return match(l.getExp(), r.getExp());
        } else if (left instanceof ExpInvert l && right instanceof ExpInvert r) {
            return match(l.getExp(), r.getExp());
        } else if (left instanceof ExpNegate l && right instanceof ExpNegate r) {
            return match(l.getExp(), r.getExp());
        } else if (left instanceof ExpNumberLiteral l && right instanceof ExpNumberLiteral r) {
            return l.getValue() == r.getValue();
        } else if (left instanceof ExpStringLiteral l && right instanceof ExpStringLiteral r) {
            return l.getValue().equals(r.getValue());
        // binary expressions
        } else if (left instanceof ExpOr l && right instanceof ExpOr r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpAnd l && right instanceof ExpAnd r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpGt l && right instanceof ExpGt r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpGeq l && right instanceof ExpGeq r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpLt l && right instanceof ExpLt r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpLeq l && right instanceof ExpLeq r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpEq l && right instanceof ExpEq r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpNeq l && right instanceof ExpNeq r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpConcat l && right instanceof ExpConcat r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpAdd l && right instanceof ExpAdd r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpSub l && right instanceof ExpSub r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpMult l && right instanceof ExpMult r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpDiv l && right instanceof ExpDiv r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpExponentiation l && right instanceof ExpExponentiation r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof ExpMod l && right instanceof ExpMod r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        }
        
        // other expressions
        else if (left instanceof TableConstructor l && right instanceof TableConstructor r) {
        	return match(l, r);
        } else if (left instanceof Feature l && right instanceof Feature r) {
        	return match(l, r);
        } 
        
        // remaining statments etc.
        else if (left instanceof Assignment l && right instanceof Assignment r) {
            return match(l, r);
        } else if (left instanceof IfThenElse l && right instanceof IfThenElse r) {
            return match(l, r);
        } else if (left instanceof ElseIf l && right instanceof ElseIf r) {
            return match(l, r);
        } else if (left instanceof WhileLoop l && right instanceof WhileLoop r) {
        	return match(l, r);
        } else if (left instanceof NumericFor l && right instanceof NumericFor r) {
        	return match(l, r);
        } else if (left instanceof GenericFor l && right instanceof GenericFor r) {
        	return match(l, r);
        }
        // TODO: I think this is not needed with the 'new' Lua CM, since the IndexExp
        //else if (left instanceof Field_AppendEntryToTable l && right instanceof Field_AppendEntryToTable r) {
        //    return match(l, r);
        //}
        else if (left instanceof FuncBody l && right instanceof FuncBody r) {
            return match(l, r);
        } else if (left instanceof Return l && right instanceof Return r) {
            return match(l, r);
        }
        // we handle Referenceables late s.t. the subclasses that need to be handled explicitly are already handled (e.g. NamedFeatures).
        else if (left instanceof Referenceable l && right instanceof Referenceable r) {
            return matchReferenceablesByName(l, r);
        }
        
        // meta model stuff
        // we say these match because of the hierarchical matching taking place
        else if (bothInstanceOfAny(left, right, List.of(Block.class, Chunk.class, Application.class))) {
        	return true;
        }
        // these have names:
        else if (left instanceof NamedChunk l && right instanceof NamedChunk r) {
            return l.getName().equals(r.getName());
        } else if (left instanceof Component l && right instanceof Component r) {
            return l.getName().equals(r.getName());
        }
        
        // old
        /*
        else if (left instanceof Functioncall_Arguments l && right instanceof Functioncall_Arguments r) {
            return matchEList(l.getArguments(), r.getArguments());
        } else if (left instanceof Function l && right instanceof Function r) {
            return matchEList(l.getArguments(), r.getArguments());
        } else if (left instanceof LastStatement_Return_WithValue l && right instanceof LastStatement_Return_WithValue r) {
            return matchEList(l.getReturnValues(), r.getReturnValues());
        } 
        else if (left instanceof Refble l && right instanceof Refble r) {
            return match(l, r);
            
        // meta model stuff
        // we say these match because of the hierarchical matching taking place
        } else if (left instanceof Block l && right instanceof Block r) {
            return true;
        } else if (left instanceof Chunk l && right instanceof Chunk r) {
            return true;
        } else if (left instanceof ComponentSet l && right instanceof ComponentSet r) {
            return true;
        } else if (left instanceof LastStatement_Return l && right instanceof LastStatement_Return r) {
            return true;
          
        // these have names:
        } else if (left instanceof NamedChunk l && right instanceof NamedChunk r) {
            return l.getName().equals(r.getName());
        } else if (left instanceof Component l && right instanceof Component r) {
            return l.getName().equals(r.getName());
        }

        // no decision from the custom matchers -> use fallback
        // TODO reenable
//        return editionDistanceEqualityChecker.match(left, right);
 
         */
        return false;
    }

    protected boolean match(TableConstructor left, TableConstructor right) {
    	if (left.getFieldList() == null || right.getFieldList() == null) {
    		return false;
    	}
        return matchEList(left.getFieldList().getFields(), right.getFieldList().getFields());
    }
    
    protected boolean match(Feature left, Feature right) {
    	if (left == null || right == null) {
    		return false;
    	}
    	//TODO: do we need to match "require" function as a special case?

    	
    	// hierarchical nature of the MatchEngine processes the 
    	// previous/following features in a feature path, i.e. we don't need to compare the
    	// path prefixes here.
    	
    	// TODO: use lukas' scoping stuff for named things?
    	
    	if (left instanceof Var l && right instanceof Var r) {
    		return match(l, r);
    	} else if (left instanceof FunctionCall l && right instanceof FunctionCall r) {
    		return match(l, r);
    	} else if (left instanceof MethodCall l && right instanceof MethodCall r) {
    		return match(l, r);
    	} else if (left instanceof TableAccess l && right instanceof TableAccess r) {
    		return match(l, r);
    	} else if (left instanceof MemberAccess l && right instanceof MemberAccess r) {
    		return match(l, r);
    	}
    	// fallback, should not be reached since all possible options should be exhausted at this point
    	// (i.e. classes are equal and all "Args-types" have a conditional)
    	logMatchingReachedFalltrhoughForMessage(Args.class);
    	//TODO: should we use editing distance here as fallback?
    	return false;
    }
    
    protected boolean match(Var left, Var right) {
    	return refblesMatchByScope(left, right);
    }
    
    protected boolean match(FunctionCall left, FunctionCall right) {
    	return left != null && right != null // TODO: test if null-check is needed
    			&& match(left.getArgs(), right.getArgs());
    }
    
    protected boolean match(MethodCall left, MethodCall right) {
    	return refblesMatchByScope(left, right)
    			&& match(left.getArgs(), right.getArgs());
    }
    
    protected boolean match(TableAccess left, TableAccess right) {
    	return match(left.getIndexExp(), right.getIndexExp())
    			&& refblesMatchByScope(left, right);
    }
    
    protected boolean match(MemberAccess left, MemberAccess right) {
    	return refblesMatchByScope(left, right);
    }


    protected boolean match(Assignment left, Assignment right) {
    	var lExpList = left.getExpList();
    	var rExpList = right.getExpList();
    	if (lExpList == null || rExpList == null) {
    		return false;
    	}
    	
    	var lExps = lExpList.getExps();
    	var rExps = rExpList.getExps();

    	return matchEList(lExps, rExps);
    }
    
    protected boolean match(IfThenElse left, IfThenElse right) {
        if (!match(left.getCondition(), right.getCondition())) {
            return false;
        }
        if (!matchEList(left.getElseIfs(), right.getElseIfs())) {
            return false;
        }
        if ((left.getElseBlock() != null && right.getElseBlock() == null)
                || (left.getElseBlock() == null && right.getElseBlock() != null)) {
            return false;
        }
        // TODO remove if this does not help:
        // seems to help but not perfectly
        // TODO: comment by juanj: why not match the stats? seems to have been tried before with !match(left.getBlock(), right.getBlock())...
        if (!matchEListByClass(left.getThenBlock().getStats(), right.getThenBlock().getStats())) {
            return false;
        }
//        if (!match(left.getBlock(), right.getBlock())) {
//            return false;
//        }
        return true;
    }

    protected boolean match(ElseIf left, ElseIf right) {
        // TODO is this enough?
    	// TODO: comment by juanj: it should be, assuming that only ElseIfs contained in the same IfThenElse are
    	// compared (hierarchical matching) and that an ElseIf with the same condition as another in the same IfThenElse
    	// seems illogical.
        return match(left.getCondition(), right.getCondition());
    }
    
    protected boolean match(WhileLoop left, WhileLoop right) {
        return match(left.getCondition(), right.getCondition());
    }

    protected boolean match(NumericFor left, NumericFor right) {
    	return left.getArg().equals(right.getArg())
    			&& match(left.getFromExp(), right.getFromExp())
    			&& match(left.getToExp(), right.getToExp())
    			&& match(left.getStepExp(), right.getStepExp());
    }
    
    protected boolean match(GenericFor left, GenericFor right) {
        return matchEList(left.getArgList().getArgs(), right.getArgList().getArgs())
        		&& matchEList(left.getExpList().getExps(), right.getExpList().getExps());
    }
    
    protected boolean match(FuncBody left, FuncBody right) {
        return match(left.getParList(), right.getParList());
    }
    
    protected boolean match(ParList left, ParList right) {
        return (left instanceof ExpVarArgs && right instanceof ExpVarArgs)
        		|| matchEList(left.getArgsList().getArgs(), right.getArgsList().getArgs());
    }
    
    protected boolean match(Return left, Return right) {
        return (left.getExpList() == null && right.getExpList() == null) // both are return statements without expressions
        		|| matchEList(left.getExpList().getExps(), right.getExpList().getExps());
    }
    
    protected boolean match(Args left, Args right) {
    	if (left == null || right == null) {
    		return false;
    	}
    	
    	if (!eClassMatch(left, right)) {
    		return false;
    	}
    	
    	if (left instanceof ParamArgs l && right instanceof ParamArgs r) {
    		var lArgs = l.getParams().getExps();
    		var rArgs = r.getParams().getExps();
    		return matchEList(lArgs, rArgs);
    	} else if (bothInstanceOf(left, right, TableConstructor.class)) {
    		match(left, right);
    	} else if (left instanceof LiteralStringArg l && right instanceof LiteralStringArg r) {
    		return l.getStr().equals(r.getStr());
    	}
    	
    	// fallback, should not be reached since all possible options should be exhausted at this point
    	// (i.e. classes are equal and all "Args-types" have a conditional)
    	logMatchingReachedFalltrhoughForMessage(Args.class);
    	//TODO: should we use editing distance here as fallback?
    	return false;
    }
    
    protected boolean refblesMatchByScope(Referenceable left, Referenceable right) {
        if (left == null || right == null) {
            return false;
        }

        if (!left.getName().equals(right.getName())) {
            return false;
        }

        var lParent = left.eContainer();
        var rParent = right.eContainer();
        while (lParent != null && rParent != null) {
            if (!eClassMatch(lParent, rParent)) {
                return false;
            }
            if (lParent instanceof Assignment && rParent instanceof Assignment) {
                var lExpList = ((Assignment) lParent).getExpList();
                var rExpList = ((Assignment) rParent).getExpList();
                if (lExpList != null && rExpList != null) {
                	var lExps = lExpList.getExps();
                	var rExps = rExpList.getExps();
                	if (lExps != null && rExps != null) {
                		if (lExps.size() != rExps.size()) {
                			return false;
                		}
                	}
                }
            }
            lParent = lParent.eContainer();
            rParent = rParent.eContainer();
        }

        // if both are now null both scopes have the same depth
        return lParent == null && rParent == null;
    }
    
/*
    protected static boolean match(Refble left, Refble right) {
        // both null
//        if (left == null && right == null) {
//            return true;
//        }
        // only one null
        if (left == null || right == null) {
            return false;
        }

        return left.getName()
            .equals(right.getName());
    }
*/
    
    // this was already commented-out in the previous version
//    protected boolean refblesMatchPositionally(Refble left, Refble right) {
//        if (left == null || right == null) {
//            return false;
//        }
//
//        if (!left.getName()
//            .equals(right.getName())) {
//            return false;
//        }
//
//        var leftBlock = EcoreUtil2.getContainerOfType(left, Block.class);
//        var rightBlock = EcoreUtil2.getContainerOfType(right, Block.class);
//        var leftStatement = EcoreUtil2.getContainerOfType(left, Statement.class);
//        var rightStatement = EcoreUtil2.getContainerOfType(right, Statement.class);
//        var leftIndex = leftBlock.getStatements()
//            .indexOf(leftStatement);
//        var rightIndex = rightBlock.getStatements()
//            .indexOf(rightStatement);
//        return leftIndex == rightIndex;
//    }
/*
    protected boolean refblesMatchByScope(Refble left, Refble right) {
        if (left == null || right == null) {
            return false;
        }

        if (!left.getName()
            .equals(right.getName())) {
            return false;
        }

        var lParent = left.eContainer();
        var rParent = right.eContainer();
        while (lParent != null && rParent != null) {
            if (!eClassMatch(lParent, rParent)) {
                return false;
            }
            if (lParent instanceof Statement_Assignment && rParent instanceof Statement_Assignment) {
                var lAss = (Statement_Assignment) lParent;
                var rAss = (Statement_Assignment) rParent;
                if (lAss.getDests()
                    .size() != rAss.getDests()
                        .size()) {
                    return false;
                }
            }
            lParent = lParent.eContainer();
            rParent = rParent.eContainer();
        }

        // if both are now null both scopes have the same depth
        return lParent == null && rParent == null;
        
    }*/
    
    /*
    protected boolean refblesMatchByScope(Refble left, Refble right) {
        if (left == null || right == null) {
            return false;
        }

        if (!left.getName()
            .equals(right.getName())) {
            return false;
        }

        var lParent = left.eContainer();
        var rParent = right.eContainer();
        while (lParent != null && rParent != null) {
            if (!eClassMatch(lParent, rParent)) {
                return false;
            }
            if (lParent instanceof Statement_Assignment && rParent instanceof Statement_Assignment) {
                var lAss = (Statement_Assignment) lParent;
                var rAss = (Statement_Assignment) rParent;
                if (lAss.getDests()
                    .size() != rAss.getDests()
                        .size()) {
                    return false;
                }
            }
            lParent = lParent.eContainer();
            rParent = rParent.eContainer();
        }

        // if both are now null both scopes have the same depth
        return lParent == null && rParent == null;
    }
    */
/*
    protected boolean match(Expression_VariableName left, Expression_VariableName right) {
        // both null
//        if (left == null && right == null) {
//            return true;
//        }
        // only one null
        if (left == null || right == null) {
            return false;
        }
//        return refblesMatchPositionally(left.getRef(), right.getRef());
        return refblesMatchByScope(left.getRef(), right.getRef());
    }

    protected boolean match(Expression_Functioncall left, Expression_Functioncall right) {
        if (left instanceof Expression_Functioncall_Direct leftCall
                && right instanceof Expression_Functioncall_Direct rightCall) {
            if (leftCall.getCalledFunction() == null || rightCall.getCalledFunction() == null
                    || !refblesMatchByScope(leftCall.getCalledFunction(), rightCall.getCalledFunction())) {
                return false;
            }
        } else if (left instanceof Expression_Functioncall_Table leftCall
                && right instanceof Expression_Functioncall_Table rightCall) {
            if (leftCall.getCalledTable() == null || rightCall.getCalledTable() == null
                    || !refblesMatchByScope(leftCall.getCalledTable(), rightCall.getCalledTable())) {
                return false;
            }
        } else {
            return false;
        }
        return matchEList(left.getCalledFunctionArgs()
            .getArguments(),
                right.getCalledFunctionArgs()
                    .getArguments());
    }


    protected boolean match(Field_AppendEntryToTable left, Field_AppendEntryToTable right) {
        return match(left.getValue(), right.getValue());
    }

    protected boolean match(Statement_If_Then_Else left, Statement_If_Then_Else right) {
        if (!match(left.getIfExpression(), right.getIfExpression())) {
            return false;
        }
        if (!matchEList(left.getElseIf(), right.getElseIf())) {
            return false;
        }
        if ((left.getElseBlock() != null && right.getElseBlock() == null)
                || (left.getElseBlock() == null && right.getElseBlock() != null)) {
            return false;
        }
        // TODO remove if this does not help:
        // seems to help but not perfectly
        if (!matchEListByClass(left.getBlock().getStatements(), right.getBlock().getStatements())) {
            return false;
        }
//        if (!match(left.getBlock(), right.getBlock())) {
//            return false;
//        }
        return true;
    }

    protected boolean match(Statement_If_Then_Else_ElseIfPart left, Statement_If_Then_Else_ElseIfPart right) {
        // TODO is this enough?
        return match(left.getElseifExpression(), right.getElseifExpression());
    }

    protected boolean match(Statement_For left, Statement_For right) {
        if ((left.isGeneric() != right.isGeneric()) || (left.isNumeric() != right.isNumeric())) {
            return false;
        }
        if (!matchEList(left.getArgExpressions(), right.getArgExpressions())) {
            return false;
        }
        if (!matchEList(left.getArguments(), right.getArguments())) {
            return false;
        }
        return true;
    }

    protected boolean match(Statement_Assignment left, Statement_Assignment right) {
        var leftDests = left.getDests();
        var rightDests = right.getDests();
        
        // TODO remove after debugging
        if (leftDests.size() > 0) {
            if (leftDests.get(0) instanceof Referenceable) {
                var destName = ((Referenceable) leftDests.get(0)).getName();
                var leftParentParent = left.eContainer().eContainer();
                if (destName.equals("counter") && leftParentParent instanceof Statement_If_Then_Else) {
                    var foo = 42;
                }
            }
        }


        if (!matchEList(leftDests, rightDests)) {
            return false;
        }
        // TODO if we uses this to compare the assignment
        // we get a "not contained in a resource error" which indicates
        // that a change in a reference was not detected
//        if (!matchEList(left.getValues(), right.getValues())) {
//            return false;
//        }
        // so im trying this:
//        if (!matchEListByClass(left.getValues(), right.getValues())) {
//            return false;
//        }

        return true;
    }

    protected boolean match(Expression_TableAccess left, Expression_TableAccess right) {
        if ((left.getIndexableExpression() != null || right.getIndexableExpression() != null)
                && !match(left.getIndexableExpression(), right.getIndexableExpression())) {
            return false;
        }

        if (!matchEList(left.getIndexExpression(), right.getIndexExpression())) {
            return false;
        }

        if ((left.getFunctionName() != null && !left.getFunctionName()
            .equals(right.getFunctionName())) || (right.getFunctionName() != null
                    && !right.getFunctionName()
                        .equals(left.getFunctionName()))) {
            return false;
        }

        return true;
    }
    */
   

    protected boolean matchEList(EList<? extends EObject> left, EList<? extends EObject> right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.size() != right.size()) {
            return false;
        }
        for (var i = 0; i < left.size(); i++) {
            if (!match(left.get(i), right.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean matchEListByClass(EList<? extends EObject> left, EList<? extends EObject> right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.size() != right.size()) {
            return false;
        }
        for (var i = 0; i < left.size(); i++) {
            if (!eClassMatch(left.get(i), right.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected static boolean eClassMatch(EObject left, EObject right) {
        var leftClass = left.eClass()
            .getName();
        var rightClass = right.eClass()
            .getName();
        return leftClass.equals(rightClass);
    }
    
    protected void logMatchingReachedFalltrhoughForMessage(Class<?> clazz) {
    	LOGGER.error("Matching of " + clazz + " reached fall-through, although all possible cases should have been exhausted.");
    }
    
    protected boolean matchReferenceablesByName(Referenceable left, Referenceable right) {
    	return left.getName().equals(right.getName());
    }
    
    protected boolean bothInstanceOfAny(final Object left, final Object right, final List<Class<?>> clazzs) {
    	return clazzs.stream().anyMatch(clazz -> bothInstanceOf(left, right, clazz));
    }
    
    protected boolean bothInstanceOf(final Object left, final Object right, final Class<?> clazz) {
    	return clazz.isInstance(left) && clazz.isInstance(right);
    }
    
    /**
     * Do objects match based on lua attributes, like name
     * 
     * @param left
     * @param right
     * @return True if matching, false if not
     */
    /*
    public boolean match(EObject left, EObject right) {
        if (left == null || right == null) {
            return false;
        }

        if (!eClassMatch(left, right)) {
            return false;
        }
        
        // this "switch" is ugly, but i have no time to make it nice :/

        // terminals
        if (left instanceof Expression_Nil l && right instanceof Expression_Nil r) {
            return true;
        } else if (left instanceof Expression_True l && right instanceof Expression_True r) {
            return true;
        } else if (left instanceof Expression_False l && right instanceof Expression_False r) {
            return true;
        } else if (left instanceof Expression_VarArgs l && right instanceof Expression_VarArgs r) {
            return true;

            // unary expressions
        } else if (left instanceof Expression_Length l && right instanceof Expression_Length r) {
            return match(l.getExp(), r.getExp());
        } else if (left instanceof Expression_Invert l && right instanceof Expression_Invert r) {
            return match(l.getExp(), r.getExp());
        } else if (left instanceof Expression_Negate l && right instanceof Expression_Negate r) {
            return match(l.getExp(), r.getExp());
        } else if (left instanceof Expression_Number l && right instanceof Expression_Number r) {
            return l.getValue() == r.getValue();
        } else if (left instanceof Expression_String l && right instanceof Expression_String r) {
            return l.getValue()
                .equals(r.getValue());

            // binary expressions
        } else if (left instanceof Expression_Or l && right instanceof Expression_Or r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_And l && right instanceof Expression_And r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Larger l && right instanceof Expression_Larger r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Larger_Equal l && right instanceof Expression_Larger_Equal r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Smaller l && right instanceof Expression_Smaller r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Smaller_Equal l && right instanceof Expression_Smaller_Equal r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Equal l && right instanceof Expression_Equal r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Not_Equal l && right instanceof Expression_Not_Equal r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Concatenation l && right instanceof Expression_Concatenation r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Plus l && right instanceof Expression_Plus r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Minus l && right instanceof Expression_Minus r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Multiplication l && right instanceof Expression_Multiplication r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Division l && right instanceof Expression_Division r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Exponentiation l && right instanceof Expression_Exponentiation r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());
        } else if (left instanceof Expression_Modulo l && right instanceof Expression_Modulo r) {
            return match(l.getLeft(), r.getLeft()) && match(l.getRight(), r.getRight());

            // other expressions
        } else if (left instanceof Expression_TableAccess l && right instanceof Expression_TableAccess r) {
            return match(l, r);
        } else if (left instanceof Expression_TableConstructor l && right instanceof Expression_TableConstructor r) {
            return match(l, r);
        } else if (left instanceof Expression_Functioncall l && right instanceof Expression_Functioncall r) {
            return match(l, r);
        } else if (left instanceof Expression_VariableName l && right instanceof Expression_VariableName r) {
            return match(l, r);
        } else if (left instanceof Expression_Import l && right instanceof Expression_Import r) {
            return l.getImportURI().equals(r.getImportURI());

            // remaining statements etc.
        } else if (left instanceof Statement_Assignment l && right instanceof Statement_Assignment r) {
            return match(l, r);
        } else if (left instanceof Statement_If_Then_Else l && right instanceof Statement_If_Then_Else r) {
            return match(l, r);
        } else if (left instanceof Statement_If_Then_Else_ElseIfPart l
                && right instanceof Statement_If_Then_Else_ElseIfPart r) {
            return match(l, r);
        } else if (left instanceof Statement_While l && right instanceof Statement_While r) {
            return match(l.getExpression(), r.getExpression());
        } else if (left instanceof Statement_For l && right instanceof Statement_For r) {
            return match(l, r);
        } else if (left instanceof Field_AppendEntryToTable l && right instanceof Field_AppendEntryToTable r) {
            return match(l, r);
        } else if (left instanceof Functioncall_Arguments l && right instanceof Functioncall_Arguments r) {
            return matchEList(l.getArguments(), r.getArguments());
        } else if (left instanceof Function l && right instanceof Function r) {
            return matchEList(l.getArguments(), r.getArguments());
        } else if (left instanceof LastStatement_Return_WithValue l && right instanceof LastStatement_Return_WithValue r) {
            return matchEList(l.getReturnValues(), r.getReturnValues());
        } else if (left instanceof Refble l && right instanceof Refble r) {
            return match(l, r);
            
        // meta model stuff
        // we say these match because of the hierarchical matching taking place
        } else if (left instanceof Block l && right instanceof Block r) {
            return true;
        } else if (left instanceof Chunk l && right instanceof Chunk r) {
            return true;
        } else if (left instanceof ComponentSet l && right instanceof ComponentSet r) {
            return true;
        } else if (left instanceof LastStatement_Return l && right instanceof LastStatement_Return r) {
            return true;
          
        // these have names:
        } else if (left instanceof NamedChunk l && right instanceof NamedChunk r) {
            return l.getName().equals(r.getName());
        } else if (left instanceof Component l && right instanceof Component r) {
            return l.getName().equals(r.getName());
        }

        // no decision from the custom matchers -> use fallback
        // TODO reenable
//        return editionDistanceEqualityChecker.match(left, right);
        return false;
    }
*/
    /*
    @Override
    protected boolean matchingEObjects(EObject left, EObject right) {
        return match(left, right);
    }*/
    
}
