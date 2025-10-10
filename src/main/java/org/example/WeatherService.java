package org.example;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherService {
    private static final String API_KEY = "84984066d6a7fe9f5c500ba06a758eae";
    private static final String API_URL = "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    public String getWeather(String city) {
        try {
            String urlString = String.format(API_URL, city, API_KEY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return "Error: Ciudad no encontrada o clave de API incorrecta.";
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            return parseWeather(content.toString());
        } catch (Exception e) {
            return "Error al conectar con la API.";
        }
    }

    private String parseWeather(String json) {
        // La librería Gson mapea el JSON a un objeto Java
        Gson gson = new Gson();
        WeatherResponse response = gson.fromJson(json, WeatherResponse.class);

        if (response != null && response.main != null) {
            return String.format(
                    "Temperatura: %.1f °C\nSensación térmica: %.1f °C\nCondición: %s",
                    response.main.temp,
                    response.main.feels_like,
                    response.weather[0].description
            );
        }
        return "No se pudo obtener la información del tiempo.";
    }

    // Clases internas para mapear la estructura JSON
    private static class WeatherResponse {
        Main main;
        Weather[] weather;
    }

    private static class Main {
        double temp;
        double feels_like;
    }

    private static class Weather {
        String description;
    }
}
