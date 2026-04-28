<script setup>
import { ref, computed } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore() // 변수명 store로 통일

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

// 1. 상태 관리
const currentDate = ref(new Date()); 
const todayDate = new Date();

// 요일 배열
const daysOfWeek = ['일', '월', '화', '수', '목', '금', '토'];

const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})

// events를 computed로 관리하거나 props 변화를 감지하도록 설정 (기존 ref 대입 방식 유지)
// const events = ref(props.eventsData);
const events = computed(() => props.eventsData);

// 4. 날짜 헬퍼 함수
const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonth = computed(() => currentDate.value.getMonth() + 1);

const prevMonth = () => { currentDate.value = new Date(currentYear.value, currentMonth.value - 2, 1); };
const nextMonth = () => { currentDate.value = new Date(currentYear.value, currentMonth.value, 1); };
const today = () => { currentDate.value = new Date(); };

const isToday = (dateString) => {
  const d = new Date(dateString);
  return d.getDate() === todayDate.getDate() && 
         d.getMonth() === todayDate.getMonth() && 
         d.getFullYear() === todayDate.getFullYear();
};

const formatDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

// 5. 캘린더 로직 (기존 로직 유지)
const calendarWeeks = computed(() => {
  const year = currentYear.value;
  const month = currentMonth.value - 1;
  const firstDayOfMonth = new Date(year, month, 1);
  const lastDayOfMonth = new Date(year, month + 1, 0);

  const startDate = new Date(firstDayOfMonth);
  startDate.setDate(startDate.getDate() - startDate.getDay());

  const weeks = [];
  let current = new Date(startDate);

  while (current <= lastDayOfMonth || current.getDay() !== 0) {
    const week = { days: [], events: [] };
    for (let i = 0; i < 7; i++) {
      const dateStr = formatDate(current);
      week.days.push({
        date: dateStr,
        dayOfMonth: current.getDate(),
        dayOfWeek: current.getDay(),
        isCurrentMonth: current.getMonth() === month
      });
      current.setDate(current.getDate() + 1);
    }
    
    const weekStart = new Date(week.days[0].date);
    const weekEnd = new Date(week.days[6].date);
    const slots = []; 

    events.value.forEach(evt => {
      const evtStart = new Date(evt.start);
      const evtEnd = new Date(evt.end);
      
      if (evtStart <= weekEnd && evtEnd >= weekStart) {
        const startOffset = Math.max(0, week.days.findIndex(d => d.date === evt.start));
        const endOffset = Math.min(6, week.days.findIndex(d => d.date === evt.end) !== -1 ? week.days.findIndex(d => d.date === evt.end) : 6);
        const span = endOffset - startOffset + 1;

        let slotIndex = 0;
        while (slots[slotIndex] && slots[slotIndex].some(s => !(startOffset >= s.end || startOffset + span - 1 < s.start))) {
          slotIndex++;
        }

        if (!slots[slotIndex]) slots[slotIndex] = [];
        slots[slotIndex].push({ start: startOffset, end: startOffset + span - 1 });

        week.events[slotIndex] = week.events[slotIndex] || [];
        week.events[slotIndex].push({
          ...evt,
          isVisible: true,
          startOffset,
          span
        });
      }
    });

    const flattenedEvents = [];
    for (let i = 0; i < slots.length; i++) {
      if (week.events[i]) {
        week.events[i].forEach(e => flattenedEvents.push(e));
      } else {
        flattenedEvents.push({ id: `empty-${i}`, isVisible: false });
      }
    }
    week.events = flattenedEvents;
    weeks.push(week);
  }
  return weeks;
});
</script>

<template>
  <div class="w-full h-screen flex flex-col transition-colors duration-300"
       :class="isDark ? 'bg-[#1e1e2d] text-slate-200' : 'bg-white text-gray-800'">
    
    <header class="flex items-center justify-between px-6 py-4 border-b transition-colors"
            :class="isDark ? 'border-[#2d2d3f] bg-[#252537]' : 'border-gray-200 bg-white'">
      <h1 class="text-2xl font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">
        {{ currentYear }}년 {{ currentMonth }}월
      </h1>
      <div class="flex space-x-2">
        <button @click="prevMonth" 
                class="px-3 py-1 text-sm rounded transition"
                :class="isDark ? 'hover:bg-[#3f3f56] bg-[#2d2d3f]' : 'hover:bg-gray-100 bg-white border border-gray-200'">지난 달</button>
        <button @click="today" class="px-3 py-1 text-sm rounded bg-[#8B5CF6] text-white hover:bg-violet-600 transition shadow-sm">이번 달</button>
        <button @click="nextMonth" 
                class="px-3 py-1 text-sm rounded transition"
                :class="isDark ? 'hover:bg-[#3f3f56] bg-[#2d2d3f]' : 'hover:bg-gray-100 bg-white border border-gray-200'">다음 달</button>
      </div>
    </header>

    <div class="grid grid-cols-7 border-b transition-colors"
         :class="isDark ? 'border-[#2d2d3f] bg-[#252537]/50' : 'border-gray-200 bg-gray-50'">
      <div v-for="day in daysOfWeek" :key="day" 
           class="py-2 text-center text-sm font-semibold border-r last:border-r-0"
           :class="isDark ? 'text-slate-400 border-[#2d2d3f]' : 'text-gray-600 border-gray-200'">
        {{ day }}
      </div>
    </div>

    <div class="flex-1 flex flex-col overflow-hidden">
      <div v-for="(week, weekIndex) in calendarWeeks" :key="weekIndex" 
           class="flex-1 grid grid-cols-7 border-b relative"
           :class="isDark ? 'border-[#2d2d3f]' : 'border-gray-200'">
        
        <div v-for="day in week.days" :key="day.date" 
             class="p-1 border-r last:border-r-0 transition-colors"
             :class="[
                isDark ? 'border-[#2d2d3f]' : 'border-gray-100',
                !day.isCurrentMonth ? (isDark ? 'bg-[#1a1a26]' : 'bg-gray-50') : ''
             ]">
          <span class="text-sm p-1 inline-block" 
                :class="{ 
                  'text-gray-500 opacity-50': !day.isCurrentMonth,
                  'text-red-500': day.dayOfWeek === 0 && day.isCurrentMonth,
                  'text-blue-500': day.dayOfWeek === 6 && day.isCurrentMonth,
                  'bg-[#8B5CF6] !text-white rounded-full px-2': isToday(day.date)
                }">
            {{ day.dayOfMonth }}
          </span>
        </div>

        <div class="absolute top-8 left-0 right-0 bottom-0 pointer-events-none flex flex-col gap-1 px-1 overflow-hidden">
          <template v-for="event in week.events" :key="event.id">
            <div v-if="event.isVisible" 
                  class="h-6 rounded-sm px-2 text-xs font-medium flex items-center truncate shadow-sm pointer-events-auto cursor-pointer"
                  :class="[event.colorClass || (isDark ? 'bg-violet-900/40 text-violet-200 border border-violet-700/50' : 'bg-violet-100 text-violet-700')]"
                  :style="{ 
                    marginLeft: `${(event.startOffset / 7) * 100}%`, 
                    width: `calc(${(event.span / 7) * 100}% - 4px)` 
                  }">
              {{ event.title }}
            </div>
            <div v-else class="h-6"></div>
          </template>
        </div>
        
      </div>
    </div>
  </div>
</template>

<style scoped>
.transition-colors {
  transition-property: background-color, border-color, color;
  transition-duration: 300ms;
}

/* 스크롤바 커스텀 (다크모드 대응) */
::-webkit-scrollbar {
  width: 8px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #4a4a6a;
  border-radius: 10px;
}
</style>