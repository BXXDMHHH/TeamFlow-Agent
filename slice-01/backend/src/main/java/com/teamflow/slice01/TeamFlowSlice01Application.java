package com.teamflow.slice01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the smallest TeamFlow chat experiment.
 *
 * <p>Keep this application intentionally small. The goal of this slice is only:
 *
 * <pre>
 * A -> WebSocket -> Spring Boot -> WebSocket -> B
 * </pre>
 */
@SpringBootApplication
public class TeamFlowSlice01Application {

    public static void main(String[] args) {
        SpringApplication.run(TeamFlowSlice01Application.class, args);
    }
}
