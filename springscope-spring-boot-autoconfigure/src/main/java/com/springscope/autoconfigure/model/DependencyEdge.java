package com.springscope.autoconfigure.model;

public record DependencyEdge(
        String source,
        String target,
        String type
) {

    public static final String DEPENDENCY = "DEPENDENCY";

    public static DependencyEdge dependency(String source, String target) {
        return new DependencyEdge(source, target, DEPENDENCY);
    }
}
