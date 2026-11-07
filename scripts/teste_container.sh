#!/usr/bin/env bash
# Testes de fumaça da clínica rodando no Docker Compose (aplicação + MySQL).
# Usa só curl, como um usuário de verdade usaria pelo navegador.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
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

echo "== Reiniciando a aplicação (os dados precisam continuar no MySQL)"
docker compose restart app >/dev/null
sleep 5
esperar_aplicacao
verificar_tudo
[[ "$(contar_pacientes)" == "2" ]] && ok "Depois de reiniciar: mesmos 2 pacientes, sem duplicar" || falha "Pacientes sumiram ou duplicaram"

echo
if [[ $FALHAS -eq 0 ]]; then echo "Tudo certo no container."; else echo "$FALHAS verificação(ões) falharam."; exit 1; fi
