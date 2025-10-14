package com.example.weather.model;

import lombok.Data;

@Data
public class GeoLocation {
    private double latitude;
    private double longitude;
    private String displayName;
}
