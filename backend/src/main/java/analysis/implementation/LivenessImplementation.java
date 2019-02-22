package analysis.implementation;

import ir.temp.Temp;

import java.util.Collections;

import util.List;

import analysis.FlowGraph;
import analysis.Liveness;
import util.graph.Node;


public class LivenessImplementation<N> extends Liveness<N> {

    public LivenessImplementation(FlowGraph<N> graph) {
        super(graph);
    }

    @Override
    public List<Temp> liveOut(Node<N> node) {
        // This dummy implementation says that nothing is live
        return List.empty();
    }

    private List<Temp> liveIn(Node<N> node) {
        // This dummy implementation says that nothing is live
        return List.empty();
    }

    private String shortList(List<Temp> l) {
        java.util.List<String> reall = new java.util.ArrayList<String>();
        for (Temp t : l) {
            reall.add(t.toString());
        }
        Collections.sort(reall);
        return String.valueOf(reall);
    }

    private String dotLabel(Node<N> n) {
        return shortList(liveIn(n)) +
                "\\n" +
                n +
                ": " +
                n.wrappee() +
                "\\n" +
                shortList(liveOut(n));
    }

    private double fontSize() {
        return (Math.max(30, Math.sqrt(Math.sqrt(g.nodes().size() + 1)) * g.nodes().size() * 1.2));
    }

    private double lineWidth() {
        return (Math.max(3.0, Math.sqrt(g.nodes().size() + 1) * 1.4));
    }

    private double arrowSize() {
        return Math.max(2.0, Math.sqrt(Math.sqrt(g.nodes().size() + 1)));
    }

    @Override
    public String dotString(String name) {
        StringBuilder out = new StringBuilder();
        out.append("digraph \"Flow graph\" {\n");
        out.append("labelloc=\"t\";\n");
        out.append("fontsize=").append(fontSize()).append(";\n");
        out.append("label=\"").append(name).append("\";\n");

        out.append("  graph [size=\"6.5, 9\", ratio=fill];\n");
        for (Node<N> n : g.nodes()) {
            out.append("  \"").append(dotLabel(n)).append("\" [fontsize=").append(fontSize());
            out.append(", style=\"setlinewidth(").append(lineWidth()).append(")\", color=").append(g.isMove(n) ? "green" : "blue");
            out.append("]\n");
        }
        for (Node<N> n : g.nodes()) {
            for (Node<N> o : n.succ()) {
                out.append("  \"").append(dotLabel(n)).append("\" -> \"").append(dotLabel(o)).append("\" [arrowhead = normal, arrowsize=").append(arrowSize()).append(", style=\"setlinewidth(").append(lineWidth()).append(")\"];\n");
            }
        }

        out.append("}\n");
        return out.toString();
    }

}
