package typechecker.implementation;

import ast.Program;
import ast.Type;
import typechecker.ErrorReport;
import typechecker.TypeChecked;
import typechecker.TypeCheckerException;
import util.ImpTable;
import util.Pair;


public class TypeCheckerImplementation extends TypeChecked {

    /**
     * The AST of the program we are type checking.
     */
    private Program program;

    /**
     * The place to which error messages get sent.
     */
    private ErrorReport errors = new ErrorReport();

    /**
     * The symbol table computed by phase 1:
     */
    private ImpTable<ClassEntry> symbolTable;

    public TypeCheckerImplementation(Program program) {
        this.program = program;
    }

    public TypeChecked typeCheck() throws TypeCheckerException {
        //Phase 1:
        symbolTable = buildTable();
        //Phase 2:
        program.accept(new TypeCheckVisitor(symbolTable, errors));
        //Th	row an exception if there were errors:
        errors.close();
        // If there was no exception:
        return this;
    }

    /**
     * This is really an internal helper method, which should not be public.
     * It has only been made public to allow us to test Phase 1 of the typechecker
     * in isolation. In normal operation (not unit testing) this method should
     * not be called by code outside the type checker.
     */
    public ImpTable<ClassEntry> buildTable() {
        symbolTable = program.accept(new BuildSymbolTableVisitor(errors));
        return symbolTable;
    }

    public ImpTable<ClassEntry> typeCheckPhaseTwo() throws TypeCheckerException {
        program.accept(new TypeCheckVisitor(symbolTable, errors));
        errors.close();
        return symbolTable;
    }

    public Program getProgram() {
        return program;
    }

    public ImpTable<ClassEntry> getTable() {
        return symbolTable;
    }

}
