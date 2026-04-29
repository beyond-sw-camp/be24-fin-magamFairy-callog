<script setup>
import { computed, ref } from 'vue'

const isAnalysisOpen = ref(false)
const selectedAnalysisFile = ref(null)

const campaign = {
  title: '파트너 공동 혜택 프로모션',
  period: '2026.05.01 - 2026.05.31',
  owner: '제휴운영팀',
}

const reviewStats = [
  { label: '검수 대기', value: 5, detail: '오늘 처리 권장', tone: 'warning' },
  { label: '승인 대기', value: 2, detail: '브랜드 1 · 법무 1', tone: 'approval' },
  { label: '오픈 차단', value: 1, detail: '로고 권리 확인 필요', tone: 'danger' },
  { label: '사용 가능', value: 8, detail: '최종 승인 완료', tone: 'ready' },
]

const reviewQueue = [
  {
    title: '메인 배너 1차 시안',
    type: '배너',
    request: '혜택 문구와 파트너 로고 병기 검수',
    issue: '최대 30% 표현 근거 확인 필요',
    owner: '브랜드팀',
    due: '오늘 16:00',
    status: '검토 필요',
    tone: 'warning',
  },
  {
    title: '파트너 제안서 공유본',
    type: '제안서',
    request: '외부 공유 가능 범위 확인',
    issue: '정산 조건표 삭제 후 공유 가능',
    owner: '제휴운영팀',
    due: '오늘 18:00',
    status: '수정 요청',
    tone: 'danger',
  },
  {
    title: '랜딩페이지 혜택 영역',
    type: '랜딩',
    request: '대상/기간/유의사항 노출 확인',
    issue: '하단 유의사항 링크 연결 필요',
    owner: '마케팅팀',
    due: '내일 11:00',
    status: '진행 중',
    tone: 'approval',
  },
  {
    title: '혜택 적용 FAQ',
    type: 'FAQ',
    request: '고객 문의 응대 문구 확인',
    issue: '혜택 재발급 조건만 보강',
    owner: 'CS운영팀',
    due: '내일 15:00',
    status: '사용 가능',
    tone: 'ready',
  },
]

const approvalMatrix = [
  {
    area: '혜택/가격',
    rule: '할인율, 쿠폰 조건, 적용 기간이 같은 화면에 노출되어야 합니다.',
    approver: '제휴운영',
    status: '확인 중',
  },
  {
    area: '파트너 로고',
    rule: '로고 색상, 여백, 병기 위치는 파트너 가이드 기준을 따릅니다.',
    approver: '브랜드',
    status: '승인 대기',
  },
  {
    area: '외부 공유',
    rule: '정산 조건, 내부 성과, 고객 데이터는 승인 없이는 포함하지 않습니다.',
    approver: '법무',
    status: '수정 요청',
  },
  {
    area: '고객 응대',
    rule: '혜택 적용 기준과 예외 케이스는 동일한 답변 기준을 사용합니다.',
    approver: 'CS운영',
    status: '승인 완료',
  },
]

const approvalSteps = [
  { step: '01', title: '검수 요청', desc: '소재와 요청 사유 등록' },
  { step: '02', title: '담당 검토', desc: '기준 자료와 표현 리스크 확인' },
  { step: '03', title: '승인/반려', desc: '승인자 의견과 수정 요청 기록' },
  { step: '04', title: '오픈 확정', desc: '최종 승인 후 운영 일정 반영' },
]

const analysisIssues = [
  {
    category: '요구사항',
    title: '혜택 수치 근거 확인 필요',
    source: '캠페인 요구사항: 혜택 조건과 적용 기준 동시 표기',
    target: '"최대 30% 혜택"',
    detail: '최대 할인율을 쓰려면 적용 조건, 제외 대상, 산정 기준이 같은 소재 또는 랜딩에 함께 노출되어야 합니다.',
  },
  {
    category: '가이드라인',
    title: '파트너 로고 사용 승인 필요',
    source: '파트너 브랜드 가이드: 로고 병기 및 메인 영역 노출 승인',
    target: '메인 배너 우측 상단 로고 병기',
    detail: '파트너 로고가 메인 히어로 영역에 노출되므로 브랜드 승인 이력이 확인되어야 합니다.',
  },
  {
    category: '법적/고지',
    title: '유의사항 가독성 보강 필요',
    source: '소비자 고지 기준: 제한 조건의 명확한 고지',
    target: '하단 작은 안내 문구',
    detail: '모바일 배너에서 유의사항이 작게 보여 혜택 제한 조건을 충분히 인지하기 어렵습니다.',
  },
]

const analysisChecks = [
  { label: '가이드라인', status: '확인 필요', tone: 'warning' },
  { label: '요구사항', status: '위반 의심', tone: 'danger' },
  { label: '법적/고지', status: '보강 필요', tone: 'approval' },
  { label: '금지어/민감표현', status: '이상 없음', tone: 'ready' },
]

const analysisHasIssues = computed(() => analysisIssues.length > 0)

const analysisFileInfo = computed(() => {
  if (!selectedAnalysisFile.value) {
    return null
  }

  const fileSize = selectedAnalysisFile.value.size / 1024 / 1024

  return {
    name: selectedAnalysisFile.value.name,
    size: `${fileSize.toFixed(fileSize >= 1 ? 1 : 2)}MB`,
    type: selectedAnalysisFile.value.type || '파일 형식 자동 감지',
  }
})

function openAnalysisRequest() {
  isAnalysisOpen.value = true
}

function closeAnalysisRequest() {
  isAnalysisOpen.value = false
}

function handleAnalysisFileChange(event) {
  const [file] = event.target.files ?? []
  selectedAnalysisFile.value = file ?? null
}
</script>

<template>
  <section class="approval-page">
    <header class="approval-hero">
      <div>
        <p>Review & Approval</p>
        <h2>검수/승인</h2>
        <span>{{ campaign.title }} · {{ campaign.period }}</span>
      </div>
      <div class="approval-hero__owner">
        <span>담당 조직</span>
        <strong>{{ campaign.owner }}</strong>
        <small>오픈 전 검수 현황</small>
      </div>
    </header>

    <section class="approval-stats" aria-label="검수 승인 현황">
      <article
        v-for="stat in reviewStats"
        :key="stat.label"
        :class="`approval-stat approval-stat--${stat.tone}`"
      >
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <p>{{ stat.detail }}</p>
      </article>
    </section>

    <section v-if="isAnalysisOpen" class="analysis-workspace">
      <div class="analysis-head">
        <div>
          <p>AI Risk Review</p>
          <h3>AI 위험 분석 요청</h3>
          <span>파일을 올리면 캠페인 자료실의 기준과 비교해 위험 요소를 먼저 표시합니다.</span>
        </div>
        <button type="button" class="ghost-button" @click="closeAnalysisRequest">목록으로</button>
      </div>

      <div class="analysis-layout">
        <section class="analysis-upload">
          <div class="upload-box">
            <input
              id="analysisFile"
              class="upload-box__input"
              type="file"
              accept="image/*,.pdf,.ppt,.pptx,.doc,.docx"
              @change="handleAnalysisFileChange"
            />
            <label for="analysisFile" class="upload-box__label">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5-5 5 5M12 5v12" />
              </svg>
              <strong>검수할 파일 업로드</strong>
              <span>배너 이미지, 랜딩 PDF, 제안서, FAQ 문서를 올릴 수 있습니다.</span>
            </label>
          </div>

          <div class="analysis-file-card">
            <template v-if="analysisFileInfo">
              <span>업로드 파일</span>
              <strong>{{ analysisFileInfo.name }}</strong>
              <p>{{ analysisFileInfo.size }} · {{ analysisFileInfo.type }}</p>
            </template>
            <template v-else>
              <span>대기 중</span>
              <strong>선택된 파일이 없습니다</strong>
              <p>파일 선택 후 AI 위험 분석 결과가 표시됩니다.</p>
            </template>
          </div>

          <div class="analysis-request-form">
            <label>
              <span>요청 유형</span>
              <select>
                <option>외부 공개 전 최종 검수</option>
                <option>파트너 전달 자료 검수</option>
                <option>브랜드/로고 사용 검수</option>
              </select>
            </label>
            <label>
              <span>검수 메모</span>
              <textarea rows="4" placeholder="예: 메인 배너의 혜택 문구와 파트너 로고 사용 가능 여부를 확인해주세요." />
            </label>
          </div>
        </section>

        <aside class="analysis-result">
          <div
            class="analysis-verdict"
            :class="{
              'analysis-verdict--empty': !analysisFileInfo,
              'analysis-verdict--clear': analysisFileInfo && !analysisHasIssues,
            }"
          >
            <span>AI 1차 판단</span>
            <strong>
              {{
                !analysisFileInfo
                  ? '파일 업로드 대기'
                  : analysisHasIssues
                    ? '확인 필요한 항목 발견'
                    : '이상 없음'
              }}
            </strong>
            <p>
              {{
                !analysisFileInfo
                  ? '파일을 올리면 기준 자료와 비교해 위반 의심 항목만 표시합니다.'
                  : analysisHasIssues
                    ? '가이드라인, 요구사항, 법적 고지 기준에 걸리는 부분을 확인해주세요.'
                    : '가이드라인, 요구사항, 법적 리스크에 걸리는 항목이 발견되지 않았습니다.'
              }}
            </p>
          </div>

          <div class="analysis-check-grid">
            <article
              v-for="check in analysisChecks"
              :key="check.label"
              :class="`analysis-check analysis-check--${check.tone}`"
            >
              <span>{{ check.label }}</span>
              <strong>{{ analysisFileInfo ? check.status : '대기' }}</strong>
            </article>
          </div>

          <div class="analysis-issues">
            <h4>확인 필요한 부분</h4>
            <template v-if="analysisFileInfo && analysisHasIssues">
              <article v-for="issue in analysisIssues" :key="issue.title" class="analysis-issue">
                <span>{{ issue.category }}</span>
                <strong>{{ issue.title }}</strong>
                <small>{{ issue.source }}</small>
                <blockquote>{{ issue.target }}</blockquote>
                <p>{{ issue.detail }}</p>
              </article>
            </template>
            <article v-else-if="analysisFileInfo" class="analysis-clear">
              <strong>이상 없음</strong>
              <p>등록된 가이드라인, 캠페인 요구사항, 법적 고지 기준에 어긋나는 부분이 발견되지 않았습니다.</p>
            </article>
            <article v-else class="analysis-clear analysis-clear--empty">
              <strong>분석 대기</strong>
              <p>파일 업로드 후 AI가 확인이 필요한 부분만 추려서 표시합니다.</p>
            </article>
          </div>

          <button type="button" class="approval-button analysis-submit" :disabled="!analysisFileInfo">
            검수 요청 생성
          </button>
        </aside>
      </div>
    </section>

    <section v-else class="approval-layout">
      <main class="approval-layout__main">
        <section class="approval-panel">
          <div class="approval-panel__head">
            <div>
              <p>Review Queue</p>
              <h3>검수 대기열</h3>
            </div>
            <button type="button" class="approval-button" @click="openAnalysisRequest">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 5v14M5 12h14" />
              </svg>
              요청 등록
            </button>
          </div>

          <div class="queue-list">
            <article v-for="item in reviewQueue" :key="item.title" class="queue-item">
              <div class="queue-item__main">
                <span>{{ item.type }}</span>
                <strong>{{ item.title }}</strong>
                <p>{{ item.request }}</p>
              </div>
              <div class="queue-item__issue">
                <small>확인 필요</small>
                <span>{{ item.issue }}</span>
              </div>
              <div class="queue-item__meta">
                <span>{{ item.owner }}</span>
                <strong>{{ item.due }}</strong>
              </div>
              <em :class="`status-chip status-chip--${item.tone}`">{{ item.status }}</em>
            </article>
          </div>
        </section>

        <section class="approval-panel">
          <div class="approval-panel__head">
            <div>
              <p>Approval Matrix</p>
              <h3>승인 기준</h3>
            </div>
          </div>

          <div class="approval-table">
            <div class="approval-table__row approval-table__row--head">
              <span>항목</span>
              <span>검수 기준</span>
              <span>승인자</span>
              <span>상태</span>
            </div>
            <div v-for="rule in approvalMatrix" :key="rule.area" class="approval-table__row">
              <strong>{{ rule.area }}</strong>
              <p>{{ rule.rule }}</p>
              <span>{{ rule.approver }}</span>
              <em>{{ rule.status }}</em>
            </div>
          </div>
        </section>
      </main>

      <aside class="approval-layout__side">
        <section class="approval-panel">
          <div class="approval-panel__head">
            <div>
              <p>Workflow</p>
              <h3>처리 흐름</h3>
            </div>
          </div>
          <div class="approval-flow">
            <article v-for="step in approvalSteps" :key="step.step">
              <span>{{ step.step }}</span>
              <strong>{{ step.title }}</strong>
              <p>{{ step.desc }}</p>
            </article>
          </div>
        </section>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.approval-page {
  display: flex;
  width: 100%;
  max-width: 1280px;
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

.approval-hero {
  display: flex;
  min-height: 132px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.approval-hero p,
.approval-panel__head p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.approval-hero h2 {
  margin-top: 5px;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 950;
}

.approval-hero span {
  display: block;
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.approval-hero__owner {
  display: grid;
  min-width: 220px;
  gap: 4px;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.approval-hero__owner span,
.approval-hero__owner small {
  margin: 0;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.approval-hero__owner strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 950;
}

.approval-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.approval-stat,
.approval-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.approval-stat {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.approval-stat span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.approval-stat strong {
  color: var(--text-primary);
  font-size: 30px;
  font-weight: 950;
  line-height: 1;
}

.approval-stat p,
.queue-item p,
.approval-table p,
.approval-flow p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.approval-stat--danger {
  background: var(--danger-surface);
  border-color: color-mix(in srgb, var(--color-danger) 32%, var(--border-color));
}

.approval-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.approval-layout__main,
.approval-layout__side {
  display: grid;
  min-width: 0;
  align-content: start;
  gap: 16px;
}

.approval-panel {
  display: grid;
  gap: 13px;
  padding: 16px;
}

.approval-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.approval-panel__head h3 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 950;
}

.approval-button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: var(--color-primary-500);
  color: #ffffff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.approval-button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.approval-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.ghost-button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  justify-content: center;
  padding: 0 13px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.analysis-workspace {
  display: grid;
  gap: 16px;
}

.analysis-head,
.analysis-upload,
.analysis-result {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.analysis-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
}

.analysis-head p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.analysis-head h3 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 950;
}

.analysis-head span {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}

.analysis-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr);
  gap: 16px;
}

.analysis-upload,
.analysis-result {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 16px;
}

.upload-box__input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.upload-box__label {
  display: grid;
  min-height: 220px;
  place-items: center;
  align-content: center;
  gap: 9px;
  padding: 28px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  text-align: center;
}

.upload-box__label svg {
  width: 34px;
  height: 34px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.upload-box__label strong,
.analysis-file-card strong,
.analysis-issues h4,
.analysis-issue strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 950;
}

.upload-box__label span,
.analysis-file-card p,
.analysis-issue p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.analysis-file-card {
  display: grid;
  gap: 5px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.analysis-file-card span,
.analysis-request-form span,
.analysis-verdict span,
.analysis-check span,
.analysis-issue span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.analysis-request-form {
  display: grid;
  gap: 12px;
}

.analysis-request-form label {
  display: grid;
  gap: 7px;
}

.analysis-request-form select,
.analysis-request-form textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}

.analysis-request-form select {
  min-height: 38px;
  padding: 0 11px;
}

.analysis-request-form textarea {
  resize: vertical;
  padding: 11px;
  line-height: 1.55;
}

.analysis-verdict {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--color-danger) 22%, var(--border-color));
  border-radius: var(--radius-md);
  background: var(--danger-surface);
}

.analysis-verdict--empty {
  border-color: var(--border-color);
  background: var(--panel-muted);
}

.analysis-verdict--clear {
  border-color: color-mix(in srgb, var(--color-success) 34%, var(--border-color));
  background: color-mix(in srgb, var(--color-success) 12%, var(--panel-color));
}

.analysis-verdict strong {
  color: var(--danger-text-strong);
  font-size: 22px;
  font-weight: 950;
}

.analysis-verdict--empty strong {
  color: var(--muted-text);
}

.analysis-verdict--clear strong {
  color: var(--color-success);
}

.analysis-verdict p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.analysis-check-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.analysis-check {
  display: grid;
  gap: 4px;
  min-height: 74px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.analysis-check strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.analysis-check--ready {
  border-color: color-mix(in srgb, var(--color-success) 28%, var(--border-color));
}

.analysis-check--approval {
  border-color: color-mix(in srgb, var(--color-primary-300) 48%, var(--border-color));
}

.analysis-check--warning {
  border-color: color-mix(in srgb, var(--color-warning) 36%, var(--border-color));
}

.analysis-check--danger {
  border-color: color-mix(in srgb, var(--color-danger) 30%, var(--border-color));
}

.analysis-issues {
  display: grid;
  gap: 9px;
}

.analysis-issue {
  display: grid;
  gap: 7px;
  padding: 13px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.analysis-issue small {
  color: var(--muted-text);
  font-size: 12px;
  line-height: 1.45;
}

.analysis-issue blockquote {
  margin: 0;
  padding: 8px 10px;
  border-left: 3px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
}

.analysis-clear {
  display: grid;
  gap: 6px;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--color-success) 34%, var(--border-color));
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-success) 12%, var(--panel-color));
}

.analysis-clear--empty {
  border-color: var(--border-color);
  background: var(--panel-muted);
}

.analysis-clear strong {
  color: var(--color-success);
  font-size: 15px;
  font-weight: 950;
}

.analysis-clear--empty strong {
  color: var(--text-primary);
}

.analysis-clear p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.analysis-submit {
  width: 100%;
}

.queue-list {
  display: grid;
  gap: 8px;
}

.queue-item {
  display: grid;
  grid-template-columns: minmax(190px, 1.2fr) minmax(180px, 1fr) minmax(110px, 0.58fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.queue-item__main,
.queue-item__issue,
.queue-item__meta {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.queue-item__main > span {
  width: fit-content;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--panel-color);
  color: var(--color-primary-700);
  font-size: 11px;
  font-weight: 950;
}

.queue-item strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.queue-item__issue small,
.queue-item__meta span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.queue-item__issue span,
.queue-item__meta strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-chip {
  display: inline-flex;
  width: fit-content;
  min-height: 25px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-style: normal;
  font-weight: 950;
  white-space: nowrap;
}

.status-chip--ready {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.status-chip--approval {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.status-chip--warning {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.status-chip--danger {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.approval-table {
  display: grid;
  overflow-x: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

.approval-table__row {
  display: grid;
  grid-template-columns: 110px minmax(260px, 1fr) 88px 96px;
  gap: 12px;
  min-width: 680px;
  align-items: center;
  padding: 12px;
  border-top: 1px solid var(--border-color);
}

.approval-table__row:first-child {
  border-top: 0;
}

.approval-table__row--head {
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 950;
}

.approval-table strong,
.approval-table span,
.approval-table em {
  color: var(--text-primary);
  font-size: 13px;
  font-style: normal;
  font-weight: 900;
}

.approval-flow {
  display: grid;
  gap: 9px;
}

.approval-flow article {
  display: grid;
  gap: 5px;
  padding: 13px;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.approval-flow span {
  color: var(--color-primary-600);
  font-size: 12px;
  font-weight: 950;
}

.approval-flow strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 950;
}

@media (max-width: 1180px) {
  .approval-layout,
  .analysis-layout {
    grid-template-columns: 1fr;
  }

  .approval-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .queue-item {
    grid-template-columns: minmax(0, 1fr) minmax(180px, 0.9fr);
  }
}

@media (max-width: 860px) {
  .approval-hero,
  .approval-panel__head,
  .analysis-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .approval-hero__owner,
  .approval-button,
  .ghost-button {
    width: 100%;
  }

  .approval-stats,
  .queue-item,
  .analysis-check-grid {
    grid-template-columns: 1fr;
  }
}
</style>
