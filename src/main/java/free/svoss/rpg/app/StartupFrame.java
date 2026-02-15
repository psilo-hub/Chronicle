package free.svoss.rpg.app;

import free.svoss.rpg.llm.OllamaClient;
import free.svoss.rpg.models.AppConfig;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * The entry point of the application.
 * Allows users to configure the LLM and resource paths before starting the game.
 */
public class StartupFrame extends JFrame {
    private JComboBox<String> modelCombo = new JComboBox<>();
    private JTextField luceneField = new JTextField("memory_index");
    private JTextField schemaField = new JTextField("");
    private JTextField generalPromptField = new JTextField("");
    private JTextField charPromptField = new JTextField("");
    private JTextField imagesField = new JTextField("");
    private JTextField playerNameField = new JTextField("");

    public StartupFrame() {
        setTitle("Chronicle Configuration");
        setSize(550, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Center Panel for Form
        JPanel formPanel = new JPanel(new GridLayout(9, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Fetch models from Ollama
        loadModels();

        formPanel.add(new JLabel(" Select Chat Model:"));
        formPanel.add(modelCombo);

        formPanel.add(new JLabel(" Lucene Storage Path:"));
        formPanel.add(luceneField);

        formPanel.add(new JLabel(" External Schema (Optional):"));
        formPanel.add(schemaField);

        formPanel.add(new JLabel(" External General Prompt:"));
        formPanel.add(generalPromptField);

        formPanel.add(new JLabel(" External Character Prompt:"));
        formPanel.add(charPromptField);

        formPanel.add(new JLabel(" External Images Folder:"));
        formPanel.add(imagesField);

        formPanel.add(new JLabel(" Player Name:"));
        formPanel.add(playerNameField);

        // Bottom Panel for Launch Button
        JButton startButton = new JButton("Launch Game");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(0, 50));

        startButton.addActionListener(e -> {
            AppConfig config = new AppConfig(
                    (String) modelCombo.getSelectedItem(),
                    luceneField.getText(),
                    schemaField.getText().isEmpty() ? null : schemaField.getText(),
                    generalPromptField.getText().isEmpty() ? null : generalPromptField.getText(),
                    charPromptField.getText().isEmpty() ? null : charPromptField.getText(),
                    imagesField.getText().isEmpty() ? null : imagesField.getText(),
                    playerNameField.getText().trim().isEmpty()?"Player":playerNameField.getText().trim()
            );

            // Close this frame and launch the main app
            this.dispose();
            SwingUtilities.invokeLater(() -> new Main(config).setVisible(true));
        });

        add(formPanel, BorderLayout.CENTER);
        add(startButton, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    /**
     * Attempts to reach Ollama via the API to list local models.
     */
    private void loadModels() {
        try {
            OllamaClient tempClient = new OllamaClient(null);
            List<String> models = tempClient.getInstalledModels();
            if (models.isEmpty()) {
                modelCombo.addItem("llama3");
            } else {
                for (String m : models) {
                    modelCombo.addItem(m);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not connect to Ollama to fetch models. Falling back to default.");
            modelCombo.addItem("llama3");
        }
    }

    /**
     * The main entry point of the entire Java project.
     */
    public static void main(String[] args) {
        // Set Look and Feel to System default for a better UI experience
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new StartupFrame().setVisible(true);
        });
    }
}