package irsim;

import ir.tree.*;
import translate.DataFragment;
import translate.Fragment;
import translate.Fragments;
import translate.ProcFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class Program {
	List<IRStm> instructions = new ArrayList<IRStm>();
	Map<String, Long> labels = new HashMap<String, Long>();
	Map<Long, Long> staticData = new HashMap<Long, Long>();
	Map<Long, String> relocationData = new HashMap<Long, String>();
	boolean inText = true;
	long pc;
	long dc = State.HEAP_BASE;
	public Program() {
		instructions = new ArrayList<IRStm>();
		labels = new HashMap<String, Long>();
		pc = 0;
	}

	public Program(Fragments fs) {
		for (Fragment f : fs) {
			if (f instanceof ProcFragment) {
				ProcFragment pf = (ProcFragment) f;
				doText();
				add(pf.getLabel().toString());
				for (IRStm s : ((ProcFragment) f).getTraceScheduledBody()) {
					if (s instanceof LABEL) {
						LABEL l = (LABEL) s;
						add(l.getLabel().toString());
					} else {
						add(s);
					}
				}
				add(new IRRet());
			} else if (f instanceof DataFragment) {
				doData();
				IRData d = ((DataFragment) f).getBody();
				add(d.getLabel().toString());
				for (IRExp e : d) {
					if (e instanceof CONST) {
						CONST c = (CONST) e;
						doQuad(Integer.toString(c.getValue()));
					} else if (e instanceof NAME) {
						NAME n = (NAME) e;
						doQuad(n.getLabel().toString());
					}
				}
			} else {
				throw new Error("Illegal fragment type");
			}
		}
	}

	public long getLabel(String label) {
		return labels.get(label);
	}

	public void doData() {
		inText = false;
	}

	public void doText() {
		inText = true;
	}

	public void doAlign(String value) {
		assertEquals(value, "4");
	}

	Pattern numberPattern = Pattern.compile("-?[0-9]+");
	Matcher numberMatcher = numberPattern.matcher("");

	public void doQuad(String value) {
		numberMatcher.reset(value);
		if (numberMatcher.matches())
			staticData.put(dc, Long.parseLong(value));
		else if (labels.containsKey(value)) 
			staticData.put(dc, labels.get(value));
		else
			relocationData.put(dc, value);
		dc += 8;
	}
	public void add(String label) {
		if (inText)
			labels.put(label, pc);
		else 
			labels.put(label, dc);
	}

	public void add(IRStm i) {
		instructions.add(i);
		pc ++;
	}

	private String labelAt(int pc) {
		for (String label : labels.keySet()) {
			Long addr = labels.get(label);
			if (addr == pc)
				return label;
		}
		return null;
	}
	Pattern boringLabel = Pattern.compile(".*_[0-9]+$");
	Matcher m = boringLabel.matcher("");
	
	public String getFunctionAt(long pc) {
		String closestLabel = null;
		long closestAddr = -1;
		if (pc == 99999999999999L)
			return "__start";
		for (String label : labels.keySet()) {
			m.reset(label);
			if (m.matches()) continue;
			Long addr = labels.get(label);
			if (addr <= pc && addr > closestAddr) {
				closestAddr = addr;
				closestLabel = label;
			}
		}
		return closestLabel == null ? "unknown" : closestLabel;
	}

	public String dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("Program with " + labels.size() + " labels and " + instructions.size() + " instructions\n");
		for (int pc = 0; pc < instructions.size(); ++pc) {
			String l = labelAt(pc);
			if (l != null) sb.append(l).append(":").append("\n");
			sb.append(instructions.get(pc)).append("\n");
		}
		return sb.toString();
	}

	public String toString() {
		return dump();
	}

	public long findMain() {
		long answer = -1;
		for (String label : labels.keySet()) {
			if (label.endsWith("main")) {
				if (answer == -1) {
					answer = labels.get(label);
				} else {
					throw new RuntimeException("Duplicate main method???");
				}
			}
		}
		return answer == -1 ? 0 : answer;
	}

	public int countInstructions() {
		return instructions.size();
	}
	
	public long writeStatic(Memory ram) {
		for (Long addr : staticData.keySet()) {
			Long value = staticData.get(addr);
			ram.write(addr, value);
		}
		for (Long addr : relocationData.keySet()) {
			String value = relocationData.get(addr);
			ram.write(addr, labels.get(value));
		}
		return dc;
	}
}
