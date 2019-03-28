package translate.implementation;

import ast.*;
import ir.frame.Access;
import ir.frame.Frame;
import ir.temp.Label;
import ir.temp.Temp;
import ir.tree.BINOP.Op;
import ir.tree.*;
import ir.tree.CJUMP.RelOp;
import translate.DataFragment;
import translate.Fragments;
import translate.ProcFragment;
import typechecker.implementation.ClassEntry;
import typechecker.implementation.MethodEntry;
import util.FunTable;
import util.ImpTable;
import util.List;
import util.Lookup;
import visitor.Visitor;

import java.util.*;

import static ir.tree.IR.*;
import static translate.TranslatorLabels.*;


/**
 * This visitor builds up a collection of IRTree code fragments for the body
 * of methods in a Functions program.
 * <p>
 * Methods that visit statements and expression return a TRExp, other methods
 * just return null, but they may add Fragments to the collection by means
 * of a side effect.
 *
 * @author kdvolder
 */
public class TranslateVisitor implements Visitor<TRExp> {
    /**
     * We build up a list of Fragment (pieces of stuff to be converted into
     * assembly) here.
     */
    private Fragments frags;

    /**
     * We use this factory to create Frame's, without making our code dependent
     * on the target architecture.
     */
    private Frame frameFactory;

    /**
     * The symbol table may be needed to find information about classes being
     * instantiated, or methods being called etc.
     */
    private Frame frame;

    private FunTable<IRExp> currentEnv;
    private String currentClass;
    private ClassEntry currentClassEntry;
    private MethodEntry currentMethodEntry;

    private Lookup<ClassEntry> classTable;
    private ImpTable<ClassDecl> classDeclTable;
    private Map<String, Map<String, Integer>> offsetTable;

    private final String THIS_IDENTIFIER = "this";

    public TranslateVisitor(Lookup<ClassEntry> table, Frame frameFactory) {
        this.frags = new Fragments(frameFactory);
        this.frameFactory = frameFactory;
        this.classTable = table;
        this.classDeclTable = new ImpTable<>();
        this.offsetTable = new HashMap<>();
    }

    /////// Helpers //////////////////////////////////////////////

    /**
     * After the visitor successfully traversed the program,
     * retrieve the built-up list of Fragments with this method.
     */
    public Fragments getResult() {
        return frags;
    }

    private boolean atGlobalScope() {
        return frame.getLabel().equals(L_MAIN);
    }

    /**
     * Create a frame with a given number of formals. We assume that
     * no formals escape, which is the case in MiniJava.
     */
    private Frame newFrame(Label name, int nFormals) {
        return frameFactory.newFrame(name, nFormals);
    }

    /**
     * Creates a label for a function (used by calls to that method).
     * The label name is simply the function name.
     */
    private Label functionLabel(String className, String functionName) {
        return Label.get(className + "_" + functionName);
    }

    private void putEnv(String name, Access access) {
        currentEnv = currentEnv.insert(name, access.exp(frame.FP()));
    }

    private IRExp getExpFromThis(String name) {
        return MEM(BINOP(
                Op.PLUS,
                currentEnv.lookup(THIS_IDENTIFIER),
                CONST(offsetTable.get(currentClass).get(name))));
    }

    ////// Visitor Methods ///////////////////////////////////////////////

    @Override
    public <T extends AST> TRExp visit(NodeList<T> ns) {
        IRStm result = IR.NOP;
        for (int i = 0; i < ns.size(); i++) {
            AST nextStm = ns.elementAt(i);
            TRExp e = nextStm.accept(this);
            // e will be null if the statement was in fact a function declaration
            // just ignore these as they generate Fragments
            if (e != null)
                result = IR.SEQ(result, e.unNx());
        }
        return new Nx(result);
    }

    @Override
    public TRExp visit(Program n) {
        currentEnv = FunTable.theEmpty();
        n.mainClass.accept(this);
        n.classes.accept(this);

        return new Nx(NOP);
    }

    @Override
    public TRExp visit(Conditional n) {
        //TODO: This is eager, as in functions-starter!
        TRExp c = n.e1.accept(this);
        TRExp t = n.e2.accept(this);
        TRExp f = n.e3.accept(this);

        TEMP v = TEMP(new Temp());
        return new Ex(ESEQ(SEQ(
                MOVE(v, f.unEx()),
                CMOVE(RelOp.EQ, c.unEx(), TRUE, v, t.unEx())),
                v));
    }

    @Override
    public TRExp visit(Print n) {
        TRExp arg = n.exp.accept(this);
        return new Ex(IR.CALL(L_PRINT, arg.unEx()));
    }

    @Override
    public TRExp visit(Assign n) {
        TRExp value = n.value.accept(this);
        IRExp assignee = currentEnv.lookup(n.name);

        if (assignee == null) {
            assignee = getExpFromThis(n.name);
        }

        return new Nx(MOVE(assignee, value.unEx()));
    }

    private TRExp relOp(final CJUMP.RelOp op, AST ltree, AST rtree) {
        final TRExp l = ltree.accept(this);
        final TRExp r = rtree.accept(this);
        return new TRExp() {
            @Override
            public IRStm unCx(Label t, Label f) {
                return IR.CJUMP(op, l.unEx(), r.unEx(), t, f);
            }

            @Override
            public IRStm unCx(IRExp dst, IRExp src) {
                return IR.CMOVE(op, l.unEx(), r.unEx(), dst, src);
            }

            @Override
            public IRExp unEx() {
                TEMP v = TEMP(new Temp());
                return ESEQ(SEQ(
                        MOVE(v, FALSE),
                        CMOVE(op, l.unEx(), r.unEx(), v, TRUE)),
                        v);
            }

            @Override
            public IRStm unNx() {
                Label end = Label.gen();
                return SEQ(unCx(end, end),
                        LABEL(end));
            }

        };

    }

    @Override
    public TRExp visit(LessThan n) {
        return relOp(RelOp.LT, n.e1, n.e2);
    }

    //////////////////////////////////////////////////////////////

    private TRExp numericOp(Op op, Expression e1, Expression e2) {
        TRExp l = e1.accept(this);
        TRExp r = e2.accept(this);
        return new Ex(IR.BINOP(op, l.unEx(), r.unEx()));
    }

    @Override
    public TRExp visit(Plus n) {
        return numericOp(Op.PLUS, n.e1, n.e2);
    }

    @Override
    public TRExp visit(Minus n) {
        return numericOp(Op.MINUS, n.e1, n.e2);
    }

    @Override
    public TRExp visit(Times n) {
        return numericOp(Op.MUL, n.e1, n.e2);
    }

    //////////////////////////////////////////////////////////////////

    @Override
    public TRExp visit(IntegerLiteral n) {
        return new Ex(IR.CONST(n.value));
    }

    @Override
    public TRExp visit(IdentifierExp n) {
        IRExp var = currentEnv.lookup(n.name);

        if (var == null) {
            var = getExpFromThis(n.name);
        }

        return new Ex(var);
    }

    @Override
    public TRExp visit(Not n) {
        final TRExp negated = n.e.accept(this);
        return new Cx() {
            @Override
            public IRStm unCx(Label ifTrue, Label ifFalse) {
                return negated.unCx(ifFalse, ifTrue);
            }

            @Override
            public IRStm unCx(IRExp dst, IRExp src) {
                return new Ex(IR.BINOP(Op.MINUS, IR.CONST(1), negated.unEx())).unCx(dst, src);
            }

            @Override
            public IRExp unEx() {
                return new Ex(IR.BINOP(Op.MINUS, IR.CONST(1), negated.unEx())).unEx();
            }
        };
    }

    @Override
    public TRExp visit(VarDecl n) {
        switch (n.kind) {
            case LOCAL:
                putEnv(n.name, frame.allocLocal(false));
                break;
            case FORMAL:
                break;
            case FIELD:
                IRData data = new IRData(functionLabel(currentClass, n.name), List.list(IR.CONST(0)));
                frags.add(new DataFragment(frame, data));
                break;
        }

        return null;
    }

    @Override
    public TRExp visit(Call n) {
        String className = null;
        List<IRExp> args = List.list();
        Expression expr = n.receiver;

        while (className == null) {
            if (expr instanceof IdentifierExp) {
                String receiverName = ((IdentifierExp) expr).name;

                Type type = currentMethodEntry.lookupVariable(receiverName);

                if (type == null) {
                    type = currentClassEntry.lookupField(receiverName);
                }

                if (type == null) {
                    throw new Error("Expected any object type!");
                }

                className = type.toString();
            } else if (expr instanceof This) {
                className = currentClass;
            } else if (expr instanceof Call) {
                // Recurse to find class name!
                expr = ((Call) n.receiver).receiver;
            } else if (expr instanceof NewObject) {
                className = ((NewObject) n.receiver).typeName;
            }
        }

        TRExp recExp = n.receiver.accept(this);
        args.add(recExp.unEx());

        for (int i = 0; i < n.rands.size(); i++) {
            args.add(n.rands.elementAt(i).accept(this).unEx());
        }

        return new Ex(IR.CALL(functionLabel(className, n.name), args));
    }

    @Override
    public TRExp visit(MainClass n) {
        frame = newFrame(L_MAIN,0);
        TRExp st = n.statement.accept(this);
        IRStm bd = frame.procEntryExit1(IR.SEQ(st.unNx()));
        frags.add(new ProcFragment(frame, bd));
        return new Nx(NOP);
    }

    @Override
    public TRExp visit(ClassDecl n) {
        // Update current class and entry.
        currentClass = n.name;
        currentClassEntry = classTable.lookup(n.name);
        ClassDecl superClassDecl = currentClassEntry.getSuperClass() != null ?
                classDeclTable.lookup(currentClassEntry.getSuperClass().name) : null;

        // Input offset for fields in class.
        if (!offsetTable.containsKey(n.name)) {
            int offset = 0;
            Map<String, Integer> fieldOffsetTable = new HashMap<>();

            for (Map.Entry<String, Type> field : currentClassEntry.getFields()) {
                fieldOffsetTable.put(field.getKey(), offset);
                boolean isFieldFound = false;
                for (int i = 0; i < n.vars.size(); i++) {
                    if (n.vars.elementAt(i).name.equals(field.getKey())) {
                        isFieldFound = true;
                    }
                }
                if (!isFieldFound && superClassDecl != null) {
                    for (int i = 0; i < superClassDecl.vars.size(); i++) {
                        if (superClassDecl.vars.elementAt(i).name.equals(field.getKey())) {
                            n.vars.add(superClassDecl.vars.elementAt(i));
                        }
                    }
                }
                offset += frame.wordSize();
            }
            offsetTable.put(n.name, fieldOffsetTable);
        }
        n.vars.accept(this);

        for (Map.Entry<String, MethodEntry> methodEntry : currentClassEntry.getMethods()) {
            boolean isMethodFound = false;
            for (int i = 0; i < n.methods.size(); i++) {
                if (n.methods.elementAt(i).name.equals(methodEntry.getKey())) {
                    isMethodFound = true;
                    break;
                }
            }
            if (!isMethodFound && superClassDecl != null) {
                for (int i = 0; i < superClassDecl.methods.size(); i++) {
                    if (superClassDecl.methods.elementAt(i).name.equals(methodEntry.getKey())) {
                        n.methods.add(superClassDecl.methods.elementAt(i));
                    }
                }
            }
        }
        n.methods.accept(this);

        try {
            classDeclTable.put(currentClass, n);
        } catch(ImpTable.DuplicateException e) {
            e.printStackTrace();
        }


        return new Nx(NOP);
    }

    @Override
    public TRExp visit(MethodDecl n) {
        // Retain previous environment, frame.
        FunTable<IRExp> prevEnv = currentEnv;
        Frame prevFrame = frame;

        frame = newFrame(functionLabel(currentClass, n.name), n.formals.size() + 1);
        currentMethodEntry = classTable.lookup(currentClass).lookupMethod(n.name);

        // Insert elements into current environment.
        putEnv(THIS_IDENTIFIER, frame.getFormal(0));

        for (int i = 0; i < n.formals.size(); i++) {
            putEnv(n.formals.elementAt(i).name, frame.getFormal(i + 1));
        }

        for (int i = 0; i < n.vars.size(); i++) {
            putEnv(n.vars.elementAt(i).name, frame.allocLocal(false));
        }

        // Visit vars, statements and return expression.
        n.vars.accept(this);
        TRExp stmts = n.statements.accept(this);
        TRExp retExp = n.returnExp.accept(this);

        IRStm body = frame.procEntryExit1(SEQ(
                stmts.unNx(),
                MOVE(frame.RV(), retExp.unEx())
        ));

        frags.add(new ProcFragment(frame, body));

        // Recover previous environment, frame.
        currentEnv = prevEnv;
        frame = prevFrame;

        return new Nx(NOP);
    }

    @Override
    public TRExp visit(Block n) {
        return n.statements.accept(this);
    }

    @Override
    public TRExp visit(If n) {
        Label tLabel = Label.gen();
        Label fLabel = Label.gen();
        Label endLabel = Label.gen();
        TRExp tst = n.tst.accept(this);
        TRExp thn = n.thn.accept(this);
        TRExp els = n.els.accept(this);

        IRStm res = IR.SEQ(
                tst.unCx(tLabel, fLabel),
                IR.LABEL(tLabel),
                thn.unNx(),
                IR.JUMP(endLabel),
                IR.LABEL(fLabel),
                els.unNx(),
                IR.LABEL(endLabel));

        return new Nx(res);
    }

    @Override
    public TRExp visit(While n) {
        TRExp test = n.tst.accept(this);
        TRExp body = n.body.accept(this);

        Label loopTest = Label.gen();
        Label loopBody = Label.gen();
        Label done = Label.gen();

        return new Nx(SEQ(SEQ(LABEL(loopTest),
                SEQ(test.unCx(loopBody, done),
                        SEQ(LABEL(loopBody),
                                SEQ(body.unNx(),
                                        JUMP(loopTest))))),
                LABEL(done)));
    }

    @Override
    public TRExp visit(ArrayAssign n) {
        IRExp base = currentEnv.lookup(n.name);

        if (base == null) {
            base = getExpFromThis(n.name);
        }
        Label checkLTArrSize = Label.gen();
        Label tLabel = Label.gen();
        Label fLabel = Label.gen();
        IRStm outBoundError = EXP(IR.CALL(
                L_ERROR,
                IR.CONST(1)));

        TRExp tstGEZero = relOp(RelOp.GE, n.index, new IntegerLiteral(0));
        TRExp tstLTArrSize = relOp(RelOp.LT, n.index, new ArrayLength(new IdentifierExp(n.name)));

        IRExp offset = IR.BINOP(
                Op.MUL,
                n.index.accept(this).unEx(),
                CONST(frame.wordSize()));
        IRExp pointer = IR.BINOP(
                Op.PLUS,
                base,
                offset);
        IRStm evalArray = IR.MOVE(
                IR.MEM(pointer),
                n.value.accept(this).unEx());

        IRStm res = IR.SEQ(
                tstGEZero.unCx(checkLTArrSize, fLabel),
                IR.LABEL(checkLTArrSize),
                tstLTArrSize.unCx(tLabel, fLabel),
                IR.LABEL(fLabel),
                outBoundError,
                IR.LABEL(tLabel),
                evalArray);
        return new Nx(res);
    }

    @Override
    public TRExp visit(And n) {
        IRExp e1 = n.e1.accept(this).unEx();
        IRExp e2 = n.e2.accept(this).unEx();

        Label l1 = Label.gen();
        Label l2 = Label.gen();
        Label and = Label.gen();

        TEMP tmp = TEMP(new Temp());

        TRExp res = new Ex(IR.ESEQ(SEQ(
                MOVE(tmp, FALSE),
                IR.CJUMP(RelOp.EQ, e1, IR.CONST(1), l1, and),
                LABEL(l1),
                IR.CJUMP(RelOp.EQ, e2, IR.CONST(1), l2, and),
                LABEL(l2),
                MOVE(tmp, TRUE),
                LABEL(and)), tmp));

        return res;
    }

    @Override
    public TRExp visit(ArrayLookup n) {
        Label checkLTArrSize = Label.gen();
        Label tLabel = Label.gen();
        Label fLabel = Label.gen();
        IRStm outBoundError = EXP(IR.CALL(
                L_ERROR,
                IR.CONST(1)));
        TRExp tstGEZero = relOp(RelOp.GE, n.index, new IntegerLiteral(0));
        TRExp tstLTArrSize = relOp(RelOp.LT, n.index, new ArrayLength(n.array));

        IRExp ptr = n.array.accept(this).unEx();
        IRExp idx = n.index.accept(this).unEx();
        IRExp evalArray = IR.MEM(IR.BINOP(
                Op.PLUS,
                ptr,
                IR.BINOP(
                        Op.MUL,
                        idx,
                        IR.CONST(frame.wordSize()))));
        IRExp finalres = IR.ESEQ(
                IR.SEQ(
                        tstGEZero.unCx(checkLTArrSize, fLabel),
                        IR.LABEL(checkLTArrSize),
                        tstLTArrSize.unCx(tLabel, fLabel),
                        IR.LABEL(fLabel),
                        outBoundError,
                        IR.LABEL(tLabel)
                        ),
                evalArray);

        return new Ex(finalres);
    }

    @Override
    public TRExp visit(ArrayLength n) {
        TRExp arr = n.array.accept(this);
        return new Ex(IR.MEM(IR.BINOP(
                Op.MINUS,
                arr.unEx(),
                IR.CONST(frame.wordSize()))));
    }

    @Override
    public TRExp visit(BooleanLiteral n) {
        if (n.value) {
            return new Ex(TRUE);
        }

        return new Ex(FALSE);
    }

    @Override
    public TRExp visit(This n) {
        Access t = frame.getFormal(0);
        return new Ex(t.exp(frame.FP()));
    }

    @Override
    public TRExp visit(NewArray n) {
        TRExp size = n.size.accept(this);
        return new Ex(IR.CALL(L_NEW_ARRAY, List.list(size.unEx())));
    }

    @Override

    public TRExp visit(NewObject n) {
        ClassEntry ce = classTable.lookup(n.typeName);
        return new Ex(IR.CALL(
                L_NEW_OBJECT,
                IR.CONST(ce.getFields().size() * frame.wordSize())));
    }

    // Types & Not supported (implemented)

    @Override
    public TRExp visit(BooleanType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(IntegerType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(UnknownType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(IntArrayType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(ObjectType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(FunctionType n) {
        throw new Error("Not implemented");
    }

    @Override
    public TRExp visit(FunctionDecl n) {
        throw new Error("Not implemented");
    }

}
