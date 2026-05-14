<script setup>
import { computed, ref } from 'vue'
import { createFrame } from '@/api/frames'
import { usePlannerStore } from '@/stores/planner'

const plannerStore = usePlannerStore()

const t = {
  eyebrow: '\uCEA0\uD398\uC778 \uC2E4\uD589 \uAE30\uC900',
  title: '\uCEA0\uD398\uC778 \uD504\uB808\uC784 \uB77C\uC774\uBE0C\uB7EC\uB9AC',
  desc: '\uBCF8\uC0AC, \uD611\uB825\uC0AC, \uB300\uD589\uC0AC\uAC00 \uAC19\uC740 \uAE30\uC900\uC73C\uB85C \uCEA0\uD398\uC778 \uBB38\uC11C\uB97C \uC791\uC131\uD558\uACE0 \uAC80\uC218\uD558\uB294 \uC2E4\uD589 \uAD6C\uC870\uC785\uB2C8\uB2E4.',
  newFrame: '\uC0C8 \uD504\uB808\uC784',
  createFrame: '\uD504\uB808\uC784 \uC0DD\uC131',
  cancel: '\uCDE8\uC18C',
}

const selectedCampaignType = ref('전체 캠페인 방식')
const selectedLibrarySort = ref('점수 높은순')
const selectedPreviewChannel = ref('sns')
const previewChannelOptions = [
  { id: 'sns', label: 'SNS 정사각' },
  { id: 'vert', label: '세로 포스터' },
  { id: 'kv', label: '웹 KV 배너' },
]

const createFrameCategoryOptions = [
  {
    value: '\uCFE0\uD3F0/\uD560\uC778',
    theme: 'coupon',
    description: '\uD560\uC778\uAD8C, \uC989\uC2DC \uD560\uC778, \uD3EC\uC778\uD2B8 \uC801\uB9BD\uC73C\uB85C \uAD6C\uB9E4 \uC804\uD658\uC744 \uB9CC\uB4ED\uB2C8\uB2E4.',
  },
  {
    value: '\uCCB4\uD5D8\uAD8C/\uC0AC\uC740\uD488',
    theme: 'gift',
    description: '\uC0D8\uD50C, \uC774\uC6A9\uAD8C, \uAD7F\uC988\uB97C \uC81C\uACF5\uD574 \uC2E0\uADDC \uCCB4\uD5D8\uC744 \uC720\uB3C4\uD569\uB2C8\uB2E4.',
  },
  {
    value: '\uBA64\uBC84\uC2ED \uD61C\uD0DD',
    theme: 'membership',
    description: 'VIP \uC804\uC6A9 \uD61C\uD0DD, \uB4F1\uAE09\uBCC4 \uB9AC\uC6CC\uB4DC\uB85C \uC7AC\uBC29\uBB38\uC744 \uB192\uC785\uB2C8\uB2E4.',
  },
  {
    value: '\uACF5\uB3D9 \uD504\uB85C\uBAA8\uC158',
    theme: 'joint',
    description: '\uC591\uC0AC \uACE0\uAC1D\uC744 \uD568\uAED8 \uD0C0\uAC9F\uD305\uD574 \uACF5\uB3D9 \uC774\uBCA4\uD2B8\uB97C \uC6B4\uC601\uD569\uB2C8\uB2E4.',
  },
  {
    value: '\uCF58\uD150\uCE20 \uD611\uC5C5',
    theme: 'content',
    description: '\uBE0C\uB79C\uB4DC \uCF58\uD150\uCE20, \uB9AC\uBDF0, \uC601\uC0C1, SNS \uCEA0\uD398\uC778\uC744 \uD568\uAED8 \uC81C\uC791\uD569\uB2C8\uB2E4.',
  },
  {
    value: '\uCC44\uB110/\uC571 \uB178\uCD9C',
    theme: 'channel',
    description: '\uC571 \uBC30\uB108, \uD478\uC2DC, \uC54C\uB9BC\uD1A1, \uC6F9 \uC601\uC5ED\uC5D0 \uD30C\uD2B8\uB108 \uD61C\uD0DD\uC744 \uB178\uCD9C\uD569\uB2C8\uB2E4.',
  },
]

const toneGuideProfiles = {
  coupon: {
    summary: '혜택 조건과 기간을 먼저 이해할 수 있게 쓰고, 가격 우위나 긴급성을 과장하지 않습니다.',
    rules: [
      { label: '호칭', value: '고객님 · 회원님' },
      { label: '어미', value: '정중한 안내체' },
      { label: '이모지', value: '사용 안 함' },
      { label: '감탄사', value: '금지' },
      { label: '숫자 표기', value: '기간 · 조건 명확히' },
      { label: '금액 표기', value: '원화 · 콤마' },
    ],
    examples: [
      { tone: '권장', text: '회원 전용 5월 할인 혜택을 안내드립니다. 사용 기간과 조건을 확인해 주세요.' },
      { tone: '금지', text: '무조건 최저가! 지금 안 사면 후회합니다!' },
    ],
  },
  gift: {
    summary: '증정 수량과 수령 방법을 투명하게 안내하고, 당첨 보장이나 과도한 희소성 표현은 피합니다.',
    rules: [
      { label: '호칭', value: '참여 고객님' },
      { label: '어미', value: '친근한 안내체' },
      { label: '이모지', value: '1개 이하' },
      { label: '감탄사', value: '제한' },
      { label: '숫자 표기', value: '수량 · 기간 필수' },
      { label: '금액 표기', value: '제공 조건 중심' },
    ],
    examples: [
      { tone: '권장', text: '사전 등록 고객님께 체험 키트를 선착순으로 제공합니다. 수령 방법을 확인해 주세요.' },
      { tone: '금지', text: '지금 아니면 못 받아요! 100% 증정 보장!' },
    ],
  },
  membership: {
    summary: '등급별 혜택은 프리미엄하게 전달하되, 비회원이나 하위 등급을 배제하는 인상을 줄입니다.',
    rules: [
      { label: '호칭', value: '회원님 · 멤버님' },
      { label: '어미', value: '격식 있는 해요체' },
      { label: '이모지', value: '최소 사용' },
      { label: '감탄사', value: '지양' },
      { label: '숫자 표기', value: '등급 · 기간 명확히' },
      { label: '금액 표기', value: '혜택 조건 병기' },
    ],
    examples: [
      { tone: '권장', text: '프리미엄 멤버님께 제공되는 5월 한정 혜택을 안내드립니다.' },
      { tone: '금지', text: 'VIP만 누리는 특권! 일반 회원은 제외됩니다.' },
    ],
  },
  joint: {
    summary: '두 브랜드가 대등하게 협력한다는 인상을 유지하고, 한쪽이 단독 주도하는 표현은 피합니다.',
    rules: [
      { label: '호칭', value: '고객님' },
      { label: '어미', value: '중립 안내체' },
      { label: '이모지', value: '사용 안 함' },
      { label: '감탄사', value: '지양' },
      { label: '숫자 표기', value: '양사 조건 병기' },
      { label: '금액 표기', value: '제공 주체 명시' },
    ],
    examples: [
      { tone: '권장', text: '양사가 함께 준비한 공동 혜택을 안내드립니다. 각 브랜드 채널에서 동일하게 확인할 수 있습니다.' },
      { tone: '금지', text: '본사가 단독으로 제공하는 유일한 제휴 혜택입니다.' },
    ],
  },
  content: {
    summary: '광고성 콘텐츠임을 분명히 밝히고, 협찬 사실을 숨기거나 후기처럼 오해될 표현을 피합니다.',
    rules: [
      { label: '호칭', value: '구독자 · 고객님' },
      { label: '어미', value: '투명한 설명체' },
      { label: '이모지', value: '채널 기준 준수' },
      { label: '감탄사', value: '최소화' },
      { label: '숫자 표기', value: '노출 기간 명시' },
      { label: '금액 표기', value: '유료 광고 명시' },
    ],
    examples: [
      { tone: '권장', text: '(광고) 브랜드와 함께 제작한 협업 콘텐츠입니다. 제공 범위와 활용 조건을 확인해 주세요.' },
      { tone: '금지', text: '내돈내산 솔직 후기예요. 협찬 아닙니다.' },
    ],
  },
  channel: {
    summary: '알림 피로도를 줄이기 위해 짧고 정보 중심으로 작성하고, 클릭을 압박하는 표현은 피합니다.',
    rules: [
      { label: '호칭', value: '회원님' },
      { label: '어미', value: '짧은 안내체' },
      { label: '이모지', value: '푸시 미사용' },
      { label: '감탄사', value: '금지' },
      { label: '숫자 표기', value: '제목 40자 기준' },
      { label: '금액 표기', value: '조건 우선 표기' },
    ],
    examples: [
      { tone: '권장', text: '관심 카테고리의 이번 주 추천 혜택을 안내드립니다.' },
      { tone: '금지', text: '긴급 알림! 지금 클릭하지 않으면 마지막 기회를 놓칩니다!' },
    ],
  },
}

const frameCatalog = {
  frames: [
    {
      id: 'coupon_discount',
      category: '쿠폰/할인',
      version: 'v2.3',
      title: '쿠폰·할인 표준 프레임',
      score: 91,
      overview: '할인율, 사용 기간, 사용 조건, 제외 대상 표기를 검수합니다. "무조건", "최대 할인" 등 과장 표현을 차단합니다.',
      preview: {
        theme: 'coupon',
      },
      required_fields: ['할인율 또는 할인 금액 명시', '유효 기간 (시작일·종료일)', '사용 조건 (최소 구매 금액 등)', '제외 대상·중복 사용 가능 여부', '발급 수량 또는 한정 조건', '고객 문의 채널'],
      banned_expressions: ['무조건', '최대 할인', '단독 혜택', '최저가', '절대 후회 없음', '한정 마감 임박'],
      recommended_expressions: ['합리적인 가격', '기간 한정 혜택', '회원 전용 할인', '특별 제공가'],
      tone_guide: '혜택의 가치를 정확하고 구체적으로 전달하되, 과장이나 긴급성 강조는 피합니다. 고객이 혜택의 조건을 명확히 이해할 수 있도록 작성하고, 비교급 표현("최고", "최저")은 사용하지 않습니다. 프리미엄 브랜드의 신뢰감을 유지하며, 거래 조건을 투명하게 제시하는 어투를 권장합니다.',
      approval_process: ['PM사 캠페인 매니저', '파트너사 제휴 담당자', '본사 브랜드/법무 담당'],
      performance: { usage_count: 24, pass_rate: 91, avg_revisions: 3.2 },
    },
    {
      id: 'experience_gift',
      category: '체험권/사은품',
      version: 'v1.8',
      title: '체험권·사은품 표준 프레임',
      score: 93,
      overview: '제공 수량, 받는 방법, 유효 기간 표기를 검수합니다. "선착순 마감 임박" 같은 과장 표현을 제한하고 신뢰감 있는 안내를 유도합니다.',
      preview: {
        theme: 'gift',
      },
      required_fields: ['제공 수량 또는 재고 명시', '수령 방법·수령 장소', '유효 기간 또는 사용 기한', '참여 자격·대상 조건', '체험 범위 및 제한 사항', '사후 처리·반환 정책'],
      banned_expressions: ['선착순 마감 임박', '한정 수량 단독', '지금 아니면 못 받음', '100% 당첨 보장', '공짜', '묻지도 따지지도 않고'],
      recommended_expressions: ['선착순 제공', '사전 등록 시 증정', '체험 기회 제공', '특별 사은 이벤트'],
      tone_guide: '체험과 증정의 가치를 진정성 있게 전달합니다. 수량과 조건을 명확히 안내해 고객이 헛걸음하지 않도록 하며, 과도한 긴급성이나 희소성 자극은 자제합니다. 브랜드가 고객에게 경험을 선사한다는 호의적인 어투를 유지하고, 추첨·증정 결과의 공정성을 신뢰할 수 있도록 작성합니다.',
      approval_process: ['PM사 캠페인 매니저', '파트너사 운영 담당자', '본사 CS/법무 담당'],
      performance: { usage_count: 9, pass_rate: 93, avg_revisions: 2.5 },
    },
    {
      id: 'membership_benefit',
      category: '멤버십 혜택',
      version: 'v2.1',
      title: '멤버십 혜택 표준 프레임',
      score: 88,
      overview: '대상 등급, 혜택 내용, 이용 방법 표기를 검수합니다. 등급 차별을 강조하는 배타적 표현을 제한합니다.',
      preview: {
        theme: 'membership',
      },
      required_fields: ['대상 회원 등급 명시', '혜택 내용·제공 범위', '이용 방법·신청 절차', '유효 기간 또는 갱신 조건', '중복 적용 가능 여부', '회원 등급별 차등 안내'],
      banned_expressions: ['VIP만', '일반 회원 제외', '특권층 전용', '당신은 안 됩니다', '등급 미달', '선택받은 자'],
      recommended_expressions: ['프리미엄 멤버 전용', 'GOLD 등급 이상', '회원 등급별 맞춤 혜택', '특별 회원 라운지'],
      tone_guide: '프리미엄한 가치는 강조하되 배타적이거나 차별적인 인상을 주지 않도록 합니다. 등급별 혜택을 안내할 때는 객관적 정보로 전달하며, 비회원이나 하위 등급 고객을 폄하하는 표현은 금지합니다. 멤버십이 고객에게 제공하는 경험과 가치에 초점을 맞추고, 우월감보다는 소속감을 자극하는 어투를 권장합니다.',
      approval_process: ['PM사 멤버십 매니저', '파트너사 제휴 담당자', '본사 브랜드/CRM 담당'],
      performance: { usage_count: 17, pass_rate: 88, avg_revisions: 3.5 },
    },
    {
      id: 'joint_promotion',
      category: '공동 프로모션',
      version: 'v2.0',
      title: '공동 프로모션 표준 프레임',
      score: 85,
      overview: '양사 로고, 비용 부담 비율, 운영 책임 범위 표기를 검수합니다. 한쪽 브랜드만 부각되지 않도록 합니다.',
      preview: {
        theme: 'joint',
      },
      required_fields: ['양사 브랜드명·로고 동등 노출', '비용 부담 주체 및 비율', '운영 책임 범위 (CS·환불 등)', '공동 캠페인 기간', '각사 기여 자산 명시', '분쟁 시 협의 절차'],
      banned_expressions: ['단독 진행', '독점 제휴', '유일한 파트너', '타사 대비 우위', '본사 주도', '주관·협찬'],
      recommended_expressions: ['공동 기획', '양사 협력 캠페인', '함께 준비한 혜택', '공동 브랜딩'],
      tone_guide: '두 브랜드가 대등한 파트너로 협력하고 있음을 분명히 드러냅니다. 한쪽 브랜드가 주도하거나 다른 쪽이 보조 역할로 보이지 않도록 표현의 비중을 균형 있게 맞춥니다. 양사가 함께 만든 가치라는 점을 강조하고, 고객이 혜택을 받는 데 있어 어느 쪽 채널에서도 동일한 경험을 할 수 있음을 전달합니다.',
      approval_process: ['PM사 캠페인 매니저', '파트너사 제휴 담당자', '양사 법무·브랜드 담당'],
      performance: { usage_count: 12, pass_rate: 85, avg_revisions: 4.1 },
    },
    {
      id: 'content_collab',
      category: '콘텐츠 협업',
      version: 'v1.5',
      title: '콘텐츠 협업 프레임',
      score: 80,
      overview: '협업 형식, 노출 채널, 저작권 귀속 표기를 검수합니다. 미공개 정보의 사전 노출을 차단합니다.',
      preview: {
        theme: 'content',
      },
      required_fields: ['협업 형식 (영상·기사·SNS 등)', '노출 채널 및 노출 기간', '저작권·2차 활용 권리', '협업자(인플루언서·매체) 정보', '광고성 표기 (#광고 #협찬)', '원본 콘텐츠 보관 조건'],
      banned_expressions: ['내돈내산', '협찬 아닙니다', '광고 X', '솔직 후기 보장', '비밀리에 공개', '단독 입수'],
      recommended_expressions: ['유료 광고 포함', '협업 콘텐츠', '브랜드 제공', '공식 콘텐츠 파트너'],
      tone_guide: '광고성 콘텐츠임을 투명하게 밝히는 것이 가장 중요합니다. 표시광고법 및 공정거래위원회 가이드를 준수하며, 협찬·광고 사실을 숨기거나 모호하게 표현하지 않습니다. 콘텐츠의 진정성과 정보 전달력을 살리되, 고객이 광고임을 명확히 인지할 수 있는 어투를 유지합니다.',
      approval_process: ['PM사 콘텐츠 담당자', '파트너사·크리에이터', '본사 법무·브랜드 담당'],
      performance: { usage_count: 5, pass_rate: 80, avg_revisions: 4.8 },
    },
    {
      id: 'channel_app_exposure',
      category: '채널/앱 노출',
      version: 'v1.2',
      title: '채널·앱 노출 표준 프레임',
      score: 86,
      overview: '노출 채널, 노출 기간, 도달 규모 예상치 표기를 검수합니다. 클릭 유도 과장 표현을 제한합니다.',
      preview: {
        theme: 'channel',
      },
      required_fields: ['노출 채널 (앱 푸시·배너·알림톡 등)', '노출 기간 및 노출 빈도', '예상 도달 규모 또는 노출 지면', '타겟 세그먼트 정의', '수신 거부·옵트아웃 안내', '성과 측정 지표 (CTR·CVR 등)'],
      banned_expressions: ['지금 클릭', '놓치면 후회', '긴급 알림', '100만 명이 본 광고', '마지막 기회', '당신만 못 받은 혜택'],
      recommended_expressions: ['회원님께 추천', '맞춤 혜택 안내', '관심 카테고리 정보', '이번 주 추천'],
      tone_guide: '고객의 알림 피로도를 고려한 절제된 어투를 유지합니다. 클릭을 유도하는 과장이나 거짓 긴급성은 사용하지 않으며, 고객이 채널을 신뢰하고 계속 받아볼 수 있도록 정보 가치를 우선시합니다. 푸시·알림톡은 짧고 명확하게, 배너는 핵심 메시지와 CTA를 분명히 전달합니다. 수신 거부 안내가 자연스럽게 노출되도록 합니다.',
      approval_process: ['PM사 채널 운영 담당자', '파트너사 마케팅 담당자', '본사 CRM·법무 담당'],
      performance: { usage_count: 14, pass_rate: 86, avg_revisions: 2.8 },
    },
  ],
}

const frameGuidelineDetails = {
  coupon_discount: {
    purpose: '할인·쿠폰 캠페인 발송 시 표시광고법 준수와 조건 명시를 동시에 만족하도록 관리합니다.',
    evidence: ['할인 정책 문서', '유효 기간 캘린더', '제외 대상 리스트', '최종 카피 파일'],
    channel: ['푸시 본문 150자 이내', '배너 PC 1920x640 / MO 750x900', '알림톡 본문 900자 이내', '(광고) 및 수신 거부 표기 필수'],
  },
  experience_gift: {
    purpose: '체험권·사은품 캠페인에서 제공 수량, 수령 방식, 유효 기간을 명확히 안내하도록 관리합니다.',
    evidence: ['제공 수량·재고 문서', '수령 장소 및 방법 안내', '유효 기간 캘린더', '사후 처리 정책'],
    channel: ['푸시 본문 150자 이내', '배너에 제공 수량 표시', '알림톡 본문 900자 이내', '수령 방법 및 기간 표기 필수'],
  },
  membership_benefit: {
    purpose: '멤버십 등급별 혜택을 안내할 때 대상 조건을 분명히 하되 배타적 인상을 줄이도록 관리합니다.',
    evidence: ['회원 등급 정책', '혜택 제공 범위 문서', '중복 적용 기준', 'CRM 발송 대상 세그먼트'],
    channel: ['푸시 제목 40자 이내', '배너에 대상 등급 표시', '알림톡 본문 900자 이내', '등급별 혜택 차등 안내 필수'],
  },
  joint_promotion: {
    purpose: '두 기업이 공동으로 진행하는 캠페인에서 브랜드 노출, 비용, 책임 범위를 균형 있게 관리합니다.',
    evidence: ['양사 로고 원본', '비용 부담 합의서', '운영 책임 분장표', '공동 캠페인 일정표'],
    channel: ['양사 로고 동일 비중 노출', '배너 CTA 주체 명시', '알림톡 공동 캠페인명 표기', '운영 책임 범위 표기 필수'],
  },
  content_collab: {
    purpose: '콘텐츠 협업 캠페인에서 광고성 표기와 저작권·2차 활용 권리를 투명하게 관리합니다.',
    evidence: ['콘텐츠 기획안', '협업자 계약 범위', '광고성 표기 가이드', '저작권·2차 활용 동의서'],
    channel: ['SNS 광고 표기 필수', '배너에 협업 주체 표시', '알림톡 링크 목적 명시', '원본 콘텐츠 보관 조건 포함'],
  },
  channel_app_exposure: {
    purpose: '앱 푸시, 배너, 알림톡 노출에서 고객 피로도를 낮추고 정보성 중심의 메시지를 유지합니다.',
    evidence: ['노출 지면 정의서', '타겟 세그먼트 기준', '발송 빈도 정책', '성과 측정 지표 정의'],
    channel: ['푸시 제목 40자·본문 150자 이내', '배너 지면별 사이즈 준수', '알림톡 본문 900자 이내', '수신 거부 안내 필수'],
  },
}

const previewSamples = {
  coupon: {
    campaign: 'CAMPAIGN · 봄맞이 회원 혜택 안내',
    title: '잠금화면에서 본 그대로',
    description: '프레임의 필수표기·금지어·권장표현이 실제 발송 화면에서 어떻게 보이는지 미리 확인합니다. 색상으로 표시된 부분은 검수 룰이 자동 감지한 영역입니다.',
    findings: [
      { tone: 'required', label: '필수', text: '(광고) 표기와 수신 거부 안내가 카피 끝에 자동 삽입되었습니다.' },
      { tone: 'banned', label: '금지', text: '본문의 "지금 클릭"은 금지 표현으로 감지되어 발송 전 수정이 필요합니다.' },
      { tone: 'recommended', label: '권장', text: '"회원님께 추천" 같은 개인화 표현이 적용되어 톤이 일관되게 유지됩니다.' },
    ],
  },
  gift: {
    campaign: 'CAMPAIGN · 체험권 사전 등록 안내',
    title: '증정 조건이 한눈에 보이게',
    description: '제공 수량, 수령 방법, 유효 기간이 고객 화면에서 누락되지 않는지 확인합니다. 과도한 긴급성 표현은 금지 영역으로 표시됩니다.',
    findings: [
      { tone: 'required', label: '필수', text: '제공 수량, 수령 장소, 유효 기간이 고객 안내 화면에 함께 표시됩니다.' },
      { tone: 'banned', label: '금지', text: '"선착순 마감 임박"은 과도한 긴급성 표현으로 수정 대상입니다.' },
      { tone: 'recommended', label: '권장', text: '"선착순 제공", "체험 기회 제공"처럼 조건 중심 표현을 권장합니다.' },
    ],
  },
  membership: {
    campaign: 'CAMPAIGN · 멤버십 등급 혜택 안내',
    title: '프리미엄은 살리고 배타감은 줄이기',
    description: '대상 등급과 혜택 조건을 명확히 보여주되, 하위 등급을 배제하는 표현은 자동으로 표시합니다.',
    findings: [
      { tone: 'required', label: '필수', text: '대상 회원 등급, 혜택 범위, 이용 방법이 같은 화면에 노출됩니다.' },
      { tone: 'banned', label: '금지', text: '"일반 회원 제외"처럼 차별적으로 보일 수 있는 표현을 감지합니다.' },
      { tone: 'recommended', label: '권장', text: '"GOLD 등급 이상"처럼 객관적 기준을 쓰는 표현을 권장합니다.' },
    ],
  },
  joint: {
    campaign: 'CAMPAIGN · 공동 프로모션 런칭',
    title: '두 브랜드가 같은 비중으로',
    description: '공동 캠페인에서 한쪽 브랜드만 주도적으로 보이지 않도록 로고, 책임 범위, 표현 균형을 미리 확인합니다.',
    findings: [
      { tone: 'required', label: '필수', text: '양사 브랜드명, 캠페인 기간, 운영 책임 범위가 검수 기준에 포함됩니다.' },
      { tone: 'banned', label: '금지', text: '"독점 제휴"처럼 한쪽 우위를 암시하는 표현을 감지합니다.' },
      { tone: 'recommended', label: '권장', text: '"양사 협력 캠페인", "함께 준비한 혜택" 표현을 권장합니다.' },
    ],
  },
  content: {
    campaign: 'CAMPAIGN · 협업 콘텐츠 공개',
    title: '광고 표기는 숨기지 않기',
    description: '협업 콘텐츠가 고객에게 노출될 때 광고성 표기와 저작권 기준이 보이는 위치를 미리 확인합니다.',
    findings: [
      { tone: 'required', label: '필수', text: '광고성 표기, 노출 채널, 저작권·2차 활용 권리를 확인합니다.' },
      { tone: 'banned', label: '금지', text: '"광고 X", "협찬 아닙니다" 같은 은폐성 표현을 감지합니다.' },
      { tone: 'recommended', label: '권장', text: '"유료 광고 포함", "협업 콘텐츠"처럼 투명한 표현을 권장합니다.' },
    ],
  },
  channel: {
    campaign: 'CAMPAIGN · 앱 노출 혜택 안내',
    title: '알림 피로도는 낮추고 정보성은 높이기',
    description: '푸시, 배너, 알림톡에서 노출 기간과 수신 거부 안내가 자연스럽게 보이는지 확인합니다.',
    findings: [
      { tone: 'required', label: '필수', text: '노출 채널, 기간, 빈도, 수신 거부 안내가 화면에 포함됩니다.' },
      { tone: 'banned', label: '금지', text: '"놓치면 후회"처럼 클릭을 압박하는 표현을 감지합니다.' },
      { tone: 'recommended', label: '권장', text: '"회원님께 추천", "이번 주 추천"처럼 정보성 표현을 권장합니다.' },
    ],
  },
}

const baseLibraryFrames = computed(() => frameCatalog.frames.map((frame) => toLibraryFrame(frame)))

const customLibraryFrames = ref([])
const libraryFrames = computed(() => [...baseLibraryFrames.value, ...customLibraryFrames.value])
const selectedLibraryFrameId = ref(frameCatalog.frames[0]?.id ?? '')
const selectedModalFrameId = ref('')
const isCreateFrameModalOpen = ref(false)
const isCreatingFrame = ref(false)
const createFrameError = ref('')
const newFrameForm = ref(createEmptyFrameForm())
const selectedCreateFrameCategory = computed(
  () => createFrameCategoryOptions.find((option) => option.value === newFrameForm.value.category) ?? createFrameCategoryOptions[0],
)
const campaignTypeOptions = computed(() => [
  '전체 캠페인 방식',
  ...new Set(libraryFrames.value.map((frame) => frame.category)),
])
const librarySortOptions = ['점수 높은순', '사용 많은순', '통과율 높은순']

const filteredLibraryFrames = computed(() => {
  const result = libraryFrames.value.filter((frame) =>
    selectedCampaignType.value === '전체 캠페인 방식' || frame.category === selectedCampaignType.value,
  )

  if (selectedLibrarySort.value === '사용 많은순') {
    return [...result].sort((a, b) => b.performance.usage_count - a.performance.usage_count)
  }

  if (selectedLibrarySort.value === '통과율 높은순') {
    return [...result].sort((a, b) => b.performance.pass_rate - a.performance.pass_rate)
  }

  return [...result].sort((a, b) => b.score - a.score)
})

const modalFrame = computed(() => libraryFrames.value.find((frame) => frame.id === selectedModalFrameId.value) ?? null)
const modalFrameCampaignHistory = computed(() => {
  if (!modalFrame.value) return []

  return plannerStore.campaigns
    .filter((campaign) => Array.isArray(campaign.campaignMethods) && campaign.campaignMethods.includes(modalFrame.value.category))
    .map((campaign) => ({
      id: campaign.id,
      title: campaign.name,
      createdAt: formatHistoryDate(campaign.createdAt),
      status: campaign.status === 'completed' ? '완료 캠페인' : campaign.status === 'active' ? '진행 중' : '초안',
      passRate: modalFrame.value.performance.pass_rate,
    }))
})

const modalUsageCount = computed(() => {
  if (!modalFrame.value) return 0
  return modalFrame.value.performance.usage_count + modalFrameCampaignHistory.value.length
})
const activePreviewChannel = computed(
  () => previewChannelOptions.find((channel) => channel.id === selectedPreviewChannel.value) ?? previewChannelOptions[0],
)
const activePreview = computed(() => {
  if (!modalFrame.value) return null

  const channels = modalFrame.value.preview.channels ?? {}
  return channels[selectedPreviewChannel.value] ?? channels.sns ?? modalFrame.value.preview
})
const activePreviewSpec = computed(() => {
  const specs = {
    sns: '1080 × 1080 · SNS 정사각',
    vert: '594 × 841 · 세로 포스터',
    kv: '1920 × 1080 · 웹 KV',
  }

  return specs[selectedPreviewChannel.value] ?? activePreview.value?.poster?.format ?? ''
})
const activePreviewVisualSpec = computed(() => {
  const specs = {
    sns: '1080 × 700',
    vert: '340 × 240',
    kv: '800 × 1080',
  }

  return specs[selectedPreviewChannel.value] ?? ''
})

function openFrameModal(frame) {
  selectedLibraryFrameId.value = frame.id
  selectedModalFrameId.value = frame.id
  selectedPreviewChannel.value = 'sns'
}

function closeFrameModal() {
  selectedModalFrameId.value = ''
}

function openCreateFrameModal() {
  createFrameError.value = ''
  newFrameForm.value = createEmptyFrameForm()
  isCreateFrameModalOpen.value = true
}

function closeCreateFrameModal() {
  if (isCreatingFrame.value) return
  isCreateFrameModalOpen.value = false
  createFrameError.value = ''
}

async function submitNewFrame() {
  if (isCreatingFrame.value) return

  createFrameError.value = ''
  isCreatingFrame.value = true

  try {
    const payload = buildCreateFramePayload()
    const createdFrame = await createFrame(payload)
    const libraryFrame = toLibraryFrame({
      ...payload,
      ...createdFrame,
      category: createdFrame.category ?? payload.category,
      preview: createdFrame.preview ?? payload.preview,
    })
    customLibraryFrames.value = [libraryFrame, ...customLibraryFrames.value]
    selectedCampaignType.value = '전체 캠페인 방식'
    selectedLibraryFrameId.value = libraryFrame.id
    selectedModalFrameId.value = libraryFrame.id
    isCreateFrameModalOpen.value = false
  } catch (error) {
    createFrameError.value = error?.message ?? '프레임 생성에 실패했습니다.'
  } finally {
    isCreatingFrame.value = false
  }
}

function createEmptyFrameForm() {
  return {
    title: '',
    category: createFrameCategoryOptions[0]?.value ?? '',
    overview: '',
    requiredFields: '',
    bannedExpressions: '',
    recommendedExpressions: '',
    toneGuide: '',
    approvalProcess: '',
  }
}

function buildCreateFramePayload() {
  const title = newFrameForm.value.title.trim()
  const categoryOption = selectedCreateFrameCategory.value ?? createFrameCategoryOptions[0]
  const category = categoryOption?.value ?? '\uACF5\uD1B5'

  return {
    category,
    version: 'v1.0',
    title,
    score: 0,
    status: 'draft',
    overview: newFrameForm.value.overview.trim(),
    preview: {
      theme: categoryOption?.theme ?? 'coupon',
    },
    required_fields: splitFrameInput(newFrameForm.value.requiredFields),
    banned_expressions: splitFrameInput(newFrameForm.value.bannedExpressions),
    recommended_expressions: splitFrameInput(newFrameForm.value.recommendedExpressions),
    tone_guide: newFrameForm.value.toneGuide.trim(),
    approval_process: splitFrameInput(newFrameForm.value.approvalProcess),
    performance: {
      usage_count: 0,
      pass_rate: 0,
      avg_revisions: 0,
    },
  }
}

function splitFrameInput(value) {
  return value
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function firstFrameItem(items, fallback) {
  return Array.isArray(items) && items.length ? items[0] : fallback
}

function getFrameTheme(frame) {
  return (
    frame.preview?.theme ??
    createFrameCategoryOptions.find((option) => option.value === frame.category)?.theme ??
    'coupon'
  )
}

const framePosterDesigns = {
  coupon: {
    campaign: 'CAMPAIGN · 쿠폰/할인 혜택 안내',
    brand: 'COUPON CLUB',
    handle: '@brand.coupon',
    description: '쿠폰·할인 프레임은 조건, 기간, 제외 대상을 눈에 잘 보이게 두는 것이 핵심입니다. 할인감은 살리되 과장 표현은 검수 하이라이트로 바로 드러납니다.',
    snsEyebrow: 'LIMITED COUPON · MEMBER ONLY',
    snsTitleTail: '\n받는\n기간 한정 쿠폰',
    snsSubtitleTail: ' 조건을 확인하고 사용할 수 있어요.',
    vertEyebrow: 'MAY COUPON CAMPAIGN',
    vertHeroTitle: 'SALE',
    vertHeroSubtitle: '회원 전용 쿠폰',
    vertTitleTail: '으로\n완성하는 할인 안내',
    vertSubtitleTail: ' 대신 할인 조건과 사용 기간을 먼저 보여줍니다.',
    kvEyebrow: 'COUPON BENEFIT · MAY',
    kvTitleTail: '으로\n할인 조건 한눈에',
    kvSubtitleTail: ' 표현은 검수 룰에 걸렸어요.',
    shapeText: '%',
  },
  gift: {
    campaign: 'CAMPAIGN · 체험권/사은품 증정 안내',
    brand: 'GIFT PASS',
    handle: '@brand.gift',
    description: '체험권·사은품 프레임은 제공 수량과 수령 방법을 선명하게 보여주는 것이 중요합니다. 이벤트 분위기는 살리되 과도한 긴급성은 바로 걸러냅니다.',
    snsEyebrow: 'GIFT EVENT · EXPERIENCE',
    snsTitleTail: '\n받는\n체험 기회',
    snsSubtitleTail: ' 조건과 수량을 확인하고 참여할 수 있어요.',
    vertEyebrow: 'EXPERIENCE GIFT EVENT',
    vertHeroTitle: 'GIFT',
    vertHeroSubtitle: '사전 등록 증정',
    vertTitleTail: '으로\n받는 체험권 안내',
    vertSubtitleTail: ' 대신 제공 수량과 수령 장소를 먼저 안내합니다.',
    kvEyebrow: 'GIFT BENEFIT · EVENT',
    kvTitleTail: '으로\n체험권 받는 방법',
    kvSubtitleTail: ' 표현은 증정 안내에서 수정 대상입니다.',
    shapeText: 'GIFT',
  },
  membership: {
    campaign: 'CAMPAIGN · 멤버십 혜택 안내',
    brand: 'MEMBERS LOUNGE',
    handle: '@brand.members',
    description: '멤버십 프레임은 프리미엄 감도와 객관적 등급 조건의 균형이 필요합니다. 배타적으로 보이는 표현은 줄이고 소속감 중심의 톤을 유지합니다.',
    snsEyebrow: 'MEMBERS ONLY · PREMIUM',
    snsTitleTail: '\n누리는\n멤버십 혜택',
    snsSubtitleTail: ' 기준으로 혜택 조건을 명확하게 확인할 수 있어요.',
    vertEyebrow: 'PREMIUM MEMBERSHIP',
    vertHeroTitle: 'VIP',
    vertHeroSubtitle: '멤버십 혜택',
    vertTitleTail: '으로\n안내하는 프리미엄 경험',
    vertSubtitleTail: ' 대신 등급 기준과 이용 방법을 객관적으로 보여줍니다.',
    kvEyebrow: 'MEMBERSHIP BENEFIT',
    kvTitleTail: '으로\n프리미엄 혜택 안내',
    kvSubtitleTail: ' 표현은 차별적으로 보일 수 있어 수정합니다.',
    shapeText: 'VIP',
  },
  joint: {
    campaign: 'CAMPAIGN · 공동 프로모션 런칭',
    brand: 'BRAND A × BRAND B',
    handle: '@brand.partner',
    description: '공동 프로모션 프레임은 두 브랜드가 같은 비중으로 보이는지가 핵심입니다. 한쪽이 주도하는 인상을 주는 표현은 검수 단계에서 잡아냅니다.',
    snsEyebrow: 'CO-BRANDING · PARTNERSHIP',
    snsTitleTail: '\n함께 만든\n공동 혜택',
    snsSubtitleTail: ' 메시지로 양사의 협력 관계를 균형 있게 보여줍니다.',
    vertEyebrow: 'JOINT PROMOTION',
    vertHeroTitle: 'A+B',
    vertHeroSubtitle: '공동 캠페인',
    vertTitleTail: '으로\n함께 준비한 혜택',
    vertSubtitleTail: ' 대신 양사 역할과 캠페인 기간을 함께 명시합니다.',
    kvEyebrow: 'PARTNER CAMPAIGN',
    kvTitleTail: '으로\n공동 브랜딩 오픈',
    kvSubtitleTail: ' 표현은 한쪽 우위로 보일 수 있어 조정합니다.',
    shapeText: 'A+B',
  },
  content: {
    campaign: 'CAMPAIGN · 협업 콘텐츠 공개',
    brand: 'CONTENT STUDIO',
    handle: '@brand.contents',
    description: '콘텐츠 협업 프레임은 광고성 표기와 저작권 기준을 숨기지 않는 것이 핵심입니다. 에디토리얼 톤은 유지하되 협찬 사실은 명확하게 드러냅니다.',
    snsEyebrow: 'COLLAB CONTENT · EDITORIAL',
    snsTitleTail: '\n공개하는\n협업 콘텐츠',
    snsSubtitleTail: ' 표기로 광고 사실과 협업 주체를 투명하게 보여줍니다.',
    vertEyebrow: 'CONTENT COLLABORATION',
    vertHeroTitle: 'POST',
    vertHeroSubtitle: '협업 콘텐츠',
    vertTitleTail: '로\n완성한 브랜드 스토리',
    vertSubtitleTail: ' 대신 광고 표기와 콘텐츠 권리 범위를 함께 안내합니다.',
    kvEyebrow: 'OFFICIAL CONTENT PARTNER',
    kvTitleTail: '로\n공개하는 협업 콘텐츠',
    kvSubtitleTail: ' 표현은 표시광고 기준에 맞지 않아 수정합니다.',
    shapeText: 'POST',
  },
  channel: {
    campaign: 'CAMPAIGN · 채널/앱 노출 안내',
    brand: 'APP CHANNEL',
    handle: '@brand.app',
    description: '채널/앱 노출 프레임은 앱 푸시, 배너, 알림톡에서 정보성은 살리고 클릭 압박은 줄이는 데 초점을 둡니다. 노출 기간과 수신 거부 안내가 자연스럽게 보여야 합니다.',
    snsEyebrow: 'APP CHANNEL · CRM',
    snsTitleTail: '\n받는\n맞춤 혜택',
    snsSubtitleTail: ' 메시지로 알림 피로도를 낮추고 정보성을 높입니다.',
    vertEyebrow: 'APP EXPOSURE CAMPAIGN',
    vertHeroTitle: 'APP',
    vertHeroSubtitle: '채널 노출',
    vertTitleTail: '으로\n도착하는 맞춤 안내',
    vertSubtitleTail: ' 대신 노출 기간과 관심 카테고리를 투명하게 보여줍니다.',
    kvEyebrow: 'APP CHANNEL EXPOSURE',
    kvTitleTail: '으로\n이번 주 추천 노출',
    kvSubtitleTail: ' 표현은 클릭 압박으로 감지됩니다.',
    shapeText: 'APP',
  },
}

function buildChannelPreview(frame, sample, channel) {
  const required = firstFrameItem(frame.required_fields ?? frame.requiredFields, '필수 조건')
  const banned = firstFrameItem(frame.banned_expressions ?? frame.bannedExpressions, '과장 표현')
  const recommended = firstFrameItem(frame.recommended_expressions ?? frame.recommendedExpressions, '권장 표현')
  const category = frame.category ?? '캠페인'
  const theme = getFrameTheme(frame)
  const design = framePosterDesigns[theme] ?? framePosterDesigns.coupon
  const campaign = design.campaign ?? sample.campaign ?? 'CAMPAIGN · 봄맞이 회원 혜택 안내'
  const description = design.description
  const findings = [
    {
      tone: 'required',
      label: '필수',
      text: `하단 디스클레이머에 브랜드명 · 기간 · 대상 · 조건과 ${required} 기준이 함께 표시됩니다.`,
    },
    {
      tone: 'banned',
      label: '금지',
      text: `메인 카피의 "${banned}" 표현은 과장 표현으로 감지되어 게시 전 교체가 필요합니다.`,
    },
    {
      tone: 'recommended',
      label: '권장',
      text: `"${recommended}" 같은 권장 표현으로 캠페인 톤을 일관되게 유지합니다.`,
    },
  ]

  if (channel === 'vert') {
    return {
      campaign,
      title: '인쇄·게시용 세로 포스터 모습',
      description,
      poster: {
        variant: theme,
        format: '594 × 841 · 세로 포스터',
        eyebrow: design.vertEyebrow,
        heroTitle: design.vertHeroTitle,
        heroSubtitle: design.vertHeroSubtitle,
        titleSegments: [
          { text: recommended, tone: 'recommended' },
          { text: design.vertTitleTail },
        ],
        subtitleSegments: [
          { text: banned, tone: 'banned' },
          { text: design.vertSubtitleTail },
        ],
        disclaimerSegments: [
          { text: `${design.brand} · ` },
          { text: '(광고)', tone: 'required' },
          { text: ` 기간 2026.05.14-2026.05.20 · ${required}\n수신거부 mybrand.kr/optout · 문의 1577-0000` },
        ],
      },
      findings,
    }
  }

  if (channel === 'kv') {
    return {
      campaign,
      title: '웹사이트 메인 KV로 노출된 모습',
      description,
      poster: {
        variant: theme,
        format: '1920 × 1080 · 웹 KV',
        eyebrow: design.kvEyebrow,
        titleSegments: [
          { text: recommended, tone: 'recommended' },
          { text: design.kvTitleTail },
        ],
        subtitleSegments: [
          { text: banned, tone: 'banned' },
          { text: design.kvSubtitleTail },
        ],
        shapeText: design.shapeText,
        disclaimerSegments: [
          { text: '(광고)', tone: 'required' },
          { text: ` ${design.brand} · 기간 5.14-5.20 · ${required} · 수신거부 mybrand.kr/optout` },
        ],
      },
      findings,
    }
  }

  return {
    campaign,
    title: '인스타그램 피드에 올라간 모습',
    description,
    poster: {
      variant: theme,
      format: '1080 × 1080 · SNS',
      eyebrow: design.snsEyebrow,
      titleSegments: [
        { text: recommended, tone: 'recommended' },
        { text: design.snsTitleTail },
      ],
      subtitleSegments: [
        { text: banned, tone: 'banned' },
        { text: design.snsSubtitleTail },
      ],
      brand: design.brand,
      handle: design.handle,
      disclaimerSegments: [
        { text: '(광고)', tone: 'required' },
        { text: ` 기간 5.14-5.20 · ${required}\n수신거부 mybrand.kr/optout` },
      ],
    },
    findings,
  }
}

function buildPreviewChannels(frame, sample, preview) {
  const overrides = preview.channels ?? {}

  return {
    sns: {
      ...buildChannelPreview(frame, sample, 'sns'),
      ...(overrides.sns ?? {}),
    },
    vert: {
      ...buildChannelPreview(frame, sample, 'vert'),
      ...(overrides.vert ?? {}),
    },
    kv: {
      ...buildChannelPreview(frame, sample, 'kv'),
      ...(overrides.kv ?? {}),
    },
  }
}

function normalizeFramePreview(frame) {
  const preview = frame.preview ?? {}
  const theme = preview.theme ?? getFrameTheme(frame)
  const sample = previewSamples[theme] ?? previewSamples.coupon
  const channels = buildPreviewChannels(frame, sample, preview)

  return {
    ...sample,
    ...preview,
    theme,
    channels,
    findings: channels.sns.findings,
  }
}

function toLibraryFrame(frame) {
  const performance = frame.performance ?? {}
  const guideline = frameGuidelineDetails[frame.id] ?? {}
  const preview = normalizeFramePreview(frame)
  const toneProfile =
    frame.tone_profile ??
    frame.toneProfile ??
    toneGuideProfiles[preview.theme] ??
    toneGuideProfiles.coupon

  return {
    ...frame,
    preview,
    purpose: frame.purpose ?? guideline.purpose ?? frame.overview ?? '',
    required_fields: frame.required_fields ?? frame.requiredFields ?? [],
    banned_expressions: frame.banned_expressions ?? frame.bannedExpressions ?? [],
    recommended_expressions: frame.recommended_expressions ?? frame.recommendedExpressions ?? [],
    tone_guide: frame.tone_guide ?? frame.toneGuide ?? '',
    tone_profile: toneProfile,
    evidence: frame.evidence ?? guideline.evidence ?? [],
    channel: frame.channel ?? guideline.channel ?? [],
    approval_process: frame.approval_process ?? frame.approvalProcess ?? [],
    performance: {
      usage_count: performance.usage_count ?? performance.usageCount ?? 0,
      pass_rate: performance.pass_rate ?? performance.passRate ?? 0,
      avg_revisions: performance.avg_revisions ?? performance.avgRevisions ?? 0,
    },
  }
}

function formatHistoryDate(value) {
  if (!value) return '날짜 없음'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value).slice(0, 10).replaceAll('-', '.')
  }

  return date.toISOString().slice(0, 10).replaceAll('-', '.')
}
</script>

<template>
  <section class="frames-page">
    <header class="frames-hero">
      <div>
        <p class="section-eyebrow">{{ t.eyebrow }}</p>
        <h2>{{ t.title }}</h2>
        <span>{{ t.desc }}</span>
      </div>
      <div class="hero-actions">
        <button type="button" class="primary-action" @click="openCreateFrameModal">
          <span class="material-symbols-outlined">add</span>
          {{ t.newFrame }}
        </button>
      </div>
    </header>

    <section class="frames-layout">
      <main class="library-column">
        <div class="library-head">
          <div class="library-title">
            <p class="section-eyebrow">FRAME LIBRARY</p>
            <h3>표준 프레임</h3>
          </div>
          <div class="library-filters">
            <select v-model="selectedCampaignType" aria-label="캠페인 방식">
              <option v-for="option in campaignTypeOptions" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
            <select v-model="selectedLibrarySort" aria-label="정렬">
              <option v-for="option in librarySortOptions" :key="option" :value="option">
                {{ option }}
              </option>
            </select>
          </div>
        </div>

        <div class="standard-frame-grid">
          <button
            v-for="frame in filteredLibraryFrames"
            :key="frame.id"
            type="button"
            class="standard-frame-card"
            :class="[{ active: selectedLibraryFrameId === frame.id }, `standard-frame-card--${frame.preview.theme}`]"
            @click="openFrameModal(frame)"
          >
            <span class="standard-frame-card__meta">
              {{ frame.category }} <i>/</i> 캠페인 검수 <i>/</i>
              <strong>표준</strong>
            </span>
            <h4>{{ frame.title }}</h4>
            <p>{{ frame.overview }}</p>
            <div class="frame-rule-preview frame-rule-preview--summary">
              <span>
                <b>필수</b>
                <strong>{{ frame.required_fields.length }}개</strong>
              </span>
              <span>
                <b>금지</b>
                <strong>{{ frame.banned_expressions.length }}개</strong>
              </span>
              <span>
                <b>권장</b>
                <strong>{{ frame.recommended_expressions.length }}개</strong>
              </span>
            </div>
            <footer>
              <span>사용 <strong>{{ frame.performance.usage_count }}회</strong></span>
              <span>통과율 <strong class="success">{{ frame.performance.pass_rate }}%</strong></span>
              <span>{{ frame.version }}</span>
            </footer>
          </button>
        </div>
      </main>
    </section>

    <div v-if="modalFrame" class="frame-modal-backdrop" @click.self="closeFrameModal">
      <section class="frame-modal" role="dialog" aria-modal="true" :aria-label="modalFrame.title">
        <header class="frame-modal__hero">
          <div>
            <nav class="frame-modal__breadcrumb" aria-label="프레임 경로">
                <span>캠페인 프레임</span>
                <span>표준 프레임</span>
                <span>{{ modalFrame.category }}</span>
                <strong>{{ modalFrame.title }}</strong>
              </nav>
            <h3>{{ modalFrame.title }}</h3>
            <div class="frame-modal__badges">
              <span>{{ modalFrame.category }}</span>
              <strong>표준 · {{ modalFrame.version }}</strong>
            </div>
          </div>
        </header>

        <div class="frame-modal__layout">
          <main class="frame-modal__main">
            <article class="frame-detail-panel frame-live-preview-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined">image</span>
                  <h4>실제 노출 미리보기</h4>
                </div>
                <p>검수 룰이 적용된 포스터 결과</p>
              </div>
              <div class="preview-channel-tabs" aria-label="미리보기 채널">
                <button
                  v-for="channel in previewChannelOptions"
                  :key="channel.id"
                  type="button"
                  :class="{ active: selectedPreviewChannel === channel.id }"
                  @click="selectedPreviewChannel = channel.id"
                >
                  {{ channel.label }}
                </button>
              </div>
              <div class="frame-live-preview" :class="`frame-live-preview--${modalFrame.preview.theme}`">
                <div
                  class="poster-stage"
                  :class="[`poster-stage--${selectedPreviewChannel}`, `poster-stage--theme-${activePreview.poster.variant}`]"
                  aria-label="캠페인 포스터 미리보기"
                >
                  <article
                    v-if="selectedPreviewChannel === 'sns'"
                    class="poster-preview poster-preview--sns"
                    :class="`poster-preview--theme-${activePreview.poster.variant}`"
                  >
                    <span class="poster-preview__tag">{{ activePreviewVisualSpec }}</span>
                    <div class="poster-preview__visual">
                      <span>메인 비주얼</span>
                      <em>{{ activePreviewVisualSpec }}</em>
                    </div>
                    <div class="poster-preview__content">
                      <p class="poster-preview__eyebrow">{{ activePreview.poster.eyebrow }}</p>
                      <h3 class="poster-preview__title">
                        <template
                          v-for="(segment, index) in activePreview.poster.titleSegments"
                          :key="`sns-title-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </h3>
                      <p class="poster-preview__subtitle">
                        <template
                          v-for="(segment, index) in activePreview.poster.subtitleSegments"
                          :key="`sns-subtitle-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </p>
                    </div>
                    <footer class="poster-preview__foot">
                      <strong>
                        {{ activePreview.poster.brand }}<br />
                        <span>{{ activePreview.poster.handle }}</span>
                      </strong>
                      <p>
                        <template
                          v-for="(segment, index) in activePreview.poster.disclaimerSegments"
                          :key="`sns-disclaimer-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </p>
                    </footer>
                  </article>

                  <article
                    v-else-if="selectedPreviewChannel === 'vert'"
                    class="poster-preview poster-preview--vert"
                    :class="`poster-preview--theme-${activePreview.poster.variant}`"
                  >
                    <span class="poster-preview__tag">{{ activePreviewVisualSpec }}</span>
                    <div class="poster-preview__visual">
                      <span>메인 비주얼</span>
                      <em>{{ activePreviewVisualSpec }}</em>
                    </div>
                    <div class="poster-preview__body">
                      <h3 class="poster-preview__title">
                        <template
                          v-for="(segment, index) in activePreview.poster.titleSegments"
                          :key="`vert-title-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </h3>
                      <p class="poster-preview__subtitle">
                        <template
                          v-for="(segment, index) in activePreview.poster.subtitleSegments"
                          :key="`vert-subtitle-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </p>
                    </div>
                    <footer class="poster-preview__disclaimer">
                      <template
                        v-for="(segment, index) in activePreview.poster.disclaimerSegments"
                        :key="`vert-disclaimer-${index}-${segment.text}`"
                      >
                        <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                        <span v-else>{{ segment.text }}</span>
                      </template>
                    </footer>
                  </article>

                  <article
                    v-else
                    class="poster-preview poster-preview--kv"
                    :class="`poster-preview--theme-${activePreview.poster.variant}`"
                  >
                    <span class="poster-preview__tag">{{ activePreviewVisualSpec }}</span>
                    <div class="poster-preview__kv-copy">
                      <p class="poster-preview__eyebrow">{{ activePreview.poster.eyebrow }}</p>
                      <h3 class="poster-preview__title">
                        <template
                          v-for="(segment, index) in activePreview.poster.titleSegments"
                          :key="`kv-title-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </h3>
                      <p class="poster-preview__subtitle">
                        <template
                          v-for="(segment, index) in activePreview.poster.subtitleSegments"
                          :key="`kv-subtitle-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </p>
                      <small>
                        <template
                          v-for="(segment, index) in activePreview.poster.disclaimerSegments"
                          :key="`kv-disclaimer-${index}-${segment.text}`"
                        >
                          <mark v-if="segment.tone" :class="`poster-mark poster-mark--${segment.tone}`">{{ segment.text }}</mark>
                          <span v-else>{{ segment.text }}</span>
                        </template>
                      </small>
                    </div>
                    <div class="poster-preview__shape">
                      <span>키 비주얼</span>
                      <em>{{ activePreviewVisualSpec }}</em>
                    </div>
                  </article>
                  <span class="poster-stage__caption">{{ activePreviewSpec }}</span>
                </div>

                <section class="frame-live-preview__copy">
                  <p class="section-eyebrow">{{ activePreview.campaign }} · {{ activePreviewChannel.label }}</p>
                  <h4>
                    {{ activePreview.title }}
                  </h4>
                  <p>{{ activePreview.description }}</p>

                  <div class="preview-legend">
                    <span class="preview-legend--required"><i></i>필수 표기</span>
                    <span class="preview-legend--banned"><i></i>금지 표현</span>
                    <span class="preview-legend--recommended"><i></i>권장 표현</span>
                  </div>

                  <div class="preview-finding-list">
                    <article
                      v-for="finding in activePreview.findings"
                      :key="finding.label"
                      :class="`preview-finding preview-finding--${finding.tone}`"
                    >
                      <strong>{{ finding.label }}</strong>
                      <p>{{ finding.text }}</p>
                    </article>
                  </div>
                </section>
              </div>
            </article>

            <article v-if="modalFrame.purpose" class="frame-detail-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined">flag</span>
                  <h4>사용 목적</h4>
                </div>
                <p>이 프레임을 쓰는 상황</p>
              </div>
              <p class="purpose-copy">{{ modalFrame.purpose }}</p>
            </article>

            <article class="frame-detail-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined">assignment</span>
                  <h4>필수 표기 항목</h4>
                </div>
                <p>캠페인 입력 시 반드시 포함</p>
              </div>
              <div class="required-item-grid">
                <span v-for="item in modalFrame.required_fields" :key="item">
                  <i class="material-symbols-outlined">check_circle</i>
                  {{ item }}
                </span>
              </div>
            </article>

            <div class="frame-split-grid">
              <article v-if="modalFrame.evidence.length" class="frame-detail-panel">
                <div class="frame-detail-panel__head">
                  <div>
                    <span class="material-symbols-outlined">folder_open</span>
                    <h4>근거 / 제출 자료</h4>
                  </div>
                  <p>검수 요청 시 첨부</p>
                </div>
                <ul class="compact-rule-list">
                  <li v-for="item in modalFrame.evidence" :key="item">
                    <span class="material-symbols-outlined">task_alt</span>
                    {{ item }}
                  </li>
                </ul>
              </article>

              <article v-if="modalFrame.channel.length" class="frame-detail-panel">
                <div class="frame-detail-panel__head">
                  <div>
                    <span class="material-symbols-outlined">ios_share</span>
                    <h4>채널 / 제출 규격</h4>
                  </div>
                  <p>발송 채널별 기준</p>
                </div>
                <ul class="compact-rule-list">
                  <li v-for="item in modalFrame.channel" :key="item">
                    <span class="material-symbols-outlined">task_alt</span>
                    {{ item }}
                  </li>
                </ul>
              </article>
            </div>

            <article class="frame-detail-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined danger">block</span>
                  <h4>금지 · 권장 표현</h4>
                </div>
                <p>검수 룰 기준</p>
              </div>
              <div class="phrase-section">
                <strong>금지 표현</strong>
                <div class="phrase-list phrase-list--danger">
                  <span v-for="item in modalFrame.banned_expressions" :key="item">× {{ item }}</span>
                </div>
              </div>
              <div class="phrase-section">
                <strong>권장 표현</strong>
                <div class="phrase-list phrase-list--success">
                  <span v-for="item in modalFrame.recommended_expressions" :key="item">✓ {{ item }}</span>
                </div>
              </div>
            </article>

            <article class="frame-detail-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined">forum</span>
                  <h4>톤앤매너 가이드</h4>
                </div>
                <p>브랜드 톤 일관성 유지</p>
              </div>
              <p class="tone-guide-copy">{{ modalFrame.tone_guide || modalFrame.tone_profile.summary }}</p>
              <div class="tone-guide-quick-rules">
                <div v-for="rule in modalFrame.tone_profile.rules" :key="rule.label">
                  <span>{{ rule.label }}</span>
                  <strong>{{ rule.value }}</strong>
                </div>
              </div>
              <div class="tone-guide-example">
                <strong>예시</strong>
                <p v-for="example in modalFrame.tone_profile.examples" :key="`${example.tone}-${example.text}`">
                  "{{ example.text }}" → {{ example.tone }}
                </p>
              </div>
            </article>

          </main>

          <aside class="frame-modal__aside">
            <article class="modal-side-panel">
              <p class="section-eyebrow">PERFORMANCE</p>
              <div class="performance-grid">
                <div>
                  <strong>{{ modalUsageCount }}</strong>
                  <span>사용 캠페인</span>
                </div>
                <div>
                  <strong class="success">{{ modalFrame.performance.pass_rate }}%</strong>
                  <span>평균 통과율</span>
                </div>
                <div>
                  <strong>{{ modalFrame.performance.avg_revisions }}</strong>
                  <span>평균 수정 횟수</span>
                </div>
                <div>
                  <strong>{{ modalFrame.version }}</strong>
                  <span>현재 버전</span>
                </div>
              </div>
            </article>

            <article class="modal-side-panel">
              <p class="section-eyebrow">FRAME HISTORY</p>
              <ul v-if="modalFrameCampaignHistory.length" class="history-list">
                <li v-for="item in modalFrameCampaignHistory" :key="item.id">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.status }} · 통과율 <em>{{ item.passRate }}%</em> · {{ item.createdAt }}</span>
                </li>
              </ul>
              <p v-else class="history-empty">
                아직 이 방식으로 생성된 캠페인이 없습니다.
              </p>
              <button type="button" class="history-link">전체 이력 보기 →</button>
            </article>

            <article class="modal-side-panel">
              <p class="section-eyebrow">FRAME SCORE</p>
              <div class="score-meter">
                <strong>{{ modalFrame.score }}</strong>
                <span>프레임 품질 점수</span>
              </div>
            </article>
          </aside>
        </div>
      </section>
    </div>

    <div v-if="isCreateFrameModalOpen" class="frame-modal-backdrop" @click.self="closeCreateFrameModal">
      <section class="frame-modal frame-modal--create" role="dialog" aria-modal="true" aria-label="새 프레임 생성">
        <header class="create-frame-head">
          <div>
            <p class="section-eyebrow">FRAME CREATE</p>
            <h3>{{ t.createFrame }}</h3>
          </div>
          <button type="button" class="icon-action" :disabled="isCreatingFrame" @click="closeCreateFrameModal">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>

        <form class="create-frame-form" @submit.prevent="submitNewFrame">
          <label>
            <span>프레임명</span>
            <input v-model="newFrameForm.title" required placeholder="예: VIP 초청 알림톡 표준 프레임" />
          </label>
          <fieldset class="create-frame-category create-frame-form__wide">
            <legend>카테고리</legend>
            <div class="create-category-grid">
              <button
                v-for="option in createFrameCategoryOptions"
                :key="option.value"
                type="button"
                class="create-category-option"
                :class="[{ active: newFrameForm.category === option.value }, `create-category-option--${option.theme}`]"
                :aria-pressed="newFrameForm.category === option.value"
                @click="newFrameForm.category = option.value"
              >
                <strong>{{ option.value }}</strong>
                <span>{{ option.description }}</span>
              </button>
            </div>
          </fieldset>
          <label class="create-frame-form__wide">
            <span>개요</span>
            <textarea v-model="newFrameForm.overview" rows="3" placeholder="이 프레임이 검수할 기준을 간단히 적어주세요." />
          </label>
          <label>
            <span>필수 입력 항목</span>
            <textarea v-model="newFrameForm.requiredFields" rows="4" placeholder="줄바꿈 또는 쉼표로 입력" />
          </label>
          <label>
            <span>금지 표현</span>
            <textarea v-model="newFrameForm.bannedExpressions" rows="4" placeholder="줄바꿈 또는 쉼표로 입력" />
          </label>
          <label>
            <span>권장 표현</span>
            <textarea v-model="newFrameForm.recommendedExpressions" rows="4" placeholder="줄바꿈 또는 쉼표로 입력" />
          </label>
          <label>
            <span>승인 프로세스</span>
            <textarea v-model="newFrameForm.approvalProcess" rows="4" placeholder="PM사 담당자, 파트너사 담당자..." />
          </label>
          <label class="create-frame-form__wide">
            <span>톤앤매너 가이드</span>
            <textarea v-model="newFrameForm.toneGuide" rows="4" placeholder="문서 작성 시 유지해야 하는 톤과 표현 기준" />
          </label>

          <p v-if="createFrameError" class="form-error">{{ createFrameError }}</p>

          <div class="create-frame-actions">
            <button type="button" class="secondary-action" :disabled="isCreatingFrame" @click="closeCreateFrameModal">
              {{ t.cancel }}
            </button>
            <button type="submit" class="primary-action" :disabled="isCreatingFrame">
              <span class="material-symbols-outlined">add</span>
              {{ isCreatingFrame ? '생성 중' : t.createFrame }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.frames-page {
  display: flex;
  width: calc(100% + 24px);
  max-width: none;
  flex-direction: column;
  gap: 10px;
  margin: -12px -12px 0;
  color: var(--text-primary);
}

.frames-hero,
.library-column {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.frames-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 16px;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.primary-action,
.secondary-action,
.icon-action {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.primary-action {
  border-color: transparent;
  background: var(--accent-color);
  color: #fff;
}

.secondary-action,
.icon-action {
  background: var(--panel-color);
  color: var(--text-primary);
}

.icon-action {
  width: 38px;
  padding: 0;
}

.primary-action:disabled,
.secondary-action:disabled,
.icon-action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.frames-hero h2,
.library-head h3 {
  margin: 4px 0 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0;
}

.frames-hero span:not(.material-symbols-outlined) {
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.55;
}

.standard-frame-card__meta {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.frames-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.library-column {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.library-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.library-title {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.library-title h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
}

.library-filters {
  display: grid;
  grid-template-columns: 160px 128px;
  gap: 8px;
}

.library-filters select {
  min-height: 32px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  padding: 0 10px;
}

.standard-frame-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 10px;
}

.standard-frame-card {
  --frame-accent: var(--accent-color);
  min-height: 138px;
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--frame-accent);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  cursor: pointer;
  text-align: left;
}

.standard-frame-card {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto;
  gap: 8px;
  padding: 12px;
}

.standard-frame-card.active {
  border-color: var(--accent-color);
  border-left-color: var(--frame-accent);
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
}

.standard-frame-card--coupon {
  --frame-accent: #7c3aed;
}

.standard-frame-card--gift {
  --frame-accent: #16a34a;
}

.standard-frame-card--membership {
  --frame-accent: #d6a84f;
}

.standard-frame-card--joint {
  --frame-accent: #0f766e;
}

.standard-frame-card--content {
  --frame-accent: #c2410c;
}

.standard-frame-card--channel {
  --frame-accent: #2563eb;
}

.standard-frame-card__meta {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.standard-frame-card__meta i {
  padding: 0 5px;
  color: var(--border-strong);
  font-style: normal;
}

.standard-frame-card__meta strong {
  color: var(--accent-strong);
}

.standard-frame-card h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 900;
  line-height: 1.3;
}

.standard-frame-card p {
  align-self: start;
  margin: 0;
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.6;
}

.standard-frame-card footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 2px;
  border-top: 1px solid var(--border-color);
  padding-top: 9px;
  color: var(--muted-text);
  font-size: 12px;
}

.standard-frame-card footer span {
  display: inline-flex;
  gap: 4px;
}

.standard-frame-card footer span + span {
  border-left: 1px solid var(--border-color);
  padding-left: 12px;
}

.standard-frame-card footer strong {
  color: var(--text-primary);
}

.standard-frame-card footer .success {
  color: var(--success-color);
}

.standard-frame-card footer time {
  margin-left: auto;
  color: var(--text-tertiary, var(--muted-text));
}

.frame-rule-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  padding: 11px 0;
}

.frame-rule-preview > span {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  gap: 4px;
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 900;
  padding: 0 9px;
}

.frame-rule-preview b {
  color: var(--muted-text);
  font-weight: 900;
}

.frame-rule-preview strong {
  color: var(--text-primary);
  font-weight: 950;
}

.frame-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  align-items: start;
  justify-items: center;
  overflow-y: auto;
  background: rgb(15 23 42 / 46%);
  padding: 18px;
}

.frame-modal {
  display: grid;
  width: min(1360px, calc(100vw - 24px));
  gap: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--app-bg, var(--panel-muted));
  box-shadow: 0 28px 80px rgb(15 23 42 / 28%);
  padding: 16px;
}

.frame-modal--create {
  width: min(760px, 100%);
}

.create-frame-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.create-frame-head h3 {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
}

.create-frame-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.create-frame-form label {
  display: grid;
  gap: 7px;
}

.create-frame-form label > span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 900;
}

.create-frame-category {
  display: grid;
  gap: 8px;
  margin: 0;
  border: 0;
  padding: 0;
}

.create-frame-category legend {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 900;
  padding: 0;
}

.create-category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.create-category-option {
  --category-accent: var(--accent-color);
  display: grid;
  gap: 5px;
  min-height: 78px;
  border: 1px solid var(--border-color);
  border-left: 4px solid transparent;
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  cursor: pointer;
  padding: 12px;
  text-align: left;
}

.create-category-option.active {
  border-color: color-mix(in srgb, var(--category-accent) 52%, var(--border-color));
  border-left-color: var(--category-accent);
  background: color-mix(in srgb, var(--category-accent) 7%, var(--panel-color));
}

.create-category-option strong {
  font-size: 14px;
  font-weight: 950;
}

.create-category-option span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 750;
  line-height: 1.45;
}

.create-category-option--coupon {
  --category-accent: #7c3aed;
}

.create-category-option--gift {
  --category-accent: #16a34a;
}

.create-category-option--membership {
  --category-accent: #d6a84f;
}

.create-category-option--joint {
  --category-accent: #0f766e;
}

.create-category-option--content {
  --category-accent: #c2410c;
}

.create-category-option--channel {
  --category-accent: #2563eb;
}

.create-frame-form input,
.create-frame-form textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  color: var(--text-primary);
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
  padding: 10px 12px;
}

.create-frame-form textarea {
  resize: vertical;
}

.create-frame-form__wide,
.form-error,
.create-frame-actions {
  grid-column: 1 / -1;
}

.form-error {
  margin: 0;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--danger-color, #ef4444) 10%, var(--panel-color));
  color: var(--danger-color, #ef4444);
  font-size: 13px;
  font-weight: 800;
  padding: 10px 12px;
}

.create-frame-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.frame-modal__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.frame-modal__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.frame-modal__breadcrumb span::after {
  content: '›';
  margin-left: 7px;
  color: var(--border-strong);
}

.frame-modal__breadcrumb strong {
  color: var(--text-secondary);
}

.frame-modal__hero h3 {
  margin: 8px 0 10px;
  color: var(--text-primary);
  font-size: 25px;
  font-weight: 900;
  letter-spacing: 0;
}

.frame-modal__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.frame-modal__badges span,
.frame-modal__badges strong {
  display: inline-flex;
  min-height: 26px;
  align-items: center;
  border-radius: var(--radius-sm);
  padding: 0 10px;
  font-size: 12px;
  font-weight: 900;
}

.frame-modal__badges span {
  background: var(--color-primary-50);
  color: var(--accent-strong);
}

.frame-modal__badges strong {
  background: color-mix(in srgb, var(--success-color) 13%, var(--panel-color));
  color: var(--success-color);
}

.history-link {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0 12px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.history-link {
  background: var(--panel-color);
  color: var(--text-primary);
}

.frame-modal__layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
}

.frame-modal__main,
.frame-modal__aside {
  display: grid;
  align-content: start;
  gap: 16px;
}

.frame-detail-panel,
.modal-side-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.frame-detail-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.frame-detail-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.frame-detail-panel__head > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.frame-detail-panel__head span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: var(--radius-sm);
  background: var(--color-primary-50);
  color: var(--accent-strong);
  font-size: 18px;
}

.frame-detail-panel__head span.danger {
  background: color-mix(in srgb, var(--danger-color, #ef4444) 10%, var(--panel-color));
  color: var(--danger-color, #ef4444);
}

.frame-detail-panel__head h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 900;
}

.frame-detail-panel__head p {
  margin: 0;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.required-item-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.required-item-grid span {
  display: flex;
  min-height: 44px;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 0 14px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.required-item-grid i {
  color: var(--accent-strong);
  font-size: 18px;
  font-style: normal;
}

.purpose-copy {
  margin: 0;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--accent-color) 6%, var(--panel-muted));
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.65;
  padding: 14px 16px;
}

.frame-split-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.compact-rule-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.compact-rule-list li {
  display: flex;
  min-height: 38px;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.35;
  padding: 8px 12px;
}

.compact-rule-list span {
  color: var(--success-color);
  font-size: 17px;
}

.phrase-section {
  display: grid;
  gap: 8px;
}

.phrase-section > strong {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 900;
}

.phrase-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.phrase-list span {
  display: inline-flex;
  min-height: 30px;
  align-items: center;
  border-radius: var(--radius-sm);
  padding: 0 10px;
  font-size: 12px;
  font-weight: 900;
}

.phrase-list--danger span {
  border: 1px solid color-mix(in srgb, var(--danger-color, #ef4444) 30%, var(--border-color));
  background: color-mix(in srgb, var(--danger-color, #ef4444) 9%, var(--panel-color));
  color: var(--danger-color, #ef4444);
}

.phrase-list--success span {
  border: 1px solid color-mix(in srgb, var(--success-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--success-color) 10%, var(--panel-color));
  color: var(--success-color);
}

.tone-guide-copy {
  margin: 0;
  border-left: 3px solid var(--accent-color);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-muted));
  padding: 14px 16px;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.tone-guide-quick-rules {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.tone-guide-quick-rules div {
  display: grid;
  gap: 4px;
  min-height: 54px;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-muted));
  padding: 11px 14px;
}

.tone-guide-quick-rules span {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 850;
}

.tone-guide-quick-rules strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.tone-guide-example {
  display: grid;
  gap: 4px;
  margin-top: 10px;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  color: var(--accent-strong);
  padding: 12px 14px;
}

.tone-guide-example strong {
  font-size: 12px;
  font-weight: 950;
}

.tone-guide-example p {
  margin: 0;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.55;
}

.frame-live-preview-panel {
  gap: 14px;
}

.preview-channel-tabs {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: -2px;
}

.preview-channel-tabs button {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  padding: 0 12px;
}

.preview-channel-tabs button.active {
  border-color: var(--text-primary);
  background: var(--text-primary);
  color: var(--panel-color);
}

.frame-live-preview {
  --preview-accent: #7c3aed;
  --preview-soft: #f3e8ff;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 34px;
  align-items: center;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--preview-accent) 18%, var(--border-color));
  border-radius: var(--radius-lg);
  background:
    radial-gradient(circle at 20% 20%, color-mix(in srgb, var(--preview-accent) 18%, transparent), transparent 50%),
    radial-gradient(circle at 82% 82%, rgb(255 177 200 / 28%), transparent 50%),
    linear-gradient(135deg, var(--preview-soft), var(--panel-color));
  min-height: 520px;
  padding: 28px;
}

.frame-live-preview--coupon {
  --preview-accent: #7c3aed;
  --preview-soft: #f5edff;
}

.frame-live-preview--gift {
  --preview-accent: #16a34a;
  --preview-soft: #ecfdf5;
}

.frame-live-preview--membership {
  --preview-accent: #c8942e;
  --preview-soft: #fff7e6;
}

.frame-live-preview--joint {
  --preview-accent: #0f766e;
  --preview-soft: #ecfeff;
}

.frame-live-preview--content {
  --preview-accent: #c2410c;
  --preview-soft: #fff1e8;
}

.frame-live-preview--channel {
  --preview-accent: #2563eb;
  --preview-soft: #eff6ff;
}

.poster-stage {
  display: flex;
  align-items: center;
  justify-content: center;
  perspective: 1200px;
}

.poster-preview {
  position: relative;
  overflow: hidden;
  background: #fff;
  box-shadow:
    0 30px 80px -20px color-mix(in srgb, var(--preview-accent) 38%, transparent),
    0 12px 24px -8px color-mix(in srgb, var(--preview-accent) 22%, transparent);
  transition: transform 0.25s ease;
}

.poster-preview:hover {
  transform: translateY(-3px);
}

.poster-preview--sns {
  display: flex;
  width: 380px;
  height: 380px;
  flex-direction: column;
  justify-content: space-between;
  border-radius: 18px;
  background: linear-gradient(140deg, #ff7aa5 0%, #a98bff 60%, var(--preview-accent) 100%);
  color: #fff;
  padding: 28px 26px;
}

.poster-preview--sns::before,
.poster-preview--sns::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  opacity: 0.58;
}

.poster-preview--sns::before {
  top: -40px;
  right: -60px;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, #ffd6e3 0%, transparent 70%);
}

.poster-preview--sns::after {
  bottom: -80px;
  left: -50px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, #c8b8ff 0%, transparent 70%);
}

.poster-preview--vert {
  display: flex;
  width: 340px;
  height: 480px;
  flex-direction: column;
  border-radius: 12px;
  background: #fbeef4;
  color: #1a1530;
}

.poster-preview--kv {
  display: grid;
  width: 540px;
  height: 340px;
  grid-template-columns: 1fr 200px;
  align-items: center;
  gap: 20px;
  border-radius: 14px;
  background: linear-gradient(105deg, #1a1530 0%, #3d2880 60%, var(--preview-accent) 100%);
  color: #fff;
  padding: 28px 32px;
}

.poster-preview--kv::before {
  content: '';
  position: absolute;
  top: -30px;
  right: -30px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: radial-gradient(circle, rgb(255 177 200 / 50%) 0%, transparent 60%);
}

.poster-preview__tag {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 3;
  border-radius: var(--radius-full);
  background: rgb(255 255 255 / 18%);
  color: currentColor;
  font-size: 10px;
  font-weight: 850;
  padding: 5px 8px;
}

.poster-preview--vert .poster-preview__tag {
  background: rgb(255 255 255 / 74%);
  color: #4b4566;
}

.poster-preview__eyebrow,
.poster-preview__title,
.poster-preview__subtitle,
.poster-preview__foot,
.poster-preview__disclaimer,
.poster-preview__kv-copy,
.poster-preview__shape {
  position: relative;
  z-index: 2;
}

.poster-preview__eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.12em;
  opacity: 0.86;
  text-transform: uppercase;
}

.poster-preview__title {
  margin: 8px 0 0;
  font-size: 34px;
  font-weight: 950;
  letter-spacing: 0;
  line-height: 1.08;
  white-space: pre-line;
}

.poster-preview__subtitle {
  max-width: 270px;
  margin: 10px 0 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  opacity: 0.92;
}

.poster-preview__foot {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  font-size: 10.5px;
  line-height: 1.5;
}

.poster-preview__foot strong {
  font-size: 13px;
  font-weight: 950;
  letter-spacing: 0.04em;
}

.poster-preview__foot span {
  font-size: 9px;
  font-weight: 650;
  opacity: 0.8;
}

.poster-preview__foot p,
.poster-preview__disclaimer,
.poster-preview__kv-copy small {
  margin: 0;
  font-size: 9.5px;
  font-weight: 650;
  line-height: 1.45;
  opacity: 0.8;
  text-align: right;
  white-space: pre-line;
}

.poster-preview__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  padding: 20px 22px;
}

.poster-preview--vert .poster-preview__title {
  color: #1a1530;
  font-size: 24px;
}

.poster-preview--vert .poster-preview__subtitle {
  color: #4b4566;
  font-size: 12.5px;
  opacity: 1;
}

.poster-preview__disclaimer {
  border-top: 1px dashed #e3dff0;
  color: #4b4566;
  padding: 12px 22px 16px;
  text-align: left;
}

.poster-preview__kv-copy {
  display: flex;
  height: 100%;
  flex-direction: column;
  gap: 10px;
}

.poster-preview--kv .poster-preview__title {
  font-size: 30px;
}

.poster-preview--kv .poster-preview__subtitle {
  font-size: 12.5px;
}

.poster-preview__kv-copy small {
  margin-top: auto;
  padding-top: 14px;
  text-align: left;
}

.poster-preview__shape {
  display: grid;
  width: 200px;
  height: 200px;
  place-items: center;
  justify-self: end;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 40%, #fae100 0 50px, #ff7aa5 51px 110px, transparent 111px);
  color: #1a1530;
  font-size: 42px;
  font-weight: 950;
  letter-spacing: 0;
}

.poster-mark {
  border-radius: 4px;
  color: inherit;
  padding: 1px 5px;
}

.poster-mark--required {
  border-bottom: 1.5px solid #a98bff;
  background: rgb(169 139 255 / 35%);
  font-weight: 850;
}

.poster-mark--banned {
  background: rgb(255 122 165 / 32%);
  text-decoration: line-through;
  text-decoration-color: rgb(255 122 165 / 85%);
  text-decoration-thickness: 1.5px;
}

.poster-mark--recommended {
  border-bottom: 1.5px solid #4ed3a6;
  background: rgb(78 211 166 / 34%);
  font-weight: 850;
}

.poster-preview--vert .poster-mark--required {
  background: rgb(107 59 255 / 18%);
  color: #4a26c4;
}

.poster-preview--vert .poster-mark--banned {
  background: rgb(224 70 124 / 15%);
  color: #b8245a;
}

.poster-preview--vert .poster-mark--recommended {
  background: rgb(26 163 122 / 16%);
  color: #0b7d5c;
}

.poster-stage {
  flex-direction: column;
  gap: 9px;
  padding: 4px 0;
}

.poster-stage__caption {
  color: #777194;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.poster-preview {
  border: 1px solid rgb(24 20 44 / 8%);
  border-radius: 4px;
  box-shadow: 0 14px 34px rgb(49 38 87 / 12%);
}

.poster-preview:hover {
  transform: none;
}

.poster-preview--sns::before,
.poster-preview--sns::after,
.poster-preview--kv::before {
  display: none;
}

.poster-preview__tag {
  display: none;
}

.poster-preview__visual {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  background:
    repeating-linear-gradient(135deg, rgb(112 96 43 / 10%) 0 1px, transparent 1px 9px),
    color-mix(in srgb, var(--preview-accent) 8%, #efe7cd);
  color: #7a735d;
  font-size: 10px;
  font-weight: 850;
  padding: 13px 16px;
}

.poster-preview__visual span::before,
.poster-preview__shape span::before {
  content: '';
}

.poster-preview__visual em,
.poster-preview__shape em {
  flex: 0 0 auto;
  font-style: normal;
  font-weight: 760;
  opacity: 0.82;
}

.poster-preview--sns {
  display: grid;
  width: 356px;
  height: 392px;
  grid-template-rows: 194px minmax(112px, 1fr) auto;
  justify-content: stretch;
  background: #f2ecd9;
  color: #0f1430;
  padding: 0;
}

.poster-preview--sns .poster-preview__content {
  min-width: 0;
  overflow-y: auto;
  padding: 14px 20px 10px;
}

.poster-preview--sns .poster-preview__eyebrow,
.poster-preview--vert .poster-preview__eyebrow {
  color: #8b7242;
  font-size: 10px;
  letter-spacing: 0.22em;
  opacity: 1;
}

.poster-preview--sns .poster-preview__title {
  color: #0f1430;
  display: -webkit-box;
  overflow: hidden;
  font-size: 22px;
  line-height: 1.12;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.poster-preview--sns .poster-preview__subtitle {
  display: -webkit-box;
  overflow: hidden;
  max-width: none;
  color: #4d4760;
  font-size: 11.5px;
  line-height: 1.6;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.poster-preview--sns .poster-preview__foot {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
  border-top: 1px solid color-mix(in srgb, var(--mock-accent, #7c3aed) 16%, transparent);
  padding: 9px 20px 14px;
}

.poster-preview--sns .poster-preview__foot strong,
.poster-preview--vert .poster-preview__foot strong {
  color: #10142f;
}

.poster-preview--sns .poster-preview__foot p {
  display: -webkit-box;
  overflow: hidden;
  color: #7a728e;
  text-align: right;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.poster-preview--vert {
  display: grid;
  width: 318px;
  height: 450px;
  grid-template-rows: 194px minmax(0, 1fr) auto;
  background: #fff;
  color: #0f1430;
}

.poster-preview--vert .poster-preview__visual {
  background:
    repeating-linear-gradient(135deg, rgb(124 58 237 / 11%) 0 1px, transparent 1px 9px),
    #f1eaff;
  color: #8178a0;
}

.poster-preview--vert .poster-preview__tag {
  color: #8178a0;
}

.poster-preview--vert .poster-preview__body {
  gap: 12px;
  min-width: 0;
  overflow: hidden;
  padding: 24px 22px 12px;
}

.poster-preview--vert .poster-preview__title {
  color: #0f1430;
  display: -webkit-box;
  overflow: hidden;
  font-size: 25px;
  line-height: 1.16;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.poster-preview--vert .poster-preview__subtitle {
  display: -webkit-box;
  overflow: hidden;
  max-width: none;
  color: #44506b;
  font-size: 11.5px;
  line-height: 1.6;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.poster-preview--vert .poster-preview__disclaimer {
  border-top: 1px solid #ece9f3;
  color: #6f6886;
  font-size: 9px;
  padding: 11px 22px 16px;
}

.poster-preview--kv {
  width: 540px;
  grid-template-columns: minmax(0, 1fr) 190px;
  align-items: stretch;
  gap: 0;
  border-left: 4px solid #f4ee00;
  border-radius: 0;
  background: #111024;
  color: #fff;
  padding: 0;
}

.poster-preview--kv > .poster-preview__tag {
  display: none;
}

.poster-preview--kv .poster-preview__kv-copy {
  justify-content: space-between;
  min-width: 0;
  overflow: hidden;
  padding: 24px 24px 20px;
}

.poster-preview--kv .poster-preview__eyebrow {
  color: #f4ee00;
  font-size: 10px;
  letter-spacing: 0.18em;
  opacity: 1;
}

.poster-preview--kv .poster-preview__title {
  color: #fff;
  display: -webkit-box;
  overflow: hidden;
  font-size: 27px;
  line-height: 1.12;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.poster-preview--kv .poster-preview__subtitle {
  display: -webkit-box;
  overflow: hidden;
  max-width: 292px;
  color: rgb(255 255 255 / 78%);
  font-size: 11px;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}


.poster-preview__kv-copy small {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 0;
  color: rgb(255 255 255 / 64%);
  font-size: 9px;
  line-height: 1.4;
  padding-top: 0;
  white-space: normal;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.poster-preview__shape {
  display: flex;
  width: auto;
  height: auto;
  flex-direction: column;
  justify-content: space-between;
  justify-self: stretch;
  border-radius: 0;
  background:
    repeating-linear-gradient(135deg, rgb(255 255 255 / 12%) 0 1px, transparent 1px 10px),
    #1a1735;
  color: rgb(255 255 255 / 70%);
  font-size: 10px;
  font-weight: 850;
  padding: 18px 16px;
  text-align: left;
}

.poster-preview__shape em {
  align-self: flex-end;
  color: rgb(255 255 255 / 46%);
}

.poster-preview--theme-coupon {
  --mock-accent: #7c3aed;
  --mock-paper: #f4ecd7;
  --mock-visual: #eee3c6;
  --mock-title: #111633;
  --mock-muted: #716785;
  --mock-kv-bg: #15112c;
  --mock-kv-visual: #241d42;
  --mock-kv-accent: #fff000;
}

.poster-preview--theme-gift {
  --mock-accent: #14905f;
  --mock-paper: #ecf8ef;
  --mock-visual: #d9f0df;
  --mock-title: #0f2f23;
  --mock-muted: #557468;
  --mock-kv-bg: #073b2f;
  --mock-kv-visual: #0f5f49;
  --mock-kv-accent: #b8f36b;
}

.poster-preview--theme-membership {
  --mock-accent: #c9942e;
  --mock-paper: #161616;
  --mock-visual: #2a2419;
  --mock-title: #fff7df;
  --mock-muted: #d8c79a;
  --mock-kv-bg: #11151f;
  --mock-kv-visual: #302616;
  --mock-kv-accent: #ffd338;
}

.poster-preview--theme-joint {
  --mock-accent: #0f766e;
  --mock-paper: #eaf8f5;
  --mock-visual: #d7f0ef;
  --mock-title: #123733;
  --mock-muted: #597b78;
  --mock-kv-bg: #102d2d;
  --mock-kv-visual: #1b1e48;
  --mock-kv-accent: #8ff1e2;
}

.poster-preview--theme-content {
  --mock-accent: #c2410c;
  --mock-paper: #fff7ed;
  --mock-visual: #ffe3c7;
  --mock-title: #351509;
  --mock-muted: #855f4e;
  --mock-kv-bg: #321007;
  --mock-kv-visual: #7c2d12;
  --mock-kv-accent: #ffb36b;
}

.poster-preview--theme-channel {
  --mock-accent: #2563eb;
  --mock-paper: #eef5ff;
  --mock-visual: #dbeafe;
  --mock-title: #10203f;
  --mock-muted: #536986;
  --mock-kv-bg: #0f172a;
  --mock-kv-visual: #162f66;
  --mock-kv-accent: #7dd3fc;
}

.poster-preview--sns,
.poster-preview--vert {
  background: var(--mock-paper, #f2ecd9);
}

.poster-preview--sns {
  border-top: 4px solid var(--mock-accent, #7c3aed);
}

.poster-preview--vert {
  border-top: 3px solid var(--mock-accent, #7c3aed);
}

.poster-preview--theme-gift.poster-preview--sns,
.poster-preview--theme-gift.poster-preview--vert {
  border-top-width: 0;
  border-left: 5px solid var(--mock-accent);
}

.poster-preview--theme-membership.poster-preview--sns,
.poster-preview--theme-membership.poster-preview--vert {
  border: 1px solid color-mix(in srgb, var(--mock-accent) 45%, #000);
}

.poster-preview--theme-content.poster-preview--sns,
.poster-preview--theme-content.poster-preview--vert {
  background:
    linear-gradient(90deg, rgb(194 65 12 / 8%) 0 1px, transparent 1px),
    linear-gradient(0deg, rgb(194 65 12 / 8%) 0 1px, transparent 1px),
    var(--mock-paper);
  background-size: 22px 22px;
}

.poster-preview--theme-channel.poster-preview--sns,
.poster-preview--theme-channel.poster-preview--vert {
  background:
    linear-gradient(90deg, rgb(37 99 235 / 9%) 0 1px, transparent 1px),
    linear-gradient(0deg, rgb(37 99 235 / 9%) 0 1px, transparent 1px),
    var(--mock-paper);
  background-size: 24px 24px;
}

.poster-preview--theme-coupon .poster-preview__visual,
.poster-preview--theme-membership .poster-preview__visual,
.poster-preview--theme-content .poster-preview__visual,
.poster-preview--theme-channel .poster-preview__visual {
  background:
    repeating-linear-gradient(135deg, color-mix(in srgb, var(--mock-accent) 18%, transparent) 0 1px, transparent 1px 9px),
    var(--mock-visual);
}

.poster-preview--theme-gift .poster-preview__visual {
  background:
    linear-gradient(90deg, transparent 46%, color-mix(in srgb, var(--mock-accent) 28%, transparent) 46% 54%, transparent 54%),
    linear-gradient(0deg, transparent 46%, color-mix(in srgb, var(--mock-accent) 24%, transparent) 46% 54%, transparent 54%),
    var(--mock-visual);
}

.poster-preview--theme-joint .poster-preview__visual {
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--mock-accent) 16%, #fff) 0 50%, #e8e7ff 50% 100%);
}

.poster-preview--theme-channel .poster-preview__visual {
  background-size: 20px 20px, auto;
}

.poster-preview--theme-coupon .poster-preview__tag,
.poster-preview--theme-coupon .poster-preview__visual,
.poster-preview--theme-coupon .poster-preview__eyebrow,
.poster-preview--theme-gift .poster-preview__tag,
.poster-preview--theme-gift .poster-preview__visual,
.poster-preview--theme-gift .poster-preview__eyebrow,
.poster-preview--theme-joint .poster-preview__tag,
.poster-preview--theme-joint .poster-preview__visual,
.poster-preview--theme-joint .poster-preview__eyebrow,
.poster-preview--theme-content .poster-preview__tag,
.poster-preview--theme-content .poster-preview__visual,
.poster-preview--theme-content .poster-preview__eyebrow,
.poster-preview--theme-channel .poster-preview__tag,
.poster-preview--theme-channel .poster-preview__visual,
.poster-preview--theme-channel .poster-preview__eyebrow {
  color: var(--mock-accent);
}

.poster-preview--theme-membership .poster-preview__tag,
.poster-preview--theme-membership .poster-preview__visual,
.poster-preview--theme-membership .poster-preview__eyebrow {
  color: #f4d47c;
}

.poster-preview--sns .poster-preview__title,
.poster-preview--vert .poster-preview__title,
.poster-preview--sns .poster-preview__foot strong,
.poster-preview--vert .poster-preview__foot strong {
  color: var(--mock-title, #0f1430);
}

.poster-preview--sns .poster-preview__subtitle,
.poster-preview--sns .poster-preview__foot p,
.poster-preview--vert .poster-preview__subtitle,
.poster-preview--vert .poster-preview__disclaimer {
  color: var(--mock-muted, #4d4760);
}

.poster-preview--kv {
  border-left-color: var(--mock-kv-accent, #f4ee00);
  background: var(--mock-kv-bg, #111024);
}

.poster-preview--kv .poster-preview__eyebrow {
  color: var(--mock-kv-accent, #f4ee00);
}

.poster-preview__shape {
  background:
    repeating-linear-gradient(135deg, rgb(255 255 255 / 13%) 0 1px, transparent 1px 10px),
    var(--mock-kv-visual, #1a1735);
}

.poster-preview--theme-gift .poster-preview__shape {
  background:
    linear-gradient(90deg, transparent 46%, rgb(255 255 255 / 16%) 46% 54%, transparent 54%),
    linear-gradient(0deg, transparent 46%, rgb(255 255 255 / 14%) 46% 54%, transparent 54%),
    var(--mock-kv-visual);
}

.poster-preview--theme-joint .poster-preview__shape {
  background: linear-gradient(90deg, #0f766e 0 50%, #4338ca 50% 100%);
}

.poster-preview--theme-content .poster-preview__shape {
  background:
    linear-gradient(90deg, rgb(255 255 255 / 12%) 0 1px, transparent 1px),
    linear-gradient(0deg, rgb(255 255 255 / 12%) 0 1px, transparent 1px),
    var(--mock-kv-visual);
  background-size: 18px 18px, 18px 18px, auto;
}

.poster-preview--theme-channel .poster-preview__shape {
  background:
    linear-gradient(90deg, rgb(125 211 252 / 13%) 0 1px, transparent 1px),
    linear-gradient(0deg, rgb(125 211 252 / 13%) 0 1px, transparent 1px),
    var(--mock-kv-visual);
  background-size: 20px 20px, 20px 20px, auto;
}

.poster-preview--theme-coupon .poster-mark--required,
.poster-preview--theme-gift .poster-mark--required,
.poster-preview--theme-membership .poster-mark--required,
.poster-preview--theme-joint .poster-mark--required,
.poster-preview--theme-content .poster-mark--required,
.poster-preview--theme-channel .poster-mark--required,
.poster-preview--theme-coupon .poster-mark--recommended,
.poster-preview--theme-gift .poster-mark--recommended,
.poster-preview--theme-membership .poster-mark--recommended,
.poster-preview--theme-joint .poster-mark--recommended,
.poster-preview--theme-content .poster-mark--recommended,
.poster-preview--theme-channel .poster-mark--recommended {
  border-bottom-color: var(--mock-accent);
  background: color-mix(in srgb, var(--mock-accent) 14%, transparent);
}

.poster-mark {
  border-radius: 2px;
  padding: 0 3px;
}

.poster-mark--required {
  border-bottom: 1.5px solid #8b75d6;
  background: rgb(139 117 214 / 16%);
}

.poster-mark--banned {
  background: rgb(232 90 139 / 16%);
  color: inherit;
}

.poster-mark--recommended {
  border-bottom: 1.5px solid #4ba585;
  background: rgb(75 165 133 / 15%);
}

.frame-live-preview__copy {
  display: grid;
  gap: 16px;
  max-width: 680px;
}

.frame-live-preview__copy .section-eyebrow {
  margin: 0;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.04em;
}

.frame-live-preview__copy h4 {
  margin: 0;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 950;
  letter-spacing: 0;
}

.frame-live-preview__copy > p:not(.section-eyebrow) {
  max-width: 660px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.7;
}

.preview-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.preview-legend span {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  gap: 7px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 900;
  padding: 0 12px;
}

.preview-legend i {
  width: 10px;
  height: 10px;
  border-radius: 4px;
}

.preview-legend--required i {
  background: color-mix(in srgb, var(--preview-accent) 55%, #fff);
}

.preview-legend--banned i {
  background: #fb7185;
}

.preview-legend--recommended i {
  background: #34d399;
}

.preview-finding-list {
  display: grid;
  gap: 10px;
}

.preview-finding {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  border: 1px solid color-mix(in srgb, var(--preview-accent) 10%, var(--border-color));
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.78);
  padding: 12px 14px;
}

.preview-finding strong {
  display: inline-flex;
  min-height: 26px;
  align-items: center;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--preview-accent) 13%, #fff);
  color: var(--preview-accent);
  font-size: 12px;
  font-weight: 950;
  padding: 0 8px;
}

.preview-finding p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
  line-height: 1.55;
}

.preview-finding--banned strong {
  background: #ffe4e6;
  color: #be123c;
}

.preview-finding--recommended strong {
  background: #d1fae5;
  color: #047857;
}

.modal-side-panel {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.performance-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.performance-grid div {
  display: grid;
  min-height: 62px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 8px;
  text-align: center;
}

.performance-grid strong {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 900;
}

.performance-grid strong.success {
  color: var(--success-color);
  font-style: normal;
}

.performance-grid span {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 800;
}

.history-list {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.history-list li {
  display: grid;
  gap: 5px;
  border-bottom: 1px solid var(--border-color);
  padding: 10px 0;
}

.history-list strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
}

.history-list span,
.history-empty {
  color: var(--muted-text);
  font-size: 12px;
  line-height: 1.45;
}

.history-list em {
  color: var(--success-color);
  font-style: normal;
  font-weight: 900;
}

.history-empty {
  margin: 0;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 12px;
}

.history-link {
  min-height: 30px;
  border: 0;
  color: var(--accent-strong);
}

.score-meter {
  display: grid;
  min-height: 92px;
  place-items: center;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-muted));
  color: var(--accent-strong);
  text-align: center;
}

.score-meter strong {
  font-size: 34px;
  font-weight: 900;
}

.score-meter span {
  color: var(--muted-text);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1180px) {
  .frames-layout {
    grid-template-columns: 1fr;
  }

  .frame-modal__layout {
    grid-template-columns: 1fr;
  }

  .frame-modal__aside {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .frame-live-preview {
    grid-template-columns: 1fr;
  }

  .poster-stage {
    justify-self: center;
  }
}

@media (max-width: 780px) {
  .create-category-grid {
    grid-template-columns: 1fr;
  }

  .frames-hero,
  .library-head,
  .frame-modal__hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }

  .hero-actions .primary-action {
    width: 100%;
  }

  .frame-modal-backdrop {
    padding: 10px;
  }

  .frame-modal {
    padding: 14px;
  }

  .create-frame-form {
    grid-template-columns: 1fr;
  }

  .frame-modal__aside,
  .required-item-grid,
  .frame-split-grid {
    grid-template-columns: 1fr;
  }

  .frame-live-preview {
    gap: 18px;
    min-height: auto;
    padding: 14px;
  }

  .poster-preview--sns {
    width: min(100%, 320px);
    height: 374px;
    grid-template-rows: 170px minmax(112px, 1fr) auto;
    padding: 0;
  }

  .poster-preview--vert {
    width: min(100%, 300px);
    height: 424px;
  }

  .poster-preview--kv {
    width: min(100%, 320px);
    height: auto;
    min-height: 360px;
    grid-template-columns: 1fr;
  }

  .poster-preview__title {
    font-size: 28px;
  }

  .poster-preview--kv .poster-preview__title,
  .poster-preview--vert .poster-preview__title {
    font-size: 22px;
  }

  .poster-preview__shape {
    width: auto;
    height: 160px;
    justify-self: stretch;
    font-size: 10px;
  }

  .tone-guide-quick-rules {
    grid-template-columns: 1fr;
  }

  .frame-live-preview__copy h4 {
    font-size: 22px;
  }

  .preview-finding {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .frame-detail-panel {
    padding: 16px;
  }

  .frame-detail-panel__head {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .library-title {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .library-filters {
    grid-template-columns: 1fr;
    width: 100%;
  }

  .standard-frame-grid {
    grid-template-columns: 1fr;
  }

  .standard-frame-card footer {
    align-items: flex-start;
    flex-direction: column;
    gap: 7px;
  }

  .standard-frame-card footer span + span {
    border-left: 0;
    padding-left: 0;
  }

  .standard-frame-card footer time {
    margin-left: 0;
  }
}
</style>
