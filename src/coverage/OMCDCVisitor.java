package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import enums.ObservedTreeType;
import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class OMCDCVisitor extends MCDCVisitor {
	private ObservedTreeType treeType;
	private List<Node> nodes;
	private List<ObservedTree> referenceTrees;
	private List<ObservedTree> sequentialTrees;
	private ObservedVisitorHelper obHelper;
	int step = 0;
	
//	public OMCDCVisitor(ExprTypeVisitor exprTypeVisitor) {
//		super(exprTypeVisitor);
//	}
	
	public OMCDCVisitor(ExprTypeVisitor exprTypeVisitor, List<Node> nodes) {
		super(exprTypeVisitor);
		this.nodes = nodes;
		obHelper = new ObservedVisitorHelper(nodes);
		referenceTrees = obHelper.buildRefTreesForInput();
//		sequentialTrees = obHelper.buildSequentialTrees();
	}
	
	//TODO: implement visit's
}
