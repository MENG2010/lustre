package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

/**
 * Generate obligations for MC/DC
 */
 public final class MCDCVisitor extends ConditionVisitor {
	public MCDCVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);

		// AND ("and")
		if (expr.op.equals(BinaryOp.AND)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.AND, expr.right);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(expr.left, BinaryOp.AND,
						rightOb.obligation);
			}
		}
		// OR ("or")
		else if (expr.op.equals(BinaryOp.OR)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
						expr.left), BinaryOp.AND, rightOb.obligation);
			}
		}
		// IMPLIES ("=>")
		// a => b
		// !a or b
		else if (expr.op.equals(BinaryOp.IMPLIES)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(expr.left, BinaryOp.AND,
						rightOb.obligation);
			}
		}
		// XOR ("xor")
		else if (expr.op.equals(BinaryOp.XOR)) {
			throw new IllegalArgumentException(
					"XOR should have been translated.");
		}
		// GREATER (">")
		// LESS ("<")
		// GREATEREQUAL (">=")
		// LESSEQUAL ("<=")
		// EQUAL ("=")
		// NOTEQUAL ("<>")
		else if (expr.op.equals(BinaryOp.GREATER)
				|| expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)) {
			// The expression itself is a boolean expression and thus a
			// condition
			currentObs.add(new Obligation(expr, true, expr));
			currentObs.add(new Obligation(expr, false, new UnaryExpr(
					UnaryOp.NOT, expr)));
		}
		// ARROW ("->")
		else if (expr.op.equals(BinaryOp.ARROW)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.ARROW, new BoolExpr(false));
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(new BoolExpr(false),
						BinaryOp.ARROW, rightOb.obligation);
			}
		}
		// PLUS ("+")
		// MINUS ("-")
		// MULTIPLY ("*")
		// DIVIDE ("/")
		// INT_DIVIDE ("div")
		// MODULUS ("mod")

		currentObs.addAll(leftObs);
		currentObs.addAll(rightObs);

		return currentObs;
	}
}
