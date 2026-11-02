package com.springscope.autoconfigure.model;

public record HealthResponse(String status, String version) {

    public static HealthResponse up(String version) {
        return new HealthResponse("UP", version);
    }
}
