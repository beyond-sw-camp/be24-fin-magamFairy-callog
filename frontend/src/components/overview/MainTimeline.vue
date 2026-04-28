<script setup>
import { computed, ref } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

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
      
      <div class="sticky left-0 z-30 flex-shrink-0 flex flex-col w-[500px] border-r transition-colors duration-300"
           :class="isDark ? 'bg-[#252537] border-[#2d2d3f] shadow-[3px_0_10px_rgba(0,0,0,0.2)]' : 'bg-white border-gray-200 shadow-[3px_0_10px_rgba(0,0,0,0.03)]'">
        
        <div class="sticky top-0 z-50 h-10 border-b flex flex-col box-border transition-colors duration-300"
             :class="isDark ? 'bg-[#2d2d3f] border-[#3f3f56]' : 'bg-gray-50 border-gray-200'">
          <div class="h-full flex text-xs font-bold text-center"
               :class="isDark ? 'text-slate-400' : 'text-gray-500'">
            <div class="w-40 border-r flex items-center justify-center" :class="isDark ? 'border-[#3f3f56]' : 'border-gray-200'">담당사명</div>
            <div class="flex-1 flex items-center justify-center">캠페인명</div>
          </div>
        </div>

        <div class="flex-1" :class="isDark ? 'bg-[#252537]' : 'bg-white'">
          <div 
            v-for="event in events" 
            :key="`sidebar-${event.id}`"
            class="h-14 border-b flex items-center text-center transition-colors duration-200"
            :class="isDark ? 'border-[#2d2d3f] hover:bg-[#2d2d3f]/50' : 'border-gray-100 hover:bg-gray-50'"
          >
            <div class="w-40 border-r font-bold truncate px-2 text-[#8B5CF6]" 
                 :class="isDark ? 'border-[#2d2d3f]' : 'border-gray-100'">
                 {{ event.projectManager }}
            </div>
            <div class="flex-1 px-4 text-left font-bold truncate transition-colors"
                 :class="isDark ? 'text-slate-200' : 'text-gray-800'">
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
</style>