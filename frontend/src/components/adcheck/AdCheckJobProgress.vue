<script setup>
import { computed } from 'vue'
import { AD_CHECK_JOB_STEPS, adCheckStatusLabel } from '@/stores/adCheckJobs'

const props = defineProps({
  job: {
    type: Object,
    required: true,
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const normalizedStatus = computed(() => String(props.job?.status || '').toUpperCase())
const progressPercent = computed(() => Math.max(0, Math.min(100, Number(props.job?.progressPercent ?? 0))))
const currentStep = computed(() => String(props.job?.currentStep || 'REQUEST_RECEIVED').toUpperCase())
const currentStepMeta = computed(() =>
  AD_CHECK_JOB_STEPS.find((step) => step.key === currentStep.value) ?? AD_CHECK_JOB_STEPS[0],
)
const effectiveCurrentOrder = computed(() => {
  if (normalizedStatus.value === 'FAILED') {
    const reachedStep = [...AD_CHECK_JOB_STEPS]
      .reverse()
      .find((step) => step.progressPercent <= progressPercent.value && step.key !== 'COMPLETED')
    return reachedStep?.order ?? currentStepMeta.value.order
  }

  return currentStepMeta.value.order
})
const statusLabel = computed(() => adCheckStatusLabel(normalizedStatus.value))
const progressStyle = computed(() => ({ width: `${progressPercent.value}%` }))

function stepState(step) {
  if (normalizedStatus.value === 'FAILED' && step.order === effectiveCurrentOrder.value) {
    return 'failed'
  }
  if (normalizedStatus.value === 'SUCCEEDED' || step.order < effectiveCurrentOrder.value) {
    return 'done'
  }
  if (step.order === effectiveCurrentOrder.value) {
    return 'current'
  }
  return 'idle'
}

function stepDisplayLabel(step) {
  const state = stepState(step)
  if (state === 'done') {
    return step.doneLabel || step.label
  }
  if (state === 'current') {
    return step.currentLabel || step.label
  }
  if (state === 'failed') {
    return `${step.label} 실패`
  }
  return step.label
}

function stepDisplayMessage(step) {
  const state = stepState(step)
  if (state === 'done') {
    return step.doneMessage || step.message
  }
  if (state === 'current') {
    return step.currentMessage || step.message
  }
  if (state === 'failed') {
    return props.job?.errorMessage || '이 단계에서 오류가 발생했습니다.'
  }
  return step.pendingMessage || step.message
}
</script>

<template>
  <article class="ad-check-progress" :class="{ 'ad-check-progress--compact': compact }">
    <header class="ad-check-progress__head">
      <div>
        <span>{{ statusLabel }}</span>
        <strong>{{ job.currentStepLabel || currentStepMeta.label }}</strong>
      </div>
      <em>{{ progressPercent }}%</em>
    </header>

    <div class="ad-check-progress__bar" aria-hidden="true">
      <i :style="progressStyle"></i>
    </div>

    <p class="ad-check-progress__message">
      {{ job.currentStepMessage || currentStepMeta.message }}
    </p>

    <ol v-if="!compact" class="ad-check-progress__steps">
      <li
        v-for="step in AD_CHECK_JOB_STEPS"
        :key="step.key"
        :class="`ad-check-progress__step ad-check-progress__step--${stepState(step)}`"
      >
        <i></i>
        <div>
          <strong>{{ stepDisplayLabel(step) }}</strong>
          <span>{{ stepDisplayMessage(step) }}</span>
        </div>
      </li>
    </ol>

    <p v-if="normalizedStatus === 'FAILED' && job.errorMessage" class="ad-check-progress__error">
      {{ job.errorMessage }}
    </p>
  </article>
</template>

<style scoped>
.ad-check-progress {
  display: grid;
  width: 100%;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.ad-check-progress__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 10px;
}

.ad-check-progress__head div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.ad-check-progress__head span,
.ad-check-progress__message,
.ad-check-progress__step span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.ad-check-progress__head strong,
.ad-check-progress__step strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 950;
}

.ad-check-progress__head em {
  min-width: 44px;
  color: var(--color-primary-700);
  font-size: 13px;
  font-style: normal;
  font-weight: 950;
  text-align: right;
}

.ad-check-progress__bar {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--border-color);
}

.ad-check-progress__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary-500);
  transition: width 180ms ease;
}

.ad-check-progress__message,
.ad-check-progress__error {
  margin: 0;
}

.ad-check-progress__steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
  margin: 2px 0 0;
  padding: 0;
  list-style: none;
}

.ad-check-progress__step {
  position: relative;
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 7px;
  align-items: start;
  text-align: center;
}

.ad-check-progress__step:not(:last-child)::after {
  position: absolute;
  top: 3px;
  right: -10px;
  color: var(--muted-text);
  content: "->";
  font-size: 12px;
  font-weight: 950;
}

.ad-check-progress__step i {
  width: 10px;
  height: 10px;
  border: 2px solid var(--border-strong);
  border-radius: 999px;
  background: var(--panel-color);
}

.ad-check-progress__step div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.ad-check-progress__step span {
  width: 100%;
  max-width: 140px;
}

.ad-check-progress__step--done i {
  border-color: var(--color-success);
  background: var(--color-success);
}

.ad-check-progress__step--current i {
  border-color: var(--color-primary-500);
  background: var(--panel-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary-500) 18%, transparent);
}

.ad-check-progress__step--failed i {
  border-color: var(--color-danger);
  background: var(--color-danger);
}

.ad-check-progress__step--idle {
  opacity: 0.55;
}

.ad-check-progress__error {
  color: var(--color-danger-dark);
  font-size: 12px;
  font-weight: 850;
}

.ad-check-progress--compact {
  padding: 0;
  border: 0;
  background: transparent;
}

@media (max-width: 720px) {
  .ad-check-progress__steps {
    grid-template-columns: repeat(5, minmax(92px, 1fr));
    overflow-x: auto;
  }

  .ad-check-progress__step {
    min-width: 92px;
  }
}
</style>
