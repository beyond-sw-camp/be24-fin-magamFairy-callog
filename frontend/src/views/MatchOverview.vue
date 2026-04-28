<script setup>
import { ref, computed } from 'vue';
import { usePlannerStore } from '@/stores/planner';

// 각 탭에 해당하는 컴포넌트를 import 해주세요. (경로는 프로젝트에 맞게 수정 필요)
import MatchDashboard from '@/components/matchengine/MatchDashboard.vue';
import CampaignMatching from '@/components/matchengine/CampaignMatching.vue';
import AssetBenefitManagement from '@/components/matchengine/AssetBenefitManagement.vue';
import PartnerEvaluation from '@/components/matchengine/PartnerEvaluation.vue';
import OperationBoard from '@/components/matchengine/OperationBoard.vue';

const store = usePlannerStore();

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

// 현재 선택된 탭 상태 (기본값: dashboard)
const currentTab = ref('dashboard');

// 탭 목록 정의
const tabs = [
  { id: 'dashboard', name: '대쉬보드', component: MatchDashboard },
  { id: 'matching', name: '캠페인 매칭', component: CampaignMatching },
  { id: 'assets', name: '내 자산/혜택 관리', component: AssetBenefitManagement },
  { id: 'evaluation', name: '파트너 평가', component: PartnerEvaluation },
  { id: 'operation', name: '운영 보드', component: OperationBoard }
];

// 현재 렌더링할 컴포넌트 계산
const currentComponent = computed(() => {
  return tabs.find(tab => tab.id === currentTab.value)?.component;
});
</script>

<template>
  <div class="w-full min-h-screen flex flex-col transition-colors duration-300"
       :class="isDark ? 'bg-[#1e1e2d]' : 'bg-[#f8f9fa]'">
    
    <!-- Header & Tab Navigation (왼쪽 위 크게 배치) -->
    <div class="px-8 pt-6 pb-0 border-b sticky top-0 z-10 transition-colors duration-300"
         :class="isDark ? 'bg-[#1e1e2d] border-[#2d2d3f]' : 'bg-white border-gray-200'">
      
      <div class="flex items-center space-x-8">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          @click="currentTab = tab.id"
          class="relative pb-4 text-xl font-bold transition-all duration-200 tracking-tight"
          :class="[
            currentTab === tab.id 
              ? 'text-[#8B5CF6]' 
              : (isDark ? 'text-slate-500 hover:text-slate-300' : 'text-gray-400 hover:text-gray-800')
          ]"
        >
          {{ tab.name }}
          
          <!-- 활성화된 탭 하단 인디케이터 바 -->
          <span 
            v-if="currentTab === tab.id" 
            class="absolute bottom-0 left-0 w-full h-[3px] bg-[#8B5CF6] rounded-t-md"
          ></span>
        </button>
      </div>

    </div>

    <!-- Main Content Area -->
    <main class="flex-1 overflow-auto p-6">
      <transition name="fade" mode="out-in">
        <KeepAlive>
          <component :is="currentComponent" :isDark="isDark" />
        </KeepAlive>
      </transition>
    </main>
  </div>
</template>

<style scoped>
/* 부드러운 화면 전환 애니메이션 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 스무스한 배경색 전환을 위한 설정 */
.transition-colors {
  transition-property: background-color, border-color, color;
}
</style>