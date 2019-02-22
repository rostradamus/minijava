package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;

public class CMov extends Instruction {
	private String condition;
	private Arg target, source;

	public CMov(String condition, Arg source, Arg target) {
		this.condition = condition;
		this.source = source;
		this.target = target;
	}

	@Override
	public void execute(State state) {
		if (state.conditionTrue(condition)) {
			long value = source.read(state);
			target.write(state, value);
		}
	}

	@Override
	public String toString() {
		return "\tcmov" + condition + "\t" + source + ", " + target;
	}

}
