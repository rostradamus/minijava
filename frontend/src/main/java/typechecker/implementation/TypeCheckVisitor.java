package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.Pair;
import visitor.Visitor;

import java.util.ArrayList;
import java.util.List;


/**
 * This class implements Phase 2 of the Type Checker. This phase
 * assumes that we have already constructed the program's symbol table in
 * Phase1.
 * <p>
 * Phase 2 checks for the use of undefined identifiers and type errors.
 * <p>
 * Visitors may return a Type as a result. Generally, only visiting
 * an expression or a type actually returns a type.
 * <p>
 * Visiting other nodes just returns null.
 *
 * @author kdvolder
 */
public class TypeCheckVisitor implements Visitor<Type> {

    /**
     * The place to send error messages to.
     */
    private ErrorReport errors;

    /**
     * The symbol table from Phase 1.
     */
    private ImpTable<ClassEntry> symbolTable;
    private ClassEntry currentClass;
    private MethodEntry currentMethod;


    public TypeCheckVisitor(ImpTable<ClassEntry> symbolTable, ErrorReport errors) {
        this.symbolTable = symbolTable;
        this.errors = errors;
    }

    //// Helpers /////////////////////

    /**
     * Check whether the type of a particular expression is as expected.
     */
    private void check(Expression exp, Type expected) {
        Type actual = exp.accept(this);
        if (!assignableFrom(expected, actual))
            errors.typeError(exp, expected, actual);
    }

    /**
     * Check whether two types in an expression are the same
     */
    private void check(Expression exp, Type t1, Type t2) {
        if (!t1.equals(t2))
            errors.typeError(exp, t1, t2);
    }

    private boolean assignableFrom(Type varType, Type valueType) {
        return varType.equals(valueType);
    }

    ///////// Visitor implementation //////////////////////////////////////

    @Override
    public <T extends AST> Type visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++) {
            ns.elementAt(i).accept(this);
        }
        return null;
    }

    @Override
    public Type visit(Program n) {
        //		variables = applyInheritance(variables);
        n.mainClass.accept(this);
        n.classes.accept(this);
        return null;
    }

    @Override
    public Type visit(BooleanType n) {
        return n;
    }

    @Override
    public Type visit(IntegerType n) {
        return n;
    }

    @Override
    public Type visit(UnknownType n) {
        return n;
    }

    /**
     * Can't use check, because print allows either Integer or Boolean types
     */
    @Override
    public Type visit(Print n) {
        Type actual = n.exp.accept(this);
        if (!assignableFrom(new IntegerType(), actual) && !assignableFrom(new BooleanType(), actual)) {
            List<Type> l = new ArrayList<Type>();
            l.add(new IntegerType());
            l.add(new BooleanType());
            errors.typeError(n.exp, l, actual);
        }
        return null;
    }

    @Override
    public Type visit(Assign n) {
        check(n.value, new IdentifierExp(n.name).accept(this), n.value.accept(this));
        return null;
    }

    @Override
    public Type visit(LessThan n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(Conditional n) {
        check(n.e1, new BooleanType());
        Type t2 = n.e2.accept(this);
        Type t3 = n.e3.accept(this);
        check(n, t2, t3);
        return t2;
    }

    @Override
    public Type visit(Plus n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Minus n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(Times n) {
        check(n.e1, new IntegerType());
        check(n.e2, new IntegerType());
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(IntegerLiteral n) {
        n.setType(new IntegerType());
        return n.getType();
    }

    @Override
    public Type visit(IdentifierExp n) {
        Type type = currentMethod.lookupVariable(n.name);
        if (type == null) {
            errors.undefinedId(n.name);
        }

        n.setType(type);
        return n.getType();
    }

    @Override
    public Type visit(Not n) {
        check(n.e, new BooleanType());
        n.setType(new BooleanType());
        return n.getType();
    }

    @Override
    public Type visit(FunctionDecl n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(VarDecl n) {
        return null;
    }

    @Override
    public Type visit(Call n) {
        Type receiverType = n.receiver.accept(this);
        if (!(receiverType instanceof ObjectType)) {
            errors.typeError(n.receiver, new ObjectType("object"), receiverType);
        }

        // check whether the class is defined
        ObjectType objectType = (ObjectType) receiverType;
        if (symbolTable.lookup(objectType.name) == null) {
            errors.undefinedId(objectType.name);
        }

        // check whether the method is defined
        if (!symbolTable.lookup(objectType.name).containsMethod(n.name)) {
            errors.undefinedId(n.name);
        }

        MethodEntry method = symbolTable.lookup(objectType.name).lookupMethod(n.name);

        // arity check
        if (method.getParameterTypes().size() != n.rands.size()) {
            errors.wrongNumberOfArguments(method.getParameterTypes().size(), n.rands.size());
        }

        // type check arguments
        for (int i = 0; i < n.rands.size(); i++) {
            check(n.rands.elementAt(i), (Type) method.getParameterTypes().elementAt(i));
        }

        n.setType(method.getReturnType());
        return n.getType();
    }

    @Override
    public Type visit(FunctionType n) {
        return n;
    }

    @Override
    public Type visit(MainClass n) {
        n.statement.accept(this);
        return null;
    }

    @Override
    public Type visit(ClassDecl n) {
        currentClass = symbolTable.lookup(n.name);
        n.methods.accept(this);
        currentClass = null;
        return null;
    }

    @Override
    public Type visit(MethodDecl n) {
        currentMethod = currentClass.lookupMethod(n.name);
        check(n.returnExp, n.returnType);
        n.statements.accept(this);
        currentMethod = null;
        return null;
    }

    @Override
    public Type visit(IntArrayType n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(ObjectType n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(Block n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(If n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(While n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(ArrayAssign n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(And n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(ArrayLookup n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(ArrayLength n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(BooleanLiteral n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(This n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(NewArray n) {
        throw new Error("Not implemented");
    }

    @Override
    public Type visit(NewObject n) {
        if (symbolTable.lookup(n.typeName) == null) {
            errors.undefinedId(n.typeName);
        }

        n.setType(new ObjectType(n.typeName));
        return n.getType();
    }
}
