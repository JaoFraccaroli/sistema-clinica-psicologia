#!/usr/bin/env bash
# Testes de fumaça da clínica rodando em container (aplicação + MySQL).
# Usa só curl, como um usuário de verdade usaria pelo navegador.
#
#   bash scripts/teste_container.sh                      (Docker Compose, porta 8080)
#   MODO=kubernetes bash scripts/teste_container.sh      (cluster kind, porta 8081)
set -euo pipefail

MODO="${MODO:-compose}"
if [[ "$MODO" == "kubernetes" ]]; then
  BASE="${BASE:-http://localhost:8081}"
else
  BASE="${BASE:-http://localhost:8080}"
fi
DB_USER="${DB_USER:-clinica}"
DB_PASSWORD="${DB_PASSWORD:-clinica-senha-demo}"
FALHAS=0

ok()    { echo "✅ $1"; }
falha() { echo "❌ $1"; FALHAS=$((FALHAS + 1)); }

esperar_aplicacao() {
  for _ in $(seq 1 100); do
    if curl -fsS "$BASE/login" >/dev/null 2>&1; then return 0; fi
    sleep 3
  done
  echo "A aplicação não respondeu a tempo"; return 1
}

# Faz login pela tela (com o token CSRF) e guarda os cookies em $1
entrar() {
  local jar="$1" usuario="$2" senha="$3" pagina token destino
  pagina=$(curl -fsS -c "$jar" "$BASE/login")
  token=$(printf '%s' "$pagina" | python3 -c 'import re,sys; h=sys.stdin.read(); m=re.search(r"name=\"_csrf\"[^>]*value=\"([^\"]+)\"|value=\"([^\"]+)\"[^>]*name=\"_csrf\"", h); print(m.group(1) or m.group(2))')
  destino=$(curl -sS -b "$jar" -c "$jar" -o /dev/null -w '%{redirect_url}' \
    --data-urlencode "username=$usuario" --data-urlencode "password=$senha" --data-urlencode "_csrf=$token" \
    "$BASE/login")
  printf '%s' "$destino"
}

pagina_contem() {   # cookies, caminho, texto
  local html; html=$(curl -fsS -b "$1" "$BASE$2") || return 1
  [[ "$html" == *"$3"* ]]
}

status_de() {       # cookies, caminho
  curl -s -o /dev/null -w '%{http_code}' -b "$1" "$BASE$2"
}

contar_pacientes() {
  if [[ "$MODO" == "kubernetes" ]]; then
    kubectl -n clinica exec clinica-banco-0 -- mysql -h 127.0.0.1 -u"$DB_USER" -p"$DB_PASSWORD" clinica -N -s \
      -e "SELECT COUNT(*) FROM clinico_paciente" 2>>/tmp/erros_mysql.txt | tr -d '[:space:]'
    return
  fi
  docker compose exec -T banco mysql -u"$DB_USER" -p"$DB_PASSWORD" clinica -N -s \
    -e "SELECT COUNT(*) FROM clinico_paciente" 2>/dev/null | tr -d '[:space:]'
}

verificar_tudo() {
  local tmp; tmp=$(mktemp -d)

  [[ "$(curl -fsS "$BASE/login")" == *"Entrar"* ]] && ok "Tela de login abre" || falha "Tela de login não abriu"

  [[ "$(entrar "$tmp/psi" psicologa psicologa123)" == */ ]] && ok "Psicóloga entra" || falha "Login da psicóloga"
  pagina_contem "$tmp/psi" /clinico/pacientes "Ana Exemplo" && ok "Psicóloga vê os pacientes" || falha "Lista de pacientes"

  [[ "$(entrar "$tmp/fin" financeiro financeiro123)" == */ ]] && ok "Financeiro entra" || falha "Login do financeiro"
  pagina_contem "$tmp/fin" /financeiro/cobrancas "Cobranças" && ok "Financeiro vê as cobranças" || falha "Lista de cobranças"
  [[ "$(status_de "$tmp/fin" /clinico/pacientes)" == "403" ]] && ok "Financeiro NÃO vê a área clínica (403)" || falha "Financeiro acessou a área clínica"

  [[ "$(entrar "$tmp/pac" PAC-DEMO0001 paciente123)" == */ ]] && ok "Paciente entra com o código" || falha "Login do paciente"
  pagina_contem "$tmp/pac" /paciente "Ana Exemplo" && ok "Paciente vê a própria área" || falha "Área do paciente"

  [[ "$(entrar "$tmp/err" psicologa senha-errada)" == *"/login?error"* ]] && ok "Senha errada é recusada" || falha "Senha errada foi aceita"

  rm -rf "$tmp"
}

echo "== Subindo e esperando a aplicação"
esperar_aplicacao

echo "== Primeira execução"
verificar_tudo
[[ "$(contar_pacientes)" == "2" ]] && ok "Dados gravados no MySQL (2 pacientes fictícios)" || falha "MySQL não tem os 2 pacientes"

if [[ "$MODO" == "kubernetes" ]]; then
  echo "== Apagando o pod da aplicação E o do banco (o Kubernetes tem que recriar sozinho, sem perder dados)"
  pod_antigo=$(kubectl -n clinica get pods -l app=clinica-app -o jsonpath='{.items[0].metadata.name}')
  kubectl -n clinica delete pod "$pod_antigo" clinica-banco-0 --wait=false >/dev/null
  pod_novo=""
  for _ in $(seq 1 100); do
    pod_novo=$(kubectl -n clinica get pods -l app=clinica-app \
      -o jsonpath='{range .items[?(@.status.containerStatuses[0].ready==true)]}{.metadata.name}{"\n"}{end}' | grep -v "^$pod_antigo$" | head -1 || true)
    [[ -n "$pod_novo" ]] && break
    sleep 3
  done
  [[ -n "$pod_novo" ]] && ok "Kubernetes recriou o pod da aplicação sozinho ($pod_antigo → $pod_novo)" \
    || falha "Pod da aplicação não voltou"
  banco_ok=false
  for _ in $(seq 1 30); do   # o pod pode estar sendo recriado (ainda "não existe") por alguns segundos
    if kubectl -n clinica wait --for=condition=Ready pod/clinica-banco-0 --timeout=20s >/dev/null 2>&1; then banco_ok=true; break; fi
    sleep 3
  done
  $banco_ok && ok "Kubernetes recriou o pod do banco com o mesmo disco" || falha "Pod do banco não voltou"
else
  echo "== Reiniciando a aplicação (os dados precisam continuar no MySQL)"
  docker compose restart app >/dev/null
fi
sleep 5
esperar_aplicacao
verificar_tudo
[[ "$(contar_pacientes)" == "2" ]] && ok "Depois de reiniciar: mesmos 2 pacientes, sem duplicar" || falha "Pacientes sumiram ou duplicaram"

if [[ $FALHAS -ne 0 && -s /tmp/erros_mysql.txt ]]; then
  echo "Erros do MySQL:"; grep -v "Using a password" /tmp/erros_mysql.txt | sort -u | head -5
fi

echo
if [[ $FALHAS -eq 0 ]]; then echo "Tudo certo no container."; else echo "$FALHAS verificação(ões) falharam."; exit 1; fi
