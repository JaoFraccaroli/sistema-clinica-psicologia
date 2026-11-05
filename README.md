# Sistema de Clínica – Spring Boot

![Testes](https://github.com/JaoFraccaroli/sistema-clinica-spring-boot/actions/workflows/testes.yml/badge.svg)

Sistema web para **cadastro de pacientes** e **agendamento de consultas**, desenvolvido como trabalho acadêmico do curso de Engenharia da Computação, em dupla com Andrew.

## Funcionalidades

- Cadastrar pacientes (nome, idade, telefone, e-mail e especialidade desejada)
- Listar pacientes cadastrados
- Agendar consultas por especialidade e horário
- Listar consultas agendadas

## Tecnologias

- Java 17 e Spring Boot 3 (Web, Data JPA)
- Thymeleaf e Bootstrap 5 nas telas
- MySQL como banco de dados (H2 em memória nos testes)
- JUnit 5, Mockito e MockMvc nos testes
- GitHub Actions para rodar os testes a cada push

## Testes e qualidade

Ao revisar o projeto, encontrei e corrigi defeitos que impediam o sistema de funcionar:

| Defeito | Correção |
|---|---|
| Os repositórios estavam com as entidades trocadas (`InterConsulta` salvava `Paciente` e vice-versa) | Cada repositório passou a usar a entidade correta |
| Métodos `saveAll` personalizados que o Spring Data não consegue implementar | Uso do método padrão `save` |
| Faltava a classe principal com `@SpringBootApplication` | Classe `TrabalhoMaromoApplication` criada |
| O controller procurava a tela `listar_consultas`, mas o arquivo se chamava `listar_consulta` | Nome corrigido |
| A pasta `Templates` com "T" maiúsculo não é encontrada no Linux | Renomeada para `templates` |
| A listagem de consultas quebrava quando a consulta não tinha paciente | Uso de navegação segura (`paciente?.nome`) |
| Senha do banco escrita no código | Configuração por variáveis de ambiente |

Os testes automatizados cobrem esses casos:

- `TrabalhoMaromoApplicationTests`: a aplicação sobe sem erros
- `PacienteServeTest`: regras do serviço de pacientes (com Mockito)
- `PacienteControllerTest` e `ConsultaControllerTest`: telas, formulários e redirecionamentos (com MockMvc)

```bash
mvn test
```

## Como rodar

1. Tenha o Java 17, o Maven e um MySQL com o banco `clinica_db` criado.
2. Configure as variáveis de ambiente do banco:
   ```bash
   export DB_URL=jdbc:mysql://localhost:3306/clinica_db
   export DB_USER=root
   export DB_PASSWORD=sua_senha
   ```
3. Rode `mvn spring-boot:run` e acesse `http://localhost:8080/pacientes/cadastrar`.
