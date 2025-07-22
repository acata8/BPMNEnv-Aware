package org.unicam.intermediate.models.pojo;

import java.util.List;

public class LogicalPlace {
    private String id;
    private String name;
    private List<Condition> conditions;
    private String expression;
    private String place;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
}
