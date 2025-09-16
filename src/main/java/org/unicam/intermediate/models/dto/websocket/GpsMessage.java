package org.unicam.intermediate.models.dto.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GpsMessage.LocationUpdate.class, name = "LOCATION_UPDATE"),
    @JsonSubTypes.Type(value = GpsMessage.Heartbeat.class, name = "HEARTBEAT"),
    @JsonSubTypes.Type(value = GpsMessage.StartTracking.class, name = "START_TRACKING"),
    @JsonSubTypes.Type(value = GpsMessage.StopTracking.class, name = "STOP_TRACKING")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class GpsMessage {
    private String type;
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationUpdate extends GpsMessage {
        private Double lat;
        private Double lon;

        private String processInstanceId;

        private String businessKey;

        public LocationUpdate(Double lat, Double lon) {
            super("LOCATION_UPDATE", Instant.now());
            this.lat = lat;
            this.lon = lon;
        }

    }

    @Data
    @AllArgsConstructor
    public static class Heartbeat extends GpsMessage {
        private String status;

        public Heartbeat() {
            super("HEARTBEAT", Instant.now());
            this.status = "alive";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartTracking extends GpsMessage {
        // Can specify either process instance ID or business key
        private String processInstanceId;
        private String businessKey;
        private String processDefinitionKey;
        private Integer updateInterval;

        public StartTracking(String processInstanceId) {
            super("START_TRACKING", Instant.now());
            this.processInstanceId = processInstanceId;
        }
    }

    @Data
    public static class StopTracking extends GpsMessage {
        private String processInstanceId;
        private String businessKey;

        public StopTracking() {
            super("STOP_TRACKING", Instant.now());
        }
    }
}