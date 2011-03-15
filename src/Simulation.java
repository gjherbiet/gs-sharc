import java.io.*;
import java.util.ArrayList;
import java.util.Random;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.stream.file.*;
import org.graphstream.algorithm.StructureStability;
import org.graphstream.algorithm.community.*;
import org.graphstream.algorithm.measure.*;

public class Simulation {

	protected String referenceMarker = "value";
	protected String weightMarker = "weight";

	public static void main(String[] args) {
		// Networks used for simulation
		ArrayList<String> networks = new ArrayList<String>();
		// addClassicalNetworks(networks, args);
		//addWlfrNetworks(networks, args);
		// addGnNetworks(networks, args);
		// addLfrNetworks(networks, args);
		// Total: 804 nets

		// Algorithms used for simulation
		String[] algorithms = { "EpidemicCommunityAlgorithm",
				// "SyncEpidemicCommunityAlgorithm",
				"Leung", "Sharc", "SawSharc", "AltSawSharc", "NewSawSharc" };
		// Total: 7 algos

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
					new Simulation(net, algo, seed, args);

		return;

	}

	public Simulation(String network, String algorithm, long seed, String[] args) {
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
			 * Structure stability
			 */
			StructureStability R = new StructureStability(algo.getMarker(), weightMarker);
			R.init(graph);

			/*
			 * Metrics
			 */
			Modularity Q = new Modularity(algo.getMarker());
			Q.init(graph);

			Modularity WQ = new Modularity(algo.getMarker(), weightMarker);
			WQ.init(graph);

			NormalizedMutualInformation NMI = new NormalizedMutualInformation(
					algo.getMarker(), referenceMarker);
			NMI.init(graph);

			CommunityDistribution D = new CommunityDistribution(
					algo.getMarker());
			D.init(graph);

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
			input.addSink(R);

			/*
			 * Run the simulation
			 */
			int step = 0;
			input.begin(network);
			while (input.nextStep() || step < 100) {
				p.println("S = " + step);

				algo.compute();

				p.println("Q = " + Q.getMeasure());
				p.println("WQ = " + WQ.getMeasure());
				p.println("NMI = " + NMI.getMeasure());
				p.println("D = " + D);
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

	// 4 nets
	public static void addClassicalNetworks(ArrayList<String> list) {
		// list.add("nets/gml/football.gml");
		// list.add("nets/gml/karate.gml");
		// list.add("nets/gml/dolphins.gml");
		list.add("nets/gml/lesmis.gml");
	}

	// 80 nets
	public static void addGnNetworks(ArrayList<String> list, String[] args) {
		String[] mu = { "0.10", "0.20", "0.30", "0.40", "0.50", "0.60", "0.70",
				"0.75" };
		for (int i = 0; i < mu.length; i++) {
			for (int s = 0; s < 10; s++) {
				list.add("nets/dgs/gn/gn-" + mu[i] + "-" + s + ".dgs");
			}
		}
	}

	// 360 nets
	public static void addLfrNetworks(ArrayList<String> list, String[] args) {
		String[] size = { "1000", "5000" };
		String[] conf = { "S", "B" };
		String[] mu = { "0.10", "0.20", "0.30", "0.40", "0.50", "0.60", "0.70",
				"0.80", "0.90" };

		for (int j = 0; j < size.length; j++) {
			for (int k = 0; k < conf.length; k++) {
				for (int i = 0; i < mu.length; i++) {
					for (int s = 0; s < 10; s++) {
						list.add("nets/dgs/lfr/lfr-" + size[j] + "-" + conf[k]
								+ "-" + mu[i] + "-" + s + ".dgs");
					}
				}
			}
		}
	}

	// 360 nets
	public static void addWlfrNetworks(ArrayList<String> list, String[] args) {
		String[] size = { "5000" };
		String[] conf = { "S", "B" };
		String[] mu = { "0.50", "0.80" };
		String[] muw = { "0.10", "0.20", "0.30", "0.40", "0.50", "0.60",
				"0.70", "0.80" };

		int l = new Integer(args[0]);
		int s = new Integer(args[1]);

		for (int j = 0; j < size.length; j++) {
			for (int k = 0; k < conf.length; k++) {
				for (int i = 0; i < mu.length; i++) {
					// for (int l = 0; l < muw.length; l++) {
					// for (int s = 0; s < 10; s++) {
					list.add("nets/dgs/wlfr/wlfr-" + size[j] + "-" + conf[k]
							+ "-" + mu[i] + "-" + muw[l] + "-" + s + ".dgs");
					// }
					// }
				}
			}
		}
	}
}
