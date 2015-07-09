package coverage;

import java.util.List;

import enums.Coverage;
import enums.Polarity;
import main.LustreMain;
import types.ExprTypeVisitor;
import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;

public class LustreCoverage {
	// By default, use polarity ALL
	public static Program program(Program program, Coverage coverage) {
		return program(program, coverage, Polarity.ALL);
	}

	public static Program program(Program program, Coverage coverage,
			Polarity polarity) {
		// Remove XOR and boolean equality/inequality
		return new LustreCoverage(LustreCleanVisitor.program(program),
				coverage, polarity).generate();
	}

	private final Program program;
	private final Coverage coverage;
	private final Polarity polarity;
	private final ExprTypeVisitor exprTypeVisitor;
	private int count;

	private LustreCoverage(Program program, Coverage coverage, Polarity polarity) {
		this.program = program;
		this.coverage = coverage;
		this.polarity = polarity;
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.count = 0;
	}

	private Program generate() {
		String coverageType = coverage.name();

		LustreMain.log("------------Generating " + coverageType
				+ " obligations");

		ProgramBuilder builder = new ProgramBuilder();
		builder.addConstants(this.program.constants)
				.addTypes(this.program.types).setMain(this.program.main);
		for (Node node : this.program.nodes) {
			builder.addNode(this.generate(node));
		}

		LustreMain.log("Number of Obligations: " + count);
		return builder.build();
	}

	// Generate obligations for a node
	private Node generate(Node node) {
		LustreMain.log("Node: " + node.id);

		NodeBuilder builder = new NodeBuilder(node);

		CoverageVisitor coverageVisitor = null;
		this.exprTypeVisitor.setNodeContext(node);

		// Determine the coverage type
		switch (coverage) {
		case MCDC:
			coverageVisitor = new MCDCVisitor(exprTypeVisitor);
			break;
		case BRANCH:
			coverageVisitor = new BranchVisitor(exprTypeVisitor);
			break;
		case CONDITION:
			coverageVisitor = new ConditionVisitor(exprTypeVisitor);
			break;
		case DECISION:
			coverageVisitor = new DecisionVisitor(exprTypeVisitor);
			break;
		default:
			throw new IllegalArgumentException("Unknown coverage: " + coverage);
		}

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

			List<Obligation> obligations = equation.expr
					.accept(coverageVisitor);

			for (Obligation obligation : obligations) {
				// Skip if the expression's polarity is different from what
				// we need
				if (polarity.equals(Polarity.TRUE)
						&& !obligation.expressionPolarity) {
					continue;
				}
				if (polarity.equals(Polarity.FALSE)
						&& obligation.expressionPolarity) {
					continue;
				}

				String property = obligation.condition + "_"
						+ (obligation.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_" + coverage.name() + "_"
						+ (obligation.expressionPolarity ? "TRUE" : "FALSE")
						+ "_" + (count++);

				builder.addLocal(new VarDecl(property, NamedType.BOOL));
				builder.addEquation(new Equation(new IdExpr(property),
						new UnaryExpr(UnaryOp.NOT, obligation.obligation)));
				builder.addProperty(property);
			}
		}
		return builder.build();
	}
}
