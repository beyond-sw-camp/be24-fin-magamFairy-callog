<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  historyData: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['close'])

const selectedHistory = ref(props.historyData[0])
const showDiff = ref(false)

const selectedDiff = computed(() => selectedHistory.value?.diff ?? null)

const previousVersionLabel = computed(() => {
  const currentIndex = props.historyData.findIndex(
    (log) => log.version === selectedHistory.value?.version,
  )
  return props.historyData[currentIndex + 1]?.version ?? '초안'
})

const previousVersionDate = computed(() => {
  const currentIndex = props.historyData.findIndex(
    (log) => log.version === selectedHistory.value?.version,
  )
  return props.historyData[currentIndex + 1]?.date ?? '기록 없음'
})

const diffDetailRows = computed(() => {
  if (!selectedDiff.value) return []

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
  <div class="flex flex-col h-full bg-white rounded-[2rem] overflow-hidden shadow-2xl border border-slate-100">
    <!-- Header -->
    <header class="px-8 py-6 border-b border-slate-50 flex justify-between items-center bg-white/50 backdrop-blur-md">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 rounded-2xl bg-blue-50 flex items-center justify-center text-blue-500">
          <span class="material-symbols-outlined text-2xl">history</span>
        </div>
        <div>
          <h3 class="text-xl font-extrabold text-slate-900 tracking-tight">컨텐츠 변경 이력</h3>
          <p class="text-xs font-bold text-slate-400 uppercase tracking-widest mt-0.5">Version History & Diff</p>
        </div>
      </div>
      <button 
        @click="emit('close')" 
        class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-900 transition-all"
      >
        <span class="material-symbols-outlined">close</span>
      </button>
    </header>

    <!-- Content Area -->
    <div class="flex-1 overflow-hidden flex flex-col min-h-0 bg-slate-50/30">
      <!-- List View -->
      <div v-if="!showDiff" class="flex-1 overflow-y-auto p-8">
        <div class="max-w-4xl mx-auto space-y-3">
          <div 
            v-for="log in historyData" 
            :key="log.version"
            @click="openDiff(log)"
            class="group flex items-center justify-between p-5 bg-white border border-slate-100 rounded-2xl hover:border-blue-200 hover:shadow-md transition-all cursor-pointer"
          >
            <div class="flex items-center gap-6">
              <span class="px-3 py-1 bg-slate-100 group-hover:bg-blue-50 text-slate-600 group-hover:text-blue-600 text-xs font-black rounded-lg transition-colors">
                {{ log.version }}
              </span>
              <div class="flex items-center gap-3">
                <div class="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-[10px] font-black text-slate-500">
                  {{ log.workerInitials }}
                </div>
                <div class="flex flex-col">
                  <span class="text-sm font-bold text-slate-900">{{ log.worker }}</span>
                  <span class="text-xs font-medium text-slate-400">{{ log.date }}</span>
                </div>
              </div>
              <p class="text-sm font-bold text-slate-600 group-hover:text-slate-900 transition-colors ml-4">
                {{ log.change }}
              </p>
            </div>
            <span class="material-symbols-outlined text-slate-300 group-hover:text-blue-500 group-hover:translate-x-1 transition-all">
              arrow_forward_ios
            </span>
          </div>
        </div>
      </div>

      <!-- Diff View -->
      <div v-else class="flex-1 flex flex-col min-h-0 overflow-hidden">
        <div class="px-8 py-4 bg-white border-b border-slate-100 flex items-center justify-between">
          <button 
            @click="showDiff = false" 
            class="flex items-center gap-2 text-sm font-bold text-slate-500 hover:text-slate-900 transition-colors"
          >
            <span class="material-symbols-outlined text-lg">arrow_back</span>
            목록으로 돌아가기
          </button>
          <div class="text-center">
            <span class="text-[10px] font-black text-blue-500 uppercase tracking-widest bg-blue-50 px-2 py-1 rounded-md">
              Comparing {{ selectedHistory.version }}
            </span>
          </div>
          <div class="w-32"></div>
        </div>

        <div class="flex-1 overflow-y-auto p-8">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 max-w-7xl mx-auto">
            <!-- Left Side: Previous -->
            <div class="space-y-4">
              <div class="flex justify-between items-center px-2">
                <span class="text-[11px] font-black text-slate-400 uppercase tracking-widest">이전 버전: {{ previousVersionLabel }}</span>
                <span class="text-[11px] font-medium text-slate-400">{{ previousVersionDate }}</span>
              </div>
              <div class="bg-white border border-slate-100 rounded-3xl p-6 space-y-6 shadow-sm">
                <div>
                  <p class="text-[10px] font-black text-slate-400 uppercase mb-3 tracking-wider">제목 (Title)</p>
                  <h4 class="text-lg font-extrabold text-slate-400 line-through decoration-slate-200">
                    {{ selectedDiff.title.previous }}
                  </h4>
                </div>
                <div class="space-y-4 pt-6 border-t border-slate-50">
                  <p class="text-[10px] font-black text-slate-400 uppercase tracking-wider">속성 상세 (Attributes)</p>
                  <div v-for="row in diffDetailRows" :key="row.label" class="flex justify-between items-center">
                    <span class="text-xs font-bold text-slate-400">{{ row.label }}</span>
                    <span class="text-xs font-bold" :class="diffDetailValueClass(row, 'previous')">
                      {{ row.previous }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Right Side: Selected -->
            <div class="space-y-4">
              <div class="flex justify-between items-center px-2">
                <span class="text-[11px] font-black text-blue-500 uppercase tracking-widest">선택 버전: {{ selectedHistory.version }}</span>
                <span class="text-[11px] font-bold text-blue-400">CURRENT VIEW</span>
              </div>
              <div class="bg-white border border-blue-100 rounded-3xl p-6 space-y-6 shadow-lg shadow-blue-500/5 ring-1 ring-blue-50">
                <div>
                  <p class="text-[10px] font-black text-blue-500 uppercase mb-3 tracking-wider">제목 (Title)</p>
                  <h4 class="text-lg font-extrabold text-slate-900">
                    {{ selectedDiff.title.current }}
                  </h4>
                </div>
                <div class="space-y-4 pt-6 border-t border-blue-50">
                  <p class="text-[10px] font-black text-blue-500 uppercase tracking-wider">속성 상세 (Attributes)</p>
                  <div v-for="row in diffDetailRows" :key="row.label" class="flex justify-between items-center">
                    <span class="text-xs font-bold text-slate-400">{{ row.label }}</span>
                    <span class="text-xs font-bold" :class="diffDetailValueClass(row, 'current')">
                      {{ row.current }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Content Diff (Source Code Style) -->
          <div class="mt-8 max-w-7xl mx-auto space-y-4">
            <p class="text-[11px] font-black text-slate-400 uppercase tracking-widest px-2">컨텐츠 변경 세부 사항 (Content Diff)</p>
            <div class="grid grid-cols-1 lg:grid-cols-2 gap-px bg-slate-200 border border-slate-200 rounded-3xl overflow-hidden shadow-sm">
              <!-- Left Content -->
              <div class="bg-white p-6 space-y-2">
                <div v-for="line in previousContentLines" :key="'prev-' + line.number" 
                     class="flex gap-4 font-mono text-[13px] leading-relaxed p-2 rounded-lg border"
                     :class="diffLineClass(line, 'previous')">
                  <span class="w-6 text-right text-slate-300 select-none">{{ line.number }}</span>
                  <span class="w-4 text-center font-bold">{{ diffLineMark(line, 'previous') }}</span>
                  <code class="flex-1 whitespace-pre-wrap">{{ line.text || ' ' }}</code>
                </div>
              </div>
              <!-- Right Content -->
              <div class="bg-white p-6 space-y-2">
                <div v-for="line in currentContentLines" :key="'curr-' + line.number" 
                     class="flex gap-4 font-mono text-[13px] leading-relaxed p-2 rounded-lg border"
                     :class="diffLineClass(line, 'current')">
                  <span class="w-6 text-right text-slate-300 select-none">{{ line.number }}</span>
                  <span class="w-4 text-center font-bold">{{ diffLineMark(line, 'current') }}</span>
                  <code class="flex-1 whitespace-pre-wrap">{{ line.text || ' ' }}</code>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <footer class="px-8 py-6 border-t border-slate-50 bg-white flex justify-end gap-3">
      <button 
        @click="emit('close')" 
        class="px-8 py-3 bg-slate-900 text-white text-sm font-bold rounded-2xl hover:bg-slate-800 active:scale-95 transition-all"
      >
        닫기
      </button>
    </footer>
  </div>
</template>

<style scoped>
.material-symbols-outlined {
  font-family: 'Material Symbols Outlined';
  font-weight: normal;
  font-style: normal;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  direction: ltr;
  -webkit-font-smoothing: antialiased;
  font-feature-settings: 'liga';
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>
