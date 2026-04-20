<script setup>
import { computed } from 'vue'
import { usePlannerStore } from '@/stores/planner'
import { differenceInDays } from '@/utils/calendar'

const props = defineProps({
  task: {
    type: Object,
    required: true,
  },
  member: {
    type: Object,
    default: null,
  },
  mode: {
    type: String,
    default: 'compact',
  },
  draggable: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'drag-task'])

const store = usePlannerStore()

const statusLabel = computed(() => store.statusLabels[props.task.status] ?? props.task.status)
const priorityLabel = computed(
  () => store.priorityLabels[props.task.priority] ?? props.task.priority,
)
const durationLabel = computed(
  () => `${differenceInDays(props.task.dueDate, props.task.startDate) + 1}일`,
)

function normalizeHex(hex) {
  const value = String(hex ?? '').replace('#', '').trim()

  if (value.length === 3) {
    return value
      .split('')
      .map((char) => `${char}${char}`)
      .join('')
  }

  if (value.length === 6) {
    return value
  }

  return 'ffffff'
}

function hexToRgb(hex) {
  const normalized = normalizeHex(hex)
  return [
    Number.parseInt(normalized.slice(0, 2), 16),
    Number.parseInt(normalized.slice(2, 4), 16),
    Number.parseInt(normalized.slice(4, 6), 16),
  ]
}

function blendHex(fromHex, toHex, ratio) {
  const from = hexToRgb(fromHex)
  const to = hexToRgb(toHex)
  const weight = Math.min(Math.max(ratio, 0), 1)

  const blended = from.map((channel, index) =>
    Math.round(channel * (1 - weight) + to[index] * weight),
  )

  return `#${blended.map((channel) => channel.toString(16).padStart(2, '0')).join('')}`
}

function hexToRgba(hex, alpha) {
  const [red, green, blue] = hexToRgb(hex)
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

const rootClass = computed(() => {
  const base =
    'task-card group relative w-full cursor-pointer select-none text-left transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white/70'

  if (props.mode === 'bar') {
    return `${base} border-0 bg-transparent p-0 shadow-none`
  }

  if (props.mode === 'calendar') {
    return `${base} isolate flex min-h-[36px] items-center justify-between gap-2 overflow-hidden rounded-[16px] border border-[color:var(--task-border)] bg-[linear-gradient(135deg,var(--task-bg),var(--task-bg-2))] px-[0.72rem] py-[0.48rem] text-[color:var(--task-text)] shadow-[0_12px_24px_var(--task-shadow)] ring-1 ring-white/40 backdrop-blur-[2px] hover:-translate-y-[1px] hover:brightness-[1.02] before:pointer-events-none before:absolute before:-right-5 before:-top-5 before:h-16 before:w-16 before:rounded-full before:bg-white/22 before:blur-3xl before:content-[''] after:pointer-events-none after:absolute after:-left-6 after:-bottom-7 after:h-20 after:w-20 after:rounded-full after:bg-white/14 after:blur-3xl after:content-['']`
  }

  return `${base} isolate flex min-h-[118px] flex-col justify-between overflow-hidden rounded-[24px] border border-[color:var(--task-border)] bg-[linear-gradient(135deg,var(--task-bg),var(--task-bg-2))] px-[1rem] py-[0.95rem] text-[color:var(--task-text)] shadow-[0_18px_36px_var(--task-shadow)] ring-1 ring-white/45 backdrop-blur-[2px] hover:-translate-y-[1px] hover:brightness-[1.02] before:pointer-events-none before:absolute before:-right-6 before:-top-6 before:h-24 before:w-24 before:rounded-full before:bg-white/22 before:blur-3xl before:content-[''] after:pointer-events-none after:absolute after:-left-10 after:-bottom-10 after:h-28 after:w-28 after:rounded-full after:bg-white/12 after:blur-3xl after:content-['']`
})

const cardStyle = computed(() => {
  const surface = props.task.palette?.surface ?? '#eaf3ff'
  const accent = props.task.palette?.accent ?? '#5b8def'
  const text = props.task.palette?.text ?? '#1d4f99'
  const fillRatio = props.mode === 'calendar' ? 0.32 : 0.28
  const cardFill = blendHex('#ffffff', accent, fillRatio)
  const cardFillSoft = blendHex(cardFill, surface, props.mode === 'calendar' ? 0.08 : 0.14)
  const borderColor = hexToRgba(accent, props.mode === 'calendar' ? 0.3 : 0.34)
  const shadow = hexToRgba(accent, props.mode === 'calendar' ? 0.18 : 0.16)

  if (props.mode === 'bar') {
    return {
      '--task-surface': surface,
      '--task-accent': accent,
      '--task-text': text,
    }
  }

  return {
    '--task-surface': surface,
    '--task-accent': accent,
    '--task-text': text,
    '--task-bg': cardFill,
    '--task-bg-2': cardFillSoft,
    '--task-border': borderColor,
    '--task-shadow': shadow,
    backgroundColor: cardFill,
    color: text,
  }
})

function handleClick() {
  emit('select', props.task.id)
}

function handleDragStart(event) {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', props.task.id)
  emit('drag-task', props.task.id)
}
</script>

<template>
  <button
    :class="rootClass"
    :style="cardStyle"
    type="button"
    :draggable="props.draggable"
    @click="handleClick"
    @dragstart="handleDragStart"
  >
    <template v-if="props.mode === 'compact'">
      <div class="relative z-10 flex flex-wrap items-center gap-1.5">
        <span class="inline-flex items-center rounded-full border border-white/55 bg-white/72 px-2.5 py-0.5 text-[0.65rem] font-semibold tracking-[0.04em] text-slate-700 shadow-sm backdrop-blur-sm">
          {{ statusLabel }}
        </span>
        <span class="inline-flex items-center rounded-full border border-white/55 bg-white/72 px-2.5 py-0.5 text-[0.65rem] font-semibold tracking-[0.04em] text-slate-700 shadow-sm backdrop-blur-sm">
          {{ priorityLabel }}
        </span>
      </div>

      <strong class="relative z-10 line-clamp-2 block w-full text-left text-[1rem] font-extrabold leading-[1.22] tracking-[-0.03em] drop-shadow-[0_1px_0_rgba(255,255,255,0.35)]">
        {{ props.task.title }}
      </strong>

      <div class="relative z-10 flex items-center gap-2 rounded-2xl bg-white/35 px-2.5 py-1.5 ring-1 ring-white/45 backdrop-blur-sm">
        <span
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[0.72rem] font-extrabold text-white shadow-[0_8px_18px_rgba(15,23,42,0.18)] ring-2 ring-white/75"
          :style="{ backgroundColor: props.member?.accent ?? '#94a3b8' }"
        >
          {{ props.member?.initials ?? 'NA' }}
        </span>
        <div class="min-w-0 grid">
          <strong class="truncate text-[0.86rem] font-semibold leading-[1.1] text-slate-900/90">
            {{ props.member?.name ?? '미지정' }}
          </strong>
        </div>
      </div>
    </template>

    <template v-else-if="props.mode === 'bar'">
      <div
        class="flex h-full min-h-[38px] items-center justify-between gap-[0.75rem] rounded-[14px] px-[0.8rem] py-[0.55rem] text-white shadow-[0_12px_24px_var(--task-shadow)] ring-1 ring-white/20"
        :style="{
          backgroundColor: blendHex(props.task.palette?.accent ?? '#5b8def', '#ffffff', 0.18),
          '--task-shadow': hexToRgba(props.task.palette?.accent ?? '#5b8def', 0.22),
        }"
      >
        <div class="min-w-0">
          <strong class="block overflow-hidden text-ellipsis whitespace-nowrap text-[0.8rem] font-semibold">
            {{ props.task.title }}
          </strong>
          <small class="block overflow-hidden text-ellipsis whitespace-nowrap text-[0.68rem] font-medium opacity-90">
            {{ statusLabel }} · {{ durationLabel }}
          </small>
        </div>

        <span class="min-w-[1.6rem] rounded-full bg-white/18 px-2 py-0.5 text-right text-[0.7rem] font-bold opacity-95 ring-1 ring-white/20">
          {{ props.member?.initials ?? 'NA' }}
        </span>
      </div>
    </template>

    <template v-else>
      <div class="relative z-10 flex min-w-0 flex-1 items-center gap-[0.42rem]">
        <span class="h-1.5 w-1.5 shrink-0 rounded-full bg-white/95 shadow-[0_0_0_1px_rgba(255,255,255,0.55)]" />
        <strong class="min-w-0 flex-1 overflow-hidden text-ellipsis whitespace-nowrap text-[0.74rem] font-bold leading-[1.1]">
          {{ props.task.title }}
        </strong>
      </div>

      <div class="relative z-10 ml-2 flex h-6 shrink-0 items-center rounded-full bg-white/28 px-2 text-[0.63rem] font-black tracking-[0.12em] text-white shadow-sm ring-1 ring-white/35">
        {{ props.member?.initials ?? 'NA' }}
      </div>
    </template>
  </button>
</template>
