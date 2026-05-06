<template>
  <div class="op">

    <!-- Breadcrumb + Edit toggle -->
    <div class="op-topbar">
      <nav class="op-breadcrumb" aria-label="breadcrumb">
        <span class="op-breadcrumb__item">제휴 모집</span>
        <span class="op-breadcrumb__sep">›</span>
        <span class="op-breadcrumb__item">진행중인 캠페인</span>
        <span class="op-breadcrumb__sep">›</span>
        <span class="op-breadcrumb__item op-breadcrumb__item--current" aria-current="page">소개 페이지</span>
      </nav>
      <div class="op-topbar__actions">
        <button v-if="!editMode" class="btn btn--ghost btn--sm" @click="enterEdit">
          <i class="ph ph-pencil-simple"></i>편집
        </button>
        <template v-else>
          <button class="btn btn--ghost btn--sm" :disabled="saving" @click="cancelEdit">취소</button>
          <button class="btn btn--primary btn--sm" :disabled="saving" @click="saveEdit">
            {{ saving ? '저장 중...' : '저장' }}
          </button>
        </template>
      </div>
    </div>

    <p v-if="errorMsg" class="op-error-banner">{{ errorMsg }}</p>

    <!-- Hero -->
    <header class="op-hero">
      <div class="op-hero__left">
        <div class="op-hero__badges">
          <span class="badge" :class="`badge--${statusToTone(campaignStatus)}`">{{ campaignStatus }}</span>
          <code v-if="!editMode && rfpCode" class="op-rfp">{{ rfpCode }}</code>
          <input
            v-if="editMode"
            v-model="editDraft.rfpCode"
            class="op-input op-input--inline"
            placeholder="RFP 코드 (예: RFP-2026-045)"
          />
        </div>
        <h1 class="op-hero__title">{{ campaignName }}</h1>
        <div class="op-hero__meta">
          <span><i class="ph ph-user-circle"></i>담당: {{ ownerLoginId }}</span>
          <span><i class="ph ph-eye"></i>공개 범위: 인증 사용자</span>
        </div>
      </div>

      <div class="op-hero__right">
        <div class="deadline-box">
          <div class="deadline-box__text">
            <div class="deadline-box__label">제안 마감까지</div>
            <div v-if="!editMode" class="deadline-box__date">{{ formatDate(recruitDeadline) }}</div>
            <input
              v-else
              v-model="editDraft.recruitDeadline"
              type="datetime-local"
              class="op-input op-input--datetime"
            />
          </div>
          <div v-if="!editMode" class="deadline-box__dday">{{ computeDday(recruitDeadline) }}</div>
        </div>
        <div v-if="!editMode" class="op-hero__actions">
          <button class="btn btn--ghost"><i class="ph ph-bookmark-simple"></i>관심 등록</button>
          <button class="btn btn--ghost"><i class="ph ph-chat-circle-question"></i>질문하기</button>
          <button class="btn btn--primary" @click="goToProposal">
            <i class="ph ph-paper-plane-tilt"></i>제안서 제출
          </button>
        </div>
      </div>
    </header>

    <!-- Tab Nav -->
    <nav class="op-tabs" aria-label="페이지 섹션">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="op-tab"
        :class="{ 'op-tab--active': activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
        <span v-if="tab.badge" class="op-tab__badge">{{ tab.badge }}</span>
      </button>
    </nav>

    <!-- Body -->
    <div class="op-body">

      <!-- Main Content -->
      <main class="op-main">

        <!-- ───── 상세 정보 탭 ───── -->
        <div v-show="activeTab === 'detail'">

        <!-- 캠페인 개요 -->
        <section class="card">
          <h2 class="card__title"><i class="ph ph-info"></i>캠페인 개요</h2>
          <p class="op-lead">{{ campaignSummary }}</p>
          <div class="overview-grid">
            <div v-for="item in overviewItems" :key="item.label" class="overview-cell">
              <div class="overview-cell__label">{{ item.label }}</div>
              <div class="overview-cell__value">{{ item.value }}</div>
            </div>
          </div>
        </section>

        <!-- 제공 자산 / 기대 역할 -->
        <div class="two-col">
          <section class="card">
            <h2 class="card__title"><i class="ph ph-gift"></i>한화 제공 자산</h2>
            <ul class="asset-list">
              <li v-for="a in hanwhaAssets" :key="a.icon" class="asset-item">
                <span class="asset-icon"><i :class="`ph ph-${a.icon}`"></i></span>
                <div>
                  <strong class="asset-item__title">{{ a.title }}</strong>
                  <p class="asset-item__desc">{{ a.desc }}</p>
                </div>
              </li>
            </ul>
          </section>
          <section class="card">
            <h2 class="card__title"><i class="ph ph-handshake"></i>파트너 기대 역할</h2>
            <ul class="asset-list">
              <li v-for="r in partnerRoles" :key="r.icon" class="asset-item">
                <span class="asset-icon"><i :class="`ph ph-${r.icon}`"></i></span>
                <div>
                  <strong class="asset-item__title">{{ r.title }}</strong>
                  <p class="asset-item__desc">{{ r.desc }}</p>
                </div>
              </li>
            </ul>
          </section>
        </div>

        <!-- 타깃 고객 + 참여 가치 -->
        <section class="card card--split">
          <div class="split-pane split-pane--l">
            <h2 class="card__title"><i class="ph ph-target"></i>타깃 고객 프로필</h2>
            <dl class="target-dl">
              <dt>핵심 세그먼트</dt>
              <dd>3040 유자녀 가족 (초등학생 이하 자녀 동반)</dd>
              <dt>고객 성향 / 관심사</dt>
              <dd>
                <div class="tag-row">
                  <span v-for="t in customerTags" :key="t" class="tag">{{ t }}</span>
                </div>
              </dd>
              <dt>예상 모객 규모</dt>
              <dd>캠페인 기간 내 패키지 구매자 약 15,000팀 (4인 기준 6만 명)</dd>
            </dl>
          </div>
          <div class="split-pane split-pane--r">
            <h2 class="card__title"><i class="ph ph-trend-up"></i>파트너 참여 가치</h2>
            <div class="value-list">
              <div v-for="v in partnerValues" :key="v.title" class="value-item">
                <div class="value-icon" :class="`value-icon--${v.tone}`">
                  <i :class="`ph ph-${v.icon}`"></i>
                </div>
                <div>
                  <h4 class="value-item__title">{{ v.title }}</h4>
                  <p class="value-item__desc">{{ v.desc }}</p>
                </div>
              </div>
            </div>
          </div>
        </section>

        </div>
        <!-- ───── /상세 정보 탭 ───── -->

        <!-- ───── 모집 일정 탭 ───── -->
        <div v-show="activeTab === 'schedule'">

        <!-- 진행 일정 타임라인 -->
        <section class="card">
          <div class="tl-header">
            <div>
              <h2 class="card__title"><i class="ph ph-calendar-blank"></i>진행 일정 및 타임라인</h2>
              <p class="card__sub">캠페인 런칭 전 주요 일정입니다. 마감 기한을 엄수해 주시기 바랍니다.</p>
            </div>
            <div class="tl-legend">
              <span v-for="l in legend" :key="l.label" class="legend-item">
                <span class="legend-dot" :style="{ background: l.color }"></span>{{ l.label }}
              </span>
            </div>
          </div>

          <div class="tl-track">
            <div
              v-for="ev in timelineEvents"
              :key="ev.id"
              class="tl-item"
              :class="{
                'tl-item--done': ev.done,
                'tl-item--urgent': ev.urgent,
              }"
            >
              <div class="tl-node-col">
                <div class="tl-node" :class="`tl-node--${ev.color}`">
                  <i v-if="ev.done" class="ph-bold ph-check"></i>
                  <span v-if="ev.urgent" class="tl-pulse"></span>
                </div>
              </div>
              <div class="tl-content">
                <div class="tl-row">
                  <h4 class="tl-title" :class="{ 'tl-title--urgent': ev.urgent }">
                    {{ ev.title }}
                    <span v-if="ev.tag" class="tl-tag" :class="`tl-tag--${ev.tagColor}`">{{ ev.tag }}</span>
                  </h4>
                  <span class="tl-date" :class="`tl-date--${ev.color}`">{{ ev.date }}</span>
                </div>
                <div v-if="ev.detail" class="tl-detail">
                  <p v-if="ev.detail.method" class="tl-detail__method">
                    <i class="ph ph-video-camera"></i>{{ ev.detail.method }}
                  </p>
                  <p class="tl-detail__text">{{ ev.detail.text }}</p>
                </div>
                <div v-if="ev.docs" class="tl-docs">
                  <p class="tl-docs__title">제출 서류:</p>
                  <ul>
                    <li v-for="doc in ev.docs" :key="doc">{{ doc }}</li>
                  </ul>
                </div>
                <p v-if="ev.note" class="tl-note">{{ ev.note }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 제출 안내 -->
        <section class="card">
          <h2 class="card__title"><i class="ph ph-upload-simple"></i>제출 안내 및 양식</h2>
          <div class="submission-box">
            <div class="submission-box__head">
              <div>
                <div class="submission-box__name">제안서 온라인 제출</div>
                <div class="submission-box__desc">우측의 '제안서 제출' 버튼을 클릭하여 웹 폼 작성 및 파일 업로드</div>
              </div>
              <span class="submission-box__limit">최대 파일 크기: 50MB (PDF, ZIP 권장)</span>
            </div>
            <h4 class="submission-docs__title">필수 제출 서류</h4>
            <ul class="submission-docs">
              <li v-for="doc in submissionDocs" :key="doc.label" class="submission-doc">
                <i :class="`ph ph-${doc.icon}`" :style="{ color: doc.color }"></i>
                <span class="submission-doc__label">{{ doc.label }}</span>
                <span class="req-badge" :class="{ 'req-badge--opt': !doc.required }">
                  {{ doc.required ? '필수' : '선택' }}
                </span>
              </li>
            </ul>
          </div>
        </section>

        </div>
        <!-- ───── /모집 일정 탭 ───── -->

      </main>

      <!-- Sidebar -->
      <aside class="op-sidebar">

        <!-- CTA -->
        <div class="card card--cta">
          <h3 class="cta-title">제휴 제안하기</h3>
          <p class="cta-desc">상세 요건을 확인하셨다면 기한 내에 제안서를 제출해 주세요.</p>
          <button class="btn btn--primary btn--block" @click="goToProposal">
            <i class="ph ph-paper-plane-right"></i>공식 제안서 제출
          </button>
          <div class="cta-sub">
            <button class="btn btn--ghost btn--sm"><i class="ph ph-star"></i>북마크</button>
            <button class="btn btn--ghost btn--sm"><i class="ph ph-share-network"></i>공유</button>
          </div>
        </div>

        <!-- 첨부 자료실 -->
        <div class="card">
          <h3 class="card__title-sm"><i class="ph ph-folder"></i>첨부 자료실</h3>
          <div class="file-list">
            <div
              v-for="f in attachedFiles"
              :key="f.name"
              class="file-item"
              :class="{ 'file-item--locked': f.locked }"
            >
              <div class="file-icon" :class="`file-icon--${f.tone}`">
                <i :class="`ph ph-${f.icon}`"></i>
              </div>
              <div class="file-info">
                <div class="file-name">{{ f.name }}</div>
                <div class="file-size">{{ f.size }}</div>
              </div>
              <i :class="`ph ph-${f.locked ? 'lock-key' : 'download-simple'} file-action`"></i>
            </div>
          </div>
        </div>

        <!-- 심사 평가 기준 (매칭 5축 가중치) -->
        <div class="card">
          <h3 class="card__title-sm"><i class="ph ph-scales"></i>심사 평가 기준 (매칭 가중치)</h3>
          <!-- 보기 모드 -->
          <div v-if="!editMode && hasAnyWeight" class="criteria-list">
            <div v-for="c in matchWeights" :key="c.label" class="criteria-item">
              <div class="criteria-row">
                <span class="criteria-label">{{ c.label }}</span>
                <strong class="criteria-pct">{{ c.value ?? 0 }}%</strong>
              </div>
              <div class="criteria-track">
                <div class="criteria-fill" :style="{ width: (c.value ?? 0) + '%' }"></div>
              </div>
            </div>
          </div>
          <p v-else-if="!editMode" class="op-empty-hint">매칭 가중치가 아직 설정되지 않았습니다.</p>
          <!-- 편집 모드 -->
          <div v-if="editMode" class="criteria-edit">
            <label class="op-field">
              <span class="op-field__label">고객 적합도 (%)</span>
              <input v-model.number="editDraft.weightCustomer" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">수익 효과 (%)</span>
              <input v-model.number="editDraft.weightRevenue" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">비용 구조 (%)</span>
              <input v-model.number="editDraft.weightCost" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">운영 부담 (%)</span>
              <input v-model.number="editDraft.weightOperation" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">브랜드 적합 (%)</span>
              <input v-model.number="editDraft.weightBrand" type="number" min="0" max="100" class="op-input" />
            </label>
          </div>
        </div>

        <!-- 담당자 문의 -->
        <div class="contact-card">
          <div class="contact-label">담당자 문의</div>
          <!-- 보기 모드 -->
          <template v-if="!editMode">
            <div class="contact-person">
              <div class="contact-avatar">{{ (contactInfo?.name ?? '?').charAt(0) }}</div>
              <div>
                <div class="contact-name">{{ contactInfo?.name ?? '담당자 미지정' }}</div>
                <div class="contact-team">{{ contactInfo?.team ?? '' }}</div>
              </div>
            </div>
            <div class="contact-info">
              <div class="contact-info__row"><i class="ph ph-envelope-simple"></i>{{ contactInfo?.email ?? '-' }}</div>
              <div class="contact-info__row"><i class="ph ph-phone"></i>{{ contactInfo?.phone ?? '-' }}</div>
            </div>
          </template>
          <!-- 편집 모드 -->
          <div v-else class="contact-edit">
            <label class="op-field">
              <span class="op-field__label">담당자명</span>
              <input v-model="editDraft.contactInfo.name" class="op-input" placeholder="홍길동 리드" />
            </label>
            <label class="op-field">
              <span class="op-field__label">소속 팀</span>
              <input v-model="editDraft.contactInfo.team" class="op-input" placeholder="○○사업팀" />
            </label>
            <label class="op-field">
              <span class="op-field__label">이메일</span>
              <input v-model="editDraft.contactInfo.email" type="email" class="op-input" placeholder="example@hanwha.com" />
            </label>
            <label class="op-field">
              <span class="op-field__label">전화번호</span>
              <input v-model="editDraft.contactInfo.phone" class="op-input" placeholder="02-1234-5678" />
            </label>
          </div>
        </div>

      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GetCampaignIntro, UpdateCampaignIntro } from '@/api/campaigns'

const route = useRoute()
const router = useRouter()

const activeTab = ref('detail')
const introData = ref(null)
const loading = ref(true)
const errorMsg = ref('')

// 인라인 편집 상태
const editMode = ref(false)
const editDraft = ref(null)
const saving = ref(false)

const tabs = [
  { id: 'detail', label: '상세 정보' },
  { id: 'schedule', label: '모집 일정' },
]

// fallback mockup — API에서 비어있을 때 보여줄 기본값
const FALLBACK = {
  hanwhaAssets: [
    { icon: 'device-mobile', title: '앱/온라인 채널 노출', desc: '한화온 앱 메인 배너 및 기획전 페이지 노출 (예상 트래픽: 50만/월)' },
    { icon: 'crown', title: 'VIP 고객 베이스', desc: '리조트 회원권 보유 VIP 타깃 e-DM 및 알림톡 발송 (10만 건)' },
    { icon: 'storefront', title: '오프라인 공간 활용', desc: '전국 12개 리조트 로비/객실 내 홍보물 비치 및 팝업 공간 제공' },
    { icon: 'ticket', title: '객실/티켓 자산', desc: '파트너사 이벤트용 리조트 숙박권 및 워터파크 이용권 지원' },
  ],
  partnerRoles: [
    { icon: 'star', title: '단독 혜택 제공', desc: '한화 패키지 이용객 대상 독점 할인 또는 한정판 굿즈/서비스 제공' },
    { icon: 'megaphone', title: '상호 마케팅 채널 지원', desc: '파트너사 온/오프라인 채널을 통한 공동 캠페인 홍보' },
    { icon: 'users-three', title: '운영 리소스 투입', desc: '제휴 서비스 제공을 위한 CS 채널 및 운영 인력 확보' },
    { icon: 'image', title: '콘텐츠 에셋 제작', desc: '기획전 구성에 필요한 브랜드 이미지 및 프로모션 소재 제공' },
  ],
  customerTags: ['프리미엄 레저', '키즈 에듀테인먼트', '편리한 이동', '미식 여행'],
  partnerValues: [
    { icon: 'crosshair', tone: 'primary', title: '고소득 구매력 타깃 확보', desc: '리조트 회원 및 프리미엄 객실 투숙객 대상의 고효율 마케팅' },
    { icon: 'hand-coins', tone: 'info', title: '브랜드 인지도 및 세일즈 증대', desc: '제휴 상품을 통한 직접적인 매출 발생 (예상 전환율 15%)' },
  ],
  timelineEvents: [
    { id: 1, color: 'gray', done: true, title: '모집 공고 오픈', date: '미정' },
    { id: 2, color: 'yellow', urgent: true, title: '제안서 제출 마감', date: '미정', tag: '중요', tagColor: 'red' },
    { id: 3, color: 'purple', title: '최종 파트너 선정 발표', date: '미정' },
  ],
  submissionDocs: [
    { icon: 'file-pdf', color: '#EF4444', required: true, label: '1. 제휴 제안서' },
    { icon: 'file-xls', color: '#22C55E', required: true, label: '2. 비용/혜택 구조 및 예상 KPI 산출표' },
    { icon: 'file-text', color: '#3B82F6', required: false, label: '3. 회사 소개서 및 레퍼런스' },
  ],
  attachedFiles: [],
  contactInfo: { name: '담당자 미지정', team: '', email: '-', phone: '-' },
}

const legend = [
  { label: '안내/설명회', color: '#60A5FA' },
  { label: '제출 마감', color: '#FBBF24' },
  { label: '심사/발표', color: '#C084FC' },
  { label: '운영 시작', color: '#34D399' },
]

// Campaign 기본 필드 매핑
const campaignName = computed(() => introData.value?.campaignName ?? '캠페인 소개')
const campaignSummary = computed(() =>
  introData.value?.campaignSummary
    ?? '아직 캠페인 소개 내용이 등록되지 않았습니다. 편집 모드에서 내용을 입력해 주세요.'
)
const campaignStatus = computed(() => introData.value?.campaignStatus ?? '준비중')
const ownerLoginId = computed(() => introData.value?.ownerLoginId ?? '미지정')
const rfpCode = computed(() => introData.value?.rfpCode ?? '')
const recruitDeadline = computed(() => introData.value?.recruitDeadline ?? null)

// JSON 필드 (없으면 fallback)
function pickList(value, fallback) {
  if (Array.isArray(value)) return value
  if (value && Array.isArray(value.list)) return value.list
  return fallback
}
const hanwhaAssets = computed(() => pickList(introData.value?.hanwhaAssets, FALLBACK.hanwhaAssets))
const partnerRoles = computed(() => pickList(introData.value?.partnerRoles, FALLBACK.partnerRoles))
const customerTags = computed(() => pickList(introData.value?.customerTags, FALLBACK.customerTags))
const partnerValues = computed(() => pickList(introData.value?.partnerValues, FALLBACK.partnerValues))
const timelineEvents = computed(() => pickList(introData.value?.timelineEvents, FALLBACK.timelineEvents))
const submissionDocs = computed(() => pickList(introData.value?.submissionDocs, FALLBACK.submissionDocs))
const attachedFiles = computed(() => pickList(introData.value?.attachedFiles, FALLBACK.attachedFiles))
const contactInfo = computed(() => introData.value?.contactInfo ?? FALLBACK.contactInfo)

// 캠페인 개요 그리드 — Campaign 기본 필드 기반
const overviewItems = computed(() => [
  { label: '캠페인 이름', value: campaignName.value },
  { label: '담당자', value: ownerLoginId.value },
  { label: '캠페인 상태', value: campaignStatus.value },
  { label: '제안 마감', value: formatDate(recruitDeadline.value) },
])

// 매칭 5축 weight (사이드바)
const matchWeights = computed(() => [
  { label: '고객 적합도', value: introData.value?.weightCustomer },
  { label: '수익 효과', value: introData.value?.weightRevenue },
  { label: '비용 구조', value: introData.value?.weightCost },
  { label: '운영 부담', value: introData.value?.weightOperation },
  { label: '브랜드 적합', value: introData.value?.weightBrand },
])
const hasAnyWeight = computed(() =>
  matchWeights.value.some(w => w.value != null && w.value > 0)
)

// 헬퍼
function statusToTone(status) {
  if (!status) return 'info'
  if (['recruiting', '모집중', 'active'].includes(status)) return 'success'
  if (['closed', '종료'].includes(status)) return 'muted'
  return 'info'
}
function formatDate(dt) {
  if (!dt) return '미정'
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return '미정'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}.${m}.${day}`
}
function computeDday(dt) {
  if (!dt) return '-'
  const target = new Date(dt)
  if (Number.isNaN(target.getTime())) return '-'
  const diff = Math.ceil((target - new Date()) / 86400000)
  return diff >= 0 ? `D-${diff}` : `D+${-diff}`
}
function goToProposal() {
  router.push({ name: 'campaign-proposal-new', params: { campaignId: route.params.campaignId } })
}

// 인라인 편집 함수
function enterEdit() {
  editDraft.value = {
    rfpCode: introData.value?.rfpCode ?? '',
    recruitDeadline: toDatetimeLocalValue(introData.value?.recruitDeadline),
    contactInfo: {
      name: introData.value?.contactInfo?.name ?? '',
      team: introData.value?.contactInfo?.team ?? '',
      email: introData.value?.contactInfo?.email ?? '',
      phone: introData.value?.contactInfo?.phone ?? '',
    },
    weightCustomer: introData.value?.weightCustomer ?? 0,
    weightRevenue: introData.value?.weightRevenue ?? 0,
    weightCost: introData.value?.weightCost ?? 0,
    weightOperation: introData.value?.weightOperation ?? 0,
    weightBrand: introData.value?.weightBrand ?? 0,
  }
  editMode.value = true
}

function cancelEdit() {
  editDraft.value = null
  editMode.value = false
}

async function saveEdit() {
  if (!editDraft.value) return
  saving.value = true
  errorMsg.value = ''
  try {
    const payload = {
      rfpCode: editDraft.value.rfpCode || null,
      recruitDeadline: editDraft.value.recruitDeadline
        ? new Date(editDraft.value.recruitDeadline).toISOString()
        : null,
      contactInfo: { ...editDraft.value.contactInfo },
      weightCustomer: Number(editDraft.value.weightCustomer) || null,
      weightRevenue: Number(editDraft.value.weightRevenue) || null,
      weightCost: Number(editDraft.value.weightCost) || null,
      weightOperation: Number(editDraft.value.weightOperation) || null,
      weightBrand: Number(editDraft.value.weightBrand) || null,
    }
    await UpdateCampaignIntro(route.params.campaignId, payload)
    introData.value = await GetCampaignIntro(route.params.campaignId)
    editMode.value = false
    editDraft.value = null
  } catch (e) {
    errorMsg.value = e?.message ?? '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function toDatetimeLocalValue(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return ''
  // YYYY-MM-DDTHH:mm 형식 (datetime-local input 표준)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  try {
    introData.value = await GetCampaignIntro(route.params.campaignId)
  } catch (e) {
    errorMsg.value = e?.message ?? '소개 페이지를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ─── Design Tokens (전역 테마와 연결) ─────────────────────────── */
.op {
  --bg: var(--app-bg);
  --surface: var(--panel-color);
  --surface-muted: var(--panel-muted);
  --border: var(--border-color);
  --border-mid: var(--border-strong);

  --text-1: var(--text-primary);
  --text-2: var(--text-secondary);
  --text-3: var(--muted-text);
  --text-4: var(--subtle-text);

  --primary: var(--color-primary-500);
  --primary-h: var(--color-primary-600);
  --primary-s: var(--color-primary-100);
  --primary-m: var(--color-primary-200);

  --success: var(--color-success);
  --success-s: var(--color-success-light);
  --success-t: var(--color-success-dark);
  --warning: var(--color-warning);
  --warning-s: var(--color-warning-light);
  --warning-t: var(--color-warning-dark);
  --danger: var(--color-danger);
  --danger-s: var(--color-danger-light);
  --danger-t: var(--color-danger-dark);
  --info: var(--color-info);
  --info-s: var(--color-info-light);
  --info-t: var(--color-info-dark);
  --purple: var(--color-primary-400);
  --purple-s: var(--color-primary-50);
  --purple-t: var(--color-primary-700);
  --emerald: var(--color-success);
  --emerald-s: var(--color-success-light);
  --emerald-t: var(--color-success-dark);

  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;

  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);

  font-family: 'Pretendard', 'Noto Sans KR', -apple-system, sans-serif;
  background: var(--bg);
  color: var(--text-1);
  padding: 32px 40px 80px;
  min-height: 100vh;
}

/* ─── Breadcrumb ─────────────────────────────────────────────── */
.op-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 24px;
  font-size: 13px;
}
.op-breadcrumb__item {
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.15s;
}
.op-breadcrumb__item:hover { color: var(--text-1); }
.op-breadcrumb__item--current {
  color: var(--text-1);
  font-weight: 600;
  cursor: default;
}
.op-breadcrumb__sep { color: var(--text-4); font-size: 11px; }

/* ─── Hero ───────────────────────────────────────────────────── */
.op-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 32px;
}
.op-hero__left { flex: 1; }
.op-hero__badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.op-hero__title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-1);
  letter-spacing: -0.03em;
  line-height: 1.3;
  margin-bottom: 14px;
}
.op-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--text-3);
}
.op-hero__meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.op-hero__meta i { font-size: 15px; color: var(--text-4); }

.op-hero__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
  min-width: 320px;
}
.op-hero__actions {
  display: flex;
  gap: 8px;
  width: 100%;
}

/* Deadline Box */
.deadline-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 18px;
  background: var(--primary-s);
  border: 1px solid var(--primary-m);
  border-radius: var(--radius-md);
}
.deadline-box__label {
  font-size: 11px;
  font-weight: 600;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 3px;
}
.deadline-box__date {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}
.deadline-box__dday {
  font-size: 28px;
  font-weight: 900;
  color: var(--primary);
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}

/* ─── Badges ─────────────────────────────────────────────────── */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}
.badge--success {
  background: var(--success-s);
  color: var(--success-t);
  border-color: #6EE7B7;
}
.badge--info {
  background: var(--info-s);
  color: var(--info-t);
  border-color: #93C5FD;
}
.badge--muted {
  background: var(--surface-muted);
  color: var(--text-3);
  border-color: var(--border);
}
.op-empty-hint {
  color: var(--text-3);
  font-size: 13px;
  padding: 8px 0;
  text-align: center;
}

/* ─── Topbar (Breadcrumb + Edit toggle) ──────────────────────── */
.op-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.op-topbar .op-breadcrumb { margin-bottom: 0; }
.op-topbar__actions {
  display: flex;
  gap: 8px;
}

.op-error-banner {
  margin-bottom: 16px;
  padding: 10px 14px;
  border: 1px solid var(--danger);
  background: var(--danger-s);
  color: var(--danger-t);
  border-radius: var(--radius-md);
  font-size: 13px;
}

/* ─── Inline Edit Inputs ──────────────────────────────────────── */
.op-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-1);
  font-size: 13px;
  font-family: inherit;
  transition: border-color 0.15s;
}
.op-input:focus {
  outline: none;
  border-color: var(--primary);
}
.op-input--inline {
  width: auto;
  min-width: 220px;
  padding: 4px 8px;
  font-size: 12px;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}
.op-input--datetime {
  width: 100%;
}

.op-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}
.op-field__label {
  font-size: 12px;
  color: var(--text-3);
  font-weight: 600;
}

.criteria-edit,
.contact-edit {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.btn--sm {
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
}
.op-rfp {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  color: var(--text-3);
  background: var(--surface-muted);
  border: 1px solid var(--border);
  padding: 2px 8px;
  border-radius: 4px;
}

/* ─── Buttons ────────────────────────────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  white-space: nowrap;
  border: 1px solid transparent;
}
.btn--primary {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 1px 4px rgba(139,92,246,0.25);
}
.btn--primary:hover { background: var(--primary-h); }
.btn--ghost {
  background: var(--surface);
  color: var(--text-2);
  border-color: var(--border);
}
.btn--ghost:hover { background: var(--surface-muted); }
.btn--block { width: 100%; justify-content: center; padding: 11px 14px; font-size: 14px; }
.btn--sm { padding: 6px 12px; font-size: 12px; }
.btn-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}
.btn-link:hover { color: var(--primary-h); }
.btn-link-sm {
  font-size: 12px;
  color: var(--text-3);
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
  transition: color 0.15s;
}
.btn-link-sm:hover { color: var(--text-1); }

/* ─── Tab Nav ────────────────────────────────────────────────── */
.op-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--border);
  margin-bottom: 32px;
  position: sticky;
  top: 0;
  background: var(--bg);
  z-index: 10;
  padding-top: 4px;
}
.op-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.15s;
}
.op-tab::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: transparent;
  transition: background 0.15s;
}
.op-tab--active { color: var(--primary); }
.op-tab--active::after { background: var(--primary); }
.op-tab__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--border);
  color: var(--text-2);
  font-size: 11px;
  font-weight: 700;
  border-radius: 9px;
}
.op-tab--active .op-tab__badge {
  background: var(--primary-m);
  color: var(--primary);
}

/* ─── Body Layout ────────────────────────────────────────────── */
.op-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 28px;
  align-items: start;
}
.op-main { display: flex; flex-direction: column; gap: 24px; min-width: 0; }
.op-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 52px;
}

/* ─── Cards ──────────────────────────────────────────────────── */
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 28px 28px 24px;
  box-shadow: var(--shadow-sm);
}
.card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 20px;
  letter-spacing: -0.01em;
}
.card__title i { color: var(--primary); font-size: 18px; }
.card__title-sm {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 16px;
}
.card__title-sm i { color: var(--text-3); font-size: 16px; }
.card__sub { font-size: 13px; color: var(--text-3); margin-top: -14px; margin-bottom: 20px; line-height: 1.5; }

/* Split card */
.card--split {
  display: flex;
  padding: 0;
  overflow: hidden;
}
.split-pane {
  padding: 28px;
  flex: 1;
}
.split-pane--r {
  background: var(--surface-muted);
  border-left: 1px solid var(--border);
}

/* CTA card */
.card--cta {
  border-color: var(--primary-m);
  background: var(--surface);
}

/* ─── Two column grid ────────────────────────────────────────── */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

/* ─── Overview Grid ──────────────────────────────────────────── */
.op-lead {
  font-size: 14px;
  line-height: 1.75;
  color: var(--text-2);
  margin-bottom: 24px;
}
.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.overview-cell {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
}
.overview-cell__label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-4);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 5px;
}
.overview-cell__value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  line-height: 1.4;
}

/* ─── Asset List ─────────────────────────────────────────────── */
.asset-list { display: flex; flex-direction: column; gap: 20px; list-style: none; padding: 0; margin: 0; }
.asset-item { display: flex; gap: 12px; }
.asset-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  border: 1px solid var(--border);
  color: var(--text-2);
  font-size: 16px;
  margin-top: 1px;
}
.asset-item__title {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 3px;
}
.asset-item__desc { font-size: 12px; color: var(--text-3); line-height: 1.55; }

/* ─── Target DL ──────────────────────────────────────────────── */
.target-dl { display: flex; flex-direction: column; gap: 16px; }
.target-dl dt {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-4);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}
.target-dl dd { font-size: 13px; color: var(--text-2); line-height: 1.5; margin: 0; }
.tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 2px; }
.tag {
  padding: 3px 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-2);
}

/* ─── Value List ─────────────────────────────────────────────── */
.value-list { display: flex; flex-direction: column; gap: 20px; }
.value-item { display: flex; gap: 14px; }
.value-icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-top: 1px;
}
.value-icon--primary { background: var(--primary-s); color: var(--primary); }
.value-icon--info { background: var(--info-s); color: var(--info); }
.value-item__title { font-size: 13px; font-weight: 700; color: var(--text-1); margin-bottom: 4px; }
.value-item__desc { font-size: 12px; color: var(--text-3); line-height: 1.6; }

/* ─── Timeline ───────────────────────────────────────────────── */
.tl-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}
.tl-legend { display: flex; gap: 14px; flex-shrink: 0; margin-top: 4px; }
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-3);
}
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

.tl-track {
  display: flex;
  flex-direction: column;
  padding-left: 16px;
  border-left: 2px solid var(--border);
  gap: 0;
}
.tl-item {
  display: flex;
  gap: 20px;
  padding-bottom: 32px;
  position: relative;
}
.tl-item:last-child { padding-bottom: 0; }
.tl-item--done { opacity: 0.55; }

.tl-node-col {
  position: relative;
  flex-shrink: 0;
  margin-left: -27px;
}
.tl-node {
  position: relative;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2.5px solid var(--border-mid);
  background: var(--surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  color: var(--text-3);
  z-index: 1;
  margin-top: 2px;
}
.tl-node--gray  { border-color: #9CA3AF; background: #F9FAFB; }
.tl-node--blue  { border-color: #60A5FA; }
.tl-node--yellow { border-color: #FBBF24; }
.tl-node--purple { border-color: #C084FC; }
.tl-node--emerald { border-color: #34D399; }

.tl-item--urgent .tl-node {
  border-color: var(--warning);
  background: var(--warning);
  width: 22px;
  height: 22px;
  margin-top: 1px;
  box-shadow: 0 0 0 4px rgba(245,158,11,0.15);
}
.tl-pulse {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 2px solid var(--warning);
  animation: pulse-ring 1.5s ease-out infinite;
  pointer-events: none;
}
@keyframes pulse-ring {
  0%   { opacity: 0.8; transform: scale(1); }
  100% { opacity: 0; transform: scale(1.6); }
}

.tl-content { flex: 1; min-width: 0; }
.tl-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}
.tl-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  letter-spacing: -0.01em;
}
.tl-title--urgent { color: #92400E; }
.tl-date {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  padding: 3px 10px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}
.tl-date--gray   { background: var(--surface-muted); color: var(--text-3); border: 1px solid var(--border); }
.tl-date--blue   { background: var(--info-s);    color: var(--info);    border: 1px solid #BFDBFE; }
.tl-date--yellow { background: var(--warning-s); color: var(--warning-t); border: 1px solid #FDE68A; }
.tl-date--purple { background: var(--purple-s);  color: var(--purple-t); border: 1px solid #DDD6FE; }
.tl-date--emerald { background: var(--emerald-s); color: var(--emerald-t); border: 1px solid #A7F3D0; }

.tl-tag {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 3px;
  border: 1px solid transparent;
}
.tl-tag--blue   { background: var(--info-s);    color: var(--info-t);    border-color: #BFDBFE; }
.tl-tag--yellow { background: var(--warning-s); color: var(--warning-t); border-color: #FDE68A; }
.tl-tag--red    { background: var(--danger-s);  color: var(--danger-t);  border-color: #FECACA; }
.tl-tag--purple { background: var(--purple-s);  color: var(--purple-t);  border-color: #DDD6FE; }
.tl-tag--emerald { background: var(--emerald-s); color: var(--emerald-t); border-color: #A7F3D0; }

.tl-item--urgent .tl-content {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: var(--radius-md);
  padding: 14px 16px;
  margin-top: -2px;
}
.tl-detail {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 8px;
}
.tl-detail__method {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 6px;
}
.tl-detail__text { font-size: 12px; color: var(--text-3); line-height: 1.6; margin: 0; }
.tl-docs {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 8px;
}
.tl-item--urgent .tl-docs {
  background: rgba(255,255,255,0.6);
  border-color: #FCD34D;
}
.tl-docs__title { font-size: 12px; font-weight: 700; color: var(--text-2); margin-bottom: 6px; }
.tl-docs ul { list-style: disc; padding-left: 16px; margin: 0; }
.tl-docs li { font-size: 12px; color: var(--text-3); line-height: 1.7; }
.tl-note { font-size: 13px; color: var(--text-3); margin-top: 6px; line-height: 1.55; }

/* ─── Submission ─────────────────────────────────────────────── */
.submission-box {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.submission-box__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
}
.submission-box__name { font-size: 14px; font-weight: 700; color: var(--text-1); margin-bottom: 3px; }
.submission-box__desc { font-size: 12px; color: var(--text-3); }
.submission-box__limit { font-size: 12px; color: var(--text-3); white-space: nowrap; margin-top: 2px; }
.submission-docs__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-2);
  padding: 14px 20px 10px;
}
.submission-docs { list-style: none; padding: 0 12px 12px; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.submission-doc {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--surface);
  border-radius: var(--radius-sm);
}
.submission-doc i { font-size: 18px; flex-shrink: 0; }
.submission-doc__label { font-size: 13px; color: var(--text-2); flex: 1; }
.req-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 3px;
  background: var(--danger-s);
  color: var(--danger-t);
  flex-shrink: 0;
}
.req-badge--opt { background: var(--surface-muted); color: var(--text-3); }

/* ─── Q&A ────────────────────────────────────────────────────── */
.qa-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.qa-hd .card__title { margin-bottom: 0; }
.qa-list { display: flex; flex-direction: column; gap: 8px; }
.qa-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.qa-q {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: var(--surface-muted);
  cursor: pointer;
  transition: background 0.12s;
  user-select: none;
}
.qa-q:hover { background: var(--border); background: #F3F4F6; }
.qa-q__mark {
  font-size: 14px;
  font-weight: 900;
  color: var(--primary);
  flex-shrink: 0;
  margin-top: 1px;
}
.qa-q__body { flex: 1; min-width: 0; }
.qa-q__meta { display: flex; align-items: center; gap: 8px; margin-bottom: 5px; }
.qa-status {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 3px;
}
.qa-status--done { background: var(--success-s); color: var(--success-t); }
.qa-status--wait { background: var(--surface); border: 1px solid var(--border); color: var(--text-3); }
.qa-from { font-size: 11px; color: var(--text-4); }
.qa-q__text { font-size: 13px; font-weight: 600; color: var(--text-1); line-height: 1.5; }
.qa-caret {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-4);
  margin-top: 3px;
  transition: transform 0.2s;
}
.qa-caret--open { transform: rotate(-180deg); }
.qa-a {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  background: var(--surface);
  border-top: 1px solid var(--border);
}
.qa-a__mark {
  font-size: 14px;
  font-weight: 900;
  color: var(--text-4);
  flex-shrink: 0;
  margin-top: 1px;
}
.qa-a p { font-size: 13px; color: var(--text-2); line-height: 1.7; margin: 0; }
.qa-more { text-align: center; margin-top: 16px; }

/* Q&A Transition */
.qa-slide-enter-active, .qa-slide-leave-active { transition: all 0.2s ease; overflow: hidden; }
.qa-slide-enter-from, .qa-slide-leave-to { opacity: 0; max-height: 0; }
.qa-slide-enter-to, .qa-slide-leave-from { opacity: 1; max-height: 200px; }

/* ─── Sidebar: CTA ───────────────────────────────────────────── */
.cta-title { font-size: 15px; font-weight: 800; color: var(--text-1); margin-bottom: 6px; letter-spacing: -0.02em; }
.cta-desc { font-size: 12px; color: var(--text-3); margin-bottom: 14px; line-height: 1.5; }
.cta-sub { display: flex; gap: 8px; margin-top: 8px; }
.cta-sub .btn { flex: 1; justify-content: center; }

/* ─── Sidebar: Files ─────────────────────────────────────────── */
.file-list { display: flex; flex-direction: column; gap: 6px; }
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.12s, border-color 0.12s;
  text-decoration: none;
  color: inherit;
}
.file-item:hover { background: var(--surface-muted); }
.file-item:hover .file-name { color: var(--primary); }
.file-item--locked { opacity: 0.65; border-style: dashed; cursor: default; }
.file-item--locked:hover { background: transparent; }
.file-item--locked:hover .file-name { color: inherit; }
.file-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.file-icon--red   { background: var(--danger-s);   color: var(--danger); }
.file-icon--blue  { background: var(--info-s);     color: var(--info); }
.file-icon--green { background: var(--success-s);  color: var(--success); }
.file-icon--gray  { background: var(--surface-muted); color: var(--text-3); border: 1px solid var(--border); }
.file-info { flex: 1; min-width: 0; }
.file-name { font-size: 13px; font-weight: 600; color: var(--text-1); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; transition: color 0.12s; }
.file-size { font-size: 11px; color: var(--text-4); margin-top: 2px; }
.file-action { font-size: 16px; color: var(--text-4); flex-shrink: 0; }

/* ─── Sidebar: Criteria ──────────────────────────────────────── */
.criteria-list { display: flex; flex-direction: column; gap: 14px; }
.criteria-item {}
.criteria-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}
.criteria-label { font-size: 12px; color: var(--text-2); }
.criteria-pct { font-size: 13px; font-weight: 800; color: var(--text-1); font-variant-numeric: tabular-nums; }
.criteria-track {
  height: 6px;
  background: var(--primary-s);
  border-radius: 99px;
  overflow: hidden;
}
.criteria-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 99px;
  transition: width 0.4s ease;
}

/* ─── Sidebar: Contact ───────────────────────────────────────── */
.contact-card {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
}
.contact-label {
  font-size: 10px;
  font-weight: 800;
  color: var(--text-4);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 12px;
}
.contact-person {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.contact-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--primary-s);
  border: 2px solid var(--primary-m);
  color: var(--primary);
  font-size: 14px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.contact-name { font-size: 14px; font-weight: 700; color: var(--text-1); }
.contact-team { font-size: 11px; color: var(--text-4); margin-top: 2px; }
.contact-info { display: flex; flex-direction: column; gap: 6px; }
.contact-info__row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--text-3);
}
.contact-info__row i { font-size: 13px; }
</style>
