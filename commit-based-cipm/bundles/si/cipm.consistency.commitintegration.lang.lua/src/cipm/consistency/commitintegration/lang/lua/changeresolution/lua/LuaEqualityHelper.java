package cipm.consistency.commitintegration.lang.lua.changeresolution.lua;

import java.util.List;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.utils.EqualityHelper;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.xtext.lua.component_extension.Application;
import org.xtext.lua.component_extension.Component;
import org.xtext.lua.component_extension.NamedChunk;
import org.xtext.lua.component_extension.impl.Component_extensionPackageImpl;
import org.xtext.lua.lua.ArgList;
import org.xtext.lua.lua.Args;
import org.xtext.lua.lua.Assignment;
import org.xtext.lua.lua.Block;
import org.xtext.lua.lua.Break;
import org.xtext.lua.lua.Chunk;
import org.xtext.lua.lua.DoEndBlock;
import org.xtext.lua.lua.ElseIf;
import org.xtext.lua.lua.ExpAdd;
import org.xtext.lua.lua.ExpAnd;
import org.xtext.lua.lua.ExpConcat;
import org.xtext.lua.lua.ExpDiv;
import org.xtext.lua.lua.ExpEq;
import org.xtext.lua.lua.ExpExponentiation;
import org.xtext.lua.lua.ExpFalse;
import org.xtext.lua.lua.ExpFunctionDeclaration;
import org.xtext.lua.lua.ExpGeq;
import org.xtext.lua.lua.ExpGt;
import org.xtext.lua.lua.ExpInvert;
import org.xtext.lua.lua.ExpLength;
import org.xtext.lua.lua.ExpLeq;
import org.xtext.lua.lua.ExpList;
import org.xtext.lua.lua.ExpLt;
import org.xtext.lua.lua.ExpMod;
import org.xtext.lua.lua.ExpMult;
import org.xtext.lua.lua.ExpNegate;
import org.xtext.lua.lua.ExpNeq;
import org.xtext.lua.lua.ExpNil;
import org.xtext.lua.lua.ExpNumberLiteral;
import org.xtext.lua.lua.ExpOr;
import org.xtext.lua.lua.ExpStringLiteral;
import org.xtext.lua.lua.ExpSub;
import org.xtext.lua.lua.ExpTrue;
import org.xtext.lua.lua.ExpVarArgs;
import org.xtext.lua.lua.Feature;
import org.xtext.lua.lua.FieldList;
import org.xtext.lua.lua.FuncBody;
import org.xtext.lua.lua.FunctionCall;
import org.xtext.lua.lua.FunctionCallStat;
import org.xtext.lua.lua.GenericFor;
import org.xtext.lua.lua.Goto;
import org.xtext.lua.lua.GroupedExp;
import org.xtext.lua.lua.IfThenElse;
import org.xtext.lua.lua.Label;
import org.xtext.lua.lua.LiteralStringArg;
import org.xtext.lua.lua.LocalAssignment;
import org.xtext.lua.lua.MemberAccess;
import org.xtext.lua.lua.MethodCall;
import org.xtext.lua.lua.NameList;
import org.xtext.lua.lua.NumericFor;
import org.xtext.lua.lua.ParList;
import org.xtext.lua.lua.ParamArgs;
import org.xtext.lua.lua.Referenceable;
import org.xtext.lua.lua.RepeatLoop;
import org.xtext.lua.lua.Return;
import org.xtext.lua.lua.TableAccess;
import org.xtext.lua.lua.TableConstructor;
import org.xtext.lua.lua.Var;
import org.xtext.lua.lua.WhileLoop;
import org.xtext.lua.lua.impl.LuaPackageImpl;

import com.google.common.cache.LoadingCache;

/**
 * This class defines how the constructs of the Lua code model should be matched (which code model objects are "equal")
 * for the comparison of multiple Lua code models comparison via emfcompare. </br></br>
 * 
 * The {@link #LuaHierarchicalMatchEngineFactory} implements hierarchical matching, i.e. the
 * left and right EObjects matched by this EqualityHelper class are assumed to always be on the same level
 * in the ASTs of the compared models.
 * @author jsaenz
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
        verifyAllEClassesHandled();
    }
    
    private void verifyAllEClassesHandled() {
    	final var luaPackageEClasses = LuaPackageImpl.init().getEClassifiers()
    										.stream()
    										.filter(eClassifier -> eClassifier instanceof EClass);
    	final var componentExtensionPackageEClasses = Component_extensionPackageImpl.init().getEClassifiers()
    										.stream()
    										.filter(eClassifier -> eClassifier instanceof EClass);
    	final var allEClasses = Stream.concat(luaPackageEClasses, componentExtensionPackageEClasses).toList();
    	System.out.println(allEClasses);
    	System.out.println(allEClasses.size());
    }
    
    @Override
    protected boolean matchingEObjects(EObject left, EObject right) {
    	// TODO: may be better to extract null-check here, instead of performing it in every match(..) function
    	//  - need to test if extracting the null check and eClass check from the match(EObject left, EObject right) function
    	//    can be extracted to here
        return match(left, right);
    }
    
    /**
     * Do objects match based on lua attributes, like name.
     * For information on how which of the overloaded methods is executed, see
     * - https://docs.oracle.com/javase/specs/jls/se10/html/jls-8.html#jls-8.4.9
     * - https://stackoverflow.com/questions/11110631/java-overloading-and-inheritance-rules
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
        else if (left instanceof ExpFunctionDeclaration l && right instanceof ExpFunctionDeclaration r) {
        	return match(l.getBody(), r.getBody());
        }
        else if (left instanceof TableConstructor l && right instanceof TableConstructor r) {
        	return match(l, r);
        } else if (left instanceof Feature l && right instanceof Feature r) {
        	return match(l, r);
        } 
        
        // remaining statments etc.
        else if (left instanceof Goto l && right instanceof Goto r) {
        	return match(l,r);
        }
        else if (left instanceof FunctionCallStat l && right instanceof FunctionCallStat r) {
        	// FunctionCallStats are FeaturePaths, Features of the path are tested separately by the HierarchicalMatchEngine
        	return true;
        } else if (left instanceof LocalAssignment l && right instanceof LocalAssignment r) {
        	return match(l, r);
        } else if (left instanceof Assignment l && right instanceof Assignment r) {
            return match(l, r);
        } else if (left instanceof IfThenElse l && right instanceof IfThenElse r) {
            return match(l, r);
        } else if (left instanceof ElseIf l && right instanceof ElseIf r) {
            return match(l, r);
        } else if (left instanceof WhileLoop l && right instanceof WhileLoop r) {
        	return match(l, r);
        } else if (left instanceof RepeatLoop l && right instanceof RepeatLoop r) {
        	return match(l, r);
        } else if (left instanceof NumericFor l && right instanceof NumericFor r) {
        	return match(l, r);
        } else if (left instanceof GenericFor l && right instanceof GenericFor r) {
        	return match(l, r);
        } else if (left instanceof ArgList l && right instanceof ArgList r) {
        	return match(l, r);
        } else if (left instanceof LiteralStringArg l && right instanceof LiteralStringArg r) {
        	return match(l, r);
        } else if (left instanceof ParList l && right instanceof ParList r) {
        	return match(l, r);
        } else if (left instanceof NameList l && right instanceof NameList r) {
        	return matchEList(l.getNames(), r.getNames());
        } else if (left instanceof ExpList l && right instanceof ExpList r) {
        	return match(l, r);
        } else if (left instanceof FieldList l && right instanceof FieldList r) {
        	return matchEList(l.getFields(), r.getFields());
        } else if (left instanceof ParamArgs l && right instanceof ParamArgs r) {
        	return match(l, r);
        } else if (left instanceof FuncBody l && right instanceof FuncBody r) {
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
        else if (bothInstanceOfAny(left, right, List.of(Block.class, Chunk.class, Application.class, Break.class, DoEndBlock.class))) {
        	return true;
        }
        // these have names:
        else if (left instanceof NamedChunk l && right instanceof NamedChunk r) {
            return l.getName().equals(r.getName());
        } else if (left instanceof Component l && right instanceof Component r) {
            return l.getName().equals(r.getName());
        }

    	logMatchingReachedFalltrhoughForMessage("match(EObject left, EObject right)", left.getClass());
        return false;
    }
    
    /**
     * Test if the {@link Label}s referenced by the given {@link Goto}s match, i.e.
     * are non-null, have the same name and the same depth inside the CM.
     */
    protected boolean match(Goto left, Goto right) {
    	if (left.getRef() == null || right.getRef() == null) {
    		return false;
    	}

        return refblesMatchByScope(left.getRef(), right.getRef());
    }


    protected boolean match(TableConstructor left, TableConstructor right) {
    	if (left.getFieldList() == null && right.getFieldList() == null) {
    		// TableConstructors with fieldList == null are empty constructors
    		return true;
    	}
        return matchEList(left.getFieldList().getFields(), right.getFieldList().getFields());
    }
    
    /**
     * For {@link Feature}s, it should suffice to match their respective characteristics (e.g. name for NamedFeatures,
     * parameters for Function and MethodCalls,..), since the hierarchical nature of the MatchEngine this EqualityHelper
     * is used in ensures that the FeaturePaths of the Features match (i.e. previous Features match).
     */
    protected boolean match(Feature left, Feature right) {
    	if (left == null || right == null) {
    		return false;
    	}

    	// hierarchical nature of the MatchEngine processes the 
    	// previous/following features in a feature path, i.e. we don't need to compare the
    	// path prefixes here.
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
    	} else if (left instanceof GroupedExp l && right instanceof GroupedExp r) {
    		return match(l.getExp(), r.getExp());
    	}

    	logMatchingReachedFalltrhoughForMessage("match feature", Feature.class);
    	//TODO: do we need to match "require" function as a special case?
    	return false;
    }
    
    protected boolean match(Var left, Var right) {
    	return refblesMatchByScope(left, right);
    }
    
    // A FunctionCall represents only the brackets with arguments "()", i.e. the matching
    // of the NamedFeature preceding the FunctionCall (e.g. "func" in "func()" was already matched by the 
    // hierarchical nature of the match engine.
    protected boolean match(FunctionCall left, FunctionCall right) {
    	return left != null && right != null
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


    protected boolean match(LocalAssignment left, LocalAssignment right) {
    	return match(left.getExpList(), right.getExpList());
    }

    protected boolean match(Assignment left, Assignment right) {
    	return match(left.getExpList(), right.getExpList());
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
        // TODO: comment by jsaenz: why not match the stats? seems to have been tried before with !match(left.getBlock(), right.getBlock())...
        if (!matchEListByClass(left.getThenBlock().getStats(), right.getThenBlock().getStats())) {
            return false;
        }
        return true;
    }

    protected boolean match(ElseIf left, ElseIf right) {
        // TODO is this enough?
    	// TODO: comment by jsaenz: it should be, assuming that only ElseIfs contained in the same IfThenElse are
    	// compared (hierarchical matching) and that an ElseIf with the same condition as another in the same IfThenElse
    	// seems illogical.
        return match(left.getCondition(), right.getCondition());
    }
    
    protected boolean match(WhileLoop left, WhileLoop right) {
        return match(left.getCondition(), right.getCondition());
    }

    protected boolean match(RepeatLoop left, RepeatLoop right) {
        return match(left.getCondition(), right.getCondition());
    }

    protected boolean match(NumericFor left, NumericFor right) {
    	return match(left.getArg(), right.getArg())
    			&& match(left.getFromExp(), right.getFromExp())
    			&& match(left.getToExp(), right.getToExp())
    			&& (left.getStepExp() == null && right.getStepExp() == null
    				|| match(left.getStepExp(), right.getStepExp())
    				);
    }
    
    protected boolean match(GenericFor left, GenericFor right) {
    	if (left == null || right == null) {
    		return false;
    	}
        return match(left.getArgList(), right.getArgList())
        		&& match(left.getExpList(), right.getExpList());
    }
    
    protected boolean match(FuncBody left, FuncBody right) {
        return match(left.getParList(), right.getParList());
    }
    
    protected boolean match(ParList left, ParList right) {
    	// ParList == null implies empty parameters
    	if (left == null && right == null) {
    		return true;
    	}
        return (left instanceof ExpVarArgs && right instanceof ExpVarArgs)
        		|| match(left.getArgsList(), right.getArgsList());
    }
    
    protected boolean match(ArgList left, ArgList right) {
    	// ArgList should never be null -> we do not need a null-check here
        return (left instanceof ExpVarArgs && right instanceof ExpVarArgs)
        		|| matchEList(left.getArgs(), right.getArgs());
    }
    
    
    protected boolean match(Return left, Return right) {
        return (left.getExpList() == null && right.getExpList() == null) // both are return statements without expressions
        		|| match(left.getExpList(), right.getExpList());
    }
    
    protected boolean match(Args left, Args right) {
    	if (left == null || right == null) {
    		return false;
    	}
    	
    	if (!eClassMatch(left, right)) {
    		return false;
    	}
    	
    	if (left instanceof ParamArgs l && right instanceof ParamArgs r) {
    		return match(l, r);
    	} else if (left instanceof TableConstructor l && right instanceof TableConstructor r) {
    		return match(l, r);
    	} else if (left instanceof LiteralStringArg l && right instanceof LiteralStringArg r) {
    		return match(l, r);
    	}
    	
    	// fallback, should not be reached since all possible options should be exhausted at this point
    	// (i.e. classes are equal and all "Args-types" have a conditional)
    	logMatchingReachedFalltrhoughForMessage("match(Args left, Args right)", Args.class);
    	return false;
    }

    protected boolean match(LiteralStringArg left, LiteralStringArg right) {
    	return left.getStr().equals(right.getStr());
    }

    protected boolean match(ParamArgs left, ParamArgs right) {
		var lExpList = left.getParams();
		var rExpList = right.getParams();
		// empty param args are null (e.g. func() has no params)
		if (lExpList == null && rExpList == null) {
			return true;
		}
		return match(lExpList, rExpList);
    }

    protected boolean match(ExpList left, ExpList right) {
    	if (left == null && right == null) {
    		return true;
    	}
    	if (left == null || right == null) {
    		return false;
    	}
    	return matchEList(left.getExps(), right.getExps());
    }
    
    /**
     * Tests if the given {@link Referenceable}s match by testing if their names are equal,
     * and if they occur at the same depth in the respective CMs.
     */
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
            // TODO: Not sure why this comparison is done here (legacy from matching for previous Lua CM)
            if (lParent instanceof Assignment la && rParent instanceof Assignment ra) {
                final var lExpList = ((Assignment) lParent).getExpList();
                final var rExpList = ((Assignment) rParent).getExpList();
                if (lExpList.getExps().size() != rExpList.getExps().size()) {
                	return false;
                }
            }
            if (lParent instanceof LocalAssignment la && rParent instanceof LocalAssignment ra) {
                var lExpList = la.getExpList();
                var rExpList = ra.getExpList();
                if (lExpList != null && rExpList != null) {
                	var lExps = lExpList.getExps();
                	var rExps = rExpList.getExps();
					if (lExps.size() != rExps.size()) {
						return false;
					}
                }
            }
            lParent = lParent.eContainer();
            rParent = rParent.eContainer();
        }

        // if both are now null both scopes have the same depth
        return lParent == null && rParent == null;
    }
    

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
    
    protected void logMatchingReachedFalltrhoughForMessage(String method, Class<?> clazz) {
    	LOGGER.error("Matching of " + clazz + " reached fall-through in " + method + ", although all possible cases should have been exhausted.");
    }
    
    protected boolean matchReferenceablesByName(Referenceable left, Referenceable right) {
    	if (left.getName() == null || right.getName() == null) {
    		System.out.println("Encountered Referenceables with null name while matching: left: " + left + ", right: " + right);
    		return false;
    	}
    	return left.getName().equals(right.getName());
    }
    
    protected boolean bothInstanceOfAny(final Object left, final Object right, final List<Class<?>> clazzs) {
    	return clazzs.stream().anyMatch(clazz -> bothInstanceOf(left, right, clazz));
    }
    
    protected boolean bothInstanceOf(final Object left, final Object right, final Class<?> clazz) {
    	return clazz.isInstance(left) && clazz.isInstance(right);
    }
}

