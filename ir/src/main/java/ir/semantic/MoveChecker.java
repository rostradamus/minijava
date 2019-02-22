package ir.semantic;

import ir.tree.*;
import ir.visitor.*;
import translate.DataFragment;
import translate.ProcFragment;
import util.List;

/**
 * This class will traverse through body of ProcFragment and makes sure:
 * all move/cmove operations are associated with TEMP or MEM as destination address.
 */

public class MoveChecker extends DefaultVisitor<Boolean> {

    private List<IRStm> errors;

    public MoveChecker() {
        errors = List.theEmpty();
    }

    public void check(ProcFragment f) {
        f.getBody().accept(this);
    }

    @Override
    public Boolean visit(MEM n) {
        super.visit(n);
        return Boolean.TRUE;
    }

    @Override
    public Boolean visit(TEMP n) {
        super.visit(n);
        return Boolean.TRUE;
    }

    // check for destination type
    @Override
    public Boolean visit(MOVE n) {
        n.src.accept(this);
        Boolean lhs = n.dst.accept(this);

        if (lhs == null || lhs != Boolean.TRUE) {
            addError(n);
        }

        return null;
    }

    // check for destination type
    @Override
    public Boolean visit(CMOVE n) {
        n.left.accept(this);
        n.right.accept(this);
        n.src.accept(this);
        Boolean lhs = n.dst.accept(this);

        if (lhs == null || lhs != Boolean.TRUE) {
            addError(n);
        }

        return null;
    }

    public void addError(IRStm move) {
        errors = errors.append(List.list(move));
    }

    public String reportErrors() {
        for (IRStm move : errors) {
            System.out.println("ERROR : the following MOVE does not have correct destination type (TEMP or MEM)");
            System.out.println(move.toString());
        }

        if (errors.size() > 0) {
            return "MOVE can only have TEMP or MEM as destination";
        }

        return null;
    }
}
