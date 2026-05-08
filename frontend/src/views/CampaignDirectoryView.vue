<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ListCampaignDirectory } from '@/api/campaigns'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const authStore = useAuthStore()

/* ───── 상태 ───── */
const loading = ref(true)
const errorText = ref('')
const allCampaigns = ref([])

/* ───── 필터 ───── */
const activeScope = ref('all')     // all / mine / applied
const searchQuery = ref('')
const filterOrgType = ref('ALL')   // ALL / HQ / AFFILIATE / EXTERNAL_PARTNER
const filterStatus = ref('ALL')    // ALL / live / review / draft / completed
const selectedTags = ref([])       // 클릭한 태그
const sortMode = ref('latest')     // latest / deadline

/* ───── 권한 ───── */
const orgType = computed(() => {
  const fromOrg = authStore.user?.organization?.type
  if (fromOrg) return String(fromOrg).toUpperCase()
  const fromClaim = authStore.user?.orgType
  return fromClaim ? String(fromClaim).toUpperCase() : 'HQ'
})

/* ───── load ───── */
async function load() {
  loading.value = true
  errorText.value = ''
  try {
    const data = await ListCampaignDirectory({
      scope: activeScope.value === 'all' ? undefined : activeScope.value,
    })
    allCampaigns.value = Array.isArray(data) ? data : data?.items ?? []
  } catch (err) {
    console.warn('[campaign-directory] load failed', err)
    errorText.value = '캠페인 목록을 불러오지 못했습니다.'
    allCampaigns.value = []
  } finally {
    loading.value = false
  }
}
onMounted(load)

/* scope 탭 변경 → backend reload */
import { watch } from 'vue'
watch(activeScope, load)

/* 권한별 노출 정책: EXTERNAL_PARTNER는 캠페인 생성 불가 → "내가 올린" 탭 숨김 */
const isExternalPartner = computed(() => orgType.value === 'EXTERNAL_PARTNER')

const SCOPE_TABS = computed(() => {
  const base = [
    { key: 'all',     label: '전체' },
    { key: 'applied', label: '협업에 지원한 캠페인' },
  ]
  if (!isExternalPartner.value) {
    base.splice(1, 0, { key: 'mine', label: '내가 올린 캠페인' })
  }
  return base
})

const emptyMessage = computed(() => {
  if (activeScope.value === 'mine') return '아직 올린 캠페인이 없습니다.'
  if (activeScope.value === 'applied') return '아직 지원한 캠페인이 없습니다.'
  return '조건에 해당하는 캠페인이 없습니다.'
})

/* ───── 파생 데이터 ───── */
const allTags = computed(() => {
  const set = new Set()
  allCampaigns.value.forEach((c) => (c.tags ?? []).forEach((t) => set.add(t)))
  return Array.from(set).sort()
})

const STATUS_META = {
  live:      { label: 'LIVE',     cls: 'st--live' },
  running:   { label: 'LIVE',     cls: 'st--live' },
  review:    { label: 'REVIEW',   cls: 'st--review' },
  draft:     { label: 'DRAFT',    cls: 'st--draft' },
  paused:    { label: 'PAUSED',   cls: 'st--paused' },
  completed: { label: 'COMPLETED', cls: 'st--done' },
}
function statusOf(s) { return STATUS_META[String(s ?? '').toLowerCase()] ?? STATUS_META.draft }

function calcDDay(endDate) {
  if (!endDate) return null
  const end = new Date(endDate)
  if (Number.isNaN(end.getTime())) return null
  const today = new Date(); today.setHours(0,0,0,0); end.setHours(0,0,0,0)
  return Math.round((end - today) / 86400000)
}
function fmtDDay(d) {
  if (d == null) return ''
  if (d > 0) return `D-${d}`
  if (d === 0) return 'D-DAY'
  return `D+${-d}`
}

/* organizationName 추출 — Phase 1은 partners[0] 또는 ownerOrg fallback */
function deriveOrgLabel(c) {
  if (c.ownerOrgName) return c.ownerOrgName
  if (Array.isArray(c.partners) && c.partners.length > 0) return c.partners[0]
  return '한화 그룹'
}
function deriveOrgType(c) {
  // Phase 1 추론 — backend가 ownerOrgType 안 주므로 partners 기반 약식
  if (c.ownerOrgType) return c.ownerOrgType
  return 'HQ'   // 미상은 HQ로
}

/* 필터링된 결과 */
const filteredCampaigns = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  const tags = selectedTags.value
  return allCampaigns.value
    .filter((c) => {
      if (q) {
        const hay = `${c.name ?? ''} ${c.purpose ?? ''} ${c.mainMessage ?? ''}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      if (filterOrgType.value !== 'ALL' && deriveOrgType(c) !== filterOrgType.value) return false
      if (filterStatus.value !== 'ALL' && (c.status ?? '').toLowerCase() !== filterStatus.value) return false
      if (tags.length > 0) {
        const cTags = c.tags ?? []
        const hasAny = tags.some((t) => cTags.includes(t))
        if (!hasAny) return false
      }
      return true
    })
    .sort((a, b) => {
      if (sortMode.value === 'deadline') {
        const ad = calcDDay(a.endDate); const bd = calcDDay(b.endDate)
        if (ad == null && bd == null) return 0
        if (ad == null) return 1
        if (bd == null) return -1
        return ad - bd
      }
      // latest — createdAt 또는 updatedAt 기준 내림차순
      const at = new Date(a.createdAt ?? a.updatedAt ?? 0).getTime()
      const bt = new Date(b.createdAt ?? b.updatedAt ?? 0).getTime()
      return bt - at
    })
})

/* 태그 토글 */
function toggleTag(tag) {
  const i = selectedTags.value.indexOf(tag)
  if (i >= 0) selectedTags.value.splice(i, 1)
  else selectedTags.value.push(tag)
}
function clearFilters() {
  searchQuery.value = ''
  filterOrgType.value = 'ALL'
  filterStatus.value = 'ALL'
  selectedTags.value = []
  sortMode.value = 'latest'
}

/* 카드 클릭 → 소개 페이지 */
function goToCampaign(c) {
  const id = c.idx ?? c.id
  if (id) router.push({ name: 'campaign-intro', params: { campaignId: id } })
}

/* 썸네일 fallback — 단색 + 이니셜 */
function thumbBg(c) {
  return c.color || '#9D85FF'
}
function thumbInitials(c) {
  if (c.initials) return c.initials
  if (!c.name) return '··'
  return c.name.replace(/\s+/g, '').slice(0, 2).toUpperCase()
}
</script>

<template>
  <div class="dir-root">
    <!-- HERO -->
    <header class="dir-hero">
      <div class="dir-hero__copy">
        <span class="dir-hero__eyebrow">한화 그룹 캠페인</span>
        <h1 class="dir-hero__title">캠페인 둘러보기</h1>
        <p class="dir-hero__sub">본사·계열사·외부 파트너가 진행하는 모든 캠페인을 한눈에 살펴보세요.</p>
      </div>
      <div class="dir-hero__search">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
        </svg>
        <input
          v-model="searchQuery"
          type="search"
          placeholder="캠페인 이름·목적·메시지로 검색"
          class="dir-hero__input"
        />
      </div>
    </header>

    <!-- SCOPE TABS — 전체 / 내가 올린 / 지원한 -->
    <nav class="dir-scope-tabs" role="tablist" aria-label="캠페인 범위">
      <button
        v-for="t in SCOPE_TABS"
        :key="t.key"
        type="button"
        role="tab"
        :aria-selected="activeScope === t.key"
        class="dir-scope-tab"
        :class="{ 'is-active': activeScope === t.key }"
        @click="activeScope = t.key"
      >{{ t.label }}</button>
    </nav>

    <!-- TAG CHIPS -->
    <section v-if="allTags.length > 0" class="dir-tags">
      <button
        v-for="tag in allTags"
        :key="tag"
        type="button"
        class="dir-tag"
        :class="{ 'is-active': selectedTags.includes(tag) }"
        @click="toggleTag(tag)"
      >
        # {{ tag }}
      </button>
    </section>

    <!-- FILTER BAR -->
    <section class="dir-filters">
      <select v-model="filterOrgType" class="dir-select">
        <option value="ALL">전체 조직</option>
        <option value="HQ">본사</option>
        <option value="AFFILIATE">계열사</option>
        <option value="EXTERNAL_PARTNER">외부 파트너</option>
      </select>
      <select v-model="filterStatus" class="dir-select">
        <option value="ALL">전체 상태</option>
        <option value="live">진행 중</option>
        <option value="review">검수 중</option>
        <option value="draft">초안</option>
        <option value="completed">완료</option>
      </select>
      <select v-model="sortMode" class="dir-select">
        <option value="latest">최신순</option>
        <option value="deadline">마감 임박순</option>
      </select>
      <button
        v-if="searchQuery || filterOrgType !== 'ALL' || filterStatus !== 'ALL' || selectedTags.length"
        type="button"
        class="dir-clear"
        @click="clearFilters"
      >초기화</button>
      <span class="dir-count">{{ filteredCampaigns.length }}건</span>
    </section>

    <!-- STATE: loading / error / empty -->
    <section v-if="loading" class="dir-state">
      <div v-for="i in 8" :key="i" class="dir-card dir-card--skeleton">
        <div class="sk-shimmer dir-card__thumb-sk"></div>
        <div class="sk-shimmer dir-card__line-sk"></div>
        <div class="sk-shimmer dir-card__line-sk dir-card__line-sk--short"></div>
      </div>
    </section>
    <p v-else-if="errorText" class="dir-err">
      {{ errorText }} <button type="button" class="dir-retry" @click="load">다시 시도</button>
    </p>
    <p v-else-if="filteredCampaigns.length === 0" class="dir-empty">
      {{ emptyMessage }}
    </p>

    <!-- GRID -->
    <section v-else class="dir-grid">
      <article
        v-for="c in filteredCampaigns"
        :key="c.idx ?? c.id"
        class="dir-card fade-in"
        @click="goToCampaign(c)"
        @keydown.enter="goToCampaign(c)"
        tabindex="0"
        role="button"
      >
        <!-- 썸네일 — backend thumbnailUrl 우선, 없으면 단색+이니셜/아이콘 fallback -->
        <div class="dir-card__thumb" :style="!c.thumbnailUrl ? { background: thumbBg(c) } : null">
          <img v-if="c.thumbnailUrl" :src="c.thumbnailUrl" :alt="c.name" class="dir-card__thumb-img" />
          <template v-else>
            <span v-if="c.icon" class="dir-card__thumb-icon">{{ c.icon }}</span>
            <span v-else class="dir-card__thumb-initials">{{ thumbInitials(c) }}</span>
          </template>
          <span v-if="calcDDay(c.endDate) != null" class="dir-card__dday"
                :class="{ 'urgent': calcDDay(c.endDate) <= 7 && calcDDay(c.endDate) >= 0 }">
            {{ fmtDDay(calcDDay(c.endDate)) }}
          </span>
          <span class="dir-card__status" :class="statusOf(c.status).cls">{{ statusOf(c.status).label }}</span>
        </div>
        <div class="dir-card__body">
          <h3 class="dir-card__title">{{ c.name }}</h3>
          <p class="dir-card__org">{{ deriveOrgLabel(c) }}</p>
          <p v-if="c.purpose" class="dir-card__summary">{{ c.purpose }}</p>
          <div v-if="c.tags && c.tags.length > 0" class="dir-card__tags">
            <span v-for="t in c.tags.slice(0, 3)" :key="t" class="dir-card__tag">#{{ t }}</span>
            <span v-if="c.tags.length > 3" class="dir-card__tag dir-card__tag--more">+{{ c.tags.length - 3 }}</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dir-root {
  margin: calc(-1 * var(--density-page-padding, 24px));
  padding: 28px 32px 60px;
  min-height: calc(100% + 2 * var(--density-page-padding, 24px));
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--color-primary-500) 8%, transparent), transparent 35%),
    linear-gradient(180deg, var(--color-primary-50) 0%, var(--surface-page) 70%);
  display: flex; flex-direction: column; gap: 18px;
  font-family: 'Pretendard Variable', 'Pretendard', 'Noto Sans KR', sans-serif;
  color: var(--text-primary);
}
:root[data-theme='dark'] .dir-root {
  background:
    radial-gradient(circle at top right, rgba(168, 85, 247, 0.18), transparent 40%),
    linear-gradient(180deg, #10141d 0%, #181024 100%);
}

/* ───── HERO ───── */
.dir-hero {
  display: flex; justify-content: space-between; align-items: flex-end; gap: 24px;
  flex-wrap: wrap;
}
.dir-hero__copy { display: flex; flex-direction: column; gap: 4px; }
.dir-hero__eyebrow {
  font-size: 11px; font-weight: 700; letter-spacing: 0.08em;
  color: var(--color-primary-700); text-transform: uppercase;
}
.dir-hero__title {
  margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.02em;
  color: var(--text-primary);
}
.dir-hero__sub { margin: 0; font-size: 13px; color: var(--muted-text); }
.dir-hero__search {
  display: inline-flex; align-items: center; gap: 8px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 0 14px;
  width: min(360px, 100%);
  height: 38px;
  color: var(--muted-text);
  transition: border-color var(--transition-fast);
}
.dir-hero__search:focus-within {
  border-color: var(--color-primary-500);
  background: var(--control-focus-color, var(--panel-color));
}
.dir-hero__input {
  flex: 1; border: 0; background: transparent; outline: none;
  font-size: 13px; color: var(--text-primary); font-family: inherit;
}
.dir-hero__input::placeholder { color: var(--muted-text); }

/* ───── SCOPE TABS ───── */
.dir-scope-tabs {
  display: inline-flex;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 4px;
  gap: 2px;
  width: fit-content;
}
.dir-scope-tab {
  padding: 7px 16px;
  border-radius: 999px;
  background: transparent;
  border: 0;
  font-size: 12px; font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer; font-family: inherit;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.dir-scope-tab:hover { color: var(--text-primary); }
.dir-scope-tab.is-active {
  background: var(--color-primary-500);
  color: #fff;
}

/* ───── TAGS ───── */
.dir-tags {
  display: flex; flex-wrap: wrap; gap: 6px;
}
.dir-tag {
  display: inline-flex; align-items: center;
  height: 26px; padding: 0 10px;
  border-radius: 999px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 11px; font-weight: 700;
  cursor: pointer; font-family: inherit;
  transition: all var(--transition-fast);
}
.dir-tag:hover {
  border-color: color-mix(in srgb, var(--color-primary-500) 32%, var(--border-color));
  color: var(--color-primary-700);
}
.dir-tag.is-active {
  background: var(--color-primary-500);
  border-color: var(--color-primary-500);
  color: #fff;
}

/* ───── FILTER BAR ───── */
.dir-filters {
  display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
}
.dir-select {
  height: 32px; padding: 0 12px;
  border-radius: 8px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px; font-weight: 600; font-family: inherit;
  cursor: pointer;
}
.dir-clear {
  height: 32px; padding: 0 12px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--muted-text);
  font-size: 11px; font-weight: 700; font-family: inherit;
  cursor: pointer;
}
.dir-clear:hover { color: var(--text-primary); border-color: var(--text-secondary); }
.dir-count { margin-left: auto; font-size: 11px; color: var(--muted-text); font-weight: 700; }

/* ───── GRID ───── */
.dir-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}
.dir-state {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}

/* ───── CARD ───── */
.dir-card {
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  display: flex; flex-direction: column;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.dir-card:hover {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--color-primary-500) 30%, var(--border-color));
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}
.dir-card:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}
:root[data-theme='dark'] .dir-card {
  background: rgba(28, 35, 48, 0.45);
  border-color: rgba(255, 255, 255, 0.06);
}

.dir-card__thumb {
  position: relative;
  aspect-ratio: 16 / 9;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  overflow: hidden;
  background: var(--panel-muted);
}
.dir-card__thumb-img {
  width: 100%; height: 100%;
  object-fit: cover;
  display: block;
}
.dir-card__thumb-icon { font-size: 56px; opacity: 0.92; }
.dir-card__thumb-initials {
  font-size: 42px; font-weight: 800; letter-spacing: -0.02em;
  text-shadow: 0 2px 6px rgba(0,0,0,0.2);
}
.dir-card__dday {
  position: absolute; top: 10px; left: 10px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--text-primary);
  font-size: 10px; font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.dir-card__dday.urgent {
  background: rgba(248, 113, 113, 0.96);
  color: #fff;
}
.dir-card__status {
  position: absolute; top: 10px; right: 10px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 10px; font-weight: 800;
}
.st--live    { background: rgba(34, 197, 94, 0.92); color: #fff; }
.st--review  { background: rgba(255, 175, 134, 0.96); color: #813A14; }
.st--draft   { background: rgba(255, 255, 255, 0.84); color: var(--text-primary); }
.st--paused  { background: rgba(148, 163, 184, 0.92); color: #fff; }
.st--done    { background: rgba(99, 102, 241, 0.92); color: #fff; }

.dir-card__body {
  padding: 12px 14px 14px;
  display: flex; flex-direction: column; gap: 4px;
}
.dir-card__title {
  margin: 0; font-size: 14px; font-weight: 700;
  color: var(--text-primary); line-height: 1.35;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2;
  overflow: hidden;
}
.dir-card__org {
  margin: 0; font-size: 11px; font-weight: 600;
  color: var(--color-primary-700);
}
.dir-card__summary {
  margin: 4px 0 6px; font-size: 12px; line-height: 1.45;
  color: var(--muted-text);
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2;
  overflow: hidden;
}
.dir-card__tags { display: flex; flex-wrap: wrap; gap: 4px; }
.dir-card__tag {
  font-size: 10px; font-weight: 700;
  padding: 2px 7px; border-radius: 999px;
  background: var(--panel-muted);
  color: var(--text-secondary);
}
.dir-card__tag--more { color: var(--muted-text); font-weight: 600; }

/* ───── State ───── */
.dir-err, .dir-empty {
  margin: 32px 0; text-align: center;
  font-size: 13px; font-weight: 600;
  color: var(--muted-text);
}
.dir-err { color: #DC2626; }
:root[data-theme='dark'] .dir-err { color: #FCA5A5; }
.dir-retry {
  margin-left: 8px;
  height: 26px; padding: 0 10px;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.08);
  border: 1px solid rgba(220, 38, 38, 0.32);
  color: #DC2626; font-size: 11px; font-weight: 700;
  cursor: pointer; font-family: inherit;
}

/* ───── Skeleton ───── */
@keyframes dir-shimmer {
  0% { background-position: -200px 0; } 100% { background-position: 200px 0; }
}
@keyframes dir-fade-in {
  from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); }
}
.fade-in { animation: dir-fade-in 0.32s cubic-bezier(0.22, 0.61, 0.36, 1) both; }
.sk-shimmer {
  background: linear-gradient(90deg,
    var(--panel-muted) 0%,
    color-mix(in srgb, var(--panel-muted) 60%, transparent) 50%,
    var(--panel-muted) 100%);
  background-size: 400px 100%;
  animation: dir-shimmer 1.4s ease-in-out infinite;
  border-radius: 8px;
}
.dir-card--skeleton { padding: 0; }
.dir-card__thumb-sk { width: 100%; aspect-ratio: 16 / 9; border-radius: 0; }
.dir-card__line-sk { width: 80%; height: 14px; margin: 10px 14px 4px; }
.dir-card__line-sk--short { width: 50%; }
:root[data-theme='dark'] .sk-shimmer {
  background: linear-gradient(90deg,
    rgba(255,255,255,0.06) 0%, rgba(255,255,255,0.12) 50%, rgba(255,255,255,0.06) 100%);
  background-size: 400px 100%;
}

/* ───── 반응형 ───── */
@media (max-width: 640px) {
  .dir-root { padding: 20px 16px 40px; }
  .dir-hero__title { font-size: 22px; }
  .dir-hero__search { width: 100%; }
}
</style>
