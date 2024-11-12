package cipm.consistency.cpr.luapcm.seffreconstruction;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.EcoreUtil2;

import lua.Arg;
import lua.Block;
import lua.ExpFunctionDeclaration;
import lua.FunctionDeclaration;
import lua.LocalFunctionDeclaration;
import lua.ParList;
import lua.Referenceable;
import lua.Stat;

public class LuaFunctionDeclaration {
	
	/**
	 * The name of the function declaration. Note that this may differ from the name of a function call
	 * calling the function declaration, since a function imported via require(...) may get assigned to a variable
	 * with a different name.
	 */
	private String name;
	private List<Arg> args;
	private Block block;
	/**
	 * The root Referenceable from the CM, might be {@link FunctionDeclaration}, {@link LocalFunctionDeclaration},
	 * or {@link ExpFunctionDeclaration}.
	 */
	private Referenceable root;
	private Stat containingStat;
	
	private LuaFunctionDeclaration() { }

	public List<Arg> getArgs() {
		return args;
	}

	public Block getBlock() {
		return block;
	}

	/**
	 * Returns the Referenceable corresponding to this LuaFunctionDeclaration from the CM.
	 */
	public Referenceable getRoot() {
		return root;
	}
	
	/**
	 * Returns the name of the function declaration. Note that this may differ from the name of a function call
	 * calling the function declaration, since a function imported via require(...) may get assigned to a variable
	 * with a different name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the statement containing this function declaration.
	 */
	public Stat getContainingStat() {
		return containingStat;
	}

	public static LuaFunctionDeclaration from(FunctionDeclaration decl) {
		var functionDeclaration = new LuaFunctionDeclaration();
		
		final var name = decl.getName();
		final var args = getArgsFromParList(decl.getBody().getParList());
		final var block = decl.getBody().getFuncBlock();
		
		functionDeclaration.init(decl, name, args, block, decl);
		return functionDeclaration;
	}
	
	public static LuaFunctionDeclaration from(LocalFunctionDeclaration decl) {
		var functionDeclaration = new LuaFunctionDeclaration();
		
		final var name = decl.getName();
		final var args = getArgsFromParList(decl.getBody().getParList());
		final var block = decl.getBody().getFuncBlock();
		
		functionDeclaration.init(decl, name, args, block, decl);
		return functionDeclaration;
	}
	
	public static LuaFunctionDeclaration from(ExpFunctionDeclaration decl) {
		var functionDeclaration = new LuaFunctionDeclaration();
		
		// name of ExpFunctionDeclaration might be null, e.g. for ExpFunctionDeclaration inside of ParamArgs
		final var name = decl.getName();
		final var containingStat = EcoreUtil2.getContainerOfType(decl, Stat.class);
		if (name == null || containingStat == null) {
			return null;
		}
		final var args = getArgsFromParList(decl.getBody().getParList());
		final var block = decl.getBody().getFuncBlock();
		
		functionDeclaration.init(decl, name, args, block, containingStat);
		return functionDeclaration;
	}
	
	private void init(Referenceable root, String name, List<Arg> args, Block block, Stat containingStat) {
		this.root = root;
		this.name = name;
		this.args = args;
		this.block = block;
		this.containingStat = containingStat;
	}
	
	private static List<Arg> getArgsFromParList(ParList parList) {
		var result = new ArrayList<Arg>();
		if (parList != null && parList.getArgsList() != null) {
			result.addAll(parList.getArgsList().getArgs());
		}
		return result;
	}
	
}
