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
import util.FunTable;
import util.List;
import util.Lookup;
import visitor.Visitor;

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
    private Type type;

    public TranslateVisitor(Lookup<ClassEntry> table, Frame frameFactory) {
        this.frags = new Fragments(frameFactory);
        this.frameFactory = frameFactory;
    }

    /////// Helpers //////////////////////////////////////////////

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
    private Label methodLabel(String functionName) {
        return Label.get(currentClass + "_" + functionName);
    }

    /**
     * After the visitor successfully traversed the program,
     * retrieve the built-up list of Fragments with this method.
     */
    public Fragments getResult() {
        return frags;
    }

    private void putEnv(String name, Access access) {
        currentEnv = currentEnv.insert(name, access.exp(frame.FP()));
    }

    private void putEnv(String name, IRExp irexp) {
        currentEnv = currentEnv.insert(name, irexp);
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

    private TRExp visitStatements(NodeList<Statement> statements) {
        IRStm result = IR.NOP;

        for (int i = 0; i < statements.size(); i++) {
            Statement nextStm = statements.elementAt(i);
            result = IR.SEQ(result, nextStm.accept(this).unNx());
        }

        return new Nx(result);
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
        IRExp assignee = currentEnv.lookup(n.name);
        TRExp value = n.value.accept(this);

        if (assignee == null) {
            Access var = frame.allocLocal(false);
            putEnv(n.name, var);
            assignee = var.exp(frame.FP());
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
                IRData data = new IRData(methodLabel(n.name), List.list(IR.CONST(0)));
                frags.add(new DataFragment(frame, data));
                break;
        }

        return null;
    }


    @Override
    public TRExp visit(Call n) {
        List<IRExp> arguments = List.list();
        String prevClass = currentClass;
        IRExp pointer;

        if (n.receiver instanceof IdentifierExp) {
            IdentifierExp iExp = (IdentifierExp) n.receiver;
            pointer = currentEnv.lookup(iExp.name);

            if (iExp.getType() instanceof ObjectType){
                currentClass = ((ObjectType) iExp.getType()).name;
            } else {
                throw new Error("Expecting an object type for receiver.");
            }
        } else if (n.receiver instanceof This) {
            pointer = currentEnv.lookup(currentClass);
        } else if (n.receiver instanceof Call) {
            n.receiver.accept(this);
            pointer = frame.RV();
            currentClass = type.toString();
        } else if (n.receiver instanceof NewObject) {
            pointer = n.receiver.accept(this).unEx();
            currentClass = ((NewObject) n.receiver).typeName;
        } else {
            throw new Error("Receiver is unexpected object.");
        }

        arguments.add(pointer);

        for (int i = 0; i < n.rands.size(); i++) {
            arguments.add(n.rands.elementAt(i).accept(this).unEx());
        }

        TRExp result = new Ex(IR.CALL(methodLabel(n.name), arguments));

        // Restore current class.
        currentClass = prevClass;

        return result;
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
        FunTable<IRExp> prevEnv = currentEnv;
        currentClass = n.name;

        NewObject ts = new NewObject("this");
        putEnv(currentClass, ts.accept(this).unEx());

//        if (n.superName != null) {
//            currentEnv.merge(prevEnv);
//        }

        n.vars.accept(this);

        if (n.superName != null) {
            currentEnv.merge(prevEnv);
        }

        n.methods.accept(this);

        return new Nx(NOP);
    }

    @Override
    public TRExp visit(MethodDecl n) {
        Frame prevFrame = frame;
        frame = newFrame(methodLabel(n.name), n.formals.size() + 1);

        for (int i = 0; i < n.formals.size(); i++) {
            putEnv(n.formals.elementAt(i).name, frame.getFormal(i + 1));
        }

        n.vars.accept(this);

        TRExp stats = n.statements.accept(this);
        TRExp exp = n.returnExp.accept(this);
        type = n.returnType;

        IRStm body = frame.procEntryExit1(IR.SEQ(stats.unNx(), IR.MOVE(frame.RV(), exp.unEx())));
        frags.add(new ProcFragment(frame, body));

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
        IRExp offset = IR.BINOP(
                Op.MUL,
                n.index.accept(this).unEx(),
                CONST(frame.wordSize()));
        IRExp pointer = IR.BINOP(
                Op.PLUS,
                base,
                offset);
        IRStm result = IR.MOVE(
                IR.MEM(pointer),
                n.value.accept(this).unEx());

        return new Nx(result);
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
                        LABEL(and)
                ), tmp));

        return res;
    }

    @Override
    public TRExp visit(ArrayLookup n) {
        IRExp ptr = n.array.accept(this).unEx();
        IRExp idx = n.index.accept(this).unEx();

        return new Ex(IR.MEM(IR.BINOP(
                                Op.PLUS,
                                ptr,
                                IR.BINOP(
                                        Op.MUL,
                                        idx,
                                        IR.CONST(frame.wordSize())))));
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
        String prevClass = currentClass;
        currentClass = n.typeName;
        int size = Label.get(currentClass).toString().length() * frame.wordSize();

        TRExp result = new Ex(IR.CALL(
                L_NEW_OBJECT,
                List.list(CONST(size))));

        currentClass = prevClass;
        return result;
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
