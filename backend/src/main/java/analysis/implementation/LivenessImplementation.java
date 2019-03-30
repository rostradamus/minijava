package analysis.implementation;

import ir.temp.Temp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import util.ActiveSet;
import util.List;

import analysis.FlowGraph;
import analysis.Liveness;
import util.graph.Node;


public class LivenessImplementation<N> extends Liveness<N> {

    private Map<Node<N>, ActiveSet<Temp>> liveIns;
    private Map<Node<N>, ActiveSet<Temp>> liveOuts;

    public LivenessImplementation(FlowGraph<N> graph) {
        super(graph);

        //initialize
        liveIns = new HashMap<Node<N>, ActiveSet<Temp>>();
        liveOuts = new HashMap<>();

        //each instruction associate with an active set
        for (Node<N> node : graph.nodes()) {
            liveIns.put(node, new ActiveSet<>());
            liveOuts.put(node, new ActiveSet<>());
        }


        for (Node<N> node : graph.nodes()) {
            livenessInImpl(node);
            livenessOutImpl(node);
        }
    }

    private void livenessInImpl(Node<N> node) {
        //in[n] = use[n] U (out[n] – def[n])
        ActiveSet<Temp> in = liveIns.get(node);
        in.addAll(g.use(node));
        in.addAll(liveOuts.get(node).remove(g.def(node)));
    }

    private void livenessOutImpl(Node<N> node) {
        //out[n] = U { in[s] | s ε succ[n] }
        ActiveSet<Temp> out = liveOuts.get(node);
        for (Node<N> s : node.succ()) {
            out.addAll(liveIns.get(s));
        }
    }

    @Override
    public List<Temp> liveOut(Node<N> node) {
        return liveOuts.get(node).getElements();
    }

    private List<Temp> liveIn(Node<N> node) {
        return liveIns.get(node).getElements();
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
