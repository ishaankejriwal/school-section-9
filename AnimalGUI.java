// Swing desktop interface for browsing and maintaining animal records.
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class AnimalGUI extends JFrame {
    // Text fields used for displaying and editing the current record.
    private JTextField displayIdField;
    private JTextField displayNameField;
    private JTextField displayAgeField;
    private JTextField displayTypeField;
    private JTextField displaySpecialField;
    // Primary action buttons for navigation, CRUD, and connection state.
    private JButton helloWorldButton;
    private JButton connectionToggleButton;
    private JButton insertButton;
    private JButton updateButton;
    private JButton deleteButton;
    private JLabel statusLabel;
    private JLabel counterLabel;
    private JLabel recordCounterLabel;
    private JPanel contentPanel;
    // In-memory and database-backed data references.
    private AnimalContainer container;
    private MySQLAnimalRepository mysqlRepository;
    private List<Animal> connectedAnimals;
    // UI and interaction state flags.
    private int clickCount;
    private boolean isExpanded;
    private boolean isConnected;
    private boolean isInsertMode;
    private int currentIndex;
    
    // Relative file paths for UI assets.
    private static final String BACKGROUND_IMAGE_PATH = "cat.jpg";
    private static final String ICON_FILE_PATH = "OIP.ico";
    // Alternate icon formats used when ICO decoding fails.
    private static final String[] ICON_FORMATS = {"OIP.png", "OIP.jpg", "OIP.gif"};

    // Creates the GUI and prepares initial application state.
    public AnimalGUI(AnimalContainer container) {
        this.container = container;
        this.mysqlRepository = MySQLAnimalRepository.fromEnvironment();
        this.connectedAnimals = new ArrayList<>();
        this.clickCount = 0;
        this.isExpanded = false;
        this.isConnected = false;
        this.isInsertMode = false;
        this.currentIndex = -1;
        setWindowIcon();
        initializeGUI();
        showExpandedLayoutOnStartup();
    }
    
    // Attempts to set the window icon from available local files.
    private void setWindowIcon() {
        try {
            // Try the preferred ICO file first.
            if (loadIconFromFile(ICON_FILE_PATH)) {
                return;
            }
            
            // Try fallback image formats when ICO is unavailable.
            for (String format : ICON_FORMATS) {
                if (loadIconFromFile(format)) {
                    System.out.println("Icon loaded from: " + format);
                    return;
                }
            }
            
            System.err.println("No icon file found. Tried: " + ICON_FILE_PATH + 
                             " and fallback formats");
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
        }
    }
    
    // Loads an icon from disk and applies it to the frame.
    private boolean loadIconFromFile(String filePath) {
        try {
            File iconFile = new File(filePath);
            if (!iconFile.exists()) {
                return false;
            }
            
            // Use ImageIO for common image formats.
            java.awt.image.BufferedImage image = ImageIO.read(iconFile);
            if (image != null) {
                setIconImage(image);
                System.out.println("Icon loaded successfully from: " + filePath);
                return true;
            }
            
            // Fall back to ImageIcon for ICO support.
            if (filePath.endsWith(".ico")) {
                ImageIcon icon = new ImageIcon(filePath);
                if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                    setIconImage(icon.getImage());
                    System.out.println("Icon loaded successfully from: " + filePath);
                    return true;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Could not load icon from " + filePath + ": " + e.getMessage());
        }
        
        return false;
    }

    // Builds the initial two-state layout (welcome view and full workspace).
    private void initializeGUI() {
        try {
            setTitle("Animal Management System");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(950, 700);
            setLocationRelativeTo(null);
            setResizable(true);

            // Build the startup view with one entry action.
            JPanel initialPanel = new JPanel(new BorderLayout());
            initialPanel.setBackground(new Color(240, 240, 240));

            JPanel welcomePanel = new JPanel(new BorderLayout());
            welcomePanel.setBackground(new Color(240, 240, 240));

            JLabel titleLabel = new JLabel("Animal Management System");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setHorizontalAlignment(JLabel.CENTER);
            welcomePanel.add(titleLabel, BorderLayout.NORTH);

            JPanel buttonCenterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonCenterPanel.setBackground(new Color(240, 240, 240));
            helloWorldButton = createStyledButton("Hello World");
            helloWorldButton.setPreferredSize(new Dimension(200, 60));
            helloWorldButton.setFont(new Font("Arial", Font.BOLD, 18));
            helloWorldButton.addActionListener(e -> expandToFullGUI());
            buttonCenterPanel.add(helloWorldButton);
            welcomePanel.add(buttonCenterPanel, BorderLayout.CENTER);

            initialPanel.add(welcomePanel, BorderLayout.CENTER);

            // Create the full editor panel and keep it hidden initially.
            contentPanel = createContentPanel();
            contentPanel.setVisible(false);

            // Use CardLayout to switch between startup and full interface.
            JPanel mainContainer = new JPanel(new CardLayout());
            mainContainer.add(initialPanel, "initial");
            mainContainer.add(contentPanel, "expanded");

            setContentPane(mainContainer);
        } catch (Exception e) {
            System.err.println("Error initializing GUI: " + e.getMessage());
        }
    }

    // Expands from the welcome state into the full application view.
    private void expandToFullGUI() {
        try {
            clickCount++;
            isExpanded = true;

            // Build top-level menus.
            createMenuBar();

            // Swap to the full content panel.
            CardLayout cl = (CardLayout) getContentPane().getLayout();
            cl.show(getContentPane(), "expanded");

            // Trigger initial record presentation.
            onHelloWorldButtonClicked();
        } catch (Exception e) {
            System.err.println("Error expanding GUI: " + e.getMessage());
        }
    }

    // Shows the full workspace immediately so navigation, CRUD, and connection controls are visible on launch.
    private void showExpandedLayoutOnStartup() {
        try {
            isExpanded = true;
            createMenuBar();
            CardLayout cl = (CardLayout) getContentPane().getLayout();
            cl.show(getContentPane(), "expanded");
            statusLabel.setText("Status: Disconnected - click Connect to load MySQL records");
        } catch (Exception e) {
            System.err.println("Error showing startup layout: " + e.getMessage());
        }
    }

    // Creates the full content panel containing record form and controls.
    private JPanel createContentPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Paint background image when available.
                try {
                    File backgroundFile = new File(BACKGROUND_IMAGE_PATH);
                    if (backgroundFile.exists()) {
                        ImageIcon background = new ImageIcon(BACKGROUND_IMAGE_PATH);
                        g.drawImage(background.getImage(), 0, 0, getWidth(), getHeight(), this);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading background image: " + e.getMessage());
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top strip showing click and status metadata.
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        counterLabel = new JLabel("Click Count: " + clickCount);
        counterLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel = new JLabel("Status: Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(0, 100, 0));

        topPanel.add(counterLabel);
        topPanel.add(statusLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center section for current record display and editing.
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.setBorder(BorderFactory.createTitledBorder("Current Record"));
        
        recordCounterLabel = new JLabel("Record: 1/1");
        recordCounterLabel.setFont(new Font("Arial", Font.BOLD, 12));
        displayPanel.add(recordCounterLabel);

        displayPanel.add(Box.createVerticalStrut(8));

        displayIdField = new JTextField();
        displayIdField.setEditable(false);
        displayIdField.setBackground(new Color(230, 230, 230));
        displayIdField.setForeground(Color.DARK_GRAY);
        displayIdField.setFont(new Font("Arial", Font.PLAIN, 12));
        displayPanel.add(createRecordEntryRow("ID:", displayIdField));

        displayNameField = new JTextField();
        displayNameField.setEditable(true);
        displayNameField.setFont(new Font("Arial", Font.PLAIN, 12));
        displayPanel.add(createRecordEntryRow("Name:", displayNameField));

        displayAgeField = new JTextField();
        displayAgeField.setEditable(true);
        displayAgeField.setFont(new Font("Arial", Font.PLAIN, 12));
        displayPanel.add(createRecordEntryRow("Age:", displayAgeField));

        displayTypeField = new JTextField();
        displayTypeField.setEditable(true);
        displayTypeField.setFont(new Font("Arial", Font.PLAIN, 12));
        displayPanel.add(createRecordEntryRow("Type:", displayTypeField));

        displaySpecialField = new JTextField();
        displaySpecialField.setEditable(true);
        displaySpecialField.setFont(new Font("Arial", Font.PLAIN, 12));
        displayPanel.add(createRecordEntryRow("Details:", displaySpecialField));
        
        mainPanel.add(displayPanel, BorderLayout.CENTER);

        // Bottom section split into navigation, CRUD, and connection controls.
        JPanel controlsPanel = new JPanel(new GridLayout(3, 1, 0, 6));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        controlsPanel.setPreferredSize(new Dimension(0, 130));

        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JButton goToStartButton = createStyledButton("Start");
        goToStartButton.addActionListener(e -> onGoToStartClicked());
        
        JButton backTwoButton = createStyledButton("Back 2");
        backTwoButton.addActionListener(e -> onBackTwoClicked());
        
        JButton backOneButton = createStyledButton("Back 1");
        backOneButton.addActionListener(e -> onBackOneClicked());
        
        JButton forwardOneButton = createStyledButton("Forward 1");
        forwardOneButton.addActionListener(e -> onForwardOneClicked());
        
        JButton forwardTwoButton = createStyledButton("Forward 2");
        forwardTwoButton.addActionListener(e -> onForwardTwoClicked());
        
        JButton goToEndButton = createStyledButton("End");
        goToEndButton.addActionListener(e -> onGoToEndClicked());

        JPanel crudPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        insertButton = createStyledButton("Insert");
        insertButton.setPreferredSize(new Dimension(120, 30));
        insertButton.addActionListener(e -> onInsertClicked());

        updateButton = createStyledButton("Update");
        updateButton.setPreferredSize(new Dimension(120, 30));
        updateButton.addActionListener(e -> onUpdateClicked());

        deleteButton = createStyledButton("Delete");
        deleteButton.setPreferredSize(new Dimension(120, 30));
        deleteButton.addActionListener(e -> onDeleteClicked());

        connectionToggleButton = createStyledButton("Connect");
        connectionToggleButton.setPreferredSize(new Dimension(160, 32));
        connectionToggleButton.addActionListener(e -> onConnectionToggleClicked());

        navigationPanel.add(goToStartButton);
        navigationPanel.add(backTwoButton);
        navigationPanel.add(backOneButton);
        navigationPanel.add(forwardOneButton);
        navigationPanel.add(forwardTwoButton);
        navigationPanel.add(goToEndButton);

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        connectionPanel.add(connectionToggleButton);

        controlsPanel.add(navigationPanel);
        crudPanel.add(insertButton);
        crudPanel.add(updateButton);
        crudPanel.add(deleteButton);
        controlsPanel.add(crudPanel);
        controlsPanel.add(connectionPanel);

        mainPanel.add(controlsPanel, BorderLayout.SOUTH);

        setRecordFieldsEditable(false);
        clearRecordDisplay();

        return mainPanel;
    }

    // Returns the currently active record set when connected.
    private List<Animal> getActiveAnimals() {
        if (!isConnected) {
            return new ArrayList<>();
        }
        return connectedAnimals;
    }

    // Normalizes details text based on selected type for consistent storage.
    private String normalizeDetailsByType(String typeText, String detailsText) {
        String safeType = typeText == null ? "" : typeText.trim().toLowerCase();
        String safeDetails = detailsText == null ? "" : detailsText.trim();

        if ("dog".equals(safeType)) {
            String breed = safeDetails.replaceFirst("(?i)^breed\\s*:\\s*", "").trim();
            return breed.isEmpty() ? "Mixed Breed" : breed;
        }

        if ("cat".equals(safeType)) {
            String color = safeDetails.replaceFirst("(?i)^color\\s*:\\s*", "").trim();
            return color.isEmpty() ? "Unknown" : color;
        }

        return safeDetails;
    }

    // Builds an Animal object from editable form fields.
    private Animal buildAnimalFromFields(Integer id) {
        String name = displayNameField.getText().trim();
        String ageText = displayAgeField.getText().trim();
        String typeText = displayTypeField.getText().trim();
        String detailsText = displaySpecialField.getText().trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (typeText.isEmpty()) {
            throw new IllegalArgumentException("Type must be Dog or Cat");
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Age must be a number");
        }

        String safeType = typeText.trim().toLowerCase();
        String normalizedDetails = normalizeDetailsByType(typeText, detailsText);

        Animal animal;
        if ("dog".equals(safeType)) {
            animal = new Dog(name, age, normalizedDetails);
        } else if ("cat".equals(safeType)) {
            animal = new Cat(name, age, normalizedDetails);
        } else {
            throw new IllegalArgumentException("Type must be Dog or Cat");
        }

        animal.setId(id);
        return animal;
    }

    // Reloads records from MySQL and restores focus to a target record when possible.
    private void reloadConnectedAnimals(Integer focusId, String statusText) throws Exception {
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

    // Handles Insert/Add mode transitions and performs record insertion.
    private void onInsertClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            if (!isInsertMode) {
                isInsertMode = true;
                insertButton.setText("Add");
                displayIdField.setText("");
                displayNameField.setText("");
                displayAgeField.setText("");
                displayTypeField.setText("");
                displaySpecialField.setText("");
                recordCounterLabel.setText("Record: New");
                statusLabel.setText("Status: Insert mode - enter data, then click Add");
                return;
            }

            Animal newAnimal = buildAnimalFromFields(null);
            Animal inserted = mysqlRepository.insertAnimal(newAnimal);

            isInsertMode = false;
            insertButton.setText("Insert");
            reloadConnectedAnimals(inserted.getId(), "Record inserted");
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Status: " + e.getMessage());
        } catch (Exception e) {
            statusLabel.setText("Status: Insert failed - " + e.getMessage());
        }
    }

    // Persists edits for the currently selected record.
    private void onUpdateClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            if (isInsertMode) {
                statusLabel.setText("Status: Finish add first or click Add to save new record");
                return;
            }

            List<Animal> animals = getActiveAnimals();
            if (currentIndex < 0 || currentIndex >= animals.size()) {
                statusLabel.setText("Status: No record selected");
                return;
            }

            Integer id = animals.get(currentIndex).getId();
            if (id == null) {
                statusLabel.setText("Status: Selected record has no ID");
                return;
            }

            Animal updatedAnimal = buildAnimalFromFields(id);
            mysqlRepository.updateAnimal(updatedAnimal);
            reloadConnectedAnimals(id, "Record updated");
        } catch (IllegalArgumentException e) {
            statusLabel.setText("Status: " + e.getMessage());
        } catch (Exception e) {
            statusLabel.setText("Status: Update failed - " + e.getMessage());
        }
    }

    // Deletes the currently selected record after user confirmation.
    private void onDeleteClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            if (isInsertMode) {
                statusLabel.setText("Status: Finish add first or click Add to save new record");
                return;
            }

            List<Animal> animals = getActiveAnimals();
            if (currentIndex < 0 || currentIndex >= animals.size()) {
                statusLabel.setText("Status: No record selected");
                return;
            }

            Animal toDelete = animals.get(currentIndex);
            Integer id = toDelete.getId();
            if (id == null) {
                statusLabel.setText("Status: Selected record has no ID");
                return;
            }

            String type = toDelete instanceof Dog ? "Dog" : (toDelete instanceof Cat ? "Cat" : toDelete.getSpecies());
            String details = normalizeDetailsByType(type, displaySpecialField.getText());

            String message = "You are about to delete this record:\n\n"
                    + "Name: " + toDelete.getName() + "\n"
                    + "Age: " + toDelete.getAge() + "\n"
                    + "Type: " + type + "\n"
                    + "Details: " + details + "\n\n"
                    + "Delete this record?";

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
            reloadConnectedAnimals(null, "Record deleted");
        } catch (Exception e) {
            statusLabel.setText("Status: Delete failed - " + e.getMessage());
        }
    }

    // Ensures record actions only run when the app is connected.
    private boolean ensureConnectedForRecordActions(String disconnectedMessage) {
        if (isConnected) {
            return true;
        }

        clearRecordDisplay();
        statusLabel.setText("Status: " + disconnectedMessage);
        return false;
    }

    // Creates one labeled form row used in the record editor.
    private JPanel createRecordEntryRow(String labelText, JTextField textField) {
        JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        rowPanel.setPreferredSize(new Dimension(0, 38));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        textField.setPreferredSize(new Dimension(0, 30));
        textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(60, 24));

        rowPanel.add(label, BorderLayout.WEST);
        rowPanel.add(textField, BorderLayout.CENTER);
        return rowPanel;
    }

    // Toggles editability for record fields based on connection state.
    private void setRecordFieldsEditable(boolean editable) {
        displayNameField.setEditable(editable);
        displayAgeField.setEditable(editable);
        displayTypeField.setEditable(editable);
        displaySpecialField.setEditable(editable);
        displayIdField.setEditable(false);
    }

    // Clears all record fields and resets index metadata.
    private void clearRecordDisplay() {
        currentIndex = -1;
        displayIdField.setText("");
        displayNameField.setText("");
        displayAgeField.setText("");
        displayTypeField.setText("");
        displaySpecialField.setText("");
        recordCounterLabel.setText("Record: 0/0");
    }

    // Connects or disconnects from MySQL and refreshes the working record list.
    private void onConnectionToggleClicked() {
        try {
            if (isConnected) {
                isConnected = false;
                isInsertMode = false;
                insertButton.setText("Insert");
                connectionToggleButton.setText("Connect");
                setRecordFieldsEditable(false);
                connectedAnimals.clear();
                clearRecordDisplay();
                statusLabel.setText("Status: Disconnected");
                return;
            }

            if (mysqlRepository == null) {
                clearRecordDisplay();
                statusLabel.setText("Status: MySQL not configured (set DB_URL, DB_USER, DB_PASSWORD)");
                return;
            }

            mysqlRepository.ensureIdAutoIncrementPrimaryKey();
            connectedAnimals = new ArrayList<>(mysqlRepository.fetchAllAnimals());
            isConnected = true;
            isInsertMode = false;
            insertButton.setText("Insert");
            connectionToggleButton.setText("Disconnect");
            setRecordFieldsEditable(true);

            List<Animal> animals = getActiveAnimals();
            if (animals.isEmpty()) {
                clearRecordDisplay();
                statusLabel.setText("Status: Connected - no records");
            } else {
                int targetIndex = currentIndex < 0 ? 0 : currentIndex;
                showRecordAtIndex(targetIndex, "Connected");
            }
        } catch (Exception e) {
            isConnected = false;
            isInsertMode = false;
            insertButton.setText("Insert");
            connectionToggleButton.setText("Connect");
            setRecordFieldsEditable(false);
            connectedAnimals.clear();
            clearRecordDisplay();
            statusLabel.setText("Status: Error changing connection state - " + e.getMessage());
        }
    }

    // Applies in-form edits to the currently selected in-memory record.
    private boolean applyCurrentRecordEdits() {
        if (!isConnected) {
            return true;
        }

        List<Animal> animals = getActiveAnimals();
        if (currentIndex < 0 || currentIndex >= animals.size()) {
            return true;
        }

        try {
            Animal current = animals.get(currentIndex);
            String editedName = displayNameField.getText().trim();
            String editedAgeText = displayAgeField.getText().trim();
            String editedType = displayTypeField.getText().trim();
            String editedDetails = displaySpecialField.getText().trim();

            if (editedName.isEmpty()) {
                statusLabel.setText("Status: Name cannot be empty");
                return false;
            }

            int editedAge = Integer.parseInt(editedAgeText);

            current.setName(editedName);
            current.setAge(editedAge);

            if (!editedType.isEmpty()) {
                current.setSpecies(editedType);
            }

            if (current instanceof Dog) {
                String breed = editedDetails.replaceFirst("(?i)^breed\\s*:\\s*", "").trim();
                if (!breed.isEmpty()) {
                    ((Dog) current).setBreed(breed);
                }
            } else if (current instanceof Cat) {
                String color = editedDetails.replaceFirst("(?i)^color\\s*:\\s*", "").trim();
                if (!color.isEmpty()) {
                    ((Cat) current).setColor(color);
                }
            }

            return true;
        } catch (NumberFormatException e) {
            statusLabel.setText("Status: Age must be a number");
            return false;
        } catch (Exception e) {
            statusLabel.setText("Status: Error applying edits");
            return false;
        }
    }

    // Creates a consistently styled application button.
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        return button;
    }

    // Builds the application menu bar in a predictable order.
    private void createMenuBar() {
        try {
            JMenuBar menuBar = new JMenuBar();

            // Help menu.
            JMenu helpMenu = new JMenu("Help");
            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.addActionListener(e -> showAboutDialog());
            helpMenu.add(aboutItem);
            menuBar.add(helpMenu);

            // File menu.
            JMenu fileMenu = new JMenu("File");
            JMenuItem saveItem = new JMenuItem("Save Data");
            saveItem.addActionListener(e -> onSaveData());
            JMenuItem loadItem = new JMenuItem("Load Data");
            loadItem.addActionListener(e -> onLoadData());
            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));

            fileMenu.add(saveItem);
            fileMenu.add(loadItem);
            fileMenu.addSeparator();
            fileMenu.add(exitItem);
            menuBar.add(fileMenu);

            // Tools menu.
            JMenu toolsMenu = new JMenu("Tools");
            JMenuItem sortItem = new JMenuItem("Sort by Name");
            sortItem.addActionListener(e -> onSortByName());
            JMenuItem exportItem = new JMenuItem("Export Report");
            exportItem.addActionListener(e -> onExportReport());

            toolsMenu.add(sortItem);
            toolsMenu.add(exportItem);
            menuBar.add(toolsMenu);

            // Analytics menu.
            JMenu analyticsMenu = new JMenu("Analytics");
            JMenuItem statsItem = new JMenuItem("Show Statistics");
            statsItem.addActionListener(e -> onShowStatistics());
            JMenuItem ageAnalysisItem = new JMenuItem("Age Analysis");
            ageAnalysisItem.addActionListener(e -> onAgeAnalysis());

            analyticsMenu.add(statsItem);
            analyticsMenu.add(ageAnalysisItem);
            menuBar.add(analyticsMenu);

            // Utilities menu.
            JMenu utilitiesMenu = new JMenu("Utilities");
            JMenuItem validateItem = new JMenuItem("Validate Data");
            validateItem.addActionListener(e -> onValidateData());
            JMenuItem helpContentsItem = new JMenuItem("Help Contents");
            helpContentsItem.addActionListener(e -> showHelpDialog());

            utilitiesMenu.add(validateItem);
            utilitiesMenu.add(helpContentsItem);
            menuBar.add(utilitiesMenu);

            setJMenuBar(menuBar);
        } catch (Exception e) {
            System.err.println("Error creating menu bar: " + e.getMessage());
        }
    }

    // Sorts active animals alphabetically by name.
    private void onSortByName() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            List<Animal> animals = getActiveAnimals();
            if (animals.isEmpty()) {
                statusLabel.setText("Status: No animals to sort");
                return;
            }
            animals.sort((a, b) -> a.getName().compareTo(b.getName()));
            if (!animals.isEmpty()) {
                showRecordAtIndex(0, "Showing first sorted animal");
            }
            statusLabel.setText("Status: Animals sorted by name");
        } catch (Exception e) {
            statusLabel.setText("Status: Error sorting animals");
        }
    }

    // Computes and displays aggregate record statistics.
    private void onShowStatistics() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            List<Animal> animals = getActiveAnimals();
            int totalAnimals = animals.size();
            long dogCount = animals.stream().filter(a -> a instanceof Dog).count();
            long catCount = animals.stream().filter(a -> a instanceof Cat).count();
            double avgAge = animals.stream().mapToInt(Animal::getAge).average().orElse(0);

            displayNameField.setText("Statistics Report");
            displayAgeField.setText("Total: " + totalAnimals + " | Dogs: " + dogCount);
            displayTypeField.setText("Cats: " + catCount);
            displaySpecialField.setText("Avg Age: " + String.format("%.2f", avgAge) + " years");
            statusLabel.setText("Status: Statistics displayed");
        } catch (Exception e) {
            statusLabel.setText("Status: Error calculating statistics");
        }
    }

    // Exports a plain-text summary report to disk.
    private void onExportReport() {
        try {
            File file = new File("animal_report.txt");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ANIMAL MANAGEMENT SYSTEM REPORT\n");
                writer.write("================================\n\n");
                writer.write(container.getContainerInfo() + "\n\n");
                writer.write(container.displayAllAnimals());
                writer.write("\nReport generated successfully.\n");
            }
            statusLabel.setText("Status: Report exported to animal_report.txt");
            displayNameField.setText("Report Exported");
            displayTypeField.setText("File: animal_report.txt");
        } catch (Exception e) {
            statusLabel.setText("Status: Error exporting report");
        }
    }

    // Handles the welcome action and moves to the first record when connected.
    private void onHelloWorldButtonClicked() {
        clickCount++;
        try {
            counterLabel.setText("Click Count: " + clickCount);
            if (!isConnected) {
                clearRecordDisplay();
                statusLabel.setText("Status: Disconnected - click Connect to load MySQL records");
                return;
            }

            statusLabel.setText("Status: Welcome! Showing first record");
            showRecordAtIndex(0, "Initial record loaded");
        } catch (Exception e) {
            displayNameField.setText("Error: " + e.getMessage());
            statusLabel.setText("Status: Error");
        }
    }

    // Displays the record at the requested index with index wrapping.
    private void showRecordAtIndex(int index, String status) {
        if (!isConnected) {
            clearRecordDisplay();
            statusLabel.setText("Status: Disconnected");
            return;
        }

        List<Animal> animals = getActiveAnimals();
        if (animals.isEmpty()) {
            clearRecordDisplay();
            statusLabel.setText("Status: No records");
            return;
        }

        if (index < 0) {
            index = animals.size() - 1;
        } else if (index >= animals.size()) {
            index = 0;
        }

        currentIndex = index;
        Animal current = animals.get(currentIndex);
    displayIdField.setText(current.getId() == null ? "" : String.valueOf(current.getId()));
        displayNameField.setText(current.getName());
        displayAgeField.setText(String.valueOf(current.getAge()));
        
        recordCounterLabel.setText("Record: " + (currentIndex + 1) + " of " + animals.size());
        
        if (current instanceof Dog) {
            displayTypeField.setText("Dog");
            displaySpecialField.setText("Breed: " + ((Dog) current).getBreed());
        } else if (current instanceof Cat) {
            displayTypeField.setText("Cat");
            displaySpecialField.setText("Color: " + ((Cat) current).getColor());
        } else {
            displayTypeField.setText("Unknown");
            displaySpecialField.setText("Sound: " + current.makeSound());
        }
        
        statusLabel.setText("Status: " + status);
    }

    // Moves one record backward.
    private void onBackOneClicked() {
        try {
            if (!applyCurrentRecordEdits()) {
                return;
            }
            showRecordAtIndex(currentIndex - 1, "Moved back 1 record");
        } catch (Exception e) {
            statusLabel.setText("Status: Error navigating records");
        }
    }

    // Moves two records backward.
    private void onBackTwoClicked() {
        try {
            if (!applyCurrentRecordEdits()) {
                return;
            }
            showRecordAtIndex(currentIndex - 2, "Moved back 2 records");
        } catch (Exception e) {
            statusLabel.setText("Status: Error navigating records");
        }
    }

    // Moves one record forward.
    private void onForwardOneClicked() {
        try {
            if (!applyCurrentRecordEdits()) {
                return;
            }
            showRecordAtIndex(currentIndex + 1, "Moved forward 1 record");
        } catch (Exception e) {
            statusLabel.setText("Status: Error navigating records");
        }
    }

    // Moves two records forward.
    private void onForwardTwoClicked() {
        try {
            if (!applyCurrentRecordEdits()) {
                return;
            }
            showRecordAtIndex(currentIndex + 2, "Moved forward 2 records");
        } catch (Exception e) {
            statusLabel.setText("Status: Error navigating records");
        }
    }

    // Jumps to the first available record.
    private void onGoToStartClicked() {
        try {
            if (!applyCurrentRecordEdits()) {
                return;
            }
            showRecordAtIndex(0, "Moved to first record");
        } catch (Exception e) {
            statusLabel.setText("Status: Error navigating records");
        }
    }

    // Jumps to the last available record.
    private void onGoToEndClicked() {
        try {
            if (!applyCurrentRecordEdits()) {
                return;
            }
            List<Animal> animals = getActiveAnimals();
            if (!animals.isEmpty()) {
                showRecordAtIndex(animals.size() - 1, "Moved to last record");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error navigating records");
        }
    }

    // Searches active records by name and displays the first match.
    private void onSearchButtonClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            String searchName = displayNameField.getText().trim();
            if (searchName.isEmpty()) {
                statusLabel.setText("Status: Please enter an animal name");
                return;
            }

            List<Animal> animals = getActiveAnimals();
            int foundIndex = -1;
            for (int i = 0; i < animals.size(); i++) {
                if (animals.get(i).getName().equalsIgnoreCase(searchName)) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex >= 0) {
                showRecordAtIndex(foundIndex, "Animal found");
            } else {
                displayNameField.setText("Not found");
                statusLabel.setText("Status: Animal '" + searchName + "' not found in container.");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error searching animal");
        }
    }

    // Adds a new dog record using external input fields.
    private void onAddAnimalButtonClicked(JTextField animalNameField, JTextField animalAgeField) {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            String name = animalNameField.getText().trim();
            String ageStr = animalAgeField.getText().trim();

            if (name.isEmpty() || ageStr.isEmpty()) {
                statusLabel.setText("Status: Please enter name and age");
                return;
            }

            int age = Integer.parseInt(ageStr);
            Dog newDog = new Dog(name, age, "Mixed Breed");
            connectedAnimals.add(newDog);

            animalNameField.setText("");
            animalAgeField.setText("");

            currentIndex = connectedAnimals.size() - 1;
            showRecordAtIndex(currentIndex, "New animal added successfully");
        } catch (NumberFormatException e) {
            statusLabel.setText("Status: Age must be a number");
        } catch (Exception e) {
            statusLabel.setText("Status: Error adding animal");
        }
    }

    // Locates and shows the first dog record.
    private void onFilterDogsButtonClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            List<Animal> animals = getActiveAnimals();
            int firstDogIndex = -1;
            long dogCount = 0;
            for (int i = 0; i < animals.size(); i++) {
                if (animals.get(i) instanceof Dog) {
                    dogCount++;
                    if (firstDogIndex < 0) {
                        firstDogIndex = i;
                    }
                }
            }

            if (firstDogIndex >= 0) {
                showRecordAtIndex(firstDogIndex, "Showing first dog (" + dogCount + " total)");
            } else {
                displayNameField.setText("No dogs");
                statusLabel.setText("Status: No dogs found");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error filtering dogs");
        }
    }

    // Refreshes the current record view.
    private void onRefreshButtonClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            if (currentIndex < 0) {
                showRecordAtIndex(0, "Data refreshed");
            } else {
                showRecordAtIndex(currentIndex, "Data refreshed");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error refreshing data");
        }
    }

    // Saves a backup snapshot of current records.
    private void onSaveData() {
        try {
            File file = new File("animals_backup.dat");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(container.displayAllAnimals());
            }
            statusLabel.setText("Status: Data saved to animals_backup.dat");
            JOptionPane.showMessageDialog(this, "Data saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            statusLabel.setText("Status: Error saving data");
            JOptionPane.showMessageDialog(this, "Error saving data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Loads backup text content and updates the display state.
    private void onLoadData() {
        try {
            File file = new File("animals_backup.dat");
            if (file.exists()) {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                displayNameField.setText("Data Loaded");
                displayTypeField.setText("From: animals_backup.dat");
                statusLabel.setText("Status: Data loaded from animals_backup.dat");
                JOptionPane.showMessageDialog(this, "Data loaded successfully!", "Load", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Backup file not found!", "Load", JOptionPane.WARNING_MESSAGE);
                statusLabel.setText("Status: Backup file not found");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error loading data");
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Displays the application help dialog.
    private void showHelpDialog() {
        try {
            String helpText = "ANIMAL MANAGEMENT SYSTEM - HELP\n\n"
                    + "BUTTONS:\n"
                    + "- Hello World: Display container info with click counter\n"
                    + "- Previous/Next: Navigate through records\n"
                    + "- Search Animal: Find animal by name (enter name in search field)\n"
                    + "- Add Animal: Add new dog to container\n"
                    + "- Show Dogs: Filter and display all dogs\n"
                    + "- Clear Display: Clear the display area\n"
                    + "- Refresh Data: Reload current data\n\n"
                    + "MENU OPTIONS:\n"
                    + "- File: Save/Load data or exit application\n"
                    + "- Tools: Sort, statistics, and export functions\n"
                    + "- Help: Display this help menu\n"
                    + "- About: Application information\n";

            JTextArea helpArea = new JTextArea(helpText);
            helpArea.setEditable(false);
            helpArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            JScrollPane scrollPane = new JScrollPane(helpArea);
            scrollPane.setPreferredSize(new Dimension(500, 400));

            JOptionPane.showMessageDialog(this, scrollPane, "Help Contents", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println("Error showing help: " + e.getMessage());
        }
    }

    // Displays the application about dialog.
    private void showAboutDialog() {
        try {
            String aboutText = "ANIMAL MANAGEMENT SYSTEM\n\n"
                    + "Version: 2.0\n"
                    + "Date: January 2026\n\n"
                    + "A professional Java application demonstrating:\n"
                    + "- Object-Oriented Programming principles\n"
                    + "- Abstract classes and interfaces\n"
                    + "- Exception handling with try-catch\n"
                    + "- Java Swing GUI components\n"
                    + "- Data management and persistence\n\n"
                    + "Features:\n"
                    + "✓ Multiple interactive buttons\n"
                    + "✓ Text field input/output\n"
                    + "✓ Menu bar with file operations\n"
                    + "✓ Animal search and filtering\n"
                    + "✓ Data export and statistics\n"
                    + "✓ Professional error handling\n";

            JTextArea aboutArea = new JTextArea(aboutText);
            aboutArea.setEditable(false);
            aboutArea.setFont(new Font("Arial", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(aboutArea);
            scrollPane.setPreferredSize(new Dimension(450, 350));

            JOptionPane.showMessageDialog(this, scrollPane, "About Application", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            System.err.println("Error showing about dialog: " + e.getMessage());
        }
    }

    // Shows runtime system information in the display area.
    private void onShowInfoButtonClicked() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            StringBuilder info = new StringBuilder("=== System Information ===\n\n");
            info.append(container.getContainerInfo()).append("\n");
            info.append("Session Click Count: ").append(clickCount).append("\n\n");
            displayNameField.setText("System Info");
            displayTypeField.setText(String.valueOf(getActiveAnimals().size()) + " animals");

            if (currentIndex < 0) {
                showRecordAtIndex(0, "System information displayed");
            } else {
                showRecordAtIndex(currentIndex, "System information displayed");
            }
        } catch (Exception e) {
            statusLabel.setText("Status: Error displaying info");
        }
    }

    // Performs min/max/average age analysis on active records.
    private void onAgeAnalysis() {
        try {
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            List<Animal> animals = getActiveAnimals();
            if (animals.isEmpty()) {
                displayNameField.setText("No data");
                return;
            }

            int minAge = animals.stream().mapToInt(Animal::getAge).min().orElse(0);
            int maxAge = animals.stream().mapToInt(Animal::getAge).max().orElse(0);
            double avgAge = animals.stream().mapToInt(Animal::getAge).average().orElse(0);

            displayNameField.setText("Age Analysis Report");
            displayAgeField.setText("Min: " + minAge + ", Max: " + maxAge);
            displayTypeField.setText("Average: " + String.format("%.2f", avgAge));
            displaySpecialField.setText("Range: " + (maxAge - minAge));
            
            statusLabel.setText("Status: Age analysis completed");
        } catch (Exception e) {
            statusLabel.setText("Status: Error in age analysis");
        }
    }

    // Runs basic data quality checks across active records.
    private void onValidateData() {
        try {
            StringBuilder validation = new StringBuilder("=== Data Validation Report ===\n\n");
            if (!ensureConnectedForRecordActions("Disconnected - connect to MySQL")) {
                return;
            }

            List<Animal> animals = getActiveAnimals();
            int errors = 0;
            int warnings = 0;

            validation.append("Checking ").append(animals.size()).append(" animals...\n\n");

            for (Animal animal : animals) {
                if (animal.getName() == null || animal.getName().isEmpty()) {
                    validation.append("❌ ERROR: Animal has no name\n");
                    errors++;
                }
                if (animal.getAge() < 0 || animal.getAge() > 50) {
                    validation.append("⚠️  WARNING: Animal age ").append(animal.getAge())
                            .append(" is outside normal range\n");
                    warnings++;
                }
                if (animal.getName().length() > 50) {
                    validation.append("⚠️  WARNING: Animal name too long (").append(animal.getName().length())
                            .append(" chars)\n");
                    warnings++;
                }
            }

            validation.append("\n--- Validation Summary ---\n");
            validation.append("Total Errors: ").append(errors).append("\n");
            validation.append("Total Warnings: ").append(warnings).append("\n");
            if (errors == 0 && warnings == 0) {
                validation.append("✓ All data is valid!\n");
            }

            displayNameField.setText("Validation Report");
            displayTypeField.setText("Errors: " + errors);
            displayAgeField.setText("Warnings: " + warnings);
            displaySpecialField.setText("Status: " + (errors == 0 && warnings == 0 ? "Valid" : "Issues found"));
            
            statusLabel.setText("Status: Data validation complete - " + errors + " errors, " + warnings + " warnings");
        } catch (Exception e) {
            statusLabel.setText("Status: Error validating data");
        }
    }

    // Updates the display name field with arbitrary content.
    public void updateDisplay(String content) {
        displayNameField.setText(content);
    }
}
