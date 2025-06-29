package org.unicam.intermediate.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Service
public class LogService {

    private static final int MAX_LOGS = 1000;

    private final LinkedList<String> logs = new LinkedList<>();

    public synchronized void addLog(String message) {
        if (logs.size() >= MAX_LOGS) {
            logs.removeFirst();
        }
        logs.addLast(message);
        System.out.println("[LogService] " + message);
    }

    public synchronized List<String> getLogs() {
        return Collections.unmodifiableList(new LinkedList<>(logs));
    }

    public synchronized void clear() {
        logs.clear();
    }
}
