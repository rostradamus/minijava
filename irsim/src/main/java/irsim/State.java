package irsim;

import ir.temp.Label;
import ir.tree.*;
import ir.tree.CJUMP.RelOp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class State {
	public static final long HEAP_BASE = 0x1000L, HEAP_SIZE = 0x1000L;
	public static final long STACK_BASE = 0x8000L, STACK_SIZE = 0x1000L;
	static final long DONE = 99999999999999L;
	public boolean beVerbose = false;
	public long maxInstructions = 10000000;
	final static String[] regs = { 
		"%rax", "%rbx", "%rcx", "%rdx", "%rsi", "%rdi", "%rsp", "%rbp",
		"%r8", "%r9", "%r10", "%r11", "%r12", "%r13", "%r14", "%r15" 
	};
	final static String[] argregs = { 
		"%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9"
	};
	final static String SP = "%rsp";
	final static String FP = "%rbp";
	final static String ARG1 = "%rdi";
	final static String AX = "%rax";
	static {
		assertTrue(HEAP_BASE + HEAP_SIZE < STACK_BASE - STACK_SIZE);
	}
	public Program p;
	public Memory ram;
	Map<String, Long> registers;
	List<Map<String, Long>> registerstack;
	public long conditionl;
	public long conditionr;
	public long pc;
	public long hp;
	public PrintStream out = System.out;
	public long instructionsExecuted = 0L;
	public String result;
	private String moveTarget = AX;
	private List<String> returnTarget = new ArrayList<String>();

	public State(Program p) {
		ram = new Memory(HEAP_BASE, HEAP_BASE + HEAP_SIZE, STACK_BASE - STACK_SIZE, STACK_BASE);
		registers = new HashMap<String, Long>();
		registerstack = new LinkedList<Map<String, Long>>();
		returnTarget.add(null);
		pc = p.findMain();
		this.p = p;
		long sp = STACK_BASE - Memory.SIZEOF_LONG;
		ram.write(sp, DONE);
		sp -= Memory.SIZEOF_LONG;
		ram.write(sp, 0);
		setReg(SP, sp);
		setReg(FP, sp);
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
		for (int i = 0; i < regs.length; ++i) 
			newregisters.put(regs[i], registers.get(regs[i]));
		registerstack.add(0, registers);
		registers = newregisters;
	}

	public void do_return() {
		// If there is nothing on the stack, we are returning off the end so do nothing
		if (registerstack.size() > 0) {
			Map<String, Long> oldregisters = registerstack.remove(0);
			for (int i = 0; i < regs.length; ++i) 
				oldregisters.put(regs[i], registers.get(regs[i]));
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
	public static boolean conditionTrue(RelOp op, long conditionl, long conditionr) {
		return 
				op == RelOp.LT && conditionl < conditionr ||
				op == RelOp.ULT && unsignedCompare(conditionl, conditionr) < 0 ||
				op == RelOp.EQ && conditionl == conditionr ||
				op == RelOp.GT && conditionl > conditionr ||
				op == RelOp.UGT && unsignedCompare(conditionl, conditionr) > 0 ||
				op == RelOp.LE && conditionl <= conditionr ||
				op == RelOp.ULE && unsignedCompare(conditionl, conditionr) <= 0 ||
				op == RelOp.NE && conditionl != conditionr ||
				op == RelOp.GE && (conditionr < 0 || conditionl >= conditionr) ||
				op == RelOp.UGE && unsignedCompare(conditionl, conditionr) >= 0;
	}
	public boolean conditionTrue(String condition) {
		return conditionTrue(condition, conditionl, conditionr);
	}
	public void jump(long address) {
		this.pc = address;
	}
	public void jump(String label) {
		this.pc = p.labels.get(label);
	}
	public long getLabel(String label) {
		return p.labels.get(label);
	}
	public void step() {
		IRStm i = p.instructions.get((int)pc);
		if (beVerbose)
			System.out.println("" + pc + ": " + i.onOneLine());
		pc = pc + 1;
		execute(i);
		instructionsExecuted ++;
	}
	private void execute(IRStm i) {
		if (i instanceof LABEL) {
			throw new Error("Can't execute LABELS");
		} else if (i instanceof MOVE) {
			MOVE m = (MOVE) i;
			if (m.dst instanceof TEMP) {
				TEMP t = (TEMP) m.dst;
				moveTarget = t.temp.getName();
				long val = execute(m.src);
				if (beVerbose) 
					System.out.println(t + " <- " + val);
				setReg(t.temp.getName(), val);
			} else if (m.dst instanceof MEM) {
				MEM mem = (MEM) m.dst;
				long address = execute(mem.exp);
				long val = execute(m.src);
				if (beVerbose)
					System.out.println("M[" + hex(address) + "] <- " + val);
				ram.write(address, val);
			} else {
				throw new Error("Bogus dst of MOVE: " + m.dst.onOneLine());
			}
		} else if (i instanceof EXP) {
			EXP e = (EXP) i;
			moveTarget = null;
			execute(e.exp);
		} else if (i instanceof IRRet) {
			long fp = getReg(FP);
			long sp = fp;
			long newfp = ram.read(fp);
			sp += Memory.SIZEOF_LONG;
			long ra = ram.read(sp);
			long newsp = sp + Memory.SIZEOF_LONG;
			if (beVerbose) {
				System.out.println("Return to " + ra + " " + p.getFunctionAt(ra));
				System.out.println(SP + " <- " +  hex(newsp));
				System.out.println(FP + " <- " +  hex(newfp));
			}
			setReg(SP, newsp);
			setReg(FP, newfp);
			do_return();
			moveTarget = returnTarget.remove(returnTarget.size() - 1);
			if (moveTarget != null) {
				long ax = getReg(AX);
				if (beVerbose) {
					System.out.println(moveTarget + " <- " +  hex(ax));
				}
				setReg(moveTarget, ax);
			}
			jump(ra);
		} else if (i instanceof JUMP) {
			JUMP j = (JUMP) i;
			if (beVerbose)
				System.out.println("Jump to " + j.getExp());
			jump(execute(j.getExp()));
		} else if (i instanceof CJUMP) {
			CJUMP cj = (CJUMP) i;
			long left = execute(cj.left);
			long right = execute(cj.right);
			RelOp op = cj.getOp();
			boolean dojump = conditionTrue(op, left, right);
			if (beVerbose)
				if (dojump)
					System.out.println("CJump taken to " + cj.getTrueLabel());
				else
					System.out.println("CJump not taken, falling through to " + cj.getFalseLabel());
			Label target = dojump ? cj.getTrueLabel() : cj.getFalseLabel(); 
			jump(target.toString());
		} else if (i instanceof CMOVE) {
			CMOVE cm = (CMOVE) i;
			long left = execute(cm.left);
			long right = execute(cm.right);
			RelOp op = cm.getOp();
			boolean domove = conditionTrue(op, left, right);
			if (beVerbose)
				if (domove)
					System.out.println("CMove executed ");
				else
					System.out.println("CMove not executed ");
			if (domove) {
				if (cm.dst instanceof TEMP) {
					TEMP t = (TEMP) cm.dst;
					moveTarget = t.temp.getName();
					long val = execute(cm.src);
					if (beVerbose) 
						System.out.println(t + " <- " + val);
					setReg(t.temp.getName(), val);
				} else if (cm.dst instanceof MEM) {
					MEM mem = (MEM) cm.dst;
					long address = execute(mem.exp);
					long val = execute(cm.src);
					if (beVerbose)
						System.out.println("M[" + hex(address) + "] <- " + val);
					ram.write(address, val);
				} else {
					throw new Error("Bogus dst of MOVE: " + cm.dst.onOneLine());
				}
			}

		} else {
			throw new Error("Can't execute a " + i.onOneLine());
		}
	}
	private long execute(IRExp i) {
		if (i instanceof CONST) {
			return ((CONST)i).getValue();
		} else if (i instanceof BINOP) {
			BINOP b = (BINOP) i;
			long left = execute(b.left);
			long right = execute(b.right);
			switch(b.binop) {
			case PLUS:
				return left + right;
			case MINUS:
				return left - right;
			case MUL:
				return left * right;
			}
			throw new Error("Can't evaluate the binop " + b.binop.toString());
		} else if (i instanceof NAME) {
			NAME n = (NAME) i;
			Label lab = n.label;
			return getLabel(lab.toString());
		} else if (i instanceof TEMP) {
			TEMP t = (TEMP) i;
			long val = getReg(t.temp.getName());
            if (beVerbose)
                System.out.println(t + " -> " + val);
            return val;
		} else if (i instanceof CALL) {
			CALL c = (CALL) i;
			IRExp funcexp = c.getFunc();
			String label = "bogus";
			long func = DONE;
			if (funcexp instanceof NAME) {
				NAME fname = (NAME) funcexp;
				label = fname.label.toString();
			} else {
				func = execute(c.getFunc());
			}

			for (int arg = c.args.size() - 1; arg >= 0; --arg) {
				IRExp a = c.args.get(arg);
				long argValue = execute(a); 
				if (arg < argregs.length) {
					if (beVerbose)
						System.out.println("Setting " + argregs[arg] + " to " + argValue);
					setReg(argregs[arg], argValue);
				} else {
					long sp = getReg(SP);
					long newsp = sp - Memory.SIZEOF_LONG;
					ram.write(newsp, argValue);
					setReg(SP, newsp);
					if (beVerbose)
						System.out.println("Setting M[" + hex(newsp) + "] to " + argValue);
				}
			}
			if (func != DONE) {
				// An indirect call
				long sp = getReg(SP) - Memory.SIZEOF_LONG;
				long fp = getReg(FP);
				ram.write(sp, pc);
				sp -= Memory.SIZEOF_LONG;
				ram.write(sp, fp);
				long newfp = sp;
				setReg(SP, sp);
				setReg(FP, newfp);
				do_call();
				jump(func);
				returnTarget.add(moveTarget);
			} else {
				if (label.equals("_cs411println")  || label.equals("cs411println")) {
					// Simulate println
					out.print(getReg(ARG1) + "\n");
					if (beVerbose)
						System.out.println("Return to " + pc + " " + p.getFunctionAt(pc));
				} else if (label.equals("_cs411newobject") || label.equals("cs411newobject")) {
					// Simulate new object
					setReg(AX, alloc(getReg(ARG1)));
					if (beVerbose)
						System.out.println("Return to " + pc + " " + p.getFunctionAt(pc));
				} else if (label.equals("_cs411newarray") || label.equals("cs411newarray")) {
					// Simulate new array
					setReg(AX, allocArray(getReg(ARG1)));
					if (beVerbose)
						System.out.println("Return to " + pc + " " + p.getFunctionAt(pc));
				} else if (label.equals("_cs411error") || label.equals("cs411error")) {
					// Simulate error
					out.println("MiniJava failure " + getReg(ARG1));
					throw new Error("MiniJava failure " + getReg(ARG1) + "\n");
					// Bail
				} else {
					// push ra
					long sp = getReg(SP) - Memory.SIZEOF_LONG;
					long fp = getReg(FP);
					ram.write(sp, pc);
					sp -= Memory.SIZEOF_LONG;
					ram.write(sp, fp);
					long newfp = sp;
					setReg(FP, newfp);
					setReg(SP, sp);
					do_call();
					jump(label);
					returnTarget.add(moveTarget);
				}
			}
			return getReg(AX);
		} else if (i instanceof MEM) {
			MEM mem = (MEM) i;
			long address = execute(mem.exp);
			long val = ram.read(address);
			if (beVerbose)
				System.out.println("M[" + hex(address) + "] -> " + val);
			return val;
		} else {
			throw new Error("Can't evaluate a " + i.getClass().getName());
		}
	}

	public String hex(long reg) {
		return "0x" + Long.toString(reg, 16);
	}

	public void run() {
		result = "";
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
			if (e.getMessage().startsWith("MiniJava failure")) {
				result = e.getMessage();
			} else {
				result = e.getMessage() + "The program was:\n" + p;
			}
		}
		return this;
	}
}
