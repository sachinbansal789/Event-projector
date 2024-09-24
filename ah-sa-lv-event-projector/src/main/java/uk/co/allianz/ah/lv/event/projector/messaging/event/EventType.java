package uk.co.allianz.ah.lv.event.projector.messaging.event;

import java.util.Arrays;

public enum EventType {

    AI_RESPONSE("AI_RESPONSE"),
    NO_MATCH("no-match");

    private final String value;

    EventType(final String value) {
        this.value = value;
    }

    public static EventType find(final String text) {
        return Arrays.asList(EventType.values()).stream()
                .filter(v -> v.value.equalsIgnoreCase(text))
                .findFirst().orElse(NO_MATCH);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value();
    }


}
