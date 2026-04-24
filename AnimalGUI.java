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
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

        JPanel controlsPanel = new JPanel(new GridLayout(3, 1, 0, 8));
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

        controlsPanel.add(navigationPanel);
        controlsPanel.add(crudPanel);
        controlsPanel.add(connectionPanel);

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

    // Allows callers to update status text without exposing label internals.
    public void updateDisplay(String content) {
        statusLabel.setText("Status: " + content);
        statusLabel.setForeground(ACCENT);
    }
}
