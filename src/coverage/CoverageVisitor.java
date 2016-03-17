package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RealExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.TupleExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.ExprVisitor;

public class CoverageVisitor implements ExprVisitor<List<Obligation>> {
	protected final ExprTypeVisitor exprTypeVisitor;

	public CoverageVisitor(ExprTypeVisitor exprTypeVisitor) {
		this.exprTypeVisitor = exprTypeVisitor;
	}
	

	@Override
	public List<Obligation> visit(ArrayAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();
		
//		System.out.println("ArrayAccessExpr >>> " + expr.toString());
		
		currentObs.addAll(expr.array.accept(this));
		currentObs.addAll(expr.index.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(ArrayExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();
//		System.out.println("ArrayExpr >>> " + expr.toString());
		for (Expr e : expr.elements) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(ArrayUpdateExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();
//		System.out.println("ArrayUpdateExpr >>> " + expr.toString());
		currentObs.addAll(expr.array.accept(this));
		currentObs.addAll(expr.index.accept(this));
		currentObs.addAll(expr.value.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();
//		System.out.println("BinaryExpr >>> " + expr.toString());
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);

		// Add the ARROW operator to the obligation
		if (expr.op.equals(BinaryOp.ARROW)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.ARROW, new BoolExpr(false));
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(new BoolExpr(false),
						BinaryOp.ARROW, rightOb.obligation);
			}
		}

		currentObs.addAll(leftObs);
		currentObs.addAll(rightObs);

		return currentObs;
	}

	@Override
	public List<Obligation> visit(BoolExpr expr) {
//		System.out.println("BoolExpr >>> " + expr.toString());
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(CastExpr expr) {
//		System.out.println("CastExpr >>> " + expr.toString());
		return expr.expr.accept(this);
	}

	@Override
	public List<Obligation> visit(CondactExpr expr) {
//		System.out.println("CondactExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.clock.accept(this));
		currentObs.addAll(expr.call.accept(this));

		for (Expr e : expr.args) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(IdExpr expr) {
//		System.out.println("IdExpr >>> " + expr.toString());
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
//		System.out.println("IfThenElseExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.cond.accept(this));

		List<Obligation> thenObs = expr.thenExpr.accept(this);
		List<Obligation> elseObs = expr.elseExpr.accept(this);

		// Add true if condition to the then branch obligation
		for (Obligation thenOb : thenObs) {
			thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND,
					thenOb.obligation);
		}

		// Add false if condition to the else branch obligation
		for (Obligation elseOb : elseObs) {
			elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
					expr.cond), BinaryOp.AND, elseOb.obligation);
		}

		currentObs.addAll(thenObs);
		currentObs.addAll(elseObs);

		return currentObs;
	}

	@Override
	public List<Obligation> visit(IntExpr expr) {
//		System.out.println("IntExpr >>> " + expr.toString());
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
//		System.out.println("NodeCallExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.args) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RealExpr expr) {
//		System.out.println("RealExpr >>> " + expr.toString());
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(RecordAccessExpr expr) {
//		System.out.println("RecordAccessExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.record.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RecordExpr expr) {
//		System.out.println("RecordExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.fields.values()) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RecordUpdateExpr expr) {
//		System.out.println("RecordUpdateExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.record.accept(this));
		currentObs.addAll(expr.value.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(TupleExpr expr) {
//		System.out.println("TupleExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.elements) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(UnaryExpr expr) {
//		System.out.println("UnaryExpr >>> " + expr.toString());
		List<Obligation> currentObs = new ArrayList<Obligation>();

		List<Obligation> unaryObs = expr.expr.accept(this);

		for (Obligation unaryOb : unaryObs) {
			// Negate expression polarity for NOT operator
			if (expr.op.equals(UnaryOp.NOT)) {
				unaryOb.expressionPolarity = !unaryOb.expressionPolarity;
			}
			// Add the unary operator to the obligation
			else {
				unaryOb.obligation = new UnaryExpr(expr.op, unaryOb.obligation);
			}
		}

		currentObs.addAll(unaryObs);

		return currentObs;
	}
}
