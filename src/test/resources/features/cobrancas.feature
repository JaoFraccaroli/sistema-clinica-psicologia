# language: pt
Funcionalidade: Cobranças
  Para receber pelas sessões
  Como responsável pelo financeiro
  Quero criar cobranças e registrar pagamentos com segurança

  Contexto:
    Dado que existe o paciente "Paciente Financeiro" com a anotação de sessão "Nada"
    E que estou logado como "financeiro"

  Esquema do Cenário: Valor da cobrança (valores de fronteira)
    Quando eu crio uma cobrança de "<valor>" para o paciente "Paciente Financeiro"
    Então a cobrança deve ser <resultado>

    Exemplos:
      | valor    | resultado |
      | 0.01     | aceita    |
      | 1.00     | aceita    |
      | 150.00   | aceita    |
      | 99999.99 | aceita    |
      | 0        | recusada  |
      | 0.00     | recusada  |
      | -0.01    | recusada  |
      | -150.00  | recusada  |

  Cenário: Cobrança para um paciente que não existe é recusada
    Quando eu crio uma cobrança de "150.00" para o código "PAC-NAOEXISTE"
    Então a cobrança deve ser recusada
    E a mensagem de erro deve conter "Paciente não encontrado"

  Cenário: Registrar o pagamento de uma cobrança
    Dado que o paciente "Paciente Financeiro" tem uma cobrança de "150.00" vencendo em 5 dias
    Quando eu registro o pagamento dessa cobrança
    Então devo ver a mensagem "Pagamento registrado"
    E a cobrança deve estar paga

  Cenário: A mesma cobrança não pode ser paga duas vezes
    Dado que o paciente "Paciente Financeiro" tem uma cobrança de "150.00" vencendo em 5 dias
    E eu registro o pagamento dessa cobrança
    Quando eu registro o pagamento dessa cobrança
    Então a mensagem de erro deve conter "já foi paga"

  Cenário: Pagamento de cobrança que não existe
    Quando eu registro o pagamento da cobrança número 999999
    Então a mensagem de erro deve conter "Cobrança não encontrada"

  Cenário: A tela de nova cobrança lista os pacientes só pelo código
    Quando eu acesso "/financeiro/cobrancas/nova"
    Então o acesso deve ser "permitido"
    E a página deve conter o código do paciente "Paciente Financeiro"
    E a página não deve conter "Paciente Financeiro"
