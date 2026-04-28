<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'
import CampaignCreateModal from '@/components/campaign/CampaignCreateModal.vue'
import { campaignLabels, campaignSidebarText, campaignStatusMeta } from '@/constants/campaignText'
import {
  CreateCampaign,
  UpdateCampaign,
  UpdateCampaignStatus,
} from '@/api/campaigns'

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()
const authStore = useAuthStore()

const FLOAT_MARGIN = 16
const FLOAT_OFFSET = 18
const PREVIEW_WIDTH = 320
const PREVIEW_HEIGHT_ESTIMATE = 320
const MENU_WIDTH = 224
const MENU_HEIGHT_ESTIMATE = 220

const createModalOpen = ref(false)
const campaignModalMode = ref('create')
const editingCampaignId = ref(null)
const previewCampaignId = ref(null)
const contextCampaignId = ref(null)
const draggedCampaignId = ref(null)
const dropTargetCampaignId = ref(null)
const dropPlacement = ref('before')
const folderDropState = ref('idle')

const previewPosition = reactive({
  top: FLOAT_MARGIN,
  left: FLOAT_MARGIN,
})

const contextMenuPosition = reactive({
  top: FLOAT_MARGIN,
  left: FLOAT_MARGIN,
})

const profile = computed(() => store.findMember(store.currentUserId))
const userStorageKey = computed(
  () =>
    authStore.user?.userId ||
    authStore.user?.id ||
    authStore.user?.loginId ||
    authStore.user?.email ||
    store.currentUserId,
)
const sidebarCampaigns = computed(() => store.sidebarCampaigns)
const visualSidebarCampaigns = computed(() => {
  const campaigns = [...sidebarCampaigns.value]
  const sourceCampaignId = draggedCampaignId.value
  const targetCampaignId = dropTargetCampaignId.value

  if (!sourceCampaignId || !targetCampaignId || sourceCampaignId === targetCampaignId) {
    return campaigns
  }

  const sourceIndex = campaigns.findIndex((campaign) => campaign.id === sourceCampaignId)

  if (sourceIndex === -1) {
    return campaigns
  }

  const [sourceCampaign] = campaigns.splice(sourceIndex, 1)
  const targetIndex = campaigns.findIndex((campaign) => campaign.id === targetCampaignId)

  if (targetIndex === -1) {
    return sidebarCampaigns.value
  }

  const insertIndex = dropPlacement.value === 'after' ? targetIndex + 1 : targetIndex
  campaigns.splice(insertIndex, 0, sourceCampaign)

  return campaigns
})
const folderCampaignCount = computed(() => store.campaignFolderCount)
const isFolderRoute = computed(() => route.name === 'campaign-folder')
const draggedCampaign = computed(
  () => store.campaigns.find((campaign) => campaign.id === draggedCampaignId.value) ?? null,
)
const canDropIntoFolder = computed(() => draggedCampaign.value?.status === 'completed')
const folderButtonTitle = computed(() => {
  if (!draggedCampaign.value) {
    return campaignSidebarText.folderLabel
  }

  return canDropIntoFolder.value
    ? campaignSidebarText.folderDropReady
    : campaignSidebarText.folderDropBlocked
})

const canCreateCampaign = computed(() => {
  const roleCandidates = [
    authStore.user?.role,
    authStore.user?.roleName,
    authStore.user?.authority,
    profile.value?.role,
  ]
    .filter(Boolean)
    .map((role) => String(role))

  return true

  // return roleCandidates.some((role) =>
  //   /HQ|Admin|bo-Admin|human|nomal/.test(role),
  // )
})

const previewCampaign = computed(
  () => store.campaigns.find((campaign) => campaign.id === previewCampaignId.value) ?? null,
)

const contextCampaign = computed(
  () => store.campaigns.find((campaign) => campaign.id === contextCampaignId.value) ?? null,
)

const editingCampaign = computed(
  () => store.campaigns.find((campaign) => campaign.id === editingCampaignId.value) ?? null,
)

const previewCampaignStyle = computed(() => ({
  top: `${previewPosition.top}px`,
  left: `${previewPosition.left}px`,
}))

const contextMenuStyle = computed(() => ({
  top: `${contextMenuPosition.top}px`,
  left: `${contextMenuPosition.left}px`,
}))

const previewCampaignStatus = computed(() => getCampaignStatusMeta(previewCampaign.value?.status))

const contextCampaignStatus = computed(() => getCampaignStatusMeta(contextCampaign.value?.status))

const contextMenuActions = computed(() => {
  const campaign = contextCampaign.value

  if (!campaign) {
    return []
  }

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

let previewHideTimer = null

function isServerCampaignId(campaignId) {
  return /^\d+$/.test(String(campaignId))
}

function getCampaignStatusMeta(status) {
  return campaignStatusMeta[status] ?? { label: campaignStatusMeta.draft.label, tone: 'draft' }
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function clearPreviewHideTimer() {
  if (previewHideTimer) {
    window.clearTimeout(previewHideTimer)
    previewHideTimer = null
  }
}

function scheduleCampaignPreviewHide() {
  clearPreviewHideTimer()
  previewHideTimer = window.setTimeout(() => {
    previewCampaignId.value = null
  }, 140)
}

function cancelCampaignPreviewHide() {
  clearPreviewHideTimer()
}

function closeCampaignPreview() {
  clearPreviewHideTimer()
  previewCampaignId.value = null
}

function closeCampaignMenu() {
  contextCampaignId.value = null
}

function closeFloatingPanels() {
  closeCampaignPreview()
  closeCampaignMenu()
}

function positionPreview(element) {
  if (!(element instanceof HTMLElement)) {
    return
  }

  const rect = element.getBoundingClientRect()

  previewPosition.left = clamp(
    rect.right + FLOAT_OFFSET,
    FLOAT_MARGIN,
    window.innerWidth - PREVIEW_WIDTH - FLOAT_MARGIN,
  )

  previewPosition.top = clamp(
    rect.top - 10,
    FLOAT_MARGIN,
    window.innerHeight - PREVIEW_HEIGHT_ESTIMATE - FLOAT_MARGIN,
  )
}

function openCampaignPreview(campaign, event) {
  if (contextCampaignId.value || draggedCampaignId.value) {
    return
  }

  cancelCampaignPreviewHide()
  previewCampaignId.value = campaign.id
  positionPreview(event.currentTarget)
}

function openCampaignMenu(campaign, event) {
  if (!canCreateCampaign.value) {
    return
  }

  event.preventDefault()
  closeCampaignPreview()

  contextMenuPosition.left = clamp(
    event.clientX + 8,
    FLOAT_MARGIN,
    window.innerWidth - MENU_WIDTH - FLOAT_MARGIN,
  )

  contextMenuPosition.top = clamp(
    event.clientY + 8,
    FLOAT_MARGIN,
    window.innerHeight - MENU_HEIGHT_ESTIMATE - FLOAT_MARGIN,
  )

  contextCampaignId.value = campaign.id
}

function openCreateModal() {
  if (!canCreateCampaign.value) {
    return
  }

  closeFloatingPanels()
  campaignModalMode.value = 'create'
  editingCampaignId.value = null
  createModalOpen.value = true
}

function openEditModal(campaignId) {
  if (!canCreateCampaign.value) {
    return
  }

  editingCampaignId.value = campaignId
  campaignModalMode.value = 'edit'
  closeFloatingPanels()
  createModalOpen.value = true
}

function closeCreateModal() {
  createModalOpen.value = false
  campaignModalMode.value = 'create'
  editingCampaignId.value = null
}

function selectCampaign(campaignId) {
  closeFloatingPanels()
  store.setActiveCampaign(campaignId)
  router.push({ name: 'dashboard' })
}

function openFolderPage() {
  closeFloatingPanels()
  router.push({ name: 'campaign-folder' })
}

async function handleCampaignSubmit(payload) {
  try {
    const campaign =
      campaignModalMode.value === 'edit' && editingCampaignId.value
        ? await saveExistingCampaign(editingCampaignId.value, payload)
        : await saveNewCampaign(payload)

    closeCreateModal()

    if (campaign?.id) {
      selectCampaign(campaign.id)
    }
  } catch (error) {
    console.warn('Campaign save failed', error)
  }
}

async function saveNewCampaign(payload) {
  const savedCampaign = await CreateCampaign(payload)
  return store.createCampaign(savedCampaign)
}

async function saveExistingCampaign(campaignId, payload) {
  if (!isServerCampaignId(campaignId)) {
    return store.updateCampaign(campaignId, payload)
  }

  const savedCampaign = await UpdateCampaign(campaignId, payload)
  return store.updateCampaign(campaignId, savedCampaign)
}

async function handleContextMenuAction(action) {
  const campaign = contextCampaign.value

  if (!campaign) {
    return
  }

  if (action.key === 'edit') {
    openEditModal(campaign.id)
    return
  }

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

function handleGlobalPointerDown(event) {
  const target = event.target

  if (!(target instanceof HTMLElement)) {
    closeCampaignMenu()
    return
  }

  if (
    contextCampaignId.value &&
    !target.closest('.campaign-rail__context-menu') &&
    !target.closest('.campaign-rail__campaign')
  ) {
    closeCampaignMenu()
  }
}

function handleGlobalKeydown(event) {
  if (event.key !== 'Escape') {
    return
  }

  if (createModalOpen.value) {
    closeCreateModal()
    return
  }

  closeFloatingPanels()
}

function handleViewportChange() {
  closeFloatingPanels()
}

function formatPartnerSummary(partners) {
  if (!Array.isArray(partners) || !partners.length) {
    return campaignSidebarText.noPartners
  }

  if (partners.length <= 2) {
    return partners.join(', ')
  }

  return `${partners.slice(0, 2).join(', ')} +${partners.length - 2}`
}

function getCampaignBadgeMeta(status) {
  if (status === 'paused') {
    return {
      tone: 'warning',
      label: campaignSidebarText.pausedAlert,
    }
  }

  if (status === 'completed') {
    return {
      tone: 'success',
      label: campaignSidebarText.completedBadge,
    }
  }

  return null
}

function resetDragState() {
  draggedCampaignId.value = null
  dropTargetCampaignId.value = null
  dropPlacement.value = 'before'
  folderDropState.value = 'idle'
}

function commitPendingCampaignReorder() {
  if (
    !draggedCampaignId.value ||
    !dropTargetCampaignId.value ||
    draggedCampaignId.value === dropTargetCampaignId.value
  ) {
    return false
  }

  return store.reorderCampaign(
    draggedCampaignId.value,
    dropTargetCampaignId.value,
    dropPlacement.value,
  )
}

function handleCampaignDragStart(campaign, event) {
  draggedCampaignId.value = campaign.id
  dropTargetCampaignId.value = null
  folderDropState.value = campaign.status === 'completed' ? 'ready' : 'blocked'
  closeFloatingPanels()

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', campaign.id)
  }
}

function handleCampaignDragEnd() {
  resetDragState()
}

function handleCampaignDragOver(campaign, event) {
  if (!draggedCampaignId.value) {
    return
  }

  if (draggedCampaignId.value === campaign.id) {
    if (dropTargetCampaignId.value) {
      event.preventDefault()

      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
      }
    }

    return
  }

  event.preventDefault()
  const rect = event.currentTarget?.getBoundingClientRect()

  if (rect) {
    dropPlacement.value = event.clientY > rect.top + rect.height / 2 ? 'after' : 'before'
  }

  dropTargetCampaignId.value = campaign.id

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function handleCampaignDrop(campaign, event) {
  event.preventDefault()

  if (!draggedCampaignId.value) {
    resetDragState()
    return
  }

  if (draggedCampaignId.value === campaign.id) {
    commitPendingCampaignReorder()
    resetDragState()
    return
  }

  store.reorderCampaign(draggedCampaignId.value, campaign.id, dropPlacement.value)
  resetDragState()
}

function handleCampaignEndDragOver(event) {
  if (!draggedCampaignId.value) {
    return
  }

  const lastCampaign = sidebarCampaigns.value
    .filter((campaign) => campaign.id !== draggedCampaignId.value)
    .at(-1)

  if (!lastCampaign) {
    return
  }

  event.preventDefault()
  dropTargetCampaignId.value = lastCampaign.id
  dropPlacement.value = 'after'

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function handleCampaignEndDrop(event) {
  event.preventDefault()

  const lastCampaign = sidebarCampaigns.value
    .filter((campaign) => campaign.id !== draggedCampaignId.value)
    .at(-1)

  if (!draggedCampaignId.value || !lastCampaign) {
    resetDragState()
    return
  }

  store.reorderCampaign(draggedCampaignId.value, lastCampaign.id, 'after')
  resetDragState()
}

function handleFolderDragEnter() {
  if (!draggedCampaign.value) {
    return
  }

  folderDropState.value = canDropIntoFolder.value ? 'ready' : 'blocked'
}

function handleFolderDragOver(event) {
  if (!draggedCampaign.value) {
    return
  }

  folderDropState.value = canDropIntoFolder.value ? 'ready' : 'blocked'

  if (!canDropIntoFolder.value) {
    return
  }

  event.preventDefault()

  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

function handleFolderDragLeave() {
  folderDropState.value = 'idle'
}

function handleFolderDrop(event) {
  event.preventDefault()

  if (!draggedCampaign.value || !canDropIntoFolder.value) {
    resetDragState()
    return
  }

  store.moveCampaignToFolder(draggedCampaign.value.id)
  closeFloatingPanels()
  resetDragState()
}

watch(
  userStorageKey,
  (nextKey) => {
    store.setCampaignUiOwnerKey(nextKey)
  },
  { immediate: true },
)

onMounted(() => {
  window.addEventListener('pointerdown', handleGlobalPointerDown)
  window.addEventListener('keydown', handleGlobalKeydown)
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('scroll', handleViewportChange, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerdown', handleGlobalPointerDown)
  window.removeEventListener('keydown', handleGlobalKeydown)
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('scroll', handleViewportChange, true)
  clearPreviewHideTimer()
  resetDragState()
})
</script>

<template>
  <aside class="campaign-rail" :aria-label="campaignSidebarText.sidebar">
    <RouterLink
      to="/dashboard"
      class="campaign-rail__logo"
      :aria-label="campaignSidebarText.goToDashboard"
    >
      <span class="campaign-rail__logo-mark">C</span>
    </RouterLink>

    <div class="campaign-rail__divider" />

    <div class="campaign-rail__campaigns">
      <TransitionGroup
        tag="nav"
        name="campaign-rail-list"
        class="campaign-rail__list"
        :aria-label="campaignSidebarText.campaignList"
        :title="campaignSidebarText.reorderHint"
      >
        <button
          v-for="campaign in visualSidebarCampaigns"
          :key="campaign.id"
          type="button"
          class="campaign-rail__campaign"
          :class="{
            active: store.activeCampaignId === campaign.id,
            'campaign-rail__campaign--dragging': draggedCampaignId === campaign.id,
            'campaign-rail__campaign--drop-target': dropTargetCampaignId === campaign.id,
            'campaign-rail__campaign--drop-before':
              dropTargetCampaignId === campaign.id && dropPlacement === 'before',
            'campaign-rail__campaign--drop-after':
              dropTargetCampaignId === campaign.id && dropPlacement === 'after',
          }"
          :title="campaign.name"
          :aria-label="campaign.name"
          :aria-current="store.activeCampaignId === campaign.id ? 'page' : undefined"
          draggable="true"
          @click="selectCampaign(campaign.id)"
          @contextmenu="openCampaignMenu(campaign, $event)"
          @pointerenter="openCampaignPreview(campaign, $event)"
          @pointerleave="scheduleCampaignPreviewHide"
          @focus="openCampaignPreview(campaign, $event)"
          @blur="scheduleCampaignPreviewHide"
          @dragstart="handleCampaignDragStart(campaign, $event)"
          @dragend="handleCampaignDragEnd"
          @dragover="handleCampaignDragOver(campaign, $event)"
          @drop="handleCampaignDrop(campaign, $event)"
        >
          <span class="campaign-rail__campaign-indicator" />
          <span class="campaign-rail__campaign-icon" :style="{ background: campaign.color }">
            {{ campaign.initials }}
            <span
              v-if="getCampaignBadgeMeta(campaign.status)"
              class="campaign-rail__campaign-badge"
              :class="`campaign-rail__campaign-badge--${getCampaignBadgeMeta(campaign.status).tone}`"
              :title="getCampaignBadgeMeta(campaign.status).label"
              :aria-label="getCampaignBadgeMeta(campaign.status).label"
            >
              <template v-if="getCampaignBadgeMeta(campaign.status).tone === 'warning'">!</template>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M7 10v8a2 2 0 0 0 2 2h6.7a2 2 0 0 0 1.9-1.36l1.6-4.5A2 2 0 0 0 17.3 11H14V6.8a1.8 1.8 0 0 0-3.14-1.2L7 10Z"
                />
                <path d="M7 10H4v10h3" />
              </svg>
            </span>
          </span>
        </button>

        <button
          key="campaign-create"
          type="button"
          class="campaign-rail__create"
          :class="{ 'campaign-rail__create--disabled': !canCreateCampaign }"
          :disabled="!canCreateCampaign"
          :title="campaignLabels.createCampaign"
          :aria-label="campaignLabels.createCampaign"
          @click="openCreateModal"
          @dragover="handleCampaignEndDragOver"
          @drop="handleCampaignEndDrop"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
        </button>
      </TransitionGroup>
    </div>

    <div class="campaign-rail__footer">
      <button
        type="button"
        class="campaign-rail__folder"
        :class="{
          active: isFolderRoute,
          'campaign-rail__folder--drop-ready': folderDropState === 'ready' && draggedCampaignId,
          'campaign-rail__folder--drop-blocked': folderDropState === 'blocked' && draggedCampaignId,
        }"
        :title="folderButtonTitle"
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
        <span v-if="folderCampaignCount" class="campaign-rail__folder-count">
          {{ folderCampaignCount }}
        </span>
      </button>
    </div>
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
      <aside
        v-if="previewCampaign"
        class="campaign-rail__preview"
        :style="previewCampaignStyle"
        @pointerenter="cancelCampaignPreviewHide"
        @pointerleave="scheduleCampaignPreviewHide"
      >
        <div class="campaign-rail__preview-head">
          <div class="campaign-rail__preview-brand">
            <span
              class="campaign-rail__preview-mark"
              :style="{ background: previewCampaign.color }"
            >
              {{ previewCampaign.initials }}
            </span>
            <div class="campaign-rail__preview-copy">
              <p>{{ campaignSidebarText.campaignBrief }}</p>
              <h3>{{ previewCampaign.name }}</h3>
            </div>
          </div>
          <span
            class="campaign-rail__status"
            :class="`campaign-rail__status--${previewCampaignStatus.tone}`"
          >
            {{ previewCampaignStatus.label }}
          </span>
        </div>

        <div class="campaign-rail__preview-meta">
          <div class="campaign-rail__preview-stat">
            <span>{{ campaignLabels.period }}</span>
            <strong>{{ previewCampaign.period }}</strong>
          </div>
          <div class="campaign-rail__preview-stat">
            <span>{{ campaignLabels.partners }}</span>
            <strong>{{ formatPartnerSummary(previewCampaign.partners) }}</strong>
          </div>
        </div>

        <div v-if="previewCampaign.tags?.length" class="campaign-rail__preview-tags">
          <span
            v-for="tag in previewCampaign.tags.slice(0, 4)"
            :key="tag"
            class="campaign-rail__tag"
          >
            #{{ tag }}
          </span>
        </div>

        <div class="campaign-rail__preview-section">
          <span>{{ campaignLabels.purpose }}</span>
          <p>{{ previewCampaign.purpose || campaignSidebarText.emptyPurpose }}</p>
        </div>

        <div class="campaign-rail__preview-section">
          <span>{{ campaignLabels.goals }}</span>
          <p>{{ previewCampaign.goals || campaignSidebarText.emptyGoals }}</p>
        </div>

        <div class="campaign-rail__preview-section">
          <span>{{ campaignLabels.mainMessage }}</span>
          <p>{{ previewCampaign.mainMessage || campaignSidebarText.emptyMainMessage }}</p>
        </div>
      </aside>
    </Transition>

    <Transition name="campaign-float">
      <div
        v-if="contextCampaign"
        class="campaign-rail__context-menu"
        :style="contextMenuStyle"
        role="menu"
        :aria-label="campaignSidebarText.campaignActions"
      >
        <div class="campaign-rail__context-head">
          <p>{{ campaignSidebarText.campaignActions }}</p>
          <strong>{{ contextCampaign.name }}</strong>
          <span
            class="campaign-rail__status"
            :class="`campaign-rail__status--${contextCampaignStatus.tone}`"
          >
            {{ contextCampaignStatus.label }}
          </span>
        </div>

        <div class="campaign-rail__context-list">
          <button
            v-for="action in contextMenuActions"
            :key="action.key"
            type="button"
            class="campaign-rail__context-item"
            :class="`campaign-rail__context-item--${action.tone}`"
            @click="handleContextMenuAction(action)"
          >
            <span class="campaign-rail__context-icon" v-html="action.icon" />
            <span class="campaign-rail__context-copy">
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
.campaign-rail {
  width: var(--sidebar-icon-width);
  flex-shrink: 0;
  background: var(--sidebar-color);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 0;
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 20;
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal);
}

.campaign-rail__logo,
.campaign-rail__create,
.campaign-rail__campaign,
.campaign-rail__folder {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.campaign-rail__logo,
.campaign-rail__create,
.campaign-rail__folder,
.campaign-rail__campaign-icon,
.campaign-rail__preview-mark {
  display: flex;
  align-items: center;
  justify-content: center;
}

.campaign-rail__logo {
  border-radius: var(--radius-lg);
  background: var(--color-primary-500);
  text-decoration: none;
  transition: background var(--transition-fast);
}

.campaign-rail__logo:hover {
  background: var(--color-primary-600);
}

.campaign-rail__logo-mark {
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.campaign-rail__divider {
  width: 100%;
  height: 1px;
  background: var(--border-color);
  margin: 12px 0;
  flex-shrink: 0;
}

.campaign-rail__campaigns {
  width: 100%;
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 0 12px;
}

.campaign-rail__list {
  display: flex;
  width: 100%;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  overflow-y: auto;
  padding: 0 0 12px;
}

.campaign-rail__list::-webkit-scrollbar {
  width: 0;
}

.campaign-rail-list-move {
  transition: transform 180ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.campaign-rail-list-enter-active,
.campaign-rail-list-leave-active {
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
}

.campaign-rail-list-enter-from,
.campaign-rail-list-leave-to {
  opacity: 0;
  transform: scale(0.92);
}

.campaign-rail__campaign {
  position: relative;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
  will-change: transform;
}

.campaign-rail__campaign--dragging {
  opacity: 0.42;
}

.campaign-rail__campaign--drop-target .campaign-rail__campaign-icon {
  box-shadow:
    0 0 0 2px color-mix(in srgb, var(--color-primary-300) 75%, white),
    var(--shadow-md);
}

.campaign-rail__campaign--drop-before::before,
.campaign-rail__campaign--drop-after::after {
  content: '';
  position: absolute;
  left: 3px;
  right: 3px;
  z-index: 2;
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--color-primary-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary-200) 34%, transparent);
  pointer-events: none;
}

.campaign-rail__campaign--drop-before::before {
  top: -6px;
}

.campaign-rail__campaign--drop-after::after {
  bottom: -6px;
}

.campaign-rail__campaign-indicator {
  position: absolute;
  left: -12px;
  top: 50%;
  width: 4px;
  height: 8px;
  border-radius: 0 999px 999px 0;
  background: transparent;
  transform: translateY(-50%);
  transition:
    height var(--transition-fast),
    background var(--transition-fast);
}

.campaign-rail__campaign-icon {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  transition:
    border-radius var(--transition-fast),
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.campaign-rail__campaign-badge {
  position: absolute;
  right: -4px;
  bottom: -4px;
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--sidebar-color);
  border-radius: 50%;
  box-shadow: var(--shadow-sm);
  font-size: 10px;
  font-weight: 900;
  line-height: 1;
}

.campaign-rail__campaign-badge svg {
  width: 10px;
  height: 10px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.campaign-rail__campaign-badge--warning {
  background: var(--color-danger);
  color: #ffffff;
}

.campaign-rail__campaign-badge--success {
  background: var(--color-success);
  color: #ffffff;
}

.campaign-rail__campaign:hover .campaign-rail__campaign-icon,
.campaign-rail__campaign.active .campaign-rail__campaign-icon,
.campaign-rail__campaign:focus-visible .campaign-rail__campaign-icon {
  border-radius: var(--radius-md);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.campaign-rail__campaign:hover .campaign-rail__campaign-indicator,
.campaign-rail__campaign:focus-visible .campaign-rail__campaign-indicator {
  height: 16px;
  background: var(--muted-text);
}

.campaign-rail__campaign.active .campaign-rail__campaign-indicator {
  height: 28px;
  background: var(--color-primary-500);
}

.campaign-rail__campaign:focus-visible,
.campaign-rail__create:focus-visible,
.campaign-rail__folder:focus-visible,
.campaign-rail__context-item:focus-visible {
  outline: 2px solid color-mix(in srgb, var(--color-primary-300) 90%, transparent);
  outline-offset: 2px;
}

.campaign-rail__create,
.campaign-rail__folder {
  border-radius: var(--radius-lg);
  background: var(--panel-muted);
  color: var(--nav-icon-color);
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    opacity var(--transition-fast),
    box-shadow var(--transition-fast);
}

.campaign-rail__create:hover:not(:disabled),
.campaign-rail__folder:hover {
  background: var(--nav-icon-hover-bg);
  color: var(--nav-icon-hover-color);
  box-shadow: var(--shadow-sm);
}

.campaign-rail__create--disabled {
  cursor: not-allowed;
  opacity: 0.42;
}

.campaign-rail__create svg,
.campaign-rail__folder svg,
.campaign-rail__context-icon svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.campaign-rail__footer {
  display: flex;
  width: 100%;
  justify-content: center;
  padding: 0 0 16px;
  flex-shrink: 0;
}

.campaign-rail__folder {
  position: relative;
  background: var(--color-gray-100);
  color: var(--color-gray-500);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-gray-200) 86%, white);
}

.campaign-rail__folder.active {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-primary-200) 92%, white);
}

.campaign-rail__folder--drop-ready {
  background: var(--color-success-light);
  color: var(--color-success-dark);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--color-success) 28%, white),
    0 0 0 4px color-mix(in srgb, var(--color-success) 14%, transparent);
}

.campaign-rail__folder--drop-blocked {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--color-danger) 26%, white),
    0 0 0 4px color-mix(in srgb, var(--color-danger) 10%, transparent);
}

.campaign-rail__folder-count {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
}

.campaign-rail__preview,
.campaign-rail__context-menu {
  position: fixed;
  z-index: 60;
  width: 320px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-elevated);
}

.campaign-rail__preview {
  padding: 20px;
}

.campaign-rail__preview::before {
  content: '';
  position: absolute;
  top: 22px;
  left: -6px;
  width: 12px;
  height: 12px;
  border-left: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-color);
  transform: rotate(45deg);
}

.campaign-rail__preview-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.campaign-rail__preview-brand {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.campaign-rail__preview-mark {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
}

.campaign-rail__preview-copy {
  min-width: 0;
}

.campaign-rail__preview-copy p,
.campaign-rail__context-head p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-rail__preview-copy h3,
.campaign-rail__context-head strong {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.campaign-rail__preview-copy h3 {
  margin-top: 2px;
}

.campaign-rail__preview-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.campaign-rail__preview-stat {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: color-mix(in srgb, var(--panel-color) 92%, var(--panel-muted));
}

.campaign-rail__preview-stat span,
.campaign-rail__preview-section span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

.campaign-rail__preview-stat strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.45;
}

.campaign-rail__preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.campaign-rail__tag {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 12px;
  font-weight: 600;
}

.campaign-rail__preview-section {
  display: grid;
  gap: 6px;
  margin-top: 16px;
}

.campaign-rail__preview-section p {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.campaign-rail__context-menu {
  padding: 8px;
}

.campaign-rail__context-head {
  display: grid;
  gap: 4px;
  padding: 10px 12px 14px;
  border-bottom: 1px solid var(--border-color);
}

.campaign-rail__context-list {
  display: grid;
  gap: 4px;
  padding-top: 8px;
}

.campaign-rail__context-item {
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
  transition:
    background var(--transition-fast),
    color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.campaign-rail__context-item:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

.campaign-rail__context-item--success:hover {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.campaign-rail__context-item--warning:hover {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.campaign-rail__context-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 1px;
}

.campaign-rail__context-icon :deep(svg) {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.campaign-rail__context-copy {
  display: grid;
  gap: 3px;
}

.campaign-rail__context-copy strong {
  color: inherit;
  font-size: 14px;
  font-weight: 600;
}

.campaign-rail__context-copy small {
  color: var(--muted-text);
  font-size: 12px;
  line-height: 1.4;
}

.campaign-rail__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.campaign-rail__status--draft {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

.campaign-rail__status--review {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.campaign-rail__status--live {
  background: var(--color-primary-500);
  color: #ffffff;
}

.campaign-rail__status--info {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.campaign-rail__status--completed {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.campaign-rail__status--paused {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.campaign-float-enter-active,
.campaign-float-leave-active {
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
}

.campaign-float-enter-from,
.campaign-float-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
