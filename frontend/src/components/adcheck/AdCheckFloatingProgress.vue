<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { isTerminalJobStatus, useAdCheckJobsStore } from '@/stores/adCheckJobs'
import { useAuthStore } from '@/stores/useAuthStore'
import { useUserSettingsStore } from '@/stores/userSettings'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const adCheckJobsStore = useAdCheckJobsStore()
const userSettingsStore = useUserSettingsStore()

const job = computed(() => adCheckJobsStore.floatingJob)
const isReviewRoute = computed(() =>
  route.name === 'campaign-detail' && String(route.query.tab || '') === 'review',
)
const isProgressPanelEnabled = computed(() =>
  userSettingsStore.notifications.enabled
  && userSettingsStore.notifications.showAdCheckProgressPanel !== false,
)
const hasPanel = computed(() => isProgressPanelEnabled.value && Boolean(job.value) && !isReviewRoute.value)
const canOpenResult = computed(() => Boolean(job.value?.targetUrl || job.value?.result?.analysisJobId))
const queuedJobs = computed(() =>
  adCheckJobsStore.queuedJobs.filter((queuedJob) => queuedJob.jobId !== job.value?.jobId),
)
const progressPercent = computed(() =>
  Math.max(0, Math.min(100, Number(job.value?.progressPercent ?? 0))),
)
const progressStyle = computed(() => ({ width: `${progressPercent.value}%` }))
const stepLabel = computed(() =>
  job.value?.currentStepLabel || (job.value?.status === 'SUCCEEDED' ? '완료' : '진행 중'),
)
const canOpenReviewPanel = computed(() => Boolean(job.value?.campaignId && job.value?.jobId))
const actionLabel = computed(() =>
  canOpenReviewPanel.value && !isTerminalJobStatus(job.value?.status) ? '진행 화면' : '결과 확인',
)

async function loadActiveJobs() {
  if (authStore.isAuthenticated) {
    await adCheckJobsStore.loadActiveJobs()
  }
}

function openJobWindow(currentJob) {
  if (!currentJob) {
    return
  }

  if (currentJob.campaignId) {
    const isTerminal = isTerminalJobStatus(currentJob.status)
    router.push({
      name: 'campaign-detail',
      params: {
        campaignId: currentJob.campaignId,
      },
      query: {
        tab: isTerminal ? 'library' : 'review',
        adCheckJobId: currentJob.jobId,
      },
    })
    return
  }

  const targetUrl = currentJob.targetUrl
    || (currentJob.result?.analysisJobId
      ? `/references?analysisJobId=${encodeURIComponent(currentJob.result.analysisJobId)}`
      : '')

  if (targetUrl) {
    router.push(targetUrl)
  }
}

function closePanel(currentJob) {
  if (!currentJob) {
    return
  }
  adCheckJobsStore.dismissJob(currentJob.jobId)
}

onMounted(() => {
  if (!authStore.isHydrated) {
    authStore.restore()
  }
  if (isProgressPanelEnabled.value) {
    void loadActiveJobs()
  }
})

watch(
  () => authStore.isAuthenticated,
  (authenticated) => {
    if (authenticated && isProgressPanelEnabled.value) {
      void loadActiveJobs()
    }
  },
)

watch(
  isProgressPanelEnabled,
  (enabled) => {
    if (enabled) {
      void loadActiveJobs()
    }
  },
)
</script>

<template>
  <aside v-if="hasPanel" class="ad-check-floating" aria-live="polite">
    <header class="ad-check-floating__head">
      <div>
        <span>AI 검수</span>
        <strong>{{ job.fileName }}</strong>
      </div>
      <button
        type="button"
        class="ad-check-floating__icon-button"
        aria-label="닫기"
        @click="closePanel(job)"
      >
        ×
      </button>
    </header>

    <div class="ad-check-floating__status">
      <div class="ad-check-floating__meta">
        <span>{{ stepLabel }}</span>
        <em>{{ progressPercent }}%</em>
      </div>
      <div class="ad-check-floating__bar" aria-hidden="true">
        <i :style="progressStyle"></i>
      </div>
    </div>

    <p v-if="job.errorMessage && job.status === 'FAILED'" class="ad-check-floating__error">
      {{ job.errorMessage }}
    </p>

    <p v-if="queuedJobs.length" class="ad-check-floating__queue">
      대기 중인 검수 {{ queuedJobs.length }}건
    </p>

    <footer class="ad-check-floating__actions">
      <button
        v-if="canOpenReviewPanel || canOpenResult"
        type="button"
        class="ad-check-floating__primary"
        @click="openJobWindow(job)"
      >
        {{ actionLabel }}
      </button>
    </footer>
  </aside>
</template>

<style scoped>
.ad-check-floating {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 80;
  display: grid;
  width: min(320px, calc(100vw - 32px));
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-md);
}

.ad-check-floating__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.ad-check-floating__head div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.ad-check-floating__head span,
.ad-check-floating__queue,
.ad-check-floating__meta span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 850;
}

.ad-check-floating__head strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ad-check-floating__status {
  display: grid;
  gap: 6px;
}

.ad-check-floating__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ad-check-floating__meta em {
  color: var(--color-primary-700);
  font-size: 13px;
  font-style: normal;
  font-weight: 950;
}

.ad-check-floating__bar {
  height: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--border-color);
}

.ad-check-floating__bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary-500);
  transition: width 180ms ease;
}

.ad-check-floating__icon-button {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.ad-check-floating__error {
  margin: 0;
  color: var(--color-danger-dark);
  font-size: 12px;
  font-weight: 850;
  line-height: 1.45;
}

.ad-check-floating__queue {
  margin: 0;
}

.ad-check-floating__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.ad-check-floating__primary,
.ad-check-floating__secondary {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0 11px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
}

.ad-check-floating__primary {
  border: 1px solid transparent;
  background: var(--color-primary-500);
  color: #fff;
}

.ad-check-floating__secondary {
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  color: var(--text-primary);
}

@media (max-width: 640px) {
  .ad-check-floating {
    right: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
  }
}
</style>
