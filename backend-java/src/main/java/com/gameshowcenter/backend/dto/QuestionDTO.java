package com.gameshowcenter.backend.dto;

import lombok.Data;

@Data
public class QuestionDTO {
    private String question;
    private String answer;
    private Integer difficulty; // Opcional, lo usaremos también en Topic Takedown
    private String imageUrl;    // Opcional: Imagen adjunta a la pregunta
    private String audioUrl;    // Opcional: Audio MP3 adjunto a la pregunta
}