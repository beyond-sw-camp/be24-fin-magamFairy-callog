<script setup>
import { computed, onMounted, ref } from 'vue'
import { CreateAsset, DeleteAsset, ListAssets, UpdateAsset } from '@/api/matchingAssets'

defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['asset-count-change', 'request-matching'])

const currentSubTab = ref('assets')
const assets = ref([])
const isAssetLoading = ref(false)
const assetError = ref('')
const isAssetFormOpen = ref(false)
const registrationBody = ref(null)
const activeRegistrationSection = ref('basic')
const editingAssetId = ref(null)
const deletingAssetId = ref(null)
const selectedAssetId = ref(null)
const assetSearch = ref('')
const isAssetFilterOpen = ref(false)
const assetFilters = ref({
  category: 'all',
  status: 'all',
})

function getTodayString() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}

function createAssetForm() {
  return {
    type: '',
    affiliate: '',
    customAffiliate: '',
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
  { value: 'customer', label: '고객 자산', desc: '예: VIP, 멤버십, 고객DB', supply: '동시 활용 가능' },
  { value: 'channel', label: '채널 자산', desc: '예: 앱 배너, 알림톡, 푸시', supply: '슬롯 단위 충돌 관리' },
  { value: 'space', label: '공간 자산', desc: '예: 백화점 매장, 라운지, 전망대', supply: '동시 활용 가능' },
  { value: 'voucher', label: '상품/이용권 자산', desc: '예: 호텔 객실, 리조트 이용권, 스포츠 티켓', supply: '재고 단위 차감' },
  { value: 'content', label: '콘텐츠/IP 자산', desc: '예: 이벤트, 선수 IP, 영상 콘텐츠', supply: '라이선스 조건 관리' },
]

const affiliateOptions = [
  '브랜드 본사',
  '리테일 계열사',
  '호텔/리조트 계열사',
  '금융 계열사',
  '스포츠/엔터테인먼트 조직',
  '디지털 서비스 조직',
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
    type: '체험/시승',
    target: '2040 뷰티 고객 · VIP/프리미엄',
    reach: '20,000명',
    scale: '10,000개',
    quantity: '10,000',
    quantityUnit: '개',
    value: '총 5,000만 원',
    unitValue: '1인당 5,000원',
    cost: '파트너 전액 부담',
    costDetail: '혜택 원가+배송비 파트너 부담',
    costItems: '배송비 포함',
    period: '2026.05.01 - 2026.06.30',
    prepPeriod: '10일',
    conditions: '1인 1회 · 중복 사용 불가',
    channels: '자사 앱, 알림톡/문자, 제휴사 채널',
    outputs: '쿠폰 코드 발급, 배너/디자인, 알림톡 문구',
    contact: '이럭시드 / partner@luxid.co.kr',
    matchAsset: '갤러리아 VIP 고객층',
    matchScore: 87,
    category: '뷰티',
    missing: [],
    status: '접수 완료',
    description: '신제품 핸드크림 10ml 샘플 제공, 럭시드 신규 라인 체험 기회',
    ourOwner: '지정 필요',
    strengths: ['갤러리아 VIP 고객층과 적합도 87%', '파트너 전액 부담으로 비용 부담 없음'],
    risks: ['준비 기간 10일로 캠페인 일정 확인 필요'],
    scores: { customerFit: 90, revenue: 85, cost: 92, operation: 80, brand: 88 },
  },
  {
    id: 2,
    partner: '멜로우',
    name: '전시 시설 30% 할인권',
    type: '할인/쿠폰',
    target: '패밀리 · 4050 · 기존 고객',
    reach: '50,000명',
    scale: '제한 없음',
    quantity: '제한 없음',
    quantityUnit: '건',
    value: '할인율 기반 정산',
    unitValue: '1인당 최대 30%',
    cost: '파트너 전액 부담',
    costDetail: '혜택 원가 부담, 운영비 별도 협의',
    costItems: '운영비 협의',
    period: '상시 협의',
    prepPeriod: '1주',
    conditions: '특정 기간 한정 · 1인 1회',
    channels: '자사 웹, SNS, 오프라인 매장',
    outputs: '공동 랜딩페이지, 배너/디자인',
    contact: '박멜로우 / alliance@mellow.co.kr',
    matchAsset: '호텔 객실 패키지',
    matchScore: 82,
    category: '여행',
    missing: [],
    status: '평가 반영',
    description: '전시 시설 할인권을 활용해 기존 고객의 재방문을 유도하는 혜택',
    ourOwner: '제휴마케팅팀',
    strengths: ['기존 고객 재방문 목표와 연결이 명확함', '할인 비용을 파트너가 부담'],
    risks: ['운영비 분담 기준은 추가 협의 필요'],
    scores: { customerFit: 78, revenue: 80, cost: 88, operation: 84, brand: 82 },
  },
  {
    id: 3,
    partner: '어반터치',
    name: '오리지널 콘텐츠 공동 프로모션',
    type: '콘텐츠/이벤트',
    target: '미입력',
    reach: '미입력',
    scale: '1,000만',
    quantity: '미입력',
    quantityUnit: '건',
    value: '미입력',
    unitValue: '필수',
    cost: '미입력',
    costDetail: '비용 부담 구조 미입력',
    costItems: '미입력',
    period: '미입력',
    prepPeriod: '미입력',
    conditions: '기타 조건 미입력',
    channels: '미입력',
    outputs: '영상 콘텐츠, 보도자료 협의 필요',
    contact: '미입력',
    matchAsset: '매칭 불가',
    matchScore: null,
    category: '콘텐츠',
    missing: ['대상 고객', '비용 부담', '유효 기간'],
    status: '임시 저장',
    description: '오리지널 콘텐츠를 활용한 공동 프로모션 제안',
    ourOwner: '지정 필요',
    strengths: ['콘텐츠 협업 형태로 브랜드 노출 가능'],
    risks: ['대상 고객, 비용 부담, 유효 기간이 없어 매칭 판단 불가'],
    scores: null,
  },
]

const selectedBenefitProposalId = ref(partnerProposals[0]?.id ?? null)

const scoreMetrics = [
  { key: 'customerFit', label: '고객 적합도', weight: 25 },
  { key: 'revenue', label: '수익 기여도', weight: 25 },
  { key: 'cost', label: '비용 효율성', weight: 20 },
  { key: 'operation', label: '운영 용이성', weight: 15 },
  { key: 'brand', label: '브랜드 적합도', weight: 15 },
]

const selectedBenefitProposal = computed(
  () => partnerProposals.find((proposal) => proposal.id === selectedBenefitProposalId.value) ?? partnerProposals[0],
)

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
    owner: asset.owner ?? asset.manager ?? asset.managerName ?? asset.affiliate ?? '미입력',
  }
}

function assetCategoryLabel(value) {
  return assetTypes.find((assetType) => assetType.value === value)?.label ?? '미입력'
}

function displayValue(value) {
  if (Array.isArray(value)) return value.length ? value.join(', ') : '미입력'
  if (value === null || value === undefined || value === '') return '미입력'
  return value
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

function assetStatusFilterValue(asset) {
  if (isAssetAvailable(asset)) return 'active'
  const normalized = String(asset.matchingStatus ?? '').toUpperCase()
  if (normalized === 'EXCLUSIVE') return 'exclusive'
  return 'paused'
}

function resetAssetFilters() {
  assetFilters.value = {
    category: 'all',
    status: 'all',
  }
}

function proposalStatusLabel(proposal) {
  if (proposal.missing?.length) return `매칭 불가 · ${proposal.missing.length}개 누락`
  if (proposal.matchScore) return `${proposal.status} · ${proposal.matchScore}점`
  return proposal.status
}

function selectBenefitProposal(proposal) {
  selectedBenefitProposalId.value = proposal.id
}

function proposalPrimaryAction(proposal) {
  if (proposal.missing?.length) return '보완 요청'
  if (proposal.status === '평가 반영') return '운영 보드로 전환'
  return '수락하기'
}

function isFilled(field) {
  const value = assetForm.value[field]
  if (field === 'affiliate' && value === '직접 입력') {
    return assetForm.value.customAffiliate.trim() !== ''
  }
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

const selectedAssetType = computed(
  () => assetTypes.find((assetType) => assetType.value === assetForm.value.category) ?? null,
)

const supplyLimitPlaceholder = computed(() => {
  if (assetForm.value.category === 'voucher') return '예: 객실 200박 중 80박 사용 가능, 티켓 1,000석'
  if (assetForm.value.category === 'channel') return '예: 앱 배너 1주 2슬롯, 알림톡 월 2회'
  if (assetForm.value.category === 'space') return '예: 라운지 주말 4회, 매장 팝업 2주'
  return '예: 분기 캠페인 3건까지 활용 가능'
})

const supplyLimitHint = computed(() => {
  if (assetForm.value.category === 'voucher') {
    return '호텔 객실, 이용권, 티켓은 캠페인에 배정한 수량만큼 남은 재고에서 차감됩니다.'
  }
  if (assetForm.value.category === 'channel') {
    return '동시 캠페인 간 노출 슬롯 충돌을 막기 위해 필요합니다.'
  }
  return '동시 캠페인 충돌을 막기 위해 필요합니다.'
})

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
    if (!assets.value.some((asset) => asset.id === selectedAssetId.value)) {
      selectedAssetId.value = assets.value[0]?.id ?? null
    }
    emit('asset-count-change', assets.value.length)
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
  const isKnownAffiliate = affiliateOptions.includes(asset.affiliate)
  assetForm.value = {
    ...createAssetForm(),
    type: asset.type === '-' ? '' : asset.type,
    affiliate: isKnownAffiliate ? asset.affiliate : '직접 입력',
    customAffiliate: isKnownAffiliate || asset.affiliate === '-' ? '' : asset.affiliate,
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

  const payload = {
    ...assetForm.value,
    affiliate:
      assetForm.value.affiliate === '직접 입력'
        ? assetForm.value.customAffiliate.trim()
        : assetForm.value.affiliate,
  }

  if (editingAssetId.value) {
    await UpdateAsset(editingAssetId.value, payload)
  } else {
    await CreateAsset(payload)
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

const filteredAssets = computed(() => {
  const keyword = assetSearch.value.trim().toLowerCase()
  return assets.value.filter((asset) => {
    const matchesKeyword =
      !keyword ||
    [
      asset.type,
      asset.affiliate,
      asset.owner,
      asset.target,
      asset.scale,
      asset.supplyLimit,
      asset.conditions,
      asset.exposureValue,
      asset.performance,
      asset.partnerFit,
      asset.blockedPartners,
    ]
      .map(displayValue)
        .some((value) => String(value).toLowerCase().includes(keyword))

    const matchesCategory =
      assetFilters.value.category === 'all' || asset.category === assetFilters.value.category
    const matchesStatus =
      assetFilters.value.status === 'all' ||
      assetStatusFilterValue(asset) === assetFilters.value.status

    return matchesKeyword && matchesCategory && matchesStatus
  })
})

const activeAssetFilterCount = computed(
  () =>
    Number(assetFilters.value.category !== 'all') +
    Number(assetFilters.value.status !== 'all'),
)

const selectedAsset = computed(
  () =>
    filteredAssets.value.find((asset) => asset.id === selectedAssetId.value) ??
    filteredAssets.value[0] ??
    assets.value.find((asset) => asset.id === selectedAssetId.value) ??
    assets.value[0] ??
    null,
)

function isEmptyAssetValue(value) {
  return value === null || value === undefined || value === '' || value === '미입력' || value === '-'
}

const selectedAssetMetrics = computed(() => {
  const asset = selectedAsset.value
  if (!asset) return []

  return [
    {
      label: '매칭 타깃',
      value: asset.target,
      sub: isEmptyAssetValue(asset.exposureValue) ? '' : `노출 가치 ${asset.exposureValue}`,
    },
    {
      label: '공급 한도',
      value: asset.scale,
      sub: asset.supplyLimit,
    },
    {
      label: '매칭 가드레일',
      value: asset.partnerFit,
      sub: isEmptyAssetValue(asset.blockedPartners) ? '차단 없음' : `차단 ${asset.blockedPartners}`,
    },
  ]
})

const selectedAssetMetaItems = computed(() => {
  const asset = selectedAsset.value
  if (!asset) return []

  return [
    { label: '자산 유형', value: assetCategoryLabel(asset.category), key: 'category' },
    { label: '소속 RFP', value: asset.affiliate, key: 'affiliate' },
    { label: '등록/소유 조직', value: asset.affiliate, key: 'organization' },
    { label: '운영 조건', value: asset.conditions, key: 'conditions' },
    { label: '과거 성과', value: asset.performance, key: 'performance' },
    { label: '희망 파트너', value: asset.partnerFit, key: 'partnerFit' },
    { label: '공개 정책', value: assetStatusLabel(asset), key: 'publicPolicy' },
  ]
})

const filledAssetMetaItems = computed(() =>
  selectedAssetMetaItems.value.filter((item) => !isEmptyAssetValue(item.value)),
)

const emptyAssetMetaItems = computed(() =>
  selectedAssetMetaItems.value.filter((item) => isEmptyAssetValue(item.value)),
)
</script>

<template>
  <section class="asset-workspace">
    <article class="asset-panel">
      <div class="asset-panel__head">
        <div class="asset-panel__title">
          <h3>매칭 추천</h3>
          <p>입력한 캠페인 조건을 기준으로 추천 조합을 확인합니다.</p>
        </div>

        <div class="asset-toolbar">
          <button
            type="button"
            class="asset-recommend"
            @click="emit('request-matching')"
          >
            매칭 추천 받기
          </button>
        </div>
      </div>

      <p v-if="assetError" class="asset-message">{{ assetError }}</p>
      <p v-else-if="isAssetLoading" class="asset-message">불러오는 중입니다.</p>

      <div v-if="currentSubTab === 'assets'" class="asset-browser">
        <aside class="asset-browser__list" aria-label="자산 목록">
          <div class="asset-list-tools">
            <label class="asset-list-search">
              <span aria-hidden="true">⌕</span>
              <input v-model="assetSearch" type="search" placeholder="자산 검색" />
            </label>
            <button
              type="button"
              class="asset-filter-button"
              :class="{ active: activeAssetFilterCount > 0 }"
              aria-label="자산 필터"
              :aria-expanded="isAssetFilterOpen"
              @click="isAssetFilterOpen = !isAssetFilterOpen"
            >
              <span aria-hidden="true">▽</span>
              <em v-if="activeAssetFilterCount">{{ activeAssetFilterCount }}</em>
            </button>
            <div v-if="isAssetFilterOpen" class="asset-filter-popover">
              <div class="asset-filter-popover__head">
                <strong>필터</strong>
                <button type="button" @click="resetAssetFilters">초기화</button>
              </div>

              <label>
                <span>자산 유형</span>
                <select v-model="assetFilters.category">
                  <option value="all">전체</option>
                  <option v-for="assetType in assetTypes" :key="assetType.value" :value="assetType.value">
                    {{ assetType.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>상태</span>
                <select v-model="assetFilters.status">
                  <option value="all">전체</option>
                  <option value="active">매칭 활성</option>
                  <option value="paused">휴면/비공개</option>
                  <option value="exclusive">전속 협의</option>
                </select>
              </label>
            </div>
          </div>
          <button
            v-for="asset in filteredAssets"
            :key="asset.id"
            type="button"
            class="asset-list-item"
            :class="{ active: selectedAsset?.id === asset.id }"
            @click="selectedAssetId = asset.id"
          >
            <span class="asset-list-item__mark">{{ String(asset.type).slice(0, 2) }}</span>
            <span class="asset-list-item__text">
              <strong>{{ asset.type }}</strong>
              <small>{{ asset.affiliate }} · {{ asset.target }}</small>
            </span>
            <em :class="{ muted: !isAssetAvailable(asset) }">{{ assetStatusLabel(asset) }}</em>
          </button>
          <p v-if="!filteredAssets.length" class="asset-list-empty">검색 결과가 없습니다.</p>
        </aside>

        <article v-if="selectedAsset" class="asset-detail asset-detail--refined">
          <header class="asset-detail__head asset-detail__head--refined">
            <div class="asset-detail__identity asset-detail__identity--refined">
              <span class="asset-detail__avatar">{{ String(selectedAsset.type).slice(0, 2) || '?' }}</span>
              <div>
                <h4>{{ selectedAsset.type }}</h4>
                <p>
                  <span>{{ selectedAsset.affiliate }}</span>
                  <span aria-hidden="true">·</span>
                  <span>등록 {{ selectedAsset.registeredAt }}</span>
                </p>
              </div>
            </div>
            <div class="asset-detail__actions asset-detail__actions--refined">
              <em :class="{ muted: !isAssetAvailable(selectedAsset) }">{{ assetStatusLabel(selectedAsset) }}</em>
              <button type="button" @click="openEditAssetForm(selectedAsset)">수정</button>
              <button
                type="button"
                class="danger"
                :disabled="deletingAssetId === selectedAsset.id"
                @click="deleteAsset(selectedAsset)"
              >
                {{ deletingAssetId === selectedAsset.id ? '삭제 중' : '삭제' }}
              </button>
            </div>
          </header>

          <section class="asset-detail__metrics asset-detail__metrics--refined">
            <div
              v-for="metric in selectedAssetMetrics"
              :key="metric.label"
              :class="{ empty: isEmptyAssetValue(metric.value) }"
            >
              <span>{{ metric.label }}</span>
              <strong>{{ isEmptyAssetValue(metric.value) ? '미입력' : metric.value }}</strong>
              <small v-if="!isEmptyAssetValue(metric.sub)">{{ metric.sub }}</small>
            </div>
          </section>

          <section class="asset-detail__info asset-detail__info--refined">
            <div class="asset-detail__info-head">
              <h5>상세 정보</h5>
              <span v-if="emptyAssetMetaItems.length">{{ emptyAssetMetaItems.length }}개 항목 미입력</span>
            </div>

            <dl v-if="filledAssetMetaItems.length" class="asset-detail__meta-grid">
              <div v-for="item in filledAssetMetaItems" :key="item.key">
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>

            <details v-if="emptyAssetMetaItems.length" class="asset-detail__empty-meta">
              <summary>
                <span>미입력 항목 펼쳐 보기</span>
                <small>{{ emptyAssetMetaItems.length }}개</small>
              </summary>
              <dl class="asset-detail__meta-grid asset-detail__meta-grid--empty">
                <div v-for="item in emptyAssetMetaItems" :key="item.key">
                  <dt>{{ item.label }}</dt>
                  <dd>미입력</dd>
                </div>
              </dl>
            </details>
          </section>
        </article>

        <article v-else class="asset-detail asset-detail--empty">
          <h4>등록된 자산이 없습니다.</h4>
          <p>오른쪽 목록에 표시할 자산을 먼저 등록하세요.</p>
        </article>
      </div>

      <div v-else class="benefit-review">
        <section class="benefit-list-panel">
          <div class="asset-table asset-table--benefits">
            <div class="asset-table__head">
              <span>파트너</span>
              <span>혜택 제안</span>
              <span>규모/기간</span>
              <span>추천 자산</span>
              <span>검토 상태</span>
            </div>
            <button
              v-for="proposal in rows"
              :key="proposal.id"
              type="button"
              class="asset-table__row benefit-row"
              :class="{ selected: selectedBenefitProposal?.id === proposal.id }"
              @click="selectBenefitProposal(proposal)"
            >
              <strong>{{ proposal.partner }}</strong>
              <span class="proposal-benefit">
                <b>{{ proposal.name }}</b>
                <small>{{ proposal.type }} · {{ proposal.target }}</small>
              </span>
              <span class="proposal-metric">
                <b>{{ proposal.quantity }}{{ proposal.quantity !== '제한 없음' && proposal.quantity !== '미입력' ? proposal.quantityUnit : '' }}</b>
                <small>{{ proposal.period }}</small>
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
              </span>
            </button>
          </div>
        </section>

        <aside v-if="selectedBenefitProposal" class="benefit-detail-panel">
          <header class="benefit-detail-hero">
            <div>
              <strong>{{ selectedBenefitProposal.partner }} — {{ selectedBenefitProposal.name }}</strong>
              <p>{{ selectedBenefitProposal.type }} · {{ selectedBenefitProposal.target }} · 도달 {{ selectedBenefitProposal.reach }}</p>
            </div>
            <span class="benefit-score" :class="{ muted: !selectedBenefitProposal.matchScore }">
              {{ selectedBenefitProposal.matchScore ? selectedBenefitProposal.matchScore + '점' : '보완 필요' }}
            </span>
          </header>

          <ul class="benefit-summary-list">
            <li v-for="item in selectedBenefitProposal.strengths" :key="item">✓ {{ item }}</li>
            <li v-for="item in selectedBenefitProposal.risks" :key="item" class="risk">! {{ item }}</li>
          </ul>

          <section v-if="selectedBenefitProposal.scores" class="benefit-score-grid">
            <h4>점수 근거</h4>
            <div v-for="metric in scoreMetrics" :key="metric.key" class="benefit-score-row">
              <span>{{ metric.label }} <small>{{ metric.weight }}%</small></span>
              <div><i :style="{ width: selectedBenefitProposal.scores[metric.key] + '%' }" /></div>
              <strong>{{ selectedBenefitProposal.scores[metric.key] }}</strong>
            </div>
          </section>

          <section v-else class="benefit-missing-box">
            <h4>보완 필요 항목</h4>
            <span v-for="item in selectedBenefitProposal.missing" :key="item">{{ item }}</span>
          </section>

          <dl class="benefit-detail-grid">
            <div>
              <dt>기본 정보</dt>
              <dd><strong>{{ selectedBenefitProposal.name }}</strong><small>{{ selectedBenefitProposal.description }}</small></dd>
            </div>
            <div>
              <dt>규모·재고</dt>
              <dd><strong>{{ selectedBenefitProposal.quantity }}{{ selectedBenefitProposal.quantity !== '제한 없음' && selectedBenefitProposal.quantity !== '미입력' ? selectedBenefitProposal.quantityUnit : '' }}</strong><small>{{ selectedBenefitProposal.unitValue }} · {{ selectedBenefitProposal.value }}</small></dd>
            </div>
            <div>
              <dt>기간</dt>
              <dd><strong>{{ selectedBenefitProposal.period }}</strong><small>준비 {{ selectedBenefitProposal.prepPeriod }}</small></dd>
            </div>
            <div>
              <dt>대상</dt>
              <dd><strong>{{ selectedBenefitProposal.target }}</strong><small>예상 도달 {{ selectedBenefitProposal.reach }}</small></dd>
            </div>
            <div>
              <dt>비용 부담</dt>
              <dd><strong>{{ selectedBenefitProposal.cost }}</strong><small>{{ selectedBenefitProposal.costDetail }} · {{ selectedBenefitProposal.costItems }}</small></dd>
            </div>
            <div>
              <dt>운영 조건</dt>
              <dd><strong>{{ selectedBenefitProposal.channels }}</strong><small>{{ selectedBenefitProposal.outputs }} · {{ selectedBenefitProposal.conditions }}</small></dd>
            </div>
            <div>
              <dt>연결 자산</dt>
              <dd><strong>{{ selectedBenefitProposal.matchAsset }}</strong><small>{{ selectedBenefitProposal.matchScore ? '적합도 ' + selectedBenefitProposal.matchScore + '%' : selectedBenefitProposal.missing.join(', ') }}</small></dd>
            </div>
            <div>
              <dt>담당자</dt>
              <dd><strong>{{ selectedBenefitProposal.contact }}</strong><small>우리 측 담당자: {{ selectedBenefitProposal.ourOwner }}</small></dd>
            </div>
          </dl>

          <label class="benefit-comment">
            <span>코멘트 / 보완 요청</span>
            <textarea rows="3" placeholder="검토 의견이나 파트너에게 요청할 보완 내용을 입력하세요." />
          </label>

          <div class="benefit-actions">
            <button type="button" class="primary">{{ proposalPrimaryAction(selectedBenefitProposal) }}</button>
            <button type="button">보류</button>
            <button type="button">보완 요청</button>
            <button type="button">거절</button>
          </div>
        </aside>
      </div>

      <div v-if="isAssetFormOpen" class="reg-modal" role="dialog" aria-modal="true" aria-labelledby="asset-reg-title">
        <form class="reg-panel" @submit.prevent="submitAsset">
          <header class="reg-head">
            <div class="reg-head__title">
              <strong id="asset-reg-title">{{ editingAssetId ? '자산 수정' : '자산 등록' }}</strong>
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
                    placeholder="예: VIP 고객층, 객실 재고, 멤버십 채널, 이벤트 티켓"
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
                  <input
                    v-if="assetForm.affiliate === '직접 입력'"
                    v-model="assetForm.customAffiliate"
                    class="reg-field__subinput"
                    placeholder="예: 브랜드전략팀, CRM팀, 리테일사업부"
                  />
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
                      <em>{{ assetType.supply }}</em>
                    </button>
                  </div>
                  <small v-if="selectedAssetType" class="reg-hint">
                    선택 유형: {{ selectedAssetType.supply }}
                  </small>
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
                  <input v-model="assetForm.supplyLimit" :placeholder="supplyLimitPlaceholder" />
                  <small class="reg-hint">{{ supplyLimitHint }}</small>
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

.asset-recommend {
  display: inline-flex;
  min-height: 2.55rem;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--accent-color);
  color: #fff;
  padding: 0 1rem;
  font-size: 0.82rem;
  font-weight: 900;
  white-space: nowrap;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--accent-color) 18%, transparent);
}

.asset-message {
  margin: 0 0 0.65rem;
  color: var(--text-secondary);
  font-size: 0.84rem;
  font-weight: 800;
}

.asset-browser {
  display: grid;
  grid-template-columns: minmax(13rem, 0.42fr) minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
}

.asset-browser__list {
  display: grid;
  align-content: start;
  gap: 0.18rem;
  min-width: 0;
  overflow-y: auto;
  border-right: 1px solid var(--border-color);
  background: var(--panel-muted);
  padding: 0.45rem;
}

.asset-list-tools {
  position: sticky;
  top: 0;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 2rem;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.22rem;
  background: var(--panel-muted);
}

.asset-list-search {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 0.4rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  padding: 0 0.65rem;
}

.asset-list-search span {
  color: var(--muted-text);
  font-size: 0.95rem;
  font-weight: 900;
  line-height: 1;
}

.asset-list-search input {
  min-width: 0;
  min-height: 2.15rem;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  font-size: 0.76rem;
  font-weight: 800;
}

.asset-list-search input:focus {
  outline: none;
}

.asset-filter-button {
  position: relative;
  display: inline-flex;
  width: 2rem;
  height: 2.15rem;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
}

.asset-filter-button.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-color));
  color: var(--accent-color);
}

.asset-filter-button span {
  display: inline-block;
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 900;
  transform: rotate(90deg);
}

.asset-filter-button.active span {
  color: var(--accent-color);
}

.asset-filter-button em {
  position: absolute;
  top: -0.28rem;
  right: -0.28rem;
  display: inline-flex;
  min-width: 1rem;
  height: 1rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--accent-color);
  color: #fff;
  font-size: 0.6rem;
  font-style: normal;
  font-weight: 900;
}

.asset-filter-popover {
  position: absolute;
  top: calc(100% + 0.35rem);
  right: 0;
  z-index: 5;
  display: grid;
  width: min(16rem, calc(100vw - 2rem));
  gap: 0.65rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.14);
  padding: 0.75rem;
}

.asset-filter-popover__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.asset-filter-popover__head strong {
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.asset-filter-popover__head button {
  border: 0;
  background: transparent;
  color: var(--accent-color);
  font-size: 0.7rem;
  font-weight: 900;
  cursor: pointer;
}

.asset-filter-popover label {
  display: grid;
  gap: 0.28rem;
}

.asset-filter-popover label > span {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.asset-filter-popover select {
  min-height: 2.2rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-primary);
  padding: 0 0.65rem;
  font-size: 0.76rem;
  font-weight: 800;
}

.asset-filter-popover select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 14%, transparent);
}

.asset-list-empty {
  margin: 0.9rem 0.35rem;
  color: var(--muted-text);
  font-size: 0.76rem;
  font-weight: 800;
  text-align: center;
}

.asset-list-item {
  display: grid;
  grid-template-columns: 2rem minmax(0, 1fr);
  gap: 0.45rem;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  padding: 0.48rem;
  cursor: pointer;
  text-align: left;
}

.asset-list-item.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-color));
  box-shadow: inset 3px 0 0 var(--accent-color);
}

.asset-list-item__mark {
  display: inline-flex;
  width: 1.8rem;
  height: 1.8rem;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--accent-color) 22%, var(--border-color));
  border-radius: 7px;
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-color));
  color: var(--accent-color);
  font-size: 0.68rem;
  font-weight: 900;
}

.asset-list-item__text {
  display: grid;
  min-width: 0;
  gap: 0.12rem;
}

.asset-list-item__text strong,
.asset-list-item__text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-list-item__text strong {
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 900;
}

.asset-list-item__text small {
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 750;
}

.asset-list-item > em {
  grid-column: 2;
  justify-self: start;
  border-radius: 999px;
  background: var(--color-success-light);
  color: var(--color-success-dark);
  padding: 0.12rem 0.45rem;
  font-size: 0.62rem;
  font-style: normal;
  font-weight: 900;
}

.asset-list-item > em.muted {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.asset-detail {
  display: grid;
  align-content: start;
  gap: 0.75rem;
  min-width: 0;
  overflow-y: auto;
  padding: 0.9rem;
}

.asset-detail__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.75rem;
}

.asset-detail__identity {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 0.65rem;
}

.asset-detail__identity > span {
  display: inline-flex;
  width: 2.7rem;
  height: 2.7rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-color));
  color: var(--accent-color);
  font-size: 0.9rem;
  font-weight: 900;
}

.asset-detail__identity h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.02rem;
  font-weight: 900;
}

.asset-detail__identity p {
  margin: 0.2rem 0 0;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 750;
}

.asset-detail__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.35rem;
}

.asset-detail__actions em {
  display: inline-flex;
  min-height: 1.75rem;
  align-items: center;
  border-radius: 999px;
  background: var(--color-success-light);
  color: var(--color-success-dark);
  padding: 0 0.7rem;
  font-size: 0.7rem;
  font-style: normal;
  font-weight: 900;
}

.asset-detail__actions em.muted {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.asset-detail__actions button {
  min-height: 1.75rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  padding: 0 0.62rem;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
}

.asset-detail__actions button.danger {
  color: var(--color-danger, #dc2626);
}

.asset-detail__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
}

.asset-detail__metrics > div {
  display: grid;
  gap: 0.18rem;
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.7rem;
}

.asset-detail__metrics span,
.asset-detail__info dt {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.asset-detail__metrics strong,
.asset-detail__metrics small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-detail__metrics strong {
  color: var(--text-primary);
  font-size: 0.84rem;
  font-weight: 900;
}

.asset-detail__metrics small {
  color: var(--text-secondary);
  font-size: 0.68rem;
  font-weight: 750;
}

.asset-detail__info {
  display: grid;
  gap: 0.45rem;
}

.asset-detail__info h5 {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 900;
}

.asset-detail__info dl {
  display: grid;
  margin: 0;
  border-top: 1px solid var(--border-color);
}

.asset-detail__info dl > div {
  display: grid;
  grid-template-columns: 7.4rem minmax(0, 1fr);
  gap: 0.7rem;
  border-bottom: 1px solid var(--border-color);
  padding: 0.74rem 0;
}

.asset-detail__info dd {
  display: grid;
  gap: 0.16rem;
  min-width: 0;
  margin: 0;
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 800;
}

.asset-detail__info dd strong,
.asset-detail__info dd small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-detail__info dd strong {
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.asset-detail__info dd small {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 750;
}

.asset-detail--empty {
  place-content: center;
  text-align: center;
}

.asset-detail--empty h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 900;
}

.asset-detail--empty p {
  margin: 0.35rem 0 0;
  color: var(--muted-text);
  font-size: 0.76rem;
  font-weight: 750;
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
  grid-template-columns: 0.6fr 1.45fr 0.95fr 1fr 0.9fr;
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

.benefit-review {
  display: grid;
  grid-template-columns: minmax(500px, 1fr) minmax(360px, 0.62fr);
  gap: 0.7rem;
  min-height: 0;
}

.benefit-list-panel,
.benefit-detail-panel {
  min-width: 0;
  min-height: 0;
}

.benefit-list-panel {
  overflow: auto;
}

.benefit-row {
  width: 100%;
  border-color: var(--border-color);
  cursor: pointer;
  text-align: left;
}

.benefit-row:hover,
.benefit-row.selected {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-muted));
}

.benefit-row.selected {
  box-shadow: inset 3px 0 0 var(--accent-color);
}

.benefit-detail-panel {
  display: grid;
  align-content: start;
  gap: 0.65rem;
  overflow: auto;
  border: 1px solid color-mix(in srgb, var(--border-strong) 74%, var(--accent-color));
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.75rem;
}

.benefit-detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.65rem;
}

.benefit-detail-hero div {
  display: grid;
  gap: 0.16rem;
  min-width: 0;
}

.benefit-detail-hero strong {
  color: var(--text-primary);
  font-size: 0.92rem;
  font-weight: 900;
  line-height: 1.35;
}

.benefit-detail-hero p {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 760;
  line-height: 1.4;
}

.benefit-score {
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--color-success-light);
  color: var(--color-success-dark);
  padding: 0.35rem 0.65rem;
  font-size: 0.76rem;
  font-weight: 900;
}

.benefit-score.muted {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.benefit-summary-list {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-color));
  padding: 0.65rem 0.75rem;
  list-style: none;
}

.benefit-summary-list li {
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 820;
  line-height: 1.45;
}

.benefit-summary-list li.risk {
  color: var(--color-warning-dark, #b45309);
}

.benefit-score-grid,
.benefit-missing-box,
.benefit-detail-grid div,
.benefit-comment {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.65rem;
}

.benefit-score-grid {
  display: grid;
  gap: 0.42rem;
}

.benefit-score-grid h4,
.benefit-missing-box h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.benefit-score-row {
  display: grid;
  grid-template-columns: 6.8rem minmax(0, 1fr) 2rem;
  align-items: center;
  gap: 0.45rem;
}

.benefit-score-row span {
  color: var(--text-secondary);
  font-size: 0.7rem;
  font-weight: 850;
}

.benefit-score-row small {
  color: var(--muted-text);
  font-size: 0.6rem;
}

.benefit-score-row div {
  height: 0.42rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-color);
}

.benefit-score-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.benefit-score-row strong {
  color: var(--text-primary);
  font-size: 0.74rem;
  text-align: right;
}

.benefit-missing-box {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.benefit-missing-box h4 {
  flex-basis: 100%;
}

.benefit-missing-box span {
  border-radius: 999px;
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
  padding: 0.25rem 0.5rem;
  font-size: 0.68rem;
  font-weight: 900;
}

.benefit-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin: 0;
}

.benefit-detail-grid dt,
.benefit-comment span {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.benefit-detail-grid dd {
  display: grid;
  gap: 0.18rem;
  margin: 0.16rem 0 0;
}

.benefit-detail-grid strong {
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 900;
  line-height: 1.35;
}

.benefit-detail-grid small {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 740;
  line-height: 1.42;
}

.benefit-comment {
  display: grid;
  gap: 0.4rem;
}

.benefit-comment textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0.6rem 0.7rem;
  font: inherit;
  font-size: 0.76rem;
}

.benefit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.benefit-actions button {
  min-height: 2.15rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  padding: 0 0.75rem;
  font-size: 0.74rem;
  font-weight: 900;
}

.benefit-actions button.primary {
  border-color: var(--accent-color);
  background: var(--accent-color);
  color: #fff;
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

.reg-field__subinput {
  margin-top: 0.18rem;
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.65rem;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--panel-muted);
  padding: 0.45rem;
}

.reg-card {
  position: relative;
  display: grid;
  gap: 0.22rem;
  justify-items: start;
  min-height: 6.2rem;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.78rem 0.82rem;
  cursor: pointer;
  text-align: left;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: all 0.15s ease;
}

.reg-card:hover {
  border-color: color-mix(in srgb, var(--accent-color) 50%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 3%, var(--panel-color));
  transform: translateY(-1px);
}

.reg-card.active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
  box-shadow: inset 0 0 0 1px var(--accent-color);
}

.reg-card.active::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  border-radius: 8px 0 0 8px;
  background: var(--accent-color);
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

.reg-card em {
  color: var(--accent-color);
  font-size: 0.62rem;
  font-style: normal;
  font-weight: 900;
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


.asset-detail--refined {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  min-height: 0;
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  background: var(--panel-color);
  padding: 1rem 1.1rem 1.1rem;
}

.asset-detail__head--refined {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.85rem;
}

.asset-detail__identity--refined {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  min-width: 0;
}

.asset-detail__avatar {
  display: inline-flex;
  width: 2.2rem;
  height: 2.2rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: color-mix(in srgb, var(--accent-color) 14%, var(--panel-muted));
  color: var(--accent-color);
  font-size: 0.78rem;
  font-weight: 900;
}

.asset-detail__identity--refined h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
  line-height: 1.25;
}

.asset-detail__identity--refined p {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin: 0.15rem 0 0;
  color: var(--muted-text);
  font-size: 0.74rem;
  font-weight: 700;
}

.asset-detail__actions--refined {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;
}

.asset-detail__metrics--refined {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
}

.asset-detail__metrics--refined div {
  display: flex;
  min-height: 4.6rem;
  flex-direction: column;
  justify-content: center;
  gap: 0.2rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.7rem 0.85rem 0.75rem;
}

.asset-detail__metrics--refined span {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.asset-detail__metrics--refined strong {
  color: var(--text-primary);
  font-size: 1.1rem;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.asset-detail__metrics--refined small {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 700;
}

.asset-detail__metrics--refined div.empty strong {
  color: var(--subtle-text, #9ca3af);
  font-size: 0.85rem;
  font-style: italic;
  font-weight: 800;
}

.asset-detail__info--refined {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.asset-detail__info-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
}

.asset-detail__info-head h5 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.85rem;
  font-weight: 900;
}

.asset-detail__info-head span {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 700;
}

.asset-detail__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.4rem 1rem;
  margin: 0;
}

.asset-detail__meta-grid > div {
  display: grid;
  grid-template-columns: 6.5rem minmax(0, 1fr);
  align-items: baseline;
  gap: 0.55rem;
  border-bottom: 1px solid var(--border-color);
  padding: 0.45rem 0;
}

.asset-detail__meta-grid dt {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.asset-detail__meta-grid dd {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.4;
  word-break: break-word;
}

.asset-detail__empty-meta {
  margin-top: 0.3rem;
}

.asset-detail__empty-meta summary {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px dashed var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--muted-text);
  padding: 0.5rem 0.7rem;
  cursor: pointer;
  font-size: 0.72rem;
  font-weight: 800;
  list-style: none;
}

.asset-detail__empty-meta summary::-webkit-details-marker {
  display: none;
}

.asset-detail__empty-meta summary::before {
  content: '▸';
  font-size: 0.65rem;
  transition: transform 0.15s;
}

.asset-detail__empty-meta[open] summary::before {
  transform: rotate(90deg);
}

.asset-detail__empty-meta summary small {
  margin-left: auto;
  color: var(--subtle-text, #9ca3af);
  font-size: 0.68rem;
  font-weight: 800;
}

.asset-detail__meta-grid--empty {
  margin-top: 0.5rem;
  opacity: 0.65;
}

.asset-detail__meta-grid--empty dd {
  color: var(--subtle-text, #9ca3af);
  font-style: italic;
  font-weight: 700;
}

@media (max-width: 720px) {
  .asset-detail__metrics--refined,
  .asset-detail__meta-grid {
    grid-template-columns: 1fr;
  }

  .asset-detail__meta-grid > div {
    grid-template-columns: 6rem minmax(0, 1fr);
  }
}

</style>
