package io.github.luanvega.communs.arch.lang;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Condição que cada item selecionado por uma regra deve atender.
 * Ao verificar um item, a condição registra eventos atendidos e violados; registrar os atendidos
 * permite inverter a condição em {@code noClasses()} com mensagens úteis.
 */
public abstract class ArchCondition<T> {

    private final String description;

    protected ArchCondition(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public static <T> ArchCondition<T> from(String description, BiConsumer<? super T, ConditionEvents> check) {
        Objects.requireNonNull(check, "check");
        return new ArchCondition<>(description) {
            @Override
            public void check(T item, ConditionEvents events) {
                check.accept(item, events);
            }
        };
    }

    public abstract void check(T item, ConditionEvents events);

    public String description() {
        return description;
    }

    public ArchCondition<T> as(String newDescription) {
        return from(newDescription, this::check);
    }

    /**
     * Condição atendida quando esta não é: eventos atendidos viram violações e vice-versa.
     */
    public ArchCondition<T> negate() {
        ArchCondition<T> original = this;
        return from("not " + description, (item, events) -> {
            ConditionEvents originalEvents = new ConditionEvents();
            original.check(item, originalEvents);
            originalEvents.all().forEach(event -> events.add(event.inverted()));
        });
    }

    public ArchCondition<T> and(ArchCondition<? super T> other) {
        ArchCondition<T> first = this;
        return from(description + " and " + other.description(), (item, events) -> {
            first.check(item, events);
            other.check(item, events);
        });
    }

    /**
     * Atendida se ao menos uma das condições for atendida; quando ambas falham, reporta as violações das duas.
     */
    public ArchCondition<T> or(ArchCondition<? super T> other) {
        ArchCondition<T> first = this;
        return from(description + " or " + other.description(), (item, events) -> {
            ConditionEvents firstEvents = new ConditionEvents();
            ConditionEvents otherEvents = new ConditionEvents();
            first.check(item, firstEvents);
            other.check(item, otherEvents);
            if (!firstEvents.containViolation() || !otherEvents.containViolation()) {
                List<ConditionEvent> satisfied = !firstEvents.containViolation() ? firstEvents.all() : otherEvents.all();
                satisfied.forEach(events::add);
            } else {
                firstEvents.all().forEach(events::add);
                otherEvents.all().forEach(events::add);
            }
        });
    }

    @Override
    public String toString() {
        return description;
    }
}
