<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GetCampaignIntro } from '@/api/campaigns'
import { addBenefit } from '@/api/matchingBenefits'

const route = useRoute()
const router = useRouter()

const emit = defineEmits(['close'])

const campaignName = ref('')
const submitting = ref(false)
const errorMsg = ref('')

const form = ref({
  benefit: {
    // [기본 정보]
    name: '',
    type: '',
    description: '',
    
    // [규모·재고]
    quantity: '',
    quantityUnit: '개',
    valuePerPerson: '',
    
    // [기간]
    periodStart: '',
    periodEnd: '',
    alwaysNegotiable: false,
    prepDays: '',
    
    // [대상]
    targetAudience: '',
    expectedReach: '',
    
    // [비용 부담]
    costBearer: '',
    costPartnerPercent: '',
    costOursPercent: '',
    costDetails: '',
    
    // [운영 조건]
    exposureChannels: '',
    requiredCollaborations: '',
    conditions: '',
    
    // [연결 자산]
    desiredAssets: '',
    autoRecommend: false,
    
    // [담당자]
    managerName: '',
    managerEmail: '',
    managerPhone: '',

    status: 'PENDING',
  }
})

// 총 환산 가치 자동 계산
const totalValue = computed(() => {
  const q = Number(form.value.benefit.quantity) || 0
  const v = Number(form.value.benefit.valuePerPerson) || 0
  return q * v
})

// 공동 부담 비율 자동 계산 편의 함수
function updateJointPercent(type) {
  if (type === 'partner') {
    const p = Number(form.value.benefit.costPartnerPercent)
    if (p >= 0 && p <= 100) form.value.benefit.costOursPercent = 100 - p
  } else {
    const o = Number(form.value.benefit.costOursPercent)
    if (o >= 0 && o <= 100) form.value.benefit.costPartnerPercent = 100 - o
  }
}

onMounted(async () => {
  try {
    const data = await GetCampaignIntro(route.params.campaignId)
    campaignName.value = data?.campaignName ?? ''
    form.value.benefit.campaignIdx = data.campaignIdx
  } catch (e) {
    errorMsg.value = e.message ?? '캠페인 정보를 불러오지 못했습니다.'
  }
}) 

async function submitBenefit() {
  errorMsg.value = ''
  submitting.value = true

  try {
    // 자동 계산된 총 환산 가치도 전송이 필요하다면 포함 (선택)
    await addBenefit({
      ...form.value.benefit,
      totalValue: totalValue.value
    })

    emit('close')
  } catch (e) {
    errorMsg.value = e.message ?? '혜택 등록에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

function cancelSubmit() {
  emit('close')
}
</script>

<template>
  <section class="proposal">
    <header class="proposal__header">
      <p class="proposal__eyebrow">혜택 제안서 작성</p>
      <h1>{{ campaignName || '캠페인' }} 혜택 제안하기</h1>
      <p class="proposal__sub">매칭에 필요한 혜택 정보를 입력해 주세요.</p>
    </header>

    <div v-if="errorMsg" class="proposal__error">{{ errorMsg }}</div>

    <form class="proposal__form" @submit.prevent="submitBenefit">

      <!-- [기본 정보] -->
      <fieldset class="proposal__group">
        <legend>기본 정보</legend>
        <label>혜택명 <span class="required">*</span>
          <input v-model="form.benefit.name" placeholder="예: 객실 패키지 20% 할인" required />
        </label>
        <label>혜택 유형 <span class="required">*</span>
          <select v-model="form.benefit.type" required>
            <option value="" disabled>선택해 주세요</option>
            <option value="할인/쿠폰">할인/쿠폰</option>
            <option value="적립/포인트">적립/포인트</option>
            <option value="한정판/굿즈">한정판/굿즈</option>
            <option value="체험/시승">체험/시승</option>
            <option value="콘텐츠/이벤트">콘텐츠/이벤트</option>
            <option value="멤버십 혜택">멤버십 혜택</option>
            <option value="기타">기타</option>
          </select>
        </label>
        <label>혜택 설명
          <textarea v-model="form.benefit.description" rows="3" placeholder="예: 객실 2박 이상 예약 시 20% 할인, 조식 무료 제공"></textarea>
        </label>
      </fieldset>

      <!-- [규모·재고] -->
      <fieldset class="proposal__group">
        <legend>규모·재고</legend>
        <label>제공 수량 <span class="required">*</span>
          <div class="input-inline">
            <input type="number" v-model="form.benefit.quantity" placeholder="숫자 입력" required min="1" />
            <select v-model="form.benefit.quantityUnit" class="unit-select">
              <option value="개">개</option>
              <option value="건">건</option>
              <option value="명">명</option>
              <option value="장">장</option>
              <option value="회">회</option>
            </select>
          </div>
        </label>
        <label>1인당 가치
          <div class="input-inline">
            <input type="number" v-model="form.benefit.valuePerPerson" placeholder="숫자 입력" min="0" />
            <span class="unit-text">원</span>
          </div>
        </label>
        <label>총 환산 가치 <span class="text-muted">(자동 계산)</span>
          <div class="input-inline">
            <input type="text" :value="totalValue.toLocaleString()" disabled class="input-disabled" />
            <span class="unit-text">원</span>
          </div>
        </label>
      </fieldset>

      <!-- [기간] -->
      <fieldset class="proposal__group">
        <legend>기간</legend>
        <label>유효 기간 <span class="required" v-if="!form.benefit.alwaysNegotiable">*</span>
          <div class="date-group" v-if="!form.benefit.alwaysNegotiable">
            <input type="date" v-model="form.benefit.periodStart" :required="!form.benefit.alwaysNegotiable" />
            <span>~</span>
            <input type="date" v-model="form.benefit.periodEnd" :required="!form.benefit.alwaysNegotiable" />
          </div>
          <label class="check-label">
            <input type="checkbox" v-model="form.benefit.alwaysNegotiable" />
            상시 협의 (기간 미정)
          </label>
        </label>
        <label>준비 필요 기간
          <div class="input-inline">
            <input type="number" v-model="form.benefit.prepDays" placeholder="숫자 입력" min="0" />
            <span class="unit-text">일</span>
          </div>
        </label>
      </fieldset>

      <!-- [대상] -->
      <fieldset class="proposal__group">
        <legend>대상</legend>
        <label>타겟 고객층
          <input v-model="form.benefit.targetAudience" placeholder="예: 2030 액티브 레저층, 가족 단위 고객, VIP 회원" />
        </label>
        <label>예상 도달 규모
          <div class="input-inline">
            <input type="number" v-model="form.benefit.expectedReach" placeholder="숫자 입력" min="0" />
            <span class="unit-text">명</span>
          </div>
        </label>
      </fieldset>

      <!-- [비용 부담] -->
      <fieldset class="proposal__group">
        <legend>비용 부담</legend>
        <label>부담 주체 <span class="required">*</span>
          <div class="radio-group">
            <label class="radio-label">
              <input type="radio" value="PARTNER" v-model="form.benefit.costBearer" required /> 파트너 전액 부담
            </label>
            <label class="radio-label">
              <input type="radio" value="OURS" v-model="form.benefit.costBearer" required /> 우리 측 전액 부담
            </label>
            <label class="radio-label">
              <input type="radio" value="JOINT" v-model="form.benefit.costBearer" required /> 공동 부담
            </label>
          </div>
          <div v-if="form.benefit.costBearer === 'JOINT'" class="joint-input-group">
            <span>파트너</span>
            <input type="number" v-model="form.benefit.costPartnerPercent" @input="updateJointPercent('partner')" placeholder="0" min="0" max="100" />
            <span>% : 우리</span>
            <input type="number" v-model="form.benefit.costOursPercent" @input="updateJointPercent('ours')" placeholder="0" min="0" max="100" />
            <span>%</span>
          </div>
        </label>
        <label>비용 부담 상세
          <textarea v-model="form.benefit.costDetails" rows="2" placeholder="예: 혜택 원가는 파트너 부담, 배송비/디자인비는 우리 측 부담"></textarea>
        </label>
      </fieldset>

      <!-- [운영 조건] -->
      <fieldset class="proposal__group">
        <legend>운영 조건</legend>
        <label>노출 채널
          <textarea v-model="form.benefit.exposureChannels" rows="2" placeholder="예: 자사 앱 푸시, 인스타그램 피드/스토리, 카카오 알림톡, 매장 POP"></textarea>
        </label>
        <label>필요 협업 산출물
          <textarea v-model="form.benefit.requiredCollaborations" rows="2" placeholder="예: 공동 랜딩페이지, 알림톡 문구, 쿠폰 코드 발급, SNS 카드뉴스 3종"></textarea>
        </label>
        <label>사용 조건/제약
          <textarea v-model="form.benefit.conditions" rows="3" placeholder="예: 1인 1회 한정, 최소 구매 5만원 이상, 평일에만 사용 가능, 다른 쿠폰과 중복 불가"></textarea>
        </label>
      </fieldset>

      <!-- [연결 자산] -->
      <fieldset class="proposal__group">
        <legend>연결 자산</legend>
        <label>연결 희망 자산
          <textarea v-model="form.benefit.desiredAssets" rows="2" :disabled="form.benefit.autoRecommend" placeholder="예: 갤러리아 VIP 고객 DB, 호텔 객실 패키지, 자사 앱 푸시"></textarea>
          <label class="check-label mt-2">
            <input type="checkbox" v-model="form.benefit.autoRecommend" />
            추천 받기 (시스템이 자동 매칭)
          </label>
        </label>
      </fieldset>

      <!-- [담당자] -->
      <fieldset class="proposal__group">
        <legend>담당자</legend>
        <label>담당자 이름 <span class="required">*</span>
          <input v-model="form.benefit.managerName" placeholder="담당자 성함 입력" required />
        </label>
        <label>이메일 <span class="required">*</span>
          <input type="email" v-model="form.benefit.managerEmail" placeholder="example@email.com" required />
        </label>
        <label>연락처
          <input type="tel" v-model="form.benefit.managerPhone" placeholder="010-0000-0000" />
        </label>
      </fieldset>

      <div class="proposal__actions">
        <button type="button" class="btn btn--ghost" @click="cancelSubmit">취소</button>
        <button type="submit" class="btn btn--primary" :disabled="submitting">
          {{ submitting ? '등록 중...' : '혜택 등록하기' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.proposal {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.proposal__header {
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 16px;
}

.proposal__eyebrow {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.proposal__header h1 {
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.proposal__sub {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.5;
}

.proposal__error {
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #EF4444;
  border-radius: 8px;
  color: #B91C1C;
  font-size: 14px;
}

.proposal__form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.proposal__group {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--panel-color);
}

.proposal__group legend {
  padding: 0 8px;
  color: var(--text-primary);
  font-weight: 700;
  font-size: 16px;
}

.proposal__group label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.required {
  color: #EF4444;
  margin-left: 2px;
}

.text-muted {
  font-weight: normal;
  color: #9CA3AF;
  font-size: 12px;
}

.proposal__group input:not([type="radio"]):not([type="checkbox"]),
.proposal__group select,
.proposal__group textarea {
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  width: 100%;
  box-sizing: border-box;
}

.proposal__group input:focus,
.proposal__group select:focus,
.proposal__group textarea:focus {
  outline: 2px solid var(--color-primary-500);
  outline-offset: -1px;
}

.input-disabled {
  background-color: #F3F4F6 !important;
  color: #6B7280 !important;
  cursor: not-allowed;
}

/* Inline Inputs Layout */
.input-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit-select {
  width: 100px !important;
}

.unit-text {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
}

/* Dates Layout */
.date-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.date-group input {
  flex: 1;
}

/* Checkboxes and Radios */
.check-label,
.radio-label {
  display: flex;
  flex-direction: row !important;
  align-items: center;
  gap: 6px;
  font-weight: normal !important;
  cursor: pointer;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.mt-2 {
  margin-top: 8px;
}

.joint-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding-left: 24px;
  font-size: 13px;
}

.joint-input-group input {
  width: 70px !important;
  text-align: right;
}

.proposal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.btn {
  min-height: 40px;
  padding: 0 18px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn--ghost {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.btn--primary {
  background: var(--color-primary-500);
  color: #fff;
}

.btn--primary:hover:not(:disabled) {
  background: var(--color-primary-600);
}

.btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>