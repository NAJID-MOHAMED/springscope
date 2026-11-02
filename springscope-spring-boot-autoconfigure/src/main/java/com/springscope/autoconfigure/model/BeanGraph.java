package com.springscope.autoconfigure.model;

import java.util.List;

public record BeanGraph(
        List<Node> nodes,
        List<DependencyEdge> edges
) {

    public BeanGraph {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public record Node(String id, String name) {
    }
}
