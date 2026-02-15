package free.svoss.rpg.models;

public class AppConfig {
    public String modelName;
    public String lucenePath;
    public String schemaPath;       // null if internal
    public String generalPromptPath; // null if internal
    public String charPromptPath;    // null if internal
    public String imagesPath;       // null if internal (directory path)
    public String nameAi=null;
    public String namePlayer=null;


    public AppConfig(String modelName, String lucenePath, String schemaPath,
                     String generalPromptPath, String charPromptPath, String imagesPath,String namePlayer) {
        this.modelName = modelName;
        this.lucenePath = lucenePath;
        this.schemaPath = schemaPath;
        this.generalPromptPath = generalPromptPath;
        this.charPromptPath = charPromptPath;
        this.imagesPath = imagesPath;
        this.namePlayer=namePlayer;
    }
}