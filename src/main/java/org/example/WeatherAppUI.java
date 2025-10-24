package org.example;

import org.example.WeatherService.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.TimeZone;

public class WeatherAppUI extends JFrame {
    private final JTextField cityField;
    private final JButton searchButton;
    private final WeatherService weatherService;

    // CAMBIO 1: Paneles de UI reorganizados
    private final JEditorPane hourlyResultArea; // El panel de HTML, ahora en el SUR
    private final JPanel dailyForecastPanel; // Nuevo panel en el CENTRO para los 5 días

    private ForecastResponse currentForecast;

    public WeatherAppUI() {
        super("Weather App");
        this.weatherService = new WeatherService();

        // --- Configuración de la ventana ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // CAMBIO 2: Ventana más grande para la nueva disposición
        setSize(700, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // --- NORTE: Panel de Búsqueda (Sin cambios) ---
        JPanel searchPanel = new JPanel(new FlowLayout());
        this.cityField = new JTextField(20);
        this.searchButton = new JButton("Buscar");
        searchPanel.add(new JLabel("Ciudad:"));
        searchPanel.add(cityField);
        searchPanel.add(searchButton);
        add(searchPanel, BorderLayout.NORTH);

        // --- CENTRO: Panel para los 5 días ---
        // CAMBIO 3: Nuevo panel con GridLayout (1 fila, 5 columnas)
        this.dailyForecastPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        this.dailyForecastPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(dailyForecastPanel, BorderLayout.CENTER);

        // --- SUR: Área de Resultados por Hora ---
        // CAMBIO 4: El JEditorPane ahora va al SUR
        this.hourlyResultArea = new JEditorPane();
        this.hourlyResultArea.setEditable(false);
        this.hourlyResultArea.setContentType("text/html");

        JScrollPane scrollPane = new JScrollPane(hourlyResultArea);
        // Le añadimos un título como en tu imagen
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Horas y Temperaturas",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12)
        ));
        // Damos un tamaño preferido al panel de horas
        scrollPane.setPreferredSize(new Dimension(getWidth(), 300));
        add(scrollPane, BorderLayout.SOUTH);

        // --- Acción del Botón de Búsqueda ---
        searchButton.addActionListener(e -> onSearch());

        setVisible(true);
    }

    /**
     * Se llama al pulsar "Buscar". Inicia la tarea de fondo.
     */
    private void onSearch() {
        String city = cityField.getText();
        if (city == null || city.trim().isEmpty()) {
            hourlyResultArea.setText("Por favor, introduce una ciudad.");
            return;
        }

        hourlyResultArea.setText("<html><body>Buscando pronóstico para " + city + "...</body></html>");
        // CAMBIO 5: Limpiar el panel de días
        dailyForecastPanel.removeAll();
        dailyForecastPanel.revalidate();
        dailyForecastPanel.repaint();
        currentForecast = null;

        new SwingWorker<ForecastResponse, Void>() {
            @Override
            protected ForecastResponse doInBackground() throws WeatherException {
                return weatherService.getWeather(city);
            }

            @Override
            protected void done() {
                try {
                    currentForecast = get();
                    updateUIWithForecast();
                } catch (Exception ex) {
                    String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    hourlyResultArea.setText("<html><body><b>Error:</b> " + errorMsg + "</body></html>");
                }
            }
        }.execute();
    }

    /**
     * CAMBIO 6: Ahora crea los 5 paneles de días en lugar de botones.
     */
    private void updateUIWithForecast() {
        if (currentForecast == null) return;

        dailyForecastPanel.removeAll();
        Map<String, List<ForecastItem>> forecastsByDay = new LinkedHashMap<>();
        for (ForecastItem item : currentForecast.list) {
            String dayString = formatDate(item.dt, "yyyy-MM-dd"); // Agrupar por fecha
            forecastsByDay.computeIfAbsent(dayString, k -> new ArrayList<>()).add(item);
        }

        // Crear un panel para cada día
        for (String day : forecastsByDay.keySet()) {
            List<ForecastItem> dayItems = forecastsByDay.get(day);

            // 1. Calcular Min/Max para ese día
            double minTemp = 1000;
            double maxTemp = -1000;
            for (ForecastItem item : dayItems) {
                if (item.main.temp < minTemp) minTemp = item.main.temp;
                if (item.main.temp > maxTemp) maxTemp = item.main.temp;
            }

            // 2. Coger el icono de mediodía (un item intermedio)
            String iconCode = dayItems.get(dayItems.size() / 2).weather.get(0).icon;

            // 3. Formatear la fecha como en la imagen
            String dateLabel = formatDate(dayItems.get(0).dt, "yyyy-MM-dd");

            // 4. Crear el panel y añadirlo
            JPanel panel = createDayPanel(dateLabel, iconCode, minTemp, maxTemp, dayItems);
            dailyForecastPanel.add(panel);
        }

        // Mostrar el pronóstico del primer día por defecto en el panel de horas
        if (!forecastsByDay.isEmpty()) {
            displayDayForecast(forecastsByDay.values().iterator().next());
        }

        dailyForecastPanel.revalidate();
        dailyForecastPanel.repaint();
    }

    /**
     * CAMBIO 7: Nuevo método para crear cada uno de los 5 paneles de día.
     */
    private JPanel createDayPanel(String date, String iconCode, double minTemp, double maxTemp, List<ForecastItem> dayItems) {
        JPanel panel = new JPanel();
        // BoxLayout apila los componentes verticalmente
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        // Borde y color de fondo para imitar el estilo
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(new Color(240, 248, 255)); // Azul "Alice"

        // 1. Fecha (alineada al centro)
        JLabel dateLabel = new JLabel(date, SwingConstants.CENTER);
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dateLabel.setFont(new Font("Arial", Font.BOLD, 14));

        // 2. Icono (usando HTML en JLabel para carga asíncrona)
        String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        JLabel iconLabel = new JLabel("<html><img src='" + iconUrl + "' width='50' height='50'></html>", SwingConstants.CENTER);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Temperaturas (Max y Min)
        String tempString = String.format("<html><center><b>%.0f°C</b><br>%.0f°C</center></html>", maxTemp, minTemp);
        JLabel tempLabel = new JLabel(tempString, SwingConstants.CENTER);
        tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tempLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        // 4. Botón "POR HORAS"
        JButton detailsButton = new JButton("POR HORAS");
        detailsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        // La acción es la misma: llamar a displayDayForecast
        detailsButton.addActionListener(e -> displayDayForecast(dayItems));

        // Añadir componentes al panel con espacios
        panel.add(dateLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5))); // Espaciador
        panel.add(iconLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(tempLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(detailsButton);

        return panel;
    }

    /**
     * Muestra el pronóstico detallado por horas en el JEditorPane del SUR
     */
    private void displayDayForecast(List<ForecastItem> items) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body style='font-family: Arial, sans-serif; font-size: 10pt; padding: 5px;'>");
        sb.append(String.format(
                "<b>Día: %s</b>" +
                        "<hr style='border: 0; border-top: 1px solid #ccc;'>",
                formatDate(items.get(0).dt, "EEEE, d 'de' MMMM")
        ));

        // Este bucle es idéntico al de la versión anterior
        for (ForecastItem item : items) {
            sb.append(formatForecastItem(item));
        }

        sb.append("</body></html>");
        hourlyResultArea.setText(sb.toString());
        hourlyResultArea.setCaretPosition(0);
    }

    /**
     * Formatea un solo item de 3 horas (idéntico a la versión anterior)
     */
    private String formatForecastItem(ForecastItem item) {
        String iconCode = item.weather.get(0).icon;
        String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";

        return String.format(
                "<table width='100%%' style='border-bottom: 1px solid #eee; margin-bottom: 5px;'>" +
                        "  <tr>" +
                        "    <td width='50' valign='middle'>" +
                        "      <img src='%s' width='50' height='50'>" +
                        "    </td>" +
                        "    <td valign='top' style='padding-left: 10px;'>" +
                        "      <b>[%s] - %s (%.0f°C)</b><br>" +
                        "      Sensación: %.0f°C<br>" +
                        "      Humedad: %d%%. Viento: %.1f km/h" +
                        "    </td>" +
                        "  </tr>" +
                        "</table>",

                iconUrl,
                formatDate(item.dt, "HH:mm"),
                item.weather.get(0).description,
                item.main.temp,
                item.main.feels_like,
                item.main.humidity,
                item.wind.speed * 3.6
        );
    }

    /**
     * Helper de fecha (idéntico a la versión anterior)
     */
    private String formatDate(long timestamp, String format) {
        Date date = new Date(timestamp * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }
}