<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { campaignLabels, campaignModalText } from '@/constants/campaignText'
import { fetchColleaguesRequest } from '@/authApi'
import KpiContributionPicker from '@/components/campaign/KpiContributionPicker.vue'

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

const TOTAL_STEPS = 4
const currentStep = ref(1)
const contributions = ref([])

function createEmptyForm() {
  return {
    name: '',
    purpose: '',
    tagInput: '',
    assetName: '',
    assetDescription: '',
    primaryGoal: '신규 고객 유입',
    customPrimaryGoal: '',
    campaignMethods: [],
    startDate: '',
    endDate: '',
    maxCost: '',
    minRevenue: '',
    goals: '',
    color: '',
    icon: '🎯',
  }
}

const ROLE_LABELS = {
  ROLE_ADMIN: '관리자',
  ROLE_GENERAL_MANAGER: '총괄 매니저',
  ROLE_MANAGER: '매니저',
  ROLE_USER: '담당자',
}

function formatRole(role) {
  if (!role) return '담당자'
  return ROLE_LABELS[role] ?? role.replace(/^ROLE_/, '')
}

// 백엔드 CampaignService.CAMPAIGN_PALETTE와 동일한 20색 (2025–2026 트렌드 큐레이션)
// WGSN/Coloro 2026 + Pantone COY + 2025 디지털 디자인 시스템에서 추출
const CAMPAIGN_PALETTE = [
  '#7C3AED', '#B68BD9', '#4F4878', '#4A4E97', '#2740B2', // 01–05 violet · lavender · dusk · indigo · cobalt
  '#0EA5E9', '#14B8A6', '#BEF264', '#F59E0B', '#FFBE98', // 06–10 sky · mint · lime · amber · peach
  '#FF6B45', '#FB7185', '#C5174D', '#EC4899', '#A47864', // 11–15 lava · coral · cherry · petal · mocha
  '#2F5D3D', '#FCD34D', '#67E8F9', '#475569', '#D946EF', // 16–20 sage · buttercream · glacier · slate · magenta
]

const goalOptions = [
  '신규 고객 유입',
  '재방문 유도',
  '회원 가입',
  '구매/예약',
  '브랜드 인지도',
  '매출 증대',
  '객단가 향상',
  '직접예약 확대',
  '리뷰/평판 개선',
  '기타',
]

const campaignMethodOptions = [
  {
    label: '쿠폰/할인',
    desc: '할인권, 즉시 할인, 포인트 적립으로 구매 전환을 만듭니다.',
  },
  {
    label: '체험권/사은품',
    desc: '샘플, 이용권, 굿즈를 제공해 신규 체험을 유도합니다.',
  },
  {
    label: '멤버십 혜택',
    desc: 'VIP 전용 혜택, 등급별 리워드로 재방문을 높입니다.',
  },
  {
    label: '공동 프로모션',
    desc: '양사 고객을 함께 타깃팅해 공동 이벤트를 운영합니다.',
  },
  {
    label: '콘텐츠 협업',
    desc: '브랜드 콘텐츠, 리뷰, 영상, SNS 캠페인을 함께 제작합니다.',
  },
  {
    label: '채널/앱 노출',
    desc: '앱 배너, 푸시, 알림톡, 웹 영역에 파트너 혜택을 노출합니다.',
  },
]

const campaignIconOptions = [
  { icon: '🎯', label: '목표' },
  { icon: '📣', label: '프로모션' },
  { icon: '👑', label: 'VIP' },
  { icon: '🏨', label: '호텔' },
  { icon: '🍽️', label: 'F&B' },
  { icon: '✈️', label: '여행' },
  { icon: '🛍️', label: '쇼핑' },
  { icon: '▶️', label: '콘텐츠' },
  { icon: '🎁', label: '혜택' },
  { icon: '💳', label: '금융' },
  { icon: '📱', label: '앱/채널' },
  { icon: '📈', label: '매출' },
]

const form = reactive(createEmptyForm())
const selectedMembers = ref([])
const memberCandidates = ref([])
const membersLoaded = ref(false)
const membersLoading = ref(false)
const membersError = ref('')
const memberSearch = ref('')
const expandedPickers = reactive({
  goal: false,
  method: false,
})

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
const resolvedPrimaryGoal = computed(() =>
  form.primaryGoal === '기타' ? form.customPrimaryGoal.trim() : form.primaryGoal,
)

// 각 단계별 진행 가능 조건
const isStep1Valid = computed(
  () => form.name.trim().length > 0 && form.assetName.trim().length > 0 && resolvedPrimaryGoal.value.length > 0,
)

const campaignMethodSummary = computed(() =>
  form.campaignMethods.length ? form.campaignMethods.join(', ') : '선택 안 함',
)
const isStep2Valid = computed(() => Boolean(form.startDate) && Boolean(form.endDate))
const isStep3Valid = computed(() => true)

const filteredMembers = computed(() => {
  const q = memberSearch.value.trim().toLowerCase()
  if (!q) return memberCandidates.value
  return memberCandidates.value.filter((m) =>
    (m.name || '').toLowerCase().includes(q)
    || (m.email || '').toLowerCase().includes(q)
    || (m.department || '').toLowerCase().includes(q),
  )
})
const isStep4Valid = computed(() =>
  // 기여 KPI는 선택 사항이지만, 매핑된 KPI는 모두 committedValue > 0 이어야 함
  contributions.value.every((c) => Number(c.committedValue) > 0),
) 

const canSubmit = computed(() => isStep1Valid.value && isStep2Valid.value && isStep3Valid.value)




const stepValidity = computed(() => [isStep1Valid.value, isStep2Valid.value, isStep3Valid.value, isStep4Valid.value])

function isStepDone(step) {
  return step < currentStep.value && stepValidity.value[step - 1]
}

const stepDescriptors = [
  { num: 1, label: '자산 & 목표' },
  { num: 2, label: '방식 & 일정' },
  { num: 3, label: '재무 & 팀원' },
  { num: 4, label: '기여 KPI' }
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
  nextForm.assetName = source.assetName ?? ''
  nextForm.assetDescription = source.assetDescription ?? ''
  nextForm.primaryGoal = source.primaryGoal ?? nextForm.primaryGoal
  nextForm.campaignMethods = Array.isArray(source.campaignMethods) ? [...source.campaignMethods] : []
  nextForm.maxCost = source.maxCost ?? ''
  nextForm.minRevenue = source.minRevenue ?? ''
  nextForm.color = source.color ?? ''
  nextForm.icon = source.icon ?? '🎯'
  Object.assign(form, nextForm)
  selectedMembers.value = Array.isArray(source.members) ? [...source.members] : []
  contributions.value = Array.isArray(source.contributions) ? [...source.contributions] : []
  expandedPickers.goal = false
  expandedPickers.method = false
  memberSearch.value = ''
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

async function loadTeamCandidates() {
  membersLoading.value = true
  membersError.value = ''
  try {
    const users = await fetchColleaguesRequest()
    memberCandidates.value = Array.isArray(users) ? users : []
    membersLoaded.value = true
  } catch (err) {
    membersError.value = err?.message ?? '팀원 목록을 불러오지 못했습니다.'
    memberCandidates.value = []
  } finally {
    membersLoading.value = false
  }
}

function isMemberSelected(user) {
  return selectedMembers.value.some((m) => m.idx === user.idx)
}

function toggleMember(user) {
  if (isMemberSelected(user)) {
    selectedMembers.value = selectedMembers.value.filter((m) => m.idx !== user.idx)
  } else {
    selectedMembers.value.push({
      idx: user.idx,
      name: user.name,
      email: user.email,
      role: user.role,
      profileImageUrl: user.profileImageUrl ?? null,
    })
  }
}

watch(currentStep, (step) => {
  if (step === 3 && !membersLoaded.value && !membersLoading.value) {
    loadTeamCandidates()
  }
})

function toggleListValue(key, value) {
  const list = form[key]
  const index = list.indexOf(value)
  if (index > -1) list.splice(index, 1)
  else list.push(value)
}

function isListValueSelected(key, value) {
  return form[key].includes(value)
}

function goNext() {
  if (currentStep.value === 1 && !isStep1Valid.value) return
  if (currentStep.value === 2 && !isStep2Valid.value) return
  if (currentStep.value === 3 && !isStep3Valid.value) return
  if (currentStep.value < TOTAL_STEPS) currentStep.value++
}
function goPrev() {
  if (currentStep.value > 1) currentStep.value--
}

function submitForm() {
  if (!canSubmit.value) return

  const ownerUserIdxs = selectedMembers.value.map((m) => m.idx).filter((v) => v != null)

  emit('submit', {
    name: form.name,
    purpose: form.purpose,
    tags: tagList.value,
    startDate: form.startDate,
    endDate: form.endDate,
    goals: form.goals,
    assetName: form.assetName,
    assetDescription: form.assetDescription,
    primaryGoal: resolvedPrimaryGoal.value,
    campaignMethods: [...form.campaignMethods],
    maxCost: form.maxCost,
    minRevenue: form.minRevenue,
    ownerUserIdxs,
    color: form.color,
    contributions: contributions.value.map((c) => ({
      targetOrgKpiId: c.targetOrgKpiId,
      committedValue: Number(c.committedValue) || 0,
    })),
    icon: form.icon,
  })
}

function avatarInitial(value) {
  const trimmed = (value ?? '').trim()
  return trimmed ? trimmed.charAt(0).toUpperCase() : '?'
}
</script>

<template>
  <Teleport to="body">
    <!-- 바깥(overlay) 클릭으로는 닫지 않음 — 입력 보존. 닫기는 X 버튼으로만. -->
    <div class="cm-overlay" role="presentation">
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

          <!-- Step 1: 자산 & 목표 -->
          <div v-if="currentStep === 1">
            <div class="step-section-title">이번 매칭으로 뭘 할 건가요?</div>
            <div class="step-section-desc">활용할 자산과 달성할 목표를 한 번에 정리합니다.</div>

            <div class="step1-grid">
              <div class="step1-grid__main">
                <div class="field-row">
                  <label class="lbl"><span>캠페인 이름 <em class="required-star">*</em></span></label>
                  <input
                    v-model="form.name"
                    type="text"
                    class="fld"
                    placeholder="예: 2026 Q3 객실 예약 증대"
                  />
                </div>

                <div class="field-row">
                  <label class="lbl"><span>자산명 <em class="required-star">*</em></span></label>
                  <input
                    v-model="form.assetName"
                    type="text"
                    class="fld"
                    placeholder="예: 갤러리아 VIP 고객층"
                  />
                </div>

                <div class="field-row">
                  <label class="lbl"><span>자산 설명</span></label>
                  <textarea
                    v-model="form.assetDescription"
                    rows="3"
                    class="fld fld--text"
                    placeholder="예: VIP App 활성 고객 5만 명, 앱 배너"
                  />
                </div>

                <div class="field-row">
                  <label class="lbl"><span>주 목표 <em class="required-star">*</em></span></label>
                  <button
                    type="button"
                    class="picker-toggle"
                    :aria-expanded="expandedPickers.goal"
                    @click="expandedPickers.goal = !expandedPickers.goal"
                  >
                    <span>{{ resolvedPrimaryGoal || form.primaryGoal }}</span>
                  </button>
                  <div v-if="expandedPickers.goal" class="picker-menu">
                    <button
                      v-for="goal in goalOptions"
                      :key="goal"
                      type="button"
                      class="picker-menu__item"
                      :class="{ selected: form.primaryGoal === goal }"
                      @click="form.primaryGoal = goal; expandedPickers.goal = false"
                    >
                      {{ goal }}
                    </button>
                  </div>
                  <input
                    v-if="form.primaryGoal === '기타'"
                    v-model="form.customPrimaryGoal"
                    type="text"
                    class="fld custom-goal-input"
                    placeholder="목표를 직접 입력하세요"
                  />
                </div>

                <div class="field-row">
                  <label class="lbl"><span>태그 <em>(쉼표로 구분)</em></span></label>
                  <input
                    v-model="form.tagInput"
                    type="text"
                    class="fld"
                    placeholder="예: VIP, 호텔, 직접예약"
                  />
                  <div v-if="tagList.length" class="pill-row" style="margin-top: 10px;">
                    <span v-for="tag in tagList" :key="tag" class="chip chip--soft">#{{ tag }}</span>
                  </div>
                </div>
              </div>

              <aside class="step1-grid__aside">
                <div class="lbl"><span>캠페인 아이콘</span></div>
                <p class="aside-hint">캠페인 성격에 가까운 아이콘을 선택하세요.</p>
                <div class="icon-picker" role="radiogroup" aria-label="캠페인 아이콘 선택">
                  <button
                    v-for="item in campaignIconOptions"
                    :key="item.icon"
                    type="button"
                    class="icon-choice"
                    :class="{ active: form.icon === item.icon }"
                    :aria-label="item.label"
                    :aria-checked="form.icon === item.icon"
                    role="radio"
                    @click="form.icon = item.icon"
                  >
                    <span>{{ item.icon }}</span>
                    <small>{{ item.label }}</small>
                  </button>
                </div>

                <div class="lbl"><span>캠페인 색상</span></div>
                <p class="aside-hint">선택하지 않으면 자동으로 부여됩니다.</p>
                <div class="color-swatches" role="radiogroup" aria-label="캠페인 색상 선택">
                  <button
                    v-for="color in CAMPAIGN_PALETTE"
                    :key="color"
                    type="button"
                    class="color-swatch"
                    :class="{ 'color-swatch--active': form.color === color }"
                    :style="{ background: color }"
                    :aria-label="`색상 ${color}`"
                    :aria-checked="form.color === color"
                    role="radio"
                    @click="form.color = form.color === color ? '' : color"
                  />
                </div>
              </aside>
            </div>
          </div>

          <!-- Step 2: 방식 & 일정 -->
          <div v-if="currentStep === 2">
            <div class="step-section-title">어떻게 진행하고 언제 운영할까요?</div>
            <div class="step-section-desc">캠페인 방식과 운영 기간을 선택합니다.</div>

            <div class="field-row">
              <label class="lbl"><span>캠페인 방식 <em>다중 선택</em></span></label>
              <button
                type="button"
                class="picker-toggle"
                :aria-expanded="expandedPickers.method"
                @click="expandedPickers.method = !expandedPickers.method"
              >
                <span>{{ campaignMethodSummary }}</span>
              </button>
              <div v-if="expandedPickers.method" class="picker-menu">
                <button
                  v-for="method in campaignMethodOptions"
                  :key="method.label"
                  type="button"
                  class="picker-menu__item picker-menu__item--rich"
                  :class="{ selected: isListValueSelected('campaignMethods', method.label) }"
                  @click="toggleListValue('campaignMethods', method.label)"
                >
                  <span>
                    <strong>{{ method.label }}</strong>
                    <small>{{ method.desc }}</small>
                  </span>
                  <b v-if="isListValueSelected('campaignMethods', method.label)">선택됨</b>
                </button>
              </div>
            </div>

            <div class="field-row field-grid-2">
              <div>
                <label class="lbl"><span>시작일 <em class="required-star">*</em></span></label>
                <input v-model="form.startDate" type="date" class="fld" />
              </div>
              <div>
                <label class="lbl"><span>종료일 <em class="required-star">*</em></span></label>
                <input v-model="form.endDate" type="date" class="fld" />
              </div>
            </div>

          </div>

          <!-- Step 3: 재무 & 팀원 -->
          <div v-if="currentStep === 3">
            <div class="step-section-title">재무 기준과 함께할 팀원을 정해주세요</div>
            <div class="step-section-desc">선택 기준을 기록하고 같은 회사 팀원을 캠페인 멤버로 자동 등록합니다.</div>

            <div class="field-row field-grid-2">
              <div>
                <label class="lbl"><span>최대 부담 비용</span></label>
                <div class="money-field">
                  <input v-model="form.maxCost" inputmode="numeric" class="fld" placeholder="예: 50000000" />
                  <b>원</b>
                </div>
              </div>
              <div>
                <label class="lbl"><span>최소 기대 매출</span></label>
                <div class="money-field">
                  <input v-model="form.minRevenue" inputmode="numeric" class="fld" placeholder="예: 120000000" />
                  <b>원</b>
                </div>
              </div>
            </div>

            <div class="field-row">
              <label class="lbl"><span>캠페인 목표명/상세 목표</span></label>
              <textarea
                v-model="form.goals"
                rows="3"
                class="fld fld--text"
                placeholder="예: 2026 Q3 객실 예약 증대, 직접예약 비중 15% 확대"
              />
            </div>

            <div class="field-row">
              <label class="lbl">
                <span>팀원 추가 <em>다중 선택 · 선택 안 해도 진행 가능</em></span>
              </label>

              <input
                v-model="memberSearch"
                type="text"
                class="fld member-search"
                placeholder="이름·이메일·부서로 검색"
              />

              <div class="member-list" role="listbox" aria-label="팀원 후보 목록">
                <div v-if="membersLoading" class="member-list__loading">
                  <span class="member-spinner" aria-hidden="true"></span>
                  <span>팀원 목록을 불러오는 중...</span>
                </div>
                <div v-else-if="membersError" class="member-list__empty">
                  {{ membersError }}
                  <button type="button" class="member-list__retry" @click="loadTeamCandidates">다시 시도</button>
                </div>
                <div v-else-if="!memberCandidates.length" class="member-list__empty">
                  같은 회사 소속 팀원이 없습니다.
                </div>
                <div v-else-if="!filteredMembers.length" class="member-list__empty">
                  검색 결과가 없습니다.
                </div>
                <button
                  v-for="user in filteredMembers"
                  v-else
                  :key="user.idx"
                  type="button"
                  class="member-item"
                  :class="{ selected: isMemberSelected(user) }"
                  :aria-pressed="isMemberSelected(user)"
                  @click="toggleMember(user)"
                >
                  <span class="member-item__left">
                    <span class="member-item__avatar" aria-hidden="true">
                      <img v-if="user.profileImageUrl" :src="user.profileImageUrl" alt="" />
                      <span v-else>{{ avatarInitial(user.name) }}</span>
                    </span>
                    <span class="member-item__name">{{ user.name }}</span>
                  </span>
                  <span class="member-item__right">
                    <span class="member-item__role">{{ formatRole(user.role) }}</span>
                    <span class="member-item__email">{{ user.email }}</span>
                  </span>
                  <span class="member-item__check" v-if="isMemberSelected(user)" aria-hidden="true">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M20 6 9 17l-5-5" />
                    </svg>
                  </span>
                </button>
              </div>

              <div class="member-summary">
                <span>{{ selectedMembers.length }}명 선택됨</span>
              </div>
            </div>
          </div>

          <!-- Step 4: 기여 KPI (cascade) -->
          <div v-if="currentStep === 4">
            <div class="step-section-title">기여하는 상위 KPI</div>
            <div class="step-section-desc">
              이 캠페인이 어떤 본사·계열사 KPI에 cascade 되는지 선택하세요. 선택한 KPI마다 약속한 기여값을 입력합니다.
              (선택 사항)
            </div>
            <KpiContributionPicker v-model="contributions" />
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
              :disabled="(currentStep === 1 && !isStep1Valid) || (currentStep === 2 && !isStep2Valid) || (currentStep === 3 && !isStep3Valid)"
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
  font-size: 22px;
  font-weight: 800;
  color: #7C3AED;
  margin-bottom: 6px;
  letter-spacing: -0.01em;
}
/* ── step1 좌/우 레이아웃 + 색상 picker ───── */
.step1-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 24px;
  align-items: start;
}

@media (max-width: 720px) {
  .step1-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .field-grid-2,
  .choice-grid,
  .choice-grid--three,
  .choice-grid--four {
    grid-template-columns: minmax(0, 1fr);
  }
}

.step1-grid__main {
  display: grid;
  gap: 14px;
}

.step1-grid__aside {
  display: grid;
  gap: 10px;
  padding: 14px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.aside-hint {
  margin: 6px 0 12px;
  font-size: 12px;
  color: var(--muted-text);
}

.color-swatches {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.icon-picker {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.icon-choice {
  display: grid;
  min-height: 56px;
  place-items: center;
  gap: 2px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  color: var(--text-primary);
  cursor: pointer;
  padding: 6px 4px;
  transition: border-color var(--transition-fast), background var(--transition-fast), box-shadow var(--transition-fast);
}

.icon-choice span {
  font-size: 20px;
  line-height: 1;
}

.icon-choice small {
  color: var(--muted-text);
  font-size: 10px;
  font-weight: 800;
}

.icon-choice:hover,
.icon-choice.active {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 10%, var(--panel-color));
}

.icon-choice.active {
  box-shadow: inset 0 0 0 1px var(--color-primary-500);
}

.color-swatch {
  position: relative;
  aspect-ratio: 1 / 1;
  width: 100%;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0;
  cursor: pointer;
  transition: transform 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease;
}

.color-swatch:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.18);
}

.color-swatch--active {
  border-color: var(--text-primary, #111);
  box-shadow: 0 0 0 2px var(--panel-color, #fff), 0 0 0 4px var(--text-primary, #111);
}

.color-swatch--active::after {
  content: '✓';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
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
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.lbl em {
  font-style: normal;
  font-weight: 500;
  font-size: 13px;
  color: var(--muted-text);
  margin-left: 4px;
}

.lbl .required-star {
  color: var(--color-primary-600, #8b5cf6);
  font-size: 15px;
  font-weight: 900;
  margin-left: 3px;
}

.fld {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  font-size: 14px;
  color: var(--text-primary);
  background: var(--control-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: border-color var(--transition-fast), background var(--transition-fast);
  font-family: inherit;
}
.fld--text {
  height: auto;
  padding: 12px 14px;
  resize: vertical;
  line-height: 1.55;
}
.fld:focus {
  outline: none;
  border-color: var(--color-primary-500);
  background: var(--control-focus-color, var(--panel-color));
}
.fld::placeholder { color: var(--subtle-text); }

.picker-toggle {
  display: flex;
  width: 100%;
  min-height: 46px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  color: var(--text-primary);
  padding: 0 14px;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.picker-toggle::after {
  content: '⌄';
  flex: 0 0 auto;
  color: var(--text-secondary);
  font-size: 16px;
  font-weight: 900;
  line-height: 1;
}

.picker-toggle span {
  overflow: hidden;
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picker-toggle:hover {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 5%, var(--panel-color));
}

.picker-panel {
  margin-top: 10px;
}

.custom-goal-input {
  margin-top: 10px;
}

.picker-menu {
  overflow: hidden;
  margin-top: -1px;
  border: 1px solid var(--border-color);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  background: var(--panel-color);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.picker-toggle[aria-expanded='true'] {
  border-color: var(--color-primary-500);
  border-radius: var(--radius-md) var(--radius-md) 0 0;
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary-500) 12%, transparent);
}

.picker-toggle[aria-expanded='true']::after {
  transform: rotate(180deg);
}

.picker-menu__item {
  display: flex;
  width: 100%;
  min-height: 34px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 0;
  border-bottom: 1px solid color-mix(in srgb, var(--border-color) 55%, transparent);
  background: transparent;
  color: var(--text-primary);
  padding: 9px 12px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 750;
  text-align: left;
}

.picker-menu__item:last-child {
  border-bottom: 0;
}

.picker-menu__item:hover {
  background: var(--panel-muted);
}

.picker-menu__item.selected {
  background: var(--color-primary-600, #2563eb);
  color: #fff;
}

.picker-menu__item--rich {
  min-height: 62px;
  align-items: center;
}

.picker-menu__item--rich span {
  display: grid;
  gap: 3px;
}

.picker-menu__item--rich strong {
  color: inherit;
  font-size: 13px;
  font-weight: 900;
}

.picker-menu__item--rich small {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 650;
  line-height: 1.35;
}

.picker-menu__item--rich.selected small {
  color: rgb(255 255 255 / 78%);
}

.picker-menu__item--rich b {
  flex: 0 0 auto;
  color: inherit;
  font-size: 11px;
  font-weight: 900;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.choice-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.choice-grid--four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.choice-card {
  display: flex;
  min-height: 54px;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.25;
  text-align: center;
  transition: border-color var(--transition-fast), background var(--transition-fast), color var(--transition-fast);
}

.choice-card--rich {
  min-height: 88px;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 6px;
  text-align: left;
}

.choice-card--rich strong {
  color: inherit;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
}

.choice-card--rich small {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 650;
  line-height: 1.45;
  word-break: keep-all;
}

.choice-card--rich.selected small {
  color: color-mix(in srgb, var(--color-primary-700) 72%, var(--muted-text));
}

.choice-card:hover {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 5%, var(--panel-color));
}

.choice-card.selected {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 12%, var(--panel-color));
  color: var(--color-primary-700);
}

.money-field {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
}

.money-field:focus-within {
  border-color: var(--color-primary-500);
  background: var(--control-focus-color, var(--panel-color));
}

.money-field .fld {
  border: 0;
  background: transparent;
}

.money-field b {
  padding-right: 14px;
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 800;
}

.input-wrap-add {
  display: flex;
  gap: 8px;
}
.input-wrap-add .fld { flex: 1; }

/* 팀원 추가 inline 리스트 */
.member-search { margin-bottom: 10px; }

.member-list {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  max-height: 280px;
  overflow-y: auto;
}

.member-list__loading,
.member-list__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 16px;
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.member-list__retry {
  margin-left: 6px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 4px 10px;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.member-list__retry:hover {
  background: var(--panel-muted);
  border-color: #7C3AED;
}

.member-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid color-mix(in srgb, #7C3AED 25%, transparent);
  border-top-color: #7C3AED;
  border-radius: 999px;
  animation: memberSpin 0.7s linear infinite;
}
@keyframes memberSpin {
  to { transform: rotate(360deg); }
}

.member-item {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  border: 0;
  border-bottom: 1px solid color-mix(in srgb, var(--border-color) 55%, transparent);
  background: transparent;
  color: var(--text-primary);
  padding: 12px 14px;
  cursor: pointer;
  text-align: left;
  transition: background var(--transition-fast);
}
.member-item:last-child { border-bottom: 0; }
.member-item:hover { background: var(--panel-muted); }
.member-item.selected {
  background: color-mix(in srgb, #7C3AED 10%, var(--panel-color));
}

.member-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.member-item__avatar {
  flex: 0 0 auto;
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: color-mix(in srgb, #7C3AED 14%, var(--panel-color));
  color: #6d28d9;
  font-size: 13px;
  font-weight: 800;
  overflow: hidden;
}
.member-item__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.member-item__name {
  font-size: 13px;
  font-weight: 800;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-item__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  min-width: 0;
}
.member-item__role {
  font-size: 11px;
  font-weight: 800;
  color: #6d28d9;
  background: color-mix(in srgb, #7C3AED 12%, transparent);
  border-radius: 999px;
  padding: 2px 9px;
  white-space: nowrap;
}
.member-item__email {
  font-size: 11px;
  font-weight: 600;
  color: var(--muted-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
}

.member-item__check {
  flex: 0 0 auto;
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #7C3AED;
  color: #fff;
}

.member-summary {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--muted-text);
  text-align: right;
}

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
