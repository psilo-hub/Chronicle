package free.svoss.rpg.app;

import free.svoss.rpg.models.AppConfig;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

public class ThoughtLogWindow extends JFrame {
    private JTextArea logArea;

    public ThoughtLogWindow(AppConfig config) {
        setTitle(config.nameAi+"'s Internal Monologue");
        setSize(400, 300);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30)); // Dark mode for logs
        logArea.setForeground(new Color(0, 255, 100)); // Neon green text
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Auto-scroll to bottom
        DefaultCaret caret = (DefaultCaret) logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        add(new JScrollPane(logArea), BorderLayout.CENTER);
    }

    public void addThought(String thought) {
        logArea.append("> THOUGHT: " + thought + "\n\n");
    }
}