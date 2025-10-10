package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WeatherAppUI extends JFrame {
    private final JTextField cityField;
    private final JTextArea resultArea;
    private final JButton searchButton;
    private final WeatherService weatherService;

    public WeatherAppUI() {
        super("Weather App");
        this.weatherService = new WeatherService();
        this.cityField = new JTextField(20);
        this.resultArea = new JTextArea(5, 20);
        this.searchButton = new JButton("Buscar");

        // Configuración de la ventana
        setLayout(new FlowLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null); // Centra la ventana

        // Componentes
        add(new JLabel("Ciudad:"));
        add(cityField);
        add(searchButton);
        add(new JScrollPane(resultArea)); // Permite desplazamiento en el área de texto

        // Evento del botón
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String city = cityField.getText();
                if (city != null && !city.trim().isEmpty()) {
                    String weatherInfo = weatherService.getWeather(city);
                    resultArea.setText(weatherInfo);
                } else {
                    resultArea.setText("Por favor, introduce una ciudad.");
                }
            }
        });

        setVisible(true);
    }
}
