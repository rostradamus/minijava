package visitor;

import ast.*;

/**
 * A modernized version of the Visitor interface, adapted from the textbook's
 * version.
 * <p>
 * Changes: this visitor allows you to return something as a result.
 * The "something" can be of any particular type, so the Visitor
 * uses Java generics to express this.
 *
 * @author kdvolder
 */
public interface Visitor<R> {

    //Lists
    <T extends AST> R visit(NodeList<T> ns);

    //Declarations
    R visit(Program n);

    R visit(MainClass n);

    R visit(ClassDecl n);

    R visit(VarDecl n);

    R visit(MethodDecl n);

    R visit(FunctionDecl n);

    //Types
    R visit(IntArrayType n);

    R visit(BooleanType n);

    R visit(IntegerType n);

    R visit(ObjectType n);

    R visit(UnknownType n);

    R visit(FunctionType n);

    //Statements
    R visit(Block n);

    R visit(If n);

    R visit(While n);

    R visit(Print n);

    R visit(Assign n);

    R visit(ArrayAssign n);

    //Expressions
    R visit(And n);

    R visit(LessThan n);

    R visit(Plus n);

    R visit(Minus n);

    R visit(Times n);

    R visit(ArrayLookup n);

    R visit(ArrayLength n);

    R visit(Call n);

    R visit(IntegerLiteral n);

    R visit(BooleanLiteral n);

    R visit(IdentifierExp n);

    R visit(This n);

    R visit(NewArray n);

    R visit(NewObject n);

    R visit(Not not);

    R visit(Conditional n);

}
