<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { campaignLabels, campaignSidebarText, campaignStatusMeta } from '@/constants/campaignText'

const router = useRouter()
const store = usePlannerStore()

const folderCampaigns = computed(() => store.folderCampaigns)

function openCampaign(campaignId) {
  store.setActiveCampaign(campaignId)
  router.push({ name: 'dashboard' })
}

function restoreCampaign(campaignId) {
  store.restoreCampaignFromFolder(campaignId)
}

function getCampaignStatusMeta(status) {
  return campaignStatusMeta[status] ?? campaignStatusMeta.completed
}

function formatPartners(partners) {
  if (!Array.isArray(partners) || !partners.length) {
    return campaignSidebarText.noPartners
  }

  return partners.join(', ')
}
</script>

<template>
  <section class="campaign-folder">
    <header class="campaign-folder__hero">
      <div>
        <p class="campaign-folder__eyebrow">{{ campaignSidebarText.folderLabel }}</p>
        <h2>{{ campaignSidebarText.folderTitle }}</h2>
        <p class="campaign-folder__summary">{{ campaignSidebarText.folderSummary }}</p>
      </div>
      <span class="campaign-folder__count">{{ folderCampaigns.length }}건</span>
    </header>

    <section v-if="folderCampaigns.length" class="campaign-folder__grid" aria-label="보관된 캠페인 목록">
      <article
        v-for="campaign in folderCampaigns"
        :key="campaign.id"
        class="campaign-folder__card"
      >
        <div class="campaign-folder__card-head">
          <div class="campaign-folder__brand">
            <span class="campaign-folder__mark" :style="{ background: campaign.color }">
              {{ campaign.initials }}
            </span>
            <div>
              <p>{{ campaignSidebarText.folderStatus }}</p>
              <h3>{{ campaign.name }}</h3>
            </div>
          </div>
          <span class="campaign-folder__status" :class="`campaign-folder__status--${getCampaignStatusMeta(campaign.status).tone}`">
            {{ getCampaignStatusMeta(campaign.status).label }}
          </span>
        </div>

        <dl class="campaign-folder__meta">
          <div>
            <dt>{{ campaignLabels.period }}</dt>
            <dd>{{ campaign.period }}</dd>
          </div>
          <div>
            <dt>{{ campaignLabels.partners }}</dt>
            <dd>{{ formatPartners(campaign.partners) }}</dd>
          </div>
          <div>
            <dt>{{ campaignLabels.purpose }}</dt>
            <dd>{{ campaign.purpose || campaignSidebarText.emptyPurpose }}</dd>
          </div>
          <div>
            <dt>{{ campaignLabels.goals }}</dt>
            <dd>{{ campaign.goals || campaignSidebarText.emptyGoals }}</dd>
          </div>
        </dl>

        <div class="campaign-folder__actions">
          <button type="button" class="campaign-folder__button campaign-folder__button--ghost" @click="restoreCampaign(campaign.id)">
            {{ campaignSidebarText.folderRestore }}
          </button>
          <button type="button" class="campaign-folder__button campaign-folder__button--primary" @click="openCampaign(campaign.id)">
            {{ campaignSidebarText.folderOpen }}
          </button>
        </div>
      </article>
    </section>

    <section v-else class="campaign-folder__empty">
      <div class="campaign-folder__empty-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H10l2 2h6.5A2.5 2.5 0 0 1 21 9.5v8A2.5 2.5 0 0 1 18.5 20h-13A2.5 2.5 0 0 1 3 17.5v-10Z" />
        </svg>
      </div>
      <h3>{{ campaignSidebarText.folderEmpty }}</h3>
      <p>{{ campaignSidebarText.folderHelper }}</p>
    </section>
  </section>
</template>

<style scoped>
.campaign-folder {
  display: flex;
  width: 100%;
  max-width: var(--content-max-width);
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

.campaign-folder__hero,
.campaign-folder__card,
.campaign-folder__empty {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.campaign-folder__hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.campaign-folder__eyebrow {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-folder__hero h2 {
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

.campaign-folder__summary {
  margin-top: 8px;
  color: var(--muted-text);
  font-size: 13px;
}

.campaign-folder__count {
  display: inline-flex;
  min-height: 28px;
  align-items: center;
  padding: 0 12px;
  border-radius: var(--radius-full);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.campaign-folder__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.campaign-folder__card {
  display: grid;
  gap: 18px;
  padding: 22px;
}

.campaign-folder__card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.campaign-folder__brand {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.campaign-folder__mark {
  display: inline-flex;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.campaign-folder__brand p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-folder__brand h3 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
}

.campaign-folder__status {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.campaign-folder__status--completed {
  background: var(--color-success-light);
  color: var(--color-success-dark);
}

.campaign-folder__status--draft {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

.campaign-folder__status--review {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.campaign-folder__status--live {
  background: var(--color-primary-500);
  color: #ffffff;
}

.campaign-folder__status--info {
  background: var(--color-info-light);
  color: var(--color-info-dark);
}

.campaign-folder__status--paused {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
}

.campaign-folder__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 16px;
}

.campaign-folder__meta div {
  display: grid;
  gap: 4px;
  padding: 14px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: color-mix(in srgb, var(--panel-color) 92%, var(--panel-muted));
}

.campaign-folder__meta dt {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-folder__meta dd {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.campaign-folder__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.campaign-folder__button {
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.campaign-folder__button--ghost {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.campaign-folder__button--ghost:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.campaign-folder__button--primary {
  background: var(--color-primary-500);
  color: #ffffff;
}

.campaign-folder__button--primary:hover {
  background: var(--color-primary-600);
}

.campaign-folder__empty {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 48px 24px;
  text-align: center;
}

.campaign-folder__empty-icon {
  display: flex;
  width: 56px;
  height: 56px;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: var(--panel-muted);
  color: var(--muted-text);
}

.campaign-folder__empty-icon svg {
  width: 28px;
  height: 28px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.campaign-folder__empty h3 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.campaign-folder__empty p {
  max-width: 360px;
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .campaign-folder__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .campaign-folder__hero,
  .campaign-folder__card-head,
  .campaign-folder__actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .campaign-folder__meta {
    grid-template-columns: 1fr;
  }
}
</style>
