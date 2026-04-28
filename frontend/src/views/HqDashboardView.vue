<script setup>
import { computed } from 'vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()
const activeCampaign = computed(() => store.activeCampaign)

const stats = [
  { label: '전체 진행률', value: '68%', caption: '+12% this week', positive: true },
  { label: '검토 대기', value: '3', caption: '초청장, 배너, SNS' },
  { label: '참여 조직', value: '4', caption: '본사 포함 4개 조직' },
  { label: '성과 기록', value: '준비중', caption: '행사 지표 입력 예정' },
]

const partners = [
  { name: '갤러리아', progress: 82 },
  { name: '호텔앤드리조트', progress: 64 },
  { name: '외부 대행사', progress: 58 },
  { name: '제휴 파트너', progress: 51 },
]

const activities = [
  { initial: 'H', text: '본사가 메인 메시지를 승인했습니다.', time: '20분 전', primary: true },
  { initial: 'G', text: '갤러리아가 VIP 초청장 검토를 요청했습니다.', time: '1시간 전' },
  { initial: 'A', text: '대행사가 SNS 카드뉴스 2안을 업로드했습니다.', time: '3시간 전' },
]

const contents = [
  {
    code: 'INV',
    title: 'VIP 초청장 문구',
    meta: '갤러리아 VIP · 이메일/알림톡',
    status: 'Review Requested',
    tone: 'review',
    primary: true,
  },
  {
    code: 'BNR',
    title: '리조트 패키지 배너',
    meta: '가족 휴양 고객 · 랜딩 페이지',
    status: 'Draft',
    tone: 'draft',
  },
  {
    code: 'SNS',
    title: 'SNS 카드뉴스',
    meta: '프리미엄 뷰티 관심 고객 · Instagram',
    status: 'Needs Revision',
    tone: 'revision',
  },
]

const segments = ['갤러리아 VIP 고객', '리조트 회원', '프리미엄 뷰티 관심 고객', '가족 단위 휴양 고객']

const reviews = [
  { label: '승인 완료', count: 1 },
  { label: '검토 요청', count: 3 },
  { label: '수정 필요', count: 1 },
]
</script>

<template>
  <section class="hq-dashboard">
    <header class="hq-dashboard__hero">
      <div>
        <p class="hq-dashboard__eyebrow">본사 통합 대시보드</p>
        <h2>{{ activeCampaign?.name ?? 'Campaign Dashboard' }}</h2>
        <span v-if="activeCampaign?.purpose" class="hq-dashboard__summary">
          {{ activeCampaign.purpose }}
        </span>
      </div>
      <span class="hq-dashboard__status">{{ activeCampaign?.status ?? 'Live' }}</span>
    </header>

    <section class="hq-dashboard__stats" aria-label="핵심 지표">
      <article v-for="stat in stats" :key="stat.label" class="hq-stat-card">
        <p>{{ stat.label }}</p>
        <strong>{{ stat.value }}</strong>
        <span :class="{ 'hq-stat-card__caption--positive': stat.positive }">{{ stat.caption }}</span>
      </article>
    </section>

    <section class="hq-dashboard__grid">
      <article class="hq-panel hq-panel--wide">
        <div class="hq-panel__header">
          <h3>Campaign Progress</h3>
          <span>Live</span>
        </div>
        <div class="hq-progress-list" aria-label="협력사별 진행률">
          <div v-for="partner in partners" :key="partner.name" class="hq-progress-row">
            <span>{{ partner.name }}</span>
            <div class="hq-progress-row__track">
              <i :style="{ width: `${partner.progress}%` }" />
            </div>
            <strong>{{ partner.progress }}%</strong>
          </div>
        </div>
      </article>

      <article class="hq-panel">
        <div class="hq-panel__header">
          <h3>Recent Activity</h3>
        </div>
        <div class="hq-activity-list">
          <div v-for="activity in activities" :key="activity.text" class="hq-activity">
            <div class="hq-activity__avatar" :class="{ 'hq-activity__avatar--primary': activity.primary }">
              {{ activity.initial }}
            </div>
            <div>
              <p>{{ activity.text }}</p>
              <span>{{ activity.time }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>

    <article class="hq-panel">
      <div class="hq-panel__header">
        <h3>Marketing Content</h3>
        <button type="button">New Card</button>
      </div>
      <div class="hq-content-grid">
        <article v-for="content in contents" :key="content.title" class="hq-content-card">
          <div class="hq-content-card__thumb" :class="{ 'hq-content-card__thumb--primary': content.primary }">
            <span>{{ content.code }}</span>
          </div>
          <div class="hq-content-card__body">
            <strong>{{ content.title }}</strong>
            <p>{{ content.meta }}</p>
            <span class="hq-badge" :class="`hq-badge--${content.tone}`">{{ content.status }}</span>
          </div>
        </article>
      </div>
    </article>

    <section class="hq-dashboard__grid hq-dashboard__grid--bottom">
      <article class="hq-panel">
        <div class="hq-panel__header">
          <h3>Customer Segments</h3>
        </div>
        <div class="hq-chip-list">
          <span v-for="segment in segments" :key="segment">{{ segment }}</span>
        </div>
      </article>

      <article class="hq-panel">
        <div class="hq-panel__header">
          <h3>Review Status</h3>
        </div>
        <div class="hq-review-list">
          <div v-for="review in reviews" :key="review.label">
            <span>{{ review.label }}</span>
            <strong>{{ review.count }}</strong>
          </div>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.hq-dashboard {
  display: flex;
  width: 100%;
  max-width: var(--content-max-width);
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

.hq-dashboard__hero,
.hq-panel,
.hq-stat-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal);
}

.hq-dashboard__hero {
  display: flex;
  min-height: 88px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
}

.hq-dashboard__eyebrow {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

.hq-dashboard__hero h2 {
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

.hq-dashboard__summary {
  display: block;
  margin-top: 6px;
  color: var(--muted-text);
  font-size: 13px;
}

.hq-dashboard__status,
.hq-panel__header span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: var(--color-primary-500);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.hq-dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.hq-stat-card {
  min-height: 134px;
  padding: 22px 24px;
}

.hq-stat-card p {
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 600;
}

.hq-stat-card strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--text-primary);
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
}

.hq-stat-card span {
  color: var(--subtle-text);
  font-size: 13px;
}

.hq-stat-card__caption--positive {
  color: var(--color-success) !important;
  font-weight: 600;
}

.hq-dashboard__grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 16px;
}

.hq-dashboard__grid--bottom {
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.62fr);
}

.hq-panel {
  padding: 20px;
}

.hq-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.hq-panel__header h3 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.hq-panel__header button {
  min-height: 34px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  background: var(--color-primary-500);
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background var(--transition-fast);
}

.hq-panel__header button:hover {
  background: var(--color-primary-600);
}

.hq-progress-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hq-progress-row {
  display: grid;
  grid-template-columns: 148px minmax(0, 1fr) 52px;
  align-items: center;
  gap: 14px;
  color: var(--text-secondary);
  font-size: 14px;
}

.hq-progress-row__track {
  height: 8px;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
}

.hq-progress-row__track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary-500);
}

.hq-progress-row strong {
  color: var(--text-primary);
  font-size: 13px;
  text-align: right;
}

.hq-activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hq-activity {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.hq-activity__avatar {
  display: flex;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 13px;
  font-weight: 700;
}

.hq-activity__avatar--primary {
  background: var(--color-primary-500);
  color: #ffffff;
}

.hq-activity p {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.45;
}

.hq-activity span {
  display: block;
  margin-top: 2px;
  color: var(--subtle-text);
  font-size: 12px;
}

.hq-content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.hq-content-card {
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    transform var(--transition-fast);
}

.hq-content-card:hover {
  border-color: var(--color-primary-300);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.hq-content-card__thumb {
  display: flex;
  height: 150px;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--border-color);
  background: var(--badge-bg);
}

.hq-content-card__thumb--primary {
  background: var(--color-primary-600);
}

.hq-content-card__thumb span {
  color: var(--badge-text);
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 2px;
}

.hq-content-card__thumb--primary span {
  color: #ffffff;
}

.hq-content-card__body {
  padding: 14px 16px 16px;
}

.hq-content-card__body strong {
  display: block;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hq-content-card__body p {
  margin: 4px 0 10px;
  overflow: hidden;
  color: var(--muted-text);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hq-badge {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
}

.hq-badge--review {
  background: var(--badge-bg);
  color: var(--badge-text);
}

.hq-badge--draft {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.hq-badge--revision {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

:global(:root[data-theme='dark']) .hq-badge--revision {
  background: rgba(245, 158, 11, 0.16);
  color: #fbbf24;
}

.hq-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hq-chip-list span {
  padding: 7px 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
}

.hq-review-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hq-review-list div {
  padding: 14px;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.hq-review-list span {
  display: block;
  color: var(--muted-text);
  font-size: 12px;
}

.hq-review-list strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .hq-dashboard__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hq-dashboard__grid,
  .hq-dashboard__grid--bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hq-dashboard__hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hq-dashboard__stats,
  .hq-content-grid,
  .hq-review-list {
    grid-template-columns: 1fr;
  }

  .hq-progress-row {
    grid-template-columns: 108px minmax(0, 1fr) 48px;
  }
}
</style>
