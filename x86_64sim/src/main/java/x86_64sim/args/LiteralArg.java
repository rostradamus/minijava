package x86_64sim.args;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class LiteralArg extends Arg {
    private long value;

    public LiteralArg(long value) {
        this.value = value;
    }

    public long read(State state) {
        return value;
    }

    public void write(State state, long value) {
        throw new UnsupportedOperationException("Can't write to literals");
    }

    public String toString() {
        return "$" + value;
    }
}
