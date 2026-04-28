<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false
  }
});

// 서브 탭 상태 관리 ('assets': 계열사 자산, 'benefits': 파트너 혜택)
const currentSubTab = ref('assets');

// MATCH_001: 등록 가능한 자산 유형 목록
const assetTypes = ['갤러리아 앱', 'VIP 고객층', '호텔 객실', '리조트 이용권', '스포츠 티켓', '멤버십 채널'];

// MATCH_001: 계열사 자산 더미 데이터
const hanwhaAssets = ref([
  {
    id: 1,
    type: '갤러리아 앱',
    affiliate: '한화갤러리아',
    target: '2030 프리미엄 쇼핑 고객',
    scale: 'MAU 45만 명',
    conditions: '월 1회 메인 팝업 노출 가능',
    isActive: true
  },
  {
    id: 2,
    type: '호텔 객실',
    affiliate: '한화호텔앤드리조트',
    target: '가족 단위 레저 고객',
    scale: '전국 12개 체인, 월 1만 객실',
    conditions: '주중(일~목) 한정 프로모션 연계',
    isActive: true
  },
  {
    id: 3,
    type: '스포츠 티켓',
    affiliate: '한화이글스',
    target: '2030 야구 팬덤',
    scale: '홈경기 회당 1,000석',
    conditions: '주말 경기 제외, 사전 협의 필요',
    isActive: false // 비활성화 상태 예시
  }
]);

// MATCH_002: 등록 가능한 혜택 유형 목록
const benefitTypes = ['샘플', '할인권', '체험권', '공동 콘텐츠', '멤버십 혜택'];

// MATCH_002: 파트너 혜택 더미 데이터
const partnerBenefits = ref([
  {
    id: 1,
    partnerName: '록시땅 (L\'OCCITANE)',
    name: '신제품 핸드크림 10ml 샘플링',
    type: '샘플',
    target: '2040 뷰티 고관여 여성',
    scale: '선착순 10,000개',
    costCondition: '샘플 전액 파트너 부담, 배송비 고객 부담',
    status: '평가 대기' // 시스템 자동 평가 대상
  },
  {
    id: 2,
    partnerName: '야놀자',
    name: '프리미엄 레저 시설 30% 할인권',
    type: '할인권',
    target: '휴가철 여행 계획 고객',
    scale: '제한 없음 (발급 기준)',
    costCondition: '할인 비용 파트너 100% 부담',
    status: '평가 완료'
  },
  {
    id: 3,
    partnerName: 'Tving (티빙)',
    name: '오리지널 콘텐츠 공동 프로모션',
    type: '공동 콘텐츠',
    target: '', // 필수값 누락 예시
    scale: '1개월 프리미엄 이용권 1,000매',
    costCondition: '', // 필수값 누락 예시
    status: '임시 저장' // 필수 입력값 부족 시 임시 저장 상태
  }
]);

// 등록 모달 상태 (UI 시뮬레이션용)
const isModalOpen = ref(false);
const openRegisterModal = () => {
  isModalOpen.value = true;
};
</script>

<template>
  <div class="space-y-6 max-w-7xl mx-auto">
    
    <!-- 상단 헤더 및 서브 탭 -->
    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b pb-4 transition-colors duration-300"
         :class="isDark ? 'border-[#37374f]' : 'border-gray-200'">
      <div class="flex space-x-6">
        <button 
          @click="currentSubTab = 'assets'"
          class="text-lg font-bold pb-4 -mb-4 border-b-2 transition-all duration-200"
          :class="[
            currentSubTab === 'assets' 
              ? (isDark ? 'border-[#8B5CF6] text-white' : 'border-[#8B5CF6] text-gray-900') 
              : 'border-transparent ' + (isDark ? 'text-slate-500 hover:text-slate-300' : 'text-gray-400 hover:text-gray-700')
          ]"
        >
          계열사 자산 관리
        </button>
        <button 
          @click="currentSubTab = 'benefits'"
          class="text-lg font-bold pb-4 -mb-4 border-b-2 transition-all duration-200"
          :class="[
            currentSubTab === 'benefits' 
              ? (isDark ? 'border-[#8B5CF6] text-white' : 'border-[#8B5CF6] text-gray-900') 
              : 'border-transparent ' + (isDark ? 'text-slate-500 hover:text-slate-300' : 'text-gray-400 hover:text-gray-700')
          ]"
        >
          파트너 혜택 관리
        </button>
      </div>
      
      <!-- 신규 등록 버튼 -->
      <button 
        @click="openRegisterModal"
        class="px-4 py-2 rounded-lg text-sm font-semibold text-white bg-[#8B5CF6] hover:bg-violet-600 transition-colors flex items-center gap-2"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
        {{ currentSubTab === 'assets' ? '신규 자산 등록' : '신규 혜택 등록' }}
      </button>
    </div>

    <!-- 1. 계열사 자산 리스트 (MATCH_001) -->
    <div v-if="currentSubTab === 'assets'" class="grid grid-cols-1 lg:grid-cols-2 gap-5 animate-fade-in">
      <div 
        v-for="asset in hanwhaAssets" 
        :key="asset.id"
        class="p-5 rounded-xl border transition-all duration-200 relative overflow-hidden flex flex-col"
        :class="isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-200'"
      >
        <div class="flex justify-between items-start mb-3">
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-1 text-xs font-semibold rounded-md border"
                  :class="isDark ? 'bg-slate-800 text-slate-300 border-slate-600' : 'bg-gray-100 text-gray-700 border-gray-200'">
              {{ asset.type }}
            </span>
            <h4 class="text-lg font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">
              {{ asset.affiliate }}
            </h4>
          </div>
          <!-- 활성화/비활성화 토글 (MATCH_001: 수정/비활성화 기능) -->
          <label class="flex items-center cursor-pointer">
            <div class="relative">
              <input type="checkbox" class="sr-only" :checked="asset.isActive">
              <div class="block w-10 h-6 rounded-full transition-colors"
                   :class="asset.isActive ? 'bg-[#8B5CF6]' : (isDark ? 'bg-slate-600' : 'bg-gray-300')"></div>
              <div class="dot absolute left-1 top-1 bg-white w-4 h-4 rounded-full transition-transform"
                   :class="asset.isActive ? 'transform translate-x-4' : ''"></div>
            </div>
            <span class="ml-2 text-xs font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
              {{ asset.isActive ? '활성' : '비활성' }}
            </span>
          </label>
        </div>

        <div class="space-y-2.5 flex-grow text-sm mt-2">
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">고객층</span>
            <span class="col-span-2" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ asset.target }}</span>
          </div>
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">도달 규모</span>
            <span class="col-span-2" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ asset.scale }}</span>
          </div>
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">활용 조건</span>
            <span class="col-span-2" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ asset.conditions }}</span>
          </div>
        </div>

        <div class="mt-5 pt-4 border-t flex justify-end gap-2" :class="isDark ? 'border-[#37374f]' : 'border-gray-100'">
          <button class="px-3 py-1.5 text-xs font-medium rounded-md transition-colors"
                  :class="isDark ? 'bg-slate-700 text-slate-300 hover:bg-slate-600' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'">수정</button>
        </div>
      </div>
    </div>

    <!-- 2. 파트너 혜택 리스트 (MATCH_002) -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-5 animate-fade-in">
      <div 
        v-for="benefit in partnerBenefits" 
        :key="benefit.id"
        class="p-5 rounded-xl border transition-all duration-200 flex flex-col"
        :class="[
          isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-200',
          benefit.status === '임시 저장' ? (isDark ? 'opacity-80 border-dashed border-slate-500' : 'opacity-80 border-dashed border-gray-400') : ''
        ]"
      >
        <div class="flex justify-between items-start mb-3">
          <div>
            <p class="text-xs font-semibold mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">{{ benefit.partnerName }}</p>
            <h4 class="text-lg font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">
              {{ benefit.name }}
            </h4>
          </div>
          
          <!-- 상태 라벨 (MATCH_002: 필수 입력값 부족 시 임시 저장, 등록 완료 시 평가 대기) -->
          <span 
            class="px-2.5 py-1 text-xs font-semibold rounded-full border whitespace-nowrap"
            :class="{
              'bg-blue-100 text-blue-700 border-blue-200': benefit.status === '평가 대기' && !isDark,
              'bg-blue-900/40 text-blue-300 border-blue-700/50': benefit.status === '평가 대기' && isDark,
              'bg-emerald-100 text-emerald-700 border-emerald-200': benefit.status === '평가 완료' && !isDark,
              'bg-emerald-900/40 text-emerald-300 border-emerald-700/50': benefit.status === '평가 완료' && isDark,
              'bg-amber-100 text-amber-700 border-amber-200': benefit.status === '임시 저장' && !isDark,
              'bg-amber-900/40 text-amber-300 border-amber-700/50': benefit.status === '임시 저장' && isDark
            }"
          >
            {{ benefit.status }}
          </span>
        </div>

        <div class="space-y-2.5 flex-grow text-sm mt-3">
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium flex items-center gap-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">혜택 유형</span>
            <span class="col-span-2" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ benefit.type }}</span>
          </div>
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium flex items-center gap-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
              대상 고객
              <span v-if="!benefit.target" class="w-1.5 h-1.5 rounded-full bg-rose-500"></span> <!-- 필수값 누락 표시 -->
            </span>
            <span class="col-span-2" :class="benefit.target ? (isDark ? 'text-slate-200' : 'text-gray-800') : 'text-rose-500 text-xs italic'">미입력 (필수)</span>
          </div>
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium flex items-center gap-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">혜택 규모</span>
            <span class="col-span-2" :class="isDark ? 'text-slate-200' : 'text-gray-800'">{{ benefit.scale }}</span>
          </div>
          <div class="grid grid-cols-3 gap-2">
            <span class="font-medium flex items-center gap-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
              비용 분담
              <span v-if="!benefit.costCondition" class="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
            </span>
            <span class="col-span-2" :class="benefit.costCondition ? (isDark ? 'text-slate-200' : 'text-gray-800') : 'text-rose-500 text-xs italic'">미입력 (필수)</span>
          </div>
        </div>
        
        <div class="mt-5 pt-4 border-t flex justify-end gap-2" :class="isDark ? 'border-[#37374f]' : 'border-gray-100'">
           <button class="px-3 py-1.5 text-xs font-medium rounded-md transition-colors"
                  :class="isDark ? 'bg-slate-700 text-slate-300 hover:bg-slate-600' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'">수정</button>
        </div>
      </div>
    </div>
    
  </div>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 토글 버튼 내부 닷 애니메이션 */
.dot {
  transition: transform 0.2s ease-in-out;
}
</style>