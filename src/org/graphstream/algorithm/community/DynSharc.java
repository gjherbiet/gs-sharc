package org.graphstream.algorithm.community;

import org.apache.commons.math.stat.descriptive.moment.*;
import org.graphstream.graph.*;

public class DynSharc extends NewSawSharc {

	protected int stallingThreshold = 5;
	protected int breakPeriod = 5;

	public DynSharc() {
		super();
	}

	public DynSharc(Graph graph, String marker) {
		super(graph, marker);
	}

	public DynSharc(Graph graph, String marker, String weightMarker) {
		super(graph, marker, weightMarker);
	}

	public DynSharc(Graph graph, String marker, int stallingThreshold,
			int breakPeriod) {
		super(graph, marker);
		setParameters(stallingThreshold, breakPeriod);
	}

	public DynSharc(Graph graph) {
		super(graph);
	}

	public DynSharc(Graph graph, int stallingThreshold, int breakPeriod) {
		super(graph);
		setParameters(stallingThreshold, breakPeriod);
	}

	public void setParameters(int stallingThreshold, int breakPeriod) {
		this.stallingThreshold = stallingThreshold;
		this.breakPeriod = breakPeriod;
	}

	/**
	 * Compute the node new assignment using the SAw-SHARC algorithm
	 * 
	 * @param u
	 *            Node for which the computation is performed
	 * @complexity O(DELTA^2) where DELTA is the average node degree in the
	 *             network
	 */
	@Override
	public void computeNode(Node u) {

		/*
		 * Recall previous community (will be used for originator update)
		 */
		Object previousCommunity = u.getAttribute(marker);
		Double previousScore = (Double) u.getAttribute(marker + ".score");
		u.setAttribute(marker + ".old_score", previousScore);

		/*
		 * Revert to self-community if no more neighbors or manage first
		 * iteration of the simulation
		 */
		if (u.getDegree() == 0 || previousCommunity == null) {
			originateCommunity(u);
		}

		/*
		 * Process node break mode
		 */
		else if (u.hasAttribute(marker + ".break")) {
			int remaining = (Integer) u.getAttribute(marker + ".break");

			if (!u.hasAttribute(marker + ".break_done")) {
				/*
				 * Search for a neighbor in break mode, otherwise, initiate a
				 * new community
				 */
				Object newCommunity = null;
				for (Edge e : u.getEnteringEdgeSet()) {
					Node v = e.getOpposite(u);
					if (v.hasAttribute(marker)
							&& v.hasAttribute(marker + ".break")
							&& v.hasAttribute(marker + ".break_done")
							&& v.hasAttribute(marker + ".broken_community")
							&& v.<Object> getAttribute(
									marker + ".broken_community").equals(
									u.<Object> getAttribute(marker
											+ ".broken_community"))) {
						newCommunity = v.getAttribute(marker);
					}
				}
				if (newCommunity == null) {
					originateCommunity(u);
				} else {
					u.setAttribute(marker, newCommunity);
				}
				u.setAttribute(marker + ".break_done", true);
			}

			/*
			 * Decrease break mode lifetime
			 */
			if (remaining > 0) {
				u.setAttribute(marker + ".break", remaining - 1);
			}

			/*
			 * Terminate break mode on lifetime expiration
			 */
			else if (remaining == 0) {
				u.removeAttribute(marker + ".break");
				u.removeAttribute(marker + ".broken_community");
				u.removeAttribute(marker + ".break_done");
			}
		}

		/*
		 * Still no update, perform standard SHARC assignment
		 */
		else
			super.computeNode(u);

		/*
		 * Set the originator: Currently originator, pass the role to a neighbor
		 * node with higher score than me or to the neighbor with the highest
		 * score if i changed community
		 */
		updateOriginator(u, previousCommunity);

		/*
		 * Update freshness counter and stalling value or reset everything if
		 * the node has changed community
		 */
		if (previousCommunity == null
				|| previousCommunity.equals(u.getAttribute(marker))) {
			int freshness;
			if (u.hasAttribute(marker + ".freshness")) {
				freshness = (Integer) u.getAttribute(marker + ".freshness");
			} else {
				freshness = 0;
			}

			updateFreshessCounter(u);

			/*
			 * Has freshness be incremented ? If no, increment the
			 */
			if (freshness >= u.getNumber(marker + ".freshness")) {

				if (u.hasAttribute(marker + ".stalling")) {
					u.setAttribute(marker + ".stalling",
							u.getNumber(marker + ".stalling") + 1);
				} else {
					u.setAttribute(marker + ".stalling", 1);
				}
			} else
				u.setAttribute(marker + ".stalling", 0);

		} else {
			u.setAttribute(marker + ".freshness", 0);
			u.setAttribute(marker + ".stalling", 0);
		}

		if (u.hasAttribute(marker + ".stalling")
				&& u.getNumber(marker + ".stalling") > 0) {
			/*
			 * Enable break mode if the stalling threshold is reached
			 */
			if (u.getNumber(marker + ".stalling") >= stallingThreshold) {

				// Enable break mode
				u.setAttribute(marker + ".break", breakPeriod - 1);
				u.setAttribute(marker + ".broken_community",
						u.getAttribute(marker));
			}
		}

	}

	@Override
	protected void originateCommunity(Node u) {
		super.originateCommunity(u);
		u.setAttribute(marker + ".originator", true);
		u.setAttribute(marker + ".new_originator", true);
	}

	protected void updateFreshessCounter(Node u) {
		/*
		 * Initialize freshness counter
		 */
		int freshness = 0;
		if (u.hasAttribute(marker + ".freshness"))
			freshness = (Integer) u.getAttribute(marker + ".freshness");

		/*
		 * Update the edge validity threshold for the current node
		 */
		setEdgeThreshold(u);

		/*
		 * Set the freshness counter to the highest value heard from one of the
		 * neighbors of the same community
		 */
		for (Edge e : u.getEnteringEdgeSet()) {
			Node v = e.getOpposite(u);
			if (v.hasAttribute(marker)
					&& v.hasAttribute(marker + ".freshness")
					&& v.<Object> getAttribute(marker).equals(
							u.<Object> getAttribute(marker))) {
				if (similarity(u, v) >= u.getNumber(marker + ".threshold")
						&& v.getNumber(marker + ".freshness") > freshness) {
					freshness = (int) v.getNumber(marker + ".freshness");
				}
			}
		}

		/*
		 * If the node is originator, increase this count
		 */
		if (u.hasAttribute(marker + ".originator"))
			freshness++;

		/*
		 * Update the freshness attribute
		 */
		u.setAttribute(marker + ".freshness", freshness);
	}

	protected void updateOriginator(Node u, Object previousCommunity) {
		if (u.hasAttribute(marker + ".originator")) {
			Double score;
			if (previousCommunity == null
					|| previousCommunity.equals(u.getAttribute(marker)))
				score = (Double) u.getAttribute(marker + ".score");
			else {
				/*
				 * Originator node changed community
				 */
				score = Double.MIN_VALUE;

			}

			Node originator = u;
			for (Edge e : u.getEnteringEdgeSet()) {
				Node v = e.getOpposite(u);
				if (v.hasAttribute(marker)
						&& v.<Object> getAttribute(marker).equals(
								previousCommunity)
						&& v.hasAttribute(marker + ".score")
						&& (Double) v.getAttribute(marker + ".score") > score) {
					score = (Double) v.getAttribute(marker + ".score");
					originator = v;
				}
			}

			/*
			 * Update originator if necessary
			 */
			if (originator != u) {
				u.removeAttribute(marker + ".originator");
				originator.setAttribute(marker + ".originator", true);
				originator.setAttribute(marker + ".new_originator", true);

			}

			if (u.hasAttribute(marker + ".new_originator"))
				u.removeAttribute(marker + ".new_originator");
		}

	}

	protected void setEdgeThreshold(Node u) {
		/*
		 * Mean and standard deviation
		 */
		Mean mean = new Mean();
		StandardDeviation stdev = new StandardDeviation();
		for (Edge e : u.getEnteringEdgeSet()) {
			Node v = e.getOpposite(u);
			if (v.hasAttribute(marker)
					&& v.<Object> getAttribute(marker) == u
							.<Object> getAttribute(marker)) {
				mean.increment(similarity(u, v));
				stdev.increment(similarity(u, v));
			}
		}

		/*
		 * Only consider as valid edges for which the similarity is above (mean
		 * - 0.5 * stdev)
		 */
		double threshold = mean.getResult() - 0.5 * stdev.getResult();
		u.setAttribute(marker + ".threshold", threshold);
	}
}
