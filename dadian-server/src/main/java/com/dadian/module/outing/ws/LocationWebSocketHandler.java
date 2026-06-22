package com.dadian.module.outing.ws;

import com.dadian.module.user.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationWebSocketHandler extends TextWebSocketHandler {
    private final TokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Set<WebSocketSession>> outingSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }
        try {
            tokenService.validateAccessToken(token);
        } catch (Exception e) {
            try { session.close(CloseStatus.POLICY_VIOLATION); } catch (IOException ignored) {}
            return;
        }
        String userId = tokenService.getUserIdFromToken(token).toString();
        String outingId = extractOutingId(session);
        if (outingId == null) {
            try { session.close(CloseStatus.BAD_DATA); } catch (IOException ignored) {}
            return;
        }
        sessionUser.put(session.getId(), userId);
        outingSessions.computeIfAbsent(outingId, k -> ConcurrentHashMap.newKeySet()).add(session);

        // Broadcast teammate_joined to all other sessions in this outing
        sendTeamEvent(outingId, "teammate_joined", userId, session.getId());

        log.debug("WS connected: user={} outing={}", userId, outingId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String outingId = extractOutingId(session);
        String userId = sessionUser.get(session.getId());
        if (outingId == null || userId == null) return;

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (Exception e) { return; }

        String type = (String) payload.getOrDefault("type", "location_update");
        payload.put("userId", userId);

        // Handle team_status request: broadcast full teammate list
        if ("team_status".equals(type)) {
            payload.put("timestamp", System.currentTimeMillis());
            String broadcast = objectMapper.writeValueAsString(payload);
            Set<WebSocketSession> sessions = outingSessions.get(outingId);
            if (sessions != null) {
                for (WebSocketSession s : sessions) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(broadcast));
                    }
                }
            }
            return;
        }

        // Default: relay message to all other sessions in the outing
        String broadcast = objectMapper.writeValueAsString(payload);
        Set<WebSocketSession> sessions = outingSessions.get(outingId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen() && !s.getId().equals(session.getId())) {
                    s.sendMessage(new TextMessage(broadcast));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String outingId = extractOutingId(session);
        String userId = sessionUser.remove(session.getId());
        if (outingId != null) {
            Set<WebSocketSession> sessions = outingSessions.get(outingId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) outingSessions.remove(outingId);
            }

            // Broadcast teammate_left to remaining sessions
            if (userId != null) {
                sendTeamEvent(outingId, "teammate_left", userId, null);
            }
        }
        log.debug("WS disconnected: user={} outing={}", userId, outingId);
    }

    /**
     * Check if a user is currently connected (online) in a given outing.
     */
    public boolean isUserOnline(String outingId, String userId) {
        Set<WebSocketSession> sessions = outingSessions.get(outingId);
        if (sessions == null) return false;
        for (WebSocketSession s : sessions) {
            if (s.isOpen() && userId.equals(sessionUser.get(s.getId()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the set of online user IDs for a given outing.
     */
    public Set<String> getOnlineUsers(String outingId) {
        Set<String> online = new HashSet<>();
        Set<WebSocketSession> sessions = outingSessions.get(outingId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    String uid = sessionUser.get(s.getId());
                    if (uid != null) online.add(uid);
                }
            }
        }
        return online;
    }

    private void sendTeamEvent(String outingId, String eventType, String userId, String excludeSessionId) {
        Set<WebSocketSession> sessions = outingSessions.get(outingId);
        if (sessions == null) return;

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", eventType);
        event.put("userId", userId);
        event.put("timestamp", System.currentTimeMillis());

        try {
            String json = objectMapper.writeValueAsString(event);
            for (WebSocketSession s : sessions) {
                if (s.isOpen() && !s.getId().equals(excludeSessionId)) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send team event {} for outing {}", eventType, outingId, e);
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) return kv[1];
        }
        return null;
    }

    private String extractOutingId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("outing".equals(parts[i]) && i + 1 < parts.length) return parts[i + 1];
        }
        return null;
    }
}
