package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class Cmp extends Instruction {
	private Arg target, source;

	public Cmp(Arg source, Arg target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public void execute(State state) {
		state.conditionr = source.read(state);
		state.conditionl = target.read(state);
	}

	@Override
	public String toString() {
		return "\tcmpq\t" + source + ", " + target;
	}

}
