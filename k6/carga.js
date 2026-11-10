// Teste de carga da clínica com K6.
// Simula psicólogas, financeiro e pacientes usando o sistema ao mesmo tempo:
// cada usuário virtual entra (com CSRF), navega pelas páginas do seu perfil e sai.
//
//   k6 run k6/carga.js                         (aplicação em http://localhost:8080)
//   k6 run -e BASE=http://localhost:8081 k6/carga.js   (no cluster kind)
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE || 'http://localhost:8080';
const SENHA = {
  psicologa: __ENV.SENHA_PSICOLOGA || 'psicologa123',
  financeiro: __ENV.SENHA_FINANCEIRO || 'financeiro123',
  'PAC-DEMO0001': __ENV.SENHA_PACIENTE || 'paciente123',
};

// Tempo só do login: é a operação mais pesada (BCrypt é lento de propósito)
const tempoLogin = new Trend('tempo_login', true);

export const options = {
  scenarios: {
    psicologas: { executor: 'ramping-vus', exec: 'psicologa', startVUs: 0,
      stages: [{ duration: '20s', target: 10 }, { duration: '40s', target: 10 }, { duration: '10s', target: 0 }] },
    financeiro: { executor: 'constant-vus', exec: 'financeiro', vus: 3, duration: '70s' },
    pacientes:  { executor: 'ramping-vus', exec: 'paciente', startVUs: 0,
      stages: [{ duration: '20s', target: 5 }, { duration: '40s', target: 5 }, { duration: '10s', target: 0 }] },
  },
  // Critérios de aprovação: se algum falhar, o K6 termina com erro e o CI fica vermelho
  thresholds: {
    http_req_failed: ['rate<0.01'],                 // menos de 1% de erros
    'http_req_duration{tipo:pagina}': ['p(95)<800'], // 95% das páginas em menos de 800 ms
    tempo_login: ['p(95)<2000'],                    // 95% dos logins em menos de 2 s
    checks: ['rate>0.99'],                          // mais de 99% das verificações certas
  },
};

function csrfDe(res) {
  const m = res.body && res.body.match(/name="_csrf"[^>]*value="([^"]+)"|value="([^"]+)"[^>]*name="_csrf"/);
  return m ? (m[1] || m[2]) : '';
}

function entrar(usuario) {
  const tela = http.get(`${BASE}/login`, { tags: { tipo: 'pagina' } });
  check(tela, { 'tela de login abre': (r) => r.status === 200 });
  const res = http.post(`${BASE}/login`,
    { username: usuario, password: SENHA[usuario], _csrf: csrfDe(tela) },
    { redirects: 0, tags: { tipo: 'login' } });
  tempoLogin.add(res.timings.duration);
  check(res, { 'login aceito': (r) => r.status === 302 && !(r.headers.Location || '').includes('error') });
}

function pagina(caminho, texto) {
  const res = http.get(`${BASE}${caminho}`, { tags: { tipo: 'pagina', name: caminho } });
  check(res, { [`${caminho} abre`]: (r) => r.status === 200,
               [`${caminho} mostra "${texto}"`]: (r) => (r.body || '').includes(texto) });
  return res;
}

function sair() {
  const tela = http.get(`${BASE}/login`, { tags: { tipo: 'pagina' } });
  const res = http.post(`${BASE}/logout`, { _csrf: csrfDe(tela) }, { redirects: 0, tags: { tipo: 'logout' } });
  check(res, { 'logout': (r) => r.status === 302 });
}

export function psicologa() {
  group('psicóloga', () => {
    entrar('psicologa');
    pagina('/clinico/pacientes', 'Ana Exemplo');
    sleep(1);
    pagina('/financeiro/cobrancas', 'Cobranças');
    sleep(1);
    sair();
  });
}

export function financeiro() {
  group('financeiro', () => {
    entrar('financeiro');
    pagina('/financeiro/cobrancas', 'Cobranças');
    const csv = http.get(`${BASE}/financeiro/cobrancas/exportar`, { tags: { tipo: 'pagina', name: 'exportar CSV' } });
    check(csv, { 'CSV exportado': (r) => r.status === 200 && (r.headers['Content-Type'] || '').includes('text/csv') });
    sleep(2);
    sair();
  });
}

export function paciente() {
  group('paciente', () => {
    entrar('PAC-DEMO0001');
    pagina('/paciente', 'Ana Exemplo');
    sleep(2);
    sair();
  });
}

// Resumo em Markdown (aparece no resumo do GitHub Actions) + JSON completo
export function handleSummary(data) {
  const m = data.metrics;
  const v = (nome, campo) => (m[nome] && m[nome].values[campo] !== undefined ? m[nome].values[campo] : NaN);
  const ms = (x) => `${x.toFixed(0)} ms`;
  const ok = (nome) => Object.values((m[nome] && m[nome].thresholds) || {}).every((t) => t.ok) ? '✅' : '❌';
  const linhas = [
    '## Teste de carga (K6)',
    '',
    `Até **${v('vus_max', 'value')} usuários simultâneos** (psicólogas, financeiro e pacientes), ` +
      `**${v('http_reqs', 'count')} requisições** em ${(data.state.testRunDurationMs / 1000).toFixed(0)} s ` +
      `(${v('http_reqs', 'rate').toFixed(1)} por segundo).`,
    '',
    '| Critério | Limite | Resultado | |',
    '|---|---|---|---|',
    `| Requisições com erro | < 1% | ${(v('http_req_failed', 'rate') * 100).toFixed(2)}% | ${ok('http_req_failed')} |`,
    `| Páginas: 95% respondem em | < 800 ms | ${ms(v('http_req_duration{tipo:pagina}', 'p(95)'))} | ${ok('http_req_duration{tipo:pagina}')} |`,
    `| Login: 95% respondem em | < 2000 ms | ${ms(v('tempo_login', 'p(95)'))} | ${ok('tempo_login')} |`,
    `| Verificações corretas | > 99% | ${(v('checks', 'rate') * 100).toFixed(2)}% | ${ok('checks')} |`,
    '',
    `Tempo médio por requisição: ${ms(v('http_req_duration', 'avg'))} · mediana: ${ms(v('http_req_duration', 'med'))} · máximo: ${ms(v('http_req_duration', 'max'))}`,
    '',
  ];
  return { stdout: linhas.join('\n') + '\n', 'relatorio-k6.md': linhas.join('\n') + '\n', 'relatorio-k6.json': JSON.stringify(data, null, 2) };
}
