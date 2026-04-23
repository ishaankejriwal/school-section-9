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

// Swing desktop interface for browsing and maintaining observation records.
public class AnimalGUI extends JFrame {
    private final AnimalContainer container;
    private final MySQLAnimalRepository mysqlRepository;

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

    private JButton connectButton;
    private JButton insertButton;

    private boolean isConnected;
    private boolean isInsertMode;
    private int currentIndex;
    private List<Animal> connectedAnimals;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AnimalGUI(AnimalContainer container) {
        this.container = container;
        this.mysqlRepository = MySQLAnimalRepository.fromEnvironment();
        this.isConnected = false;
        this.isInsertMode = false;
        this.currentIndex = -1;
        this.connectedAnimals = new ArrayList<>();
        initializeGUI();
    }

    private void initializeGUI() {
        setTitle("Animal Observation Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 620);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Animal Observation Tracker");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setToolTipText(container.getContainerInfo());

        statusLabel = new JLabel("Status: Disconnected - click Connect to load observations");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(10, 90, 10));

        recordCounterLabel = new JLabel("Record: 0/0");
        recordCounterLabel.setFont(new Font("Arial", Font.BOLD, 12));

        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(statusLabel);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(recordCounterLabel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createTitledBorder("Observation Record"));

        idField = createTextField(false);
        animalTypeField = createTextField(true);
        latitudeField = createTextField(true);
        longitudeField = createTextField(true);
        observedAtField = createTextField(true);
        durationField = createTextField(true);
        uuidField = createTextField(true);

        revisitedCheckBox = new JCheckBox("Revisited Spot");
        revisitedCheckBox.setOpaque(false);

        formPanel.add(createRecordEntryRow("ID:", idField));
        formPanel.add(createRecordEntryRow("Animal Type:", animalTypeField));
        formPanel.add(createRecordEntryRow("Latitude:", latitudeField));
        formPanel.add(createRecordEntryRow("Longitude:", longitudeField));
        formPanel.add(createRecordEntryRow("Observed At:", observedAtField));
        formPanel.add(createRecordEntryRow("Duration (min):", durationField));
        formPanel.add(createRecordEntryRow("Observation UUID:", uuidField));

        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkBoxPanel.add(revisitedCheckBox);
        formPanel.add(checkBoxPanel);

        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel hintLabel = new JLabel("Timestamp format: yyyy-MM-dd HH:mm:ss");
        hintLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        hintPanel.add(hintLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(hintPanel);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new GridLayout(3, 1, 0, 8));

        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
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
        insertButton = createStyledButton("Insert");
        JButton updateButton = createStyledButton("Update");
        JButton deleteButton = createStyledButton("Delete");

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
        connectButton = createStyledButton("Connect");
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
    }

    private JTextField createTextField(boolean editable) {
        JTextField field = new JTextField();
        field.setEditable(editable);
        field.setFont(new Font("Arial", Font.PLAIN, 12));
        field.setPreferredSize(new Dimension(0, 30));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return field;
    }

    private JPanel createRecordEntryRow(String labelText, JTextField textField) {
        JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        rowPanel.setPreferredSize(new Dimension(0, 38));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(130, 24));

        rowPanel.add(label, BorderLayout.WEST);
        rowPanel.add(textField, BorderLayout.CENTER);
        return rowPanel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        return button;
    }

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

    private boolean ensureConnected() {
        if (isConnected) {
            return true;
        }
        statusLabel.setText("Status: Disconnected - connect to MySQL first");
        return false;
    }

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
    }

    private void reloadConnectedAnimals(Integer focusId, String statusText) throws java.sql.SQLException {
        connectedAnimals = new ArrayList<>(mysqlRepository.fetchAllAnimals());

        if (connectedAnimals.isEmpty()) {
            clearRecordDisplay();
            statusLabel.setText("Status: Connected - no records");
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
                return;
            }

            if (mysqlRepository == null) {
                statusLabel.setText("Status: MySQL not configured (set DB_URL, DB_USER, DB_PASSWORD)");
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
            } else {
                showRecordAtIndex(0, "Connected");
            }
        } catch (java.sql.SQLException | RuntimeException e) {
            isConnected = false;
            setFieldsEditable(false);
            connectedAnimals.clear();
            clearRecordDisplay();
            statusLabel.setText("Status: Connection error - " + e.getMessage());
        }
    }

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
                return;
            }

            Animal newRecord = buildAnimalFromFields(null);
            Animal inserted = mysqlRepository.insertAnimal(newRecord);

            isInsertMode = false;
            insertButton.setText("Insert");
            reloadConnectedAnimals(inserted.getId(), "Observation inserted");
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Status: " + e.getMessage());
        } catch (java.sql.SQLException | RuntimeException e) {
            statusLabel.setText("Status: Insert failed - " + e.getMessage());
        }
    }

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
        } catch (java.sql.SQLException | RuntimeException e) {
            statusLabel.setText("Status: Update failed - " + e.getMessage());
        }
    }

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
                return;
            }

            mysqlRepository.deleteAnimalById(id);
            reloadConnectedAnimals(null, "Observation deleted");
        } catch (java.sql.SQLException | RuntimeException e) {
            statusLabel.setText("Status: Delete failed - " + e.getMessage());
        }
    }

    // Allows Main and future menu actions to set top-level status text.
    public void updateDisplay(String content) {
        statusLabel.setText("Status: " + content);
    }
}
