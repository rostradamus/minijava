package ir.semantic;

import java.util.Map.Entry;

import ir.temp.Label;
import ir.tree.*;
import ir.visitor.DefaultVisitor;
import translate.*;
import util.ImpTable;
import util.List;
import util.Pair;

import static translate.TranslatorLabels.*;

/**
 * this class takes fully populated fragments containing raw IR and checks the following:
 * - REQUIRE : all labels defined are unique
 * - REQUIRE : all c/jumps refer to defined labels
 * - RECOMMENDED : all labels are being referenced
 */

public class LabelChecker extends DefaultVisitor<String> {

    // list of external library functions
    private static final List<Label> extLib = List.list(
            TranslatorLabels.L_PRINT,
            TranslatorLabels.L_ERROR,
            TranslatorLabels.L_NEW_ARRAY,
            TranslatorLabels.L_NEW_OBJECT);

    // keep track of definition count and reference count
    private ImpTable<Pair<Integer, Integer>> labels;

    public LabelChecker() {
        this.labels = new ImpTable<Pair<Integer, Integer>>();

        for (Label l : extLib) {
            def(l);
            ref(l); // we don't need no-reference warning for external functions, so add a reference
        }

        // only add reference, the fragments are responsible for defining main proc
        ref(TranslatorLabels.L_MAIN);
    }

    public void check(ProcFragment f) {
        // add the proc label
        def(f.getLabel());

        // traverse through proc body
        f.getBody().accept(this);
    }

    public void check(DataFragment f) {
        // add data label
        def(f.getLabel());
    }

    @Override
    public String visit(CJUMP n) {
        super.visit(n);
        ref(n.getTrueLabel());
        ref(n.getFalseLabel());
        return null;
    }

    @Override
    public String visit(JUMP n) {
        super.visit(n);
        for (Label l : n.getJumpTargets()) {
            ref(l);
        }
        return null;
    }

    @Override
    public String visit(LABEL n) {
        super.visit(n);
        def(n.getLabel());
        return null;
    }

    @Override
    public String visit(NAME n) {
        super.visit(n);
        ref(n.getLabel());
        return null;
    }

    public String reportErrors() {
        reportInvalidReference();
        return reportInvalidDefinition();
    }

    // increment a define count
    private void def(Label l) {
        String name = l.toString();
        Pair<Integer, Integer> counts = get(name);

        labels.set(name, new Pair<>(counts.first + 1, counts.second));
    }

    // increment a reference count
    private void ref(Label l) {
        String name = l.toString();
        Pair<Integer, Integer> counts = get(name);

        labels.set(name, new Pair<>(counts.first, counts.second + 1));
    }

    // get current counts or 0s
    private Pair<Integer, Integer> get(String name) {
        Pair<Integer, Integer> counts = labels.lookup(name);
        if (counts == null) {
            counts = new Pair<Integer, Integer>(0, 0);
        }

        return counts;
    }

    // check the definition count of each label
    // 0 means there's a jump reference but label is not defined
    // 2+ means same label is defined in IR multiple times
    private String reportInvalidDefinition() {
        boolean failed = false;
        for (Entry<String,Pair<Integer, Integer>> e : labels) {
            if (e.getValue().first == 0) {
                System.out.println("Error: label " + e.getKey() + " does not exist, but " + e.getValue().second + " jumps to it exists");
                failed = true;
            } else if (e.getValue().first > 1) {
                System.out.println("Error: label " + e.getKey() + " defined more than once");
                failed = true;
            }
        }

        if (failed) {
            return "IR tree contains invalid labels. Toggle on beVerbose in ProcFragment.java to see raw IR";
        }

        return null;
    }

    // check the reference count of each label
    // 0 means a label is defined but there's no reference to it
    private void reportInvalidReference() {
        for (Entry<String,Pair<Integer, Integer>> e : labels) {
            if (e.getValue().second == 0) {
                System.out.println("Warning : label " + e.getKey() + " has no jump to it");
            }
        }
    }
}
