package org.unicam.intermediate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unicam.intermediate.models.Coordinate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ParticipantPositionService {

    public final LogService logService;

    public ParticipantPositionService(LogService logService) {
        this.logService = logService;
    }

    private final Map<String, Coordinate> positions = new ConcurrentHashMap<>();

    public void updatePosition(String participantId, double lat, double lon, String destination) {
        positions.put(participantId, new Coordinate(lat, lon, destination));
    }

    public Coordinate getPosition(String participantId) {
        return positions.get(participantId);
    }


    public String getDestination(String participantId) {
        var position = positions.get(participantId);
        return ("Destination: " + position.destination) ;
    }

    public Map<String, Coordinate> getAllPositions() {
        return Map.copyOf(positions);
    }

    public void clear() {
        positions.clear();
    }
}
