package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class Dec extends Instruction {
	private Arg target;
	
	public Dec(Arg target) {
		this.target = target;
	}
	
	@Override
	public void execute(State state) {
		long value = target.read(state) - 1;
		target.write(state, value);
	}

	@Override
	public String toString() {
		return "\tdecq\t" + target;
	}

}
