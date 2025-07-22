package org.unicam.intermediate.models.pojo;

import lombok.Getter;
import lombok.Setter;

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

}
