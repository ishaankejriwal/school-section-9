/*
 * Ishaan Kejriwal - AP CSA
 * File: AnimalGUI.java
 * Description: Swing GUI for tracking animal location observations.
 * Date: 2026-04-22
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Swing desktop interface for browsing and maintaining observation records.
public class AnimalGUI extends JFrame {
    private final AnimalContainer container;
    private final MySQLAnimalRepository mysqlRepository;

    // Root panels tracked for runtime theme switching.
    private JPanel mainPanel;
    private JPanel topPanel;
    private JPanel formPanel;

    private JTextField idField;
    private JTextField animalTypeField;
    private JTextField latitudeField;
    private JTextField longitudeField;
    private JTextField observedAtField;
    private JTextField durationField;
    private JTextField uuidField;
    private JCheckBox revisitedCheckBox;
    private JLabel statusLabel;
    private JLabel recordCounterLabel;
    private JLabel titleLabel;
    private JLabel hintLabel;

    private JButton connectButton;
    private JButton insertButton;
    private JButton themeToggleButton;

    private boolean isConnected;
    private boolean isInsertMode;
    private boolean darkMode;
    private int currentIndex;
    private List<Animal> connectedAnimals;

    // Component collections used by theme updates.
    private final List<JTextField> themedTextFields = new ArrayList<>();
    private final List<JLabel> fieldLabels = new ArrayList<>();
    private final List<JButton> themedButtons = new ArrayList<>();

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Color BG_PRIMARY = new Color(15, 20, 28);
    private static final Color BG_SURFACE = new Color(24, 31, 42);
    private static final Color BG_SURFACE_ALT = new Color(31, 40, 55);
    private static final Color BG_INPUT = new Color(36, 46, 64);
    private static final Color TEXT_PRIMARY = new Color(233, 238, 246);
    private static final Color TEXT_MUTED = new Color(160, 173, 194);
    private static final Color ACCENT = new Color(74, 171, 247);
    private static final Color SUCCESS = new Color(81, 199, 136);
    private static final Color WARNING = new Color(240, 166, 77);
    private static final Color BORDER_SOFT = new Color(56, 70, 92);

    private static final Color LIGHT_BG_PRIMARY = new Color(236, 241, 249);
    private static final Color LIGHT_BG_SURFACE = new Color(250, 252, 255);
    private static final Color LIGHT_BG_SURFACE_ALT = new Color(230, 236, 247);
    private static final Color LIGHT_BG_INPUT = new Color(255, 255, 255);
    private static final Color LIGHT_TEXT_PRIMARY = new Color(36, 48, 67);
    private static final Color LIGHT_TEXT_MUTED = new Color(91, 106, 133);
    private static final Color LIGHT_BORDER_SOFT = new Color(172, 186, 213);

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 12);

    public AnimalGUI(AnimalContainer container) {
        this.container = container;
        this.mysqlRepository = MySQLAnimalRepository.fromEnvironment();
        // Start in dark mode by default.
        this.darkMode = true;
        this.isConnected = false;
        this.isInsertMode = false;
        this.currentIndex = -1;
        this.connectedAnimals = new ArrayList<>();
        initializeGUI();
    }

    // Builds the full window layout and wires all UI actions.
    private void initializeGUI() {
        applyBaseLookAndFeel();

        setTitle("Animal Observation Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);
        setResizable(true);

        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BG_PRIMARY);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(true);
        topPanel.setBackground(BG_SURFACE);
        topPanel.setBorder(new CompoundBorder(
            new LineBorder(BORDER_SOFT, 1, true),
            new EmptyBorder(12, 14, 12, 14)
        ));

        // Header row keeps title and theme control visible at all times.
        JPanel headerRow = new JPanel(new BorderLayout(10, 0));
        headerRow.setOpaque(false);

        titleLabel = new JLabel("Animal Observation Tracker");
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setToolTipText(container.getContainerInfo());

        themeToggleButton = createStyledButton("Light Mode", new Color(89, 117, 166), new Color(105, 136, 191), new Color(75, 103, 151));
        themeToggleButton.setPreferredSize(new Dimension(128, 30));
        themeToggleButton.addActionListener(e -> onThemeToggleClicked());

        headerRow.add(titleLabel, BorderLayout.WEST);
        headerRow.add(themeToggleButton, BorderLayout.EAST);

        statusLabel = new JLabel("Status: Disconnected - click Connect to load observations");
        statusLabel.setFont(BODY_FONT);
        statusLabel.setForeground(TEXT_MUTED);

        recordCounterLabel = new JLabel("Record: 0/0");
        recordCounterLabel.setFont(LABEL_FONT);
        recordCounterLabel.setForeground(ACCENT);

        topPanel.add(headerRow);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(statusLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(recordCounterLabel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(true);
        formPanel.setBackground(BG_SURFACE);
        formPanel.setBorder(createSectionBorder("Observation Record"));

        idField = createTextField(false);
        animalTypeField = createTextField(true);
        latitudeField = createTextField(true);
        longitudeField = createTextField(true);
        observedAtField = createTextField(true);
        durationField = createTextField(true);
        uuidField = createTextField(true);

        revisitedCheckBox = new JCheckBox("Revisited Spot");
        revisitedCheckBox.setOpaque(false);
        revisitedCheckBox.setForeground(TEXT_PRIMARY);
        revisitedCheckBox.setFont(BODY_FONT);

        formPanel.add(createRecordEntryRow("ID:", idField));
        formPanel.add(createRecordEntryRow("Animal Type:", animalTypeField));
        formPanel.add(createRecordEntryRow("Latitude:", latitudeField));
        formPanel.add(createRecordEntryRow("Longitude:", longitudeField));
        formPanel.add(createRecordEntryRow("Observed At:", observedAtField));
        formPanel.add(createRecordEntryRow("Duration (min):", durationField));
        formPanel.add(createRecordEntryRow("Observation UUID:", uuidField));

        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkBoxPanel.setOpaque(false);
        checkBoxPanel.add(revisitedCheckBox);
        formPanel.add(checkBoxPanel);

        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hintPanel.setOpaque(false);
        hintLabel = new JLabel("Timestamp format: yyyy-MM-dd HH:mm:ss");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(TEXT_MUTED);
        hintPanel.add(hintLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(hintPanel);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new GridLayout(4, 1, 0, 8));
        controlsPanel.setOpaque(false);

        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        navigationPanel.setOpaque(false);
        JButton startButton = createStyledButton("Start");
        JButton backButton = createStyledButton("Back");
        JButton nextButton = createStyledButton("Next");
        JButton endButton = createStyledButton("End");

        startButton.addActionListener(e -> showRecordAtIndex(0, "Moved to first record"));
        backButton.addActionListener(e -> showRecordAtIndex(currentIndex - 1, "Moved back one record"));
        nextButton.addActionListener(e -> showRecordAtIndex(currentIndex + 1, "Moved forward one record"));
        endButton.addActionListener(e -> {
            if (!connectedAnimals.isEmpty()) {
                showRecordAtIndex(connectedAnimals.size() - 1, "Moved to last record");
            }
        });

        navigationPanel.add(startButton);
        navigationPanel.add(backButton);
        navigationPanel.add(nextButton);
        navigationPanel.add(endButton);

        JPanel crudPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        crudPanel.setOpaque(false);
        insertButton = createStyledButton("Insert", new Color(43, 120, 194), new Color(58, 141, 219), new Color(36, 104, 168));
        JButton updateButton = createStyledButton("Update", new Color(31, 152, 112), new Color(42, 176, 130), new Color(26, 126, 93));
        JButton deleteButton = createStyledButton("Delete", new Color(187, 79, 79), new Color(208, 98, 98), new Color(161, 65, 65));

        insertButton.setPreferredSize(new Dimension(120, 32));
        updateButton.setPreferredSize(new Dimension(120, 32));
        deleteButton.setPreferredSize(new Dimension(120, 32));

        insertButton.addActionListener(e -> onInsertClicked());
        updateButton.addActionListener(e -> onUpdateClicked());
        deleteButton.addActionListener(e -> onDeleteClicked());

        crudPanel.add(insertButton);
        crudPanel.add(updateButton);
        crudPanel.add(deleteButton);

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        connectionPanel.setOpaque(false);
        connectButton = createStyledButton("Connect", new Color(126, 92, 212), new Color(144, 108, 231), new Color(106, 75, 186));
        connectButton.setPreferredSize(new Dimension(220, 34));
        connectButton.addActionListener(e -> onConnectionToggleClicked());
        connectionPanel.add(connectButton);

        JPanel mappingPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        mappingPanel.setOpaque(false);
        JButton mappingButton = createStyledButton("Mapping", new Color(47, 134, 130), new Color(59, 156, 151), new Color(40, 113, 110));
        mappingButton.setPreferredSize(new Dimension(220, 34));
        mappingButton.addActionListener(e -> onMappingClicked());
        mappingPanel.add(mappingButton);

        controlsPanel.add(navigationPanel);
        controlsPanel.add(crudPanel);
        controlsPanel.add(connectionPanel);
        controlsPanel.add(mappingPanel);

        mainPanel.add(controlsPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        setFieldsEditable(false);
        clearRecordDisplay();
        applyTheme();
    }

    // Creates a text field with project-wide spacing and border treatment.
    private JTextField createTextField(boolean editable) {
        JTextField field = new JTextField();
        field.setEditable(editable);
        field.setFont(BODY_FONT);
        field.setPreferredSize(new Dimension(0, 30));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        field.setBackground(BG_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setSelectionColor(ACCENT.darker());
        field.setSelectedTextColor(TEXT_PRIMARY);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_SOFT, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        themedTextFields.add(field);
        return field;
    }

    // Creates a labeled row in the observation form.
    private JPanel createRecordEntryRow(String labelText, JTextField textField) {
        JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
        rowPanel.setOpaque(false);
        rowPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        rowPanel.setPreferredSize(new Dimension(0, 38));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(130, 24));
        label.setForeground(TEXT_MUTED);
        label.setFont(LABEL_FONT);
        fieldLabels.add(label);

        rowPanel.add(label, BorderLayout.WEST);
        rowPanel.add(textField, BorderLayout.CENTER);
        return rowPanel;
    }

    // Default button style variant used by neutral controls.
    private JButton createStyledButton(String text) {
        return createStyledButton(text, BG_SURFACE_ALT, new Color(44, 57, 77), new Color(33, 45, 62));
    }

    // Creates a themed button with hover and pressed states.
    private JButton createStyledButton(String text, Color base, Color hover, Color pressed) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setForeground(TEXT_PRIMARY);
        button.setOpaque(true);
        button.setMargin(new Insets(7, 14, 7, 14));
        button.putClientProperty("darkBaseColor", base);
        button.putClientProperty("darkHoverColor", hover);
        button.putClientProperty("darkPressedColor", pressed);
        applyButtonPalette(button, base, hover, pressed, TEXT_PRIMARY, BORDER_SOFT);
        themedButtons.add(button);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground((Color) button.getClientProperty("hoverColor"));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground((Color) button.getClientProperty("baseColor"));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground((Color) button.getClientProperty("pressedColor"));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.isEnabled()) {
                    Color next = button.getModel().isRollover()
                            ? (Color) button.getClientProperty("hoverColor")
                            : (Color) button.getClientProperty("baseColor");
                    button.setBackground(next);
                }
            }
        });

        return button;
    }

    // Stores button palette metadata so colors can be reapplied during theme changes.
    private void applyButtonPalette(JButton button, Color base, Color hover, Color pressed, Color foreground, Color borderColor) {
        button.putClientProperty("baseColor", base);
        button.putClientProperty("hoverColor", hover);
        button.putClientProperty("pressedColor", pressed);
        button.setForeground(foreground);
        button.setBackground(base);
        button.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    // Applies dark or light colors to existing components.
    private void applyTheme() {
        Color bgPrimary = darkMode ? BG_PRIMARY : LIGHT_BG_PRIMARY;
        Color bgSurface = darkMode ? BG_SURFACE : LIGHT_BG_SURFACE;
        Color bgInput = darkMode ? BG_INPUT : LIGHT_BG_INPUT;
        Color textPrimary = darkMode ? TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
        Color textMuted = darkMode ? TEXT_MUTED : LIGHT_TEXT_MUTED;
        Color borderSoft = darkMode ? BORDER_SOFT : LIGHT_BORDER_SOFT;

        mainPanel.setBackground(bgPrimary);
        topPanel.setBackground(bgSurface);
        topPanel.setBorder(new CompoundBorder(
                new LineBorder(borderSoft, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        formPanel.setBackground(bgSurface);
        formPanel.setBorder(createSectionBorder("Observation Record"));

        titleLabel.setForeground(textPrimary);
        hintLabel.setForeground(textMuted);

        for (JLabel label : fieldLabels) {
            label.setForeground(textMuted);
        }

        for (JTextField field : themedTextFields) {
            field.setBackground(bgInput);
            field.setForeground(textPrimary);
            field.setCaretColor(textPrimary);
            field.setSelectedTextColor(textPrimary);
            field.setSelectionColor(darkMode ? ACCENT.darker() : new Color(181, 210, 246));
            field.setBorder(new CompoundBorder(
                    new LineBorder(borderSoft, 1, true),
                    new EmptyBorder(6, 10, 6, 10)
            ));
        }

        revisitedCheckBox.setForeground(textPrimary);

        Color neutralBase = darkMode ? BG_SURFACE_ALT : LIGHT_BG_SURFACE_ALT;
        Color neutralHover = darkMode ? new Color(44, 57, 77) : new Color(216, 225, 241);
        Color neutralPressed = darkMode ? new Color(33, 45, 62) : new Color(203, 214, 234);
        Color buttonText = darkMode ? TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;

        for (JButton button : themedButtons) {
            Color existingBase = (Color) button.getClientProperty("baseColor");
            if (existingBase == null || existingBase.equals(BG_SURFACE_ALT) || existingBase.equals(LIGHT_BG_SURFACE_ALT)) {
                applyButtonPalette(button, neutralBase, neutralHover, neutralPressed, buttonText, borderSoft);
            } else {
                Color darkBase = (Color) button.getClientProperty("darkBaseColor");
                Color darkHover = (Color) button.getClientProperty("darkHoverColor");
                Color darkPressed = (Color) button.getClientProperty("darkPressedColor");
                applyButtonPalette(
                        button,
                        shiftForTheme(darkBase),
                        shiftForTheme(darkHover),
                        shiftForTheme(darkPressed),
                        buttonText,
                        borderSoft
                );
            }
        }

        themeToggleButton.setText(darkMode ? "Light Mode" : "Dark Mode");
        revalidate();
        repaint();
    }

    // Converts accent button shades to maintain contrast in light mode and restore in dark mode.
    private Color shiftForTheme(Color color) {
        if (darkMode) {
            return adjustBrightness(color, -16);
        }
        return adjustBrightness(color, 18);
    }

    // Handles clicks on the header theme switch button.
    private void onThemeToggleClicked() {
        darkMode = !darkMode;
        applyTheme();
        statusLabel.setText("Status: " + (darkMode ? "Dark mode enabled" : "Light mode enabled"));
        statusLabel.setForeground(ACCENT);
    }

    // Builds the titled border used by grouped panels.
    private Border createSectionBorder(String title) {
        Color borderColor = darkMode ? BORDER_SOFT : LIGHT_BORDER_SOFT;
        Color titleColor = darkMode ? TEXT_MUTED : LIGHT_TEXT_MUTED;
        return new CompoundBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(borderColor, 1, true),
                        title,
                        0,
                        0,
                        new Font("Segoe UI", Font.BOLD, 13),
                        titleColor
                ),
                new EmptyBorder(10, 12, 10, 12)
        );
    }

    // Applies shared Swing defaults used by this window.
    private void applyBaseLookAndFeel() {
        UIManager.put("ToolTip.background", BG_SURFACE_ALT);
        UIManager.put("ToolTip.foreground", TEXT_PRIMARY);
    }

    // Adjusts an RGB color by a signed brightness amount.
    private Color adjustBrightness(Color source, int amount) {
        int red = Math.max(0, Math.min(255, source.getRed() + amount));
        int green = Math.max(0, Math.min(255, source.getGreen() + amount));
        int blue = Math.max(0, Math.min(255, source.getBlue() + amount));
        return new Color(red, green, blue);
    }

    // Enables or disables form fields based on connection state.
    private void setFieldsEditable(boolean editable) {
        animalTypeField.setEditable(editable);
        latitudeField.setEditable(editable);
        longitudeField.setEditable(editable);
        observedAtField.setEditable(editable);
        durationField.setEditable(editable);
        uuidField.setEditable(editable);
        revisitedCheckBox.setEnabled(editable);
        idField.setEditable(false);
    }

    // Clears the form and resets record navigation markers.
    private void clearRecordDisplay() {
        currentIndex = -1;
        idField.setText("");
        animalTypeField.setText("");
        latitudeField.setText("");
        longitudeField.setText("");
        observedAtField.setText("");
        durationField.setText("");
        uuidField.setText("");
        revisitedCheckBox.setSelected(false);
        recordCounterLabel.setText("Record: 0/0");
    }

    // Guards actions that require an active database connection.
    private boolean ensureConnected() {
        if (isConnected) {
            return true;
        }
        statusLabel.setText("Status: Disconnected - connect to MySQL first");
        statusLabel.setForeground(WARNING);
        return false;
    }

    // Validates and maps form values into an Animal model.
    private Animal buildAnimalFromFields(Integer id) {
        String animalType = animalTypeField.getText().trim();
        String latText = latitudeField.getText().trim();
        String lonText = longitudeField.getText().trim();
        String observedAtText = observedAtField.getText().trim();
        String durationText = durationField.getText().trim();
        String uuidText = uuidField.getText().trim();

        if (animalType.isEmpty()) {
            throw new IllegalArgumentException("Animal type cannot be empty");
        }

        float latitude;
        float longitude;
        int durationMinutes;

        try {
            latitude = Float.parseFloat(latText);
            longitude = Float.parseFloat(lonText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Latitude and longitude must be valid float values");
        }

        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        try {
            durationMinutes = Integer.parseInt(durationText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Duration must be a whole number (minutes)");
        }

        if (durationMinutes < 0) {
            throw new IllegalArgumentException("Duration must be zero or greater");
        }

        LocalDateTime observedAt;
        if (observedAtText.isEmpty()) {
            observedAt = LocalDateTime.now();
        } else {
            try {
                observedAt = LocalDateTime.parse(observedAtText, DATETIME_FORMATTER);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Observed At must use yyyy-MM-dd HH:mm:ss");
            }
        }

        UUID observationUuid;
        if (uuidText.isEmpty()) {
            observationUuid = UUID.randomUUID();
        } else {
            try {
                observationUuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Observation UUID must be a valid UUID value");
            }
        }

        Animal animal = new Animal(
                animalType,
                latitude,
                longitude,
                observedAt,
                durationMinutes,
                observationUuid,
                revisitedCheckBox.isSelected()
        );
        animal.setId(id);
        return animal;
    }

    // Displays a single record and wraps around at the ends.
    private void showRecordAtIndex(int index, String status) {
        if (!isConnected) {
            clearRecordDisplay();
            statusLabel.setText("Status: Disconnected");
            return;
        }

        if (connectedAnimals.isEmpty()) {
            clearRecordDisplay();
            statusLabel.setText("Status: Connected - no records");
            return;
        }

        if (index < 0) {
            index = connectedAnimals.size() - 1;
        } else if (index >= connectedAnimals.size()) {
            index = 0;
        }

        currentIndex = index;
        Animal current = connectedAnimals.get(currentIndex);

        idField.setText(current.getId() == null ? "" : String.valueOf(current.getId()));
        animalTypeField.setText(current.getAnimalType());
        latitudeField.setText(String.valueOf(current.getLatitude()));
        longitudeField.setText(String.valueOf(current.getLongitude()));
        observedAtField.setText(current.getObservedAt() == null ? "" : current.getObservedAt().format(DATETIME_FORMATTER));
        durationField.setText(String.valueOf(current.getDurationMinutes()));
        uuidField.setText(current.getObservationUuid() == null ? "" : current.getObservationUuid().toString());
        revisitedCheckBox.setSelected(current.isRevisited());

        recordCounterLabel.setText("Record: " + (currentIndex + 1) + "/" + connectedAnimals.size());
        statusLabel.setText("Status: " + status);
        statusLabel.setForeground(SUCCESS);
    }

    // Reloads records from MySQL and focuses a preferred ID when provided.
    private void reloadConnectedAnimals(Integer focusId, String statusText) throws java.sql.SQLException {
        connectedAnimals = new ArrayList<>(mysqlRepository.fetchAllAnimals());

        if (connectedAnimals.isEmpty()) {
            clearRecordDisplay();
            statusLabel.setText("Status: Connected - no records");
            statusLabel.setForeground(WARNING);
            return;
        }

        int targetIndex = 0;
        if (focusId != null) {
            for (int i = 0; i < connectedAnimals.size(); i++) {
                Integer id = connectedAnimals.get(i).getId();
                if (id != null && id.equals(focusId)) {
                    targetIndex = i;
                    break;
                }
            }
        } else if (currentIndex >= 0 && currentIndex < connectedAnimals.size()) {
            targetIndex = currentIndex;
        } else if (currentIndex >= connectedAnimals.size()) {
            targetIndex = connectedAnimals.size() - 1;
        }

        showRecordAtIndex(targetIndex, statusText);
    }

    // Connects or disconnects from MySQL and updates UI state.
    private void onConnectionToggleClicked() {
        try {
            if (isConnected) {
                isConnected = false;
                isInsertMode = false;
                insertButton.setText("Insert");
                connectButton.setText("Connect");
                setFieldsEditable(false);
                connectedAnimals.clear();
                clearRecordDisplay();
                statusLabel.setText("Status: Disconnected");
                statusLabel.setForeground(TEXT_MUTED);
                return;
            }

            if (mysqlRepository == null) {
                statusLabel.setText("Status: MySQL not configured (set DB_URL, DB_USER, DB_PASSWORD)");
                statusLabel.setForeground(WARNING);
                return;
            }

            mysqlRepository.ensureIdAutoIncrementPrimaryKey();
            connectedAnimals = new ArrayList<>(mysqlRepository.fetchAllAnimals());
            isConnected = true;
            isInsertMode = false;
            insertButton.setText("Insert");
            connectButton.setText("Disconnect");
            setFieldsEditable(true);

            if (connectedAnimals.isEmpty()) {
                clearRecordDisplay();
                statusLabel.setText("Status: Connected - no records");
                statusLabel.setForeground(WARNING);
            } else {
                showRecordAtIndex(0, "Connected");
            }
        } catch (java.sql.SQLException | RuntimeException e) {
            isConnected = false;
            setFieldsEditable(false);
            connectedAnimals.clear();
            clearRecordDisplay();
            statusLabel.setText("Status: Connection error - " + e.getMessage());
            statusLabel.setForeground(new Color(226, 117, 117));
        }
    }

    // Starts insert mode or persists a new observation row.
    private void onInsertClicked() {
        try {
            if (!ensureConnected()) {
                return;
            }

            if (!isInsertMode) {
                isInsertMode = true;
                insertButton.setText("Add");
                clearRecordDisplay();
                observedAtField.setText(LocalDateTime.now().format(DATETIME_FORMATTER));
                statusLabel.setText("Status: Insert mode - fill fields and click Add");
                statusLabel.setForeground(ACCENT);
                return;
            }

            Animal newRecord = buildAnimalFromFields(null);
            Animal inserted = mysqlRepository.insertAnimal(newRecord);

            isInsertMode = false;
            insertButton.setText("Insert");
            reloadConnectedAnimals(inserted.getId(), "Observation inserted");
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Status: " + e.getMessage());
            statusLabel.setForeground(WARNING);
        } catch (java.sql.SQLException | RuntimeException e) {
            statusLabel.setText("Status: Insert failed - " + e.getMessage());
            statusLabel.setForeground(new Color(226, 117, 117));
        }
    }

    // Saves edits for the currently selected observation.
    private void onUpdateClicked() {
        try {
            if (!ensureConnected()) {
                return;
            }

            if (isInsertMode) {
                statusLabel.setText("Status: Finish insert first or click Add");
                return;
            }

            if (currentIndex < 0 || currentIndex >= connectedAnimals.size()) {
                statusLabel.setText("Status: No record selected");
                return;
            }

            Integer id = connectedAnimals.get(currentIndex).getId();
            if (id == null) {
                statusLabel.setText("Status: Selected record has no ID");
                return;
            }

            Animal updated = buildAnimalFromFields(id);
            mysqlRepository.updateAnimal(updated);
            reloadConnectedAnimals(id, "Observation updated");
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Status: " + e.getMessage());
            statusLabel.setForeground(WARNING);
        } catch (java.sql.SQLException | RuntimeException e) {
            statusLabel.setText("Status: Update failed - " + e.getMessage());
            statusLabel.setForeground(new Color(226, 117, 117));
        }
    }

    // Deletes the currently selected observation after confirmation.
    private void onDeleteClicked() {
        try {
            if (!ensureConnected()) {
                return;
            }

            if (isInsertMode) {
                statusLabel.setText("Status: Finish insert first or click Add");
                return;
            }

            if (currentIndex < 0 || currentIndex >= connectedAnimals.size()) {
                statusLabel.setText("Status: No record selected");
                return;
            }

            Animal selected = connectedAnimals.get(currentIndex);
            Integer id = selected.getId();
            if (id == null) {
                statusLabel.setText("Status: Selected record has no ID");
                return;
            }

            String message = "Delete observation for " + selected.getAnimalType()
                    + " at (" + selected.getLatitude() + ", " + selected.getLongitude() + ")?"
                    + "\nUUID: " + selected.getObservationUuid();

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    message,
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice != JOptionPane.YES_OPTION) {
                statusLabel.setText("Status: Delete cancelled");
                statusLabel.setForeground(TEXT_MUTED);
                return;
            }

            mysqlRepository.deleteAnimalById(id);
            reloadConnectedAnimals(null, "Observation deleted");
        } catch (java.sql.SQLException | RuntimeException e) {
            statusLabel.setText("Status: Delete failed - " + e.getMessage());
            statusLabel.setForeground(new Color(226, 117, 117));
        }
    }

    // Opens an animated globe view and plots animal observations by latitude/longitude.
    private void onMappingClicked() {
        List<Animal> sourceAnimals;
        if (isConnected) {
            sourceAnimals = new ArrayList<>(connectedAnimals);
        } else {
            sourceAnimals = container.getAllAnimals();
        }

        if (sourceAnimals.isEmpty()) {
            statusLabel.setText("Status: No observations available for mapping");
            statusLabel.setForeground(WARNING);
            return;
        }

        JFrame mapFrame = new JFrame("3D Observation Mapping");
        mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mapFrame.setSize(760, 620);
        mapFrame.setLocationRelativeTo(this);

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBorder(new EmptyBorder(8, 8, 8, 8));
        wrapper.setBackground(darkMode ? BG_PRIMARY : LIGHT_BG_PRIMARY);

        JLabel subtitle = new JLabel("Drag to rotate. Dots represent observations by latitude/longitude.");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(darkMode ? TEXT_MUTED : LIGHT_TEXT_MUTED);
        wrapper.add(subtitle, BorderLayout.NORTH);

        GlobePanel globePanel = new GlobePanel(sourceAnimals, darkMode);
        wrapper.add(globePanel, BorderLayout.CENTER);

        JPanel mapControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        mapControls.setOpaque(false);
        JButton zoomOutButton = createStyledButton("-");
        JButton zoomResetButton = createStyledButton("Reset");
        JButton zoomInButton = createStyledButton("+");

        zoomOutButton.setPreferredSize(new Dimension(52, 30));
        zoomResetButton.setPreferredSize(new Dimension(82, 30));
        zoomInButton.setPreferredSize(new Dimension(52, 30));

        JLabel zoomHint = new JLabel("Zoom");
        zoomHint.setHorizontalAlignment(SwingConstants.CENTER);
        zoomHint.setFont(BODY_FONT);
        zoomHint.setForeground(darkMode ? TEXT_MUTED : LIGHT_TEXT_MUTED);

        zoomOutButton.addActionListener(e -> globePanel.adjustZoom(-0.10));
        zoomInButton.addActionListener(e -> globePanel.adjustZoom(0.10));
        zoomResetButton.addActionListener(e -> globePanel.resetZoom());

        mapControls.add(zoomHint);
        mapControls.add(zoomOutButton);
        mapControls.add(zoomResetButton);
        mapControls.add(zoomInButton);
        wrapper.add(mapControls, BorderLayout.SOUTH);

        mapFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                globePanel.stopAnimation();
            }
        });

        mapFrame.setContentPane(wrapper);
        mapFrame.setVisible(true);

        statusLabel.setText("Status: Opened 3D mapping view");
        statusLabel.setForeground(ACCENT);
    }

    // Lightweight 3D globe renderer using projected points and a rotation animation.
    private static final class GlobePanel extends JPanel {
        private final List<Animal> animals;
        private final boolean darkMode;
        private final Timer spinTimer;
        private double rotationY;
        private double zoom;
        private int lastMouseX;

        // Coarse continent silhouettes using lat/lon points for a stylized in-app map.
        private static final double[][][] CONTINENT_SHAPES = {
            {
                {72, -170}, {66, -140}, {62, -120}, {55, -105}, {47, -125}, {38, -122},
                {27, -112}, {23, -98}, {19, -90}, {14, -84}, {23, -80}, {31, -79},
                {42, -72}, {51, -66}, {60, -74}, {68, -98}, {72, -130}
            },
            {
                {12, -81}, {6, -77}, {-3, -76}, {-14, -72}, {-24, -67}, {-34, -62},
                {-47, -70}, {-53, -73}, {-53, -59}, {-44, -50}, {-31, -45}, {-14, -50},
                {-2, -58}, {6, -66}
            },
            {
                {71, -10}, {66, 10}, {58, 21}, {56, 34}, {50, 30}, {45, 12}, {43, 0},
                {39, -4}, {37, 8}, {36, 16}, {32, 23}, {30, 35}, {31, 43}, {22, 50},
                {16, 44}, {10, 43}, {2, 36}, {-8, 35}, {-16, 31}, {-28, 27}, {-34, 20},
                {-34, 12}, {-24, 12}, {-9, 15}, {5, 5}, {19, -14}, {28, -16}, {39, -9},
                {52, -12}, {60, -20}
            },
            {
                {76, 43}, {74, 70}, {70, 100}, {58, 134}, {49, 145}, {35, 139}, {27, 121},
                {21, 106}, {11, 102}, {8, 86}, {19, 74}, {26, 66}, {31, 52}, {42, 43},
                {54, 50}, {63, 60}, {72, 72}, {78, 92}, {77, 115}, {72, 132}, {68, 150},
                {59, 162}, {52, 153}, {49, 127}, {38, 115}, {30, 124}, {24, 138}, {14, 146},
                {8, 130}, {6, 110}, {6, 95}, {17, 79}, {30, 68}, {41, 58}, {57, 60}, {70, 45}
            },
            {
                {-10, 114}, {-18, 116}, {-27, 132}, {-38, 145}, {-42, 154}, {-33, 152},
                {-25, 146}, {-19, 139}, {-15, 128}
            }
        };

        // A few stylized "country boundary" strokes to improve visual detail.
        private static final double[][][] COUNTRY_STROKES = {
            {{55, -130}, {45, -104}, {32, -96}, {24, -88}},
            {{52, -6}, {46, 4}, {41, 16}, {36, 26}},
            {{23, 37}, {12, 44}, {2, 34}, {-8, 29}},
            {{44, 78}, {33, 86}, {27, 100}, {21, 110}},
            {{-12, -71}, {-24, -64}, {-34, -58}},
            {{-15, 18}, {-24, 24}, {-31, 27}}
        };

        private GlobePanel(List<Animal> animals, boolean darkMode) {
            this.animals = new ArrayList<>(animals);
            this.darkMode = darkMode;
            this.rotationY = 0.35;
            this.zoom = 1.0;

            setOpaque(true);
            setBackground(darkMode ? new Color(17, 22, 30) : new Color(239, 244, 252));

            spinTimer = new Timer(30, e -> {
                rotationY += 0.008;
                repaint();
            });
            spinTimer.start();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMouseX = e.getX();
                }
            });

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    int dx = e.getX() - lastMouseX;
                    rotationY += dx * 0.01;
                    lastMouseX = e.getX();
                    repaint();
                }
            });

            addMouseWheelListener(e -> adjustZoom(-e.getPreciseWheelRotation() * 0.06));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int baseRadius = Math.max(90, Math.min(width, height) / 2 - 48);
            int radius = (int) Math.round(baseRadius * zoom);
            int cx = width / 2;
            int cy = height / 2 + 8;

            drawBackdrop(g2, width, height);
            drawGlobe(g2, cx, cy, radius);
            drawAnimals(g2, cx, cy, radius);

            g2.dispose();
        }

        private void drawBackdrop(Graphics2D g2, int width, int height) {
            Color top = darkMode ? new Color(28, 36, 49) : new Color(212, 226, 246);
            Color bottom = darkMode ? new Color(12, 16, 22) : new Color(244, 249, 255);
            g2.setPaint(new GradientPaint(0, 0, top, 0, height, bottom));
            g2.fillRect(0, 0, width, height);
        }

        private void drawGlobe(Graphics2D g2, int cx, int cy, int radius) {
            Color oceanA = darkMode ? new Color(38, 72, 117) : new Color(112, 161, 224);
            Color oceanB = darkMode ? new Color(19, 41, 74) : new Color(66, 118, 191);

            g2.setPaint(new GradientPaint(cx - radius, cy - radius, oceanA, cx + radius, cy + radius, oceanB));
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            drawContinents(g2, cx, cy, radius);
            drawCountryStrokes(g2, cx, cy, radius);

            g2.setColor(darkMode ? new Color(184, 205, 236, 140) : new Color(255, 255, 255, 170));
            g2.fillOval(cx - (int) (radius * 0.62), cy - (int) (radius * 0.75), (int) (radius * 0.58), (int) (radius * 0.42));

            g2.setColor(darkMode ? new Color(162, 196, 239, 120) : new Color(255, 255, 255, 140));
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            g2.setColor(darkMode ? new Color(129, 157, 193, 60) : new Color(79, 119, 173, 70));
            for (int i = -2; i <= 2; i++) {
                int y = cy + (int) (i * radius * 0.32);
                int w = (int) (radius * 2.0 * Math.cos(Math.abs(i) * 0.28));
                g2.drawOval(cx - w / 2, y - (int) (radius * 0.15), w, (int) (radius * 0.30));
            }
        }

        private void drawContinents(Graphics2D g2, int cx, int cy, int radius) {
            g2.setColor(darkMode ? new Color(78, 148, 98, 188) : new Color(108, 168, 106, 202));

            for (double[][] shape : CONTINENT_SHAPES) {
                List<Projection> projected = new ArrayList<>();
                for (double[] point : shape) {
                    Projection p = project(point[0], point[1], cx, cy, radius);
                    if (p != null) {
                        projected.add(p);
                    }
                }

                if (projected.size() < 3) {
                    continue;
                }

                int[] xs = new int[projected.size()];
                int[] ys = new int[projected.size()];
                for (int i = 0; i < projected.size(); i++) {
                    xs[i] = projected.get(i).x;
                    ys[i] = projected.get(i).y;
                }

                g2.fillPolygon(xs, ys, projected.size());
                g2.setColor(darkMode ? new Color(39, 96, 62, 210) : new Color(70, 124, 74, 220));
                g2.drawPolygon(xs, ys, projected.size());
                g2.setColor(darkMode ? new Color(78, 148, 98, 188) : new Color(108, 168, 106, 202));
            }
        }

        private void drawCountryStrokes(Graphics2D g2, int cx, int cy, int radius) {
            g2.setColor(darkMode ? new Color(215, 232, 181, 145) : new Color(240, 251, 228, 164));
            for (double[][] line : COUNTRY_STROKES) {
                Projection prev = null;
                for (double[] point : line) {
                    Projection current = project(point[0], point[1], cx, cy, radius);
                    if (prev != null && current != null) {
                        g2.drawLine(prev.x, prev.y, current.x, current.y);
                    }
                    prev = current;
                }
            }
        }

        private Projection project(double latitude, double longitude, int cx, int cy, int radius) {
            double latRad = Math.toRadians(latitude);
            double lonRad = Math.toRadians(longitude);

            double x = Math.cos(latRad) * Math.cos(lonRad);
            double y = Math.sin(latRad);
            double z = Math.cos(latRad) * Math.sin(lonRad);

            double sinR = Math.sin(rotationY);
            double cosR = Math.cos(rotationY);
            double rx = x * cosR + z * sinR;
            double rz = -x * sinR + z * cosR;

            if (rz <= -0.22) {
                return null;
            }

            int px = cx + (int) (rx * radius * 0.94);
            int py = cy - (int) (y * radius * 0.94);
            return new Projection(px, py, rz);
        }

        private void drawAnimals(Graphics2D g2, int cx, int cy, int radius) {
            List<ProjectedPoint> points = new ArrayList<>();

            for (Animal animal : animals) {
                Projection p = project(animal.getLatitude(), animal.getLongitude(), cx, cy, radius);
                if (p != null) {
                    double depth = (p.z + 1.0) / 2.0;
                    points.add(new ProjectedPoint(p.x, p.y, depth, animal));
                }
            }

            Collections.sort(points, Comparator.comparingDouble(p -> p.depth));

            for (ProjectedPoint point : points) {
                int dotSize = 5 + (int) Math.round(4 * point.depth);
                Color dotColor = colorForAnimal(point.animal.getAnimalType(), point.depth);

                g2.setColor(new Color(0, 0, 0, 95));
                g2.fillOval(point.x + 1, point.y + 1, dotSize, dotSize);

                g2.setColor(dotColor);
                g2.fillOval(point.x, point.y, dotSize, dotSize);

                String label = point.animal.getAnimalType();
                if (label.length() > 10) {
                    label = label.substring(0, 10);
                }

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(darkMode ? new Color(236, 242, 250, 220) : new Color(29, 44, 68, 220));
                g2.drawString(label, point.x + dotSize + 3, point.y + dotSize);
            }

            g2.setColor(darkMode ? new Color(226, 233, 245) : new Color(30, 49, 73));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Observations shown: " + points.size(), 12, getHeight() - 14);
            g2.drawString("Zoom: " + Math.round(zoom * 100) + "%", 160, getHeight() - 14);
        }

        private void adjustZoom(double delta) {
            zoom = Math.max(0.65, Math.min(1.95, zoom + delta));
            repaint();
        }

        private void resetZoom() {
            zoom = 1.0;
            repaint();
        }

        private void stopAnimation() {
            spinTimer.stop();
        }

        private Color colorForAnimal(String animalType, double depth) {
            int hash = animalType == null ? 0 : Math.abs(animalType.hashCode());
            float hue = (hash % 360) / 360.0f;
            float saturation = 0.72f;
            float brightness = (float) (0.62 + depth * 0.32);
            return Color.getHSBColor(hue, saturation, Math.min(1.0f, brightness));
        }

        private static final class ProjectedPoint {
            private final int x;
            private final int y;
            private final double depth;
            private final Animal animal;

            private ProjectedPoint(int x, int y, double depth, Animal animal) {
                this.x = x;
                this.y = y;
                this.depth = depth;
                this.animal = animal;
            }
        }

        private static final class Projection {
            private final int x;
            private final int y;
            private final double z;

            private Projection(int x, int y, double z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }
    }

    // Allows callers to update status text without exposing label internals.
    public void updateDisplay(String content) {
        statusLabel.setText("Status: " + content);
        statusLabel.setForeground(ACCENT);
    }
}
