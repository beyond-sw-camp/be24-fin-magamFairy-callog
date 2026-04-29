<script setup>
import { computed, ref } from 'vue'

const activeTab = ref('documents')
const judgeInputType = ref('image')
const judgeText = ref(
  '프리미엄 고객만을 위한 특별 혜택이라는 표현을 강조하고, 경쟁사보다 확실히 저렴하다는 문구를 배너에 사용합니다.',
)

const tabs = [
  {
    id: 'documents',
    label: '가이드 문서',
    caption: 'Guidelines',
    count: 8,
    icon: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 19.5A2.5 2.5 0 0 0 6.5 22H20V6a2 2 0 0 0-2-2H6.5A2.5 2.5 0 0 0 4 6.5v13Z',
  },
  {
    id: 'references',
    label: '캠페인 레퍼런스',
    caption: 'References',
    count: 24,
    icon: 'M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3',
  },
  {
    id: 'judge',
    label: 'AI 판사',
    caption: 'Risk Review',
    count: 3,
    icon: 'M12 3v18M5 6h14M7 6l-4 7h8L7 6Zm10 0l-4 7h8l-4-7ZM5 21h14',
  },
]

const documents = [
  {
    title: '브랜드 톤앤매너 v2.1',
    category: '톤앤매너',
    owner: '브랜드팀',
    updatedAt: '2026.04.28',
    status: '필수',
    summary: '문장 길이, 호칭, 프리미엄 표현 강도, CTA 문구 기준을 정리한 최신 가이드입니다.',
    tags: ['카피', 'SNS', '배너'],
  },
  {
    title: '경품 및 혜택 고지 체크리스트',
    category: '주의사항',
    owner: '법무팀',
    updatedAt: '2026.04.27',
    status: '검수중',
    summary: '혜택 조건, 기간, 대상 제한, 유의사항 노출 규칙을 캠페인 소재 기준으로 정리했습니다.',
    tags: ['법률', '고지', '프로모션'],
  },
  {
    title: 'VIP 고객 커뮤니케이션 가이드',
    category: '가이드라인',
    owner: 'CRM팀',
    updatedAt: '2026.04.25',
    status: '필수',
    summary: '과장된 우월 표현을 피하고 초대, 선별, 감사의 뉘앙스를 유지하는 문서입니다.',
    tags: ['VIP', '문자', '이메일'],
  },
  {
    title: '이미지 사용 및 저작권 주의사항',
    category: '권리검토',
    owner: '콘텐츠팀',
    updatedAt: '2026.04.22',
    status: '참고',
    summary: '모델 초상권, 출처 표기, 2차 편집 가능 범위를 레퍼런스별로 확인하는 기준입니다.',
    tags: ['이미지', '저작권', '라이선스'],
  },
]

const principles = [
  '혜택 조건과 제한 사항은 같은 화면 안에서 확인 가능해야 합니다.',
  '경쟁사 비교 표현은 객관적 근거가 있는 경우에만 사용합니다.',
  '개인화 문구는 고객 동의 범위 안의 정보만 활용합니다.',
  '의료, 금융, 투자 수익처럼 오해를 만들 수 있는 단정형 표현은 금지합니다.',
]

const blockedTerms = ['무조건 1위', '전원 당첨', '업계 최저가 보장', '손실 없음', '100% 개선']

const references = [
  {
    type: '이미지',
    title: '봄 시즌 VIP 초대장 무드보드',
    source: 'Pinterest / 내부 큐레이션',
    usage: '배경 질감과 여백 비율만 참고',
    risk: '저',
    color: '#7c3aed',
  },
  {
    type: '랜딩',
    title: '프리미엄 멤버십 혜택 페이지',
    source: 'Luxury Retail Case',
    usage: '혜택 정보 구조와 CTA 위치 참고',
    risk: '중',
    color: '#0f766e',
  },
  {
    type: '카피',
    title: '초대형 캠페인 헤드라인 묶음',
    source: '2025 Q4 Archive',
    usage: '초대, 선공개, 한정감 표현 참고',
    risk: '중',
    color: '#b45309',
  },
  {
    type: '영상',
    title: '15초 숏폼 오프닝 레퍼런스',
    source: 'Brand Social Lab',
    usage: '첫 3초 제품 노출 방식 참고',
    risk: '저',
    color: '#2563eb',
  },
]

const judgeInputTypes = [
  { id: 'image', label: '이미지', accept: 'PNG, JPG' },
  { id: 'text', label: '텍스트', accept: 'Copy' },
  { id: 'pdf', label: 'PDF', accept: 'Brief' },
]

const reviewIssues = [
  {
    level: '높음',
    title: '비교 우위 표현 근거 필요',
    quote: '경쟁사보다 확실히 저렴',
    detail: '가격 비교는 기준 시점, 비교 대상, 산정 방식이 함께 제시되어야 합니다.',
  },
  {
    level: '중간',
    title: '혜택 대상 제한 고지 누락 가능성',
    quote: '프리미엄 고객만을 위한',
    detail: '프리미엄 고객의 정의와 선정 기준을 같은 소재 또는 연결 페이지에 노출해야 합니다.',
  },
  {
    level: '낮음',
    title: '톤앤매너 강도 조정 권장',
    quote: '특별 혜택',
    detail: 'VIP 캠페인은 희소성보다 초대와 감사의 뉘앙스를 우선하는 편이 안전합니다.',
  },
]

const selectedTab = computed(() => tabs.find((tab) => tab.id === activeTab.value) ?? tabs[0])
const selectedInputType = computed(
  () => judgeInputTypes.find((type) => type.id === judgeInputType.value) ?? judgeInputTypes[0],
)
</script>

<template>
  <section class="risk-workspace">
    <header class="risk-workspace__hero">
      <div>
        <p class="risk-workspace__eyebrow">Campaign Safety Desk</p>
        <h2>캠페인 검수 자료실</h2>
        <p>
          가이드 문서와 레퍼런스를 한곳에서 정리하고, 제작물의 표현 리스크를 AI 판사로
          빠르게 확인하는 작업 화면입니다.
        </p>
      </div>

      <div class="risk-workspace__summary" aria-label="검수 요약">
        <span>활성 캠페인</span>
        <strong>프리미엄 라이프스타일 공동 프로모션</strong>
        <small>검수 기준 8개 · 레퍼런스 24개 · 대기 3건</small>
      </div>
    </header>

    <nav class="risk-tabs" aria-label="검수 자료 탭">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="risk-tabs__item"
        :class="{ 'risk-tabs__item--active': activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        <span class="risk-tabs__icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path :d="tab.icon" />
          </svg>
        </span>
        <span>
          <strong>{{ tab.label }}</strong>
          <small>{{ tab.caption }}</small>
        </span>
        <em>{{ tab.count }}</em>
      </button>
    </nav>

    <main class="risk-workspace__content">
      <section v-if="activeTab === 'documents'" class="risk-panel risk-panel--split">
        <div class="risk-panel__main">
          <div class="risk-section-head">
            <div>
              <p>{{ selectedTab.caption }}</p>
              <h3>{{ selectedTab.label }}</h3>
            </div>
            <button type="button" class="risk-button risk-button--primary">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 5v14M5 12h14" />
              </svg>
              문서 추가
            </button>
          </div>

          <div class="document-list">
            <article v-for="document in documents" :key="document.title" class="document-card">
              <div class="document-card__top">
                <span>{{ document.category }}</span>
                <strong :class="`document-card__status document-card__status--${document.status}`">
                  {{ document.status }}
                </strong>
              </div>
              <h4>{{ document.title }}</h4>
              <p>{{ document.summary }}</p>
              <div class="document-card__meta">
                <span>{{ document.owner }}</span>
                <span>{{ document.updatedAt }}</span>
              </div>
              <div class="risk-chip-list">
                <span v-for="tag in document.tags" :key="tag">#{{ tag }}</span>
              </div>
            </article>
          </div>
        </div>

        <aside class="risk-panel__side">
          <section class="rule-box">
            <div class="rule-box__head">
              <h4>핵심 검수 원칙</h4>
              <span>4</span>
            </div>
            <ul>
              <li v-for="principle in principles" :key="principle">{{ principle }}</li>
            </ul>
          </section>

          <section class="rule-box rule-box--danger">
            <div class="rule-box__head">
              <h4>주의 표현</h4>
              <span>{{ blockedTerms.length }}</span>
            </div>
            <div class="blocked-terms">
              <span v-for="term in blockedTerms" :key="term">{{ term }}</span>
            </div>
          </section>
        </aside>
      </section>

      <section v-else-if="activeTab === 'references'" class="risk-panel">
        <div class="risk-section-head">
          <div>
            <p>{{ selectedTab.caption }}</p>
            <h3>{{ selectedTab.label }}</h3>
          </div>
          <div class="risk-actions">
            <button type="button" class="risk-button">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 6h18M7 12h10M10 18h4" />
              </svg>
              필터
            </button>
            <button type="button" class="risk-button risk-button--primary">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 5v14M5 12h14" />
              </svg>
              레퍼런스 추가
            </button>
          </div>
        </div>

        <div class="reference-grid">
          <article
            v-for="reference in references"
            :key="reference.title"
            class="reference-card"
          >
            <div class="reference-card__preview" :style="{ '--reference-color': reference.color }">
              <span>{{ reference.type }}</span>
            </div>
            <div class="reference-card__body">
              <div>
                <span class="reference-card__type">{{ reference.source }}</span>
                <h4>{{ reference.title }}</h4>
              </div>
              <p>{{ reference.usage }}</p>
              <footer>
                <span :class="`risk-badge risk-badge--${reference.risk}`">권리 리스크 {{ reference.risk }}</span>
                <button type="button" aria-label="레퍼런스 열기">
                  <svg viewBox="0 0 24 24">
                    <path d="M7 17 17 7M8 7h9v9" />
                  </svg>
                </button>
              </footer>
            </div>
          </article>
        </div>
      </section>

      <section v-else class="risk-panel risk-panel--judge">
        <div class="risk-section-head">
          <div>
            <p>{{ selectedTab.caption }}</p>
            <h3>{{ selectedTab.label }}</h3>
          </div>
          <button type="button" class="risk-button risk-button--primary">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="m5 12 4 4L19 6" />
            </svg>
            검수 실행
          </button>
        </div>

        <div class="judge-layout">
          <div class="judge-input">
            <div class="input-type-tabs">
              <button
                v-for="type in judgeInputTypes"
                :key="type.id"
                type="button"
                :class="{ 'input-type-tabs__item--active': judgeInputType === type.id }"
                @click="judgeInputType = type.id"
              >
                <strong>{{ type.label }}</strong>
                <span>{{ type.accept }}</span>
              </button>
            </div>

            <div v-if="judgeInputType === 'text'" class="text-review-box">
              <label for="judgeText">검수 문안</label>
              <textarea id="judgeText" v-model="judgeText" rows="9" />
            </div>

            <div v-else class="upload-box">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5-5 5 5M12 5v12" />
              </svg>
              <strong>{{ selectedInputType.label }} 파일 검수</strong>
              <span>{{ selectedInputType.accept }} 파일을 올려 캠페인 기준과 비교합니다.</span>
              <button type="button">파일 선택</button>
            </div>

            <div class="judge-checklist">
              <label>
                <input type="checkbox" checked />
                브랜드 톤앤매너 기준 포함
              </label>
              <label>
                <input type="checkbox" checked />
                법무 주의 표현 사전 포함
              </label>
              <label>
                <input type="checkbox" />
                레퍼런스 권리 범위 대조
              </label>
            </div>
          </div>

          <aside class="judge-result">
            <div class="judge-score">
              <span>예상 리스크</span>
              <strong>72</strong>
              <small>주의 필요</small>
            </div>

            <div class="issue-list">
              <article v-for="issue in reviewIssues" :key="issue.title" class="issue-card">
                <div>
                  <span :class="`issue-card__level issue-card__level--${issue.level}`">
                    {{ issue.level }}
                  </span>
                  <h4>{{ issue.title }}</h4>
                </div>
                <blockquote>{{ issue.quote }}</blockquote>
                <p>{{ issue.detail }}</p>
              </article>
            </div>
          </aside>
        </div>
      </section>
    </main>
  </section>
</template>

<style scoped>
.risk-workspace {
  display: flex;
  width: 100%;
  max-width: 1280px;
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

.risk-workspace__hero,
.risk-tabs,
.risk-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.risk-workspace__hero {
  display: flex;
  min-height: 112px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 24px;
}

.risk-workspace__eyebrow,
.risk-section-head p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.risk-workspace__hero h2,
.risk-section-head h3 {
  color: var(--text-primary);
  font-weight: 800;
  line-height: 1.2;
}

.risk-workspace__hero h2 {
  margin-top: 4px;
  font-size: 25px;
}

.risk-workspace__hero p:not(.risk-workspace__eyebrow) {
  max-width: 720px;
  margin-top: 8px;
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.65;
}

.risk-workspace__summary {
  display: grid;
  min-width: 280px;
  gap: 4px;
  padding: 15px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.risk-workspace__summary span,
.risk-workspace__summary small {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

.risk-workspace__summary strong {
  color: var(--text-primary);
  font-size: 14px;
}

.risk-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
}

.risk-tabs__item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 62px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.risk-tabs__item:hover,
.risk-tabs__item--active {
  border-color: color-mix(in srgb, var(--color-primary-300) 72%, var(--border-color));
  background: var(--accent-soft);
  color: var(--color-primary-700);
  box-shadow: var(--shadow-sm);
}

.risk-tabs__icon {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.risk-tabs__item--active .risk-tabs__icon {
  background: var(--color-primary-500);
  color: #ffffff;
}

.risk-tabs svg,
.risk-button svg,
.reference-card footer svg,
.upload-box svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.risk-tabs strong,
.risk-tabs small,
.risk-tabs em {
  display: block;
}

.risk-tabs strong {
  overflow: hidden;
  font-size: 14px;
  font-style: normal;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-tabs small {
  margin-top: 2px;
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 700;
}

.risk-tabs em {
  min-width: 26px;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--panel-color);
  color: inherit;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
  text-align: center;
}

.risk-panel {
  padding: 20px;
}

.risk-panel--split {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.risk-panel__main,
.risk-panel__side {
  min-width: 0;
}

.risk-panel__side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.risk-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.risk-section-head h3 {
  margin-top: 3px;
  font-size: 20px;
}

.risk-actions {
  display: flex;
  gap: 8px;
}

.risk-button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.risk-button:hover {
  border-color: var(--border-strong);
  background: var(--panel-muted);
  color: var(--text-primary);
}

.risk-button--primary {
  border-color: transparent;
  background: var(--color-primary-500);
  color: #ffffff;
}

.risk-button--primary:hover {
  background: var(--color-primary-600);
  color: #ffffff;
}

.document-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.document-card,
.rule-box,
.reference-card,
.issue-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
}

.document-card {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.document-card__top,
.document-card__meta,
.reference-card footer,
.rule-box__head,
.issue-card > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.document-card__top span,
.document-card__meta,
.reference-card__type {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.document-card h4,
.reference-card h4,
.rule-box h4,
.issue-card h4 {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.document-card p,
.reference-card p,
.issue-card p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.58;
}

.document-card__status {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 12px;
}

.document-card__status--필수 {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.document-card__status--검수중 {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.document-card__status--참고 {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.risk-chip-list,
.blocked-terms {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.risk-chip-list span,
.blocked-terms span {
  display: inline-flex;
  min-height: 25px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.rule-box {
  padding: 16px;
}

.rule-box__head {
  margin-bottom: 12px;
}

.rule-box__head span {
  display: inline-flex;
  min-width: 26px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 12px;
  font-weight: 800;
}

.rule-box ul {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.rule-box--danger {
  background: var(--danger-surface);
}

.rule-box--danger .rule-box__head span {
  background: color-mix(in srgb, var(--color-danger) 15%, white);
  color: var(--color-danger-dark);
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.reference-card {
  overflow: hidden;
}

.reference-card__preview {
  display: flex;
  min-height: 148px;
  align-items: flex-end;
  padding: 14px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--reference-color) 86%, #ffffff), #111827),
    var(--reference-color);
  color: #ffffff;
}

.reference-card__preview span {
  display: inline-flex;
  min-height: 26px;
  align-items: center;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  font-weight: 800;
}

.reference-card__body {
  display: grid;
  gap: 12px;
  padding: 15px;
}

.risk-badge {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 800;
}

.risk-badge--저 {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.risk-badge--중 {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.reference-card footer button {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
}

.risk-panel--judge {
  min-height: 600px;
}

.judge-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
}

.judge-input,
.judge-result {
  min-width: 0;
}

.input-type-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;
}

.input-type-tabs button {
  display: grid;
  gap: 2px;
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
}

.input-type-tabs__item--active {
  border-color: var(--color-primary-300) !important;
  background: var(--accent-soft) !important;
  color: var(--color-primary-700) !important;
}

.input-type-tabs strong {
  font-size: 13px;
  font-weight: 800;
}

.input-type-tabs span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.upload-box,
.text-review-box {
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.upload-box {
  display: grid;
  min-height: 250px;
  place-items: center;
  align-content: center;
  gap: 9px;
  padding: 32px;
  color: var(--muted-text);
  text-align: center;
}

.upload-box svg {
  width: 34px;
  height: 34px;
}

.upload-box strong {
  color: var(--text-primary);
  font-size: 16px;
}

.upload-box span {
  font-size: 13px;
}

.upload-box button {
  min-height: 34px;
  margin-top: 8px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  background: var(--text-primary);
  color: var(--panel-color);
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
}

.text-review-box {
  display: grid;
  gap: 9px;
  padding: 14px;
}

.text-review-box label {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.text-review-box textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 14px;
  font-size: 14px;
  line-height: 1.7;
}

.judge-checklist {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.judge-checklist label {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.judge-checklist input {
  accent-color: var(--color-primary-500);
}

.judge-result {
  display: grid;
  align-content: start;
  gap: 14px;
}

.judge-score {
  display: grid;
  justify-items: center;
  gap: 3px;
  padding: 22px;
  border: 1px solid color-mix(in srgb, var(--color-danger) 22%, var(--border-color));
  border-radius: var(--radius-md);
  background: var(--danger-surface);
  text-align: center;
}

.judge-score span,
.judge-score small {
  color: var(--color-danger-dark);
  font-size: 12px;
  font-weight: 800;
}

.judge-score strong {
  color: var(--danger-text-strong);
  font-size: 54px;
  font-weight: 900;
  line-height: 1;
}

.issue-list {
  display: grid;
  gap: 10px;
}

.issue-card {
  display: grid;
  gap: 9px;
  padding: 14px;
}

.issue-card__level {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 900;
}

.issue-card__level--높음 {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.issue-card__level--중간 {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.issue-card__level--낮음 {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.issue-card blockquote {
  margin: 0;
  padding: 9px 10px;
  border-left: 3px solid var(--border-strong);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .risk-panel--split,
  .judge-layout {
    grid-template-columns: 1fr;
  }

  .reference-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .risk-panel__side {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .risk-workspace__hero,
  .risk-section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .risk-workspace__summary {
    width: 100%;
    min-width: 0;
  }

  .risk-tabs,
  .document-list,
  .reference-grid,
  .risk-panel__side,
  .judge-checklist {
    grid-template-columns: 1fr;
  }

  .risk-actions,
  .risk-button {
    width: 100%;
  }
}
</style>
