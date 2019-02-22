package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class Xor extends Instruction {
    private Arg source, target;

	public Xor(Arg source, Arg target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public void execute(State state) {
		long value = source.read(state) ^ target.read(state);
		target.write(state, value);
	}

	@Override
	public String toString() {
		return "\txorq\t" + source + ", " + target;
	}

}
