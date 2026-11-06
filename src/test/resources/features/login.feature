# language: pt
Funcionalidade: Login e saída do sistema
  Para que só pessoas autorizadas vejam os dados da clínica
  Como clínica
  Quero que cada pessoa entre com o próprio usuário e vá para a própria área

  Esquema do Cenário: Login da equipe
    Quando alguém entra com o usuário "<usuario>" e a senha "<senha>"
    Então o login deve ser <resultado>

    Exemplos:
      | usuario    | senha                  | resultado |
      | psicologa  | senha-teste-psicologa  | aceito    |
      | financeiro | senha-teste-financeiro | aceito    |
      | PSICOLOGA  | senha-teste-psicologa  | aceito    |
      | psicologa  | senha-teste-financeiro | recusado  |
      | financeiro | senha-teste-psicologa  | recusado  |
      | psicologa  | SENHA-TESTE-PSICOLOGA  | recusado  |
      | psicologa  |                        | recusado  |
      | admin      | admin                  | recusado  |
      |            | senha-teste-psicologa  | recusado  |
      | ' OR 1=1 --| qualquer               | recusado  |

  Esquema do Cenário: Cada perfil vai para a própria área depois do login
    Quando alguém entra com o usuário "<usuario>" e a senha "<senha>"
    Então o login deve ser aceito
    E a página inicial deve levar para "<area>"

    Exemplos:
      | usuario    | senha                  | area                  |
      | psicologa  | senha-teste-psicologa  | /clinico/pacientes    |
      | financeiro | senha-teste-financeiro | /financeiro/cobrancas |

  Cenário: Senha errada mostra o aviso na tela de login
    Quando alguém entra com o usuário "psicologa" e a senha "errada"
    Então o login deve ser recusado
    E devo ser enviado para "/login?error"

  Cenário: Depois de sair, não dá mais para acessar
    Dado que alguém entrou com o usuário "psicologa" e a senha "senha-teste-psicologa"
    Quando eu saio do sistema
    Então devo ser enviado para "/login?logout"
    E não consigo mais acessar "/clinico/pacientes"
