<script setup>
import { computed } from 'vue'
import { useOperationsStore } from '@/stores/operations'
import { formatShortDate } from '@/utils/calendar'

const store = useOperationsStore()

const healthConfig = {
  stable: { label: '안정', tone: 'healthy' },
  watch: { label: '관찰 필요', tone: 'watch' },
  risk: { label: '위험', tone: 'risk' },
}

const suggestionStateLabel = {
  pending: '대기 중',
  approved: '반영 완료',
  rejected: '보류',
}

const customerTagsText = computed({
  get: () => store.selectedCustomer?.tags.join(', ') ?? '',
  set: (value) => store.updateSelectedCustomerListField('tags', value),
})

const customerTraitsText = computed({
  get: () => store.selectedCustomer?.traits.join(', ') ?? '',
  set: (value) => store.updateSelectedCustomerListField('traits', value),
})

const selectedCustomerSuggestionCount = computed(() => {
  return store.customerSuggestionsForSelected.filter((item) => item.status === 'pending').length
})

function updateCustomerField(field, value) {
  store.updateSelectedCustomerField(field, value)
}

function confirmDeleteCustomer() {
  if (!store.selectedCustomer) {
    return
  }

  const confirmed = window.confirm(
    `${store.selectedCustomer.name} 고객을 운영 허브에서 삭제하시겠습니까?`,
  )

  if (confirmed) {
    store.deleteCustomer(store.selectedCustomer.id)
  }
}

function customerHealthLabel(health) {
  return healthConfig[health]?.label ?? health
}

function customerHealthTone(health) {
  return healthConfig[health]?.tone ?? 'neutral'
}
</script>

<template>
  <section class="customers-board">
    <aside
      class="surface-card customers-panel customers-panel--sticky"
      :class="{ 'customers-panel--focus': store.focusTarget === 'customers-list' }"
    >
      <header class="customers-panel__head">
        <h3>고객 목록</h3>

        <button class="soft-button" type="button" @click="store.createCustomer()">신규 고객</button>
      </header>

      <div class="customers-panel__body customers-panel__body--compact">
        <label class="customers-search">
          <span class="sr-only">고객 검색</span>
          <input
            :value="store.customerSearchQuery"
            type="text"
            placeholder="이름, 태그, 메모 검색"
            @input="store.setCustomerSearchQuery($event.target.value)"
          />
        </label>

        <div class="customers-list">
          <button
            v-for="customer in store.filteredCustomers"
            :key="customer.id"
            type="button"
            class="customers-item"
            :class="{ 'customers-item--active': store.selectedCustomerId === customer.id }"
            @click="store.setSelectedCustomer(customer.id)"
          >
            <div class="customers-item__copy">
              <strong>{{ customer.name }}</strong>
              <p>{{ customer.segment }}</p>
            </div>

            <div class="customers-item__meta">
              <span :class="['customers-badge', `customers-badge--${customerHealthTone(customer.health)}`]">
                {{ customerHealthLabel(customer.health) }}
              </span>
              <small>{{ formatShortDate(customer.lastQaUpdate) }}</small>
            </div>
          </button>

          <div v-if="!store.filteredCustomers.length" class="customers-empty">
            검색 조건에 맞는 고객이 없습니다.
          </div>
        </div>
      </div>
    </aside>

    <article class="surface-card customers-panel">
      <header class="customers-panel__head">
        <h3>{{ store.selectedCustomer?.name ?? '고객 상세' }}</h3>

        <div class="customers-panel__actions">
          <span v-if="store.selectedCustomer?.lastSavedAt" class="customers-save-hint">
            자동 저장 {{ store.selectedCustomer.lastSavedAt }}
          </span>
          <button
            v-if="store.selectedCustomer"
            class="ghost-button ghost-button--danger"
            type="button"
            @click="confirmDeleteCustomer"
          >
            삭제
          </button>
        </div>
      </header>

      <div v-if="store.selectedCustomer" class="customers-panel__body">
        <div class="customers-form-grid">
          <label class="customers-field">
            <span>고객명</span>
            <input
              :value="store.selectedCustomer.name"
              type="text"
              @input="updateCustomerField('name', $event.target.value)"
            />
          </label>

          <label class="customers-field">
            <span>담당자</span>
            <input
              :value="store.findMember(store.selectedCustomer.ownerId)?.name ?? ''"
              type="text"
              disabled
            />
          </label>
        </div>

        <div class="customers-form-grid">
          <label class="customers-field">
            <span>분류</span>
            <input
              :value="store.selectedCustomer.segment"
              type="text"
              @input="updateCustomerField('segment', $event.target.value)"
            />
          </label>

          <label class="customers-field">
            <span>이메일</span>
            <input
              :value="store.selectedCustomer.email"
              type="email"
              @input="updateCustomerField('email', $event.target.value)"
            />
          </label>
        </div>

        <label class="customers-field">
          <span>톤 가이드</span>
          <input
            :value="store.selectedCustomer.tone"
            type="text"
            @input="updateCustomerField('tone', $event.target.value)"
          />
        </label>

        <label class="customers-field">
          <span>특징</span>
          <textarea v-model="customerTraitsText" rows="3" />
        </label>

        <label class="customers-field">
          <span>태그</span>
          <textarea v-model="customerTagsText" rows="2" />
        </label>

        <label class="customers-field">
          <span>운영 메모</span>
          <textarea
            :value="store.selectedCustomer.memo"
            rows="6"
            @input="updateCustomerField('memo', $event.target.value)"
          />
        </label>
      </div>

      <div v-else class="customers-empty">
        좌측 목록에서 고객을 선택하면 이 영역에서 바로 편집할 수 있습니다.
      </div>
    </article>

    <aside class="customers-side">
      <article
        class="surface-card customers-panel"
        :class="{ 'customers-panel--focus': store.focusTarget === 'customers-suggestions' }"
      >
        <header class="customers-panel__head">
          <h3>AI 제안 {{ selectedCustomerSuggestionCount }}건</h3>
        </header>

        <div class="customers-panel__body customers-stack">
          <article
            v-for="suggestion in store.customerSuggestionsForSelected"
            :key="suggestion.id"
            class="customers-card"
          >
            <div class="customers-card__head">
              <div>
                <strong>{{ suggestion.title }}</strong>
                <p>{{ suggestion.source }}</p>
              </div>
              <span
                :class="[
                  'customers-badge',
                  `customers-badge--${suggestion.status === 'approved' ? 'healthy' : suggestion.status === 'rejected' ? 'neutral' : 'watch'}`,
                ]"
              >
                {{ suggestionStateLabel[suggestion.status] }}
              </span>
            </div>

            <p class="customers-muted">{{ suggestion.summary }}</p>

            <div class="customers-note">
              <strong>제안 메모</strong>
              <p>{{ suggestion.suggestedMemo }}</p>
            </div>

            <div class="customers-meta">
              <span>{{ suggestion.suggestedTags.join(', ') }}</span>
              <small>{{ suggestion.createdAt }}</small>
            </div>

            <div
              v-if="store.activeRole === 'admin' && suggestion.status === 'pending'"
              class="customers-card__actions"
            >
              <button class="ghost-button" type="button" @click="store.rejectSuggestion(suggestion.id)">
                보류
              </button>
              <button class="primary-button" type="button" @click="store.approveSuggestion(suggestion.id)">
                반영
              </button>
            </div>
          </article>

          <div v-if="!store.customerSuggestionsForSelected.length" class="customers-empty">
            선택한 고객에 대한 AI 제안이 없습니다.
          </div>
        </div>
      </article>

      <article class="surface-card customers-panel">
        <header class="customers-panel__head">
          <h3>변경 이력</h3>
        </header>

        <div class="customers-panel__body customers-stack">
          <div
            v-for="history in store.selectedCustomerHistory"
            :key="history.id"
            class="customers-history-item"
          >
            <span class="customers-history-item__dot" />
            <div>
              <strong>{{ history.title }}</strong>
              <p>{{ history.detail }}</p>
              <small>{{ history.createdAt }}</small>
            </div>
          </div>

          <div v-if="!store.selectedCustomerHistory.length" class="customers-empty">
            표시할 변경 이력이 없습니다.
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.customers-board {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(0, 1.15fr) minmax(300px, 360px);
  gap: 1rem;
  align-items: start;
}

.customers-side {
  display: grid;
  gap: 1rem;
}

.customers-panel {
  padding: 1rem;
  display: grid;
  gap: 0.9rem;
}

.customers-panel--sticky {
  position: sticky;
  top: 7.1rem;
}

.customers-panel--focus {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent);
}

.customers-panel__head,
.customers-card__head,
.customers-item,
.customers-item__meta,
.customers-meta,
.customers-card__actions,
.customers-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.customers-panel__head {
  align-items: flex-start;
}

.customers-panel__body,
.customers-stack,
.customers-list,
.customers-form-grid,
.customers-card,
.customers-history-item,
.customers-note,
.customers-field {
  display: grid;
  gap: 0.8rem;
}

.customers-panel__body--compact {
  gap: 0.75rem;
}

.customers-search input,
.customers-field input,
.customers-field textarea {
  width: 100%;
  padding: 0.8rem 0.9rem;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: var(--panel-muted);
}

.customers-field span {
  font-size: 0.82rem;
  font-weight: 700;
}

.customers-form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.customers-item,
.customers-card,
.customers-history-item {
  padding: 0.95rem;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: color-mix(in srgb, var(--panel-muted) 74%, white);
  text-align: left;
}

.customers-item--active {
  border-color: color-mix(in srgb, var(--accent-color) 30%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-color));
}

.customers-item__copy,
.customers-card,
.customers-note {
  display: grid;
  gap: 0.4rem;
}

.customers-item__copy p,
.customers-muted,
.customers-note p,
.customers-meta,
.customers-save-hint,
.customers-empty {
  color: var(--muted-text);
}

.customers-note,
.customers-empty {
  padding: 0.9rem;
  border-radius: 18px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
}

.customers-empty {
  background: var(--panel-muted);
}

.customers-badge {
  min-height: 2rem;
  padding: 0 0.8rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.78rem;
  font-weight: 700;
}

.customers-badge--healthy {
  background: color-mix(in srgb, var(--success-color) 12%, white);
  color: #287b47;
  border-color: color-mix(in srgb, var(--success-color) 30%, var(--border-color));
}

.customers-badge--watch {
  background: color-mix(in srgb, var(--warning-color) 16%, white);
  color: #9a6c0d;
  border-color: color-mix(in srgb, var(--warning-color) 30%, var(--border-color));
}

.customers-badge--neutral {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.customers-history-item {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
}

.customers-history-item__dot {
  width: 0.75rem;
  height: 0.75rem;
  margin-top: 0.35rem;
  border-radius: 999px;
  background: var(--accent-color);
  box-shadow: 0 0 0 6px color-mix(in srgb, var(--accent-color) 14%, transparent);
}

@media (max-width: 1380px) {
  .customers-board {
    grid-template-columns: 1fr;
  }

  .customers-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .customers-panel--sticky {
    position: static;
  }
}

@media (max-width: 980px) {
  .customers-side,
  .customers-form-grid {
    grid-template-columns: 1fr;
  }

  .customers-panel__head,
  .customers-item,
  .customers-item__meta,
  .customers-card__head,
  .customers-meta,
  .customers-card__actions,
  .customers-panel__actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
