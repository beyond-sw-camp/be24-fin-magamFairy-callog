<script setup>
const props = defineProps({
  active: {
    type: String,
    required: true,
  },
  tabs: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['select'])

function handleSelect(value) {
  emit('select', value)
}
</script>

<template>
  <div
    class="flex items-center gap-2 overflow-x-auto overflow-y-hidden border-b border-[color:var(--border-color)] px-1 pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
  >
    <button
      v-for="tab in props.tabs"
      :key="tab.value"
      class="relative inline-flex min-h-[2.9rem] shrink-0 items-center gap-2 rounded-[14px] border border-transparent px-4 py-2.5 text-[color:var(--muted-text)] transition duration-200 hover:border-[color:color-mix(in_srgb,var(--accent-color)_18%,var(--border-color))] hover:bg-[var(--panel-muted)] hover:text-[color:var(--text-primary)]"
      :class="
        tab.value === props.active
          ? 'border-[color:color-mix(in_srgb,var(--accent-color)_24%,var(--border-color))] bg-[var(--panel-muted)] text-[color:var(--text-primary)] shadow-[0_8px_20px_rgba(19,35,68,0.06)]'
          : ''
      "
      type="button"
      @click="handleSelect(tab.value)"
    >
      <svg
        v-if="tab.icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
        class="h-4 w-4 fill-none stroke-current stroke-[1.8] [stroke-linecap:round] [stroke-linejoin:round]"
      >
        <path :d="tab.icon" />
      </svg>

      <span class="font-semibold">{{ tab.label }}</span>
      <small v-if="tab.caption" class="text-[0.74rem] opacity-75">{{ tab.caption }}</small>
    </button>
  </div>
</template>
