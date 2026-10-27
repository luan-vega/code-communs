package io.github.luanvega.communs.arch.lang;

import java.util.ArrayList;
import java.util.List;

/**
 * Coleta os eventos que as condições geram ao verificar um item.
 */
public final class ConditionEvents {

    private final List<ConditionEvent> events = new ArrayList<>();

    public void add(ConditionEvent event) {
        events.add(event);
    }

    public List<ConditionEvent> all() {
        return List.copyOf(events);
    }

    public List<ConditionEvent> violations() {
        return events.stream().filter(ConditionEvent::violation).toList();
    }

    public boolean containViolation() {
        return events.stream().anyMatch(ConditionEvent::violation);
    }
}
