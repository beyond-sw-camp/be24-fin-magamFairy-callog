<script setup>
/**
 * Zone 2 — 내 캠페인 랭킹 (단일 페이지, 변형 B 바차트)
 * campaignProgress(진척률 desc 정렬)를 가로 막대 랭킹으로 표시.
 *  행 = 순위(1위 ★) · 캠페인명 · 내 담당 알약 · % / 22px 트랙 + solid fill + shine
 *  행 hover translateY(-1px) / 클릭 → 캠페인 상세
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

const rows = computed(() => {
  const list = (store.campaignProgress ?? []).map((c) => ({
    campaignId: c.campaignId,
    name: c.campaignName ?? '캠페인',
    color: c.color || '#9D85FF',
    // record boolean accessor → Jackson 직렬화 키가 isMine/mine 둘 다 대비
    mine: c.isMine ?? c.mine ?? false,
    pct: Math.max(0, Math.min(100, Math.round(c.completionPct ?? 0))),
  }))
  // 방어적으로 진척률 내림차순 정렬
  list.sort((a, b) => b.pct - a.pct)
  return list.map((r, i) => ({ ...r, rank: i + 1 }))
})

function open(id) {
  const pid = publicIdByCampaignId(id)
  if (pid) router.push(`/campaigns/${pid}`)
}
</script>

<template>
  <section class="card zone2" aria-label="내 캠페인 랭킹">
    <div class="card-h">
      <div class="card-h-ttl">
        <h2>내 캠페인 랭킹</h2>
        <span class="card-dot" />
        <p class="lede">진척률(업무 완료율) 높은 순</p>
      </div>
    </div>

    <ul v-if="rows.length" class="z2-list">
      <li
        v-for="r in rows"
        :key="r.campaignId ?? r.rank"
        class="z2-row"
        :class="{ r1: r.rank === 1 }"
        @click="open(r.campaignId)"
      >
        <div class="z2-row-h">
          <span class="z2-rank">{{ r.rank }}</span>
          <span class="z2-name">
            {{ r.name }}
            <span v-if="r.mine" class="z2-mine-tag">내 담당</span>
          </span>
          <span class="z2-pct">{{ r.pct }}%</span>
        </div>
        <div class="z2-track">
          <span class="z2-track-fill" :style="{ width: r.pct + '%', background: r.color }" />
        </div>
      </li>
    </ul>
    <div v-else class="z2-empty">표시할 캠페인이 없습니다.</div>
  </section>
</template>

<style scoped>
.zone2 { display: flex; flex-direction: column; }
.card-h { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; gap: 12px; }
.card-h-ttl { display: flex; align-items: baseline; gap: 8px; min-width: 0; }
.card-dot { width: 9px; height: 9px; border-radius: 999px; background: var(--lp-primary); align-self: center; flex-shrink: 0; }
.card-h h2 { margin: 0; font-size: 18px; font-weight: 700; color: var(--lp-text); letter-spacing: -0.01em; flex-shrink: 0; }
.card-h .lede { margin: 0; font-size: 12px; font-weight: 500; color: var(--lp-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.z2-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; overflow-y: auto; flex: 1; }
.z2-row { cursor: pointer; transition: transform .12s ease; animation: lp-rise .4s cubic-bezier(.4,0,.2,1) both; }
.z2-row:nth-child(1) { animation-delay: .04s; }
.z2-row:nth-child(2) { animation-delay: .10s; }
.z2-row:nth-child(3) { animation-delay: .16s; }
.z2-row:nth-child(4) { animation-delay: .22s; }
.z2-row:nth-child(n+5) { animation-delay: .28s; }
.z2-row:hover { transform: translateY(-1px); }

.z2-row-h { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.z2-rank {
  width: 20px; flex-shrink: 0;
  font-size: 13px; font-weight: 800; color: var(--lp-text-faint);
  font-variant-numeric: tabular-nums;
}
.z2-row.r1 .z2-rank { color: var(--lp-primary-deep); }
.z2-row.r1 .z2-rank::before {
  content: '★ ';
  /* base.css에 --lp-lime-deep 미정의 → 로컬 fallback */
  color: var(--lp-lime-deep, #A8BD42);
}
.z2-name {
  flex: 1; min-width: 0;
  font-size: 13px; font-weight: 700; color: var(--lp-text);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  display: flex; align-items: center;
}
.z2-mine-tag {
  flex-shrink: 0;
  font-size: 9.5px; font-weight: 700; color: var(--lp-primary-deep);
  background: var(--accent-soft);
  padding: 2px 7px; border-radius: 999px; margin-left: 6px;
}
.z2-pct {
  flex-shrink: 0;
  font-size: 16px; font-weight: 800; color: var(--lp-text);
  font-variant-numeric: tabular-nums; letter-spacing: -0.01em;
}

.z2-track {
  height: 22px; border-radius: 8px;
  background: var(--lp-surface-soft); overflow: hidden;
}
.z2-track-fill {
  position: relative; display: block; height: 100%;
  border-radius: 8px;
  transition: width .9s cubic-bezier(.4,0,.2,1);
  transform-origin: left;
  animation: lp-grow-x .8s cubic-bezier(.4,0,.2,1) both;
}
.z2-track-fill::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,.25) 50%, transparent 100%);
}

.z2-empty { flex: 1; display: flex; align-items: center; justify-content: center; font-size: 12.5px; color: var(--lp-text-faint); padding: 24px; }
</style>
