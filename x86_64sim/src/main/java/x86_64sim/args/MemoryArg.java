package x86_64sim.args;

import x86_64sim.State;

public class MemoryArg extends Arg {
    private String base, index;
    private long scale, offset;

    public MemoryArg(long offset, String base, String index, long scale) {
        this.offset = offset;
        this.base = base;
        this.index = index;
        this.scale = scale;
        if (scale != 1 && scale != 2 && scale != 4 && scale != 8) throw new UnsupportedOperationException("Illegal scale " + scale + " in " + this);
    }

    public long addr(State state) {
        long ptr = (base == null ? 0 : state.getReg(base)) + offset + (index == null ? 0 : state.getReg(index) * scale);
        return ptr;
    }
    public long read(State state) {
        long ptr = (base == null ? 0 : state.getReg(base)) + offset + (index == null ? 0 : state.getReg(index) * scale);
        long value = state.ram.read(ptr);
        return value;
    }

    public void write(State state, long value) {
        long ptr = (base == null ? 0 : state.getReg(base)) + offset + (index == null ? 0 : state.getReg(index) * scale);
        if (state.beVerbose)
            System.out.println(toString() + " <- " + value);
        state.ram.write(ptr, value);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (offset != 0) sb.append(offset);
        sb.append('(');
        if (base != null) sb.append(base);
        if (index != null) {
            sb.append(',');
            sb.append(index);
            sb.append(',');
            sb.append(scale);
        }
        sb.append(')');
        return sb.toString();
    }
}
