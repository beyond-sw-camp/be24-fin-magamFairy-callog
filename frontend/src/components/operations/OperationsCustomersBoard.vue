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

const panelClass =
  'grid gap-[0.9rem] rounded-[24px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4 shadow-[var(--shadow-soft)]'
const headerClass =
  'flex items-start justify-between gap-[0.75rem] max-[980px]:flex-col max-[980px]:items-start'
const fieldClass = 'grid gap-[0.8rem]'
const fieldLabelClass = 'text-[0.82rem] font-bold text-[color:var(--text-primary)]'
const inputClass =
  'w-full rounded-2xl border border-[color:var(--border-color)] bg-[var(--panel-muted)] px-4 py-3 text-sm text-[color:var(--text-primary)] outline-none transition duration-200 placeholder:text-[color:var(--muted-text)] focus:border-[color:var(--accent-color)] focus:ring-2 focus:ring-[color:color-mix(in_srgb,var(--accent-color)_12%,transparent)]'
const itemCardClass =
  'flex items-start justify-between gap-[0.75rem] rounded-[20px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_74%,white)] px-[0.95rem] py-[0.95rem] text-left transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)] max-[980px]:flex-col max-[980px]:items-start'
const mutedCardClass =
  'grid gap-2 rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-color)] px-4 py-4'
const emptyCardClass =
  'rounded-[18px] border border-[color:var(--border-color)] bg-[var(--panel-muted)] px-4 py-4 text-sm text-[color:var(--muted-text)]'
const badgeBaseClass =
  'inline-flex min-h-8 items-center justify-center rounded-full border border-[color:var(--border-color)] px-3 text-[0.78rem] font-bold'
const softButtonClass =
  'inline-flex min-h-[2.4rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--border-color)] bg-[var(--panel-muted)] px-4 text-sm font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)]'
const ghostButtonClass =
  'inline-flex min-h-[2.4rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--border-color)] bg-transparent px-4 text-sm font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:bg-[var(--panel-muted)]'
const dangerButtonClass =
  'inline-flex min-h-[2.4rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--danger-color)] bg-transparent px-4 text-sm font-semibold text-[color:var(--danger-color)] transition duration-200 hover:-translate-y-px hover:bg-[color:color-mix(in_srgb,var(--danger-color)_10%,white)]'
const primaryButtonClass =
  'inline-flex min-h-[2.4rem] items-center justify-center gap-2 rounded-xl border border-[color:var(--accent-color)] bg-[var(--accent-color)] px-4 text-sm font-semibold text-white transition duration-200 hover:-translate-y-px hover:shadow-[var(--shadow-soft)]'
const timelineDotClass = 'mt-1.5 h-3 w-3 rounded-full'
const focusPanelStyle = {
  borderColor: 'color-mix(in srgb, var(--accent-color) 30%, var(--border-color))',
  boxShadow: '0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent)',
}
const activeItemStyle = {
  borderColor: 'color-mix(in srgb, var(--accent-color) 30%, var(--border-color))',
  background: 'color-mix(in srgb, var(--accent-color) 10%, var(--panel-color))',
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

function badgeStyle(tone) {
  if (tone === 'healthy') {
    return {
      background: 'color-mix(in srgb, var(--success-color) 12%, white)',
      borderColor: 'color-mix(in srgb, var(--success-color) 30%, var(--border-color))',
      color: '#287b47',
    }
  }

  if (tone === 'watch') {
    return {
      background: 'color-mix(in srgb, var(--warning-color) 16%, white)',
      borderColor: 'color-mix(in srgb, var(--warning-color) 30%, var(--border-color))',
      color: '#9a6c0d',
    }
  }

  if (tone === 'risk') {
    return {
      background: 'color-mix(in srgb, var(--danger-color) 12%, white)',
      borderColor: 'color-mix(in srgb, var(--danger-color) 28%, var(--border-color))',
      color: '#b2455f',
    }
  }

  return {
    background: 'var(--panel-muted)',
    color: 'var(--muted-text)',
  }
}
</script>

<template>
  <section class="grid items-start gap-4 [grid-template-columns:minmax(280px,320px)_minmax(0,1.15fr)_minmax(300px,360px)] max-[1380px]:grid-cols-1">
    <aside
      :class="[panelClass, 'sticky top-[7.1rem] max-[1380px]:static']"
      :style="store.focusTarget === 'customers-list' ? focusPanelStyle : null"
    >
      <header :class="headerClass">
        <h3 class="text-base font-semibold text-[color:var(--text-primary)]">고객 목록</h3>

        <button :class="softButtonClass" type="button" @click="store.createCustomer()">신규 고객</button>
      </header>

      <div class="grid gap-3">
        <label class="grid gap-2">
          <span class="sr-only">고객 검색</span>
          <input
            :value="store.customerSearchQuery"
            :class="inputClass"
            type="text"
            placeholder="이름, 태그, 메모 검색"
            @input="store.setCustomerSearchQuery($event.target.value)"
          />
        </label>

        <div class="grid gap-3">
          <button
            v-for="customer in store.filteredCustomers"
            :key="customer.id"
            type="button"
            :class="itemCardClass"
            :style="store.selectedCustomerId === customer.id ? activeItemStyle : null"
            @click="store.setSelectedCustomer(customer.id)"
          >
            <div class="grid gap-1">
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ customer.name }}</strong>
              <p class="text-sm text-[color:var(--muted-text)]">{{ customer.segment }}</p>
            </div>

            <div class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start">
              <span
                :class="badgeBaseClass"
                :style="badgeStyle(customerHealthTone(customer.health))"
              >
                {{ customerHealthLabel(customer.health) }}
              </span>
              <small class="text-xs text-[color:var(--muted-text)]">{{ formatShortDate(customer.lastQaUpdate) }}</small>
            </div>
          </button>

          <div v-if="!store.filteredCustomers.length" :class="emptyCardClass">
            검색 조건에 맞는 고객이 없습니다.
          </div>
        </div>
      </div>
    </aside>

    <article :class="panelClass">
      <header :class="headerClass">
        <h3 class="text-base font-semibold text-[color:var(--text-primary)]">
          {{ store.selectedCustomer?.name ?? '고객 상세' }}
        </h3>

        <div class="flex items-center gap-3 max-[980px]:flex-col max-[980px]:items-start">
          <span
            v-if="store.selectedCustomer?.lastSavedAt"
            class="text-sm text-[color:var(--muted-text)]"
          >
            자동 저장 {{ store.selectedCustomer.lastSavedAt }}
          </span>
          <button
            v-if="store.selectedCustomer"
            :class="dangerButtonClass"
            type="button"
            @click="confirmDeleteCustomer"
          >
            삭제
          </button>
        </div>
      </header>

      <div v-if="store.selectedCustomer" class="grid gap-4">
        <div class="grid grid-cols-2 gap-3 max-[980px]:grid-cols-1">
          <label :class="fieldClass">
            <span :class="fieldLabelClass">고객명</span>
            <input
              :value="store.selectedCustomer.name"
              :class="inputClass"
              type="text"
              @input="updateCustomerField('name', $event.target.value)"
            />
          </label>

          <label :class="fieldClass">
            <span :class="fieldLabelClass">담당자</span>
            <input
              :value="store.findMember(store.selectedCustomer.ownerId)?.name ?? ''"
              :class="inputClass"
              type="text"
              disabled
            />
          </label>
        </div>

        <div class="grid grid-cols-2 gap-3 max-[980px]:grid-cols-1">
          <label :class="fieldClass">
            <span :class="fieldLabelClass">분류</span>
            <input
              :value="store.selectedCustomer.segment"
              :class="inputClass"
              type="text"
              @input="updateCustomerField('segment', $event.target.value)"
            />
          </label>

          <label :class="fieldClass">
            <span :class="fieldLabelClass">이메일</span>
            <input
              :value="store.selectedCustomer.email"
              :class="inputClass"
              type="email"
              @input="updateCustomerField('email', $event.target.value)"
            />
          </label>
        </div>

        <label :class="fieldClass">
          <span :class="fieldLabelClass">톤 가이드</span>
          <input
            :value="store.selectedCustomer.tone"
            :class="inputClass"
            type="text"
            @input="updateCustomerField('tone', $event.target.value)"
          />
        </label>

        <label :class="fieldClass">
          <span :class="fieldLabelClass">특징</span>
          <textarea v-model="customerTraitsText" :class="inputClass" rows="3" />
        </label>

        <label :class="fieldClass">
          <span :class="fieldLabelClass">태그</span>
          <textarea v-model="customerTagsText" :class="inputClass" rows="2" />
        </label>

        <label :class="fieldClass">
          <span :class="fieldLabelClass">운영 메모</span>
          <textarea
            :value="store.selectedCustomer.memo"
            :class="inputClass"
            rows="6"
            @input="updateCustomerField('memo', $event.target.value)"
          />
        </label>
      </div>

      <div v-else :class="emptyCardClass">
        좌측 목록에서 고객을 선택하면 이 영역에서 바로 편집할 수 있습니다.
      </div>
    </article>

    <aside class="grid gap-4 max-[1380px]:grid-cols-2 max-[980px]:grid-cols-1">
      <article
        :class="panelClass"
        :style="store.focusTarget === 'customers-suggestions' ? focusPanelStyle : null"
      >
        <header :class="headerClass">
          <h3 class="text-base font-semibold text-[color:var(--text-primary)]">
            AI 제안 {{ selectedCustomerSuggestionCount }}건
          </h3>
        </header>

        <div class="grid gap-3">
          <article
            v-for="suggestion in store.customerSuggestionsForSelected"
            :key="suggestion.id"
            class="grid gap-3 rounded-[20px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_74%,white)] px-4 py-4"
          >
            <div :class="headerClass">
              <div class="grid gap-1">
                <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ suggestion.title }}</strong>
                <p class="text-sm text-[color:var(--muted-text)]">{{ suggestion.source }}</p>
              </div>
              <span
                :class="badgeBaseClass"
                :style="
                  badgeStyle(
                    suggestion.status === 'approved'
                      ? 'healthy'
                      : suggestion.status === 'rejected'
                        ? 'neutral'
                        : 'watch',
                  )
                "
              >
                {{ suggestionStateLabel[suggestion.status] }}
              </span>
            </div>

            <p class="text-sm text-[color:var(--muted-text)]">{{ suggestion.summary }}</p>

            <div :class="mutedCardClass">
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">제안 메모</strong>
              <p class="text-sm text-[color:var(--muted-text)]">{{ suggestion.suggestedMemo }}</p>
            </div>

            <div class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start">
              <span class="text-sm text-[color:var(--muted-text)]">{{ suggestion.suggestedTags.join(', ') }}</span>
              <small class="text-xs text-[color:var(--muted-text)]">{{ suggestion.createdAt }}</small>
            </div>

            <div
              v-if="store.activeRole === 'admin' && suggestion.status === 'pending'"
              class="flex items-center justify-end gap-3 max-[980px]:flex-col max-[980px]:items-stretch"
            >
              <button :class="ghostButtonClass" type="button" @click="store.rejectSuggestion(suggestion.id)">
                보류
              </button>
              <button :class="primaryButtonClass" type="button" @click="store.approveSuggestion(suggestion.id)">
                반영
              </button>
            </div>
          </article>

          <div v-if="!store.customerSuggestionsForSelected.length" :class="emptyCardClass">
            선택한 고객에 대한 AI 제안이 없습니다.
          </div>
        </div>
      </article>

      <article :class="panelClass">
        <header :class="headerClass">
          <h3 class="text-base font-semibold text-[color:var(--text-primary)]">변경 이력</h3>
        </header>

        <div class="grid gap-3">
          <div
            v-for="history in store.selectedCustomerHistory"
            :key="history.id"
            class="grid grid-cols-[auto_minmax(0,1fr)] gap-3 rounded-[20px] border border-[color:var(--border-color)] bg-[color:color-mix(in_srgb,var(--panel-muted)_74%,white)] px-4 py-4"
          >
            <span
              :class="timelineDotClass"
              style="background: var(--accent-color); box-shadow: 0 0 0 6px color-mix(in srgb, var(--accent-color) 14%, transparent);"
            />
            <div class="grid gap-1">
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{ history.title }}</strong>
              <p class="text-sm text-[color:var(--muted-text)]">{{ history.detail }}</p>
              <small class="text-xs text-[color:var(--muted-text)]">{{ history.createdAt }}</small>
            </div>
          </div>

          <div v-if="!store.selectedCustomerHistory.length" :class="emptyCardClass">
            표시할 변경 이력이 없습니다.
          </div>
        </div>
      </article>
    </aside>
  </section>
</template>
