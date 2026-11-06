# language: pt
Funcionalidade: Separação entre a área clínica e a financeira
  Para proteger os dados sensíveis dos pacientes
  Como psicóloga
  Quero que quem cuida do financeiro não tenha acesso à área clínica

  Esquema do Cenário: Cada perfil acessa apenas as áreas permitidas
    Dado que estou logado como "<perfil>"
    Quando eu acesso "<pagina>"
    Então o acesso deve ser "<resultado>"

    Exemplos:
      | perfil     | pagina                     | resultado |
      | psicologa  | /clinico/pacientes         | permitido |
      | psicologa  | /financeiro/cobrancas      | permitido |
      | psicologa  | /financeiro/cobrancas/nova | negado    |
      | financeiro | /financeiro/cobrancas      | permitido |
      | financeiro | /financeiro/cobrancas/nova | permitido |
      | financeiro | /clinico/pacientes         | negado    |

  Cenário: Visitante sem login é enviado para a tela de login
    Dado que não estou logado
    Quando eu acesso "/clinico/pacientes"
    Então devo ser enviado para a tela de login

  Cenário: O financeiro vê o código do paciente, mas não as anotações das sessões
    Dado que existe um paciente com a anotação de sessão "Relato sigiloso de teste"
    E existe uma cobrança para esse paciente
    E que estou logado como "financeiro"
    Quando eu acesso "/financeiro/cobrancas"
    Então a página deve mostrar o código do paciente
    E a página não deve conter "Relato sigiloso de teste"

  Cenário: A psicóloga só consulta as cobranças
    Dado que estou logado como "psicologa"
    Quando eu acesso "/financeiro/cobrancas"
    Então o acesso deve ser "permitido"
    E a página deve conter "Modo somente leitura"
    E a página não deve conter "Nova cobrança"

  Esquema do Cenário: A psicóloga não consegue alterar o financeiro
    Dado que existe um paciente com a anotação de sessão "Sessão de teste"
    E existe uma cobrança para esse paciente
    E que estou logado como "psicologa"
    Quando eu tento <acao>
    Então o acesso deve ser "negado"

    Exemplos:
      | acao                                 |
      | criar uma cobrança para esse paciente |
      | registrar o pagamento dessa cobrança  |
