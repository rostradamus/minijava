package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class Mul extends Instruction {
    private Arg source, extra, target;

	public Mul(Arg extra, Arg source, Arg target) {
		this.extra = extra;
		this.source = source;
		this.target = target;
	}

    public Mul(Arg source, Arg target) {
        this.extra = null;
        this.source = source;
        this.target = target;
    }

    @Override
	public void execute(State state) {
        long value;
	    if (extra == null) {
	        value = source.read(state) * target.read(state);
        } else {
            value = extra.read(state) * source.read(state);
        }
		target.write(state, value);
	}
	@Override
	public String toString() {
		return "\timulq\t" + (extra == null ? "" : extra + ", ") + source + ", " + target;
	}
}
