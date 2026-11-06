# language: pt
Funcionalidade: Separação entre a área clínica e a financeira
  Para proteger os dados sensíveis dos pacientes
  Como psicóloga
  Quero que quem cuida do financeiro não tenha acesso à área clínica

  Esquema do Cenário: Cada perfil acessa apenas as áreas permitidas
    Dado que estou logado como "<perfil>"
    Quando eu acesso "<pagina>"
    Então o acesso deve ser "<resultado>"

    Exemplos: Psicóloga
      | perfil    | pagina                         | resultado |
      | psicologa | /clinico/pacientes             | permitido |
      | psicologa | /clinico/pacientes/novo        | permitido |
      | psicologa | /financeiro/cobrancas          | permitido |
      | psicologa | /financeiro/cobrancas/exportar | permitido |
      | psicologa | /financeiro/cobrancas/nova     | negado    |
      | psicologa | /paciente                      | negado    |

    Exemplos: Financeiro
      | perfil     | pagina                         | resultado |
      | financeiro | /financeiro/cobrancas          | permitido |
      | financeiro | /financeiro/cobrancas/nova     | permitido |
      | financeiro | /financeiro/cobrancas/exportar | permitido |
      | financeiro | /clinico/pacientes             | negado    |
      | financeiro | /clinico/pacientes/novo        | negado    |
      | financeiro | /paciente                      | negado    |

  Esquema do Cenário: Visitante sem login é enviado para a tela de login
    Dado que não estou logado
    Quando eu acesso "<pagina>"
    Então devo ser enviado para a tela de login

    Exemplos:
      | pagina                         |
      | /                              |
      | /clinico/pacientes             |
      | /clinico/pacientes/novo        |
      | /financeiro/cobrancas          |
      | /financeiro/cobrancas/exportar |
      | /paciente                      |

  Cenário: O financeiro vê o código do paciente, mas não as anotações das sessões
    Dado que existe um paciente com a anotação de sessão "Relato sigiloso de teste"
    E existe uma cobrança para esse paciente
    E que estou logado como "financeiro"
    Quando eu acesso "/financeiro/cobrancas"
    Então a página deve mostrar o código do paciente
    E a página não deve conter "Relato sigiloso de teste"

  Cenário: O financeiro não abre o prontuário nem sabendo o número do paciente
    Dado que existe um paciente com a anotação de sessão "Relato que o financeiro não pode ler"
    E que estou logado como "financeiro"
    Quando eu acesso o prontuário desse paciente
    Então o acesso deve ser "negado"

  Cenário: A psicóloga vê as anotações no prontuário
    Dado que existe um paciente com a anotação de sessão "Relato visível para a psicóloga"
    E que estou logado como "psicologa"
    Quando eu acesso o prontuário desse paciente
    Então o acesso deve ser "permitido"
    E a página deve conter "Relato visível para a psicóloga"

  Cenário: A psicóloga só consulta as cobranças
    Dado que estou logado como "psicologa"
    Quando eu acesso "/financeiro/cobrancas"
    Então o acesso deve ser "permitido"
    E a página deve conter "Modo somente leitura"
    E a página deve conter "Exportar planilha"
    E a página não deve conter "Nova cobrança"

  Esquema do Cenário: A psicóloga não consegue alterar o financeiro
    Dado que existe um paciente com a anotação de sessão "Sessão de teste"
    E existe uma cobrança para esse paciente
    E que estou logado como "psicologa"
    Quando eu tento <acao>
    Então o acesso deve ser "negado"

    Exemplos:
      | acao                                  |
      | criar uma cobrança para esse paciente |
      | registrar o pagamento dessa cobrança  |

  Cenário: A planilha exportada não leva dados clínicos
    Dado que existe o paciente "Paciente da Planilha" com a anotação de sessão "Segredo que não vai para a planilha"
    E o paciente "Paciente da Planilha" tem uma cobrança de "200.00" vencendo em 3 dias
    E que estou logado como "financeiro"
    Quando eu acesso "/financeiro/cobrancas/exportar"
    Então o acesso deve ser "permitido"
    E a página deve conter o código do paciente "Paciente da Planilha"
    E a página deve conter "200,00"
    E a página não deve conter "Paciente da Planilha"
    E a página não deve conter "Segredo que não vai para a planilha"
