<script setup>
import { computed, ref } from 'vue'
import { usePlannerStore } from '@/stores/planner'

const plannerStore = usePlannerStore()

const t = {
  eyebrow: '\uCEA0\uD398\uC778 \uC2E4\uD589 \uAE30\uC900',
  title: '\uCEA0\uD398\uC778 \uD504\uB808\uC784 \uB77C\uC774\uBE0C\uB7EC\uB9AC',
  desc: '\uBCF8\uC0AC, \uD611\uB825\uC0AC, \uB300\uD589\uC0AC\uAC00 \uAC19\uC740 \uAE30\uC900\uC73C\uB85C \uCEA0\uD398\uC778 \uBB38\uC11C\uB97C \uC791\uC131\uD558\uACE0 \uAC80\uC218\uD558\uB294 \uC2E4\uD589 \uAD6C\uC870\uC785\uB2C8\uB2E4.',
  all: '\uC804\uCCB4',
  standardZone: '\uD45C\uC900 \uD504\uB808\uC784\uC874',
  referenceZone: '\uB808\uD37C\uB7F0\uC2A4 \uAE30\uBC18 \uD504\uB808\uC784\uC874',
  draftZone: '\uAC80\uD1A0 \uC911 \uD504\uB808\uC784\uC874',
  standardDesc: '\uBCF8\uC0AC \uC2B9\uC778\uC744 \uAC70\uCE5C \uACF5\uC2DD \uAE30\uC900',
  referenceDesc: '\uC644\uB8CC \uCEA0\uD398\uC778 \uC0B0\uCD9C\uBB3C\uC5D0\uC11C \uCD94\uCD9C\uD55C \uC7AC\uC0AC\uC6A9 \uAD6C\uC870',
  draftDesc: '\uC544\uC9C1 \uAC80\uD1A0\uB098 \uC2B9\uC778\uC774 \uD544\uC694\uD55C \uD504\uB808\uC784',
  detail: '\uD504\uB808\uC784 \uAD6C\uC131',
  structureInfo: '\uD504\uB808\uC784 \uAD6C\uC131 \uBCF4\uAE30',
  close: '\uB2EB\uAE30',
  purpose: '\uC0AC\uC6A9 \uBAA9\uC801',
  required: '\uD544\uC218 \uC785\uB825 \uD56D\uBAA9',
  tone: '\uC0AC\uB0B4 \uD1A4\uC564\uB9E4\uB108',
  banned: '\uAE08\uC9C0\uC5B4 / \uAE08\uC9C0 \uD45C\uD604',
  evidence: '\uADFC\uAC70 / \uC81C\uCD9C \uC790\uB8CC',
  channel: '\uCC44\uB110 / \uC81C\uCD9C \uADDC\uACA9',
  review: '\uAC80\uC218 \uB2E8\uACC4',
  approver: '\uC2B9\uC778 \uAD8C\uD55C',
  reuse: '\uC7AC\uC0AC\uC6A9 \uAE30\uC900',
  judge: 'AI \uD310\uC0AC',
  judgeDesc: '\uBB38\uC11C\uB97C \uC62C\uB9AC\uBA74 \uC120\uD0DD\uD55C \uD504\uB808\uC784\uC758 \uD544\uC218 \uD56D\uBAA9, \uD1A4\uC564\uB9E4\uB108, \uAE08\uC9C0\uC5B4, \uC81C\uCD9C \uADDC\uACA9\uC744 \uAE30\uC900\uC73C\uB85C \uC801\uD569\uC131\uC744 \uD310\uC815\uD569\uB2C8\uB2E4.',
  upload: '\uBB38\uC11C \uC5C5\uB85C\uB4DC',
  waiting: '\uAC80\uD1A0 \uB300\uAE30',
  pass: '\uC801\uD569',
  caution: '\uBCF4\uC644 \uD544\uC694',
  apply: '\uCEA0\uD398\uC778\uC5D0 \uC801\uC6A9',
  makeStandard: '\uD45C\uC900\uC73C\uB85C \uC2B9\uACA9',
  createFromReference: '\uB808\uD37C\uB7F0\uC2A4\uC5D0\uC11C \uC0DD\uC131',
}

const frames = [
  {
    id: 'vip-talk-standard',
    zone: 'standard',
    type: '\uAC80\uC218 \uC694\uCCAD',
    name: 'VIP \uCD08\uCCAD \uC54C\uB9BC\uD1A1 \uD45C\uC900 \uD504\uB808\uC784',
    status: '\uACF5\uC2DD',
    version: 'v2.1',
    used: 18,
    updatedAt: '2026.04.24',
    score: 96,
    summary: 'VIP \uACE0\uAC1D \uB300\uC0C1 \uC54C\uB9BC\uD1A1\uC758 \uBB38\uAD6C, \uD61C\uD0DD \uD45C\uAE30, \uAC80\uC218 \uB77C\uC6B0\uD305\uC744 \uD45C\uC900\uD654\uD569\uB2C8\uB2E4.',
    purpose: '\uCD08\uCCAD \uCF58\uD150\uCE20\uAC00 \uBE0C\uB79C\uB4DC \uD1A4\uACFC \uD61C\uD0DD \uACE0\uC9C0 \uAE30\uC900\uC744 \uB3D9\uC2DC\uC5D0 \uB9CC\uC871\uD558\uB3C4\uB85D \uAD00\uB9AC',
    required: ['\uB300\uC0C1 \uACE0\uAC1D\uAD70', '\uCD08\uCCAD \uD61C\uD0DD', '\uC774\uC6A9 \uAE30\uAC04', '\uBC84\uD2BC \uB9C1\uD06C', '\uC720\uC758\uC0AC\uD56D'],
    tone: ['\uD504\uB9AC\uBBF8\uC5C4\uD558\uB418 \uACFC\uC7A5\uD558\uC9C0 \uC54A\uC74C', '\uD61C\uD0DD\uBCF4\uB2E4 \uACE0\uAC1D \uACBD\uD5D8\uC744 \uBA3C\uC800 \uC124\uBA85', '\uBCF8\uC0AC \uD1B5\uD569 \uBA54\uC2DC\uC9C0\uC640 \uD45C\uD604 \uC77C\uCE58'],
    banned: ['\uCD5C\uACE0', '\uBB34\uC870\uAC74', '\uB2E8\uB3C5 \uD61C\uD0DD', '\uD655\uC815 \uC218\uC775', '\uACFC\uB3C4\uD55C \uAE34\uAE09\uC131'],
    evidence: ['\uCD5C\uC885 \uBB38\uAD6C \uD30C\uC77C', '\uD61C\uD0DD \uC870\uAC74 \uADFC\uAC70', '\uBC1C\uC1A1 \uB300\uC0C1 \uC138\uADF8\uBA3C\uD2B8', '\uAC80\uC218 \uC694\uCCAD \uB9C1\uD06C'],
    channel: ['\uC54C\uB9BC\uD1A1 \uBCF8\uBB38 900\uC790 \uC774\uB0B4', '\uBC84\uD2BC \uCD5C\uB300 2\uAC1C', '\uD61C\uD0DD \uC870\uAC74 \uD45C\uAE30 \uD544\uC218'],
    review: ['\uB2F4\uB2F9\uC790 \uCD08\uC548', '\uD611\uB825\uC0AC \uD655\uC778', '\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uAC80\uC218', '\uCD5C\uC885 \uC2B9\uC778'],
    approver: ['\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uB9E4\uB2C8\uC800', '\uD611\uB825\uC0AC CRM \uB2F4\uB2F9'],
    reuse: ['\uB3D9\uC77C VIP \uB300\uC0C1 \uCEA0\uD398\uC778', '\uC54C\uB9BC\uD1A1 \uCC44\uB110', '\uD61C\uD0DD \uC548\uB0B4\uD615 \uCF58\uD150\uCE20'],
  },
  {
    id: 'banner-standard',
    zone: 'standard',
    type: '\uC6CC\uD06C\uC624\uB354',
    name: '\uACF5\uB3D9 \uBC30\uB108 \uC81C\uC791 \uD45C\uC900 \uD504\uB808\uC784',
    status: '\uACF5\uC2DD',
    version: 'v1.8',
    used: 11,
    updatedAt: '2026.04.22',
    score: 92,
    summary: '\uBC30\uB108 \uC81C\uC791 \uC694\uCCAD\uC5D0 \uD544\uC694\uD55C \uC0AC\uC774\uC988, CTA, \uB85C\uACE0 \uC0AC\uC6A9, \uC81C\uCD9C \uD30C\uC77C \uAE30\uC900\uC744 \uACE0\uC815\uD569\uB2C8\uB2E4.',
    purpose: '\uB300\uD589\uC0AC\uC640 \uD611\uB825\uC0AC\uAC00 \uCC98\uC74C\uBD80\uD130 \uAC19\uC740 \uC81C\uC791 \uC694\uCCAD \uAE30\uC900\uC73C\uB85C \uC0B0\uCD9C\uBB3C\uC744 \uB9CC\uB4E4\uB3C4\uB85D \uAD00\uB9AC',
    required: ['\uB178\uCD9C \uC704\uCE58', '\uC0AC\uC774\uC988', 'CTA', '\uC774\uBBF8\uC9C0 \uB808\uD37C\uB7F0\uC2A4', '\uCD5C\uC885 \uC0AC\uC6A9 \uCC44\uB110'],
    tone: ['\uBA54\uC2DC\uC9C0\uB294 \uC9E7\uACE0 \uBA85\uD655\uD558\uAC8C', '\uD61C\uD0DD\uC740 \uC870\uAC74\uACFC \uD568\uAED8 \uD45C\uAE30', '\uBE0C\uB79C\uB4DC \uC2DC\uC120 \uD750\uB984 \uC720\uC9C0'],
    banned: ['\uB85C\uACE0 \uBCC0\uD615', '\uC800\uD574\uC0C1\uB3C4 \uC774\uBBF8\uC9C0', '\uD61C\uD0DD \uC870\uAC74 \uC0DD\uB7B5', '\uBC84\uD2BC \uC5C6\uB294 CTA'],
    evidence: ['\uC81C\uC791 \uC694\uCCAD\uC11C', '\uC0AC\uC774\uC988 \uBA85\uC138', '\uB85C\uACE0 \uC6D0\uBCF8', '\uCD5C\uC885 PSD/Figma \uB9C1\uD06C'],
    channel: ['PC 1920x640', 'MO 750x900', '\uC6D0\uBCF8 \uD30C\uC77C \uC81C\uCD9C', '\uB85C\uACE0 \uC138\uC774\uD504\uC874 \uC900\uC218'],
    review: ['\uC81C\uC791 \uC694\uCCAD', '\uB514\uC790\uC778 \uAC80\uC218', '\uBB38\uAD6C \uAC80\uC218', '\uC2B9\uC778 \uC644\uB8CC'],
    approver: ['\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uB2F4\uB2F9', '\uB300\uD589\uC0AC AD'],
    reuse: ['\uACF5\uB3D9 \uBC30\uB108', '\uB79C\uB529 \uD788\uC5B4\uB85C', '\uC774\uBCA4\uD2B8 \uB178\uCD9C \uC601\uC5ED'],
  },
  {
    id: 'premium-reference',
    zone: 'reference',
    type: '\uAE30\uD68D',
    name: '\uD504\uB9AC\uBBF8\uC5C4 \uB77C\uC774\uD504\uC2A4\uD0C0\uC77C \uB808\uD37C\uB7F0\uC2A4 \uD504\uB808\uC784',
    status: '\uC2B9\uC778 \uD6C4\uBCF4',
    version: 'v0.9',
    used: 1,
    updatedAt: '2026.04.26',
    score: 78,
    summary: '\uC644\uB8CC\uB41C VIP \uD504\uB9AC\uBDF0 \uCEA0\uD398\uC778\uC5D0\uC11C \uACE0\uAC1D\uAD70, \uD61C\uD0DD, \uD611\uB825\uC0AC \uC5ED\uD560\uC744 \uCD94\uCD9C\uD55C \uD504\uB808\uC784\uC785\uB2C8\uB2E4.',
    purpose: '\uC131\uACF5 \uCEA0\uD398\uC778\uC758 \uC2E4\uD589 \uAD6C\uC870\uB97C \uB2E4\uC74C \uACF5\uB3D9 \uCEA0\uD398\uC778\uC758 \uCD08\uAE30 \uAE30\uD68D \uAE30\uC900\uC73C\uB85C \uC7AC\uC0AC\uC6A9',
    required: ['\uCEA0\uD398\uC778 \uBAA9\uC801', '\uCC38\uC5EC \uD611\uB825\uC0AC', '\uACE0\uAC1D \uC138\uADF8\uBA3C\uD2B8', '\uD575\uC2EC \uD61C\uD0DD', '\uC131\uACFC \uC9C0\uD45C'],
    tone: ['\uACC4\uC5F4\uC0AC\uBCC4 \uD61C\uD0DD\uC744 \uD558\uB098\uC758 \uACBD\uD5D8\uC73C\uB85C \uC5F0\uACB0', '\uC2E4\uD589 \uC8FC\uCCB4\uBCF4\uB2E4 \uACE0\uAC1D \uAC00\uCE58 \uC911\uC2EC', '\uD504\uB9AC\uBBF8\uC5C4 \uD1A4 \uC720\uC9C0'],
    banned: ['\uD611\uB825\uC0AC \uAC04 \uC6B0\uC5F4 \uD45C\uD604', '\uD655\uC815\uB418\uC9C0 \uC54A\uC740 \uD61C\uD0DD', '\uBE44\uAD50 \uAD11\uACE0\uC131 \uBB38\uAD6C'],
    evidence: ['\uC774\uC804 \uCEA0\uD398\uC778 \uACB0\uACFC \uBCF4\uACE0\uC11C', '\uCD5C\uC885 \uC0B0\uCD9C\uBB3C', '\uAC80\uC218 \uB85C\uADF8', '\uD611\uB825\uC0AC \uC5ED\uD560\uD45C'],
    channel: ['\uC77C\uC815\uC740 \uC8FC\uCC28 \uB2E8\uC704\uB85C \uD45C\uAE30', '\uC131\uACFC \uC9C0\uD45C 3\uAC1C \uC774\uD558', '\uD611\uB825\uC0AC\uBCC4 \uCC45\uC784\uC790 \uD544\uC218'],
    review: ['AI \uCD08\uC548', '\uBCF8\uC0AC PM \uAC80\uD1A0', '\uD611\uB825\uC0AC \uD655\uC778', '\uD45C\uC900 \uC2B9\uACA9 \uD310\uB2E8'],
    approver: ['\uBCF8\uC0AC PM', '\uD611\uB825\uC0AC \uAD00\uB9AC\uC790'],
    reuse: ['\uD504\uB9AC\uBBF8\uC5C4 \uACE0\uAC1D \uB300\uC0C1', '\uBCF5\uC218 \uD611\uB825\uC0AC \uCC38\uC5EC', '\uC624\uD504\uB77C\uC778 \uD589\uC0AC \uC5F0\uACC4'],
  },
  {
    id: 'report-draft',
    zone: 'draft',
    type: '\uACB0\uACFC \uBCF4\uACE0',
    name: '\uC131\uACFC \uAE30\uB85D \uBC0F \uC7AC\uC0AC\uC6A9 \uD504\uB808\uC784',
    status: '\uAC80\uD1A0 \uC911',
    version: 'v0.4',
    used: 2,
    updatedAt: '2026.04.23',
    score: 64,
    summary: '\uC885\uB8CC\uB41C \uCEA0\uD398\uC778\uC758 \uC131\uACFC, \uC774\uC288, \uCD5C\uC885 \uC0B0\uCD9C\uBB3C\uC744 \uB2E4\uC74C \uCEA0\uD398\uC778 \uC790\uC0B0\uC73C\uB85C \uC800\uC7A5\uD569\uB2C8\uB2E4.',
    purpose: '\uCEA0\uD398\uC778 \uC885\uB8CC \uD6C4 \uCD5C\uC885 \uC790\uB8CC\uC640 \uC131\uACFC \uADFC\uAC70\uB97C \uCD95\uC801\uD574 \uD504\uB808\uC784 \uC0DD\uC131 \uC18C\uC2A4\uB85C \uC0AC\uC6A9',
    required: ['\uC131\uACFC \uC9C0\uD45C', '\uCD5C\uC885 \uC0B0\uCD9C\uBB3C', '\uAC80\uC218 \uC774\uC288', '\uC7AC\uC0AC\uC6A9 \uD56D\uBAA9'],
    tone: ['\uC131\uACFC\uB294 \uADFC\uAC70 \uC911\uC2EC\uC73C\uB85C \uAE30\uB85D', '\uC774\uC288\uB294 \uC6D0\uC778\uACFC \uC870\uCE58\uB97C \uBD84\uB9AC', '\uC7AC\uC0AC\uC6A9 \uAC00\uB2A5\uD55C \uAE30\uC900\uB9CC \uBA85\uD655\uD788 \uD45C\uAE30'],
    banned: ['\uADFC\uAC70 \uC5C6\uB294 \uC131\uACFC \uD45C\uD604', '\uAC1C\uC778 \uD0D3 \uC911\uC2EC \uC774\uC288', '\uCD5C\uC885\uBCF8 \uB9C1\uD06C \uB204\uB77D'],
    evidence: ['\uCD5C\uC885\uBCF8 \uB9C1\uD06C', '\uC131\uACFC \uB370\uC774\uD130', '\uAC80\uC218 \uC774\uC288 \uB85C\uADF8', '\uD68C\uACE0 \uBA54\uBAA8'],
    channel: ['\uC9C0\uD45C \uADFC\uAC70 \uD544\uC218', '\uC774\uC288\uBCC4 \uC6D0\uC778/\uC870\uCE58 \uBD84\uB9AC', '\uD30C\uC77C \uB9C1\uD06C \uC720\uD6A8\uC131 \uD655\uC778'],
    review: ['\uACB0\uACFC \uC785\uB825', '\uC131\uACFC \uD655\uC778', '\uB808\uD37C\uB7F0\uC2A4 \uBD84\uB958', '\uBCF4\uAD00'],
    approver: ['\uBCF8\uC0AC PM', '\uB370\uC774\uD130 \uB2F4\uB2F9'],
    reuse: ['\uACB0\uACFC \uBCF4\uACE0', '\uB808\uD37C\uB7F0\uC2A4 \uC800\uC7A5', 'AI \uD504\uB808\uC784 \uD559\uC2B5 \uC18C\uC2A4'],
  },
]

const selectedFrameId = ref(frames[0].id)
const selectedCampaignType = ref('전체 캠페인 방식')
const selectedLibrarySort = ref('점수 높은순')
const uploadedFileName = ref('')

const frameCatalog = {
  frames: [
    {
      id: 'coupon_discount',
      category: '쿠폰/할인',
      version: 'v2.3',
      title: '쿠폰·할인 표준 프레임',
      score: 91,
      overview: '할인율, 사용 기간, 사용 조건, 제외 대상 표기를 검수합니다. "무조건", "최대 할인" 등 과장 표현을 차단합니다.',
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
      required_fields: ['노출 채널 (앱 푸시·배너·알림톡 등)', '노출 기간 및 노출 빈도', '예상 도달 규모 또는 노출 지면', '타겟 세그먼트 정의', '수신 거부·옵트아웃 안내', '성과 측정 지표 (CTR·CVR 등)'],
      banned_expressions: ['지금 클릭', '놓치면 후회', '긴급 알림', '100만 명이 본 광고', '마지막 기회', '당신만 못 받은 혜택'],
      recommended_expressions: ['회원님께 추천', '맞춤 혜택 안내', '관심 카테고리 정보', '이번 주 추천'],
      tone_guide: '고객의 알림 피로도를 고려한 절제된 어투를 유지합니다. 클릭을 유도하는 과장이나 거짓 긴급성은 사용하지 않으며, 고객이 채널을 신뢰하고 계속 받아볼 수 있도록 정보 가치를 우선시합니다. 푸시·알림톡은 짧고 명확하게, 배너는 핵심 메시지와 CTA를 분명히 전달합니다. 수신 거부 안내가 자연스럽게 노출되도록 합니다.',
      approval_process: ['PM사 채널 운영 담당자', '파트너사 마케팅 담당자', '본사 CRM·법무 담당'],
      performance: { usage_count: 14, pass_rate: 86, avg_revisions: 2.8 },
    },
  ],
}

const frameSourceMap = {
  coupon_discount: 'vip-talk-standard',
  experience_gift: 'banner-standard',
  membership_benefit: 'vip-talk-standard',
  joint_promotion: 'banner-standard',
  content_collab: 'premium-reference',
  channel_app_exposure: 'banner-standard',
}

const libraryFrames = frameCatalog.frames.map((frame) => ({
  ...frame,
  sourceFrameId: frameSourceMap[frame.id] ?? frames[0].id,
}))

const selectedLibraryFrameId = ref(libraryFrames[0].id)
const selectedModalFrameId = ref('')
const campaignTypeOptions = computed(() => [
  '전체 캠페인 방식',
  ...new Set(libraryFrames.map((frame) => frame.category)),
])
const librarySortOptions = ['점수 높은순', '사용 많은순', '통과율 높은순']

const filteredLibraryFrames = computed(() => {
  const result = libraryFrames.filter((frame) =>
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

const selectedFrame = computed(() => frames.find((frame) => frame.id === selectedFrameId.value) ?? frames[0])
const modalFrame = computed(() => libraryFrames.find((frame) => frame.id === selectedModalFrameId.value) ?? null)
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

const judgeItems = computed(() => [
  { label: t.required, result: selectedFrame.value.score >= 80 ? t.pass : t.caution },
  { label: t.tone, result: selectedFrame.value.score >= 70 ? t.pass : t.caution },
  { label: t.banned, result: selectedFrame.value.banned.length ? t.caution : t.pass },
  { label: t.channel, result: selectedFrame.value.channel.length >= 3 ? t.pass : t.caution },
])

function updateFile(event) {
  uploadedFileName.value = event.target.files?.[0]?.name ?? ''
}

function openFrameModal(frame) {
  selectedLibraryFrameId.value = frame.id
  selectedFrameId.value = frame.sourceFrameId
  selectedModalFrameId.value = frame.id
}

function closeFrameModal() {
  selectedModalFrameId.value = ''
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
            :class="{ active: selectedLibraryFrameId === frame.id }"
            @click="openFrameModal(frame)"
          >
            <span class="standard-frame-card__meta">
              {{ frame.category }} <i>/</i> 캠페인 검수 <i>/</i>
              <strong>표준</strong>
            </span>
            <h4>{{ frame.title }}</h4>
            <p>{{ frame.overview }}</p>
            <footer>
              <span>사용 <strong>{{ frame.performance.usage_count }}회</strong></span>
              <span>통과율 <strong class="success">{{ frame.performance.pass_rate }}%</strong></span>
              <span>점수 <strong>{{ frame.score }}</strong></span>
            </footer>
          </button>

        </div>
      </main>

      <aside class="judge-panel">
        <div class="judge-head">
          <span class="material-symbols-outlined">gavel</span>
          <div>
            <p class="section-eyebrow">{{ t.judge }}</p>
            <h3>{{ t.judge }}</h3>
          </div>
        </div>
        <p class="judge-desc">{{ t.judgeDesc }}</p>

        <label class="upload-box">
          <input type="file" @change="updateFile" />
          <span class="material-symbols-outlined">upload_file</span>
          <strong>{{ uploadedFileName || t.upload }}</strong>
          <small>{{ selectedFrame.name }}</small>
        </label>

        <div class="judge-score">
          <span>{{ uploadedFileName ? t.caution : t.waiting }}</span>
          <strong>{{ uploadedFileName ? selectedFrame.score : '--' }}</strong>
        </div>

        <dl class="judge-checks">
          <div v-for="item in judgeItems" :key="item.label">
            <dt>{{ item.label }}</dt>
            <dd :class="{ caution: item.result === t.caution }">{{ uploadedFileName ? item.result : t.waiting }}</dd>
          </div>
        </dl>
      </aside>
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

            <article class="frame-detail-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined danger">block</span>
                  <h4>금지 · 권장 표현</h4>
                </div>
                <p>AI 판사가 자동 감지</p>
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
              <p class="tone-guide-copy">{{ modalFrame.tone_guide }}</p>
            </article>

            <article class="frame-detail-panel">
              <div class="frame-detail-panel__head">
                <div>
                  <span class="material-symbols-outlined">approval_delegation</span>
                  <h4>승인 프로세스</h4>
                </div>
                <p>양사가 같은 기준으로 확인하는 승인 라인</p>
              </div>
              <ol class="approval-step-list">
                <li v-for="(item, index) in modalFrame.approval_process" :key="item">
                  <span>{{ index + 1 }}</span>
                  {{ item }}
                </li>
              </ol>
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
.library-column,
.judge-panel {
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

.frames-hero h2,
.library-head h3,
.judge-head h3 {
  margin: 4px 0 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0;
}

.frames-hero span:not(.material-symbols-outlined),
.frame-card p,
.zone-head p,
.judge-desc,
.upload-box small {
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.55;
}

.frame-card span,
.standard-frame-card__meta {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.frames-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(286px, 18vw, 340px);
  gap: 10px;
  align-items: start;
}

.library-column,
.judge-panel {
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
  min-height: 138px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  cursor: pointer;
  text-align: left;
}

.standard-frame-card {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: 8px;
  padding: 12px;
}

.standard-frame-card.active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 5%, var(--panel-color));
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

.zone-filter {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.zone-filter button {
  min-height: 32px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  padding: 0 12px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.zone-filter button.active {
  border-color: color-mix(in srgb, var(--accent-color) 35%, var(--border-color));
  background: var(--badge-bg);
  color: var(--badge-text);
}

.frame-zone {
  display: grid;
  gap: 10px;
}

.zone-head {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.zone-head > span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.zone-head h4,
.zone-head p {
  margin: 0;
}

.zone-head strong {
  display: inline-flex;
  min-width: 28px;
  min-height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 12px;
}

.zone-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.frame-card {
  display: grid;
  gap: 8px;
  min-height: 148px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 14px;
  text-align: left;
}

.frame-card.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
}

.frame-card strong {
  font-size: 15px;
  line-height: 1.35;
}

.frame-card div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
}

.frame-card small,
.frame-card em {
  color: var(--muted-text);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.judge-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.judge-head > span {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-100);
  color: var(--accent-strong);
}

.upload-box {
  display: grid;
  min-height: 132px;
  place-items: center;
  gap: 6px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 16px;
  text-align: center;
  cursor: pointer;
}

.upload-box input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.upload-box .material-symbols-outlined {
  color: var(--accent-strong);
  font-size: 30px;
}

.judge-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-md);
  background: var(--color-primary-50);
  padding: 12px;
  color: var(--color-primary-800);
}

.judge-score span {
  font-size: 12px;
  font-weight: 800;
}

.judge-score strong {
  font-size: 28px;
}

.judge-checks {
  display: grid;
  gap: 8px;
  margin: 0;
}

.judge-checks div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 10px 12px;
}

.judge-checks dt {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.judge-checks dd {
  margin: 0;
  color: var(--success-color);
  font-size: 12px;
  font-weight: 900;
}

.judge-checks dd.caution {
  color: var(--warning-color);
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
  padding: 28px;
}

.frame-modal {
  display: grid;
  width: min(1020px, 100%);
  gap: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--app-bg, var(--panel-muted));
  box-shadow: 0 28px 80px rgb(15 23 42 / 28%);
  padding: 22px;
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
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 16px;
}

.frame-modal__main,
.frame-modal__aside {
  display: grid;
  align-content: start;
  gap: 14px;
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

.approval-step-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.approval-step-list li {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 0 14px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.approval-step-list span {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--accent-color);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
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
}

@media (max-width: 780px) {
  .frames-hero,
  .library-head,
  .frame-modal__hero {
    flex-direction: column;
  }

  .zone-filter {
    justify-content: flex-start;
    width: 100%;
  }

  .frame-modal-backdrop {
    padding: 10px;
  }

  .frame-modal {
    padding: 14px;
  }

  .frame-modal__aside,
  .required-item-grid {
    grid-template-columns: 1fr;
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

  .zone-card-grid {
    grid-template-columns: 1fr;
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
