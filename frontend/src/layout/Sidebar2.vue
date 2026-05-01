<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'
import CampaignCreateModal from '@/components/campaign/CampaignCreateModal.vue'
import { campaignLabels, campaignSidebarText, campaignStatusMeta } from '@/constants/campaignText'
import { CreateCampaign, UpdateCampaign, UpdateCampaignStatus } from '@/api/campaigns'

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()
const authStore = useAuthStore()

const SIDEBAR_WIDTH_STORAGE_KEY = 'callog-sidebar2-width'
const SIDEBAR_DEFAULT_WIDTH = 240
const SIDEBAR_MIN_WIDTH = 200
const SIDEBAR_MAX_WIDTH = 400

const FLOAT_MARGIN = 16
const MENU_WIDTH = 224
const MENU_HEIGHT_ESTIMATE = 220

const sidebarElement = ref(null)
const sidebarWidth = ref(SIDEBAR_DEFAULT_WIDTH)
const sidebarResizeLeft = ref(0)
const isResizing = ref(false)

const createModalOpen = ref(false)
const campaignModalMode = ref('create')
const editingCampaignId = ref(null)
const contextCampaignId = ref(null)
const draggedCampaignId = ref(null)
const dropTargetCampaignId = ref(null)
const dropPlacement = ref('before')
const folderDropState = ref('idle')

const VIEW_MODE_STORAGE_KEY = 'callog-sidebar2-view-mode'
const viewMode = ref('compact') // 'compact' | 'kpi'

const contextMenuPosition = reactive({ top: FLOAT_MARGIN, left: FLOAT_MARGIN })

const sidebarStyle = computed(() => ({ '--sidebar2-width': `${sidebarWidth.value}px` }))

const sidebarCampaigns = computed(() => store.sidebarCampaigns)
const folderCampaignCount = computed(() => store.campaignFolderCount)
const isFolderRoute = computed(() => route.name === 'campaign-folder')

const visualSidebarCampaigns = computed(() => {
  const campaigns = [...sidebarCampaigns.value]
  const sourceCampaignId = draggedCampaignId.value
  const targetCampaignId = dropTargetCampaignId.value

  if (!sourceCampaignId || !targetCampaignId || sourceCampaignId === targetCampaignId) {
    return campaigns
  }

  const sourceIndex = campaigns.findIndex((c) => c.id === sourceCampaignId)
  if (sourceIndex === -1) return campaigns

  const [sourceCampaign] = campaigns.splice(sourceIndex, 1)
  const targetIndex = campaigns.findIndex((c) => c.id === targetCampaignId)
  if (targetIndex === -1) return sidebarCampaigns.value

  const insertIndex = dropPlacement.value === 'after' ? targetIndex + 1 : targetIndex
  campaigns.splice(insertIndex, 0, sourceCampaign)
  return campaigns
})

const draggedCampaign = computed(
  () => store.campaigns.find((c) => c.id === draggedCampaignId.value) ?? null,
)
const canDropIntoFolder = computed(() => draggedCampaign.value?.status === 'completed')

const contextCampaign = computed(
  () => store.campaigns.find((c) => c.id === contextCampaignId.value) ?? null,
)
const editingCampaign = computed(
  () => store.campaigns.find((c) => c.id === editingCampaignId.value) ?? null,
)

const contextMenuStyle = computed(() => ({
  top: `${contextMenuPosition.top}px`,
  left: `${contextMenuPosition.left}px`,
}))

const contextCampaignStatus = computed(() => getCampaignStatusMeta(contextCampaign.value?.status))

const contextMenuActions = computed(() => {
  const campaign = contextCampaign.value
  if (!campaign) return []
  return [
    {
      key: 'edit',
      label: campaignLabels.editCampaign,
      description: campaignSidebarText.editDescription,
      tone: 'default',
      icon: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 1 1 3 3L7 19l-4 1 1-4Z"/></svg>`,
    },
    campaign.status === 'completed'
      ? {
          key: 'reopen',
          label: campaignSidebarText.reopenCampaign,
          description: campaignSidebarText.reopenDescription,
          tone: 'default',
          nextStatus: 'live',
          icon: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 3 14 9-14 9V3Z"/></svg>`,
        }
      : {
          key: 'complete',
          label: campaignSidebarText.markAsComplete,
          description: campaignSidebarText.completeDescription,
          tone: 'success',
          nextStatus: 'completed',
          icon: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12 5 5L20 7"/></svg>`,
        },
    campaign.status === 'paused'
      ? {
          key: 'resume',
          label: campaignSidebarText.resumeCampaign,
          description: campaignSidebarText.resumeDescription,
          tone: 'default',
          nextStatus: 'live',
          icon: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 3 14 9-14 9V3Z"/></svg>`,
        }
      : {
          key: 'pause',
          label: campaignSidebarText.pauseCampaign,
          description: campaignSidebarText.pauseDescription,
          tone: 'warning',
          nextStatus: 'paused',
          icon: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 4H6v16h4V4Zm8 0h-4v16h4V4Z"/></svg>`,
        },
  ]
})

const userStorageKey = computed(
  () =>
    authStore.user?.userId ||
    authStore.user?.id ||
    authStore.user?.loginId ||
    authStore.user?.email ||
    store.currentUserId,
)

function getCampaignStatusMeta(status) {
  return campaignStatusMeta[status] ?? { label: campaignStatusMeta.draft.label, tone: 'draft' }
}

function toggleViewMode() {
  viewMode.value = viewMode.value === 'compact' ? 'kpi' : 'compact'
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(VIEW_MODE_STORAGE_KEY, viewMode.value)
  }
}

function getDaysLeft(endDate) {
  if (!endDate) return null
  const end = new Date(endDate)
  if (Number.isNaN(end.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  end.setHours(0, 0, 0, 0)
  return Math.round((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

function formatDDay(daysLeft) {
  if (daysLeft === null || daysLeft === undefined) return ''
  if (daysLeft > 0) return `D-${daysLeft}`
  if (daysLeft === 0) return 'D-DAY'
  return `D+${Math.abs(daysLeft)}`
}

function getProgressPercent(campaign) {
  if (typeof campaign.progress === 'number') {
    return Math.max(0, Math.min(100, Math.round(campaign.progress)))
  }
  // store에 progress가 없을 때 status 기반 placeholder
  const fallbackByStatus = { draft: 10, review: 70, live: 50, paused: 30, completed: 100 }
  return fallbackByStatus[campaign.status] ?? 0
}

function getTaskCount(campaign) {
  const list = store.tasks
  if (!Array.isArray(list)) return 0
  return list.filter((task) => task?.campaignId === campaign.id).length
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function clampSidebarWidth(value) {
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, value))
}

function setSidebarWidth(value) {
  sidebarWidth.value = clampSidebarWidth(Math.round(value))
}

function persistSidebarWidth() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(SIDEBAR_WIDTH_STORAGE_KEY, String(sidebarWidth.value))
}

function handleSidebarResize(event) {
  setSidebarWidth(event.clientX - sidebarResizeLeft.value)
}

function stopSidebarResize() {
  if (!isResizing.value) return
  isResizing.value = false
  persistSidebarWidth()
  window.removeEventListener('pointermove', handleSidebarResize)
  window.removeEventListener('pointerup', stopSidebarResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

function startSidebarResize(event) {
  if (store.sidebarCollapsed) return
  event.preventDefault()
  sidebarResizeLeft.value = sidebarElement.value?.getBoundingClientRect().left ?? 0
  isResizing.value = true
  handleSidebarResize(event)
  window.addEventListener('pointermove', handleSidebarResize)
  window.addEventListener('pointerup', stopSidebarResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function handleResizeKeydown(event) {
  if (store.sidebarCollapsed) return
  const step = event.shiftKey ? 24 : 8
  if (event.key === 'ArrowLeft') { event.preventDefault(); setSidebarWidth(sidebarWidth.value - step); persistSidebarWidth() }
  if (event.key === 'ArrowRight') { event.preventDefault(); setSidebarWidth(sidebarWidth.value + step); persistSidebarWidth() }
}

function isServerCampaignId(campaignId) {
  return /^\d+$/.test(String(campaignId))
}

function closeCampaignMenu() {
  contextCampaignId.value = null
}

function closeCreateModal() {
  createModalOpen.value = false
  campaignModalMode.value = 'create'
  editingCampaignId.value = null
}

function selectCampaign(campaignId) {
  closeCampaignMenu()
  store.setActiveCampaign(campaignId)
  router.push({ name: 'campaign-detail', params: { campaignId } })
}

function openFolderPage() {
  closeCampaignMenu()
  router.push({ name: 'campaign-folder' })
}

function openCreateModal() {
  closeCampaignMenu()
  campaignModalMode.value = 'create'
  editingCampaignId.value = null
  createModalOpen.value = true
}

function openEditModal(campaignId) {
  editingCampaignId.value = campaignId
  campaignModalMode.value = 'edit'
  closeCampaignMenu()
  createModalOpen.value = true
}

function openCampaignMenu(campaign, event) {
  event.preventDefault()
  contextMenuPosition.left = clamp(event.clientX + 8, FLOAT_MARGIN, window.innerWidth - MENU_WIDTH - FLOAT_MARGIN)
  contextMenuPosition.top = clamp(event.clientY + 8, FLOAT_MARGIN, window.innerHeight - MENU_HEIGHT_ESTIMATE - FLOAT_MARGIN)
  contextCampaignId.value = campaign.id
}

async function handleCampaignSubmit(payload) {
  try {
    const campaign =
      campaignModalMode.value === 'edit' && editingCampaignId.value
        ? await saveExistingCampaign(editingCampaignId.value, payload)
        : await saveNewCampaign(payload)
    closeCreateModal()
    if (campaign?.id) selectCampaign(campaign.id)
  } catch (error) {
    console.warn('Campaign save failed', error)
  }
}

async function saveNewCampaign(payload) {
  const savedCampaign = await CreateCampaign(payload)
  return store.createCampaign(savedCampaign)
}

async function saveExistingCampaign(campaignId, payload) {
  if (!isServerCampaignId(campaignId)) return store.updateCampaign(campaignId, payload)
  const savedCampaign = await UpdateCampaign(campaignId, payload)
  return store.updateCampaign(campaignId, savedCampaign)
}

async function handleContextMenuAction(action) {
  const campaign = contextCampaign.value
  if (!campaign) return
  if (action.key === 'edit') { openEditModal(campaign.id); return }
  if (action.nextStatus) {
    try {
      if (isServerCampaignId(campaign.id)) {
        const savedCampaign = await UpdateCampaignStatus(campaign.id, action.nextStatus)
        store.updateCampaign(campaign.id, savedCampaign)
      } else {
        store.updateCampaignStatus(campaign.id, action.nextStatus)
      }
    } catch (error) {
      console.warn('Campaign status update failed', error)
      return
    }
  }
  closeCampaignMenu()
}

function resetDragState() {
  draggedCampaignId.value = null
  dropTargetCampaignId.value = null
  dropPlacement.value = 'before'
  folderDropState.value = 'idle'
}

function handleCampaignDragStart(campaign, event) {
  draggedCampaignId.value = campaign.id
  dropTargetCampaignId.value = null
  folderDropState.value = campaign.status === 'completed' ? 'ready' : 'blocked'
  closeCampaignMenu()
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', campaign.id)
  }
}

function handleCampaignDragEnd() { resetDragState() }

function handleCampaignDragOver(campaign, event) {
  if (!draggedCampaignId.value) return
  if (draggedCampaignId.value === campaign.id) {
    if (dropTargetCampaignId.value) {
      event.preventDefault()
      if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
    }
    return
  }
  event.preventDefault()
  const rect = event.currentTarget?.getBoundingClientRect()
  if (rect) dropPlacement.value = event.clientY > rect.top + rect.height / 2 ? 'after' : 'before'
  dropTargetCampaignId.value = campaign.id
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function handleCampaignDrop(campaign, event) {
  event.preventDefault()
  if (!draggedCampaignId.value) { resetDragState(); return }
  if (draggedCampaignId.value === campaign.id) {
    store.reorderCampaign(draggedCampaignId.value, dropTargetCampaignId.value, dropPlacement.value)
    resetDragState(); return
  }
  store.reorderCampaign(draggedCampaignId.value, campaign.id, dropPlacement.value)
  resetDragState()
}

function handleFolderDragEnter() {
  if (!draggedCampaign.value) return
  folderDropState.value = canDropIntoFolder.value ? 'ready' : 'blocked'
}

function handleFolderDragOver(event) {
  if (!draggedCampaign.value) return
  folderDropState.value = canDropIntoFolder.value ? 'ready' : 'blocked'
  if (!canDropIntoFolder.value) return
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}

function handleFolderDragLeave() { folderDropState.value = 'idle' }

function handleFolderDrop(event) {
  event.preventDefault()
  if (!draggedCampaign.value || !canDropIntoFolder.value) { resetDragState(); return }
  store.moveCampaignToFolder(draggedCampaign.value.id)
  resetDragState()
}

function handleGlobalPointerDown(event) {
  const target = event.target
  if (!(target instanceof HTMLElement)) { closeCampaignMenu(); return }
  if (contextCampaignId.value && !target.closest('.campaign-list__context-menu') && !target.closest('.campaign-card')) {
    closeCampaignMenu()
  }
}

function handleGlobalKeydown(event) {
  if (event.key !== 'Escape') return
  if (createModalOpen.value) { closeCreateModal(); return }
  closeCampaignMenu()
}

watch(
  userStorageKey,
  (nextKey) => { store.setCampaignUiOwnerKey(nextKey) },
  { immediate: true },
)

onMounted(() => {
  const storedWidth = window.localStorage.getItem(SIDEBAR_WIDTH_STORAGE_KEY)
  const parsedWidth = Number(storedWidth)
  if (Number.isFinite(parsedWidth)) setSidebarWidth(parsedWidth)
  const storedViewMode = window.localStorage.getItem(VIEW_MODE_STORAGE_KEY)
  if (storedViewMode === 'compact' || storedViewMode === 'kpi') {
    viewMode.value = storedViewMode
  }
  window.addEventListener('pointerdown', handleGlobalPointerDown)
  window.addEventListener('keydown', handleGlobalKeydown)
})

onBeforeUnmount(() => {
  stopSidebarResize()
  window.removeEventListener('pointerdown', handleGlobalPointerDown)
  window.removeEventListener('keydown', handleGlobalKeydown)
  resetDragState()
})
</script>

<template>
  <aside
    ref="sidebarElement"
    class="campaign-list"
    :class="{
      'campaign-list--collapsed': store.sidebarCollapsed,
      'campaign-list--resizing': isResizing,
    }"
    :style="sidebarStyle"
  >
    <div class="campaign-list__clip">
    <div class="campaign-list__inner">
      <div class="campaign-list__header">
        <span class="campaign-list__header-title">Campaigns</span>
        <div class="campaign-list__header-actions">
          <button
            type="button"
            class="campaign-list__view-toggle"
            :aria-pressed="viewMode === 'kpi'"
            :title="viewMode === 'compact' ? '확장 보기로 전환' : '간단 보기로 전환'"
            @click="toggleViewMode"
          >
            {{ viewMode === 'compact' ? '확장' : '간단' }}
          </button>
          <button
            type="button"
            class="campaign-list__create-btn"
            :title="campaignLabels.createCampaign"
            :aria-label="campaignLabels.createCampaign"
            @click="openCreateModal"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 5v14M5 12h14" />
            </svg>
          </button>
        </div>
      </div>

      <div class="campaign-list__divider" />

      <TransitionGroup
        tag="nav"
        name="campaign-list-anim"
        class="campaign-list__nav"
        :class="`campaign-list__nav--${viewMode}`"
        :aria-label="campaignSidebarText.campaignList"
      >
        <button
          v-for="campaign in visualSidebarCampaigns"
          :key="campaign.id"
          type="button"
          class="campaign-card"
          :class="[
            `campaign-card--${viewMode}`,
            {
              active: store.activeCampaignId === campaign.id,
              'campaign-card--dragging': draggedCampaignId === campaign.id,
              'campaign-card--drop-before': dropTargetCampaignId === campaign.id && dropPlacement === 'before',
              'campaign-card--drop-after': dropTargetCampaignId === campaign.id && dropPlacement === 'after',
            },
          ]"
          :style="{ '--campaign-color': campaign.color }"
          :aria-current="store.activeCampaignId === campaign.id ? 'page' : undefined"
          draggable="true"
          @click="selectCampaign(campaign.id)"
          @contextmenu="openCampaignMenu(campaign, $event)"
          @dragstart="handleCampaignDragStart(campaign, $event)"
          @dragend="handleCampaignDragEnd"
          @dragover="handleCampaignDragOver(campaign, $event)"
          @drop="handleCampaignDrop(campaign, $event)"
        >
          <!-- compact (E2) -->
          <template v-if="viewMode === 'compact'">
            <span class="campaign-card__bar" />
            <span class="campaign-card__body">
              <span class="campaign-card__top">
                <span class="campaign-card__title">{{ campaign.name }}</span>
                <span v-if="getDaysLeft(campaign.endDate) !== null" class="campaign-card__dday">
                  {{ formatDDay(getDaysLeft(campaign.endDate)) }}
                </span>
              </span>
              <span class="campaign-card__bottom">
                <span class="campaign-card__pct">{{ getProgressPercent(campaign) }}%</span>
                <span class="campaign-card__progress">
                  <i :style="{ width: `${getProgressPercent(campaign)}%` }" />
                </span>
                <span
                  class="campaign-card__chip"
                  :class="`campaign-card__chip--${getCampaignStatusMeta(campaign.status).tone}`"
                >
                  {{ getCampaignStatusMeta(campaign.status).label }}
                </span>
              </span>
            </span>
          </template>

          <!-- kpi (E3) -->
          <template v-else>
            <span class="campaign-card__head">
              <span class="campaign-card__title">{{ campaign.name }}</span>
              <span
                class="campaign-card__chip"
                :class="`campaign-card__chip--${getCampaignStatusMeta(campaign.status).tone}`"
              >
                {{ getCampaignStatusMeta(campaign.status).label }}
              </span>
            </span>
            <span class="campaign-card__kpis">
              <span class="campaign-card__kpi">
                <b>{{ getProgressPercent(campaign) }}<i>%</i></b>
                <em>진행</em>
              </span>
              <span class="campaign-card__kpi-sep" />
              <span class="campaign-card__kpi">
                <template v-if="getDaysLeft(campaign.endDate) !== null">
                  <b>{{ Math.abs(getDaysLeft(campaign.endDate)) }}<i>d</i></b>
                  <em>{{ getDaysLeft(campaign.endDate) >= 0 ? '남은 일수' : '초과' }}</em>
                </template>
                <template v-else>
                  <b>-</b>
                  <em>일정 미정</em>
                </template>
              </span>
              <span class="campaign-card__kpi-sep" />
              <span class="campaign-card__kpi">
                <b>{{ getTaskCount(campaign) }}</b>
                <em>작업</em>
              </span>
            </span>
            <span class="campaign-card__progress campaign-card__progress--full">
              <i :style="{ width: `${getProgressPercent(campaign)}%` }" />
            </span>
          </template>
        </button>
      </TransitionGroup>

      <div class="campaign-list__footer">
        <button
          type="button"
          class="campaign-list__folder-btn"
          :class="{
            active: isFolderRoute,
            'campaign-list__folder-btn--drop-ready': folderDropState === 'ready' && draggedCampaignId,
            'campaign-list__folder-btn--drop-blocked': folderDropState === 'blocked' && draggedCampaignId,
          }"
          :aria-label="campaignSidebarText.folderLabel"
          @click="openFolderPage"
          @dragenter="handleFolderDragEnter"
          @dragover="handleFolderDragOver"
          @dragleave="handleFolderDragLeave"
          @drop="handleFolderDrop"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H10l2 2h6.5A2.5 2.5 0 0 1 21 9.5v8A2.5 2.5 0 0 1 18.5 20h-13A2.5 2.5 0 0 1 3 17.5v-10Z" />
          </svg>
          <span>{{ campaignSidebarText.folderLabel }}</span>
          <span v-if="folderCampaignCount" class="campaign-list__folder-count">
            {{ folderCampaignCount }}
          </span>
        </button>
      </div>
    </div>
    </div>

    <button
      type="button"
      class="campaign-list__toggle"
      :aria-label="store.sidebarCollapsed ? '캠페인 목록 열기' : '캠페인 목록 닫기'"
      @click="store.toggleSidebar"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
        <path v-if="store.sidebarCollapsed" d="m9 18 6-6-6-6" />
        <path v-else d="m15 18-6-6 6-6" />
      </svg>
    </button>

    <div
      class="campaign-list__resize-handle"
      role="separator"
      aria-label="사이드바 너비 조절"
      aria-orientation="vertical"
      tabindex="0"
      :aria-valuemin="SIDEBAR_MIN_WIDTH"
      :aria-valuemax="SIDEBAR_MAX_WIDTH"
      :aria-valuenow="sidebarWidth"
      @pointerdown="startSidebarResize"
      @keydown="handleResizeKeydown"
    />
  </aside>

  <CampaignCreateModal
    v-if="createModalOpen"
    :mode="campaignModalMode"
    :initial-values="editingCampaign"
    @close="closeCreateModal"
    @submit="handleCampaignSubmit"
  />

  <Teleport to="body">
    <Transition name="campaign-float">
      <div
        v-if="contextCampaign"
        class="campaign-list__context-menu"
        :style="contextMenuStyle"
        role="menu"
        :aria-label="campaignSidebarText.campaignActions"
      >
        <div class="campaign-list__context-head">
          <p>{{ campaignSidebarText.campaignActions }}</p>
          <strong>{{ contextCampaign.name }}</strong>
          <span
            class="campaign-list__status"
            :class="`campaign-list__status--${contextCampaignStatus.tone}`"
          >
            {{ contextCampaignStatus.label }}
          </span>
        </div>
        <div class="campaign-list__context-list">
          <button
            v-for="action in contextMenuActions"
            :key="action.key"
            type="button"
            class="campaign-list__context-item"
            :class="`campaign-list__context-item--${action.tone}`"
            @click="handleContextMenuAction(action)"
          >
            <span class="campaign-list__context-icon" v-html="action.icon" />
            <span class="campaign-list__context-copy">
              <strong>{{ action.label }}</strong>
              <small>{{ action.description }}</small>
            </span>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.campaign-list {
  --sidebar2-active-shadow: color-mix(in srgb, var(--accent-color) 24%, transparent);
  width: var(--sidebar2-width, 240px);
  flex-shrink: 0;
  background: var(--sidebar-color);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  overflow: visible;
  z-index: 15;
  transition:
    width var(--transition-normal),
    background var(--transition-normal),
    border-color var(--transition-normal);
}

.campaign-list--collapsed {
  width: 0;
  border-right-width: 0;
}

.campaign-list--resizing {
  transition: none;
}

.campaign-list__clip {
  width: 100%;
  height: 100%;
  overflow: hidden;
  flex-shrink: 0;
}

.campaign-list__inner {
  width: var(--sidebar2-width, 240px);
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.campaign-list__toggle {
  position: absolute;
  right: -16px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 25;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.campaign-list__toggle:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
  box-shadow: var(--shadow-md);
}

.campaign-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 14px 8px;
  flex-shrink: 0;
}

.campaign-list__header-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.campaign-list__header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.campaign-list__view-toggle {
  height: 24px;
  padding: 0 8px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    border-color var(--transition-fast);
}

.campaign-list__view-toggle:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
  border-color: var(--border-strong, var(--color-primary-500));
}

.campaign-list__view-toggle[aria-pressed='true'] {
  background: color-mix(in srgb, var(--color-primary-500) 12%, transparent);
  color: var(--color-primary-600);
  border-color: color-mix(in srgb, var(--color-primary-500) 22%, transparent);
}

.campaign-list__create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.campaign-list__create-btn:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.campaign-list__create-btn svg {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2.5;
}

.campaign-list__divider {
  height: 1px;
  background: var(--border-color);
  margin: 0 12px 8px;
  flex-shrink: 0;
}

.campaign-list__nav {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow-y: auto;
}

.campaign-list__nav--compact { padding: 6px 8px; gap: 0; }
.campaign-list__nav--kpi { padding: 8px; gap: 0; }

.campaign-list__nav::-webkit-scrollbar {
  width: 4px;
}

.campaign-list__nav::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 4px;
}

/* 캠페인 카드 공통 */
.campaign-card {
  position: relative;
  width: 100%;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  color: var(--text-primary);
  transition:
    border-color var(--transition-fast),
    background var(--transition-fast),
    box-shadow var(--transition-fast);
}

.campaign-card:hover {
  border-color: var(--border-strong, var(--color-primary-500));
  background: var(--panel-muted);
}

.campaign-card.active {
  border-color: var(--campaign-color, var(--color-primary-500));
  background: color-mix(in srgb, var(--campaign-color, var(--color-primary-500)) 8%, var(--panel-color));
}

.campaign-card--dragging { opacity: 0.4; }

.campaign-card--drop-before::before,
.campaign-card--drop-after::after {
  content: '';
  position: absolute;
  left: 6px;
  right: 6px;
  height: 2px;
  border-radius: var(--radius-full);
  background: var(--color-primary-500);
  pointer-events: none;
}

.campaign-card--drop-before::before { top: -3px; }
.campaign-card--drop-after::after { bottom: -3px; }

/* compact (E2) */
.campaign-card--compact {
  display: flex;
  align-items: stretch;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 5px;
  padding: 0;
}

.campaign-card--compact .campaign-card__bar {
  width: 3px;
  background: var(--campaign-color, var(--color-primary-500));
  flex-shrink: 0;
}

.campaign-card--compact .campaign-card__body {
  flex: 1;
  min-width: 0;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.campaign-card--compact .campaign-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.campaign-card--compact .campaign-card__title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.campaign-card--compact .campaign-card__dday {
  font-size: 10px;
  font-weight: 800;
  color: var(--muted-text);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.campaign-card--compact .campaign-card__bottom {
  display: flex;
  align-items: center;
  gap: 8px;
}

.campaign-card--compact .campaign-card__pct {
  font-size: 11px;
  font-weight: 800;
  color: var(--text-secondary, var(--text-primary));
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  width: 30px;
}

.campaign-card__progress {
  flex: 1;
  height: 3px;
  background: var(--panel-muted);
  border-radius: 2px;
  overflow: hidden;
  min-width: 0;
}

.campaign-card__progress i {
  display: block;
  height: 100%;
  background: var(--campaign-color, var(--color-primary-500));
  border-radius: 2px;
}

/* kpi (E3) */
.campaign-card--kpi {
  display: block;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 6px;
}

.campaign-card--kpi:hover {
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.campaign-card--kpi.active {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--campaign-color, var(--color-primary-500)) 18%, transparent);
}

.campaign-card--kpi .campaign-card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.campaign-card--kpi .campaign-card__title {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.campaign-card--kpi .campaign-card__kpis {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.campaign-card--kpi .campaign-card__kpi {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
}

.campaign-card--kpi .campaign-card__kpi b {
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  display: inline-flex;
  align-items: baseline;
}

.campaign-card--kpi .campaign-card__kpi b i {
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
  color: var(--muted-text);
  margin-left: 1px;
}

.campaign-card--kpi .campaign-card__kpi em {
  font-style: normal;
  font-size: 10px;
  font-weight: 600;
  color: var(--muted-text);
  white-space: nowrap;
}

.campaign-card--kpi.active .campaign-card__kpi b {
  color: var(--campaign-color, var(--color-primary-500));
}

.campaign-card--kpi .campaign-card__kpi-sep {
  width: 1px;
  height: 20px;
  background: var(--border-color);
  flex-shrink: 0;
}

.campaign-card--kpi .campaign-card__progress--full {
  display: block;
  width: 100%;
}

/* 상태 칩 (공통) */
.campaign-card__chip {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 3px;
  letter-spacing: 0.04em;
  white-space: nowrap;
  text-transform: uppercase;
}

.campaign-card__chip--live {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.campaign-card__chip--review {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.campaign-card__chip--paused {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.campaign-card__chip--draft {
  background: var(--color-primary-100, var(--panel-muted));
  color: var(--color-primary-700, var(--text-primary));
}

.campaign-card__chip--completed {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.campaign-list__footer {
  padding: 8px;
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}

.campaign-list__folder-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 34px;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--muted-text);
  border: none;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.campaign-list__folder-btn:hover,
.campaign-list__folder-btn.active {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.campaign-list__folder-btn svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
  flex-shrink: 0;
}

.campaign-list__folder-btn--drop-ready {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.campaign-list__folder-btn--drop-blocked {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.campaign-list__folder-count {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.campaign-list__resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  width: 10px;
  height: 100%;
  cursor: col-resize;
  touch-action: none;
  z-index: 3;
}

.campaign-list__resize-handle::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 2px;
  height: 100%;
  background: transparent;
  transition: background var(--transition-fast);
}

.campaign-list__resize-handle:hover::before,
.campaign-list--resizing .campaign-list__resize-handle::before {
  background: var(--accent-color);
  box-shadow: -4px 0 14px var(--sidebar2-active-shadow);
}

.campaign-list--collapsed .campaign-list__resize-handle {
  opacity: 0;
  pointer-events: none;
}

/* Context menu */
.campaign-list__context-menu {
  position: fixed;
  z-index: 60;
  width: 224px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-elevated);
  padding: 8px;
}

.campaign-list__context-head {
  display: grid;
  gap: 4px;
  padding: 10px 12px 14px;
  border-bottom: 1px solid var(--border-color);
}

.campaign-list__context-head p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-list__context-head strong {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.campaign-list__context-list {
  display: grid;
  gap: 4px;
  padding-top: 8px;
}

.campaign-list__context-item {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
  border: none;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.campaign-list__context-item:hover { background: var(--panel-muted); color: var(--text-primary); }
.campaign-list__context-item--success:hover { background: var(--color-success-light); color: var(--color-success-dark); }
.campaign-list__context-item--warning:hover { background: var(--color-warning-light); color: var(--color-warning-dark); }

.campaign-list__context-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.campaign-list__context-icon :deep(svg) {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.campaign-list__context-copy { display: grid; gap: 3px; }
.campaign-list__context-copy strong { color: inherit; font-size: 14px; font-weight: 600; }
.campaign-list__context-copy small { color: var(--muted-text); font-size: 12px; line-height: 1.4; }

.campaign-list__status {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}

.campaign-list__status--draft { background: var(--color-gray-100); color: var(--color-gray-600); }
.campaign-list__status--review { background: var(--color-primary-100); color: var(--color-primary-700); }
.campaign-list__status--live { background: var(--color-primary-500); color: #fff; }
.campaign-list__status--completed { background: var(--color-success-light); color: var(--color-success-dark); }
.campaign-list__status--paused { background: var(--color-warning-light); color: var(--color-warning-dark); }

.campaign-list-anim-move { transition: transform 180ms cubic-bezier(0.2, 0.8, 0.2, 1); }
.campaign-list-anim-enter-active,
.campaign-list-anim-leave-active { transition: opacity var(--transition-fast), transform var(--transition-fast); }
.campaign-list-anim-enter-from,
.campaign-list-anim-leave-to { opacity: 0; transform: translateX(-8px); }

.campaign-float-enter-active,
.campaign-float-leave-active { transition: opacity var(--transition-fast), transform var(--transition-fast); }
.campaign-float-enter-from,
.campaign-float-leave-to { opacity: 0; transform: translateY(4px); }
</style>
