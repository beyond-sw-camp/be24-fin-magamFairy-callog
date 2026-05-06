<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GetCampaignIntro, SubmitCampaignProposal } from '@/api/campaigns'

const route = useRoute()
const router = useRouter()

const campaignName = ref('')
const submitting = ref(false)
const errorMsg = ref('')

const form = ref({
  asset: {
    target: '',
    type: '',
    scale: '',
    conditions: '',
  },
  benefit: {
    name: '',
    type: '',
    scale: '',
    target: '',
    cost: '',
    status: 'PENDING',
  },
  message: '',
})

onMounted(async () => {
  try {
    const data = await GetCampaignIntro(route.params.campaignId)
    campaignName.value = data?.campaignName ?? ''
  } catch (e) {
    errorMsg.value = e.message ?? '캠페인 정보를 불러오지 못했습니다.'
  }
})

async function submitProposal() {
  errorMsg.value = ''
  submitting.value = true
  try {
    await SubmitCampaignProposal(route.params.campaignId, {
      asset: { ...form.value.asset },
      benefit: { ...form.value.benefit },
      message: form.value.message,
    })
    router.push({ name: 'campaign-intro', params: { campaignId: route.params.campaignId } })
  } catch (e) {
    errorMsg.value = e.message ?? '제안서 제출에 실패했습니다.'
  } finally {
    submitting.value = false
  }
}

function cancelProposal() {
  router.back()
}
</script>

<template>
  <section class="proposal">
    <header class="proposal__header">
      <p class="proposal__eyebrow">파트너 제안서 작성</p>
      <h1>{{ campaignName || '캠페인' }} 제휴 제안</h1>
      <p class="proposal__sub">매칭에 필요한 자산/혜택 정보를 입력해 주세요. 제출 시 캠페인 매칭 데이터로 등록됩니다.</p>
    </header>

    <div v-if="errorMsg" class="proposal__error">{{ errorMsg }}</div>

    <form class="proposal__form" @submit.prevent="submitProposal">
      <fieldset class="proposal__group">
        <legend>제공 마케팅 자산</legend>
        <label>대상 (target)
          <input v-model="form.asset.target" placeholder="예: VIP 고객" required />
        </label>
        <label>유형 (type)
          <input v-model="form.asset.type" placeholder="예: 디지털 광고 채널" required />
        </label>
        <label>규모 (scale)
          <input v-model="form.asset.scale" placeholder="예: 월간 노출 50만" required />
        </label>
        <label>조건 (conditions)
          <textarea v-model="form.asset.conditions" rows="3" placeholder="제공 조건"></textarea>
        </label>
      </fieldset>

      <fieldset class="proposal__group">
        <legend>제공 혜택</legend>
        <label>혜택명
          <input v-model="form.benefit.name" placeholder="예: 객실 패키지 20% 할인" required />
        </label>
        <label>유형
          <input v-model="form.benefit.type" placeholder="예: 할인 / 적립 / 한정판" required />
        </label>
        <label>규모
          <input v-model="form.benefit.scale" placeholder="예: 1인당 최대 5만원 상당" />
        </label>
        <label>대상
          <input v-model="form.benefit.target" placeholder="예: 가족 단위 고객" />
        </label>
        <label>비용 분담
          <input v-model="form.benefit.cost" placeholder="예: 50/50 공동 부담" />
        </label>
      </fieldset>

      <fieldset class="proposal__group">
        <legend>추가 메시지 (선택)</legend>
        <label>
          <textarea v-model="form.message" rows="4" placeholder="HQ 담당자에게 전달하고 싶은 메시지"></textarea>
        </label>
      </fieldset>

      <div class="proposal__actions">
        <button type="button" class="btn btn--ghost" @click="cancelProposal">취소</button>
        <button type="submit" class="btn btn--primary" :disabled="submitting">
          {{ submitting ? '제출 중...' : '제안서 제출' }}
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
  gap: 14px;
  background: var(--panel-color);
}

.proposal__group legend {
  padding: 0 8px;
  color: var(--text-primary);
  font-weight: 700;
  font-size: 15px;
}

.proposal__group label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.proposal__group input,
.proposal__group textarea {
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
}

.proposal__group input:focus,
.proposal__group textarea:focus {
  outline: 2px solid var(--color-primary-500);
  outline-offset: -1px;
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
