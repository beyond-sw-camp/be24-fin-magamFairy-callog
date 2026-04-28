<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  historyData: {
    type: Array,
    required: true,
  },
  initialVersion: {
    type: String,
    default: '',
  },
  startInDiff: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close'])

const selectedHistory = ref(null)
const showDiff = ref(false)

const selectedDiff = computed(() => selectedHistory.value?.diff ?? null)

watch(
  () => [props.historyData, props.initialVersion, props.startInDiff],
  () => {
    if (!props.historyData.length) {
      selectedHistory.value = null
      showDiff.value = false
      return
    }

    const nextSelection =
      props.historyData.find((log) => log.version === props.initialVersion) ?? props.historyData[0]

    selectedHistory.value = nextSelection
    showDiff.value = props.startInDiff || Boolean(props.initialVersion)
  },
  { immediate: true, deep: true },
)

const previousVersionLabel = computed(() => {
  if (!selectedHistory.value) {
    return '초안'
  }

  const currentIndex = props.historyData.findIndex(
    (log) => log.version === selectedHistory.value?.version,
  )
  return props.historyData[currentIndex + 1]?.version ?? '초안'
})

const previousVersionDate = computed(() => {
  if (!selectedHistory.value) {
    return '기록 없음'
  }

  const currentIndex = props.historyData.findIndex(
    (log) => log.version === selectedHistory.value?.version,
  )
  return props.historyData[currentIndex + 1]?.date ?? '기록 없음'
})

const diffDetailRows = computed(() => {
  if (!selectedDiff.value) {
    return []
  }

  return Object.keys(selectedDiff.value.details.current).map((label) => ({
    label,
    previous: selectedDiff.value.details.previous[label] ?? '-',
    current: selectedDiff.value.details.current[label] ?? '-',
    changed:
      (selectedDiff.value.details.previous[label] ?? '-') !==
      (selectedDiff.value.details.current[label] ?? '-'),
  }))
})

const previousContentLines = computed(() =>
  (selectedDiff.value?.content.lines ?? []).flatMap((line) => (line.left ? [line.left] : [])),
)
const currentContentLines = computed(() =>
  (selectedDiff.value?.content.lines ?? []).flatMap((line) => (line.right ? [line.right] : [])),
)

function openDiff(log) {
  selectedHistory.value = log
  showDiff.value = true
}

function diffDetailValueClass(row, side) {
  if (!row.changed) {
    return 'text-slate-600'
  }

  return side === 'previous' ? 'text-red-500' : 'text-blue-600'
}

function diffLineClass(line, side) {
  if (side === 'previous' && (line.kind === 'changed' || line.kind === 'removed')) {
    return 'border-red-100 bg-red-50 text-red-700'
  }

  if (side === 'current' && (line.kind === 'changed' || line.kind === 'added')) {
    return 'border-blue-100 bg-blue-50 text-blue-700'
  }

  return 'border-slate-100 bg-white text-slate-600'
}

function diffLineMark(line, side) {
  if (side === 'previous') {
    return line.kind === 'changed' || line.kind === 'removed' ? '-' : ' '
  }

  return line.kind === 'changed' || line.kind === 'added' ? '+' : ' '
}
</script>

<template>
  <div class="flex h-full flex-col overflow-hidden rounded-[24px] border border-slate-100 bg-white shadow-[0_20px_50px_rgba(15,23,42,0.12)]">
    <header class="flex items-center justify-between border-b border-slate-50 bg-white/50 px-6 py-5 backdrop-blur-md">
      <div class="flex items-center gap-4">
        <div class="flex h-12 w-12 items-center justify-center rounded-[18px] bg-blue-50 text-blue-500">
          <span class="material-symbols-outlined text-2xl">history</span>
        </div>
        <div>
          <h3 class="text-xl font-extrabold tracking-tight text-slate-900">컨텐츠 변경 이력</h3>
          <p class="mt-0.5 text-xs font-bold uppercase tracking-widest text-slate-400">
            Version History & Diff
          </p>
        </div>
      </div>

      <button
        type="button"
        class="flex h-10 w-10 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-900"
        @click="emit('close')"
      >
        <span class="material-symbols-outlined">close</span>
      </button>
    </header>

    <div class="flex min-h-0 flex-1 flex-col overflow-hidden bg-slate-50/30">
      <div v-if="!showDiff" class="flex-1 overflow-y-auto p-6">
        <div class="mx-auto max-w-4xl space-y-3">
          <button
            v-for="log in historyData"
            :key="log.version"
            type="button"
            class="group flex w-full cursor-pointer items-center justify-between rounded-[18px] border border-slate-100 bg-white p-5 text-left transition-all hover:border-blue-200 hover:shadow-md"
            @click="openDiff(log)"
          >
            <div class="flex min-w-0 items-center gap-4">
              <span
                class="rounded-lg bg-slate-100 px-3 py-1 text-xs font-black text-slate-600 transition-colors group-hover:bg-blue-50 group-hover:text-blue-600"
              >
                {{ log.version }}
              </span>

              <div class="flex min-w-0 items-center gap-3">
                <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-100 text-[10px] font-black text-slate-500">
                  {{ log.workerInitials }}
                </div>
                <div class="min-w-0">
                  <span class="block truncate text-sm font-bold text-slate-900">{{ log.worker }}</span>
                  <span class="block truncate text-xs font-medium text-slate-400">{{ log.date }}</span>
                </div>
              </div>

              <p class="ml-2 truncate text-sm font-bold text-slate-600 transition-colors group-hover:text-slate-900">
                {{ log.change }}
              </p>
            </div>

            <span class="material-symbols-outlined text-slate-300 transition-all group-hover:translate-x-1 group-hover:text-blue-500">
              arrow_forward_ios
            </span>
          </button>
        </div>
      </div>

      <div v-else class="flex min-h-0 flex-1 flex-col overflow-hidden">
        <div class="flex items-center justify-between border-b border-slate-100 bg-white px-6 py-4">
          <button
            type="button"
            class="flex items-center gap-2 text-sm font-bold text-slate-500 transition-colors hover:text-slate-900"
            @click="showDiff = false"
          >
            <span class="material-symbols-outlined text-lg">arrow_back</span>
            목록으로 돌아가기
          </button>

          <div class="text-center">
            <span class="rounded-md bg-blue-50 px-2 py-1 text-[10px] font-black uppercase tracking-widest text-blue-500">
              Comparing {{ selectedHistory?.version ?? 'v1.0' }}
            </span>
          </div>

          <div class="w-32"></div>
        </div>

        <div class="flex-1 overflow-y-auto p-6">
          <div class="mx-auto grid max-w-7xl grid-cols-1 gap-8 lg:grid-cols-2">
            <div class="space-y-4">
              <div class="flex items-center justify-between px-1">
                <span class="text-[11px] font-black uppercase tracking-widest text-slate-400">
                  이전 버전: {{ previousVersionLabel }}
                </span>
                <span class="text-[11px] font-medium text-slate-400">{{ previousVersionDate }}</span>
              </div>

              <div class="space-y-6 rounded-[20px] border border-slate-100 bg-white p-6 shadow-sm">
                <div>
                  <p class="mb-3 text-[10px] font-black uppercase tracking-wider text-slate-400">제목 (Title)</p>
                  <h4 class="text-lg font-extrabold text-slate-400 line-through decoration-slate-200">
                    {{ selectedDiff?.title.previous }}
                  </h4>
                </div>

                <div class="space-y-4 border-t border-slate-50 pt-6">
                  <p class="text-[10px] font-black uppercase tracking-wider text-slate-400">속성 상세 (Attributes)</p>
                  <div v-for="row in diffDetailRows" :key="row.label" class="flex items-center justify-between">
                    <span class="text-xs font-bold text-slate-400">{{ row.label }}</span>
                    <span class="text-xs font-bold" :class="diffDetailValueClass(row, 'previous')">
                      {{ row.previous }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-4">
              <div class="flex items-center justify-between px-1">
                <span class="text-[11px] font-black uppercase tracking-widest text-blue-500">
                  선택 버전: {{ selectedHistory?.version ?? 'v1.0' }}
                </span>
                <span class="text-[11px] font-bold text-blue-400">CURRENT VIEW</span>
              </div>

              <div class="space-y-6 rounded-[20px] border border-blue-100 bg-white p-6 shadow-lg shadow-blue-500/5 ring-1 ring-blue-50">
                <div>
                  <p class="mb-3 text-[10px] font-black uppercase tracking-wider text-blue-500">제목 (Title)</p>
                  <h4 class="text-lg font-extrabold text-slate-900">
                    {{ selectedDiff?.title.current }}
                  </h4>
                </div>

                <div class="space-y-4 border-t border-blue-50 pt-6">
                  <p class="text-[10px] font-black uppercase tracking-wider text-blue-500">속성 상세 (Attributes)</p>
                  <div v-for="row in diffDetailRows" :key="row.label" class="flex items-center justify-between">
                    <span class="text-xs font-bold text-slate-400">{{ row.label }}</span>
                    <span class="text-xs font-bold" :class="diffDetailValueClass(row, 'current')">
                      {{ row.current }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="mx-auto mt-8 max-w-7xl space-y-4">
            <p class="px-1 text-[11px] font-black uppercase tracking-widest text-slate-400">
              컨텐츠 변경 항목 (Content Diff)
            </p>

            <div class="grid grid-cols-1 overflow-hidden rounded-[20px] border border-slate-200 bg-slate-200 shadow-sm lg:grid-cols-2">
              <div class="space-y-2 bg-white p-6">
                <div
                  v-for="line in previousContentLines"
                  :key="`prev-${line.number}`"
                  class="flex gap-4 rounded-lg border p-2 font-mono text-[13px] leading-relaxed"
                  :class="diffLineClass(line, 'previous')"
                >
                  <span class="w-6 select-none text-right text-slate-300">{{ line.number }}</span>
                  <span class="w-4 text-center font-bold">{{ diffLineMark(line, 'previous') }}</span>
                  <code class="flex-1 whitespace-pre-wrap">{{ line.text || ' ' }}</code>
                </div>
              </div>

              <div class="space-y-2 bg-white p-6">
                <div
                  v-for="line in currentContentLines"
                  :key="`curr-${line.number}`"
                  class="flex gap-4 rounded-lg border p-2 font-mono text-[13px] leading-relaxed"
                  :class="diffLineClass(line, 'current')"
                >
                  <span class="w-6 select-none text-right text-slate-300">{{ line.number }}</span>
                  <span class="w-4 text-center font-bold">{{ diffLineMark(line, 'current') }}</span>
                  <code class="flex-1 whitespace-pre-wrap">{{ line.text || ' ' }}</code>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <footer class="flex justify-end gap-3 border-t border-slate-50 bg-white px-6 py-5">
      <button
        type="button"
        class="rounded-[18px] bg-slate-900 px-6 py-3 text-sm font-bold text-white transition-all hover:bg-slate-800 active:scale-95"
        @click="emit('close')"
      >
        닫기
      </button>
    </footer>
  </div>
</template>
