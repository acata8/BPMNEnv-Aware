package org.unicam.intermediate.models.pojo;

import java.util.List;

public class EnvironmentData {
    private List<Place> places;
    private List<Edge> edges;
    private List<LogicalPlace> logicalPlaces;
    private List<View> views;

    public List<Place> getPlaces() { return places; }
    public void setPlaces(List<Place> places) { this.places = places; }

    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }

    public List<LogicalPlace> getLogicalPlaces() { return logicalPlaces; }
    public void setLogicalPlaces(List<LogicalPlace> logicalPlaces) { this.logicalPlaces = logicalPlaces; }

    public List<View> getViews() { return views; }
    public void setViews(List<View> views) { this.views = views; }
}
