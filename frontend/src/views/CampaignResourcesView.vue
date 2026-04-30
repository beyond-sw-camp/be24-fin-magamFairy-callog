<script setup>
import { computed, ref } from 'vue'

const activeCategory = ref('전체')

const campaign = {
  title: '파트너 공동 혜택 프로모션',
  partner: 'A-브랜드 제휴',
  period: '2026.05.01 - 2026.05.31',
  owner: '제휴운영팀',
}

const categories = ['전체', '가이드라인', '톤앤매너', '위험요소', '금지어', '레퍼런스', '운영자료']

const resourceStats = [
  { label: '등록 자료', value: 18, detail: '캠페인 기준 자료' },
  { label: '필수 확인', value: 6, detail: '오픈 전 확인 필요' },
  { label: '최근 갱신', value: 4, detail: '이번 주 업데이트' },
]

const pinnedResources = [
  {
    title: '캠페인 운영 브리프',
    category: '가이드라인',
    owner: '제휴운영팀',
    updatedAt: '2026.04.29',
    summary: '캠페인 목표, 파트너 역할, 혜택 구조, 오픈 범위를 한 장으로 정리한 기준 문서입니다.',
    tags: ['목표', '범위', '역할'],
    tone: 'primary',
  },
  {
    title: '파트너 제안용 핵심 메시지',
    category: '톤앤매너',
    owner: '브랜드팀',
    updatedAt: '2026.04.28',
    summary: '파트너에게 전달할 서비스 가치, 표현 톤, 피해야 할 문장을 정리했습니다.',
    tags: ['카피', '제안', '메시지'],
    tone: 'success',
  },
  {
    title: '외부 공유 불가 자료 목록',
    category: '위험요소',
    owner: '법무팀',
    updatedAt: '2026.04.27',
    summary: '정산 조건, 내부 성과, 고객 데이터처럼 외부 공유가 제한되는 자료 기준입니다.',
    tags: ['법무', '공유범위', '주의'],
    tone: 'danger',
  },
]

const resources = [
  {
    title: '제휴 혜택 표기 가이드',
    category: '가이드라인',
    type: '문서',
    owner: '제휴운영팀',
    updatedAt: '2026.04.29',
    status: '필수',
    summary: '할인율, 쿠폰, 적립 혜택을 배너와 랜딩에서 표기하는 기본 기준입니다.',
    tags: ['혜택', '기간', '조건'],
  },
  {
    title: '브랜드 톤앤매너 문장 예시',
    category: '톤앤매너',
    type: '문서',
    owner: '브랜드팀',
    updatedAt: '2026.04.28',
    status: '필수',
    summary: '파트너 공동 캠페인에서 쓸 수 있는 문장과 피해야 할 문장을 예시로 정리했습니다.',
    tags: ['문구', 'CTA', '제휴'],
  },
  {
    title: '위험 표현 및 법무 체크포인트',
    category: '위험요소',
    type: '체크리스트',
    owner: '법무팀',
    updatedAt: '2026.04.27',
    status: '필수',
    summary: '비교 우위, 보장성 표현, 개인정보 관련 문구를 확인하는 기준입니다.',
    tags: ['법무', '개인정보', '비교표현'],
  },
  {
    title: '금지어와 대체 표현',
    category: '금지어',
    type: '리스트',
    owner: '법무팀',
    updatedAt: '2026.04.27',
    status: '필수',
    summary: '무조건 승인, 업계 최저가, 전원 혜택 등 사용 금지 표현과 대체 문구입니다.',
    tags: ['금지어', '대체문구', '배너'],
  },
  {
    title: '멤버십 랜딩 구조 레퍼런스',
    category: '레퍼런스',
    type: '링크',
    owner: '마케팅팀',
    updatedAt: '2026.04.26',
    status: '참고',
    summary: '혜택, 대상, 유의사항을 같은 화면에서 보여주는 랜딩 구조 참고 자료입니다.',
    tags: ['랜딩', 'CTA', '혜택표기'],
  },
  {
    title: '공동 프로모션 배너 레퍼런스',
    category: '레퍼런스',
    type: '이미지',
    owner: '브랜드팀',
    updatedAt: '2026.04.26',
    status: '참고',
    summary: '파트너 로고 병기와 혜택 문구 배치를 확인하기 위한 배너 레퍼런스입니다.',
    tags: ['배너', '로고', '후킹'],
  },
  {
    title: '파트너 FAQ 응대 스크립트',
    category: '운영자료',
    type: '문서',
    owner: 'CS운영팀',
    updatedAt: '2026.04.25',
    status: '참고',
    summary: '혜택 적용, 쿠폰 재발급, 제휴 문의에 대응할 때 쓰는 공통 답변 기준입니다.',
    tags: ['FAQ', 'CS', '응대'],
  },
  {
    title: '정산/계약 관련 확인 항목',
    category: '운영자료',
    type: '체크리스트',
    owner: '사업개발팀',
    updatedAt: '2026.04.24',
    status: '승인 필요',
    summary: '정산 일정, 계약 조건, 파트너 전달 전 승인 여부를 확인하는 운영 자료입니다.',
    tags: ['계약', '정산', '승인'],
  },
]

const forbiddenTerms = [
  { term: '무조건 승인', replace: '심사 기준 충족 시 승인' },
  { term: '업계 최저가 보장', replace: '제휴 고객 전용 혜택' },
  { term: '전원 혜택', replace: '대상 고객 혜택' },
  { term: '평생 무료', replace: '프로모션 기간 무료' },
]

const resourceGroups = [
  { label: '기준 자료', items: ['가이드라인', '톤앤매너', '금지어'] },
  { label: '판단 자료', items: ['위험요소', '레퍼런스'] },
  { label: '운영 자료', items: ['FAQ', '정산', '계약'] },
]

const filteredResources = computed(() => {
  if (activeCategory.value === '전체') {
    return resources
  }

  return resources.filter((resource) => resource.category === activeCategory.value)
})
</script>

<template>
  <section class="resource-room">
    <header class="resource-hero">
      <div class="resource-hero__copy">
        <p>Campaign Resource Room</p>
        <h2>캠페인 자료실</h2>
        <span>{{ campaign.title }} · {{ campaign.partner }} · {{ campaign.period }}</span>
      </div>

      <div class="resource-hero__owner">
        <span>관리 담당</span>
        <strong>{{ campaign.owner }}</strong>
        <small>자료 기준일 2026.04.29</small>
      </div>
    </header>

    <section class="resource-summary" aria-label="자료실 요약">
      <article v-for="stat in resourceStats" :key="stat.label">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <p>{{ stat.detail }}</p>
      </article>
    </section>

    <nav class="resource-categories" aria-label="자료 분류">
      <button
        v-for="category in categories"
        :key="category"
        type="button"
        :class="{ 'resource-categories__item--active': activeCategory === category }"
        @click="activeCategory = category"
      >
        {{ category }}
      </button>
    </nav>

    <section class="resource-layout">
      <main class="resource-layout__main">
        <section class="pinned-section">
          <div class="section-head">
            <div>
              <p>Pinned</p>
              <h3>오픈 전 꼭 보는 자료</h3>
            </div>
          </div>

          <div class="pinned-grid">
            <article
              v-for="resource in pinnedResources"
              :key="resource.title"
              class="pinned-card"
              :class="`pinned-card--${resource.tone}`"
            >
              <span>{{ resource.category }}</span>
              <h4>{{ resource.title }}</h4>
              <p>{{ resource.summary }}</p>
              <footer>
                <small>{{ resource.owner }} · {{ resource.updatedAt }}</small>
              </footer>
            </article>
          </div>
        </section>

        <section class="library-section">
          <div class="section-head">
            <div>
              <p>Library</p>
              <h3>자료 목록</h3>
            </div>
            <button type="button" class="resource-button">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 5v14M5 12h14" />
              </svg>
              자료 추가
            </button>
          </div>

          <div class="resource-list">
            <article v-for="resource in filteredResources" :key="resource.title" class="resource-item">
              <div class="resource-item__type">
                <span>{{ resource.type }}</span>
              </div>
              <div class="resource-item__content">
                <div class="resource-item__title">
                  <strong>{{ resource.title }}</strong>
                  <em>{{ resource.status }}</em>
                </div>
                <p>{{ resource.summary }}</p>
                <div class="resource-item__meta">
                  <span>{{ resource.category }}</span>
                  <span>{{ resource.owner }}</span>
                  <span>{{ resource.updatedAt }}</span>
                </div>
                <div class="tag-row">
                  <span v-for="tag in resource.tags" :key="tag">#{{ tag }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </main>

      <aside class="resource-layout__side">
        <section class="side-panel">
          <div class="side-panel__head">
            <h4>자료실 구성</h4>
          </div>
          <div class="resource-map">
            <article v-for="group in resourceGroups" :key="group.label">
              <strong>{{ group.label }}</strong>
              <div>
                <span v-for="item in group.items" :key="item">{{ item }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="side-panel side-panel--warning">
          <div class="side-panel__head">
            <h4>금지어 빠른 참고</h4>
          </div>
          <div class="term-list">
            <article v-for="item in forbiddenTerms" :key="item.term">
              <strong>{{ item.term }}</strong>
              <span>{{ item.replace }}</span>
            </article>
          </div>
        </section>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.resource-room {
  display: flex;
  width: 100%;
  max-width: 1280px;
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

.resource-hero {
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

.resource-hero__copy p,
.section-head p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.resource-hero__copy h2 {
  margin-top: 5px;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 950;
  line-height: 1.2;
}

.resource-hero__copy span {
  display: block;
  margin-top: 9px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.resource-hero__owner {
  display: grid;
  min-width: 220px;
  gap: 4px;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.resource-hero__owner span,
.resource-hero__owner small {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.resource-hero__owner strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 950;
}

.resource-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.resource-summary article,
.pinned-card,
.resource-item,
.side-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.resource-summary article {
  display: grid;
  gap: 5px;
  padding: 16px;
}

.resource-summary span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.resource-summary strong {
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 950;
  line-height: 1;
}

.resource-summary p,
.pinned-card p,
.resource-item p,
.term-list span {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.55;
}

.resource-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.resource-categories button,
.resource-button {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 13px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.resource-button {
  border-radius: var(--radius-md);
}

.resource-button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.resource-categories__item--active {
  border-color: transparent !important;
  background: var(--color-primary-500) !important;
  color: #ffffff !important;
}

.resource-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.resource-layout__main,
.resource-layout__side {
  display: grid;
  min-width: 0;
  align-content: start;
  gap: 16px;
}

.section-head,
.resource-item__title,
.side-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h3,
.side-panel h4 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 950;
}

.pinned-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.pinned-card {
  display: grid;
  min-height: 168px;
  gap: 10px;
  padding: 16px;
}

.pinned-card > span,
.resource-item__type span,
.resource-item__title em {
  display: inline-flex;
  width: fit-content;
  min-height: 25px;
  align-items: center;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 900;
}

.pinned-card--primary > span {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.pinned-card--success > span {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.pinned-card--danger > span {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.pinned-card h4,
.resource-item__title strong,
.resource-map strong,
.term-list strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 950;
  line-height: 1.35;
}

.pinned-card footer {
  margin-top: auto;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.library-section {
  display: grid;
  gap: 12px;
}

.resource-list {
  display: grid;
  gap: 10px;
}

.resource-item {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
}

.resource-item__type span {
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.resource-item__content {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.resource-item__title em {
  background: var(--badge-bg);
  color: var(--badge-text);
  font-style: normal;
}

.resource-item__meta,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.resource-item__meta span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.tag-row span {
  display: inline-flex;
  min-height: 23px;
  align-items: center;
  padding: 0 8px;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 800;
}

.side-panel {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.side-panel--warning {
  background: var(--danger-surface);
}

.resource-map,
.term-list {
  display: grid;
  gap: 10px;
}

.resource-map article,
.term-list article {
  display: grid;
  gap: 7px;
  padding: 11px;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.resource-map article div {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.resource-map span {
  display: inline-flex;
  min-height: 23px;
  align-items: center;
  padding: 0 8px;
  border-radius: var(--radius-full);
  background: var(--panel-color);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 800;
}

.term-list strong {
  color: var(--danger-text-strong);
}

@media (max-width: 1180px) {
  .resource-layout {
    grid-template-columns: 1fr;
  }

  .pinned-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .resource-hero,
  .section-head,
  .resource-item__title {
    align-items: flex-start;
    flex-direction: column;
  }

  .resource-hero__owner,
  .resource-button {
    width: 100%;
  }

  .resource-summary,
  .pinned-grid,
  .resource-item {
    grid-template-columns: 1fr;
  }
}
</style>
