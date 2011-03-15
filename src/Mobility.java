import java.util.Random;

import org.graphstream.algorithm.EdgeStability;
import org.graphstream.algorithm.community.*;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSinkDGS;

public class Mobility {

	protected String stability = "stability.age";

	protected Double maxEdgeWidth = 5.0;
	protected Double maxNodeSize = 50.0;

	protected int N = 50;

	protected double maxX = 100;
	protected double maxY = 100;

	protected double margin = 10;
	protected double range = 35;

	protected long seed = 0;
	protected Random rng;

	protected int steps = 600;
	protected double breathingPeriod = 150;

	public static void main(String[] args) {
		new Mobility();
	}

	public Mobility() {
		rng = new Random(seed);

		Graph graph = new DefaultGraph("graph");
		graph.addAttribute("ui.antialias");
		graph.addAttribute("stylesheet", styleSheet());
		graph.display(false);

		EdgeStability stab = new EdgeStability();
		stab.init(graph);
		//
		// FileSinkDGS output = new FileSinkDGS();
		//
		SandSharc algo = new SandSharc(graph, "community", stability);
		algo.setRandom(rng);
		algo.staticMode();

		try {
			// output.begin("mobility-test.dgs");
			// graph.addSink(output);
			graph.addSink(stab);

			/*
			 * Initialize the graph and the first connections
			 */
			init(graph);
			updateConnections(graph);
			updateEdgesDisplay(graph, stability, maxEdgeWidth);

			Thread.sleep(1000);

			/*
			 * Make the graph move
			 */
			for (int s = 0; s < steps; s++) {

				graph.stepBegins(s);

				move(graph);
				updateConnections(graph);
				updateEdgesDisplay(graph, stability, maxEdgeWidth);

				algo.compute();
				updateNodesDisplay(graph, algo.getMarker(), maxNodeSize);

				if (s > 0 && s % breathingPeriod == 0)
					breathe(graph);

			}

			/*
			 * Properly terminate things
			 */
			// output.end();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Initialize the graph
	 */
	protected void init(Graph graph) {
		for (int i = 0; i < N; i++) {
			String id = (new Integer(i)).toString();
			Node n = graph.addNode(id);

			if ((i % 2) == 0)
				n.setAttribute("x", rng.nextDouble() * ((maxX / 2.0) - margin));
			else
				n.setAttribute("x", ((maxX / 2.0) + margin) + rng.nextDouble()
						* ((maxX / 2.0) - margin));
			n.setAttribute("y", maxY * rng.nextDouble());

			n.setAttribute("target.x", n.getAttribute("x"));
			n.setAttribute("target.y", n.getAttribute("y"));
			n.setAttribute("value", (i % 2));
		}
		breathe(graph);
	}

	protected void breathe(Graph graph) {
		for (Node n : graph.getNodeSet()) {
			if ((Double) n.getAttribute("target.x") < (maxX / 2.0))
				n.setAttribute("target.x",
						((maxX / 2.0) + margin) + rng.nextDouble()
								* ((maxX / 2.0) - margin));
			else
				n.setAttribute("target.x", rng.nextDouble()
						* ((maxX / 2.0) - margin));

			if (rng.nextBoolean())
				n.setAttribute("target.y", (Double) n.getAttribute("y")
						+ margin);
			else
				n.setAttribute("target.y", (Double) n.getAttribute("y")
						- margin);

			n.setAttribute(
					"increment.x",
					((Double) n.getAttribute("target.x") - (Double) n
							.getAttribute("x")) / breathingPeriod);
			n.setAttribute("increment.y", 0.0);
		}
	}

	protected void move(Graph graph) {
		for (Node n : graph.getNodeSet()) {
			n.setAttribute(
					"x",
					(Double) n.getAttribute("x")
							+ (Double) n.getAttribute("increment.x"));
			n.setAttribute(
					"y",
					(Double) n.getAttribute("y")
							+ (Double) n.getAttribute("increment.y"));
		}
	}

	protected void updateConnections(Graph graph) {
		for (Node u : graph.getNodeSet())
			for (Node v : graph.getNodeSet())
				if (!u.equals(v) && distance(u, v) <= range
						&& !u.hasEdgeFrom(v.getId()))
					graph.addEdge(u.getId() + "_" + v.getId(), u.getId(),
							v.getId());
				else if (!u.equals(v) && distance(u, v) > range
						&& u.hasEdgeFrom(v.getId()))
					graph.removeEdge(u.getId(), v.getId());
	}

	protected double distance(Node u, Node v) {
		Double u_x = (Double) u.getAttribute("x");
		Double u_y = (Double) u.getAttribute("y");
		Double v_x = (Double) v.getAttribute("x");
		Double v_y = (Double) v.getAttribute("y");

		return Math.sqrt(Math.pow(u_x - v_x, 2) + Math.pow(u_y - v_y, 2));
	}

	protected String styleSheet() {
		String styleSheet = "graph { padding: 10px; fill-color:rgb(203,203,203); }"
				+ "edge  { fill-mode:dyn-plain; fill-color: rgb(170,170,170), rgb(0,0,0); size-mode: dyn-size; size:1px; }"
				+ "node { text-style:bold; text-color: rgb(85,85,85); size-mode: dyn-size; size:5px; }";

		/*
		 * Generate classes for the different communities
		 */
		for (int i = 0; i < 1000; i++) {
			styleSheet += "node.community_" + i + " { fill-color: rgb("
					+ rng.nextInt(255) + "," + rng.nextInt(255) + ","
					+ rng.nextInt(255) + "); }";
		}
		return styleSheet;
	}

	protected void updateEdgesDisplay(Graph graph, String marker, Double max) {
		/*
		 * Search the maximum possible value
		 */
		double maxValue = Double.NEGATIVE_INFINITY;
		for (Edge e : graph.getEdgeSet()) {
			if (e.hasAttribute(marker)) {
				Double v = (Double) e.getAttribute(marker);
				if (v != null && v > maxValue)
					maxValue = v;
			}
		}

		for (Edge e : graph.getEdgeSet()) {
			// No edge has a weight marker:
			// keep minimal width but use darkest color for better visibility
			if (maxValue == Double.NEGATIVE_INFINITY) {
				e.setAttribute("ui.size", 1);
				e.setAttribute("ui.color", 1.0);
			}
			// Normal case: use ratio of the weight over the max weight
			else if (e.hasAttribute(marker) && e.getAttribute(marker) != null
					&& maxValue != 0) {
				e.setAttribute("ui.color", e.getNumber(marker) / maxValue);
				e.setAttribute("ui.size",
						1 + (max * e.getNumber(marker) / maxValue));
			}
			// Fall-back case: use min width and lightest color
			else {
				e.setAttribute("ui.color", 0.0);
				e.setAttribute("ui.size", 1);
			}

		}
	}

	protected void updateNodesDisplay(Graph graph, String marker, Double max) {
		/*
		 * Search the maximum possible value
		 */
		double maxValue = Double.NEGATIVE_INFINITY;
		for (Node n : graph.getNodeSet()) {
			if (n.hasAttribute(marker + ".score")) {
				Double v = (Double) n.getAttribute(marker + ".score");
				if (v > maxValue)
					maxValue = v;
			}
		}

		for (Node n : graph.getNodeSet()) {
			// No edge has a score marker:
			// keep minimal width but use darkest color for better visibility
			if (maxValue == Double.NEGATIVE_INFINITY) {
				n.setAttribute("ui.size", 5);
			}
			// Normal case: use ratio of the score over the max score
			else if (n.hasAttribute(marker + ".score")
					&& n.getAttribute(marker + ".score") != null
					&& maxValue != 0.0) {
				n.setAttribute("ui.size",
						1 + (max * n.getNumber(marker + ".score") / maxValue));
			}
			// Fall-back case: use min size
			else {
				n.setAttribute("ui.size", 5);
			}
			
			n.setAttribute("label",
					n.getId() + "<" + n.getAttribute(marker) + ">");

			if (n.hasAttribute(marker + ".originator")) {
				n.setAttribute("ui.style", "text-color: red;");
			}
			else {
				n.setAttribute("ui.style", "text-color: rgb(85,85,85);");
			}
		}
	}
}
