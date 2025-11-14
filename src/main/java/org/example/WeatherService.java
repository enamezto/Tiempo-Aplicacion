package org.example;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class WeatherService {
    private static final String API_KEY = "84984066d6a7fe9f5c500ba06a758eae";

    // URL para buscar por nombre de ciudad
    private static final String API_URL_CITY = "http://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=es";

    // NUEVA URL para buscar por coordenadas
    private static final String API_URL_COORDS = "http://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=metric&lang=es";


    /**
     * Obtiene el tiempo por nombre de ciudad
     */
    public ForecastResponse getWeather(String city) throws WeatherException {
        String urlString = String.format(API_URL_CITY, city.replace(" ", "+"), API_KEY);
        return getForecastFromApi(urlString);
    }

    /**
     * NUEVO: Obtiene el tiempo por coordenadas geográficas
     */
    public ForecastResponse getWeatherByCoords(double lat, double lon) throws WeatherException {
        String urlString = String.format(API_URL_COORDS, lat, lon, API_KEY);
        return getForecastFromApi(urlString);
    }

    /**
     * NUEVO: Método privado refactorizado para manejar la lógica de la API
     */
    private ForecastResponse getForecastFromApi(String urlString) throws WeatherException {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 404) {
                    throw new WeatherException("Error: Datos no encontrados para la ubicación.");
                }
                throw new WeatherException("Error: (" + responseCode + ") al conectar con la API.");
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            Gson gson = new Gson();
            ForecastResponse response = gson.fromJson(content.toString(), ForecastResponse.class);

            if (response == null || response.list == null || response.list.isEmpty()) {
                throw new WeatherException("No se pudieron obtener datos del pronóstico.");
            }
            return response;

        } catch (Exception e) {
            throw new WeatherException("Error al conectar o parsear: " + e.getMessage());
        }
    }


    // --- Clases internas (sin cambios) ---
    public static class ForecastResponse {
        public List<ForecastItem> list;
        public City city;
    }

    public static class ForecastItem {
        public long dt;
        public Main main;
        public List<Weather> weather;
        public Wind wind;
        public double pop;
    }

    public static class Main {
        public double temp;
        public double feels_like;
        public double temp_min;
        public double temp_max;
        public int pressure;
        public int humidity;
    }

    public static class Weather {
        public String main;
        public String description;
        public String icon;
    }

    public static class Wind {
        public double speed;
        public int deg;
    }

    // NUEVO: Clase para coordenadas
    public static class Coord {
        public double lat;
        public double lon;
    }

    public static class City {
        public String name;
        public String country;
        public Coord coord; // NUEVO: Coordenadas
    }
}