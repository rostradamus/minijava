package irsim.test;

import ir.parser.ParseException;
import ir.parser.Parser;
import ir.tree.IR;
import irsim.Program;
import irsim.Sim;
import irsim.State;
import org.junit.Test;
import test.TestIR;
import translate.Fragments;
import util.SampleCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSimExecute {
    private boolean beVerbose = true;

	protected State accept(Fragments f) {
		Program p = new Program(f);
		State s = new State(p);
		System.out.println("Running:");
		s.beVerbose = beVerbose;
		s.runForResult();
		System.out.println("Static: " + p.countInstructions() + " instructions generated");
		System.out.println("Dynamic: " + s.instructionsExecuted + " instructions executed");
		return s;
	}

	protected void run(String input) {
		String result = Sim.ulate(input, beVerbose).result;
		System.out.println("Resulting output:");
		System.out.print(result);
	}

	protected State accept(File file) {
		Fragments f = null;
		Program p = null;
		State s = null;
		String outname = null;
		String outtmpname = null;
		try {
			System.out.println("parsing file: "+file);
			f = Parser.parse(file);
			System.out.println("running file: "+file);
			p = new Program(f);
			if (beVerbose && false) {
                System.out.println("Program:");
                System.out.println(p.dump());
            }
			s = new State(p);
			Matcher m = Pattern.compile("\\.ir+$").matcher(file.getPath());
			outname = m.replaceAll(".out");
			outtmpname = m.replaceAll(".out.tmp");
			File out = new File(outtmpname);
			s.beVerbose = beVerbose;
			s.run(out);
		} catch (Error e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (p == null) return s;
		System.out.println("Static: " + p.countInstructions() + " instructions generated");
		System.out.println("Dynamic: " + s.instructionsExecuted + " instructions executed");
		System.out.println("Running diff ...");
		Process proc;
		try {
			proc = Runtime.getRuntime().exec("diff -c " + outname + " " + outtmpname);
			proc.waitFor();
			InputStream diffout = proc.getInputStream();
			byte sb[] = new byte[64 * 1024];
			StringBuilder sbr = new StringBuilder();
			int nread;
			while ((nread = diffout.read(sb)) > 0) 
				for (int ii = 0; ii < nread; ++ii)
				sbr.append((char)sb[ii]);
			System.out.println(sbr.toString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (outtmpname != null) {
			File out = new File(outtmpname);
			if (!out.delete()) 
				System.out.println("Can't remove temporary output file " + outtmpname);
		}
		return s;
	}

	private void test(String expected, Fragments p) {
		State s = accept(p);
		String output = s.result;
		if (!expected.equals(output)) {
			System.out.println("Expected \"" + expected + "\" but got \"" + output + "\"");
		}
	}
	
	@Test
	public void TestNOP() throws Exception {
		test("", TestIR.makeFragments(null, IR.NOP));
	}

	@Test 
	public void testExecuteSampleCode() throws Exception {
		File[] files = SampleCode.sampleFiles("ir");
		for (File file : files) {
			try {
				accept(file);
			} catch (Error e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
