package ir.semantic;

import ir.tree.CONST;
import ir.tree.IRExp;
import ir.tree.NAME;
import translate.DataFragment;
import translate.ProcFragment;
import util.List;

/**
 * This class will check all data defined for each IRData and makes sure that:
 * It is initialized with either CONST or NAME
 */

public class DataChecker {

    private List<IRExp> errors;

    public DataChecker() {
        errors = List.theEmpty();
    }

    public void check(DataFragment f) {
        // traverse through data body
        for (IRExp data : f.getBody()) {
            if (!(data instanceof CONST || data instanceof NAME)) {
                addError(data);
            }
        }
    }


    public void addError(IRExp exp) {
        errors = errors.append(List.list(exp));
    }

    public String reportErrors() {
        for (IRExp exp : errors) {
            System.out.println("ERROR : IRData is not initialized with correct type (CONST or MEM)");
            System.out.println(exp.toString());
        }

        if (errors.size() > 0) {
            return "IRData can only be initialized with CONST or NAME";
        }

        return null;
    }
}
