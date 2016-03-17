package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
public class ObservedCoverageHelper {
	private List<Node> nodes;
	private HashMap<Node, List<VarDecl>> outputsTable = new HashMap<Node, List<VarDecl>>();
	private List<VarDecl> idList = new ArrayList<VarDecl>();
	private boolean isSeqRoot;
	private HashMap<String, VarDecl> idsOfNode = new HashMap<String, VarDecl>();
	private HashMap<IdExpr, Expr> exprTable = new HashMap<IdExpr, Expr>();
	
	public ObservedCoverageHelper(List<Node> nodes) {
		setNodes(nodes);
		this.nodes = nodes;
		populateOutputTable(this.nodes);
	}
	
	/*
	 * Build referring trees
	 */
	public HashMap<Node, HashMap<VarDecl, ObservedTree>> buildRefTreesForInput() {
//		HashMap<Node, List<VarDecl>> outputTable = getOutputTable(nodes);
		HashMap<Node, HashMap<VarDecl, ObservedTree>> trees = new HashMap<Node, HashMap<VarDecl, ObservedTree>>();
		HashMap<VarDecl, ObservedTree> treesOfNode = new HashMap<VarDecl, ObservedTree>();
		// build reference trees, node by node
		for (Node node : this.nodes) {
			LustreMain.log(">>> Building reference trees for " + node.id);
			
			treesOfNode = buildRefTreesForNode(node);
//			LustreMain.log(">>> Done!!! Building reference trees for " + node.id);
			trees.put(node, treesOfNode);
//			LustreMain.log(trees.toString() + "\n\n");
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
	public HashMap<VarDecl, ObservedTree> buildRefTreesForNode(Node node) {
		List<VarDecl> roots = getRootList(node);
		this.idList = populateIdList(node);
		idsOfNode = getIds(node);
		Map<String, Expr> expressions = getStrExprTable(node);
		HashMap<VarDecl, ObservedTree> subTrees = new HashMap<VarDecl, ObservedTree>();
		System.out.println("All ids in node \"" + node.id + "\":\n" + idList.toString() + "\n");
		// create one tree per output
		for (VarDecl r : roots) {
			ObservedTreeNode treeRoot = new ObservedTreeNode(r.id);
			buildTreeRecursively(treeRoot, expressions, true);
			subTrees.put(r, new ObservedTree(treeRoot));
		}
		return subTrees;
	}

	// build sequential trees
	public HashMap<Node, HashMap<VarDecl, ObservedTree>> buildSeqTreesForInput() {
		HashMap<Node, HashMap<VarDecl, ObservedTree>> trees = new HashMap<Node, HashMap<VarDecl, ObservedTree>>();
		HashMap<VarDecl, ObservedTree> treesOfNode = new HashMap<VarDecl, ObservedTree>();
		
		// build reference trees, node by node
		for (Node node : this.nodes) {
//			LustreMain.log(">>> Building sequential trees for " + node.id);
			idList = populateIdList(node);
			treesOfNode = buildSeqTreesForNode(node);
			trees.put(node, treesOfNode);
		}
		return trees;		
	}
	
	public HashMap<VarDecl, ObservedTree> buildSeqTreesForNode(Node node) {
		HashMap<VarDecl, ObservedTree> seqTrees = new HashMap<VarDecl, ObservedTree>();
		HashMap<String, Expr> expressions = getStrExprTable(this.nodes.get(0));
		HashMap<String, VarDecl> ids = getIds(node);
		List<String> roots = getSeqTreeRoots(expressions);
		ObservedTreeNode treeNode;
		
		for (String root : roots) {
			isSeqRoot = true;
			treeNode = buildTreeRecursively(new ObservedTreeNode(root), expressions, false);
			seqTrees.put(ids.get(root), new ObservedTree(treeNode));
		}
		
		return seqTrees;
	}
	
	
	/*
	 * Recursively build a subtree given a root.
	 */
	private ObservedTreeNode buildTreeRecursively(ObservedTreeNode root, Map<String, Expr> exprs, 
			boolean isBuildingRef) {
		ObservedTreeNode rootNode = new ObservedTreeNode(root.data);
		List<String> ids = getStrIds();
//		HashMap<String, VarDecl> ids = this.getIds(node);
		ArrayList<String> childList = new ArrayList<String>();
		
		if (isBuildingRef) {
			// build reference tree
			if (!exprs.keySet().contains(root.data)) {
				throw new IllegalArgumentException("!!! No corresponding expression!!! >>> " + root.data);
			} else if (exprs.get(root.data).toString().contains(UnaryOp.PRE.toString() + " ")) {
				return null;
			}
		} else {
			// build sequential tree
			if (!exprs.keySet().contains(root.data)) {
				throw new IllegalArgumentException("!!! No corresponding expression!!! >>> " + root.data);
			} else if (exprs.get(root.data).toString().contains(UnaryOp.PRE.toString() + " ")) {
				if (isSeqRoot) {
					// if it's root, build the tree
					isSeqRoot = false;
				} else {
					// otherwise, stop building sub-tree
					return null;
				}
			}
		}
		
//		LustreMain.log(">>>>>> Building tree for " + root.data);
//		LustreMain.log(">>>>>> Expression = " + exprs.get(root.data));
		if (exprs.get(root.data).toString().indexOf(" ") > 0) {
			String[] items = exprs.get(root.data).toString().replaceAll("[(){}]", " ").split(" ");
			for (String item : items) {
				item = item.trim();
				if (item.isEmpty()) {
					continue;
				} else if (!ids.contains(item)) {
					continue;
				} else if (childList.contains(item)) {
					continue;
				} else {
					childList.add(item);
				}
			}
		} else {
			childList.add(exprs.get(root.data).toString().replaceAll("[ (){}]", ""));
		}
		
//		System.out.println("Child List: " + childList.toString());
		// add children to current root
		for (int i = 0; i < childList.size(); i++) {
			ObservedTreeNode node = new ObservedTreeNode(childList.get(i).toString());
			root.addChild(node);
			if (exprs.keySet().contains(node.data)) {
				// build the tree recursively if next node is 
				// on the left-hand-side of some expressions
				buildTreeRecursively(node, exprs, isBuildingRef);
			} else {
				// stop recursion if the next node does not depend on other variables
				continue;
			}
		}
		System.out.println("(" + root.data + ") has " + root.getNumberOfChildren() 
							+ " children: "+ root.getChildren().toString());
		return root;
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
		for (Node node : nodes) {
			// get the output list, node by node
			List<VarDecl> outputs = node.outputs;
			this.outputsTable.put(node, outputs);
		}
	}
	
	/*
	 * return all variables (inputs, outputs, and locals) given a node 
	 */
	private List<VarDecl> populateIdList(Node node) {
		List<VarDecl> varList = new ArrayList<VarDecl>();
		for (VarDecl output : node.outputs) {
			varList.add(output);
		}
		for (VarDecl input : node.inputs) {
			varList.add(input);
		}
		for (VarDecl local : node.locals) {
			varList.add(local);
		}
		return varList;
	}
	
	public HashMap<String, VarDecl> getIds(Node node) {
		HashMap<String, VarDecl> idList = new HashMap<String, VarDecl>();
		for (VarDecl output : node.outputs) {
			idList.put(output.id, output);
		}
		for (VarDecl input : node.inputs) {
			idList.put(input.id, input);
		}
		for (VarDecl local : node.locals) {
			idList.put(local.id, local);
		}
		return idList;
	}
	
	private List<String> getStrIds() {
		List<String> strIdList = new ArrayList<String>();
		for (VarDecl id : this.idList) {
			strIdList.add(id.id);
		}
		return strIdList;
	}
	
	/*
	 * Search for all roots of delay dependency trees in given node
	 */
	private List<String> getSeqTreeRoots(HashMap<String, Expr> exprTable) {
		List<String> rootStr = new ArrayList<String>();
		
		for (String lhs : exprTable.keySet()) {
			if (exprTable.get(lhs).toString().contains(UnaryOp.PRE + " ")) {
				rootStr.add(lhs);
			}
		}
		
		return rootStr;
	}
	
	/*
	 * re-organize expressions of given node with hashtable
	 */
	private HashMap<String, Expr> getStrExprTable(Node node) {
		HashMap<String, Expr> exprTable = new HashMap<String, Expr>();
		List<Equation> equations = node.equations;
		
		/*
		 * put expressions of given node into a hashmap<id, expr>
		 */
		for (Equation equation : equations) {
			exprTable.put(equation.lhs.get(0).id, equation.expr);
		}
		
		return exprTable;
	}
	
}

