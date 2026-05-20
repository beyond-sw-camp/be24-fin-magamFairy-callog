<script setup>
/**
 * Zone 2 — 내 캠페인 랭킹 (단일 페이지, 리더보드)
 * campaign-progress(진척률 desc 정렬 완료)를 순위 리스트로 표시.
 *  행 = 순위 · ●컬러 · 캠페인명 · 진척률 막대 · % / isMine 행 강조 / 클릭 → 캠페인 상세
 */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboard'

const router = useRouter()
const store = useDashboardStore()

function publicIdByCampaignId(id) {
  if (id == null) return null
  const hit = (store.myCampaigns ?? []).find((c) => (c.idx ?? c.id) === id)
  return hit?.id ?? hit?.publicId ?? null
}

const rows = computed(() => (store.campaignProgress ?? []).map((c, i) => ({
  rank: i + 1,
  campaignId: c.campaignId,
  name: c.campaignName ?? '캠페인',
  color: c.color || '#9D85FF',
  // record boolean accessor → Jackson 직렬화 키가 isMine/mine 둘 다 대비
  mine: c.isMine ?? c.mine ?? false,
  pct: Math.max(0, Math.min(100, Math.round(c.completionPct ?? 0))),
})))

function open(id) {
  const pid = publicIdByCampaignId(id)
  if (pid) router.push(`/campaigns/${pid}`)
}
</script>

<template>
  <section class="card zone2" aria-label="내 캠페인 랭킹">
    <div class="card-h">
      <div>
        <h2>내 캠페인 랭킹</h2>
        <p class="lede">진척률(업무 완료율) 높은 순</p>
      </div>
    </div>

    <ul v-if="rows.length" class="z2-list">
      <li
        v-for="r in rows"
        :key="r.campaignId ?? r.rank"
        class="z2-row"
        :class="{ 'is-mine': r.mine }"
        @click="open(r.campaignId)"
      >
        <span class="z2-badge" :class="{ crown: r.rank === 1 }" :style="{ background: r.color }">{{ r.rank }}</span>
        <div class="z2-mid">
          <div class="z2-name">
            {{ r.name }}
            <span v-if="r.mine" class="z2-mine-tag">내 담당</span>
          </div>
          <div class="z2-bar"><span class="z2-bar-fill" :style="{ width: r.pct + '%', background: r.color }" /></div>
        </div>
        <span class="z2-pct">{{ r.pct }}%</span>
      </li>
    </ul>
    <div v-else class="z2-empty">표시할 캠페인이 없습니다.</div>
  </section>
</template>

<style scoped>
.zone2 { display: flex; flex-direction: column; }
.card-h { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 18px; gap: 12px; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; }
.card-h .lede { margin: 4px 0 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); }

.z2-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; flex: 1; }
.z2-row { display: flex; align-items: center; gap: 13px; padding: 12px 14px; border-radius: 16px; background: var(--lp-surface-soft); cursor: pointer; transition: background .15s ease, transform .12s ease; }
.z2-row:hover { background: var(--lp-border); }
.z2-row:active { transform: scale(.99); }
.z2-row.is-mine { background: rgba(157,133,255,.12); border: 1px solid rgba(157,133,255,.35); }
.z2-row.is-mine:hover { background: rgba(157,133,255,.18); }
.z2-badge {
  position: relative; width: 38px; height: 38px; border-radius: 999px; flex-shrink: 0;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-size: 15px; font-weight: 800; font-variant-numeric: tabular-nums;
  box-shadow: 0 2px 6px rgba(63,52,99,.18);
}
.z2-badge.crown::after {
  content: '★'; position: absolute; top: -6px; right: -6px;
  width: 18px; height: 18px; border-radius: 999px;
  background: var(--lp-lime); color: var(--lp-primary-deep);
  font-size: 10px; display: inline-flex; align-items: center; justify-content: center;
  box-shadow: 0 1px 3px rgba(63,52,99,.25);
}
.z2-mid { flex: 1; min-width: 0; }
.z2-name { font-size: 14px; font-weight: 700; color: var(--lp-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; gap: 6px; }
.z2-mine-tag { font-size: 9.5px; font-weight: 700; color: var(--lp-primary-deep); background: rgba(157,133,255,.2); padding: 1px 6px; border-radius: 999px; flex-shrink: 0; }
.z2-bar { margin-top: 7px; height: 6px; border-radius: 999px; background: var(--lp-border); overflow: hidden; }
.z2-bar-fill { display: block; height: 100%; border-radius: 999px; transition: width .6s cubic-bezier(.4,0,.2,1); }
.z2-pct { font-size: 20px; font-weight: 800; color: var(--lp-text); flex-shrink: 0; min-width: 46px; text-align: right; font-variant-numeric: tabular-nums; letter-spacing: -0.02em; }

.z2-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; }
</style>
