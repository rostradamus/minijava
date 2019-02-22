package x86_64sim.args;

import x86_64sim.State;

public class IdLiteralArg extends Arg {
    private String id;

    public IdLiteralArg(String id) {
        this.id = id;
    }

    public long read(State state) {
        return state.getLabel(id);
    }

    public void write(State state, long id) {
        throw new UnsupportedOperationException("Can't write to literals");
    }

    public String toString() {
        return "$" + id;
    }
}
