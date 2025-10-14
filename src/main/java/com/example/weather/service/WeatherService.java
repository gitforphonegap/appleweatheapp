package com.example.weather.service;

import com.example.weather.model.WeatherResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Simple in-memory cache: key=address, value=CacheEntry
    private static class CacheEntry {
        WeatherResponse response;
        Instant timestamp;
        CacheEntry(WeatherResponse response, Instant timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
    }
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_SECONDS = 1800; // 30 minutes

    public WeatherResponse getWeather(String address) {
        Instant now = Instant.now();
        CacheEntry entry = cache.get(address.toLowerCase());
        if (entry != null && now.getEpochSecond() - entry.timestamp.getEpochSecond() < CACHE_DURATION_SECONDS) {
            WeatherResponse cached = entry.response;
            cached.setFromCache(true);
            return cached;
        }

        String geoUrl = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                .queryParam("q", address)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("limit", "1")
                .toUriString();

        Map[] geoResults = restTemplate.getForObject(geoUrl, Map[].class);
        if (geoResults == null || geoResults.length == 0) {
            throw new RuntimeException("Location not found: " + address);
        }

        Map<String, Object> geoData = geoResults[0];
        double lat = Double.parseDouble((String) geoData.get("lat"));
        double lon = Double.parseDouble((String) geoData.get("lon"));
        String displayName = (String) geoData.get("display_name");

        // Extract address details if available
        Map<String, Object> addressDetails = new LinkedHashMap<>();
        if (geoData.containsKey("address")) {
            Map<String, Object> addr = (Map<String, Object>) geoData.get("address");
            for (Map.Entry<String, Object> entry2 : addr.entrySet()) {
                addressDetails.put(entry2.getKey(), entry2.getValue());
            }
        }

        String weatherUrl = UriComponentsBuilder.fromHttpUrl("https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current_weather", true)
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum")
                .queryParam("timezone", "auto")
                .toUriString();

        Map<String, Object> weatherData = restTemplate.getForObject(weatherUrl, Map.class);

        WeatherResponse response = new WeatherResponse();
        response.setAddress(address);
        response.setDisplayName(displayName);
        response.setCurrentWeather((Map<String, Object>) weatherData.get("current_weather"));
        response.setForecast((Map<String, Object>) weatherData.get("daily"));
        // Add address details to the response if present
        if (!addressDetails.isEmpty()) {
            response.getCurrentWeather().put("address_details", addressDetails);
        }
        response.setFromCache(false);

        // Cache the response
        cache.put(address.toLowerCase(), new CacheEntry(response, now));

        return response;
    }
}
