<script setup>
import { ref, computed } from 'vue';

// Overview.vue에서 전달받는 다크모드 상태
const props = defineProps({
  isDark: {
    type: Boolean,
    default: false
  }
});

// 상단 요약 위젯용 더미 데이터
const summaryStats = ref([
  { label: '등록된 자산', value: '12', unit: '건', color: 'text-blue-500' },
  { label: '신규 파트너 제안', value: '8', unit: '건', color: 'text-fuchsia-500' },
  { label: '최우선 추천 대기', value: '3', unit: '건', color: 'text-[#8B5CF6]' },
  { label: '진행중인 캠페인', value: '5', unit: '건', color: 'text-emerald-500' },
]);

// MATCH_013: 파트너 제안 목록 더미 데이터
// MATCH_009: 90점 이상(최우선 추천), 80점 이상(우선 검토), 70점 이상(조건부 검토) 기준 반영
const partnerProposals = ref([
  {
    id: 1,
    partnerName: '스타벅스 코리아',
    benefitSummary: '시즌 음료 사이즈업 쿠폰 1만장 및 앱 배너 노출',
    totalScore: 94,
    grade: '최우선 추천',
    date: '2026-04-28'
  },
  {
    id: 2,
    partnerName: '나이키 코리아',
    benefitSummary: '러닝앱 멤버십 공동 챌린지 및 리미티드 굿즈',
    totalScore: 85,
    grade: '우선 검토',
    date: '2026-04-27'
  },
  {
    id: 3,
    partnerName: 'CGV',
    benefitSummary: 'VIP 고객 대상 프리미엄 관람권 1+1 혜택',
    totalScore: 76,
    grade: '조건부 검토',
    date: '2026-04-25'
  },
  {
    id: 4,
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 런칭 기념 샘플링 키트 제공',
    totalScore: 65,
    grade: '보완 필요',
    date: '2026-04-24'
  }
]);

// 추천 등급에 따른 배지 스타일 반환 함수
const getGradeBadgeStyle = (grade) => {
  const styles = {
    '최우선 추천': 'bg-violet-100 text-violet-700 border-violet-200',
    '우선 검토': 'bg-blue-100 text-blue-700 border-blue-200',
    '조건부 검토': 'bg-amber-100 text-amber-700 border-amber-200',
    '보완 필요': 'bg-rose-100 text-rose-700 border-rose-200',
    '추천 제외': 'bg-gray-100 text-gray-700 border-gray-200'
  };
  
  // 다크모드일 경우의 색상
  const darkStyles = {
    '최우선 추천': 'bg-violet-900/40 text-violet-300 border-violet-700/50',
    '우선 검토': 'bg-blue-900/40 text-blue-300 border-blue-700/50',
    '조건부 검토': 'bg-amber-900/40 text-amber-300 border-amber-700/50',
    '보완 필요': 'bg-rose-900/40 text-rose-300 border-rose-700/50',
    '추천 제외': 'bg-gray-800/40 text-gray-400 border-gray-700/50'
  };

  return props.isDark ? (darkStyles[grade] || darkStyles['추천 제외']) : (styles[grade] || styles['추천 제외']);
};
</script>

<template>
  <div class="space-y-6">
    
    <!-- 1. 상단 현황 요약 위젯 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div 
        v-for="(stat, index) in summaryStats" 
        :key="index"
        class="p-6 rounded-xl border transition-colors duration-300 shadow-sm"
        :class="isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-100'"
      >
        <p class="text-sm font-medium mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
          {{ stat.label }}
        </p>
        <div class="flex items-baseline space-x-1">
          <span class="text-3xl font-bold" :class="[isDark ? 'text-white' : 'text-gray-900']">
            {{ stat.value }}
          </span>
          <span class="text-sm font-medium" :class="stat.color">{{ stat.unit }}</span>
        </div>
      </div>
    </div>

    <!-- 2. 파트너 제안 목록 리스트 (MATCH_013) -->
    <div>
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-bold" :class="isDark ? 'text-white' : 'text-gray-800'">
          신규 파트너 제안 요약
        </h3>
        <button class="text-sm font-medium text-[#8B5CF6] hover:underline">
          전체 보기
        </button>
      </div>

      <!-- 제안 카드 그리드 -->
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
        <div 
          v-for="proposal in partnerProposals" 
          :key="proposal.id"
          class="relative p-6 rounded-xl border transition-all duration-200 hover:shadow-md cursor-pointer flex flex-col h-full"
          :class="isDark ? 'bg-[#262637] border-[#37374f] hover:border-[#8B5CF6]/50' : 'bg-white border-gray-200 hover:border-[#8B5CF6]/50'"
        >
          <!-- 카드 헤더: 파트너사명 및 뱃지 -->
          <div class="flex justify-between items-start mb-4">
            <div>
              <h4 class="text-lg font-bold" :class="isDark ? 'text-slate-100' : 'text-gray-900'">
                {{ proposal.partnerName }}
              </h4>
              <p class="text-xs mt-1" :class="isDark ? 'text-slate-500' : 'text-gray-400'">
                제안일: {{ proposal.date }}
              </p>
            </div>
            
            <!-- 추천 등급 배지 -->
            <span 
              class="px-2.5 py-1 text-xs font-semibold rounded-full border"
              :class="getGradeBadgeStyle(proposal.grade)"
            >
              {{ proposal.grade }}
            </span>
          </div>

          <!-- 카드 본문: 제공 혜택 요약 -->
          <div class="flex-1 mb-6">
            <p class="text-sm line-clamp-2" :class="isDark ? 'text-slate-300' : 'text-gray-600'">
              {{ proposal.benefitSummary }}
            </p>
          </div>

          <!-- 카드 푸터: 종합 점수 -->
          <div class="pt-4 border-t flex justify-between items-center" 
               :class="isDark ? 'border-[#37374f]' : 'border-gray-100'">
            <span class="text-sm font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">종합 평가 점수</span>
            <div class="flex items-center space-x-2">
              <!-- 점수 바 시각화 (간단한 프로그레스 바) -->
              <div class="w-20 h-2 rounded-full overflow-hidden" :class="isDark ? 'bg-slate-700' : 'bg-gray-100'">
                <div 
                  class="h-full rounded-full" 
                  :class="proposal.totalScore >= 80 ? 'bg-[#8B5CF6]' : (proposal.totalScore >= 70 ? 'bg-amber-400' : 'bg-rose-400')"
                  :style="`width: ${proposal.totalScore}%`"
                ></div>
              </div>
              <span class="text-lg font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">
                {{ proposal.totalScore }}<span class="text-xs font-normal ml-0.5" :class="isDark ? 'text-slate-500' : 'text-gray-400'">점</span>
              </span>
            </div>
          </div>
          
        </div>
      </div>
    </div>
    
  </div>
</template>