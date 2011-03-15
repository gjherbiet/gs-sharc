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

import org.graphstream.graph.Graph;

/**
 * Alternative implementation of the SAw-SHARC community detection algorithm
 * (Stability Aware Sharper Heuristic for Assignment of Robust Communities).
 * 
 * @reference TO BE PROVIDED
 * @author Guillaume-Jean Herbiet
 * 
 */
public class AltSawSharc extends SawSharc {

	protected boolean forcedYes = true;
	
	/**
	 * New instance of the SAw-SHARC community detection algorithm, not attached
	 * to a graph and using the default community marker.
	 */
	public AltSawSharc() {
		super();
	}

	/**
	 * New instance of the SAw-SHARC community detection algorithm, attached to
	 * the specified graph and using the specified marker for the community
	 * attribute.
	 * 
	 * @param graph
	 *            the graph to which the algorithm will be applied
	 * @param marker
	 *            String used as marker for the community attribute
	 */
	public AltSawSharc(Graph graph, String marker) {
		super(graph, marker);
	}

	/**
	 * Create a new SAw-SHARC algorithm instance, attached to the specified
	 * graph, using the specified marker to store the community attribute, and
	 * the specified weightMarker to retrieve the weight attribute of graph
	 * edges.
	 * 
	 * @param graph
	 *            graph to which the algorithm will be applied
	 * @param marker
	 *            community attribute marker
	 * @param weightMarker
	 *            edge weight marker
	 */
	public AltSawSharc(Graph graph, String marker, String weightMarker) {
		super(graph, marker, weightMarker);
	}

	/**
	 * New instance of the SAw-SHARC community detection algorithm, attached to
	 * the specified graph and using the default community marker.
	 * 
	 * @param graph
	 *            the graph to which the algorithm will be applied
	 */
	public AltSawSharc(Graph graph) {
		super(graph);
	}
}
