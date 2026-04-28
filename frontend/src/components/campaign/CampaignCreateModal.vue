<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { campaignLabels, campaignModalText } from '@/constants/campaignText'

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

function createEmptyForm() {
  return {
    name: '',
    purpose: '',
    tagInput: '',
    startDate: '',
    endDate: '',
    partnerInput: '',
    goals: '',
    mainMessage: '',
  }
}

const form = reactive(createEmptyForm())
const partners = ref([])

const modalEyebrow = computed(() =>
  props.mode === 'edit' ? campaignModalText.eyebrowEdit : campaignModalText.eyebrowCreate,
)

const modalTitle = computed(() =>
  props.mode === 'edit' ? campaignLabels.editCampaign : campaignLabels.createCampaign,
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

const canSubmit = computed(() => {
  return form.name.trim() && form.purpose.trim() && form.startDate && form.endDate
})

function hydrateForm(values) {
  const source = values ?? {}
  const nextForm = createEmptyForm()

  nextForm.name = source.name ?? ''
  nextForm.purpose = source.purpose ?? ''
  nextForm.tagInput = Array.isArray(source.tags) ? source.tags.join(', ') : ''
  nextForm.startDate = source.startDate ?? ''
  nextForm.endDate = source.endDate ?? ''
  nextForm.goals = source.goals ?? ''
  nextForm.mainMessage = source.mainMessage ?? ''

  Object.assign(form, nextForm)
  partners.value = Array.isArray(source.partners) ? [...source.partners] : []
}

watch(
  () => [props.mode, props.initialValues],
  () => {
    if (props.mode === 'edit' && props.initialValues) {
      hydrateForm(props.initialValues)
      return
    }

    hydrateForm(null)
  },
  { immediate: true, deep: true },
)

function addPartner() {
  const nextPartner = form.partnerInput.trim()

  if (!nextPartner || partners.value.includes(nextPartner)) {
    form.partnerInput = ''
    return
  }

  partners.value.push(nextPartner)
  form.partnerInput = ''
}

function removePartner(partner) {
  partners.value = partners.value.filter((item) => item !== partner)
}

function handlePartnerKeydown(event) {
  if (event.key === 'Enter') {
    event.preventDefault()
    addPartner()
  }
}

function submitForm() {
  if (!canSubmit.value) {
    return
  }

  emit('submit', {
    name: form.name,
    purpose: form.purpose,
    tags: tagList.value,
    startDate: form.startDate,
    endDate: form.endDate,
    partners: partners.value,
    goals: form.goals,
    mainMessage: form.mainMessage,
  })
}
</script>

<template>
  <Teleport to="body">
    <div class="campaign-modal" role="presentation" @click.self="emit('close')">
      <section
        class="campaign-modal__panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="campaign-modal-title"
      >
        <header class="campaign-modal__header">
          <div>
            <p>{{ modalEyebrow }}</p>
            <h2 id="campaign-modal-title">{{ modalTitle }}</h2>
          </div>
          <button
            type="button"
            class="campaign-modal__close"
            :aria-label="campaignModalText.close"
            @click="emit('close')"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 6l12 12M18 6 6 18" />
            </svg>
          </button>
        </header>

        <form class="campaign-form" @submit.prevent="submitForm">
          <div class="campaign-form__grid">
            <label class="campaign-field campaign-field--wide">
              <span>{{ campaignModalText.name }}</span>
              <input
                v-model="form.name"
                type="text"
                :placeholder="campaignModalText.namePlaceholder"
              />
            </label>

            <label class="campaign-field campaign-field--wide">
              <span>{{ campaignLabels.purpose }}</span>
              <textarea
                v-model="form.purpose"
                rows="3"
                :placeholder="campaignModalText.purposePlaceholder"
              />
            </label>

            <label class="campaign-field">
              <span>{{ campaignModalText.tags }}</span>
              <input
                v-model="form.tagInput"
                type="text"
                :placeholder="campaignModalText.tagsPlaceholder"
              />
            </label>

            <div class="campaign-field">
              <span>{{ campaignLabels.period }}</span>
              <div class="campaign-date-range">
                <input
                  v-model="form.startDate"
                  type="date"
                  :aria-label="campaignModalText.startDate"
                />
                <input v-model="form.endDate" type="date" :aria-label="campaignModalText.endDate" />
              </div>
            </div>

            <div class="campaign-field campaign-field--wide">
              <span>{{ campaignLabels.partners }}</span>
              <div class="campaign-invite">
                <input
                  v-model="form.partnerInput"
                  type="text"
                  :placeholder="campaignModalText.partnersPlaceholder"
                  @keydown="handlePartnerKeydown"
                />
                <button type="button" @click="addPartner">
                  {{ campaignModalText.addPartner }}
                </button>
              </div>
              <div v-if="partners.length" class="campaign-chip-list">
                <button
                  v-for="partner in partners"
                  :key="partner"
                  type="button"
                  class="campaign-chip"
                  @click="removePartner(partner)"
                >
                  {{ partner }}
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M6 6l12 12M18 6 6 18" />
                  </svg>
                </button>
              </div>
            </div>

            <label class="campaign-field campaign-field--wide">
              <span>{{ campaignLabels.goals }}</span>
              <textarea
                v-model="form.goals"
                rows="3"
                :placeholder="campaignModalText.goalsPlaceholder"
              />
            </label>

            <label class="campaign-field campaign-field--wide">
              <span>{{ campaignLabels.mainMessage }}</span>
              <textarea
                v-model="form.mainMessage"
                rows="4"
                :placeholder="campaignModalText.mainMessagePlaceholder"
              />
            </label>
          </div>

          <footer class="campaign-modal__footer">
            <div class="campaign-modal__hint">
              {{ campaignModalText.hint }}
            </div>
            <div class="campaign-modal__actions">
              <button
                type="button"
                class="campaign-button campaign-button--secondary"
                @click="emit('close')"
              >
                {{ campaignModalText.cancel }}
              </button>
              <button
                type="submit"
                class="campaign-button campaign-button--primary"
                :disabled="!canSubmit"
              >
                {{ submitLabel }}
              </button>
            </div>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.campaign-modal {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px;
  background: rgba(15, 23, 42, 0.42);
}

.campaign-modal__panel {
  width: min(760px, 100%);
  max-height: min(860px, calc(100vh - 56px));
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-elevated);
}

.campaign-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid var(--border-color);
}

.campaign-modal__header p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-modal__header h2 {
  margin-top: 2px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 700;
}

.campaign-modal__close {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: var(--muted-text);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.campaign-modal__close:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.campaign-modal__close svg,
.campaign-chip svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.campaign-form {
  max-height: calc(100vh - 162px);
  overflow-y: auto;
}

.campaign-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 24px;
}

.campaign-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.campaign-field--wide {
  grid-column: 1 / -1;
}

.campaign-field span {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.campaign-field input,
.campaign-field textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition:
    border-color var(--transition-fast),
    background var(--transition-fast),
    box-shadow var(--transition-fast);
}

.campaign-field input {
  height: 40px;
  padding: 0 12px;
}

.campaign-field textarea {
  resize: vertical;
  min-height: 92px;
  padding: 12px;
}

.campaign-field input::placeholder,
.campaign-field textarea::placeholder {
  color: var(--subtle-text);
}

.campaign-field input:focus,
.campaign-field textarea:focus {
  border-color: var(--color-primary-300);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary-300) 20%, transparent);
}

.campaign-date-range,
.campaign-invite {
  display: flex;
  gap: 8px;
}

.campaign-invite input {
  flex: 1;
}

.campaign-invite button {
  min-width: 70px;
  border-radius: var(--radius-md);
  background: var(--color-primary-500);
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  transition: background var(--transition-fast);
}

.campaign-invite button:hover {
  background: var(--color-primary-600);
}

.campaign-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.campaign-chip {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  gap: 6px;
  padding: 3px 8px 3px 10px;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
  color: var(--badge-text);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.campaign-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px 24px;
  border-top: 1px solid var(--border-color);
}

.campaign-modal__hint {
  color: var(--muted-text);
  font-size: 12px;
}

.campaign-modal__actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.campaign-button {
  min-height: 40px;
  border-radius: var(--radius-md);
  padding: 0 16px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.campaign-button--secondary {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.campaign-button--secondary:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.campaign-button--primary {
  background: var(--color-primary-500);
  color: #ffffff;
}

.campaign-button--primary:hover:not(:disabled) {
  background: var(--color-primary-600);
}

.campaign-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 720px) {
  .campaign-modal {
    align-items: stretch;
    padding: 12px;
  }

  .campaign-form__grid {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .campaign-date-range,
  .campaign-modal__footer {
    flex-direction: column;
  }

  .campaign-modal__actions {
    width: 100%;
  }

  .campaign-button {
    flex: 1;
  }
}
</style>
