package typechecker.implementation;

import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import visitor.DefaultVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symboltable.
 *
 * @author norm
 */
public class BuildSymbolTableVisitor extends DefaultVisitor<ImpTable<ClassEntry>> {

    private final ImpTable<ClassEntry> symbolTable = new ImpTable<ClassEntry>();
    private final ErrorReport errors;
    private ClassEntry currentClass;
    private MethodEntry currentMethod;

    public BuildSymbolTableVisitor(ErrorReport errors) {
        this.errors = errors;
    }

    /////////////////// Phase 1 ///////////////////////////////////////////////////////
    // In our implementation, Phase 1 builds up a symbol table containing all the
    // global identifiers defined in a Functions program, as well as symbol tables
    // for each of the function declarations encountered.
    //
    // We also check for duplicate identifier definitions in each symbol table

    @Override
    public ImpTable<ClassEntry> visit(Program n) {
        n.mainClass.accept(this);
        n.classes.accept(this); // process all the "normal" classes.
        return symbolTable;
    }

    @Override
    public ImpTable<ClassEntry> visit(MainClass n) {
        addClass(n.className, new ClassEntry(n.className, new ImpTable<Type>(), new ImpTable<MethodEntry>()));
        return null;
    }

    @Override
    public <T extends AST> ImpTable<ClassEntry> visit(NodeList<T> ns) {
        for (int i = 0; i < ns.size(); i++)
            ns.elementAt(i).accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(ClassDecl n) {
        currentClass = new ClassEntry(n.name, new ImpTable<Type>(), new ImpTable<MethodEntry>());

        if (n.superName != null) {
            if (symbolTable.lookup(n.superName) == null) {
                errors.undefinedId(n.superName);
            }
            currentClass.setSuperClass(symbolTable.lookup(n.superName));
        }

        n.vars.accept(this);
        n.methods.accept(this);

        addClass(n.name, currentClass);
        currentClass = null;
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(VarDecl n) {
        addVariable(n);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(MethodDecl n) {

        NodeList paramTypes = n.formals;

        currentMethod = new MethodEntry(new MethodEntry.MethodSignature(n.returnType, paramTypes), currentClass);

        n.formals.accept(this);
        n.vars.accept(this);

        addMethod(n.name, currentMethod);
        currentMethod = null;
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Assign n) {
        throw new Error("Implementation removed");
/*        n.value.accept(this);
        def(symbolTable, n.name, new UnknownType());
        return null;*/
    }


    @Override
    public ImpTable<ClassEntry> visit(IdentifierExp n) {
        if (symbolTable.lookup(n.name) == null)
            errors.undefinedId(n.name);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(BooleanType n) {
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(IntegerType n) {
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Print n) {
        n.exp.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(LessThan n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Conditional n) {
        n.e1.accept(this);
        n.e2.accept(this);
        n.e3.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Plus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Minus n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Times n) {
        n.e1.accept(this);
        n.e2.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(IntegerLiteral n) {
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(Not not) {
        not.e.accept(this);
        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(UnknownType n) {
        return null;
    }


    ///////////////////// Helpers ///////////////////////////////////////////////

/*    *//**
     * Add an entry to a table, and check whether the name already existed.
     * If the name already existed before, the new definition is ignored and
     * an error is sent to the error report.
     *//*
    private <V> void def(ImpTable<V> tab, String name, V value) {
        try {
            tab.put(name, value);
        } catch (DuplicateException e) {
            errors.duplicateDefinition(name);
        }
    }*/

    private void addClass(String name, ClassEntry entry) {
        try {
            symbolTable.put(name, entry);
        } catch (DuplicateException e) {
            errors.duplicateDefinition(name);
        }
    }

    private void addMethod(String name, MethodEntry entry) {
        try {
            currentClass.insertMethod(name, entry);
        } catch (DuplicateException e) {
            errors.duplicateDefinition(name);
        }
    }

    private void addVariable(VarDecl var) {
        try {
            if (var.kind == VarDecl.Kind.FIELD) {
                currentClass.insertField(var.name, var.type);
            } else if (var.kind == VarDecl.Kind.FORMAL) {
                currentMethod.insertVariable(var.name, var.type);
            }
            else {
                currentMethod.insertVariable(var.name, var.type);
            }
        } catch (DuplicateException e) {
            errors.duplicateDefinition(var.name);
        }
    }

}
