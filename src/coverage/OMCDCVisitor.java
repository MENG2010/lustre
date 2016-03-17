package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import enums.Polarity;
import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class OMCDCVisitor extends CoverageVisitor {
	List<Node> nodes;
	ObservedCoverageHelper obHelper;
	MCDCVisitor mcdcVisitor;
	
	String addition = "_COMB_USED_BY_";
	String property = "";
	IdExpr parent;
	HashMap<String, VarDecl> idList;
	List<String> properties = new ArrayList<String>();
	int count = 0;
	
	public OMCDCVisitor(ExprTypeVisitor exprTypeVisitor, List<Node> nodes) {
		super(exprTypeVisitor);
		this.nodes = nodes;
		this.obHelper = new ObservedCoverageHelper(nodes);
	}
	
	// main entrance to get OMCDC obligations
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<Obligation>();
		obligations.addAll(getMCDCObligation(exprTypeVisitor));
		obligations.addAll(generateCombObervedExpr());
		obligations.addAll(generateDelayDependencyExpr());
		
		obligations.addAll(generateTokenActions());
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> obligations = new ArrayList<Obligation>();
		
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);
		
//		System.out.println("for expression: " + expr.toString());
		
		// and, or
		if (expr.op.equals(BinaryOp.AND)) {
			//TODO: does not function! need to fix.
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = expr.right;
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = expr.left;
			}
			
		} else if (expr.op.equals(BinaryOp.OR)) {
			//TODO: does not function! need to fix.
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.right);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.left);
			}
		} 
		
		// >, >=, <, <=, ==, !=
		// +, -, *, /, div, %
		else if (expr.op.equals(BinaryOp.GREATER)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)
				|| expr.op.equals(BinaryOp.PLUS)
				|| expr.op.equals(BinaryOp.MINUS)
				|| expr.op.equals(BinaryOp.MULTIPLY)
				|| expr.op.equals(BinaryOp.DIVIDE)
				|| expr.op.equals(BinaryOp.INT_DIVIDE)
				|| expr.op.equals(BinaryOp.MODULUS)) {
			
			if (expr.left instanceof IdExpr) {
				obligations.add(new Obligation(expr.left, true, new BoolExpr(true)));
			}
			
			if (expr.right instanceof IdExpr) {
				obligations.add(new Obligation(expr.right, true, new BoolExpr(true)));
			}
			
			if (!(expr.left instanceof IdExpr || expr.right instanceof IdExpr)) {
				obligations.add(new Obligation(expr, true, new BoolExpr(true)));
			}
			
		} 
		
		// a => b
		// !a or b
		else if (expr.op.equals(BinaryOp.IMPLIES)) {
			//TODO: does not function! need to fix.
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = expr.right;
			}
			for (Obligation rightOb : rightObs) {

				rightOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.left);
			}				
		}
		
		// a -> b
		else if (expr.op.equals(BinaryOp.ARROW)) {
			Expr currentObligation;
			Expr left;
			
			if (expr.right instanceof IdExpr) {
				// a -> b
				if (expr.left instanceof BoolExpr 
						|| expr.left instanceof IdExpr) {
					left = expr.left;
				} else {
					left = new BoolExpr(false);
				}
				currentObligation = new BinaryExpr(left, BinaryOp.ARROW, 
						new BoolExpr(true));
				obligations.add(new Obligation(expr.right, true, currentObligation));
			} else if (expr.right instanceof UnaryExpr) {
				if (expr.left instanceof BoolExpr
						|| expr.left instanceof IdExpr) {
					left = expr.left;
				} else {
					left = new BoolExpr(false);
				}
				
				if (((UnaryExpr) expr.right).op.equals(UnaryOp.NOT)) {
					// a -> (not b)
					currentObligation = new BinaryExpr(left, BinaryOp.ARROW, 
							new BoolExpr(true));
					obligations.add(new Obligation(((UnaryExpr) expr.right).expr,
							true, currentObligation));
				} else if (((UnaryExpr) expr.right).op.equals(UnaryOp.PRE)) {
					// a -> (pre b)
					currentObligation = new BoolExpr(false);
					obligations.add(new Obligation(((UnaryExpr) expr.right).expr,
							true, currentObligation));
				}
			} else {
				// a -> (expression)
				if (expr.left instanceof BoolExpr
						|| expr.left instanceof IdExpr) {
					left = expr.left;
				} else {
					left = new BoolExpr(false);
				}
				
				currentObligation = new BinaryExpr(left, BinaryOp.ARROW, 
						new BoolExpr(true));
				obligations.add(new Obligation(expr, true, currentObligation));
			}
		}
		
		// xor
		else if (expr.op.equals(BinaryOp.XOR)) {
			throw new IllegalArgumentException(
					"XOR should have been translated.");
		}
		
		obligations.addAll(leftObs);
		obligations.addAll(rightObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> obligations = new ArrayList<Obligation>();
		obligations.addAll(expr.cond.accept(this));
		List<Obligation> thenObs = expr.thenExpr.accept(this);
		List<Obligation> elseObs = expr.elseExpr.accept(this);
		List<Obligation> condObs = expr.cond.accept(this);
		
		for (Obligation thenOb : thenObs) {
			thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND, thenOb.obligation);
		}
		
		for (Obligation elseOb : elseObs) {
			elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT, expr.cond),
					BinaryOp.AND, elseOb.obligation);
		}
		
		for (Obligation condOb : condObs) {
		}
		obligations.addAll(thenObs);
		obligations.addAll(elseObs);
		
		return obligations;
	}
	
	// get MCDC obligations (without prefix NOTs), using original APIs
	private List<Obligation> getMCDCObligation(ExprTypeVisitor exprTypeVisitor) {
		mcdcVisitor = new MCDCVisitor(exprTypeVisitor);
		List<Obligation> obligations = new ArrayList<Obligation>();
		for (Equation equation : nodes.get(0).equations) {
			List<Obligation> obs = equation.expr.accept(mcdcVisitor);
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
			
			String property;
			for (Obligation ob : obs) {
				property = ob.condition + "_"
						+ (ob.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_MCDC_"
						+ (ob.expressionPolarity ? "TRUE" : "FALSE")
						+ "_" + (count++);
				properties.add(property);
				Obligation currentOb = new Obligation(new IdExpr(property), false, ob.obligation);
				obligations.add(currentOb);
			}
		}
		
		return obligations;
	}
	
	// generate COMB_OBSERVED expressions
	private List<Obligation> generateCombObervedExpr() {
		CombObservedEquation combObsEquation = new CombObservedEquation();
		return combObsEquation.generate(obHelper.buildRefTreesForInput());
		
	}
	// generate SEQ_USED_BY expressions
	private List<Obligation> generateDelayDependencyExpr() {
		SequentialEquation delayDepdnEquation = new SequentialEquation();
		return delayDepdnEquation.generate(obHelper.buildSeqTreesForInput());
	}
	
	// generate TOKEN Actions
	private List<Obligation> generateTokenActions() {
		TokenAction tokenAction = new TokenAction(obHelper.buildSeqTreesForInput());
		return tokenAction.generate();
	}
	
	//TODO: generate affecting_at_capture expressions
	
	//TODO: generate obligations
}
