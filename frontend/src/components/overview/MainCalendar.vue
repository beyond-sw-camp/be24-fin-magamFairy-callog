<script setup>
import { ref, computed, onMounted } from 'vue';

// 1. 상태 관리
const currentDate = ref(new Date()); // 현재 표시 기준일
const todayDate = new Date();

// 요일 배열
const daysOfWeek = ['일', '월', '화', '수', '목', '금', '토'];

const events = ref([])

const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})

events.value = props.eventsData;

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

// 5. 캘린더 로직 생성
const calendarWeeks = computed(() => {
const year = currentYear.value;
const month = currentMonth.value - 1;
const firstDayOfMonth = new Date(year, month, 1);
const lastDayOfMonth = new Date(year, month + 1, 0);

const startDate = new Date(firstDayOfMonth);
startDate.setDate(startDate.getDate() - startDate.getDay()); // 첫 주의 일요일로 이동

const weeks = [];
let current = new Date(startDate);

// 달력 격자 생성 (주별로 분리)
while (current <= lastDayOfMonth || current.getDay() !== 0) {
  const week = { days: [], events: [] };
  
  // 7일(한 주) 채우기
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
  
  // 이 주(Week)에 해당하는 일정 필터링 및 계산
  const weekStart = new Date(week.days[0].date);
  const weekEnd = new Date(week.days[6].date);
  
  // 겹침(Overlapping) 방지를 위한 간단한 슬롯 로직
  const slots = []; 

  events.value.forEach(evt => {
    const evtStart = new Date(evt.start);
    const evtEnd = new Date(evt.end);
    
    // 일정이 현재 주간에 걸쳐있는지 확인
    if (evtStart <= weekEnd && evtEnd >= weekStart) {
      // 주간 내 시작 요일 인덱스 (0~6)
      const startOffset = Math.max(0, week.days.findIndex(d => d.date === evt.start));
      // 주간 내 끝 요일 인덱스 (0~6)
      const endOffset = Math.min(6, week.days.findIndex(d => d.date === evt.end) !== -1 ? week.days.findIndex(d => d.date === evt.end) : 6);
      
      const span = endOffset - startOffset + 1;

      // 비어있는 슬롯 찾기
      let slotIndex = 0;
      while (slots[slotIndex] && slots[slotIndex].some(s => !(startOffset >= s.end || startOffset + span - 1 < s.start))) {
        slotIndex++;
      }

      if (!slots[slotIndex]) slots[slotIndex] = [];
      slots[slotIndex].push({ start: startOffset, end: startOffset + span - 1 });

      // 렌더링용 데이터
      week.events[slotIndex] = week.events[slotIndex] || [];
      week.events[slotIndex].push({
        ...evt,
        isVisible: true,
        startOffset,
        span
      });
    }
  });

  // Vue 템플릿에서 순회를 쉽게 하기 위해 슬롯 배열 평탄화
  const flattenedEvents = [];
  for (let i = 0; i < slots.length; i++) {
    if (week.events[i]) {
      // 해당 줄에 렌더링될 일정들 (margin-left를 사용하므로 순서는 상관없음)
      week.events[i].forEach(e => flattenedEvents.push(e));
    } else {
      // 일정이 없는 줄은 빈 공간 유지
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
<div class="w-full h-screen flex flex-col bg-white text-gray-800 font-sans">
  
  <header class="flex items-center justify-between px-6 py-4 border-b border-gray-200">
    <h1 class="text-2xl font-bold text-gray-900">
      {{ currentYear }}년 {{ currentMonth }}월
    </h1>
    <div class="flex space-x-2">
      <button @click="prevMonth" class="px-3 py-1 text-sm rounded hover:bg-gray-100 transition">지난 달</button>
      <button @click="today" class="px-3 py-1 text-sm rounded bg-[#8B5CF6] text-white hover:bg-violet-600 transition">이번 달</button>
      <button @click="nextMonth" class="px-3 py-1 text-sm rounded hover:bg-gray-100 transition">다음 달</button>
    </div>
  </header>

  <div class="grid grid-cols-7 border-b border-gray-200 bg-gray-50">
    <div v-for="day in daysOfWeek" :key="day" class="py-2 text-center text-sm font-semibold text-gray-600 border-r last:border-r-0">
      {{ day }}
    </div>
  </div>

  <div class="flex-1 flex flex-col">
    <div v-for="(week, weekIndex) in calendarWeeks" :key="weekIndex" class="flex-1 grid grid-cols-7 border-b border-gray-200 relative">
      
      <div v-for="day in week.days" :key="day.date" 
            class="p-1 border-r last:border-r-0 border-gray-100"
            :class="{ 'bg-gray-50': !day.isCurrentMonth }">
        <span class="text-sm p-1" 
              :class="{ 
                'text-gray-400': !day.isCurrentMonth,
                'text-red-500': day.dayOfWeek === 0 && day.isCurrentMonth,
                'text-blue-500': day.dayOfWeek === 6 && day.isCurrentMonth,
                'bg-[#8B5CF6] text-white rounded-full px-2': isToday(day.date)
              }">
          {{ day.dayOfMonth }}
        </span>
      </div>

      <div class="absolute top-8 left-0 right-0 bottom-0 pointer-events-none flex flex-col gap-1 px-1 overflow-hidden">
        <template v-for="event in week.events" :key="event.id">
          <div v-if="event.isVisible" 
                class="h-6 rounded-sm px-2 text-xs font-medium flex items-center truncate shadow-sm pointer-events-auto cursor-default"
                :class="[event.colorClass]"
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

