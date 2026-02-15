package free.svoss.rpg.util;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ResourceLoader {

    public static String loadText(String path, String internalName) {
        if (path != null && new File(path).exists()) {
            try {
                return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
            } catch (IOException e) { e.printStackTrace(); }
        }

        try (InputStream is = ResourceLoader.class.getResourceAsStream("/" + internalName)) {
            if (is == null) return "Error: Internal resource " + internalName + " missing.";
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        } catch (Exception e) { return ""; }
    }

    public static ImageIcon loadImage(String folderPath, String expression) {
        // 1. Try external first
        if (folderPath != null && !folderPath.isEmpty()) {
            File file = new File(folderPath, expression.toLowerCase() + ".jpg");
            if (file.exists()) {
                return new ImageIcon(file.getAbsolutePath());
            }
        }

        // 2. Fallback to internal resources
        String internalPath = "/images/" + expression.toLowerCase() + ".jpg";
        URL imgURL = ResourceLoader.class.getResource(internalPath);

        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Resource not found: " + internalPath);
            return null;
        }
    }
}