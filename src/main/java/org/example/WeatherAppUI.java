package org.example;

import org.example.WeatherService.*;

import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.TimeZone;

public class WeatherAppUI extends JFrame {

    // --- CAMPOS DE LA UI ---
    private final WeatherService weatherService;
    private JEditorPane hourlyResultArea;
    private JPanel dailyForecastPanel;
    private JTextField cityField;
    private JButton searchButton;
    private JXMapKit mapKit;

    private ForecastResponse currentForecast;

    // --- Colores ---
    private static final Color COLOR_PRIMARIO = new Color(0, 123, 255);
    private static final Color COLOR_SECUNDARIO = new Color(108, 117, 125);
    private static final Color COLOR_FONDO_PRINCIPAL = new Color(245, 245, 245);
    private static final Color COLOR_FONDO_PANEL = Color.WHITE;
    private static final Color COLOR_BORDE = new Color(220, 220, 220);

    public WeatherAppUI() {
        super("Weather App");
        this.weatherService = new WeatherService();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // --- 1. Configuración de la ventana: MAXIMIZADA ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Iniciar en pantalla completa
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO_PRINCIPAL);

        // --- 2. Crear el panel dividido 50/50 ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setEnabled(false);
        splitPane.setBorder(null);

        // --- 3. Asignar los paneles izquierdo y derecho ---
        splitPane.setLeftComponent(createLeftPanel());
        splitPane.setRightComponent(createRightPanel());

        add(splitPane, BorderLayout.CENTER);

        setVisible(true);
    }

    /**
     * Crea el panel de la IZQUIERDA (Mapa + Búsqueda)
     */
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(COLOR_FONDO_PANEL);

        // --- 1. El panel de búsqueda (arriba) ---
        JPanel searchWrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        searchWrapperPanel.setBackground(COLOR_FONDO_PANEL);
        searchWrapperPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDE));

        this.cityField = new JTextField(20);
        this.cityField.setFont(new Font("Arial", Font.PLAIN, 14));
        this.searchButton = new JButton("Buscar");
        styleButton(searchButton, COLOR_PRIMARIO, 14);

        JLabel cityLabel = new JLabel("Introduce un lugar:");
        cityLabel.setFont(new Font("Arial", Font.BOLD, 14));

        searchWrapperPanel.add(cityLabel);
        searchWrapperPanel.add(cityField);
        searchWrapperPanel.add(searchButton);

        // Acciones
        searchButton.addActionListener(e -> onSearchByCity());
        cityField.addActionListener(e -> onSearchByCity());

        leftPanel.add(searchWrapperPanel, BorderLayout.NORTH);

        // --- 2. El mapa (en el centro) ---
        mapKit = new JXMapKit();

        // 2a. Configuración del TileFactory (HTTPS + UserAgent)
        TileFactoryInfo info = new OSMTileFactoryInfo("OpenStreetMap", "https://tile.openstreetmap.org");
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        tileFactory.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
        mapKit.getMainMap().setTileFactory(tileFactory);

        // 2b. Posición inicial (España) y controles
        GeoPosition initialPosition = new GeoPosition(40.4167, -3.7032);
        mapKit.setAddressLocation(initialPosition);

        // --- MODIFICADO: Zoom más cercano (como en tu imagen) ---
        mapKit.setZoom(12);

        mapKit.setMiniMapVisible(false);

        // --- NUEVO: Hacer los botones de zoom más grandes ---
        JButton zoomInButton = mapKit.getZoomInButton();
        JButton zoomOutButton = mapKit.getZoomOutButton();
        Font buttonFont = new Font("Arial", Font.BOLD, 18); // Fuente más grande
        Insets buttonMargin = new Insets(2, 8, 2, 8); // (arriba/abajo, izq/der)

        zoomInButton.setFont(buttonFont);
        zoomInButton.setMargin(buttonMargin);
        zoomOutButton.setFont(buttonFont);
        zoomOutButton.setMargin(buttonMargin);
        // --- FIN DE LA MODIFICACIÓN ---

        // 2c. Listener de clic en el mapa
        mapKit.getMainMap().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point clickPoint = e.getPoint();
                GeoPosition geo = mapKit.getMainMap().convertPointToGeoPosition(clickPoint);
                onSearchByCoords(geo);
            }
        });

        leftPanel.add(mapKit, BorderLayout.CENTER);

        return leftPanel;
    }

    /**
     * Crea el panel de la DERECHA (Resultados del tiempo)
     */
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(COLOR_FONDO_PRINCIPAL);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding

        // --- 1. Panel de 5 días (Arriba) ---
        this.dailyForecastPanel = new JPanel(new GridLayout(1, 5, 10, 10));
        this.dailyForecastPanel.setBackground(COLOR_FONDO_PRINCIPAL);
        rightPanel.add(dailyForecastPanel, BorderLayout.NORTH);

        // --- 2. Panel de horas (Resto del espacio) ---
        this.hourlyResultArea = new JEditorPane();
        this.hourlyResultArea.setEditable(false);
        this.hourlyResultArea.setContentType("text/html");
        this.hourlyResultArea.setBackground(COLOR_FONDO_PANEL);

        JScrollPane scrollPane = new JScrollPane(hourlyResultArea);
        Border etchedBorder = BorderFactory.createEtchedBorder(COLOR_BORDE, Color.lightGray);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                etchedBorder,
                "Pronóstico por Horas",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14),
                new Color(50, 50, 50)
        ));

        rightPanel.add(scrollPane, BorderLayout.CENTER);

        return rightPanel;
    }


    /**
     * Se llama al pulsar "Buscar"
     */
    private void onSearchByCity() {
        String city = cityField.getText();
        if (city == null || city.trim().isEmpty()) {
            hourlyResultArea.setText("Por favor, introduce una ciudad.");
            return;
        }
        clearUIForSearch("Buscando pronóstico para " + city + "...");

        new SwingWorker<ForecastResponse, Void>() {
            @Override
            protected ForecastResponse doInBackground() throws WeatherException {
                return weatherService.getWeather(city);
            }
            @Override
            protected void done() {
                handleForecastResponse(this);
            }
        }.execute();
    }

    /**
     * Se llama al hacer clic en el mapa
     */
    private void onSearchByCoords(GeoPosition geo) {
        String status = String.format("Buscando en Lat: %.4f, Lon: %.4f...", geo.getLatitude(), geo.getLongitude());
        clearUIForSearch(status);

        new SwingWorker<ForecastResponse, Void>() {
            @Override
            protected ForecastResponse doInBackground() throws WeatherException {
                return weatherService.getWeatherByCoords(geo.getLatitude(), geo.getLongitude());
            }
            @Override
            protected void done() {
                handleForecastResponse(this);
            }
        }.execute();
    }

    /**
     * Helper para limpiar la UI antes de una búsqueda
     */
    private void clearUIForSearch(String message) {
        if (hourlyResultArea != null) {
            hourlyResultArea.setText("<html><body style='padding: 10px;'>" + message + "</body></html>");
        }
        if (dailyForecastPanel != null) {
            dailyForecastPanel.removeAll();
            dailyForecastPanel.revalidate();
            dailyForecastPanel.repaint();
        }
        currentForecast = null;
    }

    /**
     * Helper refactorizado que maneja la respuesta del SwingWorker
     */
    private void handleForecastResponse(SwingWorker<ForecastResponse, Void> worker) {
        try {
            currentForecast = worker.get();
            String cityName = (currentForecast.city != null) ? currentForecast.city.name : "Ubicación seleccionada";
            updateHourlyPanelTitle("Pronóstico por Horas para: " + cityName);

            updateUIWithForecast();
        } catch (Exception ex) {
            String errorMsg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            hourlyResultArea.setText("<html><body><b style='color: red; padding: 10px;'>Error:</b> " + errorMsg + "</body></html>");
        }
    }

    /**
     * Helper para cambiar el título del JScrollPane
     */
    private void updateHourlyPanelTitle(String newTitle) {
        if (hourlyResultArea.getParent().getParent() instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) hourlyResultArea.getParent().getParent();
            if (scrollPane.getBorder() instanceof TitledBorder) {
                TitledBorder border = (TitledBorder) scrollPane.getBorder();
                border.setTitle(newTitle);
                scrollPane.repaint();
            }
        }
    }


    /**
     * Actualiza la UI con los 5 paneles de días
     */
    private void updateUIWithForecast() {
        if (currentForecast == null) return;

        dailyForecastPanel.removeAll();
        Map<String, List<ForecastItem>> forecastsByDay = new LinkedHashMap<>();
        for (ForecastItem item : currentForecast.list) {
            String dayString = formatDate(item.dt, "yyyy-MM-dd");
            forecastsByDay.computeIfAbsent(dayString, k -> new ArrayList<>()).add(item);
        }

        int dayCount = 0;
        for (String day : forecastsByDay.keySet()) {
            if (dayCount >= 5) break;

            List<ForecastItem> dayItems = forecastsByDay.get(day);

            double minTemp = 1000;
            double maxTemp = -1000;
            for (ForecastItem item : dayItems) {
                if (item.main.temp < minTemp) minTemp = item.main.temp;
                if (item.main.temp > maxTemp) maxTemp = item.main.temp;
            }

            String iconCode = dayItems.get(dayItems.size() / 2).weather.get(0).icon;
            String dateLabel = formatDate(dayItems.get(0).dt, "EEE, d MMM");

            JPanel panel = createDayPanel(dateLabel, iconCode, minTemp, maxTemp, dayItems);
            dailyForecastPanel.add(panel);
            dayCount++;
        }

        if (!forecastsByDay.isEmpty()) {
            displayDayForecast(forecastsByDay.values().iterator().next());
        }

        dailyForecastPanel.revalidate();
        dailyForecastPanel.repaint();
    }


    /**
     * Helper para dar estilo a los botones
     */
    private void styleButton(JButton button, Color background, int fontSize) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, fontSize));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(background.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(background);
            }
        });
    }

    /**
     * Crea cada uno de los 5 paneles de día
     */
    private JPanel createDayPanel(String date, String iconCode, double minTemp, double maxTemp, List<ForecastItem> dayItems) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDE, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        panel.setBackground(COLOR_FONDO_PANEL);

        JLabel dateLabel = new JLabel(date, SwingConstants.CENTER);
        dateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dateLabel.setFont(new Font("Arial", Font.BOLD, 16));
        dateLabel.setForeground(new Color(30, 30, 30));

        String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";
        JLabel iconLabel = new JLabel("<html><img src='" + iconUrl + "' width='50' height='50'></html>", SwingConstants.CENTER);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String tempString = String.format("<html><center><span style='font-size: 1.1em; color: #d9534f;'>%.0f°C</span><br><span style='color: #5bc0de;'>%.0f°C</span></center></html>", maxTemp, minTemp);
        JLabel tempLabel = new JLabel(tempString, SwingConstants.CENTER);
        tempLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tempLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JButton detailsButton = new JButton("POR HORAS");
        detailsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsButton.addActionListener(e -> displayDayForecast(dayItems));

        styleButton(detailsButton, COLOR_SECUNDARIO, 12);

        panel.add(dateLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(iconLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(tempLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(detailsButton);

        return panel;
    }

    /**
     * Muestra el pronóstico detallado por horas
     */
    private void displayDayForecast(List<ForecastItem> items) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
                "<html><body style='font-family: Arial, sans-serif; font-size: 11pt; padding: 10px; background-color: %s;'>",
                "#ffffff"
        ));

        sb.append(String.format(
                "<h3 style='margin: 0 0 10px 0; color: #333;'>Día: %s</h3>" +
                        "<hr style='border: 0; border-top: 1px solid #ccc;'>",
                formatDate(items.get(0).dt, "EEEE, d 'de' MMMM")
        ));

        for (ForecastItem item : items) {
            sb.append(formatForecastItem(item));
        }

        sb.append("</body></html>");
        hourlyResultArea.setText(sb.toString());
        hourlyResultArea.setCaretPosition(0);
    }

    /**
     * Formatea un solo item de 3 horas
     */
    private String formatForecastItem(ForecastItem item) {
        String iconCode = item.weather.get(0).icon;
        String iconUrl = "http://openweathermap.org/img/wn/" + iconCode + "@2x.png";

        return String.format(
                "<div style='display: flex; align-items: center; border-bottom: 1px solid #eee; padding: 8px 0;'>" +
                        "  <div style='flex-shrink: 0; width: 60px; text-align: center;'>" +
                        "    <img src='%s' width='50' height='50'>" +
                        "  </div>" +
                        "  <div style='flex-grow: 1; padding-left: 10px;'>" +
                        "    <strong style='font-size: 1.1em; color: #0056b3;'>[%s] - %s (%.0f°C)</strong><br>" +
                        "    <span style='color: #555;'>Sensación: %.0f°C | Humedad: %d%% | Viento: %.1f km/h</span>" +
                        "  </div>" +
                        "</div>",

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
     * Helper de fecha
     */
    private String formatDate(long timestamp, String format) {
        Date date = new Date(timestamp * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }
}