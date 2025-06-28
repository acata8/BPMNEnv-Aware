package org.unicam.intermediate.service;

import org.springframework.stereotype.Service;
import org.unicam.intermediate.models.Coordinate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParticipantPositionService {

    private final Map<String, Coordinate> positions = new ConcurrentHashMap<>();

    public void updatePosition(String participantId, double lat, double lon, String destination) {
        positions.put(participantId, new Coordinate(lat, lon, destination));

        Coordinate coord = getPosition(participantId);

        System.out.println("[Participant Position] " + coord);
    }

    public Coordinate getPosition(String participantId) {
        return positions.get(participantId);
    }

    public Map<String, Coordinate> getAllPositions() {
        return Map.copyOf(positions);
    }

    public void clear() {
        positions.clear();
    }
}
