<script setup>
import { computed, ref, onUnmounted } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

/* ─── 컬럼 리사이즈 (담당사명 / 캠페인명 각각) ─── */
const LEFT_COL_KEY = 'overviewTimelineLeftColWidth'
const NAME_COL_KEY = 'overviewTimelineNameColWidth'
const LEFT_MIN = 80
const LEFT_MAX = 320
const NAME_MIN = 120
const NAME_MAX = 600

const leftColWidth = ref(
  Number(typeof window !== 'undefined' ? window.localStorage.getItem(LEFT_COL_KEY) : null) || 160
)
const nameColWidth = ref(
  Number(typeof window !== 'undefined' ? window.localStorage.getItem(NAME_COL_KEY) : null) || 340
)
const activeResize = ref(null)  // 'left' | 'name' | null

function clamp(v, min, max) { return Math.min(max, Math.max(min, v)) }

const panelWidth = computed(() => leftColWidth.value + nameColWidth.value)

function startResize(target, e) {
  e.preventDefault()
  activeResize.value = target
  const startX = e.clientX
  const startLeft = leftColWidth.value
  const startName = nameColWidth.value
  const onMove = (ev) => {
    const dx = ev.clientX - startX
    if (target === 'left') {
      leftColWidth.value = clamp(startLeft + dx, LEFT_MIN, LEFT_MAX)
    } else if (target === 'name') {
      nameColWidth.value = clamp(startName + dx, NAME_MIN, NAME_MAX)
    }
  }
  const onUp = () => {
    activeResize.value = null
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', onUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    try {
      window.localStorage.setItem(LEFT_COL_KEY, String(leftColWidth.value))
      window.localStorage.setItem(NAME_COL_KEY, String(nameColWidth.value))
    } catch { /* ignore */ }
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}
const startLeftResize = (e) => startResize('left', e)
const startNameResize = (e) => startResize('name', e)

onUnmounted(() => {
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
})

// const events = ref([])
const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})

// events.value = props.eventsData;
const events = computed(() => props.eventsData);

// 2. 환경 설정
const dayWidth = 18; 
const rowHeight = 56; 

// 3. 헬퍼 함수
const parseDate = (dateStr) => {
  const [y, m, d] = dateStr.split('-');
  return new Date(y, m - 1, d);
};

// 4. 타임라인 동적 계산 로직 (기존과 동일)
const timelineRange = computed(() => {
  if (events.value.length === 0) return { start: new Date(), end: new Date() };
  
  const startDates = events.value.map(e => parseDate(e.start).getTime());
  const endDates = events.value.map(e => parseDate(e.end).getTime());

  const minDate = new Date(Math.min(...startDates));
  const maxDate = new Date(Math.max(...endDates));

  return {
    start: new Date(minDate.getFullYear(), minDate.getMonth(), 1),
    end: new Date(maxDate.getFullYear(), maxDate.getMonth() + 1, 0)
  };
});

const months = computed(() => {
  const arr = [];
  let current = new Date(timelineRange.value.start);
  const end = timelineRange.value.end;

  while (current <= end) {
    const y = current.getFullYear();
    const m = current.getMonth();
    const daysInMonth = new Date(y, m + 1, 0).getDate();
    
    arr.push({
      year: y,
      month: m + 1,
      days: daysInMonth,
      width: daysInMonth * dayWidth
    });
    current = new Date(y, m + 1, 1);
  }
  return arr;
});

const totalWidth = computed(() => months.value.reduce((acc, curr) => acc + curr.width, 0));

const getEventStyle = (event, index) => {
  const eStart = parseDate(event.start);
  const eEnd = parseDate(event.end);
  const tStart = timelineRange.value.start;

  const startDiff = (eStart.getTime() - tStart.getTime()) / (1000 * 3600 * 24);
  const duration = ((eEnd.getTime() - eStart.getTime()) / (1000 * 3600 * 24)) + 1;

  return {
    top: `${(index * rowHeight) + ((rowHeight - 36) / 2)}px`,
    left: `${startDiff * dayWidth}px`,
    width: `${duration * dayWidth}px`
  };
};

const getTodayLineStyle = () => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const tStart = timelineRange.value.start;
  const tEnd = timelineRange.value.end;

  if (today < tStart || today > tEnd) return { display: 'none' };
  const startDiff = (today.getTime() - tStart.getTime()) / (1000 * 3600 * 24);

  return {
    left: `${startDiff * dayWidth}px`,
    display: 'block'
  };
};
</script>

<template>
  <div class="w-full h-full flex flex-col border transition-colors duration-300 rounded-xl shadow-sm overflow-hidden text-sm font-sans"
       :class="isDark ? 'bg-[#1e1e2d] border-[#2d2d3f]' : 'bg-white border-gray-200'">

    <div class="flex-1 overflow-auto flex relative custom-scrollbar"
         :class="isDark ? 'bg-[#1e1e2d]' : 'bg-white'">
      
      <div class="sticky left-0 z-30 flex-shrink-0 flex flex-col border-r transition-colors duration-300 relative"
           :style="{ width: panelWidth + 'px' }"
           :class="isDark ? 'bg-[#252537] border-[#2d2d3f] shadow-[3px_0_10px_rgba(0,0,0,0.2)]' : 'bg-white border-gray-200 shadow-[3px_0_10px_rgba(0,0,0,0.03)]'">

        <!-- 핸들 1: 담당사명 ↔ 캠페인명 -->
        <div
          class="col-resize-handle"
          :class="{ 'col-resize-handle--active': activeResize === 'left' }"
          :style="{ left: (leftColWidth - 3) + 'px' }"
          @pointerdown="startLeftResize"
          role="separator"
          aria-orientation="vertical"
          aria-label="담당사명 컬럼 너비 조절"
          title="좌우로 드래그해서 담당사명 컬럼 너비를 조절하세요"
        ></div>

        <!-- 핸들 2: 캠페인명 ↔ 타임라인 -->
        <div
          class="col-resize-handle"
          :class="{ 'col-resize-handle--active': activeResize === 'name' }"
          :style="{ left: (panelWidth - 3) + 'px' }"
          @pointerdown="startNameResize"
          role="separator"
          aria-orientation="vertical"
          aria-label="캠페인명 컬럼 너비 조절"
          title="좌우로 드래그해서 캠페인명 컬럼 너비를 조절하세요"
        ></div>

        <div class="sticky top-0 z-50 h-10 border-b flex flex-col box-border transition-colors duration-300"
             :class="isDark ? 'bg-[#2d2d3f] border-[#3f3f56]' : 'bg-gray-50 border-gray-200'">
          <div class="h-full flex text-xs font-bold text-center"
               :class="isDark ? 'text-slate-400' : 'text-gray-500'">
            <div
              class="border-r flex items-center justify-center flex-shrink-0"
              :class="isDark ? 'border-[#3f3f56]' : 'border-gray-200'"
              :style="{ width: leftColWidth + 'px' }"
            >담당사명</div>
            <div
              class="flex items-center justify-center flex-shrink-0"
              :style="{ width: nameColWidth + 'px' }"
            >캠페인명</div>
          </div>
        </div>

        <div class="flex-1" :class="isDark ? 'bg-[#252537]' : 'bg-white'">
          <div 
            v-for="event in events" 
            :key="`sidebar-${event.id}`"
            class="h-14 border-b flex items-center text-center transition-colors duration-200"
            :class="isDark ? 'border-[#2d2d3f] hover:bg-[#2d2d3f]/50' : 'border-gray-100 hover:bg-gray-50'"
          >
            <div
              class="border-r font-bold truncate px-2 text-[#8B5CF6] flex-shrink-0 flex items-center"
              :class="isDark ? 'border-[#2d2d3f]' : 'border-gray-100'"
              :style="{ width: leftColWidth + 'px' }"
            >
              {{ event.projectManager }}
            </div>
            <div
              class="px-4 text-left font-bold truncate transition-colors flex items-center flex-shrink-0"
              :class="isDark ? 'text-slate-200' : 'text-gray-800'"
              :style="{ width: nameColWidth + 'px' }"
            >
              {{ event.title }}
            </div>
          </div>
        </div>
      </div>

      <div class="relative transition-colors duration-300" 
           :class="isDark ? 'bg-[#1e1e2d]' : 'bg-white'"
           :style="{ width: `${totalWidth}px` }">
        
        <div class="sticky top-0 z-40 h-10 border-b flex flex-col box-border transition-colors duration-300"
             :class="isDark ? 'bg-[#2d2d3f] border-[#3f3f56]' : 'bg-white border-gray-200'">
          <div class="h-full flex">
            <div 
              v-for="(month, idx) in months" 
              :key="`month-header-${idx}`"
              class="border-r h-full relative transition-colors"
              :class="isDark ? 'border-[#3f3f56]' : 'border-gray-200'"
              :style="{ width: `${month.width}px` }"
            >
              <span class="sticky left-0 right-0 w-max mx-auto h-full flex items-center font-bold px-4"
                    :class="isDark ? 'text-slate-300' : 'text-gray-600'">
                {{ month.month }}월
              </span>
            </div>
          </div>
        </div>

        <div class="relative" :style="{ height: `${events.length * 56}px` }">
          
          <div class="absolute inset-0 flex pointer-events-none z-0">
            <div 
              v-for="(month, idx) in months" 
              :key="`grid-col-${idx}`" 
              class="h-full border-r transition-colors" 
              :class="isDark ? 'border-[#2d2d3f]' : 'border-gray-100'"
              :style="{ width: `${month.width}px` }"
            ></div>
          </div>

          <div class="absolute inset-0 pointer-events-none z-0 flex flex-col">
            <div v-for="event in events" 
                 :key="`grid-row-${event.id}`" 
                 class="h-14 border-b transition-colors"
                 :class="isDark ? 'border-[#2d2d3f]' : 'border-gray-100'"></div>
          </div>

          <div 
            v-for="(event, index) in events" 
            :key="`bar-${event.id}`"
            class="absolute h-[36px] rounded-md shadow-sm hover:shadow-md transition-shadow cursor-default flex items-center px-3 z-10 overflow-hidden"
            :class="event.colorClass"
            :style="getEventStyle(event, index)"
          >
            <span class="text-xs font-bold truncate">{{ event.title }}</span>
          </div>

          <div 
            class="absolute top-0 bottom-0 w-[2px] z-20 pointer-events-none"
            :class="isDark ? 'bg-red-500' : 'bg-red-400'"
            :style="getTodayLineStyle()"
          ></div>

        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.transition-colors {
  transition-property: background-color, border-color, color;
}

/* 커스텀 스크롤바 (다크모드 반영) */
.custom-scrollbar::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: v-bind('isDark ? "#1e1e2d" : "#f8fafc"');
  border-left: 1px solid v-bind('isDark ? "#2d2d3f" : "#e2e8f0"');
  border-top: 1px solid v-bind('isDark ? "#2d2d3f" : "#e2e8f0"');
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: v-bind('isDark ? "#3f3f56" : "#cbd5e1"');
  border-radius: 6px;
  border: 2px solid v-bind('isDark ? "#1e1e2d" : "#f8fafc"');
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: #8B5CF6;
}

/* 컬럼 너비 리사이즈 핸들 */
.col-resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 60;
  transition: background-color 0.15s;
  touch-action: none;
}
.col-resize-handle::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  margin-left: -0.5px;
  background: transparent;
  transition: background-color 0.15s;
}
.col-resize-handle:hover::after,
.col-resize-handle--active::after {
  background: #8B5CF6;
  width: 2px;
  margin-left: -1px;
}
.col-resize-handle:hover,
.col-resize-handle--active {
  background: rgba(139, 92, 246, 0.08);
}
</style>