package free.svoss.rpg.app;

import free.svoss.rpg.llm.OllamaClient;
import free.svoss.rpg.models.AppConfig;
import free.svoss.rpg.models.CharacterResponse;
import free.svoss.rpg.rag.MemoryVault;
import free.svoss.rpg.util.ResourceLoader;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class Main extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JLabel portraitLabel;
    private ThoughtLogWindow thoughtLog;
    private GameEngine engine;
    private AppConfig config;

    public Main(AppConfig config) {
        this.config = config;
         setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);       // Wrap lines when they hit the edge
        chatArea.setWrapStyleWord(true);  // Wrap at word boundaries (whitespace)

        portraitLabel = new JLabel(ResourceLoader.loadImage(config.imagesPath, "neutral"));

        inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendButton, BorderLayout.EAST);

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        add(portraitLabel, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);

        try {
            OllamaClient client = new OllamaClient(config); // Updated to take config
            MemoryVault vault = new MemoryVault(config.lucenePath);
            engine = new GameEngine(client, vault, config);
        } catch (Exception e) {
            log.error("Failed to initialize game engine : {}", e.getMessage());
            e.printStackTrace();
        }
        setTitle("Chronicle: Conversation between "+config.namePlayer+" and "+config.nameAi+" (" + config.modelName+")");
        thoughtLog = new ThoughtLogWindow(config);
        thoughtLog.setVisible(true);

        sendButton.addActionListener(e -> handleSend());
        inputField.addActionListener(e -> handleSend());
    }

    private void handleSend() {
        String text = inputField.getText();
        String player = (config.namePlayer==null||config.namePlayer.trim().isEmpty())?"You":(config.namePlayer.trim()+" (You)");

        chatArea.append(player+": " + text + "\n");
        inputField.setText("");

        new SwingWorker<CharacterResponse, Void>() {
            protected CharacterResponse doInBackground() throws Exception {
                return engine.processTurn(text);
            }
            protected void done() {
                try {
                    CharacterResponse res = get();
                    String ai=config.nameAi.equalsIgnoreCase("ai")?"AI":(config.nameAi+" (AI)");
                    chatArea.append(ai+": " + res.getDialogue() + "\n\n");
                    thoughtLog.addThought(res.getThought());
                    portraitLabel.setIcon(ResourceLoader.loadImage(config.imagesPath, res.getExpression()));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }
}