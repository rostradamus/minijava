package irsim;

import ir.parser.ParseException;
import ir.parser.Parser;
import translate.Fragments;
import util.Utils;

import java.io.File;

public class Sim {
	public static State ulate(String program) {
		return ulate(program, true);
	}
	public static State ulate(String program, boolean beVerbose) {
		if (beVerbose) System.out.print("Parsing string:\n"+ program);
		Program p;
		try {
			Fragments f = Parser.parse(program);
			p = new Program(f);
		} catch (Error e) {
			System.out.println("Parsing problem, bailing");
			e.printStackTrace();
			System.out.println("The program is:\n" + program);
			return null;
		} catch (ParseException e) {
			System.out.println("Parsing problem, bailing");
			e.printStackTrace();
			System.out.println("The program is:\n" + program);
			return null;
		}
		if (beVerbose && false) {
            System.out.println("Program:");
            System.out.println(p.dump());
        }
		State s = new State(p);
		s.beVerbose = beVerbose;
		String result = s.runForResult().result;
        System.out.println(result);
		System.out.println("Static: " + p.countInstructions() + " instructions generated");
		System.out.println("Dynamic: " + s.instructionsExecuted + " instructions executed");
        System.out.println("Memory: " + (s.ram.nReads + s.ram.nWrites) + " references");
		return s;
	}
    public static void main(String[] args) {
        for (String filename : args) {
            String program = Utils.getContents(new File(filename));
            ulate(program, true);
        }
    }
}
