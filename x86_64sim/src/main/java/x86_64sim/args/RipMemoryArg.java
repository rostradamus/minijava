package x86_64sim.args;

import x86_64sim.State;

public class RipMemoryArg extends MemoryArg {
    String label;
    public RipMemoryArg(String label) {
        super(0, null, null, 1);
        this.label = label;
    }

    public long read(State state) {
        throw new UnsupportedOperationException("Can't read a RipMemoryArg");
    }
    public long addr(State state) {
        return state.getLabel(label);
    }
    public void write(State state, long value) {
        throw new UnsupportedOperationException("Can't write a RipMemoryArg");
    }

    public String toString() {
        return label + "(%rip)";
    }
}
