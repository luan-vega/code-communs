# QUALIDADE-001 Sem credenciais do Sonar em arquivos

**Severidade:** erro

## O que verifica
`sonar.token`, `sonar.login` e `sonar.password` não aparecem com valor fixo no `pom.xml` nem no `sonar-project.properties`. Referências como `${env.SONAR_TOKEN}` são aceitas.

## Por quê
O token vai junto com o código para o Git e dá acesso ao servidor do Sonar a quem ler o repositório. Um token vazado precisa ser revogado.

## Errado
```xml
<properties>
    <sonar.token>squ_3f9a...</sonar.token>
</properties>
```

## Certo
Guarde o token como segredo do CI e passe pela variável de ambiente, que o scanner lê sozinho:
```yaml
- run: mvn -B verify sonar:sonar
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```
