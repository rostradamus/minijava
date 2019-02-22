package ir.semantic;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Objects;

import translate.*;

/**
 * This class does basic checks on generated IR tree to make sure:
 * - LABELs are well defined
 * - DATAs are initialized with appropriate IR types
 * - MOVE refers to correct destination types
 */

public class SemanticChecker {

    public static void run(Fragments fragments) {

        DataChecker dataChecker = new DataChecker();
        LabelChecker labelChecker = new LabelChecker();
        MoveChecker moveChecker = new MoveChecker();

        for (Fragment f : fragments) {
            if (f instanceof ProcFragment) {
                ProcFragment pf = (ProcFragment) f;
                labelChecker.check(pf);
                moveChecker.check(pf);
            } else if (f instanceof DataFragment) {
                DataFragment df = (DataFragment) f;
                dataChecker.check(df);
                labelChecker.check(df);
            } else {
                throw new Error("Unknown fragment (" + f.getClass() + ") found");
            }
        }

        String[] errors = {
            dataChecker.reportErrors(),
            labelChecker.reportErrors(),
            moveChecker.reportErrors()
        };

        String msg = Arrays.stream(errors).filter(Objects::nonNull).collect(Collectors.joining("\n"));

        if (msg != null && !msg.isEmpty()) {
            throw new Error(msg);
        }
    }
}
