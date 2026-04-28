<script setup>
import { ref, onMounted } from 'vue';
import MainCalendar from '@/components/overview/MainCalendar.vue';
import MainTable from '@/components/overview/MainTable.vue';
import MainTimeline from '@/components/overview/MainTimeline.vue';
import { getCampagin } from '@/api/overview';

onMounted(async () => {
  try {
    const res = await getCampagin()
    events.value = res
    console.log('캠페인 정보를 불러왔습니다.')
  } catch (error) {
    console.error('캠페인 정보를 불러오는데 실패했습니다.', error)
  }
})

// 현재 선택된 뷰 상태 (기본값: calendar)
const currentView = ref('calendar');

// 뷰 옵션 정의
const viewOptions = [
  { id: 'calendar', name: '캘린더', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
  { id: 'timeline', name: '타임라인', icon: 'M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2' },
  { id: 'table', name: '테이블', icon: 'M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z' }
];

const pastelPalette = [
'bg-violet-100 text-violet-800 border border-violet-200',   // 1. 연한 보라 (포인트 컬러 유사)
'bg-fuchsia-100 text-fuchsia-800 border border-fuchsia-200', // 2. 연한 핑크
'bg-blue-100 text-blue-800 border border-blue-200',       // 3. 연한 파랑
'bg-emerald-100 text-emerald-800 border border-emerald-200', // 4. 연한 민트/초록
'bg-amber-100 text-amber-800 border border-amber-200'        // 5. 연한 노랑
];

// 공통데이터 더미 (추후 API로 변경)
const events = ref([
{ id: 1, title: '한화 시스템 신규 개발 프로젝트 캠페인', start: '2026-03-20', end: '2026-04-24', projectManager:'한화 시스템', colorClass: pastelPalette[1] },
{ id: 2, title: '갤러리아 아이브 팝업스토어 캠페인', start: '2026-04-21', end: '2026-05-20', projectManager:'한화 갤러리아', colorClass: pastelPalette[4] },
{ id: 3, title: '한화 호텔앤리조트 콜라보 이벤트 캠페인', start: '2026-04-27', projectManager:'한화 호텔앤리조트', end: '2026-05-02', colorClass: pastelPalette[2] },
{ id: 4, title: '한화 캠페인 4', start: '2026-04-26', end: '2026-05-15', projectManager:'한화 계열사1', colorClass: pastelPalette[1] },
{ id: 5, title: '한화 캠페인 5', start: '2026-05-04', end: '2026-05-28', projectManager:'한화 계열사2', colorClass: pastelPalette[0] },
{ id: 6, title: '한화 캠페인 6', start: '2026-05-13', end: '2026-06-02', projectManager:'한화 계열사3', colorClass: pastelPalette[3] },
{ id: 7, title: '한화 캠페인 6', start: '2026-05-13', end: '2026-06-02', projectManager:'한화 계열사3', colorClass: pastelPalette[3] },
]);

onMounted

</script>

<template>
  <div class="w-full min-h-screen bg-white flex flex-col">
<div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-white sticky top-0 z-10">
    
    <h2 class="text-xl font-bold text-gray-900">일정 한눈에 보기</h2>
    
    <div class="flex items-center">
      <template v-for="(view, index) in viewOptions" :key="view.id">
        <button 
          @click="currentView = view.id"
          :class="[
            'flex items-center space-x-2 px-4 py-1.5 text-sm font-medium transition-all duration-200',
            currentView === view.id 
              ? 'text-[#8B5CF6]' 
              : 'text-gray-500 hover:text-gray-700'
          ]"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="view.icon" />
          </svg>
          <span>{{ view.name }}</span>
        </button>

        <div 
          v-if="index < viewOptions.length - 1" 
          class="w-[1px] h-3 bg-gray-300 mx-1"
        ></div>
      </template>
    </div>

  </div>

    <main class="flex-1 overflow-auto">
      <transition name="fade" mode="out-in">
        <div :key="currentView" class="h-full">
          <MainCalendar v-if="currentView === 'calendar'" :eventsData="events" />
          <MainTable v-else-if="currentView === 'table'" :eventsData="events" />
          <MainTimeline v-else-if="currentView === 'timeline'" :eventsData="events"/>
        </div>
      </transition>
    </main>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>