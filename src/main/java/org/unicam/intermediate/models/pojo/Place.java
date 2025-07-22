package org.unicam.intermediate.models.pojo;

import java.util.List;
import java.util.Map;

public class Place {
    private String id;
    private String name;
    private List<List<Double>> coordinates;
    private Map<String, Object> attributes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<List<Double>> getCoordinates() { return coordinates; }
    public void setCoordinates(List<List<Double>> coordinates) { this.coordinates = coordinates; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
