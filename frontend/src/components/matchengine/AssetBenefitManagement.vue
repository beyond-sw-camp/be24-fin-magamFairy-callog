<script setup>
import { computed, onMounted, ref } from 'vue'
import { CreateAsset, DeleteAsset, ListAssets, UpdateAsset } from '@/api/matchingAssets'

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
const registrationBody = ref(null)
const activeRegistrationSection = ref('basic')
const editingAssetId = ref(null)
const deletingAssetId = ref(null)

function getTodayString() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

function createAssetForm() {
  return {
    type: '',
    affiliate: '',
    registeredAt: getTodayString(),
    category: '',
    target: '',
    scale: '',
    exposureValue: '',
    performance: '',
    conditions: '',
    partnerFit: [],
    blockedPartners: [],
    supplyLimit: '',
    publicStatus: 'PUBLIC',
    matchingStatus: 'ACTIVE',
  }
}

const assetForm = ref(createAssetForm())

const assetTypes = [
  { value: 'customer', label: '고객 자산', desc: 'VIP, 멤버십, 고객DB' },
  { value: 'channel', label: '채널 자산', desc: '앱, 알림톡, 푸시' },
  { value: 'space', label: '공간 자산', desc: '호텔, 리조트, 매장' },
  { value: 'content', label: '콘텐츠 자산', desc: '이벤트, IP, 영상' },
]

const affiliateOptions = [
  '한화갤러리아',
  '한화호텔앤드리조트',
  '한화이글스',
  '한화생명',
  '한화손해보험',
  '한화시스템',
  '직접 입력',
]

const partnerCategoryOptions = [
  '럭셔리 뷰티',
  '프리미엄 F&B',
  '호텔/리조트',
  '카드/금융',
  '패션/리테일',
  '여행/항공',
  '엔터테인먼트',
  '리빙/홈',
]

const blockedCategoryOptions = ['저가 브랜드', '성인/도박성', '대부업/대출', '담배/주류']

const customPartnerInput = ref('')

const formSections = [
  {
    id: 'basic',
    title: '기본 정보',
    description: '자산을 식별할 핵심 정보',
    required: ['type', 'affiliate', 'category'],
  },
  {
    id: 'attractiveness',
    title: '매칭 매력도',
    description: '파트너에게 보여줄 자산 가치',
    required: ['target', 'scale'],
  },
  {
    id: 'conditions',
    title: '매칭 조건',
    description: '어떤 파트너와 어떻게 매칭할지',
    required: ['supplyLimit'],
  },
  {
    id: 'visibility',
    title: '공개 설정',
    description: '파트너 페이지 노출 방식',
    required: ['publicStatus', 'matchingStatus'],
  },
]

const partnerProposals = [
  {
    id: 1,
    partner: '럭시드',
    name: '핸드크림 10ml 샘플',
    type: '샘플',
    target: '2040 뷰티 고객',
    scale: '10,000개',
    value: '5,000만 원',
    unitValue: '단가 5,000원',
    cost: '파트너 전액 부담',
    costDetail: '혜택 원가+배송비 파트너 부담',
    period: '2026.05.01 - 2026.06.30',
    matchAsset: '갤러리아 VIP 고객층',
    matchScore: 87,
    category: '뷰티',
    missing: [],
    status: '접수 완료',
  },
  {
    id: 2,
    partner: '멜로우',
    name: '전시 시설 30% 할인권',
    type: '할인권',
    target: '휴가철 여행 계획 고객',
    scale: '제한 없음',
    value: '협의 필요',
    unitValue: '할인율 기반 정산',
    cost: '파트너 100% 부담',
    costDetail: '혜택 원가 부담, 운영비 별도 협의',
    period: '상시 협의',
    matchAsset: '호텔 객실 패키지',
    matchScore: 82,
    category: '여행',
    missing: [],
    status: '평가 반영',
  },
  {
    id: 3,
    partner: '어반터치',
    name: '오리지널 콘텐츠 공동 프로모션',
    type: '공동 콘텐츠',
    target: '미입력',
    scale: '1,000만',
    value: '미입력',
    unitValue: '필수',
    cost: '미입력',
    costDetail: '비용 부담 구조 미입력',
    period: '미입력',
    matchAsset: '매칭 불가',
    matchScore: null,
    category: '콘텐츠',
    missing: ['대상 고객', '비용 부담', '유효 기간'],
    status: '임시 저장',
  },
]

function formatDateValue(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function mapAsset(asset) {
  return {
    id: asset.id ?? asset.assetId ?? asset.idx,
    type: asset.type ?? asset.assetName ?? asset.name ?? '-',
    affiliate: asset.affiliate ?? asset.affiliateName ?? '-',
    registeredAt: formatDateValue(asset.registeredAt ?? asset.createdAt ?? asset.createdDate),
    category: asset.category ?? asset.assetCategory ?? 'customer',
    target: asset.target ?? asset.targetCustomer ?? '-',
    scale: asset.scale ?? asset.assetScale ?? '-',
    exposureValue: asset.exposureValue ?? asset.mediaValue ?? asset.adValue ?? '미입력',
    performance: asset.performance ?? asset.pastPerformance ?? '미입력',
    conditions: asset.conditions ?? asset.condition ?? '-',
    partnerFit: asset.partnerFit ?? asset.preferredPartnerCategories ?? '미입력',
    blockedPartners: asset.blockedPartners ?? asset.excludedPartnerCategories ?? '없음',
    supplyLimit: asset.supplyLimit ?? asset.availableCapacity ?? '미입력',
    publicStatus: asset.publicStatus ?? asset.partnerVisibleStatus ?? 'PRIVATE',
    matchingStatus: asset.matchingStatus ?? asset.status ?? (asset.active ?? asset.isActive ? 'ACTIVE' : 'PAUSED'),
    accessPolicy: asset.accessPolicy ?? '공유',
  }
}

function assetStatusLabel(asset) {
  const publicStatus = String(asset.publicStatus ?? '').toUpperCase()
  const matchingStatus = String(asset.matchingStatus ?? '').toUpperCase()

  const publicLabel = publicStatus === 'PUBLIC' || publicStatus === 'VISIBLE' ? '파트너 공개' : '비공개'
  const matchingLabel =
    matchingStatus === 'AVAILABLE' || matchingStatus === 'ACTIVE'
      ? '매칭 활성'
      : matchingStatus === 'IN_USE'
        ? '사용 중'
        : matchingStatus === 'EXCLUSIVE'
          ? '전속'
        : matchingStatus === 'EXPIRED'
          ? '기간 만료'
          : matchingStatus === 'NEEDS_REVIEW'
            ? '검토 필요'
            : '휴면'

  return `${publicLabel} · ${matchingLabel}`
}

function isAssetAvailable(asset) {
  const normalized = String(asset.matchingStatus ?? '').toUpperCase()
  return normalized === 'AVAILABLE' || normalized === 'ACTIVE'
}

function proposalStatusLabel(proposal) {
  if (proposal.missing?.length) return `매칭 불가 · ${proposal.missing.length}개 누락`
  if (proposal.matchScore) return `${proposal.status} · ${proposal.matchScore}점`
  return proposal.status
}

function isFilled(field) {
  const value = assetForm.value[field]
  if (Array.isArray(value)) return value.length > 0
  return value !== '' && value != null
}

function sectionFilled(section) {
  return section.required.filter(isFilled).length
}

const totalRequired = computed(() =>
  formSections.reduce((sum, section) => sum + section.required.length, 0),
)

const filledRequired = computed(() =>
  formSections.reduce((sum, section) => sum + section.required.filter(isFilled).length, 0),
)

const progressPercent = computed(() =>
  Math.round((filledRequired.value / totalRequired.value) * 100),
)

const canSubmitAsset = computed(() => filledRequired.value === totalRequired.value)

function toggleChip(list, value) {
  const items = assetForm.value[list]
  const index = items.indexOf(value)
  if (index > -1) items.splice(index, 1)
  else items.push(value)
}

function isChipActive(list, value) {
  return assetForm.value[list].includes(value)
}

function addCustomPartner() {
  const value = customPartnerInput.value.trim()
  if (value && !assetForm.value.partnerFit.includes(value)) {
    assetForm.value.partnerFit.push(value)
  }
  customPartnerInput.value = ''
}

function removeChip(list, value) {
  assetForm.value[list] = assetForm.value[list].filter((item) => item !== value)
}

function scrollToRegistrationSection(id) {
  activeRegistrationSection.value = id
  const element = document.getElementById(`asset-reg-${id}`)
  if (element) element.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function updateActiveRegistrationSection() {
  const container = registrationBody.value
  if (!container) return

  const isAtBottom =
    container.scrollTop + container.clientHeight >= container.scrollHeight - 8

  if (isAtBottom) {
    activeRegistrationSection.value = formSections[formSections.length - 1].id
    return
  }

  const containerRect = container.getBoundingClientRect()
  const anchorY = containerRect.top + containerRect.height * 0.48
  let currentSection = formSections[0].id

  for (const section of formSections) {
    const element = document.getElementById(`asset-reg-${section.id}`)
    if (!element) continue

    if (element.getBoundingClientRect().top <= anchorY) {
      currentSection = section.id
    }
  }

  activeRegistrationSection.value = currentSection
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
  assetForm.value = createAssetForm()
  editingAssetId.value = null
  isAssetFormOpen.value = true
  activeRegistrationSection.value = 'basic'
}

function openEditAssetForm(asset) {
  editingAssetId.value = asset.id
  assetForm.value = {
    ...createAssetForm(),
    type: asset.type === '-' ? '' : asset.type,
    affiliate: asset.affiliate === '-' ? '' : asset.affiliate,
    registeredAt: asset.registeredAt === '-' ? getTodayString() : asset.registeredAt,
    category: asset.category ?? 'customer',
    target: asset.target === '-' ? '' : asset.target,
    scale: asset.scale === '-' ? '' : asset.scale,
    exposureValue: asset.exposureValue === '미입력' ? '' : asset.exposureValue,
    performance: asset.performance === '미입력' ? '' : asset.performance,
    conditions: asset.conditions === '-' ? '' : asset.conditions,
    partnerFit: Array.isArray(asset.partnerFit)
      ? [...asset.partnerFit]
      : asset.partnerFit && asset.partnerFit !== '미입력'
        ? String(asset.partnerFit).split(',').map((item) => item.trim()).filter(Boolean)
        : [],
    blockedPartners: Array.isArray(asset.blockedPartners)
      ? [...asset.blockedPartners]
      : asset.blockedPartners && asset.blockedPartners !== '없음'
        ? String(asset.blockedPartners).replace(/^차단\s*/, '').split(',').map((item) => item.trim()).filter(Boolean)
        : [],
    supplyLimit: asset.supplyLimit === '미입력' ? '' : asset.supplyLimit,
    publicStatus: asset.publicStatus ?? 'PUBLIC',
    matchingStatus: asset.matchingStatus ?? 'ACTIVE',
  }
  isAssetFormOpen.value = true
  activeRegistrationSection.value = 'basic'
}

function closeAssetForm() {
  isAssetFormOpen.value = false
  editingAssetId.value = null
}

async function submitAsset() {
  if (!canSubmitAsset.value) return

  if (editingAssetId.value) {
    await UpdateAsset(editingAssetId.value, assetForm.value)
  } else {
    await CreateAsset(assetForm.value)
  }

  assetForm.value = createAssetForm()
  editingAssetId.value = null

  isAssetFormOpen.value = false
  await loadAssets()
}

async function deleteAsset(asset) {
  const confirmed = window.confirm(`'${asset.type}' 자산을 삭제할까요? 삭제하면 매칭 추천 입력값에서도 제외됩니다.`)
  if (!confirmed) return

  deletingAssetId.value = asset.id
  try {
    await DeleteAsset(asset.id)
    await loadAssets()
  } finally {
    deletingAssetId.value = null
  }
}

function saveAssetDraft() {
  isAssetFormOpen.value = false
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
          <span>자산/RFP</span>
          <span>파트너 매력도</span>
          <span>공급 한도</span>
          <span>매칭 가드레일</span>
          <span>공개/상태</span>
          <span>관리</span>
        </div>
        <div v-for="asset in rows" :key="asset.id" class="asset-table__row">
          <span class="asset-info">
            <b>{{ asset.type }}</b>
            <small>{{ asset.affiliate }} · 등록 {{ asset.registeredAt }}</small>
          </span>
          <span class="asset-info">
            <b>{{ asset.target }}</b>
            <small>노출 가치 {{ asset.exposureValue }} · 과거 성과 {{ asset.performance }}</small>
          </span>
          <span class="asset-info">
            <b>{{ asset.scale }}</b>
            <small>{{ asset.supplyLimit }} · {{ asset.conditions }}</small>
          </span>
          <span class="asset-info">
            <b>{{ asset.partnerFit }}</b>
            <small>차단 {{ asset.blockedPartners }} · {{ asset.accessPolicy }}</small>
          </span>
          <em :class="{ muted: !isAssetAvailable(asset) }">
            {{ assetStatusLabel(asset) }}
          </em>
          <span class="asset-row-actions">
            <button type="button" @click="openEditAssetForm(asset)">수정</button>
            <button
              type="button"
              class="danger"
              :disabled="deletingAssetId === asset.id"
              @click="deleteAsset(asset)"
            >
              {{ deletingAssetId === asset.id ? '삭제 중' : '삭제' }}
            </button>
          </span>
        </div>
      </div>

      <div v-else class="asset-table asset-table--benefits">
        <div class="asset-table__head">
          <span>파트너</span>
          <span>제안 혜택</span>
          <span>규모/환산 가치</span>
          <span>유효 기간</span>
          <span>비용 부담</span>
          <span>추천 자산</span>
          <span>검토 상태</span>
        </div>
        <div v-for="proposal in rows" :key="proposal.id" class="asset-table__row">
          <strong>{{ proposal.partner }}</strong>
          <span class="proposal-benefit">
            <b>{{ proposal.name }}</b>
            <small>{{ proposal.category }} · {{ proposal.type }} · {{ proposal.target }}</small>
          </span>
          <span class="proposal-metric">
            <b>{{ proposal.scale }}</b>
            <small>{{ proposal.value }} · {{ proposal.unitValue }}</small>
          </span>
          <span>{{ proposal.period }}</span>
          <span class="proposal-metric" :title="proposal.costDetail">
            <b>{{ proposal.cost }}</b>
            <small>{{ proposal.costDetail }}</small>
          </span>
          <span class="proposal-match" :class="{ muted: !proposal.matchScore }">
            <b>{{ proposal.matchAsset }}</b>
            <small v-if="proposal.matchScore">적합도 {{ proposal.matchScore }}%</small>
            <small v-else>{{ proposal.missing.join(', ') }}</small>
          </span>
          <span class="proposal-status">
            <em :class="{ muted: proposal.missing?.length || proposal.status === '임시 저장' }">
              {{ proposalStatusLabel(proposal) }}
            </em>
            <button v-if="proposal.missing?.length" type="button">보완 요청</button>
          </span>
        </div>
      </div>

      <div v-if="isAssetFormOpen" class="reg-modal" role="dialog" aria-modal="true" aria-labelledby="asset-reg-title">
        <form class="reg-panel" @submit.prevent="submitAsset">
          <header class="reg-head">
            <div class="reg-head__title">
              <strong id="asset-reg-title">{{ editingAssetId ? '한화 자산 수정' : '한화 자산 등록' }}</strong>
              <span>파트너 페이지에 노출되는 정보입니다. 정확하게 채울수록 매칭 추천이 정교해집니다.</span>
            </div>
            <div class="reg-head__right">
              <div class="reg-progress" :title="`필수 ${filledRequired}/${totalRequired}`">
                <div class="reg-progress__meta">
                  <span>필수 입력</span>
                  <b>{{ filledRequired }}/{{ totalRequired }}</b>
                </div>
                <div class="reg-progress__bar">
                  <div :style="{ width: `${progressPercent}%` }" />
                </div>
              </div>
              <button type="button" class="reg-close" aria-label="닫기" @click="closeAssetForm">×</button>
            </div>
          </header>

          <nav class="reg-nav" aria-label="등록 섹션">
            <button
              v-for="(section, index) in formSections"
              :key="section.id"
              type="button"
              class="reg-nav__item"
              :class="{
                active: activeRegistrationSection === section.id,
                done: sectionFilled(section) === section.required.length,
              }"
              @click="scrollToRegistrationSection(section.id)"
            >
              <em>{{ index + 1 }}</em>
              <span>
                <b>{{ section.title }}</b>
                <small>{{ sectionFilled(section) }}/{{ section.required.length }} 필수</small>
              </span>
            </button>
          </nav>

          <div ref="registrationBody" class="reg-body" @scroll="updateActiveRegistrationSection">
            <section id="asset-reg-basic" class="reg-section">
              <header class="reg-section__head">
                <strong>1. 기본 정보</strong>
                <small>자산을 식별할 핵심 정보</small>
              </header>

              <div class="reg-grid">
                <label class="reg-field reg-field--full">
                  <span class="reg-field__label">자산명 <em>*</em></span>
                  <input
                    v-model="assetForm.type"
                    placeholder="예: 갤러리아 VIP 고객층, 한화호텔 객실 재고, 이글스 홈경기 티켓"
                  />
                  <small class="reg-hint">파트너가 이 자산을 명확히 인식할 수 있는 이름으로 입력하세요.</small>
                </label>

                <label class="reg-field">
                  <span class="reg-field__label">보유 조직 <em>*</em></span>
                  <select v-model="assetForm.affiliate">
                    <option value="">선택하세요</option>
                    <option v-for="option in affiliateOptions" :key="option" :value="option">
                      {{ option }}
                    </option>
                  </select>
                  <small class="reg-hint">자산을 소유하거나 운영하는 계열사/부서입니다.</small>
                </label>

                <label class="reg-field">
                  <span class="reg-field__label">등록일</span>
                  <input v-model="assetForm.registeredAt" type="date" />
                  <small class="reg-hint">기본값은 오늘 날짜이며, 서버 등록일과 함께 관리됩니다.</small>
                </label>

                <div class="reg-field reg-field--full">
                  <span class="reg-field__label">자산 유형 <em>*</em></span>
                  <div class="reg-cards">
                    <button
                      v-for="assetType in assetTypes"
                      :key="assetType.value"
                      type="button"
                      class="reg-card"
                      :class="{ active: assetForm.category === assetType.value }"
                      @click="assetForm.category = assetType.value"
                    >
                      <b>{{ assetType.label }}</b>
                      <small>{{ assetType.desc }}</small>
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <section id="asset-reg-attractiveness" class="reg-section">
              <header class="reg-section__head">
                <strong>2. 매칭 매력도</strong>
                <small>파트너에게 보여줄 자산 가치와 추천 점수의 핵심 입력값</small>
              </header>

              <div class="reg-grid">
                <label class="reg-field reg-field--full">
                  <span class="reg-field__label">고객 규모 / 특성 <em>*</em></span>
                  <input
                    v-model="assetForm.target"
                    placeholder="예: VIP 고객 5만 명 / 2040 여성 / 평균 객단가 18만 원 / 활성률 70%"
                  />
                  <small class="reg-hint">규모, 인구통계, 활성률을 함께 적으면 추천 정확도가 올라갑니다.</small>
                </label>

                <label class="reg-field">
                  <span class="reg-field__label">제공 규모 <em>*</em></span>
                  <input v-model="assetForm.scale" placeholder="예: MAU 45만, 월 1만 객실, 시즌 50회" />
                  <small class="reg-hint">파트너 페이지에 표시될 실제 가용 규모입니다.</small>
                </label>

                <label class="reg-field">
                  <span class="reg-field__label">노출 가치</span>
                  <input v-model="assetForm.exposureValue" placeholder="예: 앱 배너 1주 = 시장가 1,500만 원" />
                  <small class="reg-hint">파트너 분담률 계산의 기준입니다.</small>
                </label>

                <label class="reg-field reg-field--full">
                  <span class="reg-field__label">과거 캠페인 성과</span>
                  <input
                    v-model="assetForm.performance"
                    placeholder="예: 평균 전환율 4.2%, 재방문율 +8%, 객실 점유율 +12%"
                  />
                  <small class="reg-hint">선택 입력. 비워두면 추천 시 업종 평균값으로 대체됩니다.</small>
                </label>
              </div>
            </section>

            <section id="asset-reg-conditions" class="reg-section">
              <header class="reg-section__head">
                <strong>3. 매칭 조건</strong>
                <small>어떤 파트너와, 어떤 조건으로 매칭할지 정의합니다.</small>
              </header>

              <div class="reg-grid">
                <label class="reg-field">
                  <span class="reg-field__label">사용 조건</span>
                  <input v-model="assetForm.conditions" placeholder="예: 월 1회 메인 팝업, 주중 한정" />
                  <small class="reg-hint">운영/노출 제약이 있으면 짧게 적어주세요.</small>
                </label>

                <label class="reg-field">
                  <span class="reg-field__label">이번 분기 공급 한도 <em>*</em></span>
                  <input v-model="assetForm.supplyLimit" placeholder="예: 객실 200박 중 80박 사용 가능" />
                  <small class="reg-hint">동시 캠페인 충돌을 막기 위해 필요합니다.</small>
                </label>

                <div class="reg-field reg-field--full">
                  <span class="reg-field__label">매칭 희망 파트너</span>
                  <small class="reg-hint reg-hint--top">선호하는 파트너 카테고리를 선택하거나 직접 입력하세요.</small>
                  <div class="reg-chips">
                    <button
                      v-for="option in partnerCategoryOptions"
                      :key="option"
                      type="button"
                      class="reg-chip"
                      :class="{ active: isChipActive('partnerFit', option) }"
                      @click="toggleChip('partnerFit', option)"
                    >
                      {{ option }}
                    </button>
                  </div>
                  <div class="reg-chip-input">
                    <input
                      v-model="customPartnerInput"
                      placeholder="직접 입력 후 Enter 또는 추가"
                      @keydown.enter.prevent="addCustomPartner"
                    />
                    <button type="button" @click="addCustomPartner">추가</button>
                  </div>
                  <div v-if="assetForm.partnerFit.length" class="reg-chips reg-chips--selected">
                    <span v-for="value in assetForm.partnerFit" :key="value" class="reg-chip-selected">
                      {{ value }}
                      <button type="button" aria-label="제거" @click="removeChip('partnerFit', value)">×</button>
                    </span>
                  </div>
                </div>

                <div class="reg-field reg-field--full">
                  <span class="reg-field__label">차단 카테고리</span>
                  <small class="reg-hint reg-hint--top">매칭에서 제외할 파트너 카테고리입니다.</small>
                  <div class="reg-chips">
                    <button
                      v-for="option in blockedCategoryOptions"
                      :key="option"
                      type="button"
                      class="reg-chip reg-chip--block"
                      :class="{ active: isChipActive('blockedPartners', option) }"
                      @click="toggleChip('blockedPartners', option)"
                    >
                      {{ option }}
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <section id="asset-reg-visibility" class="reg-section">
              <header class="reg-section__head">
                <strong>4. 공개 설정</strong>
                <small>파트너 페이지 노출 방식과 매칭 가능 상태를 정합니다.</small>
              </header>

              <div class="reg-grid">
                <div class="reg-field">
                  <span class="reg-field__label">파트너 공개 상태 <em>*</em></span>
                  <div class="reg-radios">
                    <label class="reg-radio" :class="{ active: assetForm.publicStatus === 'PUBLIC' }">
                      <input v-model="assetForm.publicStatus" type="radio" value="PUBLIC" />
                      <b>파트너 공개</b>
                      <small>모든 파트너가 RFP 열람</small>
                    </label>
                    <label class="reg-radio" :class="{ active: assetForm.publicStatus === 'PRIVATE' }">
                      <input v-model="assetForm.publicStatus" type="radio" value="PRIVATE" />
                      <b>비공개</b>
                      <small>지정 파트너만 초대 열람</small>
                    </label>
                  </div>
                </div>

                <div class="reg-field">
                  <span class="reg-field__label">매칭 상태 <em>*</em></span>
                  <div class="reg-radios">
                    <label class="reg-radio" :class="{ active: assetForm.matchingStatus === 'ACTIVE' }">
                      <input v-model="assetForm.matchingStatus" type="radio" value="ACTIVE" />
                      <b>매칭 활성</b>
                      <small>지금 매칭 가능</small>
                    </label>
                    <label class="reg-radio" :class="{ active: assetForm.matchingStatus === 'PAUSED' }">
                      <input v-model="assetForm.matchingStatus" type="radio" value="PAUSED" />
                      <b>휴면</b>
                      <small>임시 보류</small>
                    </label>
                    <label class="reg-radio" :class="{ active: assetForm.matchingStatus === 'EXCLUSIVE' }">
                      <input v-model="assetForm.matchingStatus" type="radio" value="EXCLUSIVE" />
                      <b>전속 협의</b>
                      <small>특정 파트너 한정</small>
                    </label>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <footer class="reg-foot">
            <span class="reg-foot__hint">
              <em>*</em> 표시는 필수 입력
              <b v-if="!canSubmitAsset">· 필수 {{ totalRequired - filledRequired }}개 남음</b>
              <b v-else class="reg-foot__hint--ok">· 모든 필수 항목 완료</b>
            </span>
            <div class="reg-foot__actions">
              <button type="button" class="reg-btn reg-btn--ghost" @click="closeAssetForm">취소</button>
              <button type="button" class="reg-btn reg-btn--secondary" @click="saveAssetDraft">임시 저장</button>
              <button type="submit" class="reg-btn reg-btn--primary" :disabled="!canSubmitAsset">
                {{ editingAssetId ? '수정 저장' : '등록' }}
              </button>
            </div>
          </footer>
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
  grid-template-columns: 1.05fr 1.35fr 0.9fr 1.2fr max-content 5.9rem;
}

.asset-table--benefits .asset-table__head,
.asset-table--benefits .asset-table__row {
  grid-template-columns: 0.62fr 1.35fr 1fr 0.9fr 1.08fr 1.05fr 1.1fr;
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

.proposal-benefit,
.proposal-metric,
.proposal-match,
.proposal-status,
.asset-info {
  display: grid;
  min-width: 0;
  gap: 0.12rem;
}

.proposal-benefit b,
.proposal-metric b,
.proposal-match b,
.asset-info b {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.proposal-benefit small,
.proposal-metric small,
.proposal-match small,
.asset-info small {
  overflow: hidden;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.proposal-match b {
  color: var(--accent-color);
}

.proposal-match.muted b {
  color: var(--muted-text);
}

.asset-table__row em {
  display: inline-flex;
  width: max-content;
  min-height: 1.45rem;
  align-items: center;
  justify-content: center;
  justify-self: start;
  border-radius: 999px;
  background: var(--color-success-light);
  color: var(--color-success-dark);
  padding: 0 0.7rem;
  font-size: 0.72rem;
  font-style: normal;
  font-weight: 900;
}

.asset-table__row em.muted {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.asset-row-actions {
  display: flex !important;
  gap: 0.35rem;
  justify-content: flex-end;
  justify-self: end;
  overflow: visible !important;
}

.asset-row-actions button {
  min-height: 1.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0 0.55rem;
  font-size: 0.7rem;
  font-weight: 900;
  cursor: pointer;
}

.asset-row-actions button:hover {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-color));
  color: var(--accent-color);
}

.asset-row-actions button.danger {
  color: var(--color-danger, #dc2626);
}

.asset-row-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.proposal-status em {
  justify-self: start;
  padding: 0 0.55rem;
}

.proposal-status button {
  justify-self: start;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0.2rem 0.45rem;
  font-size: 0.66rem;
  font-weight: 900;
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
  width: min(46rem, 100%);
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  grid-column: 1 / -1;
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

.asset-modal input,
.asset-modal select {
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

.reg-modal {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.4);
  padding: 1.25rem;
}

.reg-panel {
  display: grid;
  width: min(60rem, 100%);
  max-height: calc(100vh - 2.5rem);
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  background: var(--panel-color);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
}

.reg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid var(--border-color);
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--accent-color) 6%, var(--panel-color)),
      var(--panel-color)
    );
  padding: 0.95rem 1.1rem;
}

.reg-head__title {
  display: grid;
  gap: 0.22rem;
  min-width: 0;
}

.reg-head__title strong {
  color: var(--text-primary);
  font-size: 1.05rem;
  font-weight: 900;
  line-height: 1.1;
}

.reg-head__title span {
  color: var(--muted-text);
  font-size: 0.74rem;
  font-weight: 700;
}

.reg-head__right {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.reg-progress {
  display: grid;
  gap: 0.32rem;
  min-width: 11rem;
}

.reg-progress__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.reg-progress__meta span {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 800;
}

.reg-progress__meta b {
  color: var(--accent-color);
  font-size: 0.78rem;
  font-weight: 900;
}

.reg-progress__bar {
  height: 0.36rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-muted);
}

.reg-progress__bar > div {
  height: 100%;
  background: var(--accent-color);
  transition: width 0.25s ease;
}

.reg-close {
  width: 2.1rem;
  height: 2.1rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 1.15rem;
  font-weight: 900;
  cursor: pointer;
}

.reg-nav {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.5rem;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-muted);
  padding: 0.65rem 1.1rem;
}

.reg-nav__item {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.5rem 0.6rem;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.reg-nav__item:hover {
  border-color: color-mix(in srgb, var(--accent-color) 40%, var(--border-color));
}

.reg-nav__item.active {
  border-color: var(--border-color);
  background: var(--panel-color);
  box-shadow: none;
}

.reg-nav__item.done {
  border-color: var(--border-color);
  background: var(--panel-color);
}

.reg-nav__item.active.done {
  border-color: var(--border-color);
  background: var(--panel-color);
}

.reg-nav__item em {
  display: inline-flex;
  width: 1.55rem;
  height: 1.55rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 12%, transparent);
  color: var(--accent-color);
  font-size: 0.74rem;
  font-style: normal;
  font-weight: 900;
}

.reg-nav__item.done em {
  background: color-mix(in srgb, var(--accent-color) 12%, transparent);
  color: var(--accent-color);
}

.reg-nav__item.active em {
  background: color-mix(in srgb, var(--accent-color) 18%, transparent);
  color: var(--accent-color);
}

.reg-nav__item.active b {
  color: var(--accent-color);
}

.reg-nav__item span {
  display: grid;
  gap: 0.05rem;
  min-width: 0;
}

.reg-nav__item b {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reg-nav__item small {
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 800;
}

.reg-body {
  display: grid;
  gap: 1rem;
  overflow-y: auto;
  padding: 1.1rem 1.1rem 0.6rem;
}

.reg-section {
  display: grid;
  gap: 0.7rem;
  border: 1px solid var(--border-color);
  border-radius: 9px;
  background: var(--panel-muted);
  padding: 0.9rem;
}

.reg-section__head {
  display: grid;
  gap: 0.18rem;
  border-bottom: 1px dashed var(--border-color);
  padding-bottom: 0.6rem;
}

.reg-section__head strong {
  color: var(--text-primary);
  font-size: 0.94rem;
  font-weight: 900;
}

.reg-section__head small {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 700;
}

.reg-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.reg-field {
  display: grid;
  gap: 0.34rem;
  align-content: start;
  min-width: 0;
}

.reg-field--full {
  grid-column: 1 / -1;
}

.reg-field__label {
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 900;
}

.reg-field__label em {
  margin-left: 0.18rem;
  color: var(--accent-color);
  font-style: normal;
}

.reg-field input,
.reg-field select {
  height: 2.55rem;
  min-height: 2.55rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0 0.78rem;
  font-size: 0.84rem;
  font-weight: 700;
  transition: all 0.12s ease;
}

.reg-field input:focus,
.reg-field select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 16%, transparent);
}

.reg-hint {
  display: block;
  min-height: 0.9rem;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 700;
}

.reg-hint--top {
  margin-top: -0.18rem;
}

.reg-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.5rem;
}

.reg-card {
  display: grid;
  gap: 0.22rem;
  justify-items: start;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.7rem 0.75rem;
  cursor: pointer;
  text-align: left;
  transition: all 0.15s ease;
}

.reg-card:hover {
  border-color: color-mix(in srgb, var(--accent-color) 50%, var(--border-color));
  transform: translateY(-1px);
}

.reg-card.active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 7%, var(--panel-color));
  box-shadow: inset 0 0 0 1px var(--accent-color);
}

.reg-card b {
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.reg-card.active b {
  color: var(--accent-color);
}

.reg-card small {
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 700;
}

.reg-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.36rem;
}

.reg-chip {
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0.36rem 0.78rem;
  font-size: 0.74rem;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.13s ease;
}

.reg-chip:hover {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-color));
}

.reg-chip.active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--accent-color);
}

.reg-chip--block.active {
  border-color: var(--color-warning-dark);
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.reg-chip-input {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.45rem;
}

.reg-chip-input input {
  flex: 1;
  min-height: 2.25rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  padding: 0 0.65rem;
  font-size: 0.78rem;
  font-weight: 700;
}

.reg-chip-input button {
  min-height: 2.25rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0 0.95rem;
  font-size: 0.74rem;
  font-weight: 900;
  cursor: pointer;
}

.reg-chips--selected {
  margin-top: 0.5rem;
  border-top: 1px dashed var(--border-color);
  padding-top: 0.5rem;
}

.reg-chip-selected {
  display: inline-flex;
  align-items: center;
  gap: 0.32rem;
  border-radius: 999px;
  background: var(--accent-color);
  color: #fff;
  padding: 0.3rem 0.4rem 0.3rem 0.72rem;
  font-size: 0.72rem;
  font-weight: 900;
}

.reg-chip-selected button {
  display: inline-flex;
  width: 1.15rem;
  height: 1.15rem;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 900;
  cursor: pointer;
}

.reg-radios {
  display: grid;
  gap: 0.4rem;
}

.reg-radio {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  column-gap: 0.6rem;
  row-gap: 0.05rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.55rem 0.75rem;
  cursor: pointer;
  transition: all 0.13s ease;
}

.reg-radio:hover {
  border-color: color-mix(in srgb, var(--accent-color) 40%, var(--border-color));
}

.reg-radio.active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-color));
}

.reg-radio input {
  grid-row: 1 / 3;
  accent-color: var(--accent-color);
}

.reg-radio b {
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.reg-radio small {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 700;
}

.reg-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-top: 1px solid var(--border-color);
  background: var(--panel-color);
  padding: 0.85rem 1.1rem;
}

.reg-foot__hint {
  color: var(--muted-text);
  font-size: 0.74rem;
  font-weight: 800;
}

.reg-foot__hint em {
  color: var(--accent-color);
  font-style: normal;
}

.reg-foot__hint b {
  margin-left: 0.25rem;
  color: var(--muted-text);
  font-weight: 800;
}

.reg-foot__hint--ok {
  color: var(--color-success-dark) !important;
}

.reg-foot__actions {
  display: flex;
  gap: 0.5rem;
}

.reg-btn {
  min-height: 2.5rem;
  border-radius: 7px;
  padding: 0 1.15rem;
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.13s ease;
}

.reg-btn--primary {
  border: 1px solid var(--accent-color);
  background: var(--accent-color);
  color: #fff;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent-color) 20%, transparent);
}

.reg-btn--primary:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  box-shadow: none;
}

.reg-btn--secondary {
  border: 1px solid color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--accent-color);
}

.reg-btn--ghost {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
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

@media (max-width: 920px) {
  .reg-nav,
  .reg-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .reg-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .reg-field--full {
    grid-column: 1;
  }

  .reg-head,
  .reg-foot {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
