package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class Sub extends Instruction {
    private Arg source, target;

	public Sub(Arg source, Arg target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public void execute(State state) {
		long value = target.read(state) - source.read(state);
		target.write(state, value);
	}

	@Override
	public String toString() {
		return "\tsubq\t" + source + ", " + target;
	}

}
