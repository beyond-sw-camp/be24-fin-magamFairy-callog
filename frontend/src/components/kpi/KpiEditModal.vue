<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useOrganizationKpiStore } from '@/stores/organizationKpi'
import KpiTemplatePicker from './KpiTemplatePicker.vue'

const props = defineProps({
  mode: { type: String, default: 'create' },
  initialValues: { type: Object, default: null },
  defaultOwnerOrgId: { type: [Number, String, null], default: null },
  defaultOwnerOrgType: { type: String, default: 'HQ' },
})

const emit = defineEmits(['close', 'submit'])

const store = useOrganizationKpiStore()

const CATEGORY_OPTIONS = [
  { value: 'OTHER', label: '기타' },
  { value: 'IMPRESSION', label: '노출' },
  { value: 'ENGAGEMENT', label: '참여' },
  { value: 'CONVERSION', label: '전환' },
  { value: 'REVENUE', label: '매출' },
  { value: 'BRAND', label: '브랜드' },
]
const ESG_OPTIONS = [
  { value: 'ENVIRONMENTAL', label: '환경 (E)' },
  { value: 'SOCIAL', label: '사회 (S)' },
  { value: 'GOVERNANCE', label: '거버넌스 (G)' },
]

/* ───── 기간 옵션 자동 생성 (이번/다음 분기 + 올해/내년 연간 + 직접 지정) ───── */
const PERIOD_OPTIONS = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const currentQ = Math.ceil(month / 3)
  const nextQ = currentQ === 4 ? 1 : currentQ + 1
  const nextQYear = currentQ === 4 ? year + 1 : year
  return [
    { value: `${year}-Q${currentQ}`, label: `${year} Q${currentQ} · 이번 분기`, type: 'QUARTERLY' },
    { value: `${nextQYear}-Q${nextQ}`, label: `${nextQYear} Q${nextQ} · 다음 분기`, type: 'QUARTERLY' },
    { value: `${year}-FY`, label: `${year}년 (연간)`, type: 'ANNUAL' },
    { value: `${year + 1}-FY`, label: `${year + 1}년 (내년 연간)`, type: 'ANNUAL' },
    { value: '__CUSTOM__', label: '직접 지정...', type: 'CUSTOM' },
  ]
})

function defaultPeriod() {
  return PERIOD_OPTIONS.value[0]
}

/* periodCode → start/end 자동 산출 */
function derivePeriodDates(code) {
  if (!code) return { start: '', end: '' }
  const qMatch = code.match(/^(\d{4})-Q([1-4])$/)
  if (qMatch) {
    const y = parseInt(qMatch[1])
    const q = parseInt(qMatch[2])
    const startMonth = (q - 1) * 3 + 1
    const endMonth = q * 3
    const start = `${y}-${String(startMonth).padStart(2, '0')}-01`
    const lastDay = new Date(y, endMonth, 0).getDate()
    const end = `${y}-${String(endMonth).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
    return { start, end }
  }
  const yMatch = code.match(/^(\d{4})(?:-FY)?$/)
  if (yMatch) {
    const y = yMatch[1]
    return { start: `${y}-01-01`, end: `${y}-12-31` }
  }
  return { start: '', end: '' }
}

function inferPeriodTypeFromCode(code) {
  if (!code) return 'QUARTERLY'
  if (/^\d{4}-Q[1-4]$/.test(code)) return 'QUARTERLY'
  if (/^\d{4}(-FY)?$/.test(code)) return 'ANNUAL'
  return 'CUSTOM'
}

function emptyForm() {
  const def = defaultPeriod()
  const dates = derivePeriodDates(def.value)
  return {
    name: '',
    ownerOrgId: props.defaultOwnerOrgId ?? null,
    parentKpiId: null,
    contributionToParent: null,
    periodSelect: def.value,
    periodType: def.type,
    periodCode: def.value,
    periodStart: dates.start,
    periodEnd: dates.end,
    targetValue: null,
    unit: '',
    category: 'OTHER',
    esgEnabled: false,
    esgCategory: '',
    kind: props.defaultOwnerOrgType === 'HQ' ? 'STRATEGIC' : 'TACTICAL',
    status: 'DRAFT',
    achievabilityNote: '',
    templateId: null,
    visibleToAffiliate: false,
  }
}

// 소유 조직 유형 (토글/매핑 강제 분기)
const isHqOwner = computed(() => props.defaultOwnerOrgType === 'HQ')
const isAffiliateOwner = computed(() => props.defaultOwnerOrgType === 'AFFILIATE')

const form = reactive(emptyForm())
const showAdvanced = ref(false)
const showTemplate = ref(false)
const submitError = ref('')
const isSubmitting = ref(false)

watch(
  () => [props.mode, props.initialValues],
  () => {
    if (props.mode === 'edit' && props.initialValues) {
      const iv = props.initialValues
      // 기간 매칭: 알려진 옵션이면 그 value, 아니면 CUSTOM
      const matched = PERIOD_OPTIONS.value.find(
        (o) => o.value === iv.periodCode && o.value !== '__CUSTOM__',
      )
      Object.assign(form, emptyForm(), {
        ...iv,
        periodSelect: matched ? iv.periodCode : '__CUSTOM__',
        periodType: iv.periodType ?? inferPeriodTypeFromCode(iv.periodCode),
        periodCode: iv.periodCode ?? '',
        periodStart: iv.periodStart ?? '',
        periodEnd: iv.periodEnd ?? '',
        esgEnabled: !!iv.esgCategory,
        esgCategory: iv.esgCategory ?? '',
      })
      // 편집 모드에서 cascade·ESG·메모가 있으면 더보기 자동 펼침
      if (iv.parentKpiId || iv.esgCategory || iv.achievabilityNote) {
        showAdvanced.value = true
      }
    } else {
      Object.assign(form, emptyForm())
      // 계열사 상위 KPI는 상단에 별도 표시 → 더보기는 기본 접힘
      showAdvanced.value = false
    }
    // 계열사면 상위(본사) KPI 후보 미리 로드
    if (isAffiliateOwner.value && form.ownerOrgId) {
      void store.fetchParentCandidates(form.ownerOrgId)
    }
    submitError.value = ''
  },
  { immediate: true, deep: true },
)

/* periodSelect 변경 시 자동 채움 */
watch(
  () => form.periodSelect,
  (next) => {
    const opt = PERIOD_OPTIONS.value.find((o) => o.value === next)
    if (!opt) return
    if (opt.value === '__CUSTOM__') {
      form.periodType = 'CUSTOM'
      // 코드/날짜는 사용자가 직접 입력
    } else {
      form.periodType = opt.type
      form.periodCode = opt.value
      const dates = derivePeriodDates(opt.value)
      form.periodStart = dates.start
      form.periodEnd = dates.end
    }
  },
)

watch(
  () => form.parentKpiId,
  (next) => {
    if (next && form.ownerOrgId) {
      void store.fetchParentCandidates(form.ownerOrgId)
    }
  },
)

const titleLabel = computed(() => (props.mode === 'edit' ? 'KPI 수정' : '새 분기 KPI'))
const submitLabel = computed(() => (props.mode === 'edit' ? '저장' : '등록'))
const isCustomPeriod = computed(() => form.periodSelect === '__CUSTOM__')

const validation = computed(() => {
  const errs = {}
  if (!form.name?.trim()) errs.name = '이름을 입력해 주세요.'
  if (!form.targetValue || Number(form.targetValue) <= 0)
    errs.targetValue = '목표값(0보다 큰 수)을 입력해 주세요.'
  if (!form.unit?.trim()) errs.unit = '단위를 입력해 주세요.'
  if (isCustomPeriod.value) {
    if (!form.periodCode?.trim()) errs.periodCode = '기간 코드 (예: 2026-H1)'
    if (!form.periodStart) errs.periodStart = '시작일을 선택해 주세요.'
    if (!form.periodEnd) errs.periodEnd = '종료일을 선택해 주세요.'
  }
  // 계열사 KPI 는 반드시 본사(상위) KPI 에 매핑되어야 함
  if (isAffiliateOwner.value && !form.parentKpiId) {
    errs.parentKpiId = '계열사 KPI는 본사 KPI에 매핑해야 합니다.'
  }
  return errs
})

const isValid = computed(() => Object.keys(validation.value).length === 0)

async function applyTemplate(template) {
  if (!template) return
  form.templateId = template.idx
  if (template.name && !form.name) form.name = template.name
  if (template.defaultUnit && !form.unit) form.unit = template.defaultUnit
  // category는 default 'OTHER'라 사용자가 안 바꿨으면 템플릿 값으로 override
  if (template.defaultCategory && (!form.category || form.category === 'OTHER')) {
    form.category = template.defaultCategory
  }
  if (template.defaultKind) form.kind = template.defaultKind
  if (template.defaultEsgCategory) {
    form.esgEnabled = true
    form.esgCategory = template.defaultEsgCategory
  }
  showTemplate.value = false
}

function toggleEsg() {
  form.esgEnabled = !form.esgEnabled
  if (!form.esgEnabled) form.esgCategory = ''
  else if (!form.esgCategory) form.esgCategory = 'ENVIRONMENTAL'
}

async function submit() {
  if (!isValid.value) {
    submitError.value = '필수 항목을 확인해 주세요.'
    return
  }
  submitError.value = ''
  isSubmitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      ownerOrgId: form.ownerOrgId,
      // 자동 추론 — 백엔드에서도 동일한 로직 fallback
      periodType: form.periodType,
      periodCode: form.periodCode.trim(),
      periodStart: form.periodStart || null,
      periodEnd: form.periodEnd || null,
      targetValue: Number(form.targetValue),
      unit: form.unit.trim(),
      category: form.category,
      kind: form.kind,
      status: form.status,
      // 옵션
      parentKpiId: form.parentKpiId || null,
      contributionToParent:
        form.parentKpiId && form.contributionToParent
          ? Number(form.contributionToParent)
          : null,
      esgCategory: form.esgEnabled && form.esgCategory ? form.esgCategory : null,
      achievabilityNote: form.achievabilityNote?.trim() || null,
      templateId: form.templateId || null,
      // HQ 소유일 때만 계열사 노출 토글 전송 (그 외는 false)
      visibleToAffiliate: isHqOwner.value ? !!form.visibleToAffiliate : false,
    }
    emit('submit', payload)
  } finally {
    isSubmitting.value = false
  }
}

const parentOptions = computed(() => store.parentCandidates ?? [])
</script>

<template>
  <Teleport to="body">
    <div class="kpi-overlay" role="presentation" @click.self="emit('close')">
      <section
        class="kpi-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="kpi-edit-title"
      >
        <header class="kpi-modal__head">
          <div>
            <span class="kpi-modal__eyebrow">KPI · 분기 목표</span>
            <h2 id="kpi-edit-title">{{ titleLabel }}</h2>
          </div>
          <button class="kpi-modal__close" aria-label="닫기" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div v-if="props.mode === 'create'" class="kpi-modal__template">
          <button
            type="button"
            class="kpi-modal__tpl-toggle"
            :class="{ 'is-open': showTemplate }"
            @click="showTemplate = !showTemplate"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect width="18" height="18" x="3" y="3" rx="2" />
              <path d="M9 9h6v6H9z" />
            </svg>
            <span>템플릿에서 시작 {{ showTemplate ? '닫기' : '' }}</span>
          </button>
          <KpiTemplatePicker
            v-if="showTemplate"
            v-model="form.templateId"
            @apply="applyTemplate"
          />
        </div>

        <form class="kpi-modal__body" @submit.prevent="submit">
          <!-- ─── 계열사: 상위 본사 KPI 매핑 (필수, 최상단) ─── -->
          <div v-if="isAffiliateOwner" class="parent-map">
            <div class="grid-2">
              <div class="field-row">
                <label class="lbl">
                  <span>상위 본사 KPI <em class="hint-em" style="color: var(--danger-text-strong, #DC2626)">필수</em></span>
                </label>
                <select v-model="form.parentKpiId" class="fld">
                  <option :value="null">— 본사 KPI 선택 —</option>
                  <option v-for="p in parentOptions" :key="p.idx" :value="p.idx">
                    {{ p.name }} ({{ p.periodCode }})
                  </option>
                </select>
                <p v-if="validation.parentKpiId" class="err">{{ validation.parentKpiId }}</p>
              </div>
              <div class="field-row">
                <label class="lbl"><span>약속 기여값 <em class="hint-em">상위에 기여하는 양</em></span></label>
                <input
                  v-model.number="form.contributionToParent"
                  type="number"
                  class="fld"
                  :disabled="!form.parentKpiId"
                  placeholder="예: 30"
                />
              </div>
            </div>
            <p v-if="!parentOptions.length" class="hint">
              본사가 노출(ON)한 KPI가 없습니다. 본사에서 먼저 KPI를 만들고 "계열사에 노출"을 켜야 합니다.
            </p>
          </div>

          <!-- ─── 필수 4개 ─── -->
          <div class="field-row">
            <label class="lbl"><span>이름</span></label>
            <input
              v-model="form.name"
              type="text"
              class="fld"
              placeholder="예: Q3 신규 제휴사 25곳 확보"
              autofocus
            />
            <p v-if="validation.name" class="err">{{ validation.name }}</p>
          </div>

          <div class="grid-3">
            <div class="field-row">
              <label class="lbl"><span>목표값</span></label>
              <input
                v-model.number="form.targetValue"
                type="number"
                class="fld"
                step="any"
                placeholder="예: 25"
              />
              <p v-if="validation.targetValue" class="err">{{ validation.targetValue }}</p>
            </div>
            <div class="field-row">
              <label class="lbl"><span>단위</span></label>
              <input
                v-model="form.unit"
                type="text"
                class="fld"
                placeholder="곳, 건, %, 원"
              />
              <p v-if="validation.unit" class="err">{{ validation.unit }}</p>
            </div>
            <div class="field-row">
              <label class="lbl"><span>분류</span></label>
              <select v-model="form.category" class="fld">
                <option v-for="opt in CATEGORY_OPTIONS" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
          </div>

          <div class="field-row">
            <label class="lbl"><span>기간</span></label>
            <select v-model="form.periodSelect" class="fld">
              <option v-for="opt in PERIOD_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
            <!-- CUSTOM 모드일 때만 코드 + 날짜 입력 -->
            <div v-if="isCustomPeriod" class="custom-period">
              <input
                v-model="form.periodCode"
                type="text"
                class="fld"
                placeholder="기간 코드 (예: 2026-H1)"
              />
              <div class="dual-input">
                <input v-model="form.periodStart" type="date" class="fld" />
                <input v-model="form.periodEnd" type="date" class="fld" />
              </div>
              <p v-if="validation.periodCode" class="err">{{ validation.periodCode }}</p>
              <p v-if="validation.periodStart" class="err">{{ validation.periodStart }}</p>
              <p v-if="validation.periodEnd" class="err">{{ validation.periodEnd }}</p>
            </div>
            <p v-else class="hint">
              {{ form.periodStart }} ~ {{ form.periodEnd }} (자동 계산)
            </p>
          </div>

          <!-- ─── HQ 전용: 계열사 노출 토글 ─── -->
          <div v-if="isHqOwner" class="visible-toggle">
            <label class="esg-toggle">
              <input type="checkbox" v-model="form.visibleToAffiliate" />
              <span>계열사에 노출</span>
            </label>
            <p class="hint">켜면 계열사가 이 본사 KPI를 자기 목표의 상위로 매핑할 수 있습니다.</p>
          </div>

          <!-- ─── 더보기 (옵션) ─── -->
          <button
            type="button"
            class="advanced-toggle"
            :class="{ 'is-open': showAdvanced }"
            @click="showAdvanced = !showAdvanced"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m6 9 6 6 6-6" />
            </svg>
            <span>{{ showAdvanced ? '간단히' : '더보기 — 상위 KPI · ESG · 합리성 메모' }}</span>
          </button>

          <div v-if="showAdvanced" class="advanced">
            <!-- 상위 KPI cascade (계열사는 상단에 별도 표시하므로 여기선 비계열사만) -->
            <div v-if="!isAffiliateOwner" class="grid-2">
              <div class="field-row">
                <label class="lbl">
                  <span>상위 KPI <em class="hint-em">선택 — 자체 목표면 비워두세요</em></span>
                </label>
                <select v-model="form.parentKpiId" class="fld">
                  <option :value="null">— 자체 목표 —</option>
                  <option v-for="p in parentOptions" :key="p.idx" :value="p.idx">
                    {{ p.name }} ({{ p.periodCode }})
                  </option>
                </select>
              </div>
              <div class="field-row">
                <label class="lbl">
                  <span>약속 기여값 <em class="hint-em">상위 KPI 선택 시</em></span>
                </label>
                <input
                  v-model.number="form.contributionToParent"
                  type="number"
                  class="fld"
                  :disabled="!form.parentKpiId"
                  placeholder="상위에 기여하는 양"
                />
              </div>
            </div>

            <!-- ESG -->
            <div class="field-row">
              <label class="esg-toggle">
                <input
                  type="checkbox"
                  :checked="form.esgEnabled"
                  @change="toggleEsg"
                />
                <span>ESG 분류 활성화</span>
              </label>
              <select
                v-if="form.esgEnabled"
                v-model="form.esgCategory"
                class="fld"
              >
                <option v-for="opt in ESG_OPTIONS" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>

            <!-- 합리성 -->
            <div class="field-row">
              <label class="lbl">
                <span>합리성 근거 <em class="hint-em">선택</em></span>
              </label>
              <textarea
                v-model="form.achievabilityNote"
                rows="2"
                class="fld fld--text"
                placeholder="과거 추세, 투입 리소스, 캠페인 라인업 근거 (선택)"
              />
            </div>

            <!-- edit 모드에서만 status 토글 -->
            <div v-if="props.mode === 'edit'" class="field-row">
              <label class="lbl"><span>상태</span></label>
              <div class="status-toggle">
                <button
                  v-for="status in ['DRAFT', 'ACTIVE', 'ARCHIVED']"
                  :key="status"
                  type="button"
                  class="status-toggle__btn"
                  :class="{ 'is-active': form.status === status }"
                  @click="form.status = status"
                >
                  {{ status === 'DRAFT' ? '초안' : status === 'ACTIVE' ? '활성' : '보관' }}
                </button>
              </div>
            </div>
          </div>

          <p v-if="submitError" class="err err--global">{{ submitError }}</p>
        </form>

        <footer class="kpi-modal__foot">
          <button class="btn btn--ghost" type="button" @click="emit('close')">취소</button>
          <button
            class="btn btn--primary"
            type="button"
            :disabled="!isValid || isSubmitting"
            @click="submit"
          >
            {{ submitLabel }}
          </button>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.kpi-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
  background: rgba(15, 23, 42, 0.46);
  overflow-y: auto;
}

.kpi-modal {
  width: min(640px, 100%);
  max-height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  background: var(--panel-color);
  border-radius: 24px;
  border: 1px solid var(--border-color);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.26);
  overflow: hidden;
  color: var(--text-primary);
}

.kpi-modal__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 22px 28px 12px;
}
.kpi-modal__eyebrow {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--muted-text);
  text-transform: uppercase;
}
.kpi-modal__head h2 {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
}
.kpi-modal__close {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
}
.kpi-modal__close:hover { background: var(--panel-color); color: var(--text-primary); }

.kpi-modal__template {
  padding: 0 28px 8px;
}
.kpi-modal__tpl-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 20%, transparent);
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  margin-bottom: 8px;
}
.kpi-modal__tpl-toggle:hover { background: var(--color-primary-100); }

.kpi-modal__body {
  padding: 12px 28px 8px;
  overflow-y: auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field-row { display: flex; flex-direction: column; gap: 6px; }
.lbl {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
}
.lbl em.hint-em {
  font-style: normal;
  font-weight: 500;
  color: var(--muted-text);
  margin-left: 6px;
  font-size: 11px;
}

.fld {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--control-color);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-family: inherit;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}
.fld--text {
  height: auto;
  padding: 10px 12px;
  resize: vertical;
  line-height: 1.5;
}
.fld:focus {
  outline: none;
  border-color: var(--color-primary-500);
  background: var(--control-focus-color);
}
.fld:disabled { opacity: 0.55; cursor: not-allowed; }

.dual-input {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.custom-period { display: flex; flex-direction: column; gap: 6px; margin-top: 4px; }

.hint {
  font-size: 11px;
  color: var(--muted-text);
  margin: 0;
  font-variant-numeric: tabular-nums;
}

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
@media (max-width: 600px) {
  .grid-2, .grid-3 { grid-template-columns: 1fr; }
}

.advanced-toggle {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-primary-600);
  cursor: pointer;
  padding: 4px 0;
  font-family: inherit;
}
.advanced-toggle svg { transition: transform 0.2s ease; }
.advanced-toggle.is-open svg { transform: rotate(180deg); }
.advanced-toggle:hover { color: var(--color-primary-700); }

.advanced {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: var(--panel-muted);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.esg-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  cursor: pointer;
}
.visible-toggle {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  background: var(--color-primary-50);
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 18%, transparent);
  border-radius: 10px;
}
.parent-map {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: var(--color-primary-50);
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 22%, transparent);
  border-radius: 12px;
}
.esg-toggle input { width: 14px; height: 14px; cursor: pointer; }

.status-toggle {
  display: inline-flex;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 3px;
  gap: 2px;
  width: fit-content;
}
.status-toggle__btn {
  padding: 6px 14px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);
  font-family: inherit;
}
.status-toggle__btn.is-active {
  background: var(--color-primary-500);
  color: #fff;
}

.err {
  font-size: 11px;
  color: var(--danger-text-strong, #DC2626);
  margin: 0;
  font-weight: 600;
}
.err--global {
  background: var(--danger-surface);
  border-radius: 8px;
  padding: 8px 12px;
}

.kpi-modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 28px 20px;
  border-top: 1px solid var(--border-color);
}
.btn {
  height: 38px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn--primary {
  background: var(--color-primary-500);
  color: #fff;
  border: 1px solid var(--color-primary-500);
}
.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-600);
  border-color: var(--color-primary-600);
}
.btn--ghost {
  background: var(--panel-color);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}
.btn--ghost:hover { background: var(--panel-muted); color: var(--text-primary); }
</style>
