import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class WeatherAppConsole {

    private static final String API_KEY = "TU_CLAVE_AQUI"; // RECUERDA CAMBIAR ESTO
    private static final String API_URL_TEMPLATE = "http://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Introduce el nombre de una ciudad: ");
        String city = scanner.nextLine();
        scanner.close();

        if (city == null || city.trim().isEmpty()) {
            System.out.println("Por favor, introduce una ciudad válida.");
            return;
        }

        try {
            // Construir la URL de la API
            String apiUrl = String.format(API_URL_TEMPLATE, city, API_KEY);
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Leer la respuesta de la API
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            connection.disconnect();

            // Analizar la respuesta JSON y mostrar los datos
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content.toString(), JsonObject.class);
            JsonObject main = jsonObject.getAsJsonObject("main");
            JsonObject weather = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject();

            double temp = main.get("temp").getAsDouble();
            double feelsLike = main.get("feels_like").getAsDouble();
            String description = weather.get("description").getAsString();

            System.out.printf("El tiempo en %s es:\n", city);
            System.out.printf("Temperatura: %.1f °C\n", temp);
            System.out.printf("Sensación térmica: %.1f °C\n", feelsLike);
            System.out.printf("Descripción: %s\n", description);

        } catch (Exception e) {
            System.err.println("Error al obtener la información del tiempo. Asegúrate de que el nombre de la ciudad sea correcto y que tu clave de API sea válida.");
            e.printStackTrace();
        }
    }
}
