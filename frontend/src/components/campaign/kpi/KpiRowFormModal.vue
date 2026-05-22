<template>
  <div class="modal-backdrop" @click.self="$emit('close')">
    <div class="modal">
      <h3 class="modal__title">{{ editTarget ? 'KPI 수정' : 'KPI 추가' }}</h3>

      <!-- ───── 상위 KPI에서 import (A) — 신규 등록 시만 ───── -->
      <div v-if="!editTarget" class="import-block">
        <button
          type="button"
          class="import-toggle"
          :class="{ 'is-open': importOpen }"
          @click="onToggleImport"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          <span>{{ form.parentOrgKpiId
            ? `📌 ${form.parentOrgKpiName ?? '상위'} 에서 import 됨 (해제)`
            : '상위 KPI에서 import (자동 채움)' }}</span>
          <svg v-if="!form.parentOrgKpiId" class="import-toggle__caret" width="12" height="12"
               viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
               stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </button>

        <div v-if="importOpen && !form.parentOrgKpiId" class="import-list">
          <p v-if="orgKpiStore.loading" class="import-list__msg">불러오는 중…</p>
          <p v-else-if="importableKpis.length === 0" class="import-list__msg">
            현재 활성(ACTIVE) 상위 KPI가 없습니다.
          </p>
          <ul v-else class="import-list__items">
            <li v-for="kpi in importableKpis" :key="kpi.idx">
              <button
                type="button"
                class="import-item"
                @click="applyImport(kpi)"
              >
                <span class="import-item__pill" :class="kpi.ownerOrgType === 'HQ' ? 'pill--hq' : 'pill--ga'">
                  {{ kpi.ownerOrgType === 'HQ' ? '본사' : (kpi.ownerOrgName ?? '계열사') }}
                </span>
                <span class="import-item__name">{{ kpi.name }}</span>
                <span class="import-item__target">{{ kpi.targetValue }}{{ kpi.unit ? ` ${kpi.unit}` : '' }}</span>
                <span class="import-item__period">{{ kpi.periodCode }}</span>
              </button>
            </li>
          </ul>
        </div>
      </div>

      <!-- ───── 폼 ───── -->
      <div class="form">
        <label>
          이름
          <input v-model="form.name" type="text" placeholder="예: 총 노출 수" />
        </label>
        <label>
          분류
          <select v-model="form.category">
            <option v-for="(label, key) in CATEGORIES" :key="key" :value="key">
              {{ label }}
            </option>
          </select>
        </label>
        <label>
          목표값
          <input v-model.number="form.targetValue" type="number" />
        </label>
        <label>
          단위
          <input v-model="form.unit" type="text" placeholder="Views, %, 원 …" />
        </label>
        <label>
          담당자 표시명
          <input v-model="form.ownerLabel" type="text" />
        </label>
      </div>

      <div class="modal__footer">
        <button class="btn btn--outline" @click="$emit('close')">취소</button>
        <button class="btn btn--primary" :disabled="!canSubmit" @click="submit">저장</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, ref } from 'vue'
import { useOrganizationKpiStore } from '@/stores/organizationKpi'

const props = defineProps({ editTarget: Object })
const emit = defineEmits(['close', 'submit'])

const orgKpiStore = useOrganizationKpiStore()

const CATEGORIES = {
  GROWTH: '성장',
  FINANCIAL: '재무',
  BRAND: '브랜드',
  OPERATIONAL: '운영',
  SUSTAINABILITY: '지속가능성'
}

const form = reactive({
  name: props.editTarget?.name ?? '',
  category: props.editTarget?.category ?? 'GROWTH',
  targetValue: props.editTarget?.targetValue ?? null,
  unit: props.editTarget?.unit ?? '',
  ownerLabel: props.editTarget?.ownerLabel ?? '',
  ownerUserIdx: props.editTarget?.ownerUserIdx ?? null,
  // A: 상위 KPI cascade
  parentOrgKpiId: props.editTarget?.parentOrgKpiId ?? null,
  parentOrgKpiName: props.editTarget?.parentOrgKpiName ?? null,
})

const canSubmit = computed(() => form.name && form.targetValue && form.unit)

function submit() {
  emit('submit', { ...form })
}

/* ───── 상위 KPI import (A) ───── */
const importOpen = ref(false)

async function onToggleImport() {
  if (form.parentOrgKpiId) {
    // 해제
    form.parentOrgKpiId = null
    form.parentOrgKpiName = null
    return
  }
  importOpen.value = !importOpen.value
  if (importOpen.value && (orgKpiStore.items?.length ?? 0) === 0) {
    await orgKpiStore.fetch({ status: 'ACTIVE' })
  }
}

const importableKpis = computed(() =>
  (orgKpiStore.items ?? []).filter((k) => k.status === 'ACTIVE'),
)

function applyImport(kpi) {
  // 폼 자동 채움 (사용자가 수정 가능)
  form.name = kpi.name
  form.unit = kpi.unit ?? form.unit
  form.category = kpi.category ?? form.category
  // targetValue는 사용자가 직접 입력 (캠페인이 기여하는 양)
  form.parentOrgKpiId = kpi.idx
  form.parentOrgKpiName = `${kpi.ownerOrgType === 'HQ' ? '본사' : (kpi.ownerOrgName ?? '계열사')} · ${kpi.name}`
  importOpen.value = false
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: var(--panel-color);
  border-radius: 10px;
  padding: 24px;
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: var(--shadow-sm);
}

.modal__title {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--text-primary);
}

/* ───── Import 섹션 ───── */
.import-block {
  margin-bottom: 16px;
  padding: 10px;
  background: var(--color-primary-50);
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 25%, transparent);
  border-radius: 8px;
}
.import-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 0;
  padding: 4px 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary-700);
  cursor: pointer;
  font-family: inherit;
  width: 100%;
  text-align: left;
}
.import-toggle__caret { transition: transform var(--transition-fast); margin-left: auto; }
.import-toggle.is-open .import-toggle__caret { transform: rotate(180deg); }
.import-toggle:hover { color: var(--color-primary-800); }

.import-list {
  margin-top: 8px;
  background: var(--panel-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  max-height: 220px;
  overflow-y: auto;
}
.import-list__msg {
  padding: 14px;
  text-align: center;
  font-size: 12px;
  color: var(--muted-text);
  margin: 0;
}
.import-list__items {
  list-style: none;
  margin: 0;
  padding: 4px;
}
.import-item {
  display: grid;
  grid-template-columns: 50px 1fr auto auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  background: transparent;
  border: 0;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  font-variant-numeric: tabular-nums;
  transition: background var(--transition-fast);
}
.import-item:hover { background: var(--panel-muted); }
.import-item__pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 18px;
  padding: 0 6px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.04em;
  border-radius: 999px;
  text-transform: uppercase;
}
.pill--hq { background: var(--color-primary-100); color: var(--color-primary-700); }
.pill--ga { background: #dbeafe; color: #2563eb; }
.import-item__name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.import-item__target {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary-700);
}
.import-item__period {
  font-size: 11px;
  color: var(--muted-text);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--muted-text);
  font-weight: 500;
}

.form input,
.form select {
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--panel-color);
  transition: border-color var(--transition-fast);
}

.form input:focus,
.form select:focus {
  outline: none;
  border-color: var(--border-strong);
}

.modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  font-weight: 600;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--primary {
  background: var(--color-primary-500);
  color: #fff;
}

.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-600);
}

.btn--outline {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
}

.btn--outline:hover:not(:disabled) {
  background: var(--panel-muted);
  border-color: var(--border-strong);
}
</style>
