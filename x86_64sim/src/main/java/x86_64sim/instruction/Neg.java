package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class Neg extends Instruction {
    private Arg target;

	public Neg(Arg target) {
		this.target = target;
	}
	
	@Override
	public void execute(State state) {
		long value = -target.read(state);
		target.write(state, value);
	}

	@Override
	public String toString() {
		return "\tnegq\t" + target;
	}

}
