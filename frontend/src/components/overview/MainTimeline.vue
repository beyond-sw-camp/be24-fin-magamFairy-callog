<script setup>
import { computed, ref } from 'vue';

const events = ref([])
const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})
events.value = props.eventsData;
console.log(events.value)


// 2. 환경 설정
const dayWidth = 18; // 하루치 너비 (px). 넓게 보려면 숫자를 키우세요.
const rowHeight = 56; // 각 행의 높이 (px). (h-14 Tailwind 클래스와 일치)

// 3. 헬퍼 함수
const parseDate = (dateStr) => {
  const [y, m, d] = dateStr.split('-');
  return new Date(y, m - 1, d);
};

// 4. 타임라인 동적 계산 로직
const timelineRange = computed(() => {
  if (events.length === 0) return { start: new Date(), end: new Date() };
  
  const startDates = events.value.map(e => parseDate(e.start).getTime());
  const endDates = events.value.map(e => parseDate(e.end).getTime());

  const minDate = new Date(Math.min(...startDates));
  const maxDate = new Date(Math.max(...endDates));

  // 시작일의 달 1일 ~ 종료일의 달 말일까지 넉넉하게 렌더링
  return {
    start: new Date(minDate.getFullYear(), minDate.getMonth(), 1),
    end: new Date(maxDate.getFullYear(), maxDate.getMonth() + 1, 0)
  };
});

// 상단 헤더용 월(Month) 배열 생성
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
    // 다음 달 1일로 이동
    current = new Date(y, m + 1, 1);
  }
  return arr;
});

// 전체 타임라인 가로 길이 산출
const totalWidth = computed(() => months.value.reduce((acc, curr) => acc + curr.width, 0));

// 상단 연도 표시 포맷팅
const displayYearRange = computed(() => {
  const startYear = timelineRange.value.start.getFullYear();
  const endYear = timelineRange.value.end.getFullYear();
  return startYear === endYear ? `${startYear}` : `${startYear} - ${endYear}`;
});

// 5. 일정 바 위치 계산
const getEventStyle = (event, index) => {
  const eStart = parseDate(event.start);
  const eEnd = parseDate(event.end);
  const tStart = timelineRange.value.start;

  // 시작점 오프셋 (일 단위)
  const startDiff = (eStart.getTime() - tStart.getTime()) / (1000 * 3600 * 24);
  // 기간 (일 단위, 양끝 포함 +1)
  const duration = ((eEnd.getTime() - eStart.getTime()) / (1000 * 3600 * 24)) + 1;

  return {
    top: `${(index * rowHeight) + ((rowHeight - 36) / 2)}px`, // 셀 중앙 정렬
    left: `${startDiff * dayWidth}px`,
    width: `${duration * dayWidth}px`
  };
};


// 오늘 날짜 계산
const getTodayLineStyle = () => {
  const today = new Date();
  today.setHours(0, 0, 0, 0); // 시간 단위를 자정으로 맞춰 정확한 일(Day) 계산

  const tStart = timelineRange.value.start;
  const tEnd = timelineRange.value.end;

  // 오늘 날짜가 현재 렌더링된 타임라인 범위 밖에 있으면 숨김
  if (today < tStart || today > tEnd) {
    return { display: 'none' };
  }

  // 시작점으로부터의 오프셋 (일 단위)
  const startDiff = (today.getTime() - tStart.getTime()) / (1000 * 3600 * 24);

  return {
    left: `${startDiff * dayWidth}px`,
    display: 'block'
  };
};

</script>

<template>
  <div class="w-full h-full flex flex-col bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden text-sm font-sans">

    <div class="flex-1 overflow-auto flex relative custom-scrollbar bg-white">
      
      <div class="sticky left-0 z-50 bg-white flex-shrink-0 flex flex-col w-[500px] shadow-[3px_0_10px_rgba(0,0,0,0.03)] border-r border-gray-200">
        
        <div class="sticky top-0 z-50 bg-gray-50 h-10 border-b border-gray-200 flex flex-col box-border">
          <div class="h-full flex text-xs font-bold text-gray-500 text-center">
            <div class="w-40 border-r border-gray-200 flex items-center justify-center">담당사명</div>
            <div class="flex-1 flex items-center justify-center">캠페인명</div>
          </div>
        </div>

        <div class="flex-1 bg-white">
          <div 
            v-for="event in events" 
            :key="`sidebar-${event.id}`"
            class="h-14 border-b border-gray-100 flex items-center text-center hover:bg-gray-50 transition-colors"
          >
            <div class="w-40 border-r border-gray-100 text-[#8B5CF6] font-bold truncate px-2">{{ event.projectManager }}</div>
            <div class="flex-1 px-4 text-left font-bold text-gray-800 truncate">{{ event.title }}</div>
          </div>
        </div>
      </div>

      <div class="relative bg-white" :style="{ width: `${totalWidth}px` }">
        
        <div class="sticky top-0 z-40 bg-white h-10 border-b border-gray-200 flex flex-col box-border">
          <div class="h-full flex">
            <div 
              v-for="(month, idx) in months" 
              :key="`month-header-${idx}`"
              class="border-r border-gray-200 h-full relative"
              :style="{ width: `${month.width}px` }"
            >
              <span class="sticky left-0 right-0 w-max mx-auto h-full flex items-center font-bold text-gray-600 px-4">
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
              class="h-full border-r border-gray-100" 
              :style="{ width: `${month.width}px` }"
            ></div>
          </div>

          <div class="absolute inset-0 pointer-events-none z-0 flex flex-col">
            <div v-for="event in events" :key="`grid-row-${event.id}`" class="h-14 border-b border-gray-100 w-full"></div>
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
            class="absolute top-0 bottom-0 w-[2px] bg-red-400 z-20 pointer-events-none"
            :style="getTodayLineStyle()"
          ></div>

        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
/* 커스텀 스크롤바 */
.custom-scrollbar::-webkit-scrollbar {
  width: 12px;
  height: 12px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: #f8fafc;
  border-left: 1px solid #e2e8f0;
  border-top: 1px solid #e2e8f0;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 6px;
  border: 2px solid #f8fafc;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: #8B5CF6; /* 포인트 컬러 호버 */
}
</style>