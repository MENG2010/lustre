package types;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayType;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Constant;
import jkind.lustre.EnumType;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RealExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordType;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.TupleExpr;
import jkind.lustre.TupleType;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.ExprVisitor;
import jkind.lustre.visitors.TypeMapVisitor;
import jkind.util.Util;

/**
 * Get type of an expression.
 */
public final class ExprTypeVisitor implements ExprVisitor<Type> {
	private final Map<String, Type> typeTable = new HashMap<>();
	private final Map<String, Type> constantTable = new HashMap<>();
	private final Map<String, EnumType> enumValueTable = new HashMap<>();
	private final Map<String, Type> variableTable = new HashMap<>();
	private final Map<String, Node> nodeTable = new HashMap<>();

	public ExprTypeVisitor(Program program) {
		populateTypeTable(program.types);
		populateEnumValueTable(program.types);
		populateConstantTable(program.constants);
		nodeTable.putAll(Util.getNodeTable(program.nodes));
//		System.out.println(">>>>>>> ExprTypeVisitor <<<<<<<<<<");
//		System.out.println("TypeTable >>>>");
//		for (String key : typeTable.keySet()) {
//			System.out.println(key + " : " + typeTable.get(key));
//		}
//		System.out.println("ConstantTable >>>>");
//		for (String key : constantTable.keySet()) {
//			System.out.println(key + " : " + constantTable.get(key));
//		}
//		System.out.println("EnumValueTable >>>>");
//		for (String key : enumValueTable.keySet()) {
//			System.out.println(key + " : " + enumValueTable.get(key));
//		}
//		System.out.println("VariableTable >>>>");
//		for (String key : variableTable.keySet()) {
//			System.out.println(key + " : " + variableTable.get(key));
//		}
//		System.out.println("NodeTable >>>>");
//		for (String key : nodeTable.keySet()) {
//			System.out.println(key + " : " + nodeTable.get(key));
//		}
//		System.out.print(">>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
	}

	private void populateTypeTable(List<TypeDef> typeDefs) {
		typeTable.putAll(Util.createResolvedTypeTable(typeDefs));
	}

	private void populateConstantTable(List<Constant> constants) {
		for (Constant c : constants) {
			if (c.type == null) {
				constantTable.put(c.id, c.expr.accept(this));
			} else {
				constantTable.put(c.id, this.resolveType(c.type));
			}
		}
	}

	private void populateEnumValueTable(List<TypeDef> typeDefs) {
		for (EnumType et : Util.getEnumTypes(typeDefs)) {
			for (String id : et.values) {
				enumValueTable.put(id, et);
			}
		}
	}

	public void setNodeContext(Node node) {
		variableTable.clear();
		Util.getVarDecls(node).forEach(this::addVariable);
	}

	private void addVariable(VarDecl varDecl) {
		variableTable.put(varDecl.id, this.resolveType(varDecl.type));
	}

	@Override
	public Type visit(ArrayAccessExpr expr) {
		ArrayType array = (ArrayType) expr.array.accept(this);
		return array.base;
	}

	@Override
	public Type visit(ArrayExpr expr) {
		Type type = expr.elements.get(0).accept(this);
		// Throw away SubrangeIntType for arrays
		if (type instanceof SubrangeIntType) {
			return new ArrayType(NamedType.INT, expr.elements.size());
		}
		return new ArrayType(type, expr.elements.size());
	}

	@Override
	public Type visit(ArrayUpdateExpr expr) {
		return expr.array.accept(this);
	}

	@Override
	public Type visit(BinaryExpr expr) {
		Type type = null;

		switch (expr.op) {
		case EQUAL:
		case NOTEQUAL:
		case GREATER:
		case LESS:
		case GREATEREQUAL:
		case LESSEQUAL:
		case OR:
		case AND:
		case XOR:
		case IMPLIES:
			return NamedType.BOOL;

		case PLUS:
		case MINUS:
		case MULTIPLY:
			type = expr.left.accept(this);
			if (type instanceof SubrangeIntType) {
				// Cast SubrangeIntType to Int
				return NamedType.INT;
			} else {
				return type;
			}

		case DIVIDE:
			return NamedType.REAL;

		case INT_DIVIDE:
		case MODULUS:
			return NamedType.INT;

		case ARROW:
			type = expr.left.accept(this);
			if (type instanceof SubrangeIntType) {
				// Resolve SubrangeIntType to SubrangeIntType or Int
				return resolveSubrangeIntType(type, expr.right.accept(this));
			} else {
				return type;
			}

		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Type visit(BoolExpr expr) {
		return NamedType.BOOL;
	}

	@Override
	public Type visit(CastExpr expr) {
		return expr.type;
	}

	@Override
	public Type visit(CondactExpr expr) {
		return expr.call.accept(this);
	}

	@Override
	public Type visit(IdExpr expr) {
		if (variableTable.containsKey(expr.id)) {
			return variableTable.get(expr.id);
		} else if (constantTable.containsKey(expr.id)) {
			return constantTable.get(expr.id);
		} else if (enumValueTable.containsKey(expr.id)) {
			return enumValueTable.get(expr.id);
		} else {
			throw new IllegalArgumentException("Unknown variable: " + expr.id);
		}
	}

	@Override
	public Type visit(IfThenElseExpr expr) {
		Type type = expr.thenExpr.accept(this);
		if (type instanceof SubrangeIntType) {
			// Resolve SubrangeIntType to SubrangeIntType or Int
			return resolveSubrangeIntType(type, expr.elseExpr.accept(this));
		} else {
			return type;
		}
	}

	@Override
	public Type visit(IntExpr expr) {
		// Integers are assigned SubrangeIntType temporarily
		return new SubrangeIntType(new BigInteger(expr.toString()),
				new BigInteger(expr.toString()));
	}

	@Override
	public Type visit(NodeCallExpr expr) {
		Node node = nodeTable.get(expr.node);
		List<Type> outputs = new ArrayList<>();
		for (VarDecl output : node.outputs) {
			outputs.add(resolveType(output.type));
		}
		return TupleType.compress(outputs);
	}

	@Override
	public Type visit(RealExpr expr) {
		return NamedType.REAL;
	}

	@Override
	public Type visit(RecordAccessExpr expr) {
		RecordType record = (RecordType) expr.record.accept(this);
		return record.fields.get(expr.field);
	}

	@Override
	public Type visit(RecordExpr expr) {
		if (typeTable.containsKey(expr.id)) {
			return typeTable.get(expr.id);
		} else {
			// If user types have already been inlined, we reconstruct the type
			Map<String, Type> fields = new HashMap<>();
			for (String field : expr.fields.keySet()) {
				fields.put(field, expr.fields.get(field).accept(this));
			}
			return new RecordType(expr.id, fields);
		}
	}

	@Override
	public Type visit(RecordUpdateExpr expr) {
		return expr.record.accept(this);
	}

	@Override
	public Type visit(TupleExpr expr) {
		List<Type> types = new ArrayList<>();
		for (Expr e : expr.elements) {
			types.add(e.accept(this));
		}
		return new TupleType(types);
	}

	@Override
	public Type visit(UnaryExpr expr) {
		if (expr.op.equals(UnaryOp.NOT)) {
			return NamedType.BOOL;
		} else {
			Type type = expr.expr.accept(this);

			// Negate and replace low and high for SubrangeIntType with
			// UnaryOp.NEGATIVE
			if (expr.op.equals(UnaryOp.NEGATIVE)
					&& type instanceof SubrangeIntType) {
				SubrangeIntType subrange = (SubrangeIntType) type;
				return new SubrangeIntType(subrange.high.negate(),
						subrange.low.negate());
			} else {
				return type;
			}
		}
	}

	public Type resolveType(Type type) {
		return type.accept(new TypeMapVisitor() {
			@Override
			public Type visit(NamedType e) {
				if (e.isBuiltin()) {
					return e;
				} else {
					return typeTable.get(e.name);
				}
			}
		});
	}

	// Resolve SubrangeIntType to SubrangeIntType or Int
	public Type resolveSubrangeIntType(Type leftType, Type rightType) {
		if (!(leftType instanceof SubrangeIntType)) {
			throw new IllegalArgumentException("Invalid leftType: " + leftType);
		}

		if (rightType instanceof SubrangeIntType) {
			// Extend the range of combined SubrangeIntType
			BigInteger low = ((SubrangeIntType) leftType).low
					.compareTo(((SubrangeIntType) rightType).low) < 0 ? ((SubrangeIntType) leftType).low
					: ((SubrangeIntType) rightType).low;
			BigInteger high = ((SubrangeIntType) leftType).high
					.compareTo(((SubrangeIntType) rightType).high) > 0 ? ((SubrangeIntType) leftType).high
					: ((SubrangeIntType) rightType).high;
			return new SubrangeIntType(low, high);
		} else if (rightType.equals(NamedType.INT)) {
			return NamedType.INT;
		} else {
			throw new IllegalArgumentException("Invalid rightType: "
					+ rightType);
		}
	}
}
