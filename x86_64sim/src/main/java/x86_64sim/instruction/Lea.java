package x86_64sim.instruction;

import x86_64sim.State;
import x86_64sim.args.Arg;
import x86_64sim.args.MemoryArg;

public class Lea extends Instruction {
    private MemoryArg source;
    private Arg target;
	
	public Lea(MemoryArg source, Arg target) {
		this.source = source;
        this.target = target;
	}
	
	@Override
	public void execute(State state) {
		long value;
		value = source.addr(state);
		target.write(state, value);
	}

	@Override
	public String toString() {
	    return "\tleaq\t" + source + ", " + target;
	}
}
