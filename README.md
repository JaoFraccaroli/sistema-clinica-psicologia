# Sistema para Clínica de Psicologia

![Testes](https://github.com/JaoFraccaroli/sistema-clinica-psicologia/actions/workflows/testes.yml/badge.svg)

Sistema web para uma clínica de psicologia com **área clínica e área financeira separadas**.

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

## Como a separação funciona

- **Login com perfis** (Spring Security): `PSICOLOGA` e `FINANCEIRO`. O perfil financeiro recebe **acesso negado (403)** em qualquer página da área clínica, e a psicóloga só consulta o financeiro: criar cobrança e registrar pagamento são bloqueados no servidor, não só escondidos na tela.
- **Módulos independentes**: as tabelas clínicas (`clinico_*`) e financeiras (`fin_*`) não têm ligação entre si. A cobrança guarda só o código do paciente.
- **Uma única porta de saída**: o financeiro só recebe `DadosFaturamento` (código do paciente, nome e CPF de quem paga, valor da sessão), pela interface `FonteDeDadosFaturamento`. Quem implementa essa interface é a área clínica, que decide o que pode sair de lá.
- **Regra de arquitetura testada**: um teste falha se algum arquivo do módulo financeiro passar a usar código da área clínica.

## Testes

| Tipo | O que verifica |
|---|---|
| **BDD (Cucumber, em português)** | `src/test/resources/features/separacao_dos_dados.feature`: o que cada perfil pode acessar e que a tela do financeiro não mostra anotações das sessões |
| **Controle de acesso (MockMvc + Spring Security Test)** | Financeiro recebe 403 na área clínica; psicóloga recebe 403 ao criar cobrança ou registrar pagamento, mesmo enviando o formulário direto; login, senha inválida e sair; validação do cadastro |
| **Regras de negócio (JUnit + Mockito)** | Cobrança com valor zero ou negativo, paciente inexistente, pagamento em dobro |
| **Arquitetura** | Financeiro não depende da área clínica e só recebe os campos permitidos |

Exemplo de cenário:

```gherkin
Cenário: O financeiro vê o código do paciente, mas não as anotações das sessões
  Dado que existe um paciente com a anotação de sessão "Relato sigiloso de teste"
  E existe uma cobrança para esse paciente
  E que estou logado como "financeiro"
  Quando eu acesso "/financeiro/cobrancas"
  Então a página deve mostrar o código do paciente
  E a página não deve conter "Relato sigiloso de teste"
```

Os testes rodam no GitHub Actions a cada push, e o relatório do Cucumber fica nos artefatos da execução.

## Tecnologias

Java 17, Spring Boot 3 (Web, Data JPA, Security, Validation), Thymeleaf, Bootstrap 5, H2/MySQL, JUnit 5, Mockito, Cucumber.

## Como rodar

Só é preciso ter o **Java 17 ou mais novo**. O projeto traz o **Maven Wrapper** (`mvnw`), que baixa o Maven sozinho na primeira vez.

No Windows (cmd ou PowerShell), dentro da pasta do projeto:

```bash
mvnw spring-boot:run
```

No Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Acesse `http://localhost:8080`. Usuários de demonstração (com pacientes **fictícios**):

| Usuário | Senha | Perfil |
|---|---|---|
| `psicologa` | `psicologa123` | Área clínica e consulta do financeiro |
| `financeiro` | `financeiro123` | Somente área financeira |

As senhas podem ser trocadas pelas variáveis `SENHA_PSICOLOGA` e `SENHA_FINANCEIRO`. Por padrão o sistema usa um banco H2 em memória (os dados somem ao parar a aplicação); para MySQL, rode com `mvnw spring-boot:run -Dspring-boot.run.profiles=mysql` e configure `DB_URL`, `DB_USER` e `DB_PASSWORD`.

Para rodar os testes: `mvnw test`.

## Próximos passos

- Bancos de dados separados para cada área (hoje são tabelas separadas no mesmo banco)
- Registro de quem acessou cada prontuário (auditoria)
- Emissão de recibos pela área financeira

## Histórico

Começou como um trabalho acadêmico de cadastro de pacientes e consultas, feito em dupla com Andrew, e foi reestruturado para o cenário de uma clínica de psicologia.
