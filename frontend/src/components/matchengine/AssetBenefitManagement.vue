<script setup>
import { computed, ref } from 'vue'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const currentSubTab = ref('assets')

const assets = [
  { id: 1, type: '갤러리아 앱', affiliate: '한화갤러리아', target: '2030 프리미엄 쇼핑 고객', scale: 'MAU 45만', conditions: '월 1회 메인 팝업', active: true },
  { id: 2, type: '호텔 객실', affiliate: '한화호텔앤드리조트', target: '가족 단위 레저 고객', scale: '월 1만 객실', conditions: '주중 한정 연계', active: true },
  { id: 3, type: '스포츠 티켓', affiliate: '한화이글스', target: '2030 야구 팬덤', scale: '경기당 1,000석', conditions: '주말 제외 협의', active: false },
]

const benefits = [
  { id: 1, partner: '록시땅', name: '핸드크림 10ml 샘플링', type: '샘플', target: '2040 뷰티 고관여 여성', scale: '10,000개', cost: '파트너 전액 부담', status: '평가 대기' },
  { id: 2, partner: '야놀자', name: '레저 시설 30% 할인권', type: '할인권', target: '휴가철 여행 계획 고객', scale: '제한 없음', cost: '파트너 100% 부담', status: '평가 완료' },
  { id: 3, partner: '티빙', name: '오리지널 콘텐츠 공동 프로모션', type: '공동 콘텐츠', target: '미입력', scale: '1,000매', cost: '미입력', status: '임시 저장' },
]

const rows = computed(() => (currentSubTab.value === 'assets' ? assets : benefits))
</script>

<template>
  <section class="asset-workspace">
    <aside class="asset-side">
      <button
        type="button"
        :class="{ active: currentSubTab === 'assets' }"
        @click="currentSubTab = 'assets'"
      >
        <strong>계열사 자산</strong>
        <span>{{ assets.length }}개</span>
      </button>
      <button
        type="button"
        :class="{ active: currentSubTab === 'benefits' }"
        @click="currentSubTab = 'benefits'"
      >
        <strong>파트너 혜택</strong>
        <span>{{ benefits.length }}개</span>
      </button>
      <button type="button" class="asset-primary">
        {{ currentSubTab === 'assets' ? '자산 등록' : '혜택 등록' }}
      </button>
    </aside>

    <article class="asset-panel">
      <div class="asset-panel__head">
        <h3>{{ currentSubTab === 'assets' ? '계열사 자산 관리' : '파트너 혜택 관리' }}</h3>
        <span>매칭 입력값</span>
      </div>

      <div v-if="currentSubTab === 'assets'" class="asset-table asset-table--assets">
        <div class="asset-table__head">
          <span>유형</span>
          <span>계열사</span>
          <span>고객층</span>
          <span>규모</span>
          <span>조건</span>
          <span>상태</span>
        </div>
        <div v-for="asset in rows" :key="asset.id" class="asset-table__row">
          <strong>{{ asset.type }}</strong>
          <span>{{ asset.affiliate }}</span>
          <span>{{ asset.target }}</span>
          <span>{{ asset.scale }}</span>
          <span>{{ asset.conditions }}</span>
          <em :class="{ muted: !asset.active }">{{ asset.active ? '활성' : '비활성' }}</em>
        </div>
      </div>

      <div v-else class="asset-table asset-table--benefits">
        <div class="asset-table__head">
          <span>파트너</span>
          <span>혜택</span>
          <span>유형</span>
          <span>대상</span>
          <span>규모</span>
          <span>비용</span>
          <span>상태</span>
        </div>
        <div v-for="benefit in rows" :key="benefit.id" class="asset-table__row">
          <strong>{{ benefit.partner }}</strong>
          <span>{{ benefit.name }}</span>
          <span>{{ benefit.type }}</span>
          <span>{{ benefit.target }}</span>
          <span>{{ benefit.scale }}</span>
          <span>{{ benefit.cost }}</span>
          <em :class="{ muted: benefit.status === '임시 저장' }">{{ benefit.status }}</em>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.asset-workspace {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.asset-side,
.asset-panel {
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 0.8rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
  min-height: 0;
}

.asset-side {
  display: grid;
  align-content: start;
  gap: 0.5rem;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-muted) 86%, var(--accent-soft)),
      var(--panel-muted)
    );
  border-color: color-mix(in srgb, var(--border-strong) 72%, var(--accent-color));
  box-shadow: inset -1px 0 0 color-mix(in srgb, var(--border-color) 72%, transparent);
}

.asset-side button {
  display: flex;
  min-height: 2.8rem;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: color-mix(in srgb, var(--panel-color) 66%, var(--panel-muted));
  padding: 0 0.65rem;
  color: var(--text-secondary);
  cursor: pointer;
}

.asset-side button.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 11%, var(--panel-color));
  color: var(--text-primary);
  box-shadow:
    0 5px 14px rgba(19, 35, 68, 0.06),
    inset 3px 0 0 var(--accent-color);
}

.asset-side strong {
  font-size: 0.82rem;
}

.asset-side span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.asset-side .asset-primary {
  justify-content: center;
  margin-top: 0.25rem;
  background: var(--accent-color);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 800;
}

.asset-panel {
  min-width: 0;
  overflow: auto;
  background: var(--panel-color);
  border-color: var(--border-strong);
}

.asset-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.65rem;
}

.asset-panel__head h3 {
  color: var(--text-primary);
  font-size: 0.95rem;
}

.asset-panel__head span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.asset-table {
  display: grid;
  gap: 0.4rem;
}

.asset-table__head,
.asset-table__row {
  display: grid;
  align-items: center;
  gap: 0.55rem;
}

.asset-table--assets .asset-table__head,
.asset-table--assets .asset-table__row {
  grid-template-columns: 0.8fr 1fr 1.45fr 0.8fr 1.1fr 70px;
}

.asset-table--benefits .asset-table__head,
.asset-table--benefits .asset-table__row {
  grid-template-columns: 0.8fr 1.35fr 0.75fr 1.2fr 0.7fr 0.9fr 80px;
}

.asset-table__head {
  padding: 0 0.55rem 0.25rem;
}

.asset-table__head span {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 900;
}

.asset-table__row {
  min-height: 3.15rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.55rem;
}

.asset-table__row strong {
  color: var(--text-primary);
  font-size: 0.82rem;
}

.asset-table__row span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.76rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-table__row em {
  display: inline-flex;
  min-height: 1.45rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--color-success-light);
  color: var(--color-success-dark);
  font-size: 0.66rem;
  font-style: normal;
  font-weight: 900;
}

.asset-table__row em.muted {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

@media (max-width: 1180px) {
  .asset-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
