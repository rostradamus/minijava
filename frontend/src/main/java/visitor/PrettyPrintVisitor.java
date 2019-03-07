package visitor;

import ast.*;
import util.IndentingWriter;

import java.io.PrintWriter;


/**
 * This is an adaptation of the PrettyPrintVisitor from the textbook
 * online material, but updated to work with the "modernized"
 * Visitor and our own versions of the AST classes.
 * <p>
 * This version is also cleaned up to actually produce *properly* indented
 * output.
 *
 * @author kdvolder
 */
public class PrettyPrintVisitor implements Visitor<Void> {

    /**
     * Where to send out.print output.
     */
    private IndentingWriter out;

    public PrettyPrintVisitor(PrintWriter out) {
        this.out = new IndentingWriter(out);
    }

    ///////////// Visitor methods /////////////////////////////////////////

    @Override
    public Void visit(Program n) {
        n.mainClass.accept(this);
        n.classes.accept(this);
        return null;
    }

    @Override
    public Void visit(BooleanType n) {
        out.print("boolean");
        return null;
    }

    @Override
    public Void visit(UnknownType n) {
        out.print("unknown");
        return null;
    }

    @Override
    public Void visit(IntegerType n) {
        out.print("int");
        return null;
    }

    @Override
    public Void visit(Conditional n) {
        out.print("( ");
        n.e1.accept(this);
        out.print(" ? ");
        n.e2.accept(this);
        out.print(" : ");
        n.e3.accept(this);
        out.print(" )");
        return null;
    }

    @Override
    public Void visit(Print n) {
        out.print("System.out.println(");
        n.exp.accept(this);
        out.print(");");
        return null;
    }

    @Override
    public Void visit(Assign n) {
        out.print(n.name + " = ");
        n.value.accept(this);
        out.print(";");
        return null;
    }

    @Override
    public Void visit(LessThan n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" < ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Plus n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" + ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Minus n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" - ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(Times n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" * ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(IntegerLiteral n) {
        out.print("" + n.value);
        return null;
    }

    @Override
    public Void visit(IdentifierExp n) {
        out.print(n.name);
        return null;
    }

    @Override
    public Void visit(Not n) {
        out.print("!");
        n.e.accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionDecl n) {
        n.returnType.accept(this);
        out.print(" " + n.name);
        out.print(" (");
        for (int i = 0; i < n.formals.size(); i++) {
            n.formals.elementAt(i).accept(this);
            if (i + 1 < n.formals.size()) {
                out.print(", ");
            }
        }
        out.println(") { ");
        out.indent();
        for (int i = 0; i < n.statements.size(); i++) {
            n.statements.elementAt(i).accept(this);
        }
        out.print("return ");
        n.returnExp.accept(this);
        out.println(";");
        out.outdent();
        out.println("}");
        return null;
    }

    @Override
    public Void visit(VarDecl n) {
        n.type.accept(this);
        out.print(" " + n.name + ";");
        return null;
    }

    @Override
    public Void visit(FunctionType n) {
        out.print("function (");
        for (int i = 0; i < n.formals.size(); ++i) {
            VarDecl v = n.formals.elementAt(i);
            out.print(v.type/* + " " + v.name*/);
            if (i < n.formals.size() - 1) out.print(", ");
        }
        out.print(") -> ");
        n.returnType.accept(this);
        out.print("\n  locals ");
        out.print(n.locals);
        return null;
    }

    @Override
    public Void visit(Call n) {
        n.receiver.accept(this);

        out.print("." + n.name);
        out.print("(");
        for (int i = 0; i < n.rands.size(); i++) {
            n.rands.elementAt(i).accept(this);
            if (i + 1 < n.rands.size()) {
                out.print(", ");
            }
        }
        out.print(")");
        return null;
    }

    @Override
    public <T extends AST> Void visit(NodeList<T> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            nodes.elementAt(i).accept(this);
            out.print("\n");
        }
        return null;
    }

    @Override
    public Void visit(MainClass n) {
        out.println(String.format("class %s {", n.className));
        out.indent();
        out.println(String.format("public static void main (String[] %s) {", n.argName));
        out.indent();
        n.statement.accept(this);
        out.outdent();
        out.println("}");
        out.outdent();
        out.println("}");
        return null;
    }

    @Override
    public Void visit(ClassDecl n) {
        out.println(n.superName != null ? String.format("class %s extends %s{", n.name, n.superName)
                : String.format("class %s {", n.name));
        out.indent();
        n.vars.accept(this);
        n.methods.accept(this);
        out.outdent();
        out.println("}");
        return null;
    }

    @Override
    public Void visit(MethodDecl n) {
        out.print("public ");
        n.returnType.accept(this);
        out.print(" " + n.name + "(");
        for (int i = 0; i < n.formals.size(); i++) {
            if (i != 0) out.print(", ");
            VarDecl param = n.formals.elementAt(i);
            param.type.accept(this);
            out.print(" " + param.name);
        }
        out.println(") {");
        out.indent();
        n.vars.accept(this);
        n.statements.accept(this);
        out.print("return ");
        n.returnExp.accept(this);
        out.println(";");
        out.outdent();
        out.print("}");
        return null;
    }

    @Override
    public Void visit(IntArrayType n) {
        out.print("int[]");
        return null;
    }

    @Override
    public Void visit(ObjectType n) {
        out.print(n.name);
        return null;
    }

    @Override
    public Void visit(Block n) {
        out.println("{");
        out.indent();
        n.statements.accept(this);
        out.outdent();
        out.println("}");
        return null;
    }

    @Override
    public Void visit(If n) {
        out.print("if (");
        n.tst.accept(this);
        out.println(")");
        out.indent();
        n.thn.accept(this);
        out.outdent();
        out.println("else");
        out.indent();
        n.els.accept(this);
        out.outdent();
        return null;
    }

    @Override
    public Void visit(While n) {
        out.print("while (");
        n.tst.accept(this);
        out.print(") ");
        n.body.accept(this);
        return null;
    }

    @Override
    public Void visit(ArrayAssign n) {
        out.print(n.name + "[");
        n.index.accept(this);
        out.print("] = ");
        n.value.accept(this);
        out.print(";");
        return null;
    }

    @Override
    public Void visit(And n) {
        out.print("(");
        n.e1.accept(this);
        out.print(" && ");
        n.e2.accept(this);
        out.print(")");
        return null;
    }

    @Override
    public Void visit(ArrayLookup n) {
        n.array.accept(this);
        out.print("[");
        n.index.accept(this);
        out.print("]");
        return null;
    }

    @Override
    public Void visit(ArrayLength n) {
        n.array.accept(this);
        out.print(".length");
        return null;
    }

    @Override
    public Void visit(BooleanLiteral n) {
        out.print(n.value);
        return null;
    }

    @Override
    public Void visit(This n) {
        out.print("this");
        return null;
    }

    @Override
    public Void visit(NewArray n) {
        out.print("new int[");
        n.size.accept(this);
        out.print("]");
        return null;
    }

    @Override
    public Void visit(NewObject n) {
        out.print("new " + n.typeName + "()");
        return null;
    }
}
