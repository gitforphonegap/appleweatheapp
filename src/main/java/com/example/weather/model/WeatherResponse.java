package com.example.weather.model;

import lombok.Data;
import java.util.Map;

@Data
public class WeatherResponse {
    private String address;
    private String displayName;
    private Map<String, Object> currentWeather;
    private Map<String, Object> forecast;
    private boolean fromCache;
}
