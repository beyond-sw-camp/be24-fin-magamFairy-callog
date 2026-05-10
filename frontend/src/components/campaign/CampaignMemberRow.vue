<script setup>
import { computed } from 'vue'

const props = defineProps({
  member: { type: Object, required: true },
  canManage: { type: Boolean, default: false },
  manageMode: { type: Boolean, default: false },
  canExpel: { type: Boolean, default: false },
  expelLoading: { type: Boolean, default: false },
})
const emit = defineEmits(['manage', 'expel'])

const roleLabel = computed(() => {
  switch (props.member.campaignRole) {
    case 'GENERAL_MANAGER': return 'GM'
    case 'MANAGER': return 'Manager'
    case 'USER': default: return 'User'
  }
})
const roleTone = computed(() => {
  switch (props.member.campaignRole) {
    case 'GENERAL_MANAGER': return 'primary'
    case 'MANAGER': return 'info'
    default: return 'gray'
  }
})

function onManage(event) {
  emit('manage', { member: props.member, target: event.currentTarget })
}

function onExpel() {
  emit('expel', props.member)
}
</script>

<template>
  <div class="cm-row">
    <div class="cm-row__person">
      <span class="cm-row__avatar">{{ (member.name || '?').slice(0, 1) }}</span>
      <div>
        <strong>{{ member.name }}</strong>
        <small>{{ member.email || member.userId }}</small>
      </div>
    </div>
    <span class="cm-row__cell">{{ member.companyName }} · {{ member.department }}</span>
    <span :class="['cm-row__chip', `cm-row__chip--${roleTone}`]">{{ roleLabel }}</span>
    <span class="cm-row__cell cm-row__cell--muted">{{ member.joinedAt?.slice(0, 10) }}</span>
    <template v-if="manageMode">
      <button
        v-if="canExpel"
        type="button"
        class="cm-row__expel"
        :disabled="expelLoading"
        @click="onExpel"
      >
        {{ expelLoading ? '처리 중' : '추방' }}
      </button>
      <span v-else />
    </template>
    <template v-else>
      <button v-if="canManage" type="button" class="cm-row__manage" @click="onManage">관리</button>
      <span v-else />
    </template>
  </div>
</template>

<style scoped>
.cm-row {
  display: grid;
  grid-template-columns: 2fr 1.5fr 80px 100px 60px;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
}
.cm-row:last-child { border-bottom: none; }

.cm-row__person {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.cm-row__avatar {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  font-weight: 800;
  flex-shrink: 0;
}
.cm-row__person strong { display: block; color: var(--text-primary); }
.cm-row__person small { display: block; color: var(--muted-text); font-size: 11px; }

.cm-row__cell { color: var(--text-primary); }
.cm-row__cell--muted { color: var(--muted-text); font-size: 12px; }

.cm-row__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}
.cm-row__chip--primary { background: var(--color-primary-100); color: var(--color-primary-700); }
.cm-row__chip--info { background: var(--color-info-light); color: var(--color-info-dark); }
.cm-row__chip--gray { background: var(--panel-muted); color: var(--text-secondary, var(--text-primary)); }

.cm-row__manage {
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-color);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.cm-row__manage:hover { background: var(--panel-muted); border-color: var(--border-strong, var(--color-primary-500)); }
.cm-row__expel {
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-sm);
  background: var(--color-danger);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.cm-row__expel:hover:not(:disabled) { filter: brightness(0.92); }
.cm-row__expel:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
