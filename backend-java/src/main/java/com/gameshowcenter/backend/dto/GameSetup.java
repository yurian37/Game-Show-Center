package com.gameshowcenter.backend.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, 
    include = JsonTypeInfo.As.PROPERTY, 
    property = "game"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RouletteSetup.class, name = "Roulette"),
    @JsonSubTypes.Type(value = RouletteSetup.class, name = "Roulette_Game"),
    @JsonSubTypes.Type(value = RouletteSetup.class, name = "Flip_Coin"),
    @JsonSubTypes.Type(value = RouletteSetup.class, name = "Flip Coin"),
    @JsonSubTypes.Type(value = HangmanSetup.class, name = "Hangman"),
    @JsonSubTypes.Type(value = TicTacToeSetup.class, name = "TicTacToe"),
    @JsonSubTypes.Type(value = TriviaQuizSetup.class, name = "Trivia_Quiz"),
    @JsonSubTypes.Type(value = ZeroMarginSetup.class, name = "Zero_Margin"),
    @JsonSubTypes.Type(value = GeoLocationSetup.class, name = "GeoLocation"),
    @JsonSubTypes.Type(value = MediaPoolSetup.class, name = "Rapid_Rhythm"),
    @JsonSubTypes.Type(value = MediaPoolSetup.class, name = "Guess_Character"),
    @JsonSubTypes.Type(value = SnapSolveSetup.class, name = "Snap_Solve"),
    @JsonSubTypes.Type(value = SnapSolveSetup.class, name = "Snap Solve"),
    @JsonSubTypes.Type(value = TopicTakedownSetup.class, name = "Topic_Takedown"),
    @JsonSubTypes.Type(value = TimeLineSetup.class, name = "TimeLine"),
    @JsonSubTypes.Type(value = TimeLineSetup.class, name = "Time_Line")
})
public abstract class GameSetup {
    
    // Todas las clases hijas están obligadas a usar este método
    public abstract void validate(int numPlayers);
}