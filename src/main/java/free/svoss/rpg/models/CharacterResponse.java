package free.svoss.rpg.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CharacterResponse {
    private String thought;
    private String expression;
    private String dialogue;

    // Default constructor for Jackson
    public CharacterResponse() {}

    @JsonProperty("thought")
    public String getThought() { return thought; }

    @JsonProperty("expression")
    public String getExpression() { return expression; }

    @JsonProperty("dialogue")
    public String getDialogue() { return dialogue; }

}