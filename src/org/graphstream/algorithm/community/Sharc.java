/*
 * Copyright (C) 2010 Guillaume-Jean Herbiet
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.graphstream.algorithm.community;

import java.util.*;

import org.graphstream.graph.*;

/**
 * Implementation of the SHARC community detection algorithm (Sharper Heuristic
 * for Assignment of Robust Communities).
 * 
 * @reference G.-J. Herbiet and P. Bouvry, SHARC: community-based partitioning
 *            for mobile ad hoc networks using neighborhood similarity, in
 *            <i>IEEE WoWMoM 2010 (IEEE WoWMoM 2010)</i>, Montreal, Canada, 6
 *            2010.
 * 
 * @author Guillaume-Jean Herbiet
 * 
 */
public class Sharc extends EpidemicCommunityAlgorithm {
	protected HashMap<Object, Double> communityCounts;

	/**
	 * New instance of the SHARC community detection algorithm, not attached to
	 * a graph and using the default community marker.
	 */
	public Sharc() {
		super();
	}

	/**
	 * New instance of the SHARC community detection algorithm, attached to the
	 * specified graph and using the specified marker for the community
	 * attribute.
	 * 
	 * @param graph
	 *            the graph to which the algorithm will be applied
	 * @param marker
	 *            String used as marker for the community attribute
	 */
	public Sharc(Graph graph, String marker) {
		super(graph, marker);
	}

	/**
	 * New instance of the SHARC community detection algorithm, attached to the
	 * specified graph and using the default community marker.
	 * 
	 * @param graph
	 *            the graph to which the algorithm will be applied
	 */
	public Sharc(Graph graph) {
		super(graph);
	}

	@Override
	public void computeNode(Node u) {
		/*
		 * Perform standard Sharc assignment
		 */
		super.computeNode(u);

		/*
		 * If the node final score is 0, i.e. there were no preferred community,
		 * fall back to the "simple" epidemic assignment
		 */
		if (((Double) u.getAttribute(marker + ".score")) == 0.0) {
			//System.out.println(u.getId() + " Falling back to epidemic.");
			communityScores.clear();
			communityScores = communityCounts;
			super.computeNode(u);
		}

	}

	/**
	 * Compute the scores for all relevant communities for the selected node
	 * using the SHARC algorithm
	 * 
	 * @param u
	 *            Node for which the computation is performed
	 * @complexity O(DELTA^2) where DELTA is the average node degree in the
	 *             network
	 */
	@Override
	protected void communityScores(Node u) {
		/*
		 * Compute the "simple" count of received messages for each community.
		 * This will be used as a fallback metric if the maximum "Sharc" score
		 * is 0, meaning there is no preferred community.
		 */
		super.communityScores(u);
		communityCounts = communityScores;

		/*
		 * Reset the scores for each communities
		 */
		communityScores = new HashMap<Object, Double>();

		/*
		 * Iterate over the nodes that this node "hears"
		 */
		for (Edge e : u.getEnteringEdgeSet()) {
			Node v = e.getOpposite(u);

			/*
			 * Update the count for this community
			 */
			if (v.hasAttribute(marker)) {
				// Update score
				if (communityScores.get(v.getAttribute(marker)) == null)
					communityScores.put(v.getAttribute(marker),
							similarity(u, v));
				else
					communityScores.put(v.getAttribute(marker),
							communityScores.get(v.getAttribute(marker))
									+ similarity(u, v));
			}
		}
	}

	/**
	 * Neighborhood similarity between two nodes
	 * 
	 * @param a
	 *            The first node
	 * @param b
	 *            The second node
	 * @return The similarity value between the two nodes
	 * @complexity O(DELTA) where DELTA is the average node degree in the
	 *             network
	 */
	protected Double similarity(Node a, Node b) {
		Double similarity = 0.0;

		for (Edge e : a.getEnteringEdgeSet()) {
			Node v = e.getOpposite(a);
			if (!b.hasEdgeFrom(v.getId()))
				similarity += 1.0;
		}

		for (Edge e : b.getEnteringEdgeSet()) {
			Node v = e.getOpposite(b);
			if (!a.hasEdgeFrom(v.getId()))
				similarity += 1.0;
		}

		if (a.getDegree() == 0 && b.getDegree() == 0)
			return 0.0;
		else
			return 1 - (similarity / (a.getDegree() + b.getDegree()));
	}
}
