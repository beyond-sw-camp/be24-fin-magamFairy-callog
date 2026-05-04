<script setup>
defineProps({
  position: { type: Object, required: true }, // { top, left }
  actions: { type: Array, required: true }, // ['promote'|'demote'|'expel']
})
const emit = defineEmits(['action', 'close'])

function handle(action) {
  emit('action', action)
}
</script>

<template>
  <Teleport to="body">
    <div
      class="member-popover"
      :style="{ top: `${position.top}px`, right: `${position.right}px` }"
      role="menu"
      @mouseleave="emit('close')"
    >
      <button v-if="actions.includes('promote')" type="button" class="member-popover__btn member-popover__btn--primary" @click="handle('promote')">
        승격
      </button>
      <button v-if="actions.includes('demote')" type="button" class="member-popover__btn member-popover__btn--warning" @click="handle('demote')">
        강등
      </button>
      <button v-if="actions.includes('expel')" type="button" class="member-popover__btn member-popover__btn--danger" @click="handle('expel')">
        추방
      </button>
    </div>
  </Teleport>
</template>

<style scoped>
.member-popover {
  position: fixed;
  z-index: 70;
  display: flex;
  gap: 4px;
  padding: 6px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-elevated, 0 12px 30px rgba(15, 23, 42, 0.18));
}
.member-popover__btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}
.member-popover__btn--primary:hover {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  border-color: var(--color-primary-500);
}
.member-popover__btn--warning:hover {
  background: var(--color-warning-light);
  color: var(--color-warning-dark);
  border-color: var(--color-warning);
}
.member-popover__btn--danger:hover {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
  border-color: var(--color-danger);
}
</style>
