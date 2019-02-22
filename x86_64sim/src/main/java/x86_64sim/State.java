package x86_64sim;

import x86_64sim.instruction.Instruction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class State {
	public static final long HEAP_BASE = 0x1000L;
    private static final long HEAP_SIZE = 0x1000L;
	private static final long STACK_BASE = 0x8000L, STACK_SIZE = 0x1000L;
	private static final long DONE = 99999999999999L;
	public boolean beVerbose = false;
	private long maxInstructions = 10000000;
	private final static String[] regs = { Instruction.AX /*"%rax"*/, "%rbx", "%rcx", Instruction.DX /*"%rdx"*/, "%rsi", Instruction.ARG1 /*"%rdi"*/, Instruction.SP, Instruction.BP,
		"%r8", "%r9", "%r10", "%r11", "%r12", "%r13", "%r14", "%r15" };
	static {
		assertTrue(HEAP_BASE + HEAP_SIZE < STACK_BASE - STACK_SIZE);
	}
	public Program p;
	public Memory ram;
	private Map<String, Long> registers;
	private List<Map<String, Long>> registerstack;
	public long conditionl;
	public long conditionr;
	public long pc;
	public long hp;
	public PrintStream out = System.out;
	public long instructionsExecuted = 0L;
	public String result;

	public State(Program p) {
		ram = new Memory(HEAP_BASE, HEAP_BASE + HEAP_SIZE, STACK_BASE - STACK_SIZE, STACK_BASE);
		registers = new HashMap<String, Long>();
		registerstack = new LinkedList<Map<String, Long>>();
		pc = p.findMain();
		this.p = p;
		long sp = STACK_BASE - Memory.SIZEOF_LONG;
		ram.write(sp, DONE);
		setReg(Instruction.SP, sp);
		hp = p.writeStatic(ram);
	}

	public long alloc(long size) {
		assertTrue(hp + size <= HEAP_BASE + HEAP_SIZE);
		long answer = hp;
		hp += size;
		return answer;
	}
	public long allocArray(long nElements) {
		long size = (nElements + 1) * Memory.SIZEOF_LONG;
		assertTrue(hp + size <= HEAP_BASE + HEAP_SIZE);
		long answer = hp;
		ram.write(answer, nElements);
		hp += size;
		return answer + Memory.SIZEOF_LONG;
	}
	public void do_call() {
		Map<String, Long> newregisters = new HashMap<String, Long>();
		for (String reg : regs)
			newregisters.put(reg, registers.get(reg));
		registerstack.add(0, registers);
		registers = newregisters;
	}

	public void do_return() {
		// If there is nothing on the stack, we are returning off the end so do nothing
		if (registerstack.size() > 0) {
			Map<String, Long> oldregisters = registerstack.remove(0);
			for (String reg : regs)
				oldregisters.put(reg, registers.get(reg));
			registers = oldregisters;
		}
	}

	public void setReg(String reg, long value) {
		registers.put(reg, value);
	}

	public long getReg(String reg) {
		Long l = registers.get(reg);
		return (l == null) ? 0 : l;
	}

	private static long unsignedCompare(long conditionl, long conditionr) {
		if (conditionl < 0 && conditionr < 0) {
			// more negative is smaller unsigned
			return conditionl - conditionr;
		} else if (conditionl < 0 && conditionr > 0) {
			// negative is bigger than any positive
			return 1;
		} else if (conditionl > 0 && conditionr < 0) {
			// negative is still bigger than any positive
			return -1;
		} else {
			return conditionl - conditionr;
		}
	}
	public static boolean conditionTrue(String condition, long conditionl, long conditionr) {
		if (! (condition.equals("l") || condition.equals("e") ||
				condition.equals("g") || condition.equals("le") ||
				condition.equals("ne") || condition.equals("ge") ||
				condition.equals("b") || condition.equals("be") ||
				condition.equals("a") || condition.equals("ae"))) {
			System.out.println("Illegal condition in branch " + condition);
		}
		return 
				condition.equals("l") && conditionl < conditionr ||
				condition.equals("b") && unsignedCompare(conditionl, conditionr) < 0 ||
				condition.equals("e") && conditionl == conditionr ||
				condition.equals("g") && conditionl > conditionr ||
				condition.equals("a") && unsignedCompare(conditionl, conditionr) > 0 ||
				condition.equals("le") && conditionl <= conditionr ||
				condition.equals("be") && unsignedCompare(conditionl, conditionr) <= 0 ||
				condition.equals("ne") && conditionl != conditionr ||
				condition.equals("ge") && (conditionr < 0 || conditionl >= conditionr) ||
				condition.equals("ae") && unsignedCompare(conditionl, conditionr) >= 0;
	}
	public boolean conditionTrue(String condition) {
		return conditionTrue(condition, conditionl, conditionr);
	}
	public void jump(long address) {
		this.pc = address;
	}
	public void jump(String label) {
		this.pc = p.getLabel(label);
	}
	public long getLabel(String label) {
		return p.getLabel(label);
	}
	private void step() {
		Instruction i = p.getInstruction((int)pc);
		if (beVerbose)
			System.out.println("" + pc + ": " + i);
		pc = pc + 1;
		i.execute(this);
		instructionsExecuted ++;
	}
	public String hex(long reg) {
		return "0x" + Long.toString(reg, 16);
	}

	public void run() {
		while (pc != DONE) {
			step();
		}
	}

	public void run(File out) {
		try {
			this.out = new PrintStream(out);
		} catch (FileNotFoundException e) {
			// do nothing, use System.out
		}
		while (pc != DONE && instructionsExecuted < maxInstructions) {
			step();
		}
		if (instructionsExecuted >= maxInstructions)
			throw new Error("X86_64 simulator exceeded max instructions\n");
		this.out.close();
	}

	public State runForResult() {
		ByteArrayOutputStream os;
		os = new ByteArrayOutputStream();
		this.out = new PrintStream(os);
		try {
			while (pc != DONE && instructionsExecuted < maxInstructions) {
				step();
			}
			if (instructionsExecuted >= maxInstructions)
				throw new Error("X86_64 simulator exceeded max instructions\n");
			this.out.close();
			result = os.toString();
		} catch (Error e) {
		    String msg = e.getMessage();
			if (msg.startsWith("MiniJava failure")) {
                result = msg;
            } else if (msg.startsWith("Write at address ")) {
			    String addressStr = msg.replaceAll("Write at address ", "").replaceAll(" out of bounds", "");
                long address = Long.parseLong(addressStr);
                if (address <= STACK_BASE - STACK_SIZE && STACK_BASE - 2 * STACK_SIZE <= address) {
                    // This is a stack overflow, so just report a minijava failure
                    result = "MiniJava failure 2\n";
                } else {
                    result = msg + "\nThe program was:\n" + p;
                }
            } else {
				result = msg + "\nThe program was:\n" + p;
			}
		}
		return this;
	}

	public long getMemoryReferences() {
	    return ram.nReads + ram.nWrites;
    }
}
