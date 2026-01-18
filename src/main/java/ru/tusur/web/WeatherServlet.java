package ru.tusur.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class WeatherServlet extends HttpServlet {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");

        if (!isAuthed(req)) {
            resp.sendRedirect("login.jsp");
            return;
        }
        req.getRequestDispatcher("/weather.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html; charset=UTF-8");

        if (!isAuthed(req)) {
            resp.sendRedirect("login.jsp");
            return;
        }

        String city = req.getParameter("city");
        if (city == null || city.trim().isEmpty()) {
            req.setAttribute("error", "Введите город");
            req.getRequestDispatcher("/weather.jsp").forward(req, resp);
            return;
        }

        try {
            CityGeo geo = geocodeCity(city.trim());
            if (geo == null) {
                req.setAttribute("error", "Город не найден");
                req.getRequestDispatcher("/weather.jsp").forward(req, resp);
                return;
            }

            WeatherData wd = fetchWeather(geo.lat, geo.lon);

            req.setAttribute("city", geo.displayName);
            req.setAttribute("temp", wd.temperature);
            req.setAttribute("wind", wd.windSpeed);
            req.setAttribute("state", wd.state);

            req.getRequestDispatcher("/weather.jsp").forward(req, resp);

        } catch (Exception e) {
            req.setAttribute("error", "Ошибка получения погоды: " + e.getMessage());
            req.getRequestDispatcher("/weather.jsp").forward(req, resp);
        }
    }

    private CityGeo geocodeCity(String city) throws Exception {
        String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + q + "&count=1&language=ru&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) return null;

        JsonNode root = mapper.readTree(response.body());
        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.size() == 0) return null;

        JsonNode r = results.get(0);

        double lat = r.get("latitude").asDouble();
        double lon = r.get("longitude").asDouble();

        String name = r.get("name").asText();
        String country = r.hasNonNull("country") ? r.get("country").asText() : "";
        String admin1 = r.hasNonNull("admin1") ? r.get("admin1").asText() : "";

        String display = name;
        if (!admin1.isEmpty()) display += ", " + admin1;
        if (!country.isEmpty()) display += ", " + country;

        return new CityGeo(lat, lon, display);
    }

    private WeatherData fetchWeather(double lat, double lon) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&current=temperature_2m,wind_speed_10m,weather_code"
                + "&wind_speed_unit=ms"
                + "&timezone=auto";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) throw new RuntimeException("HTTP " + response.statusCode());

        JsonNode root = mapper.readTree(response.body());
        JsonNode cur = root.get("current");

        double temp = cur.get("temperature_2m").asDouble();
        double wind = cur.get("wind_speed_10m").asDouble();
        int code = cur.get("weather_code").asInt();

        String state = decodeWeatherCode(code);

        return new WeatherData(temp, wind, state);
    }

    // Простой перевод кодов Open-Meteo в текст
    private String decodeWeatherCode(int code) {
        switch (code) {
            case 0: return "Ясно";
            case 1: case 2: case 3: return "Переменная облачность";
            case 45: case 48: return "Туман";
            case 51: case 53: case 55: return "Морось";
            case 61: case 63: case 65: return "Дождь";
            case 71: case 73: case 75: return "Снег";
            case 80: case 81: case 82: return "Ливень";
            case 95: return "Гроза";
            default: return "Код погоды: " + code;
        }
    }

    private boolean isAuthed(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s != null && s.getAttribute("role") != null;
    }

    private static class CityGeo {
        final double lat;
        final double lon;
        final String displayName;
        CityGeo(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }
    }

    private static class WeatherData {
        final double temperature;
        final double windSpeed;
        final String state;
        WeatherData(double temperature, double windSpeed, String state) {
            this.temperature = temperature;
            this.windSpeed = windSpeed;
            this.state = state;
        }
    }
}
