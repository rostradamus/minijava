package x86_64sim.args;

import x86_64sim.State;

public class RegisterArg extends Arg {
    private String name;

    public RegisterArg(String name) {
        this.name = name;
    }

    public long read(State state) {
        long value = state.getReg(name);
        return value;
    }

    public void write(State state, long value) {
        if (state.beVerbose)
            System.out.println(toString() + " <- " + value);
        state.setReg(name, value);
    }

    public String toString() {
        return name;
    }
}
