# language: pt
Funcionalidade: Área do paciente
  Para acompanhar minhas sessões e pagamentos
  Como paciente da clínica
  Quero entrar com o meu código e ver somente as minhas informações

  Contexto:
    Dado que existe o paciente "Paciente Alfa" com a anotação de sessão "Segredo da Alfa"
    E o paciente "Paciente Alfa" tem uma sessão agendada daqui a 3 dias
    E o paciente "Paciente Alfa" tem uma cobrança de "150.00" vencendo em 5 dias
    E a psicóloga criou o acesso do paciente "Paciente Alfa"

  Cenário: Paciente entra com o código e a senha recebida
    Quando o paciente "Paciente Alfa" entra com a senha recebida
    Então o login deve ser aceito
    E a página inicial deve levar para "/paciente"

  Cenário: Paciente pode digitar o código em minúsculas
    Quando o paciente "Paciente Alfa" entra com o código em minúsculas e a senha recebida
    Então o login deve ser aceito

  Esquema do Cenário: Senha errada não entra
    Quando o paciente "Paciente Alfa" entra com a senha "<senha>"
    Então o login deve ser recusado

    Exemplos:
      | senha                  |
      | SenhaErrada123         |
      | senha-teste-psicologa  |
      | senha-teste-financeiro |
      | 12345678               |

  Cenário: A senha antiga para de funcionar quando a psicóloga gera uma nova
    Dado que a psicóloga gerou uma nova senha para o paciente "Paciente Alfa"
    Quando o paciente "Paciente Alfa" entra com a senha antiga
    Então o login deve ser recusado
    Quando o paciente "Paciente Alfa" entra com a senha recebida
    Então o login deve ser aceito

  Cenário: Paciente sem acesso criado não entra
    Dado que existe o paciente "Paciente Sem Acesso" com a anotação de sessão "Nada"
    Quando o paciente "Paciente Sem Acesso" entra com a senha "QualquerSenha1"
    Então o login deve ser recusado

  Cenário: Paciente vê as próprias sessões e cobranças, mas não as anotações
    Dado que estou logado como o paciente "Paciente Alfa"
    Quando eu acesso "/paciente"
    Então o acesso deve ser "permitido"
    E a página deve conter "Paciente Alfa"
    E a página deve conter "Agendada"
    E a página deve conter "Realizada"
    E a página deve conter "R$ 150,00"
    E a página não deve conter "Segredo da Alfa"

  Cenário: Paciente não vê nada de outro paciente
    Dado que existe o paciente "Paciente Beta" com a anotação de sessão "Segredo do Beta"
    E o paciente "Paciente Beta" tem uma cobrança de "999.99" vencendo em 2 dias
    E que estou logado como o paciente "Paciente Alfa"
    Quando eu acesso "/paciente"
    Então a página não deve conter "Paciente Beta"
    E a página não deve conter "Segredo do Beta"
    E a página não deve conter "999,99"
    E a página não deve conter o código do paciente "Paciente Beta"

  Cenário: Trocar o código na URL não mostra outro paciente
    Dado que existe o paciente "Paciente Gama" com a anotação de sessão "Segredo da Gama"
    E que estou logado como o paciente "Paciente Alfa"
    Quando eu acesso a minha área informando o código do paciente "Paciente Gama"
    Então a página deve conter "Paciente Alfa"
    E a página não deve conter "Paciente Gama"

  Esquema do Cenário: Paciente não acessa as áreas da equipe
    Dado que estou logado como o paciente "Paciente Alfa"
    Quando eu acesso "<pagina>"
    Então o acesso deve ser "negado"

    Exemplos:
      | pagina                         |
      | /clinico/pacientes             |
      | /clinico/pacientes/novo        |
      | /financeiro/cobrancas          |
      | /financeiro/cobrancas/nova     |
      | /financeiro/cobrancas/exportar |

  Cenário: Paciente não abre o próprio prontuário
    Dado que estou logado como o paciente "Paciente Alfa"
    Quando eu acesso o prontuário do paciente "Paciente Alfa"
    Então o acesso deve ser "negado"

  Cenário: O financeiro não cria acesso de paciente
    Dado que existe o paciente "Paciente Delta" com a anotação de sessão "Nada"
    E que estou logado como "financeiro"
    Quando eu tento criar o acesso do paciente "Paciente Delta"
    Então o acesso deve ser "negado"

  Esquema do Cenário: Situação da cobrança que o paciente vê
    Dado que existe o paciente "<paciente>" com a anotação de sessão "Nada"
    E o paciente "<paciente>" tem uma cobrança de "120.00" vencendo em <dias> dias
    E que estou logado como o paciente "<paciente>"
    Quando eu acesso "/paciente"
    Então a página deve conter "<situacao>"
    E a página deve conter "<total>"

    Exemplos: Valores de fronteira do vencimento
      | paciente          | dias | situacao | total     |
      | Paciente Venc -30 | -30  | Atrasada | R$ 120,00 |
      | Paciente Venc -1  | -1   | Atrasada | R$ 120,00 |
      | Paciente Venc 0   | 0    | Pendente | R$ 120,00 |
      | Paciente Venc 1   | 1    | Pendente | R$ 120,00 |
      | Paciente Venc 30  | 30   | Pendente | R$ 120,00 |
