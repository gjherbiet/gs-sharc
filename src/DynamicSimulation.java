import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.stream.file.*;
import org.graphstream.algorithm.EdgeStability;
import org.graphstream.algorithm.StructureStability;
import org.graphstream.algorithm.community.*;
import org.graphstream.algorithm.measure.*;

public class DynamicSimulation {

	protected String referenceMarker = "value";
	protected String weightMarker = "weight";

	public static void main(String[] args) {
		// Networks used for simulation
		String[] networks = {
//			"nets/dgs/mall-120-0.dgs",
//			"nets/dgs/mall-120-1.dgs",
//			"nets/dgs/mall-120-2.dgs",
//			"nets/dgs/mall-120-3.dgs",
//			"nets/dgs/highway-120-0.dgs",
//			"nets/dgs/highway-120-1.dgs",
//			"nets/dgs/highway-120-2.dgs",
//			"nets/dgs/highway-120-3.dgs",
//			"nets/dgs/boids1000.dgs"
		};
		// Total: 5 nets

		// Algorithms used for simulation
		String[] algorithms = { "EpidemicCommunityAlgorithm",
				// "SyncEpidemicCommunityAlgorithm",
				"Leung", "Sharc", "NewSawSharc", "SandSharc" };
		// Total: 5 algos

		// Seeds for the simulations
		long[] seeds = { 32, 17, 5648, 110283, 299, 654, 13449, 8095, 48293805,
				323, 1153, 7607, 3709, 466644, 160, 125, 170910, 1036, 9085,
				976619 };
		// Total: 20 seeds

		// Total: 67200 runs (WLFR only)
		// Total: 112560 runs
		for (String net : networks)
			for (String algo : algorithms)
				for (long seed : seeds)
					new DynamicSimulation(net, algo, seed, args);

		return;

	}

	public DynamicSimulation(String network, String algorithm, long seed,
			String[] args) {
		System.out.println("----------");
		System.out.println("Starting new simulation with :");
		System.out.println("network file " + network);
		System.out.println("algorithm " + algorithm);
		System.out.println("seed " + seed);

		/*
		 * Random
		 */
		Random rng = new Random(seed);

		/*
		 * Graph
		 */
		Graph graph = new DefaultGraph("graph");

		try {

			/*
			 * Algorithm from parameter
			 */
			Class algorithmClass;
			DecentralizedCommunityAlgorithm algo;
			algorithmClass = Class
					.forName("org.graphstream.algorithm.community." + algorithm);
			algo = (DecentralizedCommunityAlgorithm) algorithmClass
					.newInstance();
			algo.init(graph);
			algo.setRandom(rng);

			/*
			 * Input stream
			 */
			FileSource input = FileSourceFactory.sourceFor(network);

			/*
			 * Stability measure
			 */
			EdgeStability S = new EdgeStability();
			S.init(graph);
			
			/*
			 * Structure stability
			 */
			StructureStability R = new StructureStability(algo.getMarker(), weightMarker);
			R.init(graph);

			/*
			 * Metrics
			 */
//			Modularity Q = new Modularity(algo.getMarker());
//			Q.init(graph);
//
//			Modularity WQ = new Modularity(algo.getMarker(), weightMarker);
//			WQ.init(graph);
//
//			NormalizedMutualInformation NMI = new NormalizedMutualInformation(
//					algo.getMarker(), referenceMarker);
//			NMI.init(graph);

			CommunityDistribution D = new CommunityDistribution(
					algo.getMarker());
			D.init(graph);
			
			CommunityAssignment A = new CommunityAssignment(algo.getMarker());
			A.init(graph);

			/*
			 * Output file
			 */
			FileOutputStream out = new FileOutputStream("log/"
					+ getNetworkName(network) + "_" + algorithm + "_" + seed
					+ ".log");
			PrintStream p = new PrintStream(out);

			/*
			 * Connect everything together
			 */
			input.addSink(graph);
			input.addSink(algo);
			input.addSink(S);
			input.addSink(R);

			/*
			 * Run the simulation
			 */
			int step = 0;
			input.begin(network);
			while (input.nextStep() || step < 100) {
				p.println("S = " + step);

				// Copy edge stability attribute to weight
				for (Edge e: graph.getEdgeSet()) {
					e.setAttribute(weightMarker, e.getAttribute("stability.age"));
				}
				
				algo.compute();

//				p.println("Q = " + Q.getMeasure());
//				p.println("WQ = " + WQ.getMeasure());
//				p.println("NMI = " + NMI.getMeasure());
				p.println("D = " + D);
				p.println(A);
				p.println("R = " + R);

				step++;
			}

			/*
			 * Close stuff properly
			 */
			p.close();
			input.end();
			algo.terminate();

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

	protected String getNetworkName(String network) {
		File f = new File(network);
		String name = f.getName();
		return name.substring(0, name.lastIndexOf('.'));
	}
}
