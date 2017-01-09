package coverage;

import enums.Coverage;
import enums.Polarity;
import types.ExprTypeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.VarDecl;
import observability.AffectAtCaptureEquation;
import observability.CombObservedEquation;
import observability.DelayVisitor;
import observability.NodeCallVisitor;
import observability.ObservabilityObligation;
import observability.ObserverVisitor;
import observability.SequentialUsedEquation;
import observability.TokenAction;
import observability.TreeBuilder;
import observability.VariableVisitor;
import observability.tree.Tree;
import observability.tree.TreeNode;

public final class ObservabilityCoverage {
	private final Node node;
	private final Coverage coverage;
	private final ExprTypeVisitor exprTypeVisitor;
	private final Polarity polarity;
	private ObservabilityVisitor observabilityVisitor;
	private int count;
	
	private Map<String, List<String>> delayTable = new HashMap<>();
	private Map<String, List<String>> observerTable = new HashMap<>();
	/* <lhs_of_equation, <raw_var_in_rhs, <renamed_var, time_seen>>> */
	private Map<String, Map<String, Map<String, Integer>>> observerArithMap = new HashMap<>();
	/* <lhs_of_equation, <raw_var_in_rhs, <renamed_var, time_seen>>> */
	private Map<String, Map<String, Map<String, Integer>>> delayArithMap = new HashMap<>();
	
	private Map<String, VarDecl> ids = new HashMap<>();
	private List<VarDecl> decls = new ArrayList<>();
	private List<String> strIds = new ArrayList<>();
	private List<String> outputs = new ArrayList<>();
	private List<String> inputs = new ArrayList<>();
	
	private Map<String, Tree> observerTrees = new HashMap<>();
	private Map<String, Tree> delayTrees = new HashMap<>();
	private Map<String, Tree> deadNodeTrees = new HashMap<>();
	private List<String> deadNodes = new ArrayList<>();
	private Map<String, Map<String, Integer>> affectAtCaptureTable = new TreeMap<>();
	
	// node calls in a node
	private Map<String, Type> nodecalls = new HashMap<>();
	
	// dynamic tokens
	private final String prefix = "TOKEN_D";
	private IdExpr[] tokens;
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	private Map<TreeNode, List<TreeNode>> tokenDepTable = new HashMap<>();
	// token to tree node (root), Map<Token, Node>
	private Map<IdExpr, TreeNode> tokenToNode = new HashMap<>();
	// tree node (root) to token, Map<Node, Token>
	private Map<TreeNode, IdExpr> nodeToToken = new HashMap<>();
	
	private Map<String, List<String>> affectPairs = new HashMap<>();
	
	public ObservabilityCoverage(Node node, Coverage coverage, 
				Polarity polarity, ExprTypeVisitor exprTypeVisitor) {
		this.node = node;
		this.coverage = coverage;
		this.polarity = polarity;
		this.exprTypeVisitor = exprTypeVisitor;
		this.observabilityVisitor = new ObservabilityVisitor(exprTypeVisitor);
		this.count = 0;
		populateMaps();
	}

	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		
		obligations.addAll(generateNonObservedEquations());
		obligations.addAll(generateObservedEquations());
		
		buildTrees();
		drawTokenMaps();
		drawTokenDependantTable();
		
		obligations.addAll(generateCombObervedEquations());
		obligations.addAll(generateSeqUsedByEquations());
		obligations.addAll(generateTokenActions());
		obligations.addAll(generateAffectAtCaptureEquations()); 
		obligations.addAll(generateObligations());
		
		return obligations;
	}
		
	// generate obligations for observability coverage
	private List<Obligation> generateObservedEquations() {
		List<Obligation> obligations = new ArrayList<>();
		
		// Start generating obligations
		for (Equation equation : node.equations) {
			String id = null;
			
			if (equation.lhs.isEmpty()) {
				id = "EMPTY";
			} else {
				id = equation.lhs.get(0).id;
			}

			// Concatenate IDs with more than one left-hand variables
			for (int i = 1; i < equation.lhs.size(); i++) {
				id += "_" + equation.lhs.get(i);
			}
			
			// A = B; or A = (UnaryOp B);
			if (equation.expr instanceof IdExpr
					|| equation.expr instanceof NodeCallExpr
					|| ((equation.expr instanceof UnaryExpr)
							&& ((UnaryExpr)equation.expr).expr instanceof IdExpr)) {
				observabilityVisitor.setIsDef(true);
			} else {
				observabilityVisitor.setIsDef(false);
			}
			
			// obligations/expressions generated by visitors
			List<Obligation> observabilityObs = equation.expr
					.accept(observabilityVisitor);
			
			HashMap<String, Expr> map = new HashMap<>();
			
			for (Obligation ob : observabilityObs) {				
				if (polarity.equals(Polarity.TRUE)
						&& !ob.expressionPolarity) {
					continue;
				}
				if (polarity.equals(Polarity.FALSE)
						&& ob.expressionPolarity) {
					continue;
				}
				
				String lhs = ob.condition + "_COMB_USED_BY_" + id;
				count++;
				
				Expr expr = ob.obligation;
				
				if (!map.containsKey(lhs)) {
					map.put(lhs, expr);
				} else if (!map.get(lhs).toString().contains(expr.toString())) {
					expr = new BinaryExpr(map.get(lhs), BinaryOp.OR, expr);
					map.put(lhs, expr);
				}
			}
			
			for (String lhs : map.keySet()) {
				obligations.add(new Obligation(new IdExpr(lhs), true, map.get(lhs)));
			}
		}
				
		return obligations;
	}

	private List<Obligation> generateNonObservedEquations() {
		CoverageVisitor coverageVisitor = null;
		List<Obligation> obligations = new ArrayList<>();
		String property;
		
		switch (coverage) {
		case OMCDC:
			coverageVisitor = new MCDCVisitor(exprTypeVisitor);
			break;
		case OCONDITION:
			coverageVisitor = new ConditionVisitor(exprTypeVisitor);
			break;
		case OBRANCH:
			coverageVisitor = new BranchVisitor(exprTypeVisitor);
			break;
		case ODECISION:
			coverageVisitor = new DecisionVisitor(exprTypeVisitor);
			break;
		default:
			throw new IllegalArgumentException("Incorrect coverage: " + coverage);
		}
		
		for (Equation equation : node.equations) {
			List<Obligation> obs = equation.expr.accept(coverageVisitor);
			List<Obligation> updatedObs = new ArrayList<>();
			
			String id = null;
			/* for rename: <renamedId, timeSeen>*/
			Map<String, Integer> conditions = new TreeMap<>();
			
			if (equation.lhs.isEmpty()) {
				id = "EMPTY";
			} else {
				id = equation.lhs.get(0).id;
			}

			// Concatenate IDs with more than one left-hand variables
			for (int i = 1; i < equation.lhs.size(); i++) {
				id += "_" + equation.lhs.get(i);
			}
			
			for (Obligation ob : obs) {
				if (ob.condition.toLowerCase().startsWith("token_")) {
					// skip equations of "token_something"
					continue;
				}
				updatedObs.add(ob);
			}
			
			// count occurrence of each var (ob.condition) in the equation, for renaming
			for (Obligation ob : updatedObs) {
				
				if (conditions.containsKey(ob.condition)) {
					conditions.put(ob.condition, conditions.get(ob.condition) + 1);
				} else {
					conditions.put(ob.condition, 1);
				}
			}
			
			HashMap<String, Integer> handledList = new HashMap<>();
			int i;
			
			// generate obligations
			for (Obligation ob : updatedObs) {
				i = 0;
				
				int occ = conditions.get(ob.condition);
					property = ob.condition;
				
					if (occ > 2) {
						if (handledList.containsKey(ob.condition)) {
							i = handledList.get(ob.condition) + 1;
						}
						property = property + "_" + (i / 2);
					}
					
					handledList.put(ob.condition, i);
					
					property = property + "_"
							+ (ob.polarity ? "TRUE" : "FALSE") + "_AT_"
							+ id + "_" + coverage.name() + "_"
							+ (ob.polarity ? "TRUE" : "FALSE");
					
					Obligation currentOb = new Obligation(new IdExpr(property), true, ob.obligation);
					obligations.add(currentOb);
			}
			
			// populate map
			affectAtCaptureTable.put(id, conditions);
		}
		
		return obligations;
	}
		
	// generate COMB_OBSERVED expressions
	private List<Obligation> generateCombObervedEquations() {
		return CombObservedEquation.generate(this.observerTrees, 
										this.delayTrees,
										this.deadNodes, this.nodecalls.keySet());
	}
	
	// generate SEQ_USED_BY expressions
	private List<Obligation> generateSeqUsedByEquations() {
		return SequentialUsedEquation.generate(this.delayTrees);
	}
	
	// generate TOKEN Actions
	private List<Obligation> generateTokenActions() {
		return TokenAction.generate(this.delayTrees, this.tokenDepTable,
								this.tokenToNode, this.nodeToToken, this.tokens);
	}
	
	// generate affecting_at_capture expressions
	private List<Obligation> generateAffectAtCaptureEquations() {
		AffectAtCaptureEquation affect = new AffectAtCaptureEquation(delayTrees,
							observerTrees, delayTable, affectAtCaptureTable,
							coverage, tokenDepTable, nodeToToken);
		affect.setDeadNodes(deadNodes);
		affect.setSingleNodeTrees(deadNodeTrees);
		
		List<Obligation> affectObligations = new ArrayList<>();
		affectObligations.addAll(affect.generate());
		
		// get affect pairs for final obligations generation
		affectPairs = affect.getAffectPairs();
		
		return affectObligations;
	}
	
	// generate observability obligations for each expression
	private List<Obligation> generateObligations() {
		return ObservabilityObligation.generate(this.affectAtCaptureTable,
								this.affectPairs, this.coverage);
	}
	
	/* ******************************************************
	 * 			utilities
	 * ****************************************************** 
	 */
	
	private void populateMaps() {
		populateIds(ids);
		getNodeCallList();
		populateStrIds(strIds);
		populateOutputs(outputs);
		populateInputs(inputs);
		populateDecls(decls);
	}
	
	private void getNodeCallList() {
		this.nodecalls.putAll(NodeCallVisitor.get(exprTypeVisitor, node));
	}

	private void buildTrees() {
		populateDelayTable(delayTable);
		populateObserverTable(observerTable);
		
		populateArithMaps();
		
		TreeBuilder builder = new TreeBuilder(this.observerArithMap, 
											this.delayArithMap,
											Obligation.arithExprById,
											this.nodecalls, this.ids, this.outputs);
		
		observerTrees = builder.buildObserverTree();
		delayTrees = builder.buildDelayTree();
		
		populateDeadNodes(this.deadNodes);
		deadNodeTrees = builder.buildDeadRootTree(this.deadNodes);
	}
	
	private void populateDeadNodes(List<String> deadNodes) {
		Set<String> reachedSet = new HashSet<>();
		Set<String> handled = new HashSet<>();
		
		Map<String, Expr> exprTable = getExprTable();
		
		for (String root : outputs) {
			if (exprTable.get(root) == null) {
				continue;
			} else {
				VariableVisitor visitor = new VariableVisitor(exprTypeVisitor);
				reachedSet.add(root);
				
				for (String lhs : reachedSet) {
					if (reachedSet.size() == handled.size()) {
						break;
					}
					if (handled.contains(lhs)) {
						continue;
					}
					reachedSet.addAll(exprTable.get(lhs).accept(visitor));
					handled.add(lhs);
				}
			}
		}
		
		for (String id : strIds) {
			if (! reachedSet.contains(id) && ! id.startsWith("token")) {
				deadNodes.add(id);
			}
		}		
	}
	
	private void populateIds(Map<String, VarDecl> ids) {
		for (VarDecl decl : node.inputs) {
			ids.put(decl.id, decl);
		}
		
		for (VarDecl decl : node.outputs) {
			ids.put(decl.id, decl);
		}
		
		for (VarDecl decl : node.locals) {
			ids.put(decl.id, decl);
		}
	}

	private void populateDecls(List<VarDecl> decls) {
		decls.addAll(node.inputs);
		decls.addAll(node.outputs);
		decls.addAll(node.locals);
	}
	
	private void populateStrIds(List<String> strIds) {
		for (String id : ids.keySet()) {
			strIds.add(id);
		}
	}

	private void populateOutputs(List<String> outputs) {
		for (VarDecl decl : node.outputs) {
			outputs.add(decl.id);
		}
	}
	
	private void populateInputs(List<String> inputs) {
		for (VarDecl decl : node.inputs) {
			inputs.add(decl.id);
		}
	}

	private void populateDelayTable(Map<String, List<String>> delayTable) {
		DelayVisitor visitor = new DelayVisitor(exprTypeVisitor);
		
		for (Equation equation : node.equations) {
			String rhs = equation.expr.toString().toLowerCase();
			
			if (rhs.contains("pre ")) {
				List<String> list = new ArrayList();
				list.addAll(equation.expr.accept(visitor));
				if (list != null && !list.isEmpty()) {
					delayTable.put(equation.lhs.get(0).id, list);
				}
			}
		}
		
	}
	
	private void populateObserverTable(Map<String, List<String>> observerTable) {
		ObserverVisitor visitor = new ObserverVisitor(exprTypeVisitor);
		
		for (Equation equation : node.equations) {
			observerTable.put(equation.lhs.get(0).id, equation.expr.accept(visitor));
		}
	}
	
	private Map<String, Expr> getExprTable() {
		Map<String, Expr> exprTable = new HashMap<>();
		
		for (Equation equation : node.equations) {
			exprTable.put(equation.lhs.get(0).id, equation.expr);
		}
		
		return exprTable;
	}
	
	private void populateArithMaps() {
		Map<String, Expr> exprTable = getExprTable();
		
		for (String lhs : observerTable.keySet()) {
			Map<String, Map<String, Integer>> map = new HashMap<>();
			String rhs = exprTable.get(lhs).toString();
			
			for (String id : observerTable.get(lhs)) {
				Map<String, Integer> ariths = new HashMap<>();
				
				for (String arithExpr : Obligation.arithExprByExpr.keySet()) {
					if (rhs.contains(arithExpr) && arithExpr.equals(id)) {
						String arith = Obligation.arithExprByExpr.get(arithExpr);
						if (ariths.containsKey(arith)) {
							ariths.put(arith, ariths.get(arith) + 1);
						} else {
							ariths.put(Obligation.arithExprByExpr.get(arithExpr), 1);
						}
					}
				}
				
				if (ariths.isEmpty()) {
					for (String strId : observerTable.get(lhs)) {
						if (! strId.equals(id)) {
							continue;
						}
						if (ariths.containsKey(id)) {
							ariths.put(id, ariths.get(id) + 1);
						} else {
							ariths.put(id, 1);
						}
					}
				}
				
				map.put(id, ariths);
			}
						
			observerArithMap.put(lhs, map);
		}
		
		for (String lhs : delayTable.keySet()) {
			Map<String, Map<String, Integer>> map = new HashMap<>();
			String rhs = exprTable.get(lhs).toString();
			
			for (String id : delayTable.get(lhs)) {
				Map<String, Integer> ariths = new HashMap<>();
				for (String arithExpr : Obligation.arithExprByExpr.keySet()) {
					if (rhs.contains(arithExpr) && arithExpr.equals(id)) {
						String arith = Obligation.arithExprByExpr.get(arithExpr);
						if (ariths.containsKey(arith)) {
							ariths.put(arith, ariths.get(arith) + 1);
						} else {
							ariths.put(Obligation.arithExprByExpr.get(arithExpr), 1);
						}
					}
				}
				
				if (ariths.isEmpty()) {
					for (String strId : delayTable.get(lhs)) {
						if (! strId.equals(id)) {
							continue;
						}
						if (ariths.containsKey(id)) {
							ariths.put(id, ariths.get(id) + 1);
						} else {
							ariths.put(id, 1);
						}
					}
				}
				map.put(id, ariths);
			}
			delayArithMap.put(lhs, map);
		}
		
	}
	
	public int getTokenRange() {
		return this.delayArithMap.keySet().size();
	}
	
	// build token-to-node, node-to-token maps
	private void drawTokenMaps() {
		tokens = new IdExpr[delayTrees.size()];
		int count = 0;
		
		for (String tree : delayTrees.keySet()) {
			tokens[count] = new IdExpr(prefix + (count + 1));
			TreeNode node = delayTrees.get(tree).root;
			tokenToNode.put(tokens[count], node);
			nodeToToken.put(node, tokens[count]);

			count++;
		}
	}
		
	public void drawTokenDependantTable() {
		for (TreeNode node : nodeToToken.keySet()) {
			List<TreeNode> list = new ArrayList<>();
			for (String rootStr : delayTrees.keySet()) {
				Tree delayTree = delayTrees.get(rootStr);
				for (TreeNode child : delayTree.root.children) {
					if (child.containsNode(node.rawId)) {
						list.add(delayTrees.get(rootStr).root);
					}
				}
			}
			tokenDepTable.put(node, list);
		}
	} 
}
