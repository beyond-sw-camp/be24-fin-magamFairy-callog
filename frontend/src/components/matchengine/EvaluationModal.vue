<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  isOpen: {
    type: Boolean,
    required: true,
  },
  campaignInfo: {
    type: Object,
    required: true,
  },
  proposals: {
    type: Array,
    required: true,
  },
  // 실제 데이터에 id가 없으므로 쓰지 않아도 무방합니다.
  initialSelectedId: {
    type: [Number, String, null],
    default: null,
  },
})

const emit = defineEmits(['update:isOpen', 'submit'])

// 단일 선택용 '객체' 저장 (id 대신 객체 통째로)
const selectedBenefit = ref(null)

// 모달이 열릴 때마다 선택 초기화
watch(
  () => props.isOpen,
  (newVal) => {
    if (newVal) {
      selectedBenefit.value = null
    }
  }
)

function closeModal() {
  emit('update:isOpen', false)
}

function submitRequest() {
  // 선택된 객체가 없는 경우 검증
  if (!selectedBenefit.value) {
    alert('평가할 혜택을 선택해주세요.')
    return
  }

  // 평가 요청에 필요한 정보 담기
  const submit = {
    benefitIdx: selectedBenefit.value.idx,
  }

  // console.log(submit)
  emit('submit', submit)

  closeModal()
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content">
      <header class="modal-header">
        <h4>평가 요청하기</h4>
        <button class="modal-close" @click="closeModal">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </header>

      <div class="modal-body">
        <section class="modal-section">
          <h5>캠페인 정보</h5>
          <div class="campaign-info">
            <p><span>캠페인명</span> <strong>{{ campaignInfo.title }}</strong></p>
            <p><span>매칭 자산</span> {{ campaignInfo.asset }}</p>
            <p><span>타깃 고객</span> {{ campaignInfo.target }}</p>
          </div>
        </section>

        <section class="modal-section">
          <h5>제안된 혜택 선택</h5>
          <p class="modal-desc">이 캠페인에 적용할 혜택을 하나만 선택해주세요.</p>
          <div class="benefit-check-list">
            <!-- 고유 id가 없으므로 배열의 index를 key로 사용 -->
            <label v-for="(proposal, index) in proposals" :key="index" class="check-item">
              <!-- value를 객체 자체(proposal)로 바인딩 -->
              <input type="radio" name="benefitSelection" :value="proposal" v-model="selectedBenefit" />
              <div class="check-item__info">
                <strong>{{ proposal.managerName || proposal.partner }}</strong>
                <span>{{ proposal.name }}</span>
                <small>{{ proposal.type }} · {{ proposal.targetAudience || proposal.target }}</small>
              </div>
            </label>
          </div>
        </section>
      </div>

      <footer class="modal-footer">
        <button type="button" @click="closeModal">취소</button>
        <button type="button" class="primary" @click="submitRequest">
          선택한 혜택 평가 요청
        </button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}
.modal-content {
  background: var(--benefit-surface, #ffffff);
  width: 100%;
  max-width: 460px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem;
  border-bottom: 1px solid var(--benefit-line, #ecedf0);
}
.modal-header h4 {
  margin: 0;
  color: var(--benefit-text, #0f1115);
  font-size: 1.1rem;
  font-weight: 900;
}
.modal-close {
  background: transparent;
  border: none;
  color: var(--benefit-text-3, #8a8f99);
  cursor: pointer;
  padding: 0.2rem;
  display: flex;
}
.modal-close:hover {
  color: var(--benefit-text, #0f1115);
}
.modal-body {
  padding: 1.25rem;
  overflow-y: auto;
  max-height: 60vh;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.modal-section h5 {
  margin: 0 0 0.5rem;
  color: var(--benefit-text, #0f1115);
  font-size: 0.85rem;
  font-weight: 900;
}
.campaign-info {
  background: var(--benefit-muted, #fafafb);
  border: 1px solid var(--benefit-line, #ecedf0);
  border-radius: 8px;
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}
.campaign-info p {
  margin: 0;
  font-size: 0.78rem;
  color: var(--benefit-text-2, #4a4f5a);
}
.campaign-info span {
  display: inline-block;
  width: 60px;
  color: var(--benefit-text-3, #8a8f99);
  font-weight: 800;
}
.campaign-info strong {
  color: var(--benefit-text, #0f1115);
  font-weight: 900;
}
.modal-desc {
  margin: 0 0 0.75rem;
  font-size: 0.75rem;
  color: var(--benefit-text-3, #8a8f99);
}
.benefit-check-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.check-item {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem;
  border: 1px solid var(--benefit-line, #ecedf0);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.check-item:hover {
  background: var(--benefit-muted, #fafafb);
}
.check-item:has(input:checked) {
  border-color: var(--benefit-brand, #5b5bf5);
  background: var(--benefit-brand-soft, #eef0ff);
}

/* checkbox -> radio로 CSS 선택자 수정 */
.check-item input[type="radio"] {
  width: 18px;
  height: 18px;
  accent-color: var(--benefit-brand, #5b5bf5);
  cursor: pointer;
}

.check-item__info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.check-item__info strong {
  font-size: 0.7rem;
  color: var(--benefit-brand, #5b5bf5);
}
.check-item__info span {
  font-size: 0.85rem;
  font-weight: 900;
  color: var(--benefit-text, #0f1115);
}
.check-item__info small {
  font-size: 0.7rem;
  color: var(--benefit-text-3, #8a8f99);
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--benefit-line, #ecedf0);
  background: var(--benefit-surface, #ffffff);
}
.modal-footer button {
  min-height: 2.35rem;
  border: 1px solid var(--benefit-line, #ecedf0);
  border-radius: 8px;
  background: var(--benefit-surface, #ffffff);
  color: var(--benefit-text-2, #4a4f5a);
  padding: 0 1rem;
  font-size: 0.8rem;
  font-weight: 900;
  cursor: pointer;
}
.modal-footer button.primary {
  border-color: var(--benefit-brand, #5b5bf5);
  background: var(--benefit-brand, #5b5bf5);
  color: #fff;
}
</style>