/**
 * 
 */
package org.graphstream.algorithm.community;

import java.util.HashMap;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

/**
 * @author Guillaume-Jean Herbiet
 * 
 */
public class SandSharc extends DynSharc {

	/**
	 * 
	 */
	public SandSharc() {
		super();
	}

	/**
	 * @param graph
	 * @param marker
	 */
	public SandSharc(Graph graph, String marker) {
		super(graph, marker);
	}

	public SandSharc(Graph graph, String marker, String weightMarker) {
		super(graph, marker, weightMarker);
	}

	/**
	 * @param graph
	 * @param marker
	 * @param stallingThreshold
	 * @param breakPeriod
	 */
	public SandSharc(Graph graph, String marker, int stallingThreshold,
			int breakPeriod) {
		super(graph, marker, stallingThreshold, breakPeriod);
	}

	/**
	 * @param graph
	 */
	public SandSharc(Graph graph) {
		super(graph);
	}

	/**
	 * @param graph
	 * @param stallingThreshold
	 * @param breakPeriod
	 */
	public SandSharc(Graph graph, int stallingThreshold, int breakPeriod) {
		super(graph, stallingThreshold, breakPeriod);
	}

	protected void updateOriginator(Node u, Object previousCommunity) {
		/*
		 * Current node has originator token
		 */
		if (u.hasAttribute(marker + ".originator")
				&& !u.hasAttribute(marker + ".new_originator")) {

			/*
			 * Originator stayed in the same community: Make the originator
			 * token wander using a "local optimum favored" weighted random
			 * walk.
			 */
			if (previousCommunity != null
					&& previousCommunity.equals(u.getAttribute(marker))) {

				double score = u.getNumber(marker + ".score");
				double max = Double.NEGATIVE_INFINITY;
				HashMap<Node, Double> scores = new HashMap<Node, Double>();
				double total = 0;

				/*
				 * Search for the maximum neighboring score in the same
				 * community update total at the same time
				 */
				for (Edge e : u.getEnteringEdgeSet()) {
					Node v = e.getOpposite(u);
					if (v.hasAttribute(marker)
							&& v.<Object> getAttribute(marker).equals(
									u.<Object> getAttribute(marker))
							&& v.getId() != u.getAttribute(marker + ".originator_from")) {
						scores.put(v, v.getNumber(marker + ".score"));
						total += v.getNumber(marker + ".score");
						if (v.getNumber(marker + ".score") > max)
							max = v.getNumber(marker + ".score");
					}
				}

				/*
				 * Current node is the local optimum: Originator token will pass
				 * only with a given probability. Otherwise token is passed
				 * using weighted random walk
				 */
				if (max > score || rng.nextDouble() < (max / score)) {

					double random = rng.nextDouble() * total;
					Node originator = null;
					for (Node v : scores.keySet()) {
						if (random <= scores.get(v) &&
							v.getId() != u.getAttribute(marker + ".originator_from")) {
								originator = v;
						} else {
							random -= scores.get(v);
						}
					}

					if (originator != null) {
						u.removeAttribute(marker + ".originator");
						u.removeAttribute(marker + ".originator_from");
						
						originator.setAttribute(marker + ".originator", true);
						originator.setAttribute(marker + ".new_originator",
								true);
						originator.setAttribute(marker + ".originator_from", u.getId());
					}
				}
			}

			/*
			 * Originator node changed community: Simply pass the originator
			 * token to the neighbor of previous community with the highest
			 * score
			 */
			else {
				u.removeAttribute(marker + ".originator");
				u.removeAttribute(marker + ".originator_from");

				double score = Double.NEGATIVE_INFINITY;
				Node originator = null;
				for (Edge e : u.getEnteringEdgeSet()) {
					Node v = e.getOpposite(u);
					if (v.hasAttribute(marker)
							&& v.<Object> getAttribute(marker).equals(
									previousCommunity)
							&& v.hasAttribute(marker + ".score")
							&& v.getNumber(marker + ".score") > score) {
						score = v.getNumber(marker + ".score");
						originator = v;
					}
				}

				/*
				 * A neighbor is found
				 */
				if (originator != null) {
					originator.setAttribute(marker + ".originator", true);
					originator.setAttribute(marker + ".new_originator", true);
				}
			}
		}

		/*
		 * The node has been processed, so it can't be a new originator
		 */
		if (u.hasAttribute(marker + ".new_originator"))
			u.removeAttribute(marker + ".new_originator");
	}

}
