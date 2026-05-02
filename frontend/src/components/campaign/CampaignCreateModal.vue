<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { campaignLabels, campaignModalText } from '@/constants/campaignText'

const props = defineProps({
  mode: {
    type: String,
    default: 'create',
  },
  initialValues: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['close', 'submit'])

const TOTAL_STEPS = 3
const currentStep = ref(1)

function createEmptyForm() {
  return {
    name: '',
    purpose: '',
    tagInput: '',
    startDate: '',
    endDate: '',
    partnerInput: '',
    goals: '',
    mainMessage: '',
  }
}

const form = reactive(createEmptyForm())
const partners = ref([])

const modalEyebrow = computed(() =>
  props.mode === 'edit' ? `CAMPAIGN · ${campaignModalText.eyebrowEdit}` : 'CAMPAIGN · 새 캠페인 만들기',
)
const modalTitle = computed(() =>
  props.mode === 'edit' ? campaignLabels.editCampaign : '캠페인 생성',
)
const submitLabel = computed(() =>
  props.mode === 'edit' ? campaignModalText.saveChanges : campaignLabels.createCampaign,
)

const tagList = computed(() =>
  form.tagInput
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean),
)

// 각 단계별 진행 가능 조건
const isStep1Valid = computed(() => form.name.trim().length > 0 && form.purpose.trim().length > 0)
const isStep2Valid = computed(() => Boolean(form.startDate) && Boolean(form.endDate))
const isStep3Valid = computed(() => true) // 목표·메시지는 선택

const canSubmit = computed(() => isStep1Valid.value && isStep2Valid.value)

const stepValidity = computed(() => [isStep1Valid.value, isStep2Valid.value, isStep3Valid.value])

function isStepDone(step) {
  return step < currentStep.value && stepValidity.value[step - 1]
}

const stepDescriptors = [
  { num: 1, label: '기본 정보' },
  { num: 2, label: '파트너 & 일정' },
  { num: 3, label: '목표 & 메시지' },
]

function hydrateForm(values) {
  const source = values ?? {}
  const nextForm = createEmptyForm()
  nextForm.name = source.name ?? ''
  nextForm.purpose = source.purpose ?? ''
  nextForm.tagInput = Array.isArray(source.tags) ? source.tags.join(', ') : ''
  nextForm.startDate = source.startDate ?? ''
  nextForm.endDate = source.endDate ?? ''
  nextForm.goals = source.goals ?? ''
  nextForm.mainMessage = source.mainMessage ?? ''
  Object.assign(form, nextForm)
  partners.value = Array.isArray(source.partners) ? [...source.partners] : []
}

watch(
  () => [props.mode, props.initialValues],
  () => {
    if (props.mode === 'edit' && props.initialValues) {
      hydrateForm(props.initialValues)
    } else {
      hydrateForm(null)
    }
    currentStep.value = 1
  },
  { immediate: true, deep: true },
)

function addPartner() {
  const next = form.partnerInput.trim()
  if (!next || partners.value.includes(next)) {
    form.partnerInput = ''
    return
  }
  partners.value.push(next)
  form.partnerInput = ''
}
function removePartner(partner) {
  partners.value = partners.value.filter((item) => item !== partner)
}
function handlePartnerKeydown(event) {
  if (event.key === 'Enter') {
    event.preventDefault()
    addPartner()
  }
}

function goNext() {
  if (currentStep.value === 1 && !isStep1Valid.value) return
  if (currentStep.value === 2 && !isStep2Valid.value) return
  if (currentStep.value < TOTAL_STEPS) currentStep.value++
}
function goPrev() {
  if (currentStep.value > 1) currentStep.value--
}

function submitForm() {
  if (!canSubmit.value) return
  emit('submit', {
    name: form.name,
    purpose: form.purpose,
    tags: tagList.value,
    startDate: form.startDate,
    endDate: form.endDate,
    partners: partners.value,
    goals: form.goals,
    mainMessage: form.mainMessage,
  })
}

function avatarInitial(value) {
  const trimmed = (value ?? '').trim()
  return trimmed ? trimmed.charAt(0).toUpperCase() : '?'
}
</script>

<template>
  <Teleport to="body">
    <div class="cm-overlay" role="presentation" @click.self="emit('close')">
      <section
        class="modal-shell"
        role="dialog"
        aria-modal="true"
        aria-labelledby="campaign-modal-title"
      >
        <!-- 헤더 -->
        <div class="modal-header">
          <div>
            <div class="modal-header__eyebrow">
              <span>{{ modalEyebrow }}</span>
            </div>
            <h2 id="campaign-modal-title">{{ modalTitle }}</h2>
          </div>
          <button class="iconbtn btn-close" aria-label="닫기" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- 스텝퍼 -->
        <div class="stepper">
          <template v-for="(s, i) in stepDescriptors" :key="s.num">
            <div
              class="step"
              :class="{
                'is-active': currentStep === s.num,
                'is-done': isStepDone(s.num),
              }"
            >
              <span class="step__num">
                <svg v-if="isStepDone(s.num)" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 6 9 17l-5-5" />
                </svg>
                <template v-else>{{ s.num }}</template>
              </span>
              <span class="step__label">{{ s.label }}</span>
            </div>
            <div
              v-if="i < stepDescriptors.length - 1"
              class="step__line"
              :class="{ 'is-done': isStepDone(s.num) || (currentStep > s.num) }"
            />
          </template>
        </div>

        <!-- 본문 -->
        <form class="modal-body" @submit.prevent="submitForm">

          <!-- Step 1: 기본 정보 -->
          <div v-if="currentStep === 1">
            <div class="step-section-title">캠페인 기본 정보</div>
            <div class="step-section-desc">캠페인의 이름과 목적을 정해 주세요. 태그를 활용하면 검색·필터에 도움이 됩니다.</div>

            <div class="field-row">
              <label class="lbl"><span>캠페인 이름</span></label>
              <input
                v-model="form.name"
                type="text"
                class="fld"
                :placeholder="campaignModalText.namePlaceholder"
              />
            </div>

            <div class="field-row">
              <label class="lbl"><span>{{ campaignLabels.purpose }}</span></label>
              <textarea
                v-model="form.purpose"
                rows="3"
                class="fld fld--text"
                :placeholder="campaignModalText.purposePlaceholder"
              />
            </div>

            <div class="field-row">
              <label class="lbl"><span>태그 <em>(쉼표로 구분)</em></span></label>
              <input
                v-model="form.tagInput"
                type="text"
                class="fld"
                :placeholder="campaignModalText.tagsPlaceholder"
              />
              <div v-if="tagList.length" class="pill-row" style="margin-top: 10px;">
                <span v-for="tag in tagList" :key="tag" class="chip chip--soft">#{{ tag }}</span>
              </div>
            </div>
          </div>

          <!-- Step 2: 파트너 & 일정 -->
          <div v-if="currentStep === 2">
            <div class="step-section-title">함께할 파트너와 운영 기간</div>
            <div class="step-section-desc">초대할 파트너와 캠페인 기간을 설정합니다. 추후 변경 가능합니다.</div>

            <div class="field-row field-grid-2">
              <div>
                <label class="lbl"><span>시작일</span></label>
                <input v-model="form.startDate" type="date" class="fld" />
              </div>
              <div>
                <label class="lbl"><span>종료일</span></label>
                <input v-model="form.endDate" type="date" class="fld" />
              </div>
            </div>

            <div class="field-row">
              <label class="lbl"><span>파트너 초대</span></label>
              <div class="input-wrap-add">
                <input
                  v-model="form.partnerInput"
                  class="fld"
                  placeholder="파트너명 또는 담당자를 추가하세요"
                  @keydown="handlePartnerKeydown"
                />
                <button type="button" class="btn btn--primary" @click="addPartner">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 5v14M5 12h14" />
                  </svg>
                  <span> 추가</span>
                </button>
              </div>
              <div v-if="partners.length" class="pill-row" style="margin-top: 10px;">
                <span v-for="partner in partners" :key="partner" class="chip">
                  <span class="avatar" style="width: 18px; height: 18px; font-size: 10px; margin-right: 4px;">
                    <span>{{ avatarInitial(partner) }}</span>
                  </span>
                  <span>{{ partner }}</span>
                  <button type="button" class="chip__x" :aria-label="`${partner} 제거`" @click="removePartner(partner)">
                    <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M18 6 6 18M6 6l12 12" />
                    </svg>
                  </button>
                </span>
              </div>
            </div>
          </div>

          <!-- Step 3: 목표 & 메시지 -->
          <div v-if="currentStep === 3">
            <div class="step-section-title">성과 목표와 핵심 메시지</div>
            <div class="step-section-desc">캠페인이 달성하려는 결과와 협력사·고객에게 전달할 메시지를 정리합니다.</div>

            <div class="field-row">
              <label class="lbl"><span>{{ campaignLabels.goals }}</span></label>
              <textarea
                v-model="form.goals"
                rows="3"
                class="fld fld--text"
                :placeholder="campaignModalText.goalsPlaceholder"
              />
            </div>

            <div class="field-row">
              <label class="lbl"><span>{{ campaignLabels.mainMessage }}</span></label>
              <textarea
                v-model="form.mainMessage"
                rows="4"
                class="fld fld--text"
                :placeholder="campaignModalText.mainMessagePlaceholder"
              />
            </div>
          </div>
        </form>

        <!-- 푸터 -->
        <div class="modal-footer">
          <div class="modal-footer__hint">
            {{ currentStep }}<span> / {{ TOTAL_STEPS }} 단계</span>
          </div>
          <div class="modal-footer__actions">
            <button
              type="button"
              class="btn btn--secondary"
              :disabled="currentStep === 1"
              @click="goPrev"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m15 18-6-6 6-6" />
              </svg>
              <span> 이전</span>
            </button>
            <button
              v-if="currentStep < TOTAL_STEPS"
              type="button"
              class="btn btn--primary"
              :disabled="(currentStep === 1 && !isStep1Valid) || (currentStep === 2 && !isStep2Valid)"
              @click="goNext"
            >
              <span>다음 </span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="m9 18 6-6-6-6" />
              </svg>
            </button>
            <button
              v-else
              type="button"
              class="btn btn--primary"
              :disabled="!canSubmit"
              @click="submitForm"
            >
              {{ submitLabel }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
/* ── overlay ──────────────────────────── */
.cm-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
  background: rgba(15, 23, 42, 0.46);
}

/* ── shell ────────────────────────────── */
.modal-shell {
  width: min(720px, 100%);
  max-height: min(860px, calc(100vh - 56px));
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.26);
  overflow: hidden;
  color: var(--text-primary);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 12px;
}
.modal-header__eyebrow span {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--muted-text);
  text-transform: uppercase;
}
.modal-header h2 {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-primary);
}

.iconbtn.btn-close {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--transition-fast);
}
.iconbtn.btn-close:hover { background: var(--panel-color); color: var(--text-primary); }

/* ── stepper ──────────────────────────── */
.stepper {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 24px 16px;
}
.step {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.step__num {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}
.step.is-active .step__num {
  background: var(--color-primary-500);
  color: #fff;
  border-color: var(--color-primary-500);
}
.step.is-done .step__num {
  background: var(--color-primary-500);
  color: #fff;
  border-color: var(--color-primary-500);
}
.step__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--muted-text);
  white-space: nowrap;
}
.step.is-active .step__label { color: var(--text-primary); font-weight: 700; }
.step.is-done .step__label { color: var(--text-secondary, var(--text-primary)); }

.step__line {
  flex: 1;
  height: 2px;
  background: var(--border-color);
  border-radius: 2px;
  min-width: 12px;
}
.step__line.is-done { background: var(--color-primary-500); }

/* ── body ─────────────────────────────── */
.modal-body {
  padding: 12px 24px 8px;
  overflow-y: auto;
  flex: 1;
}

.step-section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.step-section-desc {
  font-size: 13px;
  color: var(--muted-text);
  margin-bottom: 18px;
}

.field-row { margin-bottom: 16px; }
.field-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.lbl {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.lbl em {
  font-style: normal;
  font-weight: 500;
  color: var(--muted-text);
  margin-left: 4px;
}

.fld {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  font-size: 13px;
  color: var(--text-primary);
  background: var(--control-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: border-color var(--transition-fast), background var(--transition-fast);
  font-family: inherit;
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
  background: var(--control-focus-color, var(--panel-color));
}
.fld::placeholder { color: var(--subtle-text); }

.input-wrap-add {
  display: flex;
  gap: 8px;
}
.input-wrap-add .fld { flex: 1; }

/* chips / pills */
.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}
.chip--soft {
  background: color-mix(in srgb, var(--color-primary-500) 8%, transparent);
  color: var(--color-primary-700, var(--color-primary-600));
  border-color: color-mix(in srgb, var(--color-primary-500) 18%, transparent);
}
.chip__x {
  display: inline-grid;
  place-items: center;
  width: 16px;
  height: 16px;
  border: none;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  border-radius: 999px;
}
.chip__x:hover { background: var(--border-color); color: var(--text-primary); }
.avatar {
  display: inline-grid;
  place-items: center;
  border-radius: 999px;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  font-weight: 800;
}

/* ── footer ───────────────────────────── */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 24px 18px;
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}
.modal-footer__hint {
  font-size: 12px;
  font-weight: 700;
  color: var(--muted-text);
}
.modal-footer__hint span { font-weight: 500; }

.modal-footer__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
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
.btn--secondary {
  background: var(--panel-color);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}
.btn--secondary:hover:not(:disabled) {
  background: var(--panel-muted);
  border-color: var(--border-strong, var(--color-primary-500));
}
</style>
