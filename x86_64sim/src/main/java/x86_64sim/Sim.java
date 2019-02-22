package x86_64sim;

import util.Utils;
import x86_64sim.parser.SimParser;

import java.io.File;

public class Sim {
	public static State ulate(String program) {
		return ulate(program, false);
	}
	public static State ulate(String program, boolean beVerbose) {
		if (beVerbose) System.out.print("Parsing string:\n"+ program);
		Program p;
		try {
			p = SimParser.parse(program);
		} catch (Error e) {
            System.out.println(e);
//			System.out.println("Parsing problem, bailing");
//			System.out.println("The program is:\n" + program);
			return null;
		}
		//			System.out.println("Program:");
		//			System.out.println(p.dump());
		State s = new State(p);
		s.beVerbose = beVerbose;
		String result = s.runForResult().result;
        System.out.println(result);
		System.out.println("Static: " + p.countInstructions() + " instructions generated");
		System.out.println("Dynamic: " + s.instructionsExecuted + " instructions executed");
        System.out.println("Memory: " + s.getMemoryReferences() + " references");
		return s;
	}

    public static void main(String[] args) {
        for (String filename : args) {
            String program = Utils.getContents(new File(filename));
            ulate(program, false);
        }
    }
}
