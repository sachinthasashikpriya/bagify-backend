package com.mycompany.app.user.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    // Map to keep track of active SSE connections per seller ID
    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Integer sellerId) {
        // Create an emitter with a 5-minute timeout (300,000 milliseconds)
        SseEmitter emitter = new SseEmitter(300_000L);

        emitters.put(sellerId, emitter);

        // Remove the emitter when it is completed, timed out, or errors
        emitter.onCompletion(() -> emitters.remove(sellerId));
        emitter.onTimeout(() -> emitters.remove(sellerId));
        emitter.onError((e) -> emitters.remove(sellerId));

        try {
            // Send an initial handshake event to establish the connection successfully
            emitter.send(SseEmitter.event().name("init").data("Connected successfully"));
        } catch (IOException e) {
            emitters.remove(sellerId);
        }

        return emitter;
    }

    public void sendVerificationUpdate(Integer sellerId, String status) {
        SseEmitter emitter = emitters.get(sellerId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("verification-update")
                        .data(Map.of("status", status)));
            } catch (IOException e) {
                emitters.remove(sellerId);
            }
        }
    }
}
