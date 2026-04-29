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
    <article class="asset-panel">
      <div class="asset-panel__head">
        <div class="asset-panel__title">
          <h3>{{ currentSubTab === 'assets' ? '계열사 자산' : '제안 혜택 후보' }}</h3>
        </div>

        <div class="asset-toolbar">
          <div class="asset-segment" role="tablist" aria-label="매칭 입력값 유형">
            <button
              type="button"
              :class="{ active: currentSubTab === 'assets' }"
              @click="currentSubTab = 'assets'"
            >
              계열사 자산
              <span>{{ assets.length }}</span>
            </button>
            <button
              type="button"
              :class="{ active: currentSubTab === 'benefits' }"
              @click="currentSubTab = 'benefits'"
            >
              제안 혜택
              <span>{{ benefits.length }}</span>
            </button>
          </div>

          <button type="button" class="asset-primary">
            {{ currentSubTab === 'assets' ? '자산 등록' : '혜택 후보 등록' }}
          </button>
        </div>
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
  height: 100%;
  min-height: 0;
}

.asset-panel {
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 0.8rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
  min-height: 0;
}

.asset-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-width: 0;
  overflow: auto;
  background: var(--panel-color);
  border-color: var(--border-strong);
}

.asset-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-color) 94%, var(--panel-muted)),
      var(--panel-color)
    );
  padding: 0.55rem 0.65rem;
  margin-bottom: 0.65rem;
}

.asset-panel__title {
  display: flex;
  min-height: 2.6rem;
  align-items: center;
  gap: 0.55rem;
}

.asset-panel__title::before {
  content: '';
  width: 0.28rem;
  height: 1.55rem;
  border-radius: 999px;
  background: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent);
}

.asset-panel__head h3 {
  color: var(--text-primary);
  font-size: 1.02rem;
  font-weight: 900;
  line-height: 1;
}

.asset-toolbar {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.asset-segment {
  display: inline-flex;
  gap: 0.35rem;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--panel-muted);
  padding: 0.3rem;
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--border-color) 55%, transparent);
}

.asset-segment button {
  display: inline-flex;
  width: 7.6rem;
  min-height: 2.6rem;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border-radius: 8px;
  padding: 0 0.95rem;
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
}

.asset-segment button.active {
  background: var(--panel-color);
  color: var(--accent-color);
  box-shadow:
    0 4px 12px rgba(19, 35, 68, 0.06),
    inset 0 -2px 0 var(--accent-color);
}

.asset-segment span {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.asset-primary {
  display: inline-flex;
  width: 7.8rem;
  min-height: 3.2rem;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--accent-color);
  color: #fff;
  padding: 0 1.05rem;
  font-size: 0.82rem;
  font-weight: 800;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent-color) 18%, transparent);
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
  font-size: 0.76rem;
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
  font-size: 0.9rem;
}

.asset-table__row span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.84rem;
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
  font-size: 0.72rem;
  font-style: normal;
  font-weight: 900;
}

.asset-table__row em.muted {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

@media (max-width: 1180px) {
  .asset-panel__head,
  .asset-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .asset-segment {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
