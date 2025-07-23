package org.unicam.intermediate.models.pojo;

import lombok.Getter;
import lombok.Setter;
import org.unicam.intermediate.models.LocationArea;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class Place {

    private String id;

    private String name;
    private String temperature;

    private List<List<Double>> coordinates;

    private Map<String, Object> attributes;

    // mette in cache e non ricarica n volte
    private transient LocationArea locationArea;

    public LocationArea getLocationArea() {
        if (locationArea == null) {
            double minLat = coordinates.stream().mapToDouble(c -> c.get(1)).min().orElse(0);
            double maxLat = coordinates.stream().mapToDouble(c -> c.get(1)).max().orElse(0);
            double minLon = coordinates.stream().mapToDouble(c -> c.get(0)).min().orElse(0);
            double maxLon = coordinates.stream().mapToDouble(c -> c.get(0)).max().orElse(0);
            locationArea = new LocationArea(minLon, maxLon, minLat, maxLat);
        }
        return locationArea;
    }


}
