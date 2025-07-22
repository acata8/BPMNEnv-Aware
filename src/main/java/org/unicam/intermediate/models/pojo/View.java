package org.unicam.intermediate.models.pojo;

import java.util.List;
import java.util.Map;

public class View {
    private String id;
    private String name;
    private List<String> logicalPlaces;
    private Map<String, Object> attributes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getLogicalPlaces() { return logicalPlaces; }
    public void setLogicalPlaces(List<String> logicalPlaces) { this.logicalPlaces = logicalPlaces; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
