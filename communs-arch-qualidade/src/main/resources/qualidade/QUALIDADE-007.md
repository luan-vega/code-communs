# QUALIDADE-007 SonarCloud com organização

**Severidade:** erro

## O que verifica
Quando `sonar.host.url` aponta para o `sonarcloud.io`, `sonar.organization` está preenchido.

## Por quê
No SonarCloud todo projeto pertence a uma organização. Sem ela a análise falha no CI com um erro de projeto não encontrado ou sem permissão.

## Errado
```properties
sonar.host.url=https://sonarcloud.io
sonar.projectKey=luan-vega_pedidos
```

## Certo
```properties
sonar.host.url=https://sonarcloud.io
sonar.organization=luan-vega
sonar.projectKey=luan-vega_pedidos
```
