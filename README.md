# communs-arch

Regras de arquitetura testáveis para projetos Java, recriadas a partir do [ArchUnit](https://www.archunit.org/) com alterações para necessidades internas.

Requisitos: Java 21 e Maven 3.9+.

```bash
mvn verify
```

## Módulos

| Módulo | Responsabilidade |
| --- | --- |
| `communs-arch-core` | Importador de bytecode (ASM) e modelo de classes, membros, dependências e anotações |
| `communs-arch-lang` | DSL fluente `classes().that()...should()...` e avaliação de regras |
| `communs-arch-library` | Regras padrão e o catálogo `RegrasRegistry` |
| `communs-arch-junit` | Integração com JUnit 5 (`@AnalisarClasses`, `@RegraArch`) |
| `communs-arch-runner` | CLI que roda o catálogo num projeto e gera relatório JSON |

Cada módulo só depende dos que estão acima dele na tabela. Regras alternativas (internas) e a integração com chat entram em fases posteriores.
