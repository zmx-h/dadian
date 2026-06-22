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

        payload.put("userId", userId);
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
        }
        log.debug("WS disconnected: user={} outing={}", userId, outingId);
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
