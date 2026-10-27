package io.github.luanvega.communs.arch.lang;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Casa nomes de pacote com padrões no estilo do ArchUnit:
 * <ul>
 *     <li>{@code ..} representa qualquer quantidade de pacotes, inclusive nenhum
 *         ({@code ..service..} casa {@code service}, {@code com.app.service} e {@code com.app.service.impl});</li>
 *     <li>{@code *} representa um trecho qualquer de um único nome de pacote ({@code com.*.api});</li>
 *     <li>o restante é comparado literalmente.</li>
 * </ul>
 */
public final class PackageMatcher {

    private final String pattern;
    private final Pattern regex;

    private PackageMatcher(String pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
        if (pattern.contains("...")) {
            throw new IllegalArgumentException("Padrão de pacote inválido (use '..', não '...'): " + pattern);
        }
        this.regex = Pattern.compile(toRegex(pattern));
    }

    public static PackageMatcher of(String pattern) {
        return new PackageMatcher(pattern);
    }

    public boolean matches(String packageName) {
        return regex.matcher(packageName).matches();
    }

    public String pattern() {
        return pattern;
    }

    private static String toRegex(String pattern) {
        if (pattern.equals("..")) {
            return ".*";
        }
        StringBuilder regex = new StringBuilder();
        String[] parts = pattern.split("\\.\\.", -1);
        for (int i = 0; i < parts.length; i++) {
            boolean first = i == 0;
            boolean last = i == parts.length - 1;
            String part = parts[i];
            if (!first) {
                // ".." entre dois trechos: zero ou mais pacotes no meio.
                boolean previousEmpty = parts[i - 1].isEmpty();
                if (previousEmpty && i == 1) {
                    regex.append("(?:.*\\.)?");
                } else if (part.isEmpty() && last) {
                    regex.append("(?:\\..*)?");
                } else {
                    regex.append("(?:\\..*)?\\.");
                }
            }
            regex.append(literal(part));
        }
        return regex.toString();
    }

    private static String literal(String part) {
        StringBuilder literal = new StringBuilder();
        for (char c : part.toCharArray()) {
            switch (c) {
                case '*' -> literal.append("[^.]*");
                case '.' -> literal.append("\\.");
                default -> literal.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return literal.toString();
    }

    @Override
    public String toString() {
        return "'" + pattern + "'";
    }
}
