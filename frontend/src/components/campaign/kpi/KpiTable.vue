<template>
  <div class="kpi-table-wrap">
    <table class="kpi-table">
      <thead>
        <tr>
          <th>이름</th>
          <th>분류</th>
          <th>목표</th>
          <th>실적</th>
          <th>달성률</th>
          <th>상태</th>
          <th>담당자</th>
          <th>최종 측정</th>
          <th v-if="editable">작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="kpi in items" :key="kpi.idx">
          <td>{{ kpi.name }}</td>
          <td>{{ categoryLabel(kpi.category) }}</td>
          <td>{{ kpi.targetValue }} {{ kpi.unit }}</td>
          <td>{{ kpi.actualValue != null ? `${kpi.actualValue} ${kpi.unit}` : '-' }}</td>
          <td>{{ kpi.achievementPercent != null ? kpi.achievementPercent + '%' : '-' }}</td>
          <td><span :class="`badge badge--${kpi.status?.toLowerCase()}`">{{ statusLabel(kpi.status) }}</span></td>
          <td>{{ kpi.ownerLabel || kpi.ownerUserName || '-' }}</td>
          <td>{{ kpi.measuredAt ? fmtDate(kpi.measuredAt) : '-' }}</td>
          <td v-if="editable" class="actions">
            <button class="act" @click="$emit('editActual', kpi)">실적</button>
            <button class="act" @click="$emit('editMeta', kpi)">수정</button>
            <button class="act act--danger" @click="$emit('delete', kpi.idx)">삭제</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({ items: Array, editable: Boolean })
defineEmits(['editActual', 'editMeta', 'delete'])

const CAT = { IMPRESSION: '노출도', ENGAGEMENT: '참여도', CONVERSION: '전환', REVENUE: '매출/ROI', BRAND: '브랜드', ESG: 'ESG', OTHER: '기타' }
const ST = { OVER: '초과달성', ACHIEVED: '달성', BEHIND: '미달', PENDING: '대기' }
const categoryLabel = (c) => CAT[c] ?? c
const statusLabel = (s) => ST[s] ?? s
const fmtDate = (dt) => new Date(dt).toLocaleDateString('ko-KR')
</script>

<style scoped>
.kpi-table-wrap {
  overflow-x: auto;
}

.kpi-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.kpi-table th,
.kpi-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  white-space: nowrap;
}

.kpi-table th {
  background: var(--panel-color);
  font-weight: 600;
  font-size: 12px;
  color: var(--muted-text);
}

.badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.badge--over {
  background: #dcfce7;
  color: #15803d;
}

.badge--achieved {
  background: #dbeafe;
  color: #1d4ed8;
}

.badge--behind {
  background: #fee2e2;
  color: #dc2626;
}

.badge--pending {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.actions {
  display: flex;
  gap: 6px;
}

.act {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.act:hover:not(:disabled) {
  background: var(--panel-muted);
  border-color: var(--border-strong);
}

.act--danger {
  color: #dc2626;
}
</style>
