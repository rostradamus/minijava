package typechecker.implementation;

import java.util.*;
import ast.*;
import typechecker.ErrorReport;
import util.ImpTable;
import util.ImpTable.DuplicateException;
import visitor.DefaultVisitor;

/**
 * This visitor implements Phase 1 of the TypeChecker. It constructs the symboltable.
 *
 * @author norm
 */
public class BuildSymbolTableVisitor extends DefaultVisitor<ImpTable<ClassEntry>> {

    private final ImpTable<ClassEntry> symbolTable = new ImpTable<>();
    private final ErrorReport errors;
    private ClassEntry thisClass;
    private MethodEntry thisMethod;

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
        ClassEntry ce = new ClassEntry(n.className, new ImpTable<>(), new ImpTable<>());
        def(symbolTable, n.className, ce);
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
        thisClass = new ClassEntry(n.name, new ImpTable<>(), new ImpTable<>());

        if (n.superName != null) {
            if (symbolTable.lookup(n.superName) == null) {
                errors.undefinedId(n.superName);
            }
            thisClass.setSuperClass(symbolTable.lookup(n.superName));
        }

        n.vars.accept(this);
        n.methods.accept(this);

        def(symbolTable, n.name, thisClass);
        thisClass = null;

        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(VarDecl n) {
        if (n.kind == VarDecl.Kind.FIELD) {
            def(thisClass.fields, n.name, n.type);
        } else {
            def(thisMethod.variables, n.name, n.type);
        }

        return null;
    }

    @Override
    public ImpTable<ClassEntry> visit(MethodDecl n) {
        List pTypes = new ArrayList<>();

        System.out.println("n.name " + n.name);
        System.out.println("n.formals.size() " + n.formals.size());
        System.out.println("n.vars.size()" + n.vars.size());

        for (int i = 0; i < n.formals.size(); i++) {
            pTypes.add(i, n.formals.elementAt(i).type);
        }

        thisMethod = new MethodEntry(n.returnType, new NodeList<>(pTypes), thisClass);

        n.formals.accept(this);
        n.vars.accept(this);

        def(thisClass.methods, n.name, thisMethod);
        thisMethod = null;

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
    public ImpTable<ClassEntry> visit(BooleanLiteral n) {
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

    /**
     * Add an entry to a table, and check whether the name already existed.
     * If the name already existed before, the new definition is ignored and
     * an error is sent to the error report.
     */
    private <V> void def(ImpTable<V> tab, String name, V value) {
        try {
            tab.put(name, value);
        } catch (DuplicateException e) {
            errors.duplicateDefinition(name);
        }
    }

}