package com.gameshowcenter.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimeLineSetup extends GameSetup {

    @Data
    public static class TimeLineEvent {
        private String id;
        private String title;
        private int year;
        private String description;
    }

    private List<TimeLineEvent> events;

    @Override
    public void validate(int numPlayers) {
        if (events == null || events.size() < 3) {
            throw new IllegalArgumentException("TimeLine: Se requieren al menos 3 hitos en la línea de tiempo.");
        }
        for (TimeLineEvent ev : events) {
            if (ev.getTitle() == null || ev.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("TimeLine: Todos los hitos deben incluir un título.");
            }
        }
    }
}
