package irsim;

import util.IndentingWriter;
import util.List;
import ir.interp.X86_64SimFrame;
import ir.temp.Label;
import ir.tree.IRExp;
import ir.tree.IRStm;
import ir.visitor.Visitor;

public class IRRet extends IRStm {

	@Override
	public <R> R accept(Visitor<R> v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dump(IndentingWriter out) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<IRExp> kids() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IRStm build(List<IRExp> kids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Label interp(X86_64SimFrame frame) {
		// TODO Auto-generated method stub
		return null;
	}

}
