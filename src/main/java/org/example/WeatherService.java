package org.example;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class WeatherService {
    private static final String API_KEY = "84984066d6a7fe9f5c500ba06a758eae";
    private static final String API_URL = "http://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=es";

    /**
     * CAMBIO 1: Ahora devuelve un objeto ForecastResponse y lanza una excepción en caso de error.
     */
    public ForecastResponse getWeather(String city) throws WeatherException {
        try {
            String urlString = String.format(API_URL, city.replace(" ", "+"), API_KEY);
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                if (responseCode == 404) {
                    throw new WeatherException("Error: Ciudad no encontrada.");
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

            // CAMBIO 2: Parsea el JSON y lo devuelve directamente
            Gson gson = new Gson();
            ForecastResponse response = gson.fromJson(content.toString(), ForecastResponse.class);

            if (response == null || response.list == null || response.list.isEmpty()) {
                throw new WeatherException("No se pudieron obtener datos del pronóstico.");
            }
            return response;

        } catch (Exception e) {
            // Envuelve la excepción original en nuestra excepción personalizada
            throw new WeatherException("Error al conectar o parsear: " + e.getMessage());
        }
    }

    // CAMBIO 3: Ya no necesitamos los métodos 'parseWeather' ni 'formatDate' aquí.
    // La UI se encargará de formatear.

    // CAMBIO 4: Las clases internas ahora son 'public static' para ser accesibles desde la UI.
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

    public static class City {
        public String name;
        public String country;
    }
}