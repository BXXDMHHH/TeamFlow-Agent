package com.teamflow.slice01.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple HTTP smoke test so the backend can be verified before WebSocket.
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of(
                "service", "teamflow-slice-01-backend",
                "status", "UP"
        );
    }
}
