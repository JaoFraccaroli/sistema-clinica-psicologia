# Sistema para Clínica de Psicologia

![Testes](https://github.com/JaoFraccaroli/sistema-clinica-psicologia/actions/workflows/testes.yml/badge.svg)

Sistema web para uma clínica de psicologia com **área clínica e área financeira separadas**, usado como projeto de **Qualidade de Software em Java**: testes de unidade, BDD, valores de fronteira, testes dirigidos por dados, cobertura e testes de mutação, tudo rodando no GitHub Actions a cada push.

| Testes | Cobertura de linhas (JaCoCo) | Mutation score (PIT) |
|---|---|---|
| 107, todos passando | 87,1% (mínimo exigido: 85%) | 100%, 53 de 53 mutantes mortos (mínimo exigido: 90%) |

## Por que separar?

Dados de saúde são **dados pessoais sensíveis** pela LGPD, e o conteúdo dos atendimentos é sigiloso. Ao mesmo tempo, muitos psicólogos preferem que outra pessoa cuide das cobranças. Se o sistema mistura tudo, não dá para entregar o financeiro a outra pessoa sem expor o prontuário.

Aqui a separação é feita no próprio sistema:

| | Área clínica | Área financeira |
|---|---|---|
| Quem acessa | Psicóloga | Quem cuida do financeiro (completo) e a psicóloga (só consulta) |
| O que tem | Pacientes, sessões e anotações (prontuário) | Cobranças e pagamentos |
| Como identifica o paciente | Nome e dados completos | Apenas um **código** (ex.: `PAC-1A2B3C4D`) e os dados de quem paga |

### Permissões de cada perfil

| Ação | `psicologa` | `financeiro` |
|---|---|---|
| Ver pacientes e prontuário | ✅ | ❌ (403) |
| Registrar sessões | ✅ | ❌ (403) |
| Ver a lista de cobranças | ✅ (modo somente leitura) | ✅ |
| Criar cobrança | ❌ (403) | ✅ |
| Registrar pagamento | ❌ (403) | ✅ |
| Exportar planilha das cobranças | ✅ | ✅ |

## Como a separação funciona

- **Login com perfis** (Spring Security): `PSICOLOGA` e `FINANCEIRO`. O perfil financeiro recebe **acesso negado (403)** em qualquer página da área clínica, e a psicóloga só consulta o financeiro: criar cobrança e registrar pagamento são bloqueados no servidor, não só escondidos na tela.
- **Módulos independentes**: as tabelas clínicas (`clinico_*`) e financeiras (`fin_*`) não têm ligação entre si. A cobrança guarda só o código do paciente.
- **Uma única porta de saída**: o financeiro só recebe `DadosFaturamento` (código do paciente, nome e CPF de quem paga, valor da sessão), pela interface `FonteDeDadosFaturamento`. Quem implementa essa interface é a área clínica, que decide o que pode sair de lá.
- **Regra de arquitetura testada**: um teste falha se algum arquivo do módulo financeiro passar a usar código da área clínica.

## Técnicas de QA aplicadas

As mesmas técnicas dos exercícios de Python do repositório [estudos-qualidade-software](https://github.com/JaoFraccaroli/estudos-qualidade-software), agora em Java e aplicadas a regras reais do sistema:

| Técnica | Ferramenta | Onde está |
|---|---|---|
| **BDD** (cenários em português) | Cucumber | [`separacao_dos_dados.feature`](src/test/resources/features/separacao_dos_dados.feature): o que cada perfil pode acessar, e que o financeiro não vê anotações das sessões |
| **Testes dirigidos por dados** | JUnit `@CsvFileSource` | [`cpfs.csv`](src/test/resources/dados/cpfs.csv): 23 casos de CPF (válidos, dígito errado, todos iguais, tamanho, formatação, letras, vazio), cada um com o motivo, lidos por [`ValidadorCpfTest`](src/test/java/com/jaofraccaroli/clinica/comum/ValidadorCpfTest.java) |
| **Valores de fronteira** | JUnit `@ParameterizedTest` | [`SituacaoCobrancaTest`](src/test/java/com/jaofraccaroli/clinica/financeiro/SituacaoCobrancaTest.java): vence ontem, hoje e amanhã; [`CobrancaServiceTest`](src/test/java/com/jaofraccaroli/clinica/CobrancaServiceTest.java): valor -0,01, 0 e 0,01 |
| **Testes de segurança e de integração** | MockMvc + Spring Security Test | [`ControleDeAcessoTest`](src/test/java/com/jaofraccaroli/clinica/ControleDeAcessoTest.java): 403 para cada acesso proibido, inclusive enviando o formulário direto; login, senha inválida e sair |
| **Segurança dos dados exportados** | JUnit | [`ExportadorCsvCobrancasTest`](src/test/java/com/jaofraccaroli/clinica/financeiro/ExportadorCsvCobrancasTest.java): a planilha só tem colunas financeiras, e textos como `=SOMA(...)` não viram fórmula no Excel (injeção de CSV) |
| **Persistência** | Spring Boot | [`PersistenciaEmArquivoTest`](src/test/java/com/jaofraccaroli/clinica/PersistenciaEmArquivoTest.java): liga a aplicação, cadastra, desliga, liga de novo e confere que nada sumiu nem foi duplicado |
| **Mocks** | Mockito | `CobrancaServiceTest`: regras de cobrança sem banco de dados |
| **Regra de arquitetura** | JUnit | [`SeparacaoDeModulosTest`](src/test/java/com/jaofraccaroli/clinica/SeparacaoDeModulosTest.java): o financeiro não pode depender da área clínica |
| **Cobertura** | JaCoCo | `mvn verify` falha se a cobertura de linhas ficar abaixo de 85% |
| **Testes de mutação** | PIT (Pitest) | Altera o código das regras de negócio (troca `<` por `<=`, inverte condições, troca retornos) e confere se algum teste falha. Abaixo de 90% de mutantes mortos, o build falha |
| **Evidências e métricas** | GitHub Actions | Cada execução publica um resumo (testes, cobertura e mutation score) e os relatórios HTML para baixar |

### Defeitos encontrados pelos testes

| Defeito | Como foi encontrado | Correção |
|---|---|---|
| Ao clicar em **Sair**, a página `/login?logout` dava erro 404 | Teste manual; depois reproduzido simulando o navegador no GitHub Actions | Tela de login própria, em português, com testes de login e logout |
| `CobrancaService.listar()` e `pacientesParaCobranca()` sem teste de unidade | **PIT**: 2 mutantes "sem cobertura" | Testes de unidade novos; mutation score foi de 96% para 100% |
| Entidades trocadas nos repositórios, classe principal faltando, tela com nome errado | Revisão do projeto original | Ver o histórico de commits |

### Como ver os relatórios

No GitHub, abra a aba **Actions**, entre na execução mais recente e baixe o artefato **relatorios-de-qualidade**. Ele contém:
- `site/jacoco/index.html`: cobertura linha a linha
- `pit-reports/index.html`: cada mutante e o teste que o matou
- `relatorio-cucumber.html`: os cenários BDD

## Tecnologias

Java 17, Spring Boot 3 (Web, Data JPA, Security, Validation), Thymeleaf, Bootstrap 5, H2/MySQL, JUnit 5, Mockito, Cucumber, JaCoCo e PIT.

## Como rodar

É preciso ter o **Java 17 ou mais novo**.

**Pelo VS Code (mais fácil):** com a extensão *Extension Pack for Java* instalada, abra a pasta do projeto, abra `ClinicaApplication.java` e clique em **Run**. O VS Code baixa as dependências sozinho.

**Pelo terminal (precisa do Maven instalado):**

```bash
mvn spring-boot:run          # sobe a aplicação em http://localhost:8080
mvn verify                   # testes + relatório e cobertura mínima (JaCoCo)
mvn test-compile org.pitest:pitest-maven:mutationCoverage   # testes de mutação (PIT)
```

Usuários de demonstração (com pacientes **fictícios**):

| Usuário | Senha | Perfil |
|---|---|---|
| `psicologa` | `psicologa123` | Área clínica e consulta do financeiro |
| `financeiro` | `financeiro123` | Somente área financeira |

As senhas podem ser trocadas pelas variáveis `SENHA_PSICOLOGA` e `SENHA_FINANCEIRO`.

### Onde ficam os dados

- **Banco H2 em arquivo**: tudo fica em `dados/clinica.mv.db`, dentro da pasta do projeto, e **continua lá depois de reiniciar**. Os pacientes fictícios só são criados na primeira vez.
- **Começar do zero**: pare a aplicação e apague a pasta `dados`.
- A pasta `dados` está no `.gitignore`, então **nunca vai para o GitHub**.
- **Planilha**: na tela de cobranças, o botão **Exportar planilha** baixa um `.csv` que abre no Excel, com código do paciente, pagador, CPF, valor, vencimento, situação e data do pagamento. **Nenhum dado clínico** vai para a planilha. Ela é uma cópia para consulta: quem guarda os dados é o banco.
- **Testes** usam um banco separado, em memória, e não mexem nos seus dados.
- **MySQL**: use o perfil `mysql` e configure `DB_URL`, `DB_USER` e `DB_PASSWORD`.

## Próximos passos

- Bancos de dados separados para cada área (hoje são tabelas separadas no mesmo banco)
- Registro de quem acessou cada prontuário (auditoria)
- Emissão de recibos pela área financeira (o CPF do pagador já é validado)

## Histórico

Começou como um trabalho acadêmico de cadastro de pacientes e consultas, feito em dupla com Andrew, e foi reestruturado para o cenário de uma clínica de psicologia.
