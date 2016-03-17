package coverage;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.Expr;
import jkind.lustre.IdExpr;

public final class Obligation {
	// Mapping from an arithmetic expression to its name
	protected static final Map<String, String> arithExprByExpr = new HashMap<String, String>();
	// Mapping from a name to an arithmetic expression
	protected static final Map<String, String> arithExprById = new HashMap<String, String>();

	protected final String condition;
	// Polarity of the current condition
	protected final boolean polarity;
//	protected boolean polarity;
	// The obligation as an expression
	protected Expr obligation;
	// Polarity of the expression that uses the current condition
	protected boolean expressionPolarity;

	public Obligation(Expr condition, boolean polarity, Expr obligation) {
		if (condition instanceof IdExpr) {
			this.condition = condition.toString();
		} else {
			this.condition = this.createArithVar(condition);
		}
		this.polarity = polarity;
		this.obligation = obligation;
		this.expressionPolarity = polarity;
	}
	
//	public Obligation(Expr condition) {
//		this.condition = condition.toString();
//	}

	// Create a variable for an arithmetic expression
	private String createArithVar(Expr expr) {
		String exprStr = expr.toString();
		if (arithExprByExpr.get(exprStr) == null) {
			String arithExprId = "ArithExpr_" + arithExprByExpr.size();
			arithExprByExpr.put(exprStr, arithExprId);
			arithExprById.put(arithExprId, exprStr);
		}
		return arithExprByExpr.get(exprStr);
	}

	public String toString() {
		return this.obligation.toString();
	}
}
