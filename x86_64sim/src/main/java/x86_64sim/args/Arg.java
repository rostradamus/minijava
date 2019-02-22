package x86_64sim.args;

import x86_64sim.State;

public abstract class Arg {
    abstract public long read(State state);
    abstract public void write(State state, long value);
}
