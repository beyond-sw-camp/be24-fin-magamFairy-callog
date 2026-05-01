<script setup>
import { computed, onMounted, ref } from 'vue'
import { CreateAsset, ListAssets } from '@/api/matchingAssets'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const currentSubTab = ref('assets')
const assets = ref([])
const isAssetLoading = ref(false)
const assetError = ref('')
const isAssetFormOpen = ref(false)

const assetForm = ref({
  type: '',
  target: '',
  scale: '',
  conditions: '',
})

const partnerProposals = [
  {
    id: 1,
    partner: '럭시드',
    name: '핸드크림 10ml 샘플',
    type: '샘플',
    target: '2040 뷰티 고객',
    scale: '10,000개',
    cost: '파트너 전액 부담',
    period: '2026.05.01 - 2026.06.30',
    status: '접수 완료',
  },
  {
    id: 2,
    partner: '멜로우',
    name: '전시 시설 30% 할인권',
    type: '할인권',
    target: '휴가철 여행 계획 고객',
    scale: '제한 없음',
    cost: '파트너 100% 부담',
    period: '상시 협의',
    status: '평가 반영',
  },
  {
    id: 3,
    partner: '어반터치',
    name: '오리지널 콘텐츠 공동 프로모션',
    type: '공동 콘텐츠',
    target: '미입력',
    scale: '1,000만',
    cost: '미입력',
    period: '미입력',
    status: '임시 저장',
  },
]

function mapAsset(asset) {
  return {
    id: asset.id ?? asset.assetId ?? asset.idx,
    type: asset.type ?? asset.assetName ?? asset.name ?? '-',
    affiliate: asset.affiliate ?? asset.affiliateName ?? '-',
    target: asset.target ?? asset.targetCustomer ?? '-',
    scale: asset.scale ?? asset.assetScale ?? '-',
    conditions: asset.conditions ?? asset.condition ?? '-',
    status: asset.status ?? (asset.active ?? asset.isActive ? 'AVAILABLE' : 'PAUSED'),
  }
}

function assetStatusLabel(status) {
  const normalized = String(status ?? '').toUpperCase()

  if (normalized === 'AVAILABLE' || normalized === 'ACTIVE') return '사용 가능'
  if (normalized === 'IN_USE') return '사용 중'
  if (normalized === 'EXPIRED') return '기간 만료'
  if (normalized === 'NEEDS_REVIEW') return '검토 필요'
  return '추천 제외'
}

function isAssetAvailable(status) {
  const normalized = String(status ?? '').toUpperCase()
  return normalized === 'AVAILABLE' || normalized === 'ACTIVE'
}

async function loadAssets() {
  isAssetLoading.value = true
  assetError.value = ''

  try {
    const data = await ListAssets()
    assets.value = (data.assetList ?? data ?? []).map(mapAsset)
  } catch (error) {
    assetError.value = error.message ?? '자산을 불러오지 못했습니다.'
  } finally {
    isAssetLoading.value = false
  }
}

function openAssetForm() {
  isAssetFormOpen.value = true
}

function closeAssetForm() {
  isAssetFormOpen.value = false
}

async function submitAsset() {
  await CreateAsset(assetForm.value)

  assetForm.value = {
    type: '',
    target: '',
    scale: '',
    conditions: '',
  }

  isAssetFormOpen.value = false
  await loadAssets()
}

onMounted(loadAssets)

const rows = computed(() =>
  currentSubTab.value === 'assets' ? assets.value : partnerProposals,
)
</script>

<template>
  <section class="asset-workspace">
    <article class="asset-panel">
      <div class="asset-panel__head">
        <div class="asset-panel__title">
          <h3>{{ currentSubTab === 'assets' ? '한화 자산 풀' : '파트너 제안' }}</h3>
          <p>
            {{
              currentSubTab === 'assets'
                ? '여러 캠페인에서 재사용할 수 있는 본사 보유 자산입니다.'
                : '파트너사가 제출한 혜택을 조회하고 매칭 평가에 반영합니다.'
            }}
          </p>
        </div>

        <div class="asset-toolbar">
          <button
            v-if="currentSubTab === 'assets'"
            type="button"
            class="asset-primary"
            aria-label="한화 자산 등록"
            title="한화 자산 등록"
            @click="openAssetForm"
          >
            +
          </button>

          <div class="asset-segment" role="tablist" aria-label="매칭 입력값 유형">
            <button
              type="button"
              :class="{ active: currentSubTab === 'assets' }"
              @click="currentSubTab = 'assets'"
            >
              한화 자산
              <span>{{ assets.length }}</span>
            </button>
            <button
              type="button"
              :class="{ active: currentSubTab === 'benefits' }"
              @click="currentSubTab = 'benefits'"
            >
              파트너 제안
              <span>{{ partnerProposals.length }}</span>
            </button>
          </div>
        </div>
      </div>

      <p v-if="assetError" class="asset-message">{{ assetError }}</p>
      <p v-else-if="isAssetLoading" class="asset-message">불러오는 중입니다.</p>

      <div v-if="currentSubTab === 'assets'" class="asset-table asset-table--assets">
        <div class="asset-table__head">
          <span>자산명</span>
          <span>보유 조직</span>
          <span>접점 고객</span>
          <span>제공 규모</span>
          <span>사용 조건</span>
          <span>추천 사용</span>
        </div>
        <div v-for="asset in rows" :key="asset.id" class="asset-table__row">
          <strong>{{ asset.type }}</strong>
          <span>{{ asset.affiliate }}</span>
          <span>{{ asset.target }}</span>
          <span>{{ asset.scale }}</span>
          <span>{{ asset.conditions }}</span>
          <em :class="{ muted: !isAssetAvailable(asset.status) }">
            {{ assetStatusLabel(asset.status) }}
          </em>
        </div>
      </div>

      <div v-else class="asset-table asset-table--benefits">
        <div class="asset-table__head">
          <span>파트너</span>
          <span>제안 혜택</span>
          <span>혜택 유형</span>
          <span>대상 고객</span>
          <span>제공 규모</span>
          <span>비용 부담</span>
          <span>검토 상태</span>
        </div>
        <div v-for="proposal in rows" :key="proposal.id" class="asset-table__row">
          <strong>{{ proposal.partner }}</strong>
          <span>{{ proposal.name }}</span>
          <span>{{ proposal.type }}</span>
          <span>{{ proposal.target }}</span>
          <span>{{ proposal.scale }}</span>
          <span>{{ proposal.cost }}</span>
          <em :class="{ muted: proposal.status === '임시 저장' }">{{ proposal.status }}</em>
        </div>
      </div>

      <div v-if="isAssetFormOpen" class="asset-modal">
        <form class="asset-modal__panel" @submit.prevent="submitAsset">
          <div class="asset-modal__head">
            <strong>한화 자산 등록</strong>
            <button type="button" @click="closeAssetForm">닫기</button>
          </div>

          <label>
            <span>자산명</span>
            <input v-model="assetForm.type" placeholder="예: 갤러리아 앱, 호텔 객실, 멤버십 채널" />
          </label>
          <label>
            <span>접점 고객</span>
            <input v-model="assetForm.target" placeholder="예: 2030 프리미엄 쇼핑 고객" />
          </label>
          <label>
            <span>제공 규모</span>
            <input v-model="assetForm.scale" placeholder="예: MAU 45만, 월 1만 객실" />
          </label>
          <label>
            <span>사용 조건</span>
            <input v-model="assetForm.conditions" placeholder="예: 월 1회 메인 팝업, 주중 한정" />
          </label>

          <div class="asset-modal__actions">
            <button type="button" class="asset-modal__secondary" @click="closeAssetForm">취소</button>
            <button type="submit" class="asset-modal__primary">저장</button>
          </div>
        </form>
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
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.8rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
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
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  min-height: 2.6rem;
  align-items: center;
  column-gap: 0.55rem;
  row-gap: 0.16rem;
  min-width: 0;
}

.asset-panel__title::before {
  content: '';
  grid-row: 1 / 3;
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

.asset-panel__title p {
  overflow: hidden;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-toolbar {
  display: flex;
  align-items: center;
  gap: 0.45rem;
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
  width: 2.55rem;
  height: 2.55rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--accent-color);
  color: #fff;
  padding: 0;
  font-size: 1.35rem;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent-color) 18%, transparent);
}

.asset-message {
  margin: 0 0 0.65rem;
  color: var(--text-secondary);
  font-size: 0.84rem;
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

.asset-modal {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.32);
  padding: 1rem;
}

.asset-modal__panel {
  display: grid;
  width: min(28rem, 100%);
  gap: 0.8rem;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 1rem;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.2);
}

.asset-modal__head,
.asset-modal__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.asset-modal__head strong {
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
}

.asset-modal label {
  display: grid;
  gap: 0.35rem;
}

.asset-modal label span {
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 800;
}

.asset-modal input {
  min-height: 2.6rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-primary);
  padding: 0 0.75rem;
}

.asset-modal button {
  min-height: 2.4rem;
  border-radius: 7px;
  padding: 0 0.9rem;
  font-weight: 800;
  cursor: pointer;
}

.asset-modal__primary {
  background: var(--accent-color);
  color: #fff;
}

.asset-modal__secondary,
.asset-modal__head button {
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
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
