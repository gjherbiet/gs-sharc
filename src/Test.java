import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.stream.GraphParseException;
import org.graphstream.stream.file.*;
import org.graphstream.stream.file.FileSinkImages.*;
import org.graphstream.algorithm.EdgeStability;
import org.graphstream.algorithm.StructureStability;
import org.graphstream.algorithm.community.*;
import org.graphstream.algorithm.measure.*;

public class Test {

	protected String stability = "stability.age";

	protected Double maxEdgeWidth = 10.0;
	protected Double maxNodeSize = 50.0;

	protected Random rng;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Test();
		return;
	}

	public Test() {
		Graph graph = new DefaultGraph("graph", false, true);
		//FileSourceGML input = new FileSourceGML();
		//FileSourceDGS1And2 input = new FileSourceDGS1And2();
		FileSourceDGS input = new FileSourceDGS();

		rng = new Random(1153);

		SandSharc algo = new SandSharc(graph, "community", stability);
		//AutonomyOrientedCommunityDetection algo = new AutonomyOrientedCommunityDetection(graph);
		algo.setRandom(rng);

		EdgeStability E = new EdgeStability();
		E.init(graph);
		
		//StructureStability R = new StructureStability(algo.getMarker(), stability);
		//R.init(graph);

//		Modularity Q = new Modularity(algo.getMarker());
//		Q.init(graph);
//
//		Modularity WQ = new Modularity(algo.getMarker(), stability);
//		WQ.init(graph);
//
		CommunityDistribution D = new CommunityDistribution(algo.getMarker());
		D.init(graph);
		
		CommunityAssignment A = new CommunityAssignment(algo.getMarker());
		A.init(graph);
//
//		NormalizedMutualInformation NMI = new NormalizedMutualInformation(
//				algo.getMarker(), "value");
//		NMI.init(graph);

		graph.addAttribute("stylesheet", styleSheet());
		graph.addAttribute("ui.antialias", true);
		//graph.display(false);
		
		//OutputPolicy outputPolicy = OutputPolicy.ByStepOutput;
		//String prefix = "output_";
		//OutputType type = OutputType.PNG;
		//Resolution resolution = Resolutions.HD720;

		//FileSinkImages fsi = new FileSinkImages(prefix, type, resolution, outputPolicy );
		//fsi.setStyleSheet(styleSheet());
		//fsi.setQuality(Quality.HIGH);
		//fsi.setHighQuality();
		//fsi.setLayoutPolicy( LayoutPolicy.NoLayout );

		try {
			// FileSource input =
			// FileSourceFactory.sourceFor("nets/dgs/test-0.dgs");
			
			input.addSink(graph);
			input.addSink(E);
//			input.addSink(R);
			graph.addSink(algo);
			//graph.addSink(fsi);
			
			//graph.read("nets/gml/karate.gml");
			

			//input.begin( "nets/gml/karate.gml" );
			//input.begin( "nets/gml/football.gml" );
			//input.begin("nets/gml/lesmis.gml");
			
			//System.out.println(getNetworkName("nets/dgs/wlfr/wlfr-5000-S-0.80-0.80-9.dgs"));
			//input.begin("nets/dgs/wlfr/wlfr-5000-S-0.80-0.80-9.dgs");
			//input.begin( "nets/dgs/highway-120-0.dgs" );
			//input.begin( "nets/dgs/mall-120-0.dgs" );
			//input.begin( "nets/dgs/infocom2006.dgs" );
			input.begin( "nets/dgs/cabs_5000.dgs" );
			//input.begin( "nets/dgs/mobility-test.dgs" );
			// input.begin( "nets/dgs/boids1000.dgs" );

			// input.begin("nets/dgs/test-0.dgs");
			// input.begin( "nets/dgs/tutorial2.dgs" );
			// input.begin( "test-2.dgs" );
			// input.begin( "test-02.dgs" );
			//algo.staticMode();

			
			int step = 0;
			while ( input.nextStep() || step < 20) {
				System.out.println("==> Step " + step);

				/*
				 * Update the edges display
				 */
				updateEdgesDisplay(graph, stability, maxEdgeWidth);

				/*
				 * Start the computation
				 */
				algo.compute();
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				/*
				 * Update the nodes display
				 */
				updateNodesDisplay(graph, algo.getMarker(), maxNodeSize);

//				System.out.println("Q = " + Q.getMeasure());
//				System.out.println("WQ = " + WQ.getMeasure());
//				System.out.println("NMI = " + NMI.getMeasure());
				System.out.println("D = " + D);
				System.out.println(A);
//				System.out.println("R = " + R);

				step++;
			}

			input.end();
			algo.terminate();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void simulationStep() {
		
	}

	protected String styleSheet() {
		System.out.println("Generating stylesheet");
		
		String styleSheet = "graph { padding: 10px; fill-color:rgb(255,255,255); }" //203,203,203
				+ "edge  { fill-mode:dyn-plain; fill-color: rgb(170,170,170), rgb(0,0,0); size-mode: dyn-size; size:1px; }"
				+ "node { text-style:bold; text-color: rgb(85,85,85); size-mode: dyn-size; size:5px; }";

		/*
		 * Generate classes for the different communities
		 */
		for (int i = 0; i < 1000; i++) {
			styleSheet += "node.community_" + i + " { fill-color: rgb("
					+ rng.nextInt(255) + "," + rng.nextInt(255) + ","
					+ rng.nextInt(255) + "); }";
			if (i % 100 == 0) {
				System.out.print(".");
			}
		}
		System.out.println();
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
				n.setAttribute("ui.size", 10);
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
				n.setAttribute("ui.size", 10);
			}
			
			//n.setAttribute("label",
			//		n.getId() + "<" + n.getAttribute(marker) + ">" + n.getNumber(marker + ".freshness"));

			if (n.hasAttribute(marker + ".originator")) {
				n.setAttribute("ui.style", "text-color: red;");
			}
			else {
				n.setAttribute("ui.style", "text-color: rgb(85,85,85);");
			}
		}
	}
	
	protected String getNetworkName(String network) {
		File f = new File(network);
		String name = f.getName();
		return name.substring(0, name.lastIndexOf('.'));
	}
}
