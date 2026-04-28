<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false
  }
});

// MATCH_012: 운영 보드에 함께 저장되는 파트너 평가 점수와 추천 사유 데이터
const campaignContext = ref({
  matchTitle: '갤러리아 VIP 프리미엄 리프레시',
  partnerName: '스타벅스 코리아',
  totalScore: 94,
  recommendationReason: '한화의 2030 액티브 레저 고객층과 스타벅스 브랜드 타겟이 매우 일치하며, 높은 브랜드 적합도와 앱 가입 전환(수익 기여)이 기대되어 최우선으로 추천된 캠페인입니다.',
  startDate: '2026-05-15',
  endDate: '2026-06-05'
});

// 칸반 보드 컬럼 정의
const columns = ref([
  { id: 'todo', title: '할 일 (To Do)', color: 'border-gray-300' },
  { id: 'in_progress', title: '진행 중 (In Progress)', color: 'border-blue-400' },
  { id: 'review', title: '검수 대기 (Review)', color: 'border-amber-400' },
  { id: 'done', title: '완료 (Done)', color: 'border-emerald-400' }
]);

// MATCH_012: 자동 생성 가능한 작업 리스트 반영
const tasks = ref([
  { id: 1, title: '문구 생성', columnId: 'in_progress', tag: '카피라이팅', assignee: '김콘텐츠', dueDate: '05.02' },
  { id: 2, title: '일정 관리', columnId: 'in_progress', tag: '운영', assignee: '박운영', dueDate: '05.01' },
  { id: 3, title: '콘텐츠 제작', columnId: 'todo', tag: '디자인', assignee: '이디자인', dueDate: '05.05' },
  { id: 4, title: '검수 요청', columnId: 'todo', tag: '법무/브랜드', assignee: '최법무', dueDate: '05.08' },
  { id: 5, title: '파트너 공유', columnId: 'todo', tag: '커뮤니케이션', assignee: '박운영', dueDate: '05.10' },
  { id: 6, title: '승인 관리', columnId: 'todo', tag: '관리', assignee: '정리드', dueDate: '05.12' },
  { id: 7, title: '결과 기록', columnId: 'todo', tag: '데이터', assignee: '박운영', dueDate: '06.10' }
]);

// 컬럼별 태스크 필터링
const getTasksByColumn = (columnId) => {
  return tasks.value.filter(task => task.columnId === columnId);
};

// 진행률 계산 로직 (완료된 태스크 / 전체 태스크)
const progressRate = computed(() => {
  const doneTasks = getTasksByColumn('done').length;
  const totalTasks = tasks.value.length;
  return totalTasks === 0 ? 0 : Math.round((doneTasks / totalTasks) * 100);
});
</script>

<template>
  <div class="flex flex-col h-[calc(100vh-140px)] max-w-7xl mx-auto space-y-6">
    
    <!-- 1. 캠페인 컨텍스트 요약 패널 (MATCH_012: 파트너 평가 점수 및 추천 사유 저장) -->
    <div class="p-6 rounded-2xl border transition-colors duration-300 shadow-sm shrink-0"
         :class="isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-100'">
      <div class="flex flex-col lg:flex-row justify-between gap-6">
        
        <!-- 좌측: 기본 정보 -->
        <div class="flex-1">
          <div class="flex items-center gap-3 mb-2">
            <span class="px-2.5 py-1 text-xs font-bold rounded-md bg-violet-100 text-violet-700 dark:bg-violet-900/40 dark:text-violet-300 border border-violet-200 dark:border-violet-700/50">
              운영 보드 활성화
            </span>
            <span class="text-sm font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
              {{ campaignContext.startDate }} ~ {{ campaignContext.endDate }}
            </span>
          </div>
          <h2 class="text-2xl font-bold mb-1" :class="isDark ? 'text-white' : 'text-gray-900'">
            {{ campaignContext.matchTitle }}
          </h2>
          <p class="text-sm font-medium" :class="isDark ? 'text-[#A78BFA]' : 'text-[#8B5CF6]'">
            파트너: {{ campaignContext.partnerName }}
          </p>
        </div>

        <!-- 중앙: 평가 점수 및 추천 사유 -->
        <div class="flex-1 p-4 rounded-xl border" :class="isDark ? 'bg-[#1e1e2d] border-[#37374f]' : 'bg-gray-50 border-gray-200'">
          <div class="flex items-start gap-4">
            <div class="text-center shrink-0">
              <p class="text-[10px] font-bold mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">종합 점수</p>
              <p class="text-2xl font-bold text-[#8B5CF6]">{{ campaignContext.totalScore }}</p>
            </div>
            <div class="w-px h-10 my-auto" :class="isDark ? 'bg-[#37374f]' : 'bg-gray-200'"></div>
            <div>
              <p class="text-[10px] font-bold mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">시스템 추천 사유</p>
              <p class="text-xs leading-relaxed" :class="isDark ? 'text-slate-300' : 'text-gray-700'">
                {{ campaignContext.recommendationReason }}
              </p>
            </div>
          </div>
        </div>

        <!-- 우측: 진행률 -->
        <div class="lg:w-48 flex flex-col justify-center">
          <div class="flex justify-between text-sm mb-2 font-bold">
            <span :class="isDark ? 'text-slate-300' : 'text-gray-700'">전체 진행률</span>
            <span class="text-[#8B5CF6]">{{ progressRate }}%</span>
          </div>
          <div class="w-full h-2.5 rounded-full overflow-hidden" :class="isDark ? 'bg-slate-700' : 'bg-gray-100'">
            <div class="h-full bg-[#8B5CF6] transition-all duration-500" :style="{ width: `${progressRate}%` }"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 칸반(Kanban) 보드 영역 (MATCH_012: 진행 상태 추적) -->
    <div class="flex-1 flex gap-5 overflow-x-auto pb-4">
      <div 
        v-for="col in columns" 
        :key="col.id"
        class="flex-1 min-w-[280px] flex flex-col rounded-xl border transition-colors duration-300"
        :class="isDark ? 'bg-[#1e1e2d] border-[#37374f]' : 'bg-gray-50/50 border-gray-200'"
      >
        <!-- 컬럼 헤더 -->
        <div class="p-4 border-b flex justify-between items-center" :class="[isDark ? 'border-[#37374f]' : 'border-gray-200']">
          <div class="flex items-center gap-2">
            <div class="w-3 h-3 rounded-full border-2" :class="col.color"></div>
            <h3 class="font-bold text-sm" :class="isDark ? 'text-white' : 'text-gray-900'">{{ col.title }}</h3>
          </div>
          <span class="text-xs font-bold px-2 py-0.5 rounded-full" 
                :class="isDark ? 'bg-slate-700 text-slate-300' : 'bg-gray-200 text-gray-600'">
            {{ getTasksByColumn(col.id).length }}
          </span>
        </div>

        <!-- 컬럼 내 태스크 리스트 (MATCH_012: 문구 생성, 콘텐츠 제작 등 자동 생성된 작업 표시) -->
        <div class="flex-1 p-3 space-y-3 overflow-y-auto">
          <div 
            v-for="task in getTasksByColumn(col.id)" 
            :key="task.id"
            class="p-4 rounded-lg border cursor-grab hover:-translate-y-0.5 transition-all duration-200 shadow-sm hover:shadow-md relative"
            :class="isDark ? 'bg-[#262637] border-[#37374f] hover:border-slate-500' : 'bg-white border-gray-100 hover:border-gray-300'"
          >
            <div class="flex justify-between items-start mb-3">
              <span class="text-[10px] font-bold px-2 py-1 rounded"
                    :class="isDark ? 'bg-slate-700 text-slate-300' : 'bg-gray-100 text-gray-600'">
                {{ task.tag }}
              </span>
              <span class="text-xs font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-400'">
                마감: {{ task.dueDate }}
              </span>
            </div>
            
            <h4 class="font-bold text-sm mb-4" :class="isDark ? 'text-slate-100' : 'text-gray-800'">
              {{ task.title }}
            </h4>
            
            <div class="flex justify-between items-center border-t pt-3 mt-2" :class="isDark ? 'border-[#37374f]' : 'border-gray-50'">
              <div class="flex items-center gap-1.5">
                <div class="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold text-white bg-[#8B5CF6]">
                  {{ task.assignee.charAt(0) }}
                </div>
                <span class="text-xs font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">{{ task.assignee }}</span>
              </div>
              
              <!-- 진행 상태 변경 아이콘 (UI 시뮬레이션용) -->
              <button class="p-1 rounded-md transition-colors" :class="isDark ? 'hover:bg-slate-700 text-slate-400' : 'hover:bg-gray-100 text-gray-400'">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
              </button>
            </div>
          </div>
          
          <!-- 태스크 추가 버튼 -->
          <button class="w-full py-2 flex items-center justify-center gap-1 text-xs font-medium rounded-lg border border-dashed transition-colors"
                  :class="isDark ? 'border-slate-600 text-slate-400 hover:bg-slate-800 hover:text-slate-200' : 'border-gray-300 text-gray-500 hover:bg-gray-50 hover:text-gray-700'">
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            작업 추가
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* 커서 스타일 적용 (드래그 앤 드롭 느낌 시뮬레이션) */
.cursor-grab {
  cursor: -webkit-grab;
  cursor: grab;
}
.cursor-grab:active {
  cursor: -webkit-grabbing;
  cursor: grabbing;
}
/* 스크롤바 숨기기 (깔끔한 UI용) */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(139, 92, 246, 0.2);
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(139, 92, 246, 0.5);
}
</style>