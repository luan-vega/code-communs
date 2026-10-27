package io.github.luanvega.communs.arch.lang;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Predicado com uma descrição legível, usada para montar o texto das regras
 * (ex.: "reside in a package '..service..'").
 */
public abstract class DescribedPredicate<T> implements Predicate<T> {

    private final String description;

    protected DescribedPredicate(String description) {
        this.description = Objects.requireNonNull(description, "description");
    }

    public static <T> DescribedPredicate<T> describe(String description, Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        return new DescribedPredicate<>(description) {
            @Override
            public boolean test(T value) {
                return predicate.test(value);
            }
        };
    }

    public static <T> DescribedPredicate<T> not(DescribedPredicate<? super T> predicate) {
        return describe("not " + predicate.description(), value -> !predicate.test(value));
    }

    public String description() {
        return description;
    }

    public DescribedPredicate<T> as(String newDescription) {
        return describe(newDescription, this);
    }

    public DescribedPredicate<T> and(DescribedPredicate<? super T> other) {
        return describe(description + " and " + other.description(), value -> test(value) && other.test(value));
    }

    public DescribedPredicate<T> or(DescribedPredicate<? super T> other) {
        return describe(description + " or " + other.description(), value -> test(value) || other.test(value));
    }

    @Override
    public String toString() {
        return description;
    }
}
