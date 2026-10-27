package io.github.luanvega.communs.arch.runner;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Escritor de JSON mínimo para o relatório (mapas, listas, textos, números, booleanos e null),
 * para o runner não depender de uma biblioteca de JSON. A saída é sempre ASCII.
 */
final class Json {

    private Json() {
    }

    static String escrever(Object valor) {
        StringBuilder saida = new StringBuilder();
        escrever(valor, saida, 0);
        return saida.append('\n').toString();
    }

    private static void escrever(Object valor, StringBuilder saida, int nivel) {
        switch (valor) {
            case null -> saida.append("null");
            case String texto -> texto(texto, saida);
            case Number numero -> saida.append(numero);
            case Boolean booleano -> saida.append(booleano);
            case Map<?, ?> mapa -> objeto(mapa, saida, nivel);
            case Collection<?> lista -> lista(lista, saida, nivel);
            default -> throw new IllegalArgumentException("Tipo não suportado no JSON: " + valor.getClass());
        }
    }

    private static void objeto(Map<?, ?> mapa, StringBuilder saida, int nivel) {
        if (mapa.isEmpty()) {
            saida.append("{}");
            return;
        }
        saida.append("{\n");
        Iterator<? extends Map.Entry<?, ?>> itens = mapa.entrySet().iterator();
        while (itens.hasNext()) {
            Map.Entry<?, ?> item = itens.next();
            recuo(saida, nivel + 1);
            texto(String.valueOf(item.getKey()), saida);
            saida.append(": ");
            escrever(item.getValue(), saida, nivel + 1);
            saida.append(itens.hasNext() ? ",\n" : "\n");
        }
        recuo(saida, nivel);
        saida.append('}');
    }

    private static void lista(Collection<?> lista, StringBuilder saida, int nivel) {
        if (lista.isEmpty()) {
            saida.append("[]");
            return;
        }
        saida.append("[\n");
        Iterator<?> itens = lista.iterator();
        while (itens.hasNext()) {
            recuo(saida, nivel + 1);
            escrever(itens.next(), saida, nivel + 1);
            saida.append(itens.hasNext() ? ",\n" : "\n");
        }
        recuo(saida, nivel);
        saida.append(']');
    }

    private static void texto(String texto, StringBuilder saida) {
        saida.append('"');
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                default -> {
                    // Fora do ASCII sai como escape unicode: o JSON fica correto mesmo num terminal que não está em UTF-8.
                    if (c < 0x20 || c > 0x7e) {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        saida.append('"');
    }

    private static void recuo(StringBuilder saida, int nivel) {
        saida.append("  ".repeat(nivel));
    }
}
