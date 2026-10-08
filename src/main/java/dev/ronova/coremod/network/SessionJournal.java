package dev.ronova.coremod.network;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded last-receipt journal for independent client connection sessions. */
final class SessionJournal<T> {
    private final int maximumSessions;
    private final LinkedHashMap<Long, T> entries = new LinkedHashMap<>(8, 0.75F, true);

    SessionJournal(int maximumSessions) {
        this.maximumSessions = Math.max(1, maximumSessions);
    }

    synchronized T get(long sessionId) {
        return entries.get(sessionId);
    }

    synchronized void put(long sessionId, T value) {
        entries.put(sessionId, value);
        while (entries.size() > maximumSessions) {
            Map.Entry<Long, T> eldest = entries.entrySet().iterator().next();
            entries.remove(eldest.getKey());
        }
    }
}
