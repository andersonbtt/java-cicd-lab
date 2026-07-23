#!/usr/bin/env bash
# smoke-api.sh — valida todos os endpoints da labjavacicd
#
# Uso:
#   ./scripts/smoke-api.sh
#   BASE_URL=http://127.0.0.1:18080 ./scripts/smoke-api.sh
#
# Atenção (macOS + kind): a porta 8080 costuma estar ocupada pelo Ingress do kind
# (extraPortMappings). Para testar a app local via Maven, use outra porta:
#
#   # na raiz do repositório:
#   SERVER_PORT=18080 mvn -f app/pom.xml spring-boot:run
#
#   # ou já dentro de app/:
#   SERVER_PORT=18080 mvn spring-boot:run
#
#   BASE_URL=http://127.0.0.1:18080 ./scripts/smoke-api.sh
#
# Contra o cluster (imagem 0.3.0+ com Ingress):
#   BASE_URL=http://127.0.0.1:8080 ./scripts/smoke-api.sh
#
# Requisitos: curl, bash; jq é opcional (usa python3 como fallback).

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
BASE_URL="${BASE_URL%/}"

PASS=0
FAIL=0
TMP_DIR="$(mktemp -d)"
BODY_FILE="$TMP_DIR/body.json"
HDR_FILE="$TMP_DIR/headers.txt"
trap 'rm -rf "$TMP_DIR"' EXIT

RED=$'\033[0;31m'
GREEN=$'\033[0;32m'
YELLOW=$'\033[0;33m'
NC=$'\033[0m'

json_get() {
  local expr="$1"
  if command -v jq >/dev/null 2>&1; then
    jq -r "$expr" "$BODY_FILE"
  else
    python3 - "$expr" "$BODY_FILE" <<'PY'
import json, sys
expr, path = sys.argv[1], sys.argv[2]
data = json.load(open(path))
mapping = {
    ".status": lambda d: d.get("status", ""),
    ".message": lambda d: d.get("message", ""),
    ".code": lambda d: d.get("code", ""),
    ".id": lambda d: d.get("id", ""),
    ".title": lambda d: d.get("title", ""),
    ".createdOperations": lambda d: d.get("createdOperations", ""),
    ".listedOperations": lambda d: d.get("listedOperations", ""),
    ".tasksStored": lambda d: d.get("tasksStored", ""),
    ".details.title": lambda d: (d.get("details") or {}).get("title", ""),
}
if expr not in mapping:
    raise SystemExit(f"expressao nao suportada sem jq: {expr}")
print(mapping[expr](data))
PY
  fi
}

pass() {
  echo "${GREEN}PASS${NC}  $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "${RED}FAIL${NC}  $1"
  FAIL=$((FAIL + 1))
}

assert_eq() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    pass "$label"
  else
    fail "$label (esperado='$expected' obtido='$actual')"
  fi
}

assert_contains() {
  local label="$1"
  local needle="$2"
  local haystack="$3"
  if [[ "$haystack" == *"$needle"* ]]; then
    pass "$label"
  else
    fail "$label (nao encontrou '$needle')"
  fi
}

assert_http() {
  local label="$1"
  local expected_code="$2"
  local actual_code="$3"
  assert_eq "${label} [HTTP ${expected_code}]" "$expected_code" "$actual_code"
}

# request METHOD PATH [extra curl args...]
# Body JSON: passe --data '...'
# Headers: passe -H 'Name: value'
request() {
  local method="$1"
  local path="$2"
  shift 2
  local http_code
  http_code="$(curl -sS -D "$HDR_FILE" -o "$BODY_FILE" -w '%{http_code}' \
    -X "$method" \
    "$@" \
    "${BASE_URL}${path}")"
  printf '%s' "$http_code"
}

header_value() {
  local name="$1"
  local key
  key="$(printf '%s' "$name" | tr '[:upper:]' '[:lower:]')"
  awk -v key="$key" '
    BEGIN { FS=": " }
    {
      line = tolower($0)
      if (index(line, key ":") == 1) {
        sub(/\r$/, "", $2)
        print $2
        exit
      }
    }
  ' "$HDR_FILE"
}

echo "${YELLOW}==> Smoke API labjavacicd${NC}"
echo "BASE_URL=$BASE_URL"
echo

# 0) Conectividade
if ! curl -sS -o /dev/null --connect-timeout 3 "${BASE_URL}/actuator/health"; then
  fail "nao foi possivel conectar em ${BASE_URL}"
  echo "Suba a app (SERVER_PORT=18080 mvn spring-boot:run) ou use a imagem 0.3.0+ no cluster."
  exit 1
fi
pass "conectividade"

# Diagnóstico precoce: tasks do Módulo 09
probe_tasks="$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 3 "${BASE_URL}/api/tasks" || true)"
if [[ "$probe_tasks" == "404" ]]; then
  echo
  echo "${YELLOW}AVISO${NC}  GET /api/tasks retornou 404 neste BASE_URL."
  echo "Isso costuma significar:"
  echo "  1) porta 8080 = Ingress do kind com imagem antiga (ex.: labjavacicd:0.2.0 sem API de tasks)"
  echo "  2) ou app local antiga ainda em execução"
  echo
  echo "Corrija com uma das opções:"
  echo "  A) App local na 18080:"
  echo "       # na raiz do repo:"
  echo "       SERVER_PORT=18080 mvn -f app/pom.xml spring-boot:run"
  echo "       # ou dentro de app/:"
  echo "       SERVER_PORT=18080 mvn spring-boot:run"
  echo "       BASE_URL=http://127.0.0.1:18080 ./scripts/smoke-api.sh"
  echo "  B) Cluster com 0.3.0:"
  echo "       docker build -t labjavacicd:0.3.0 app/"
  echo "       kind load docker-image labjavacicd:0.3.0 --name lab"
  echo "       kubectl apply -k k8s/overlays/dev && kubectl rollout restart deploy/labjavacicd -n app"
  echo
fi
echo

# 1) Health
code="$(request GET /actuator/health)"
assert_http "GET /actuator/health" "200" "$code"
assert_eq "health.status=UP" "UP" "$(json_get .status)"
echo

# 2) Hello + request id gerado
code="$(request GET /api/hello)"
assert_http "GET /api/hello" "200" "$code"
assert_eq "hello.message" "Hello from labjavacicd" "$(json_get .message)"
assert_eq "hello.status" "ok" "$(json_get .status)"
req_id="$(header_value X-Request-Id)"
if [[ -n "$req_id" ]]; then
  pass "X-Request-Id gerado ($req_id)"
else
  fail "X-Request-Id ausente na resposta"
fi
echo

# 3) Hello + request id propagado
code="$(request GET /api/hello -H 'X-Request-Id: smoke-fixed-id')"
assert_http "GET /api/hello (request id fixo)" "200" "$code"
assert_eq "X-Request-Id propagado" "smoke-fixed-id" "$(header_value X-Request-Id)"
echo

# 4) Criar tarefa valida
code="$(request POST /api/tasks -H 'Content-Type: application/json' -H 'X-Request-Id: smoke-create-1' --data '{"title":"Smoke task A"}')"
assert_http "POST /api/tasks (valido)" "201" "$code"
assert_eq "task.title" "Smoke task A" "$(json_get .title)"
assert_eq "task.status" "TODO" "$(json_get .status)"
TASK_ID="$(json_get .id)"
if [[ -n "$TASK_ID" && "$TASK_ID" != "null" ]]; then
  pass "task.id gerado ($TASK_ID)"
else
  fail "task.id vazio"
  TASK_ID=""
fi
echo

# 5) Validacao — title curto
code="$(request POST /api/tasks -H 'Content-Type: application/json' --data '{"title":"ab"}')"
assert_http "POST /api/tasks (title curto)" "400" "$code"
assert_eq "validation code" "validation_failed" "$(json_get .code)"
assert_contains "validation details.title" "title" "$(json_get .details.title)"
echo

# 6) Validacao — title vazio
code="$(request POST /api/tasks -H 'Content-Type: application/json' --data '{"title":""}')"
assert_http "POST /api/tasks (title vazio)" "400" "$code"
assert_eq "validation code (vazio)" "validation_failed" "$(json_get .code)"
echo

# 7) Listagem
code="$(request GET /api/tasks)"
assert_http "GET /api/tasks" "200" "$code"
if [[ "$code" == "200" ]]; then
  if command -v jq >/dev/null 2>&1; then
    count="$(jq 'length' "$BODY_FILE")"
  else
    count="$(python3 -c 'import json; print(len(json.load(open("'"$BODY_FILE"'"))))')"
  fi
  if [[ "$count" -ge 1 ]]; then
    pass "listagem contem >= 1 tarefa (count=$count)"
  else
    fail "listagem vazia"
  fi
else
  echo "${YELLOW}HINT${NC}  /api/tasks retornou $code — use imagem/código do Módulo 09 (tasks) e BASE_URL correto"
fi
echo

# 8) Filtro status
code="$(request GET '/api/tasks?status=TODO')"
assert_http "GET /api/tasks?status=TODO" "200" "$code"
echo

# 9) Busca por id
if [[ -n "$TASK_ID" ]]; then
  code="$(request GET "/api/tasks/${TASK_ID}")"
  assert_http "GET /api/tasks/{id}" "200" "$code"
  assert_eq "getById.title" "Smoke task A" "$(json_get .title)"
else
  echo "${YELLOW}SKIP${NC}  GET /api/tasks/{id} (sem TASK_ID)"
fi
echo

# 10) Not found
code="$(request GET /api/tasks/does-not-exist-000)"
assert_http "GET /api/tasks/{id} inexistente" "404" "$code"
assert_eq "not_found code" "task_not_found" "$(json_get .code)"
echo

# 11) Stats
code="$(request GET /api/tasks/stats)"
assert_http "GET /api/tasks/stats" "200" "$code"
created="$(json_get .createdOperations)"
listed="$(json_get .listedOperations)"
stored="$(json_get .tasksStored)"
created="${created:-0}"
listed="${listed:-0}"
stored="${stored:-0}"
if [[ "$created" =~ ^[0-9]+$ && "$listed" =~ ^[0-9]+$ && "$stored" =~ ^[0-9]+$ \
  && "$created" -ge 1 && "$listed" -ge 1 && "$stored" -ge 1 ]]; then
  pass "stats coerentes (created=$created listed=$listed stored=$stored)"
else
  fail "stats inesperados (created=$created listed=$listed stored=$stored)"
fi
echo

TOTAL=$((PASS + FAIL))
echo "----------------------------------------"
echo "Resultado: ${GREEN}${PASS} passed${NC}, ${RED}${FAIL} failed${NC} (total $TOTAL)"
if [[ "$FAIL" -gt 0 ]]; then
  exit 1
fi
echo "${GREEN}Todas as funcionalidades validadas.${NC}"
