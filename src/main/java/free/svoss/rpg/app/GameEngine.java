package free.svoss.rpg.app;

import free.svoss.rpg.llm.OllamaClient;
import free.svoss.rpg.models.AppConfig;
import free.svoss.rpg.models.CharacterResponse;
import free.svoss.rpg.rag.MemoryVault;
import free.svoss.rpg.util.ResourceLoader;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class GameEngine {
    private final OllamaClient ollamaClient;
    private final MemoryVault memoryVault;
    private final String combinedSystemPrompt;
    private final AppConfig config;

    public GameEngine(OllamaClient ollamaClient, MemoryVault memoryVault, AppConfig config) {
        this.ollamaClient = ollamaClient;
        this.memoryVault = memoryVault;
        this.config=config;

        String general = ResourceLoader.loadText(config.generalPromptPath, "general_rules.txt");
        String character = ResourceLoader.loadText(config.charPromptPath, "character_backstory.txt");
        parseAiCharacterName(config,character);

        this.combinedSystemPrompt = general + "\n\n" + character + "\n\nRelevant Memories:\n{RAG_CONTEXT}";
    }

    private static void parseAiCharacterName(AppConfig config, String character) {
        log.info("Parsing Ai name");
        if (character == null) config.nameAi = "Ai";
        else {
            String[] lines = character.split("\n");
            String nameAi=null;
            for(String line : lines)if(nameAi==null&&line.toLowerCase(Locale.ROOT).startsWith("name:"))
                nameAi=line.substring(5).trim();

            config.nameAi=nameAi==null?"Ai":nameAi;
        }
        log.info("Ai name : {}", config.nameAi);
    }
    private final LinkedList<String> rollingMessageLog=new LinkedList<>();
    private final static int rollingMessageLogLength=3;
    public CharacterResponse processTurn(String userInput) throws Exception {
        //log.info("userInput : {}", userInput);
        float[] queryVector = ollamaClient.getEmbedding(userInput);
        List<String> memories = memoryVault.findRelevantMemories(queryVector, 4);
        String ragContext = String.join("\n---\n", memories);
        //log.info("\n--------------------\nragContext length: {}", ragContext.length());

        String finalPrompt = combinedSystemPrompt.replace("{RAG_CONTEXT}", ragContext);
        CharacterResponse characterResponse= ollamaClient.chat(finalPrompt, userInput);
        //todo store in vault (maybe including timestamp?)
        //log.info("aiOutput : {}", characterResponse.getDialogue());
        appendToLogAndStorInVault(userInput,characterResponse.getDialogue());
        return characterResponse;
    }

    private void appendToLogAndStorInVault(String userInput, String dialogue) throws Exception {
        String combinedDialog = combineDialog(userInput,dialogue);
        log.info("combined dialog :\n{}", combinedDialog);
        rollingMessageLog.add(combinedDialog);
        if(rollingMessageLog.size()>rollingMessageLogLength)rollingMessageLog.removeFirst();
        String documentForVault=getMsgLogForVault();
        float[] embedding= ollamaClient.getEmbedding(documentForVault);
        memoryVault.addMemory(documentForVault,embedding);
    }

    private String getMsgLogForVault() {
        StringBuilder sb = new StringBuilder();
        for(String msg : rollingMessageLog)
            sb.append(msg).append("\n\n");

        return sb.toString().trim();
    }

    private String combineDialog(String userInput, String dialogue) {
        return getTimeStamp() + "\n" + getPlayerName() + ": " + userInput + "\n\n" + getAiNameForVault() + ": " + dialogue;
    }

    private String getAiNameForVault() {
        if(config.nameAi!=null&&!config.nameAi.trim().isEmpty())return config.nameAi+" (you)";
        else return "you";
    }

    private String getPlayerName() {
        if(config.namePlayer!=null&&!config.namePlayer.trim().isEmpty())return config.namePlayer;
        else return "player";
    }

    private final static SimpleDateFormat sdf = new SimpleDateFormat("EEEE LLLL dd, yyyy; HH:mm:ss");
    private String getTimeStamp() {
        return sdf.format(new Date());
    }
}