package io.github.luanvega.communs.arch.qualidade;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Lê o {@code pom.xml} e o {@code sonar-project.properties} de um diretório. Só olha o próprio arquivo:
 * o que vem de poms pais (fora o {@code communs-qualidade-parent}) não é resolvido.
 */
public final class LeitorDeProjeto {

    static final String POM = "pom.xml";
    static final String SONAR_PROPERTIES = "sonar-project.properties";

    private static final String GRUPO_PADRAO_DE_PLUGINS = "org.apache.maven.plugins";

    private LeitorDeProjeto() {
    }

    public static ProjetoMaven ler(Path diretorio) {
        if (!Files.isDirectory(diretorio)) {
            throw new IllegalArgumentException("Diretório do projeto não existe: " + diretorio);
        }
        Map<String, Propriedade> propriedades = new LinkedHashMap<>();
        List<PluginMaven> plugins = new ArrayList<>();
        Optional<String> pai = Optional.empty();
        Path pom = diretorio.resolve(POM);
        boolean temPom = Files.isRegularFile(pom);
        if (temPom) {
            Element projeto = lerXml(pom).getDocumentElement();
            pai = filho(projeto, "parent").map(parent -> texto(parent, "groupId") + ":" + texto(parent, "artifactId"));
            for (Element bloco : elementos(projeto, "properties")) {
                // Só <properties> do projeto e de perfis; plugins também têm <properties> na configuração.
                if (!(bloco.getParentNode() instanceof Element dono && (dono == projeto || dono.getTagName().equals("profile")))) {
                    continue;
                }
                for (Element propriedade : filhos(bloco)) {
                    propriedades.putIfAbsent(propriedade.getTagName(),
                            new Propriedade(propriedade.getTagName(), propriedade.getTextContent().trim(), POM));
                }
            }
            for (Element plugin : elementos(projeto, "plugin")) {
                if (plugin.getParentNode() instanceof Element lista && lista.getTagName().equals("plugins")) {
                    plugins.add(plugin(plugin));
                }
            }
        }
        Path sonar = diretorio.resolve(SONAR_PROPERTIES);
        if (Files.isRegularFile(sonar)) {
            Properties arquivo = new Properties();
            try (Reader reader = Files.newBufferedReader(sonar, StandardCharsets.UTF_8)) {
                arquivo.load(reader);
            } catch (IOException e) {
                throw new UncheckedIOException("Falha ao ler " + sonar, e);
            }
            arquivo.stringPropertyNames().forEach(chave ->
                    propriedades.put(chave, new Propriedade(chave, arquivo.getProperty(chave).trim(), SONAR_PROPERTIES)));
        }
        return new ProjetoMaven(diretorio, temPom, pai, propriedades, plugins);
    }

    private static PluginMaven plugin(Element plugin) {
        String groupId = filho(plugin, "groupId").map(e -> e.getTextContent().trim()).orElse(GRUPO_PADRAO_DE_PLUGINS);
        Set<String> goals = new LinkedHashSet<>();
        elementos(plugin, "goal").forEach(goal -> goals.add(goal.getTextContent().trim()));
        Set<String> dependencias = new LinkedHashSet<>();
        elementos(plugin, "dependency").forEach(dep -> dependencias.add(texto(dep, "groupId") + ":" + texto(dep, "artifactId")));
        Map<String, String> configuracao = new LinkedHashMap<>();
        for (Element bloco : elementos(plugin, "configuration")) {
            for (Element item : filhos(bloco)) {
                if (filhos(item).isEmpty()) {
                    configuracao.putIfAbsent(item.getTagName(), item.getTextContent().trim());
                }
            }
        }
        return new PluginMaven(groupId, texto(plugin, "artifactId"), goals, dependencias, configuracao);
    }

    private static Document lerXml(Path arquivo) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            // O pom vem do projeto analisado: nada de DTD nem entidades externas.
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            fabrica.setExpandEntityReferences(false);
            DocumentBuilder construtor = fabrica.newDocumentBuilder();
            return construtor.parse(arquivo.toFile());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalArgumentException("pom.xml inválido em " + arquivo + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + arquivo, e);
        }
    }

    private static List<Element> elementos(Element raiz, String nome) {
        NodeList nos = raiz.getElementsByTagName(nome);
        List<Element> resultado = new ArrayList<>(nos.getLength());
        for (int i = 0; i < nos.getLength(); i++) {
            resultado.add((Element) nos.item(i));
        }
        return resultado;
    }

    private static List<Element> filhos(Element pai) {
        List<Element> resultado = new ArrayList<>();
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element elemento) {
                resultado.add(elemento);
            }
        }
        return resultado;
    }

    private static Optional<Element> filho(Element pai, String nome) {
        return filhos(pai).stream().filter(e -> e.getTagName().equals(nome)).findFirst();
    }

    private static String texto(Element pai, String nome) {
        return filho(pai, nome).map(e -> e.getTextContent().trim()).orElse("");
    }
}
