<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false,
  },
  recommendationCriteria: {
    type: Object,
    default: null,
  },
})
const emit = defineEmits(['request-evaluation'])

const selectedGoal = ref('PURCHASE_BOOKING')
const drawerComboId = ref(null)
const checkedComboIds = ref(new Set())
const evaluatedComboIds = ref(new Set())
const toastMessage = ref(null)
const isSidebarCollapsed = ref(false)
let toastTimer = null

const goals = [
  { id: 'NEW_CUSTOMER', name: '신규 고객 유입', count: 1 },
  { id: 'CUSTOMER_REVISIT', name: '기존 고객 재방문', count: 1 },
  { id: 'MEMBER_SIGNUP', name: '회원 가입 유도', count: 1 },
  { id: 'PURCHASE_BOOKING', name: '구매/예약 유도', count: 1 },
  { id: 'BRAND_AWARENESS', name: '브랜드 인지도 확대', count: 2 },
  { id: 'REVENUE', name: '매출 증대', count: 1 },
  { id: 'UPSELL', name: '객단가/업셀 향상', count: 1 },
  { id: 'DIRECT_BOOKING', name: '직접예약 비중 확대', count: 1 },
  { id: 'REVIEW_REPUTATION', name: '리뷰/평판 개선', count: 1 },
  { id: 'OTHER', name: '기타', count: 0 },
]

const campaignMethodLabels = {
  COUPON_DISCOUNT: '쿠폰/할인 혜택',
  TRIAL_GIFT: '체험권/사은품 제공',
  MEMBERSHIP_LOYALTY: '멤버십·로열티 강화',
  JOINT_PROMOTION: '공동 프로모션',
  CONTENT_COLLABORATION: '콘텐츠 협업',
  CHANNEL_APP_PROMOTION: '채널/앱 프로모션',
  OTHER: '기타',
}

const sortLabels = {
  HIGH_SCORE: '점수 높은 순',
  LOW_EFFORT: '운영 쉬운 순',
  BRAND_FIT: '브랜드 적합도 높은 순',
}

const combinations = [
  {
    id: 1,
    isSample: true,
    goal: 'UPSELL',
    methods: ['MEMBERSHIP_LOYALTY', 'COUPON_DISCOUNT'],
    benefitIds: [1],
    title: '갤러리아 VIP 프리미엄 리프레시',
    grade: '최우선 추천',
    partner: '스타벅스 코리아',
    asset: '갤러리아 VIP 고객층, 앱 배너',
    offer: '리저브 사이즈업 쿠폰, 전용 굿즈',
    target: 'VIP App 활성 고객 약 5만 명',
    channels: '갤러리아 앱, 알림톡, 스타벅스 앱',
    outputs: '랜딩 페이지, 알림톡 문구, 쿠폰 난수',
    schedule: '기획 2주 / 운영 3주',
    risk: '쿠폰 소진 속도 제한 필요',
    riskCount: 1,
    score: 94,
    reasons: [
      'VIP 고객층과 스타벅스 프리미엄 타겟이 잘 맞습니다.',
      '양사 앱 동시 노출로 도달 효율이 높습니다.',
      '쿠폰 소진 속도 제한이 필요합니다.',
    ],
    scoreBreakdown: [
      { label: '고객 적합도', score: 95, weight: 25 },
      { label: '수익 기여도', score: 92, weight: 25 },
      { label: '비용 효율성', score: 90, weight: 20 },
      { label: '운영 용이성', score: 95, weight: 15 },
      { label: '브랜드 적합도', score: 96, weight: 15 },
    ],
    detailCards: [
      { label: '보유 자산', value: '갤러리아 VIP 고객층, 앱 배너', meta: 'VIP App 활성 고객 약 5만 명 · 자체 채널 노출 가치 높음' },
      { label: '파트너 혜택', value: '리저브 사이즈업 쿠폰, 전용 굿즈', meta: '쿠폰 1만 장 · 굿즈 제작비 파트너 부담 가정' },
      { label: '채널', value: '갤러리아 앱, 알림톡, 스타벅스 앱', meta: '앱 배너 2주 · 알림톡 2회 · 파트너 앱 동시 노출' },
      { label: '산출물', value: '랜딩 페이지, 알림톡 문구, 쿠폰 난수', meta: '디자인팀 05.05 · 콘텐츠팀 05.02 · 운영팀 쿠폰 세팅' },
      { label: '일정', value: '기획 2주 / 운영 3주', meta: '2026.05.06 시작 · 05.13 랜딩 확정 · 05.29 종료' },
      { label: '리스크', value: '쿠폰 소진 속도 제한 필요', meta: '영향도 중 · 일별 사용량 제한과 조기 소진 알림으로 대응' },
    ],
    targetKpis: ['VIP App 활성 고객 5만 명 도달', '가입 전환율 7% 이상', '쿠폰 사용률 60% 이상'],
    operationMeta: {
      period: '2026.05.06 ~ 2026.05.29',
      owner: '김캘리',
      partnerContact: '박스벅',
      budget: '0 / 5,000만 원',
    },
    effortScore: 78,
    brandScore: 92,
  },
  {
    id: 2,
    isSample: true,
    goal: 'PURCHASE_BOOKING',
    methods: ['TRIAL_GIFT', 'JOINT_PROMOTION'],
    benefitIds: [2],
    title: '호텔앤드리조트 액티브 스테이',
    grade: '우선 검토',
    partner: '나이키 코리아',
    asset: '객실 패키지, 리조트 이용권',
    offer: 'NRC 챌린지, 리워드 굿즈',
    target: '2030 액티브 레저 고객',
    channels: '리조트 공홈, 나이키 NRC 앱',
    outputs: '패키지 페이지, SNS 소재 3종',
    schedule: '기획 3주 / 운영 1개월',
    risk: '클래스 일정 확정 필요',
    riskCount: 1,
    score: 88,
    reasons: [
      '2030 액티브 레저 고객과 파트너 이용층이 겹칩니다.',
      '패키지 예약과 앱 챌린지를 함께 유도할 수 있습니다.',
      '오프라인 클래스 일정 확정이 필요합니다.',
    ],
    scoreBreakdown: [
      { label: '고객 적합도', score: 90, weight: 25 },
      { label: '수익 기여도', score: 86, weight: 25 },
      { label: '비용 효율성', score: 84, weight: 20 },
      { label: '운영 용이성', score: 82, weight: 15 },
      { label: '브랜드 적합도', score: 88, weight: 15 },
    ],
    detailCards: [
      { label: '보유 자산', value: '객실 패키지, 리조트 이용권', meta: '주말 객실 패키지 200실 · 리조트 이용권 1,000매' },
      { label: '파트너 혜택', value: 'NRC 챌린지, 리워드 굿즈', meta: '챌린지 참여 리워드 · 굿즈 3,000개 가정' },
      { label: '채널', value: '리조트 공홈, 나이키 NRC 앱', meta: '공홈 패키지 페이지 · 파트너 앱 챌린지 배너' },
      { label: '산출물', value: '패키지 페이지, SNS 소재 3종', meta: '랜딩 1식 · SNS 카드 3종 · 클래스 안내 문구' },
      { label: '일정', value: '기획 3주 / 운영 1개월', meta: '클래스 확정 후 4주 운영 · 우천 대체안 필요' },
      { label: '리스크', value: '클래스 일정 확정 필요', meta: '영향도 중 · 강사/장소 확정 전 예약 오픈 제한' },
    ],
    targetKpis: ['패키지 예약 300건', '클래스 참여 1,000명', 'NRC 앱 연계 참여율 20%'],
    operationMeta: {
      period: '2026.05.06 ~ 2026.06.05',
      owner: '김캘리',
      partnerContact: '나이키 제휴 담당',
      budget: '0 / 3,000만 원',
    },
    effortScore: 82,
    brandScore: 86,
  },
  {
    id: 3,
    isSample: true,
    goal: 'MEMBER_SIGNUP',
    methods: ['CHANNEL_APP_PROMOTION', 'COUPON_DISCOUNT'],
    benefitIds: [3],
    title: '앱 신규 가입 시네마 베네핏',
    grade: '우선 검토',
    partner: 'CGV',
    asset: '앱 가입 온보딩, 신규 쿠폰함',
    offer: '프리미엄 상영관 1+1 예매권',
    target: '앱 미가입 기존 구매 고객',
    channels: '앱 배너, 푸시, 문자',
    outputs: '가입 배너, 쿠폰 유의사항',
    schedule: '기획 1주 / 운영 2주',
    risk: '예매권 조건 문구 검수',
    riskCount: 1,
    score: 82,
    reasons: [
      '앱 미가입 고객에게 가입 이유를 직접 제공합니다.',
      '쿠폰함과 푸시 채널로 전환 경로가 짧습니다.',
      '예매권 조건 문구 검수가 필요합니다.',
    ],
    scoreBreakdown: [
      { label: '고객 적합도', score: 84, weight: 25 },
      { label: '수익 기여도', score: 82, weight: 25 },
      { label: '비용 효율성', score: 80, weight: 20 },
      { label: '운영 용이성', score: 90, weight: 15 },
      { label: '브랜드 적합도', score: 76, weight: 15 },
    ],
    detailCards: [
      { label: '보유 자산', value: '앱 가입 온보딩, 신규 쿠폰함', meta: '미가입 고객 세그먼트 · 온보딩 첫 화면 노출' },
      { label: '파트너 혜택', value: '프리미엄 상영관 1+1 예매권', meta: '예매권 5,000매 · 조건부 사용' },
      { label: '채널', value: '앱 배너, 푸시, 문자', meta: '앱 배너 2주 · 푸시 1회 · 문자 1회' },
      { label: '산출물', value: '가입 배너, 쿠폰 유의사항', meta: '배너 2종 · 유의사항 문안 · 쿠폰 등록 QA' },
      { label: '일정', value: '기획 1주 / 운영 2주', meta: '짧은 운영 가능 · 검수 완료 후 즉시 오픈' },
      { label: '리스크', value: '예매권 조건 문구 검수', meta: '영향도 낮음 · 사용 조건을 명확히 고지' },
    ],
    targetKpis: ['신규 회원 가입 30,000건', 'D7 잔존율 18%', '쿠폰 등록률 45%'],
    operationMeta: {
      period: '2026.05.06 ~ 2026.05.20',
      owner: '김캘리',
      partnerContact: 'CGV 제휴 담당',
      budget: '0 / 2,000만 원',
    },
    effortScore: 90,
    brandScore: 80,
  },
]

const criteriaSummary = computed(() => {
  const criteria = props.recommendationCriteria ?? {}

  return {
    goal: goals.find((goal) => goal.id === selectedGoal.value)?.name ?? '-',
    methods: (criteria.campaignMethods ?? []).map((method) => campaignMethodLabels[method] ?? method),
    benefitCount: criteria.benefitIds?.length ?? 0,
    sort: sortLabels[criteria.sortType] ?? sortLabels.HIGH_SCORE,
  }
})

const visibleCombinations = computed(() => {
  const criteria = props.recommendationCriteria ?? {}
  const methodFilters = Array.isArray(criteria.campaignMethods) ? criteria.campaignMethods : []
  const benefitFilters = Array.isArray(criteria.benefitIds) ? criteria.benefitIds : []

  const filtered = combinations.filter((combo) => {
    if (combo.goal !== selectedGoal.value) return false
    if (methodFilters.length && !methodFilters.some((method) => combo.methods?.includes(method))) return false
    if (benefitFilters.length && !benefitFilters.some((benefitId) => combo.benefitIds?.includes(benefitId))) return false
    return true
  })

  const results = filtered.length ? filtered : combinations
  const sortType = criteria.sortType ?? 'HIGH_SCORE'

  return [...results].sort((a, b) => {
    if (sortType === 'LOW_EFFORT') return (b.effortScore ?? 0) - (a.effortScore ?? 0)
    if (sortType === 'BRAND_FIT') return (b.brandScore ?? 0) - (a.brandScore ?? 0)
    return (b.score ?? 0) - (a.score ?? 0)
  })
})

const isFallbackRecommendation = computed(() => {
  return visibleCombinations.value.some((combo) => combo.goal === selectedGoal.value) === false
})

const drawerCombo = computed(
  () => visibleCombinations.value.find((combo) => combo.id === drawerComboId.value) ?? null,
)

const checkedCount = computed(() => {
  return visibleCombinations.value.filter((combo) => checkedComboIds.value.has(combo.id)).length
})

function selectGoal(goalId) {
  selectedGoal.value = goalId
  drawerComboId.value = null
  checkedComboIds.value = new Set()
}

watch(
  () => props.recommendationCriteria,
  (criteria) => {
    if (!criteria?.goalType) return
    selectedGoal.value = criteria.goalType
    drawerComboId.value = null
    checkedComboIds.value = new Set()
  },
  { immediate: true },
)

function scoreTone(score) {
  if (score >= 90) return 'match-tone--strong'
  if (score >= 80) return 'match-tone--info'
  return 'match-tone--warning'
}

const radarCenter = 120
const radarRadius = 76

function radarPoint(index, score = 100, radius = radarRadius) {
  const angle = -Math.PI / 2 + (Math.PI * 2 * index) / 5
  const distance = radius * (score / 100)
  return {
    x: radarCenter + Math.cos(angle) * distance,
    y: radarCenter + Math.sin(angle) * distance,
  }
}

function radarPolygon(items, scoreKey = 'score') {
  return (items ?? [])
    .map((item, index) => {
      const point = radarPoint(index, item?.[scoreKey] ?? 100)
      return point.x + ',' + point.y
    })
    .join(' ')
}

function radarGridPolygon(percent) {
  return Array.from({ length: 5 }, (_, index) => {
    const point = radarPoint(index, percent, radarRadius)
    return point.x + ',' + point.y
  }).join(' ')
}

function radarLabelPoint(index) {
  return radarPoint(index, 118, radarRadius)
}

function openDrawer(comboId) {
  drawerComboId.value = comboId
}

function closeDrawer() {
  drawerComboId.value = null
}

function toggleCheck(comboId, event) {
  event.stopPropagation()
  if (checkedComboIds.value.has(comboId)) checkedComboIds.value.delete(comboId)
  else checkedComboIds.value.add(comboId)
  checkedComboIds.value = new Set(checkedComboIds.value)
}

function showToast(message, action = null) {
  const id = Date.now()
  toastMessage.value = { message, action, id }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    if (toastMessage.value?.id === id) toastMessage.value = null
  }, 4000)
}

function dismissToast() {
  if (toastTimer) clearTimeout(toastTimer)
  toastMessage.value = null
}

function viewEvaluation(combo) {
  emit('request-evaluation', combo)
}

function markEvaluated(combo) {
  if (!combo) return
  evaluatedComboIds.value.add(combo.id)
  evaluatedComboIds.value = new Set(evaluatedComboIds.value)
}

function sendToEvaluation(combo, event) {
  event?.stopPropagation()
  if (!combo) return
  markEvaluated(combo)
  showToast(`'${combo.title}' 을(를) 평가로 보냈습니다.`, {
    label: '평가 탭에서 보기',
    onClick: () => viewEvaluation(combo),
  })
}

function sendCheckedToEvaluation() {
  const targets = visibleCombinations.value.filter((combo) => checkedComboIds.value.has(combo.id))
  if (!targets.length) return
  targets.forEach(markEvaluated)
  showToast(`${targets.length}건을 평가로 보냈습니다.`, {
    label: '평가 탭에서 보기',
    onClick: () => viewEvaluation(targets[0]),
  })
  checkedComboIds.value = new Set()
}

function clearChecked() {
  checkedComboIds.value = new Set()
}

function isEvaluated(comboId) {
  return evaluatedComboIds.value.has(comboId)
}

function isChecked(comboId) {
  return checkedComboIds.value.has(comboId)
}

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const operationTasks = [
  { title: '랜딩/배너 제작', owner: '이디자인', due: '05.05', status: '실행 준비', priority: '높음', output: '랜딩 1식, 배너 2종', checklist: '0/3', dday: 'D-2' },
  { title: '푸시 문구 생성', owner: '김콘텐츠', due: '05.02', status: '진행 중', priority: '보통', output: '푸시 문구 3안', checklist: '1/3', dday: 'D+1' },
  { title: '유의사항 검수', owner: '최법무', due: '05.08', status: '검수 대기', priority: '높음', output: '고지 문안', checklist: '0/2', dday: 'D-5' },
]

const operationColumns = ['실행 준비', '진행 중', '검수 대기', '완료']
const completedTaskCount = computed(() => operationTasks.filter((task) => task.status === '완료').length)
const operationProgress = computed(() => Math.round((completedTaskCount.value / operationTasks.length) * 100))

function tasksByStatus(status) {
  return operationTasks.filter((task) => task.status === status)
}
</script>

<template>
  <section class="matching-workspace" :class="{ 'matching-workspace--collapsed': isSidebarCollapsed }">
    <aside class="match-sidebar">
      <button
        type="button"
        class="match-sidebar__toggle"
        :aria-label="isSidebarCollapsed ? '목표 패널 펼치기' : '목표 패널 접기'"
        @click="toggleSidebar"
      >
        <svg viewBox="0 0 24 24" width="14" height="14">
          <path
            v-if="!isSidebarCollapsed"
            d="M15 18l-6-6 6-6"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            v-else
            d="M9 18l6-6-6-6"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>

      <div v-if="!isSidebarCollapsed" class="match-sidebar__inner">
        <div class="match-sidebar__head">
          <h3>목표</h3>
          <span>{{ goals.length }}</span>
        </div>

        <button
          v-for="goal in goals"
          :key="goal.id"
          type="button"
          class="match-goal"
          :class="{ 'match-goal--active': selectedGoal === goal.id }"
          @click="selectGoal(goal.id)"
        >
          <strong>{{ goal.name }}</strong>
          <span>{{ goal.count }}건</span>
        </button>
      </div>
    </aside>

    <div class="match-main">
      <header class="match-main__head">
        <div class="match-main__title">
          <h3>추천 조합</h3>
          <span class="match-main__count">{{ visibleCombinations.length }}건</span>
        </div>
      </header>

      <p v-if="isFallbackRecommendation" class="match-fallback">
        선택 조건과 정확히 일치하는 예시가 없어 전체 추천 후보를 보여줍니다.
      </p>

      <div v-if="checkedCount > 0" class="match-batchbar">
        <span><b>{{ checkedCount }}</b>건 선택됨</span>
        <div class="match-batchbar__actions">
          <button type="button" class="match-batchbar__ghost" @click="clearChecked">선택 해제</button>
          <button type="button" class="match-batchbar__primary" @click="sendCheckedToEvaluation">
            선택한 {{ checkedCount }}건 평가로 보내기
          </button>
        </div>
      </div>

      <div class="match-listview">
        <article
          v-for="combo in visibleCombinations"
          :key="combo.id"
          class="match-list-row"
          :class="{
            'match-list-row--active': drawerComboId === combo.id,
            'match-list-row--evaluated': isEvaluated(combo.id),
            'match-list-row--checked': isChecked(combo.id),
          }"
          tabindex="0"
          role="button"
          @click="openDrawer(combo.id)"
          @keydown.enter="openDrawer(combo.id)"
        >
          <label class="match-list-row__check" @click.stop>
            <input type="checkbox" :checked="isChecked(combo.id)" @change="toggleCheck(combo.id, $event)" />
            <span></span>
          </label>

          <div class="match-list-row__score">
            <strong :class="scoreTone(combo.score)">{{ combo.score }}</strong>
            <span :class="scoreTone(combo.score)">{{ combo.grade }}</span>
          </div>

          <div class="match-list-row__main">
            <div class="match-list-row__titleline">
              <h4>{{ combo.title }}</h4>
              <span v-if="combo.isSample" class="sample-badge">샘플</span>
              <small>{{ combo.partner }}</small>
              <em v-if="isEvaluated(combo.id)">평가 진행 중</em>
            </div>
            <p>{{ combo.reasons[0] }}</p>
            <dl>
              <div>
                <dt>자산</dt>
                <dd>{{ combo.asset }}</dd>
              </div>
              <div>
                <dt>혜택</dt>
                <dd>{{ combo.offer }}</dd>
              </div>
              <div>
                <dt>리스크</dt>
                <dd>{{ combo.riskCount }}건 · {{ combo.risk }}</dd>
              </div>
            </dl>
          </div>

          <div class="match-list-row__actions" @click.stop>
            <button type="button" class="match-list-row__icon" aria-label="상세보기" @click="openDrawer(combo.id)">→</button>
            <button v-if="!isEvaluated(combo.id)" type="button" class="match-list-row__primary" @click="sendToEvaluation(combo, $event)">
              평가로 보내기
            </button>
            <button v-else type="button" class="match-list-row__success" @click="viewEvaluation(combo)">
              평가 보기
            </button>
          </div>
        </article>
      </div>
    </div>

    <transition name="drawer">
      <aside v-if="drawerCombo" class="match-drawer">
        <header class="match-drawer__head">
          <div class="match-drawer__title">
            <strong class="match-drawer__score" :class="scoreTone(drawerCombo.score)">{{ drawerCombo.score }}점</strong>
            <span class="match-drawer__grade" :class="scoreTone(drawerCombo.score)">{{ drawerCombo.grade }}</span>
          </div>
          <button type="button" class="match-drawer__close" aria-label="상세 닫기" @click="closeDrawer">×</button>
        </header>

        <div class="match-drawer__hero">
          <div class="match-drawer__titleline">
            <h3>{{ drawerCombo.title }}</h3>
            <span v-if="drawerCombo.isSample" class="sample-badge">샘플</span>
          </div>
          <p>{{ drawerCombo.partner }} · {{ drawerCombo.target }}</p>
        </div>

        <ul class="match-reasons">
          <li class="primary">✓ {{ drawerCombo.reasons[0] }}</li>
          <li>✓ {{ drawerCombo.reasons[1] }}</li>
          <li class="risk">! {{ drawerCombo.reasons[2] }}</li>
        </ul>

                <section class="match-drawer__section">
          <h4>점수 근거</h4>
          <div class="match-radar">
            <svg class="match-radar__chart" viewBox="0 0 240 240" role="img" aria-label="점수 근거 레이더 차트">
              <polygon class="match-radar__grid" :points="radarGridPolygon(100)" />
              <polygon class="match-radar__grid match-radar__grid--inner" :points="radarGridPolygon(70)" />
              <line
                v-for="(item, index) in drawerCombo.scoreBreakdown"
                :key="item.label + '-axis'"
                class="match-radar__axis"
                :x1="radarCenter"
                :y1="radarCenter"
                :x2="radarPoint(index).x"
                :y2="radarPoint(index).y"
              />
              <polygon class="match-radar__area" :points="radarPolygon(drawerCombo.scoreBreakdown)" />
              <circle
                v-for="(item, index) in drawerCombo.scoreBreakdown"
                :key="item.label + '-dot'"
                class="match-radar__dot"
                :cx="radarPoint(index, item.score).x"
                :cy="radarPoint(index, item.score).y"
                r="4"
              />
              <text
                v-for="(item, index) in drawerCombo.scoreBreakdown"
                :key="item.label + '-label'"
                class="match-radar__label"
                :x="radarLabelPoint(index).x"
                :y="radarLabelPoint(index).y"
                text-anchor="middle"
              >
                {{ item.label.replace('도', '') }} {{ item.score }}
              </text>
            </svg>
            <dl class="match-radar__legend">
              <div v-for="item in drawerCombo.scoreBreakdown" :key="item.label">
                <dt>{{ item.label }}</dt>
                <dd>{{ item.score }}점 · {{ item.weight }}%</dd>
              </div>
            </dl>
          </div>
        </section>
        <section class="match-drawer__section">
          <h4>목표 KPI</h4>
          <ul class="match-kpis match-kpis--ranked">
            <li v-for="(kpi, index) in drawerCombo.targetKpis" :key="kpi">
              <span>{{ index === 0 ? '핵심 KPI' : '보조 KPI' }}</span>
              <strong>{{ kpi }}</strong>
            </li>
          </ul>
        </section>

        <section class="match-drawer__section">
          <h4>조합 구성</h4>
          <dl class="match-detail-grid">
            <div
              v-for="card in drawerCombo.detailCards"
              :key="card.label"
              class="match-detail-card"
              :class="{ 'match-detail-card--risk': card.label === '리스크' }"
            >
              <dt>
                <span class="match-detail-card__icon">{{ card.label === '보유 자산' ? '◇' : card.label === '파트너 혜택' ? '＋' : card.label === '채널' ? '⌁' : card.label === '산출물' ? '□' : card.label === '일정' ? '◷' : '!' }}</span>
                {{ card.label }}
              </dt>
              <dd>
                <strong>{{ card.value }}</strong>
                <small>
                  <span v-for="part in card.meta.split(' · ')" :key="part">{{ part }}</span>
                </small>
              </dd>
            </div>
          </dl>
        </section>

        <footer class="match-drawer__foot">
          <button v-if="!isEvaluated(drawerCombo.id)" type="button" class="match-drawer__primary" @click="sendToEvaluation(drawerCombo, $event)">
            평가로 보내기
          </button>
          <button v-else type="button" class="match-drawer__success" @click="viewEvaluation(drawerCombo)">
            평가 탭에서 보기 →
          </button>
        </footer>
      </aside>
    </transition>

    <transition name="toast">
      <div v-if="toastMessage" class="match-toast" role="status">
        <span class="match-toast__icon">✓</span>
        <span class="match-toast__msg">{{ toastMessage.message }}</span>
        <button v-if="toastMessage.action" type="button" class="match-toast__action" @click="toastMessage.action.onClick(); dismissToast()">
          {{ toastMessage.action.label }}
        </button>
        <button type="button" class="match-toast__close" aria-label="알림 닫기" @click="dismissToast">×</button>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.matching-workspace {
  display: grid;
  grid-template-columns: minmax(220px, 0.48fr) minmax(320px, 0.62fr) minmax(520px, 1.55fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.match-panel {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  padding: 0.8rem;
  box-shadow: 0 6px 18px rgba(19, 35, 68, 0.04);
}

.match-goals,
.match-list {
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--panel-muted) 86%, var(--accent-soft)),
      var(--panel-muted)
    );
  border-color: color-mix(in srgb, var(--border-strong) 72%, var(--accent-color));
  box-shadow: inset -1px 0 0 color-mix(in srgb, var(--border-color) 72%, transparent);
}

.match-detail {
  background: var(--panel-color);
  border-color: var(--border-strong);
}

.match-empty {
  align-content: center;
  justify-items: center;
  min-height: 18rem;
  text-align: center;
}

.match-empty strong {
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 900;
}

.match-empty p {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.76rem;
  font-weight: 750;
}

.match-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}

.match-panel__head h3 {
  color: var(--text-primary);
  font-size: 0.95rem;
  line-height: 1.1;
}

.match-panel__head span {
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.match-goals,
.match-list,
.match-detail {
  display: grid;
  align-content: start;
  gap: 0.5rem;
}

.match-goal,
.match-row {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: color-mix(in srgb, var(--panel-color) 66%, var(--panel-muted));
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
}

.match-goal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  min-height: 2.65rem;
  padding: 0.58rem 0.65rem;
}

.match-goal strong,
.match-row__main strong,
.match-detail__hero strong {
  color: var(--text-primary);
  font-size: 0.9rem;
}

.match-goal span,
.match-row__main small,
.match-row__main em,
.match-detail__hero p {
  color: var(--muted-text);
  font-size: 0.74rem;
}

.match-fallback {
  margin: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  color: var(--muted-text);
  padding: 0.55rem 0.65rem;
  font-size: 0.7rem;
  font-weight: 750;
}

.match-criteria {
  display: grid;
  gap: 0.32rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.6rem 0.65rem;
}

.match-criteria span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.7rem;
  font-weight: 780;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-goal--active,
.match-row--active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-strong));
  background: color-mix(in srgb, var(--accent-color) 11%, var(--panel-color));
  box-shadow:
    0 5px 14px rgba(19, 35, 68, 0.06),
    inset 3px 0 0 var(--accent-color);
}

.match-row {
  display: grid;
  grid-template-columns: 2.7rem minmax(0, 1fr);
  align-items: center;
  gap: 0.65rem;
  min-height: 5.2rem;
  padding: 0.65rem;
}

.match-row__main {
  display: grid;
  min-width: 0;
  gap: 0.12rem;
}

.match-row__main strong,
.match-row__main small,
.match-row__main em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-row__main em {
  font-size: 0.68rem;
  font-style: normal;
  font-weight: 700;
}

.match-score {
  display: grid;
  width: 2.45rem;
  height: 2.45rem;
  place-items: center;
  border-radius: 7px;
  font-size: 0.9rem;
  font-weight: 900;
}

.match-badge {
  display: inline-flex;
  grid-column: 2;
  justify-self: start;
  min-height: 1.55rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 0.55rem;
  font-size: 0.68rem;
  font-weight: 900;
  white-space: nowrap;
}

.match-tone--strong {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
}

.match-tone--info {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.match-tone--warning {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.match-detail__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.65rem;
}

.match-detail__hero div {
  display: grid;
  gap: 0.15rem;
  min-width: 0;
}

.match-detail-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.3rem;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-muted);
  padding: 0.28rem;
}

.match-detail-switch button {
  min-height: 2.15rem;
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 900;
  cursor: pointer;
}

.match-detail-switch button.active {
  background: var(--panel-color);
  color: var(--accent-color);
  box-shadow:
    0 4px 12px rgba(19, 35, 68, 0.06),
    inset 0 -2px 0 var(--accent-color);
}

.match-detail-switch button.disabled {
  cursor: not-allowed;
  color: var(--subtle-text);
  opacity: 0.62;
}

.match-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin: 0;
}

.match-detail__grid div {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.58rem 0.65rem;
}

.match-detail__grid dt {
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 800;
}

.match-detail__grid dd {
  display: grid;
  gap: 0.18rem;
  margin: 0.16rem 0 0;
}

.match-detail__grid dd strong {
  color: var(--text-primary);
  font-size: 0.78rem;
  line-height: 1.35;
}

.match-detail__grid dd small {
  color: var(--muted-text);
  font-size: 0.7rem;
  line-height: 1.38;
}

.match-reasons,
.target-kpis ul {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.match-reasons {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-color));
  padding: 0.65rem 0.75rem;
}

.match-reasons li {
  color: var(--text-secondary);
  font-size: 0.76rem;
  font-weight: 800;
  line-height: 1.42;
}

.match-reasons li.risk {
  color: var(--color-warning-dark, #b45309);
}

.match-reasons--preview li.primary {
  color: var(--text-primary);
  font-size: 0.82rem;
  font-weight: 900;
}

.match-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin: 0;
}

.match-preview-grid div,
.match-compare {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.6rem 0.65rem;
}

.match-preview-grid dt,
.match-compare h4 {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 900;
}

.match-preview-grid dd {
  overflow: hidden;
  margin: 0.18rem 0 0;
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 850;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-compare {
  display: grid;
  gap: 0.45rem;
}

.match-compare__row {
  display: grid;
  grid-template-columns: minmax(7rem, 0.9fr) minmax(0, 1fr) 2rem;
  align-items: center;
  gap: 0.45rem;
}

.match-compare__row span {
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-compare__row div {
  height: 0.42rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-color);
}

.match-compare__row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.match-compare__row strong {
  color: var(--text-primary);
  font-size: 0.76rem;
  text-align: right;
}

.score-breakdown {
  display: grid;
  gap: 0.42rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.65rem;
}

.score-breakdown__row {
  display: grid;
  grid-template-columns: 7.2rem minmax(0, 1fr) 2rem;
  align-items: center;
  gap: 0.5rem;
}

.score-breakdown__row span {
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 850;
}

.score-breakdown__row small {
  color: var(--muted-text);
  font-size: 0.62rem;
}

.score-breakdown__row div {
  height: 0.42rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-color);
}

.score-breakdown__row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.score-breakdown__row strong {
  color: var(--text-primary);
  font-size: 0.76rem;
}

.target-kpis {
  display: grid;
  gap: 0.45rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.65rem;
}

.target-kpis h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.82rem;
}

.target-kpis li {
  color: var(--text-secondary);
  font-size: 0.74rem;
  font-weight: 780;
}

.target-kpis li::before {
  content: '•';
  margin-right: 0.4rem;
  color: var(--accent-color);
}

.match-primary {
  min-height: 2.35rem;
  border-radius: 7px;
  background: var(--accent-color);
  color: #fff;
  font-size: 0.82rem;
  font-weight: 800;
}

.operation-inline {
  display: grid;
  gap: 0.6rem;
}

.operation-inline__head,
.operation-inline__meta div,
.operation-progress,
.operation-column {
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.62rem;
}

.operation-inline__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.operation-inline__head > div {
  display: grid;
  gap: 0.16rem;
}

.operation-inline__head button,
.operation-actions button {
  min-height: 1.9rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-color);
  color: var(--text-secondary);
  padding: 0 0.65rem;
  font-size: 0.7rem;
  font-weight: 900;
}

.operation-inline__head strong,
.operation-inline__meta strong,
.operation-card strong {
  color: var(--text-primary);
  font-size: 0.84rem;
}

.operation-inline__head span,
.operation-inline__meta span,
.operation-card p,
.operation-card small {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 800;
}

.operation-inline__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
}

.operation-progress {
  display: grid;
  gap: 0.4rem;
}

.operation-progress > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 0.7rem;
}

.operation-progress span,
.operation-progress p {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 800;
}

.operation-progress strong {
  color: var(--accent-color);
  font-size: 0.82rem;
}

.operation-progress__bar {
  height: 0.45rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-color);
}

.operation-progress__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--accent-color);
}

.operation-board {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.5rem;
}

.operation-column {
  display: grid;
  align-content: start;
  gap: 0.45rem;
}

.operation-column h4 {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
  margin: 0;
  color: var(--text-primary);
  font-size: 0.78rem;
}

.operation-column h4 span {
  color: var(--accent-color);
}

.operation-card {
  display: grid;
  gap: 0.25rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-color);
  padding: 0.55rem;
}

.operation-card > div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.45rem;
}

.operation-card em {
  flex: 0 0 auto;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
  color: var(--accent-color);
  padding: 0.12rem 0.42rem;
  font-size: 0.62rem;
  font-style: normal;
  font-weight: 900;
}

.operation-card em.late {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.operation-card p,
.operation-card small {
  margin: 0;
}

.operation-card footer,
.operation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.operation-card footer span {
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  padding: 0.12rem 0.42rem;
  font-size: 0.62rem;
  font-weight: 850;
}

.operation-empty {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 750;
}

@media (max-width: 1180px) {
  .matching-workspace {
    grid-template-columns: 1fr;
  }

  .operation-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}


/* Card-grid recommendation layout override */
.matching-workspace {
  position: relative;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 0.7rem;
  height: 100%;
  min-height: 0;
}

.matching-workspace--collapsed {
  grid-template-columns: 36px minmax(0, 1fr);
}

.match-sidebar {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.7rem 0.6rem;
}

.match-sidebar__toggle {
  position: absolute;
  top: 0.5rem;
  right: 0.4rem;
  z-index: 2;
  display: grid;
  width: 1.4rem;
  height: 1.4rem;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: 50%;
  background: var(--panel-color);
  color: var(--muted-text);
  cursor: pointer;
}

.match-sidebar__inner {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  overflow-y: auto;
  padding-right: 0.2rem;
}

.match-sidebar__head,
.match-main__head,
.match-main__title,
.match-batchbar,
.match-card__head,
.match-card__foot,
.match-drawer__head,
.match-drawer__title,
.match-toast {
  display: flex;
  align-items: center;
}

.match-sidebar__head,
.match-main__head,
.match-batchbar,
.match-card__foot,
.match-drawer__head {
  justify-content: space-between;
}

.match-sidebar__head {
  margin-bottom: 0.3rem;
  border-bottom: 1px solid var(--border-color);
  padding: 0 0.2rem 0.5rem;
}

.match-sidebar__head h3,
.match-main__title h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 900;
}

.match-sidebar__head span {
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 800;
}

.match-main {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: 0.65rem;
  overflow-y: auto;
  padding: 0.05rem;
}

.match-main__head {
  flex-wrap: wrap;
  gap: 1rem;
}

.match-main__title {
  gap: 0.5rem;
}

.match-main__count {
  display: inline-flex;
  min-width: 1.4rem;
  height: 1.4rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 0.7rem;
  font-weight: 900;
  padding: 0 0.45rem;
}

.match-criteria {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem 0.7rem;
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 700;
}

.match-criteria b {
  margin-right: 0.25rem;
  color: var(--muted-text);
  font-weight: 900;
}

.match-batchbar {
  gap: 0.5rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  border-radius: 7px;
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
  padding: 0.5rem 0.75rem;
}

.match-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 0.65rem;
}

.match-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  background: var(--panel-color);
  padding: 0.85rem 0.85rem 0.7rem;
  cursor: pointer;
  outline: none;
}

.match-card:hover,
.match-card--active,
.match-card--checked {
  border-color: var(--accent-color);
  box-shadow: 0 6px 16px rgba(19, 35, 68, 0.06);
}

.match-card--evaluated {
  background: color-mix(in srgb, var(--accent-color) 4%, var(--panel-color));
}

.match-card--checked {
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-color));
}

.match-card__check {
  position: absolute;
  top: 0.55rem;
  left: 0.55rem;
  cursor: pointer;
}

.match-card__check input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.match-card__check span {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  border: 1.5px solid var(--border-color);
  border-radius: 4px;
  background: var(--panel-color);
}

.match-card__check input:checked + span {
  border-color: var(--accent-color);
  background: var(--accent-color);
}

.match-card__status {
  position: absolute;
  top: 0.55rem;
  right: 0.55rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 35%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 14%, var(--panel-color));
  color: var(--accent-color);
  padding: 0.16rem 0.5rem;
  font-size: 0.62rem;
  font-weight: 900;
}

.match-card__head {
  gap: 0.5rem;
  padding-left: 1.6rem;
}

.match-card--evaluated .match-card__head {
  padding-right: 5.5rem;
}

.match-card__score {
  background: transparent !important;
  font-size: 1.55rem;
  font-weight: 900;
  line-height: 1;
}

.match-card__grade,
.match-drawer__grade {
  display: inline-flex;
  height: 1.3rem;
  align-items: center;
  border-radius: 999px;
  padding: 0 0.5rem;
  font-size: 0.66rem;
  font-weight: 900;
}

.match-card__title {
  margin: 0;
  color: var(--text-primary);
  font-size: 0.92rem;
  font-weight: 900;
  line-height: 1.3;
}

.match-card__partner {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.match-card__meta {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  border-top: 1px solid var(--border-color);
  padding-top: 0.55rem;
}

.match-card__meta div {
  display: grid;
  grid-template-columns: 4.4rem minmax(0, 1fr);
  gap: 0.4rem;
}

.match-card__meta dt {
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 900;
}

.match-card__meta dd {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1.4;
}

.match-card__actions,
.match-batchbar__actions {
  display: flex;
  gap: 0.35rem;
}

.match-card__btn-ghost,
.match-card__btn-primary,
.match-card__btn-success,
.match-batchbar__ghost,
.match-batchbar__primary {
  border-radius: 6px;
  padding: 0.38rem 0.68rem;
  font-size: 0.7rem;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.match-card__btn-ghost,
.match-batchbar__ghost {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.match-card__btn-primary,
.match-batchbar__primary,
.match-drawer__primary {
  border: 0;
  background: var(--accent-color);
  color: #fff;
}

.match-card__btn-success,
.match-drawer__success {
  border: 1px solid color-mix(in srgb, var(--accent-color) 40%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 14%, var(--panel-color));
  color: var(--accent-color);
}

.match-drawer {
  position: absolute;
  inset: 0 0 0 auto;
  z-index: 10;
  display: flex;
  width: min(440px, 95%);
  flex-direction: column;
  overflow: hidden;
  border-left: 1px solid var(--border-strong);
  background: var(--panel-color);
  box-shadow: -8px 0 24px rgba(19, 35, 68, 0.08);
}

.match-drawer__head {
  gap: 0.5rem;
  border-bottom: 1px solid var(--border-color);
  padding: 0.85rem 1rem;
}

.match-drawer__title {
  gap: 0.55rem;
}

.match-drawer__score {
  background: transparent !important;
  font-size: 1.35rem;
  font-weight: 900;
}

.match-drawer__close {
  width: 1.8rem;
  height: 1.8rem;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 900;
}

.match-drawer__hero,
.match-drawer__section,

.match-kpis--ranked {
  gap: 0.45rem;
}

.match-kpis--ranked li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.45rem;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  background: var(--panel-muted);
  padding: 0.52rem 0.65rem;
}

.match-kpis--ranked strong {
  grid-column: 1;
  grid-row: 1;
  color: var(--text-primary);
  font-size: 0.78rem;
  font-weight: 900;
  line-height: 1.35;
}

.match-kpis--ranked span {
  grid-column: 2;
  grid-row: 1;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-color));
  color: var(--accent-color);
  padding: 0.16rem 0.48rem;
  font-size: 0.62rem;
  font-weight: 900;
  white-space: nowrap;
}

.match-detail-card {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  min-height: 8.2rem;
}

.match-detail-card dt {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.match-detail-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.15rem;
  height: 1.15rem;
  border-radius: 5px;
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-color));
  color: var(--accent-color);
  font-size: 0.68rem;
  font-weight: 900;
}

.match-detail-card dd {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 0.48rem;
  min-width: 0;
}

.match-detail-card dd strong {
  display: block;
  color: var(--text-primary);
  font-size: 0.83rem;
  font-weight: 900;
  line-height: 1.35;
  word-break: keep-all;
}

.match-detail-card dd small {
  display: grid;
  gap: 0.22rem;
  margin-top: auto;
  border-top: 1px solid var(--border-color);
  padding-top: 0.45rem;
  color: var(--muted-text);
  font-size: 0.7rem;
  font-weight: 750;
  line-height: 1.42;
}

.match-detail-card dd small span {
  display: block;
  word-break: keep-all;
}

.match-detail-card--risk {
  border-color: color-mix(in srgb, var(--color-warning-dark, #b45309) 45%, var(--border-color)) !important;
  background: color-mix(in srgb, var(--color-warning-light, #fef3c7) 38%, var(--panel-muted)) !important;
}

.match-detail-card--risk .match-detail-card__icon {
  background: var(--color-warning-light, #fef3c7);
  color: var(--color-warning-dark, #b45309);
}

.match-drawer__foot {
  padding: 0.7rem 1rem;
}

.match-drawer__hero h3 {
  margin: 0 0 0.15rem;
  color: var(--text-primary);
  font-size: 1rem;
  font-weight: 900;
}

.match-drawer__hero p {
  margin: 0;
  color: var(--muted-text);
  font-size: 0.74rem;
  font-weight: 800;
}

.match-drawer__section {
  border-top: 1px solid var(--border-color);
}

.match-bars,
.match-kpis {
  display: grid;
  gap: 0.4rem;
  margin: 0;
  padding: 0;
  list-style: none;
}


.match-radar {
  display: grid;
  grid-template-columns: minmax(190px, 0.72fr) minmax(0, 1fr);
  align-items: center;
  gap: 0.75rem;
}

.match-radar__chart {
  width: 100%;
  max-width: 250px;
  aspect-ratio: 1;
  overflow: visible;
}

.match-radar__grid {
  fill: color-mix(in srgb, var(--accent-color) 4%, transparent);
  stroke: color-mix(in srgb, var(--accent-color) 28%, var(--border-color));
  stroke-width: 1;
}

.match-radar__grid--inner {
  fill: transparent;
  stroke-dasharray: 3 3;
  opacity: 0.75;
}

.match-radar__axis {
  stroke: color-mix(in srgb, var(--border-color) 80%, var(--accent-color));
  stroke-width: 1;
}

.match-radar__area {
  fill: color-mix(in srgb, var(--accent-color) 22%, transparent);
  stroke: var(--accent-color);
  stroke-width: 2.5;
  stroke-linejoin: round;
}

.match-radar__dot {
  fill: var(--accent-color);
  stroke: var(--panel-color);
  stroke-width: 2;
}

.match-radar__label {
  fill: var(--text-secondary);
  font-size: 10px;
  font-weight: 900;
}

.match-radar__legend {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.4rem;
  margin: 0;
}

.match-radar__legend div {
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-muted);
  padding: 0.45rem 0.55rem;
}

.match-radar__legend dt {
  color: var(--muted-text);
  font-size: 0.66rem;
  font-weight: 900;
}

.match-radar__legend dd {
  margin: 0.12rem 0 0;
  color: var(--text-primary);
  font-size: 0.74rem;
  font-weight: 900;
}
.match-bars li {
  display: grid;
  grid-template-columns: 7rem minmax(0, 1fr) 2rem;
  align-items: center;
  gap: 0.5rem;
}

.match-bars__track {
  height: 0.42rem;
  overflow: hidden;
  border-radius: 999px;
  background: var(--panel-muted);
}

.match-bars__track i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--accent-color);
}

.match-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.4rem;
  margin: 0;
}

.match-detail-grid div {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--panel-muted);
  padding: 0.5rem 0.6rem;
}

.match-drawer__foot {
  margin-top: auto;
  border-top: 1px solid var(--border-color);
  background: var(--panel-color);
}

.match-drawer__primary,
.match-drawer__success {
  width: 100%;
  border-radius: 7px;
  padding: 0.7rem;
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
}

.drawer-enter-active,
.drawer-leave-active,
.toast-enter-active,
.toast-leave-active {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.match-toast {
  position: fixed;
  right: 1.5rem;
  bottom: 1.5rem;
  z-index: 100;
  max-width: 420px;
  gap: 0.6rem;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--text-primary);
  color: #fff;
  padding: 0.65rem 0.85rem;
  box-shadow: 0 12px 28px rgba(19, 35, 68, 0.18);
}

.match-toast__icon {
  display: inline-flex;
  width: 1.3rem;
  height: 1.3rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--accent-color);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 900;
}

.match-toast__msg {
  min-width: 0;
  flex: 1;
  font-size: 0.78rem;
  font-weight: 800;
}

.match-toast__action,
.match-toast__close {
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.78);
  cursor: pointer;
  font-weight: 900;
}

.match-toast__action {
  color: color-mix(in srgb, var(--accent-color) 60%, white);
  font-size: 0.74rem;
  white-space: nowrap;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 900px) {
  .matching-workspace {
    grid-template-columns: 1fr;
  }

  .match-drawer {
    width: 100%;
  }
}

/* Drawer usability fix: wide enough to read, scrollable enough to finish. */
.match-drawer {
  width: min(760px, calc(100% - 48px));
  max-width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

.match-drawer__head {
  position: sticky;
  top: 0;
  z-index: 2;
  background: var(--panel-color);
}

.match-drawer__foot {
  position: sticky;
  bottom: 0;
  z-index: 2;
  margin-top: 0;
}

.match-bars li {
  grid-template-columns: 8.5rem minmax(0, 1fr) 2.4rem;
}

.match-detail-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 980px) {
  .match-drawer {
    width: min(100%, 680px);
  }

  .match-detail-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .match-drawer {
    width: 100%;
  }

  .match-radar,
  .match-bars li,
  .match-detail-grid {
    grid-template-columns: 1fr;
  }
}



/* Recommendation list view: comparison-first layout */
.match-listview {
  display: grid;
  gap: 0.5rem;
}

.match-list-row {
  display: grid;
  grid-template-columns: 1.45rem 6rem minmax(0, 1fr) max-content;
  align-items: center;
  gap: 0.75rem;
  min-height: 5.6rem;
  border: 1px solid var(--border-strong);
  border-radius: 8px;
  background: var(--panel-color);
  padding: 0.72rem 0.8rem;
  cursor: pointer;
  outline: none;
}

.match-list-row:hover,
.match-list-row--active,
.match-list-row--checked {
  border-color: color-mix(in srgb, var(--accent-color) 55%, var(--border-color));
  box-shadow: 0 5px 14px rgba(19, 35, 68, 0.06);
}

.match-list-row--active,
.match-list-row--checked {
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-color));
}

.match-list-row--evaluated {
  background: color-mix(in srgb, var(--accent-color) 4%, var(--panel-color));
}

.match-list-row__check {
  display: grid;
  place-items: center;
  cursor: pointer;
}

.match-list-row__check input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.match-list-row__check span {
  display: inline-block;
  width: 1rem;
  height: 1rem;
  border: 1.5px solid var(--border-color);
  border-radius: 4px;
  background: var(--panel-color);
}

.match-list-row__check input:checked + span {
  border-color: var(--accent-color);
  background: var(--accent-color);
}

.match-list-row__score {
  display: grid;
  justify-items: start;
  gap: 0.18rem;
}

.match-list-row__score strong {
  background: transparent !important;
  font-size: 1.45rem;
  font-weight: 900;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.match-list-row__score span {
  display: inline-flex;
  min-height: 1.35rem;
  align-items: center;
  border-radius: 999px;
  padding: 0 0.48rem;
  font-size: 0.64rem;
  font-weight: 900;
  white-space: nowrap;
}

.match-list-row__main {
  display: grid;
  min-width: 0;
  gap: 0.38rem;
}

.match-list-row__titleline {
  display: flex;
  align-items: baseline;
  gap: 0.45rem;
  min-width: 0;
}

.match-list-row__titleline h4 {
  overflow: hidden;
  margin: 0;
  color: var(--text-primary);
  font-size: 0.9rem;
  font-weight: 900;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-list-row__titleline small {
  flex: 0 0 auto;
  color: var(--muted-text);
  font-size: 0.72rem;
  font-weight: 800;
}

.sample-badge {
  display: inline-flex;
  align-items: center;
  min-height: 1.25rem;
  border: 1px solid color-mix(in srgb, var(--accent-color) 28%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 9%, var(--panel-color));
  color: var(--accent-color);
  padding: 0 0.45rem;
  font-size: 0.62rem;
  font-weight: 900;
  white-space: nowrap;
}

.match-drawer__titleline {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
}

.match-drawer__titleline h3 {
  margin: 0;
}

.match-list-row__titleline em {
  flex: 0 0 auto;
  border: 1px solid color-mix(in srgb, var(--accent-color) 35%, var(--border-color));
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-color));
  color: var(--accent-color);
  padding: 0.12rem 0.44rem;
  font-size: 0.62rem;
  font-style: normal;
  font-weight: 900;
}

.match-list-row__main p {
  overflow: hidden;
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.73rem;
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-list-row__main dl {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.1fr) minmax(8rem, 0.85fr);
  gap: 0.65rem;
  margin: 0;
}

.match-list-row__main dl div {
  display: grid;
  grid-template-columns: 2.45rem minmax(0, 1fr);
  align-items: baseline;
  gap: 0.35rem;
  min-width: 0;
}

.match-list-row__main dt {
  color: var(--muted-text);
  font-size: 0.65rem;
  font-weight: 900;
}

.match-list-row__main dd {
  overflow: hidden;
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-list-row__actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.match-list-row__icon,
.match-list-row__primary,
.match-list-row__success {
  min-height: 2rem;
  border-radius: 6px;
  font-size: 0.72rem;
  font-weight: 900;
  cursor: pointer;
  white-space: nowrap;
}

.match-list-row__icon {
  width: 2rem;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.match-list-row__primary {
  border: 0;
  background: var(--accent-color);
  color: #fff;
  padding: 0 0.72rem;
}

.match-list-row__success {
  border: 1px solid color-mix(in srgb, var(--accent-color) 40%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 14%, var(--panel-color));
  color: var(--accent-color);
  padding: 0 0.72rem;
}

@media (max-width: 1100px) {
  .match-list-row {
    grid-template-columns: 1.45rem 5.4rem minmax(0, 1fr);
  }

  .match-list-row__actions {
    grid-column: 3;
    justify-self: start;
  }

  .match-list-row__main dl {
    grid-template-columns: 1fr;
    gap: 0.28rem;
  }
}

@media (max-width: 680px) {
  .match-list-row {
    grid-template-columns: 1.45rem minmax(0, 1fr);
    align-items: start;
  }

  .match-list-row__score,
  .match-list-row__main,
  .match-list-row__actions {
    grid-column: 2;
  }

  .match-list-row__titleline {
    flex-wrap: wrap;
  }
}

/* Drawer density cleanup */
.match-drawer {
  background: var(--panel-color);
}

.match-drawer__hero,
.match-drawer__section {
  padding: 0.9rem 1.05rem;
}

.match-drawer__hero h3 {
  font-size: 1.05rem;
  line-height: 1.35;
}

.match-reasons {
  margin: 0.55rem 1.05rem 0.25rem;
  padding: 0.72rem 0.82rem;
}

.match-drawer__section h4 {
  margin: 0 0 0.68rem;
  color: var(--text-primary);
  font-size: 0.84rem;
  font-weight: 900;
}

.match-radar {
  grid-template-columns: minmax(170px, 0.56fr) minmax(0, 1fr);
  gap: 1.05rem;
  align-items: center;
}

.match-radar__chart {
  max-width: 210px;
  justify-self: center;
}

.match-radar__legend {
  gap: 0.5rem;
}

.match-radar__legend div {
  padding: 0.58rem 0.65rem;
  border-radius: 8px;
  background: var(--panel-color);
}

.match-kpis--ranked {
  gap: 0.38rem;
}

.match-kpis--ranked li {
  min-height: 2.6rem;
  padding: 0.52rem 0.75rem;
  background: var(--panel-color);
}

.match-detail-grid {
  gap: 0.65rem;
}

.match-detail-card {
  min-height: 9.4rem;
  padding: 0.72rem 0.78rem !important;
  border-radius: 8px !important;
  background: var(--panel-color) !important;
}

.match-detail-card dt {
  color: var(--muted-text);
  font-size: 0.68rem;
}

.match-detail-card dd strong {
  font-size: 0.8rem;
  line-height: 1.42;
}

.match-detail-card dd small {
  padding-top: 0.5rem;
  font-size: 0.68rem;
  line-height: 1.5;
}

.match-detail-card--risk {
  background: color-mix(in srgb, var(--color-warning-light, #fef3c7) 24%, var(--panel-color)) !important;
}

.match-drawer__foot {
  padding: 0.85rem 1.05rem 1rem;
  box-shadow: 0 -8px 18px rgba(19, 35, 68, 0.06);
}

.match-drawer__primary,
.match-drawer__success {
  min-height: 2.6rem;
}

@media (max-width: 760px) {
  .match-radar,
  .match-radar__legend,
  .match-detail-grid {
    grid-template-columns: 1fr;
  }

  .match-detail-card {
    min-height: auto;
  }
}

</style>
