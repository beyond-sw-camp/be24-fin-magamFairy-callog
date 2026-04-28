<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false
  }
});

// MATCH_010: 선택 가능한 캠페인 목표 리스트
const campaignGoals = [
  { id: 'new_cust', name: '신규 고객 유입', icon: 'M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z' },
  { id: 'vip', name: 'VIP 혜택 강화', icon: 'M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z' },
  { id: 'room_res', name: '객실 예약 증가', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { id: 'app_join', name: '앱 가입 증가', icon: 'M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z' },
  { id: 'brand_exp', name: '브랜드 노출', icon: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z' }
];

const selectedGoal = ref('vip'); // 기본 선택값
const isGenerating = ref(false);

// MATCH_011: 제휴 캠페인 조합 더미 데이터 (목표 선택 시 필터링/정렬된다고 가정)
const proposedCombinations = ref([
  {
    id: 1,
    matchTitle: '갤러리아 VIP 프리미엄 리프레시',
    badge: '최적 조합 (수익 기여도 최상)',
    hanwhaAsset: '갤러리아 VIP 고객층, 앱 배너',
    partnerOffer: '스타벅스 리저브 사이즈업 & 전용 굿즈',
    targetAudience: '갤러리아 VIP App 활성 고객 (약 5만 명)',
    benefitStructure: '스타벅스 1만명 쿠폰 + 한화 앱 전용 기획전',
    roles: '한화(타겟팅/앱운영), 스타벅스(쿠폰/굿즈제공)',
    channels: '갤러리아 앱, 알림톡, 스타벅스 앱 공지',
    outputs: '이벤트 랜딩 페이지, 알림톡 문구, 쿠폰 난수',
    schedule: '기획 2주 / 운영 3주 (5월 중순 시작 예상)',
    totalScore: 94
  },
  {
    id: 2,
    matchTitle: '호텔 앤 리조트 액티브 스테이',
    badge: '우선 검토 (목표 연관성 높음)',
    hanwhaAsset: '호텔 객실 패키지, 리조트 이용권',
    partnerOffer: '나이키 러닝앱 공동 챌린지 및 굿즈',
    targetAudience: '2030 액티브 레저 관심 고객',
    benefitStructure: '숙박권 연계 러닝 패키지 할인권 제공',
    roles: '한화(객실할인), 나이키(러닝앱 챌린지 개발)',
    channels: '한화리조트 공홈, 나이키 NRC 앱',
    outputs: '패키지 상품 페이지, SNS 홍보물 3종',
    schedule: '기획 3주 / 운영 1개월 (6월 초 시작 예상)',
    totalScore: 88
  }
]);

// 목표 변경 시 로딩 애니메이션 시뮬레이션
const selectGoal = (goalId) => {
  if (selectedGoal.value === goalId) return;
  selectedGoal.value = goalId;
  isGenerating.value = true;
  
  // 실제 환경에서는 여기서 API를 호출하여 해당 목표에 맞는 조합을 다시 가져옵니다[cite: 1].
  setTimeout(() => {
    isGenerating.value = false;
  }, 800);
};

// 운영 보드 전환 핸들러 (MATCH_012)
const moveToOperationBoard = (combo) => {
  alert(`'${combo.matchTitle}' 조합을 운영 보드로 전환합니다.\n필요한 작업(문구 생성, 콘텐츠 제작 등)이 자동 생성됩니다.`);
};
</script>

<template>
  <div class="space-y-8 max-w-7xl mx-auto">
    
    <!-- 1. 캠페인 목표 선택 영역 (MATCH_010) -->
    <section 
      class="p-6 rounded-2xl border transition-colors duration-300 shadow-sm"
      :class="isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-100'"
    >
      <div class="mb-5">
        <h3 class="text-lg font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">
          1. 캠페인 목표 설정
        </h3>
        <p class="text-sm mt-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
          제휴 캠페인을 통해 달성하고자 하는 핵심 목표를 선택해주세요. 선택된 목표에 따라 최적의 파트너 조합이 추천됩니다.
        </p>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
        <button
          v-for="goal in campaignGoals"
          :key="goal.id"
          @click="selectGoal(goal.id)"
          class="relative p-4 rounded-xl border flex flex-col items-center justify-center space-y-2 transition-all duration-200 focus:outline-none"
          :class="[
            selectedGoal === goal.id
              ? (isDark ? 'bg-[#8B5CF6]/20 border-[#8B5CF6] text-[#A78BFA]' : 'bg-violet-50 border-[#8B5CF6] text-[#8B5CF6]')
              : (isDark ? 'bg-[#1e1e2d] border-[#37374f] text-slate-400 hover:border-slate-500' : 'bg-gray-50 border-gray-200 text-gray-500 hover:border-gray-300')
          ]"
        >
          <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" :d="goal.icon" />
          </svg>
          <span class="text-sm font-semibold">{{ goal.name }}</span>
          
          <!-- 선택됨 표시 링 -->
          <div v-if="selectedGoal === goal.id" class="absolute top-2 right-2 w-2 h-2 rounded-full bg-[#8B5CF6]"></div>
        </button>
      </div>
    </section>

    <!-- 2. 제휴 캠페인 조합 자동 제안 영역 (MATCH_011) -->
    <section>
      <div class="flex items-end justify-between mb-5">
        <div>
          <h3 class="text-lg font-bold flex items-center gap-2" :class="isDark ? 'text-white' : 'text-gray-900'">
            2. AI 캠페인 조합 추천
            <span v-if="isGenerating" class="flex h-3 w-3 relative">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#8B5CF6] opacity-75"></span>
              <span class="relative inline-flex rounded-full h-3 w-3 bg-[#8B5CF6]"></span>
            </span>
          </h3>
          <p class="text-sm mt-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
            한화 자산, 파트너 혜택, 파트너 평가 점수를 종합하여 성공 확률이 높은 시나리오를 제안합니다.
          </p>
        </div>
      </div>

      <!-- 조합 리스트 -->
      <div class="space-y-5" :class="{ 'opacity-50 pointer-events-none': isGenerating }">
        <div 
          v-for="combo in proposedCombinations" 
          :key="combo.id"
          class="p-6 rounded-2xl border transition-all duration-300 flex flex-col lg:flex-row gap-6 shadow-sm hover:shadow-md"
          :class="isDark ? 'bg-[#262637] border-[#37374f] hover:border-[#8B5CF6]/40' : 'bg-white border-gray-200 hover:border-[#8B5CF6]/40'"
        >
          <!-- 좌측: 제목 및 기본 정보 -->
          <div class="lg:w-1/3 flex flex-col">
            <span class="inline-block px-2.5 py-1 text-xs font-semibold rounded-md border w-max mb-3"
                  :class="isDark ? 'bg-violet-900/30 text-violet-300 border-violet-800/50' : 'bg-violet-100 text-violet-700 border-violet-200'">
              {{ combo.badge }}
            </span>
            <h4 class="text-xl font-bold mb-4" :class="isDark ? 'text-white' : 'text-gray-900'">
              {{ combo.matchTitle }}
            </h4>
            
            <div class="space-y-3 flex-grow text-sm">
              <div>
                <p class="font-medium mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">매칭 자산 (한화)</p>
                <p :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.hanwhaAsset }}</p>
              </div>
              <div class="flex items-center gap-2" :class="isDark ? 'text-slate-500' : 'text-gray-300'">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/></svg>
              </div>
              <div>
                <p class="font-medium mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">파트너 제공 혜택</p>
                <p :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.partnerOffer }}</p>
              </div>
            </div>

            <div class="mt-6">
              <button 
                @click="moveToOperationBoard(combo)"
                class="w-full py-2.5 rounded-lg text-sm font-semibold transition-colors flex items-center justify-center gap-2"
                :class="isDark ? 'bg-[#8B5CF6] text-white hover:bg-violet-600' : 'bg-[#8B5CF6] text-white hover:bg-violet-700'"
              >
                조합 선택 및 운영 보드 전환
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
              </button>
            </div>
          </div>

          <!-- 우측: 세부 시나리오 정보 (MATCH_011 세부항목) -->
          <div class="lg:w-2/3 grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="p-4 rounded-xl" :class="isDark ? 'bg-[#1e1e2d]' : 'bg-gray-50'">
              <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">대상 고객</p>
              <p class="text-sm" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.targetAudience }}</p>
            </div>
            <div class="p-4 rounded-xl" :class="isDark ? 'bg-[#1e1e2d]' : 'bg-gray-50'">
              <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">활용 채널</p>
              <p class="text-sm" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.channels }}</p>
            </div>
            <div class="p-4 rounded-xl md:col-span-2" :class="isDark ? 'bg-[#1e1e2d]' : 'bg-gray-50'">
              <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">혜택 구조</p>
              <p class="text-sm" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.benefitStructure }}</p>
            </div>
            <div class="p-4 rounded-xl" :class="isDark ? 'bg-[#1e1e2d]' : 'bg-gray-50'">
              <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">참여사 역할</p>
              <p class="text-sm" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.roles }}</p>
            </div>
            <div class="p-4 rounded-xl" :class="isDark ? 'bg-[#1e1e2d]' : 'bg-gray-50'">
              <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">필요한 산출물</p>
              <p class="text-sm" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.outputs }}</p>
            </div>
            <div class="p-4 rounded-xl md:col-span-2" :class="isDark ? 'bg-[#1e1e2d]' : 'bg-gray-50'">
              <div class="flex justify-between items-center">
                <div>
                  <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">예상 일정</p>
                  <p class="text-sm" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ combo.schedule }}</p>
                </div>
                <div class="text-right">
                  <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-500' : 'text-gray-500'">종합 점수</p>
                  <p class="text-xl font-bold text-[#8B5CF6]">{{ combo.totalScore }}<span class="text-sm ml-0.5">점</span></p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

  </div>
</template>