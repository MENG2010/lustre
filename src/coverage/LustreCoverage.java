package coverage;

import java.util.List;

import enums.Coverage;
import enums.Polarity;
import main.LustreMain;
import types.ExprTypeVisitor;
import jkind.lustre.BoolExpr;
import jkind.lustre.Constant;
import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;

public final class LustreCoverage {
	int upperbound = 0;
	
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
		

		if (coverage.equals(Coverage.OMCDC)) {
			// add more constants
			Type type = new NamedType("subrange[-2," + upperbound + "] of int");
			Constant constDemo = new Constant("testing", NamedType.BOOL, new BoolExpr(true));
			builder.addConstant(new Constant("TOKEN_INIT_STATE", type, new IntExpr(-2)));
			builder.addConstant(new Constant("TOKEN_ERROR_STATE", type, new IntExpr(-1)));
			builder.addConstant(new Constant("TOKEN_OUTPUT_STATE", type, new IntExpr(-0)));
			builder.addConstant(constDemo);
			
			System.out.println(constDemo.id + ", " + constDemo.type.toString() + ", " + constDemo.expr.toString());
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
		case OMCDC: // for OMCDC coverage. Meng
			coverageVisitor = new OMCDCVisitor(exprTypeVisitor, program.nodes); 
			break;
		case OBRANCH:
			break;
		case OCONDITION:
			break;
		case ODECISION:
			break;
		default:
			throw new IllegalArgumentException("Unknown coverage: " + coverage);
		}
		
		// Start generating obligations
		// non-observed & comb_used_by obligations
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

				String property = "";
				if (coverage == coverage.OMCDC) {
					// name pattern for OMCDC. Meng
					count++; // counting obligations
					property = obligation.condition + "_COMB_USED_BY_" + id;
					builder.addEquation(new Equation(new IdExpr(property), obligation.obligation));
				} else {
					// keep the rest in original pattern.
					property = obligation.condition + "_"
						+ (obligation.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_" + coverage.name() + "_"
						+ (obligation.expressionPolarity ? "TRUE" : "FALSE")
						+ "_" + (count++);
					
					builder.addEquation(new Equation(new IdExpr(property),
							new UnaryExpr(UnaryOp.NOT, obligation.obligation)));
				}

				builder.addLocal(new VarDecl(property, NamedType.BOOL));
				builder.addProperty(property);
			}
		}
		
		if (coverage == coverage.OMCDC) {
			// obligations for observed coverage only. Meng
			String property = "";
			
			List<Obligation> obligations = ((OMCDCVisitor) coverageVisitor).generate();
			count += obligations.size();
			upperbound = ((OMCDCVisitor) coverageVisitor).getTokenRange();
			StringBuilder subrange = new StringBuilder();
			subrange.append("subrange");
			
			if (upperbound > 0) {
				// add local token definition if there is any
				subrange.append("[").append("1,").append(upperbound).append("]");
				builder.addInput(new VarDecl("token_nondet", new NamedType(subrange.toString() + " of int")));
				builder.addInput(new VarDecl("token_init", NamedType.BOOL));
				subrange.delete(subrange.indexOf("["), subrange.length());
			}
			subrange.append("[").append("-2,").append(upperbound).append("]");
			
			for (Obligation obligation : obligations) {
				property = obligation.condition;
				builder.addEquation(new Equation(new IdExpr(property), obligation.obligation));
				if (property.contains("token_")) {
					builder.addLocal(new VarDecl(property, new NamedType(subrange.toString() + " of int")));
				} else if (!property.contains("token")){
					builder.addLocal(new VarDecl(property, NamedType.BOOL));
				}
				builder.addProperty(property);
			}
			
		}
		
		return builder.build();
	}
}
