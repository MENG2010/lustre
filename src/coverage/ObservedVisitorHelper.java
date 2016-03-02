package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import enums.ObservedTreeType;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import main.LustreMain;
/*
 * Parse node into trees.
 * 	- Dependency relationship of equation ids and variables.
 *  - Delay dependency.
 *  - And any necessary dependencies.
 */
public class ObservedVisitorHelper {
	private List<Node> nodes;
	private HashMap<Node, List<VarDecl>> outputsTable = new HashMap<Node, List<VarDecl>>();
	private List<String> idList = new ArrayList<String>();
	private List<String> preTreeRootList = new ArrayList<String>();
	private HashMap<IdExpr, Expr> exprTable = new HashMap<IdExpr, Expr>();
	private List<String> delayTreeRoots = new ArrayList<String>();
	
	public ObservedVisitorHelper(List<Node> nodes) {
		setNodes(nodes);
	}
	
	/*
	 * Build referring trees
	 */
	public List<ObservedTree> buildRefTreesForInput() {
		populateOutputTable(this.nodes);
//		HashMap<Node, List<VarDecl>> outputTable = getOutputTable(nodes);
		List<ObservedTree> trees = new ArrayList<ObservedTree>();
		// build reference trees, node by node
		for (Node node : this.nodes) {
			LustreMain.log(">>> Building reference trees for " + node.id);
			trees = buildRefTreesForNode(node);
			LustreMain.log(">>> Done!!! Building reference trees for " + node.id);
			LustreMain.log(trees.toString() + "\n\n");
		}
		return trees;
	}
	
	/*
	 * Build referring trees for given node, one tree per output.
	 * Example: A is an output variable, and A = (B and C); B = (C or D);
	 * 			then the tree should look like,
	 * 					A
	 * 				  /   \
	 * 				 B	   C
	 *				/ \
	 *             C   D
	 */
	public List<ObservedTree> buildRefTreesForNode(Node node) {
		List<VarDecl> roots = getRootList(node);
		this.idList = getIdList(node);
		Map<String, Expr> expressions = getExprTable(node);
		List<ObservedTree> subTrees = new ArrayList<ObservedTree>();
		System.out.println("All ids in node \"" + node.id + "\":\n" + idList.toString() + "\n");
		// create one tree per output
		for (VarDecl r : roots) {
			TreeNode treeRoot = new TreeNode(r.id);
			ObservedTree tree = buildTreeRecursively(treeRoot, expressions);
			LustreMain.log(">>>>>> Done!!! Building reference tree for " + r.id);
			subTrees.add(new ObservedTree(treeRoot));
		}
		
		return subTrees;
	}

	public List<ObservedTree> buildSequentialTrees() {
		List<ObservedTree> seqTrees = new ArrayList<ObservedTree>();
		
		
		return seqTrees;
	}
	
	
	/*
	 * Recursively build a subtree given a root.
	 */
	private ObservedTree buildTreeRecursively(TreeNode root, Map<String, Expr> exprs) {
		ObservedTree subtree = new ObservedTree(root);
		ArrayList<String> childList = new ArrayList<String>();
		
		if (!exprs.keySet().contains(root.data)) {
			throw new IllegalArgumentException("!!! No corresponding expression!!! >>> " + root.data);
		} else if (exprs.get(root.data).toString().contains(UnaryOp.PRE.toString() + " ")) {
			return null;
		}
		
		LustreMain.log(">>>>>> Building reference tree for " + root.data);
//		LustreMain.log(">>>>>> Expression = " + exprs.get(root.data));
		if (exprs.get(root.data).toString().indexOf(" ") > 0) {
			String[] items = exprs.get(root.data).toString().replaceAll("[(){}]", " ").split(" ");
			for (String item : items) {
				item = item.trim();
				if (!idList.contains(item) || childList.contains(item)) {
					continue;
				} else {
					childList.add(item);
				}
			}
		} else {
			childList.add(exprs.get(root.data).toString().replaceAll("[ (){}]", ""));
		}
		
		System.out.println("Child List: " + childList.toString());
		for (int i = 0; i < childList.size(); i++) {
			TreeNode node = new TreeNode(childList.get(i).toString());
			root.addChild(node);
//			System.out.println(">>>>>>>>> add " + node.data + " to " + root.data);
			if (exprs.keySet().contains(node.data)) {
				// build the tree recursively if next node is 
				// on the left-hand-side of some expressions
				buildTreeRecursively(node, exprs);
			} else {
				// stop recursion if the next node does not depend on other variables
				continue;
			}
		}

		return subtree;
	}
	
	private void setNodes(List<Node> nodes) {
		if (nodes == null || nodes.size() == 0) {
			throw new IllegalArgumentException("Null or empty node list!");
		}
		this.nodes = nodes;
	}
	
	private List<VarDecl> getRootList(Node node) {
		return this.outputsTable.get(node);
	}
	
	private void populateOutputTable(List<Node> nodes) {
//		HashMap<Node, List<VarDecl>> outputTable = new HashMap<Node, List<VarDecl>>();
		for (Node node : nodes) {
			// get the output list, node by node
			List<VarDecl> outputs = node.outputs;
			this.outputsTable.put(node, outputs);
		}
//		return outputTable;
	}
	
	/*
	 * return all variables (inputs, outputs, and locals) given a node 
	 */
	private List<String> getIdList(Node node) {
		List<String> varList = new ArrayList<String>();
//		System.out.println("local variable list of " + node.id);
		for (VarDecl output : node.outputs) {
			varList.add(output.id);
		}
		for (VarDecl input : node.inputs) {
			varList.add(input.id);
		}
		for (VarDecl local : node.locals) {
			varList.add(local.id);
		}
		return varList;
	}
	
	/*
	 * Search for all roots of delay dependency trees in given node
	 */
	private List<String> getPreTreeRoots(HashMap<IdExpr, Expr> exprTable) {
		List<String> roots = new ArrayList<String>();
		
		for (IdExpr lhs : exprTable.keySet()) {
			if (exprTable.get(lhs).toString().contains(UnaryOp.PRE + " ")) {
				roots.add(lhs.id);
			}
		}
		
		return roots;
	}
	
	/*
	 * re-organize expressions of given node with hashtable
	 */
	private HashMap<String, Expr> getExprTable(Node node) {
		HashMap<String, Expr> exprTable = new HashMap<String, Expr>();
		List<Equation> equations = node.equations;
		
		/*
		 * put expressions of given node into a hashmap<id, expr>
		 */
		for (Equation equation : equations) {
			exprTable.put(equation.lhs.get(0).id, equation.expr);
//			System.out.println(equation.lhs.get(0).id + " = " + equation.expr);
		}
		
		return exprTable;
	}
	
}

