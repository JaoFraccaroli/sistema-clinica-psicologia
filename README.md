# Sistema para Clínica de Psicologia

![Testes](https://github.com/JaoFraccaroli/sistema-clinica-psicologia/actions/workflows/testes.yml/badge.svg)

Sistema web para uma clínica de psicologia com **área clínica, área financeira e área do paciente separadas**, usado como projeto de **Qualidade de Software em Java**: testes de unidade, BDD, valores de fronteira, testes dirigidos por dados, cobertura e testes de mutação, tudo rodando no GitHub Actions a cada push.

| Testes | Cobertura de linhas (JaCoCo) | Mutation score (PIT) |
|---|---|---|
| 342, todos passando (75 cenários BDD), mais 98 verificações de API (Postman) e teste de carga (K6) | 97,9% (mínimo exigido: 85%) | 100%, 128 de 128 mutantes mortos (mínimo exigido: 90%) |

## Por que separar?

Dados de saúde são **dados pessoais sensíveis** pela LGPD, e o conteúdo dos atendimentos é sigiloso. Ao mesmo tempo, muitos psicólogos preferem que outra pessoa cuide das cobranças. Se o sistema mistura tudo, não dá para entregar o financeiro a outra pessoa sem expor o prontuário.

Aqui a separação é feita no próprio sistema:

| | Área clínica | Área financeira |
|---|---|---|
| Quem acessa | Psicóloga | Quem cuida do financeiro (completo) e a psicóloga (só consulta) |
| O que tem | Pacientes, sessões e anotações (prontuário) | Cobranças e pagamentos |
| Como identifica o paciente | Nome e dados completos | Apenas um **código** (ex.: `PAC-1A2B3C4D`) e os dados de quem paga |

### Permissões de cada perfil

| Ação | `psicologa` | `financeiro` | paciente |
|---|---|---|---|
| Ver pacientes e prontuário | ✅ | ❌ (403) | ❌ (403) |
| Registrar sessões | ✅ | ❌ (403) | ❌ (403) |
| Criar o acesso (senha) do paciente | ✅ | ❌ (403) | ❌ (403) |
| Ver a lista de cobranças | ✅ (modo somente leitura) | ✅ | ❌ (403) |
| Criar cobrança | ❌ (403) | ✅ | ❌ (403) |
| Registrar pagamento | ❌ (403) | ✅ | ❌ (403) |
| Exportar planilha das cobranças | ✅ | ✅ | ❌ (403) |
| Ver as **próprias** sessões e cobranças | ❌ (403) | ❌ (403) | ✅ (sem as anotações) |

### Área do paciente

- **Login:** o usuário é o **código do paciente** (ex.: `PAC-1A2B3C4D`). No prontuário, a psicóloga clica em **Criar acesso do paciente** e o sistema gera uma senha aleatória de 10 caracteres, mostrada **uma única vez** para ela entregar ao paciente.
- **Senha:** fica guardada só como hash BCrypt, numa tabela separada do cadastro clínico. Gerar uma nova senha faz a antiga parar de funcionar.
- **O que o paciente vê:** próximas sessões, sessões anteriores (data e situação) e as próprias cobranças, com o total em aberto. **Nunca vê as anotações** das sessões, nem dados de outros pacientes.
- **Segurança:** o código do paciente vem do login, nunca da URL. Por isso trocar parâmetros no endereço não mostra dados de outra pessoa (teste de IDOR).

## Como a separação funciona

- **Login com perfis** (Spring Security): `PSICOLOGA` e `FINANCEIRO`. O perfil financeiro recebe **acesso negado (403)** em qualquer página da área clínica, e a psicóloga só consulta o financeiro: criar cobrança e registrar pagamento são bloqueados no servidor, não só escondidos na tela.
- **Módulos independentes**: as tabelas clínicas (`clinico_*`) e financeiras (`fin_*`) não têm ligação entre si. A cobrança guarda só o código do paciente.
- **Uma única porta de saída**: o financeiro só recebe `DadosFaturamento` (código do paciente, nome e CPF de quem paga, valor da sessão), pela interface `FonteDeDadosFaturamento`. Quem implementa essa interface é a área clínica, que decide o que pode sair de lá.
- **Regra de arquitetura testada**: um teste falha se algum arquivo do módulo financeiro passar a usar código da área clínica.

## Técnicas de QA aplicadas

As mesmas técnicas dos exercícios de Python do repositório [estudos-qualidade-software](https://github.com/JaoFraccaroli/estudos-qualidade-software), agora em Java e aplicadas a regras reais do sistema:

| Técnica | Ferramenta | Onde está |
|---|---|---|
| **BDD** (cenários em português) | Cucumber | 4 funcionalidades e 75 cenários em [`features`](src/test/resources/features): [separação dos dados](src/test/resources/features/separacao_dos_dados.feature), [área do paciente](src/test/resources/features/area_do_paciente.feature), [cobranças](src/test/resources/features/cobrancas.feature) e [login](src/test/resources/features/login.feature) |
| **Testes dirigidos por dados** | JUnit `@CsvFileSource` | [`cpfs.csv`](src/test/resources/dados/cpfs.csv): 23 casos de CPF (válidos, dígito errado, todos iguais, tamanho, formatação, letras, vazio), cada um com o motivo, lidos por [`ValidadorCpfTest`](src/test/java/com/jaofraccaroli/clinica/comum/ValidadorCpfTest.java) |
| **Valores de fronteira** | JUnit `@ParameterizedTest` | [`SituacaoCobrancaTest`](src/test/java/com/jaofraccaroli/clinica/financeiro/SituacaoCobrancaTest.java): vence ontem, hoje e amanhã; [`CobrancaServiceTest`](src/test/java/com/jaofraccaroli/clinica/CobrancaServiceTest.java): valor -0,01, 0 e 0,01 |
| **Testes de segurança e de integração** | MockMvc + Spring Security Test | [`ControleDeAcessoTest`](src/test/java/com/jaofraccaroli/clinica/ControleDeAcessoTest.java) e [`AreaDoPacienteTest`](src/test/java/com/jaofraccaroli/clinica/AreaDoPacienteTest.java): 403 para cada acesso proibido, inclusive enviando o formulário direto; requisição sem token CSRF; paciente tentando ver outro paciente (IDOR); senha guardada só como hash; senha antiga invalidada; login, senha inválida e sair |
| **Segurança dos dados exportados** | JUnit | [`ExportadorCsvCobrancasTest`](src/test/java/com/jaofraccaroli/clinica/financeiro/ExportadorCsvCobrancasTest.java): a planilha só tem colunas financeiras, e textos como `=SOMA(...)` não viram fórmula no Excel (injeção de CSV) |
| **Persistência** | Spring Boot | [`PersistenciaEmArquivoTest`](src/test/java/com/jaofraccaroli/clinica/PersistenciaEmArquivoTest.java): liga a aplicação, cadastra, desliga, liga de novo e confere que nada sumiu nem foi duplicado |
| **Mocks** | Mockito | `CobrancaServiceTest`: regras de cobrança sem banco de dados |
| **Regra de arquitetura** | JUnit | [`SeparacaoDeModulosTest`](src/test/java/com/jaofraccaroli/clinica/SeparacaoDeModulosTest.java): o financeiro não pode depender da área clínica, e a área do paciente não pode ler as anotações das sessões |
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

Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation), Thymeleaf, Bootstrap 5, H2/MySQL, JUnit, Mockito, Cucumber, JaCoCo, PIT, Postman/Newman, K6, Docker, Docker Compose, Kubernetes (kind) e Trivy.

## Rodar com Docker (aplicação + MySQL)

Com o **Docker** instalado (no Windows, pelo Docker Desktop ou pelo WSL2), não é preciso ter Java nem Maven:

```bash
docker compose up --build     # sobe a aplicação + MySQL em http://localhost:8080
docker compose down           # para (os dados continuam no volume do MySQL)
docker compose down -v        # para e apaga os dados
```

- O [`Dockerfile`](Dockerfile) compila em duas etapas: o Maven roda **dentro** do container e a imagem final tem só o Java e o `.jar`. A aplicação roda com um **usuário sem privilégios**, não como root.
- O [`docker-compose.yml`](docker-compose.yml) sobe o **MySQL 8.4** com verificação de saúde. A aplicação só inicia quando o banco está pronto.
- As senhas padrão são só para demonstração. Para trocar, copie [`.env.exemplo`](.env.exemplo) para `.env`; o `.env` não vai para o GitHub.
- A cada push, o GitHub Actions sobe o Compose e roda [`scripts/teste_container.sh`](scripts/teste_container.sh): login de cada perfil, permissões (o financeiro recebe 403 na área clínica), senha errada, dados gravados no MySQL e **dados mantidos depois de reiniciar a aplicação**.

## Rodar no Kubernetes (cluster local com kind)

O [kind](https://kind.sigs.k8s.io/) cria um cluster Kubernetes **dentro do Docker**, de graça e sem conta em nuvem. No Windows, com o Docker Desktop aberto, instale o kind uma vez pelo PowerShell (`winget install Kubernetes.kind`); o `kubectl` já vem com o Docker Desktop.

```bash
kind create cluster --name clinica --config k8s/kind-config.yaml   # cria o cluster (1 vez)
docker build -t clinica:local .                                     # constrói a imagem
kind load docker-image clinica:local --name clinica                 # entrega a imagem ao cluster
kubectl apply -k k8s                                                # sobe MySQL + aplicação
kubectl -n clinica get pods -w                                      # acompanha até ficar Running (Ctrl+C para sair)
```

Abra **http://localhost:8081** (a 8080 fica livre para o Docker Compose). Para apagar tudo: `kind delete cluster --name clinica`.

O que tem em [`k8s/`](k8s):

| Arquivo | O que faz |
|---|---|
| `banco.yaml` | MySQL como **StatefulSet**, com disco próprio (PersistentVolumeClaim) que sobrevive se o pod for apagado |
| `aplicacao.yaml` | **Deployment** da aplicação, com *initContainer* que espera o banco, *probes* de inicialização/prontidão/vida, limites de memória e execução sem root; **Service** NodePort |
| `segredo-demo.yaml` | **Secret** com as senhas de demonstração (num cluster real, crie pelo terminal e não guarde no Git) |
| `kind-config.yaml` | Cluster de 1 nó, repassando a porta 30080 do nó para `localhost:8081` |

A cada push, o GitHub Actions cria um cluster kind, aplica os manifestos e roda `MODO=kubernetes bash scripts/teste_container.sh`: os mesmos testes do Compose e, além deles, **apaga os pods da aplicação e do banco** e confere que o Kubernetes os recria sozinho, sem perder nem duplicar dados.

## Imagem pronta no GitHub Container Registry

[![Imagem no ghcr.io](https://img.shields.io/badge/imagem-ghcr.io%2Fjaofraccaroli%2Fsistema--clinica--psicologia-2496ED?logo=docker&logoColor=white)](https://github.com/JaoFraccaroli/sistema-clinica-psicologia/pkgs/container/sistema-clinica-psicologia)

A cada push na `main`, **depois que todos os testes passam** (Java 17/21/25, Compose e Kubernetes), o GitHub Actions:

1. publica a imagem em `ghcr.io/jaofraccaroli/sistema-clinica-psicologia` com duas etiquetas: `latest` e o código curto do commit (para voltar a uma versão antiga);
2. faz uma **varredura de vulnerabilidades** com o [Trivy](https://trivy.dev/) **antes** de publicar e mostra o relatório no resumo da execução;
3. **bloqueia a publicação** se houver vulnerabilidade **crítica** que já tenha correção (portão de qualidade);
4. **baixa a imagem publicada** e roda de novo os testes de fumaça, para garantir que o que foi publicado funciona.

Para usar sem compilar nada:

```bash
docker compose -f docker-compose.ghcr.yml up          # aplicação + MySQL em http://localhost:8080
kubectl apply -k k8s/ghcr                             # no cluster kind, sem docker build/kind load
```

## Testes de API (Postman) e de carga (K6)

**Postman / Newman** ([`postman/clinica.postman_collection.json`](postman/clinica.postman_collection.json)): **38 requisições e 98 verificações**, em 4 pastas.

| Pasta | O que verifica |
|---|---|
| 1. Segurança sem login | Páginas protegidas redirecionam para o login. Cabeçalhos de segurança (`nosniff`, `X-Frame-Options: DENY`, `no-store`). Login sem token CSRF é recusado (403). Senha errada e usuário inexistente também são recusados. |
| 2. Psicóloga | Vê pacientes e cobranças, mas **não** cria cobranças nem acessa a área do paciente. Depois do logout, perde o acesso. |
| 3. Financeiro | O CSV exportado tem BOM UTF-8, 7 colunas separadas por `;`, a linha da cobrança fictícia e nenhuma célula iniciada por `= + - @` (injeção de fórmula). Vem como anexo `.csv`. O financeiro **não** vê a área clínica. |
| 4. Paciente | Entra com o código em minúsculas e vê só os próprios dados, sem os de outro paciente. Recebe 403 nas áreas clínica e financeira. Paciente sem acesso liberado não entra. |

Para abrir no **Postman**: *Import* → selecione o arquivo da coleção → *Run collection*, com a aplicação rodando em `http://localhost:8080`. Pelo terminal: `npx newman run postman/clinica.postman_collection.json`.

**K6** ([`k6/carga.js`](k6/carga.js)): simula **até 18 usuários ao mesmo tempo**, sendo psicólogas, financeiro e pacientes. Cada um entra no sistema (com CSRF), navega e sai, por 70 segundos. O teste só passa se cumprir todos os critérios:

| Critério | Limite |
|---|---|
| Requisições com erro | menos de 1% |
| Páginas: 95% respondem em | menos de 800 ms |
| Login (BCrypt): 95% respondem em | menos de 2 s |
| Verificações corretas | mais de 99% |

```bash
k6 run k6/carga.js                                  # aplicação em http://localhost:8080
docker run --rm -i --network host grafana/k6 run - < k6/carga.js   # sem instalar o k6 (Linux)
```

A cada push, o GitHub Actions sobe a aplicação com MySQL e roda os dois. Os resultados aparecem no resumo da execução. O relatório HTML do Postman e o JSON do K6 ficam nos artefatos.

## Como rodar sem Docker

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
| `PAC-DEMO0001` | `paciente123` | Paciente fictícia Ana: área do paciente |

As senhas podem ser trocadas pelas variáveis `SENHA_PSICOLOGA` e `SENHA_FINANCEIRO`.

### Onde ficam os dados

- **Banco H2 em arquivo**: tudo fica em `dados/clinica.mv.db`, dentro da pasta do projeto, e **continua lá depois de reiniciar**. Os pacientes fictícios só são criados na primeira vez.
- **Começar do zero**: pare a aplicação e apague a pasta `dados`.
- A pasta `dados` está no `.gitignore`, então **nunca vai para o GitHub**.
- **Planilha**: na tela de cobranças, o botão **Exportar planilha** baixa um `.csv` que abre no Excel, com código do paciente, pagador, CPF, valor, vencimento, situação e data do pagamento. **Nenhum dado clínico** vai para a planilha. Ela é uma cópia para consulta: quem guarda os dados é o banco.
- **Testes** usam um banco separado, em memória, e não mexem nos seus dados.
- **MySQL**: use o perfil `mysql` e configure `DB_URL`, `DB_USER` e `DB_PASSWORD`.

## Próximos passos

- Paciente trocar a própria senha no primeiro acesso
- Bloquear o login depois de várias senhas erradas seguidas
- Bancos de dados separados para cada área (hoje são tabelas separadas no mesmo banco)
- Registro de quem acessou cada prontuário (auditoria)
- Emissão de recibos pela área financeira (o CPF do pagador já é validado)

## Histórico

Começou como um trabalho acadêmico de cadastro de pacientes e consultas, feito em dupla com Andrew, e foi reestruturado para o cenário de uma clínica de psicologia.
