<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
  isDark: {
    type: Boolean,
    default: false
  }
});

// MATCH_003 ~ MATCH_008, MATCH_015: 평가 대상 파트너 제안 더미 데이터
const proposals = ref([
  {
    id: 1,
    partnerName: '나이키 코리아',
    benefitSummary: '러닝앱 멤버십 공동 챌린지 및 리미티드 굿즈',
    // 5대 지표 원점수 (0~100)
    scores: {
      customerFit: 90, // 고객 적합도 25%
      revenue: 85,     // 수익 기여도 25%
      cost: 80,        // 비용 효율성 20%
      operation: 70,   // 운영 난이도 15% (높을수록 쉬움)
      brand: 95        // 브랜드 적합도 15%
    },
    // MATCH_015: 검토 필요 알림 배열
    warnings: [
      { type: '운영 난이도 검토', message: '산출물이 많고 파트너 조율이 필요하여 일정이 촉박할 수 있습니다.' }
    ],
    // MATCH_013: 문장 형태의 최종 추천 사유
    recommendationReason: '한화의 2030 액티브 레저 고객층과 나이키 브랜드의 타겟이 매우 일치하며, 높은 브랜드 적합도와 앱 가입 전환(수익 기여)이 기대되어 최우선으로 추천합니다.',
    // MATCH_014: 점수 보정 이력
    correctionHistory: [],
    manualScore: null // 수동 보정 점수
  },
  {
    id: 2,
    partnerName: 'LG 생활건강',
    benefitSummary: '신규 뷰티 브랜드 런칭 기념 샘플링 키트 제공',
    scores: { customerFit: 70, revenue: 60, cost: 85, operation: 80, brand: 60 },
    warnings: [
      { type: '고객 적합도 검토', message: '제공되는 샘플의 희망 대상 고객층이 불명확합니다.' },
      { type: '브랜드 적합도 검토', message: '신규 브랜드로 한화 계열사 이미지와의 일관성 검토가 필요합니다.' }
    ],
    recommendationReason: '샘플 비용은 전액 파트너가 부담하여 비용 효율성은 좋으나, 수익 기여도 및 고객 적합도가 낮아 조건부 검토가 필요합니다.',
    correctionHistory: [
      { date: '2026-04-29', before: 68, after: 70.5, reason: '호텔 VIP 객실 비치용으로 활용 시 가치 상승 고려', user: '김운영 리드' }
    ],
    manualScore: 70.5
  }
]);

const selectedProposalId = ref(proposals.value[0].id);

const selectedProposal = computed(() => 
  proposals.value.find(p => p.id === selectedProposalId.value)
);

// 지표 메타 정보 (MATCH_009: 가중치)
const metricsMeta = [
  { key: 'customerFit', label: '고객 적합도', weight: 25, color: 'bg-blue-500' },
  { key: 'revenue', label: '수익 기여도', weight: 25, color: 'bg-emerald-500' },
  { key: 'cost', label: '비용 효율성', weight: 20, color: 'bg-amber-500' },
  { key: 'operation', label: '운영 난이도', weight: 15, color: 'bg-rose-500' },
  { key: 'brand', label: '브랜드 적합도', weight: 15, color: 'bg-purple-500' }
];

// MATCH_009: 종합 점수 및 추천 등급 산정 함수
const calculateFinalScore = (proposal) => {
  // 수동 보정 점수가 있으면 우선 적용
  if (proposal.manualScore !== null) return proposal.manualScore;
  
  let total = 0;
  total += proposal.scores.customerFit * 0.25;
  total += proposal.scores.revenue * 0.25;
  total += proposal.scores.cost * 0.20;
  total += proposal.scores.operation * 0.15;
  total += proposal.scores.brand * 0.15;
  return Number(total.toFixed(1));
};

const getGrade = (score) => {
  if (score >= 90) return { text: '최우선 추천', class: 'text-violet-600 bg-violet-100 border-violet-200' };
  if (score >= 80) return { text: '우선 검토', class: 'text-blue-600 bg-blue-100 border-blue-200' };
  if (score >= 70) return { text: '조건부 검토', class: 'text-amber-600 bg-amber-100 border-amber-200' };
  if (score >= 60) return { text: '보완 필요', class: 'text-rose-600 bg-rose-100 border-rose-200' };
  return { text: '추천 제외', class: 'text-gray-600 bg-gray-100 border-gray-200' };
};

// MATCH_014: 점수 수동 보정 모드 상태
const isEditMode = ref(false);
const editForm = ref({ score: 0, reason: '' });

const openEditMode = () => {
  editForm.value.score = calculateFinalScore(selectedProposal.value);
  editForm.value.reason = '';
  isEditMode.value = true;
};

const saveCorrection = () => {
  if (!editForm.value.reason) {
    alert('보정 사유를 입력해야 합니다.');
    return;
  }
  const prop = selectedProposal.value;
  prop.correctionHistory.push({
    date: new Date().toISOString().split('T')[0],
    before: calculateFinalScore(prop),
    after: Number(editForm.value.score),
    reason: editForm.value.reason,
    user: '현재 접속자'
  });
  prop.manualScore = Number(editForm.value.score);
  isEditMode.value = false;
};
</script>

<template>
  <div class="flex flex-col lg:flex-row gap-6 h-[calc(100vh-140px)] max-w-7xl mx-auto">
    
    <!-- 좌측: 평가 목록 리스트 -->
    <div class="lg:w-1/3 flex flex-col h-full rounded-2xl border transition-colors duration-300"
         :class="isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-100 shadow-sm'">
      <div class="p-5 border-b" :class="isDark ? 'border-[#37374f]' : 'border-gray-100'">
        <h3 class="font-bold text-lg" :class="isDark ? 'text-white' : 'text-gray-900'">자동 평가 완료 목록</h3>
      </div>
      <div class="flex-1 overflow-y-auto p-3 space-y-3">
        <button
          v-for="prop in proposals"
          :key="prop.id"
          @click="selectedProposalId = prop.id; isEditMode = false;"
          class="w-full text-left p-4 rounded-xl border transition-all duration-200"
          :class="[
            selectedProposalId === prop.id
              ? (isDark ? 'bg-[#8B5CF6]/20 border-[#8B5CF6]' : 'bg-violet-50 border-[#8B5CF6]')
              : (isDark ? 'bg-[#1e1e2d] border-[#37374f] hover:border-slate-500' : 'bg-white border-gray-200 hover:border-gray-300')
          ]"
        >
          <div class="flex justify-between items-start mb-2">
            <span class="font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">{{ prop.partnerName }}</span>
            <span class="text-sm font-bold" :class="isDark ? 'text-[#A78BFA]' : 'text-[#8B5CF6]'">
              {{ calculateFinalScore(prop) }}점
            </span>
          </div>
          <p class="text-xs line-clamp-1 mb-2" :class="isDark ? 'text-slate-400' : 'text-gray-500'">{{ prop.benefitSummary }}</p>
          
          <!-- 경고 표시 (MATCH_015) -->
          <div v-if="prop.warnings.length > 0" class="flex items-center gap-1 mt-2">
            <svg class="w-3.5 h-3.5 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
            <span class="text-[10px] text-rose-500 font-medium">검토 필요 항목 존재</span>
          </div>
        </button>
      </div>
    </div>

    <!-- 우측: 상세 평가 결과 화면 (MATCH_013) -->
    <div v-if="selectedProposal" 
         class="lg:w-2/3 flex flex-col h-full rounded-2xl border transition-colors duration-300 overflow-y-auto"
         :class="isDark ? 'bg-[#262637] border-[#37374f]' : 'bg-white border-gray-100 shadow-sm'">
      
      <div class="p-6 md:p-8">
        <!-- 1. 헤더 영역 -->
        <div class="flex flex-wrap justify-between items-start gap-4 mb-8 pb-6 border-b" :class="isDark ? 'border-[#37374f]' : 'border-gray-100'">
          <div>
            <h2 class="text-2xl font-bold mb-2" :class="isDark ? 'text-white' : 'text-gray-900'">{{ selectedProposal.partnerName }}</h2>
            <p class="text-sm" :class="isDark ? 'text-slate-400' : 'text-gray-500'">{{ selectedProposal.benefitSummary }}</p>
          </div>
          
          <!-- 점수 및 등급 뱃지 (MATCH_009) -->
          <div class="flex flex-col items-end">
            <span class="px-3 py-1 text-sm font-bold rounded-full border mb-2"
                  :class="[getGrade(calculateFinalScore(selectedProposal)).class, isDark ? 'opacity-90' : '']">
              {{ getGrade(calculateFinalScore(selectedProposal)).text }}
            </span>
            <div class="flex items-baseline gap-1">
              <span class="text-3xl font-bold" :class="isDark ? 'text-white' : 'text-gray-900'">{{ calculateFinalScore(selectedProposal) }}</span>
              <span class="text-sm font-medium" :class="isDark ? 'text-slate-400' : 'text-gray-500'">/ 100</span>
            </div>
            <span v-if="selectedProposal.manualScore !== null" class="text-[10px] mt-1 text-[#8B5CF6]">*수동 보정됨</span>
          </div>
        </div>

        <!-- 2. 예외 및 검토 필요 표시 (MATCH_015) -->
        <div v-if="selectedProposal.warnings.length > 0" class="mb-8 space-y-2">
          <div v-for="(warn, idx) in selectedProposal.warnings" :key="idx" 
               class="flex items-start gap-3 p-3.5 rounded-lg border bg-rose-50/50 border-rose-200"
               :class="isDark ? 'bg-rose-900/20 border-rose-800/50' : 'bg-rose-50 border-rose-200'">
            <svg class="w-5 h-5 text-rose-500 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
            <div>
              <p class="text-sm font-bold text-rose-600" :class="isDark ? 'text-rose-400' : ''">{{ warn.type }}</p>
              <p class="text-xs text-rose-500 mt-0.5" :class="isDark ? 'text-rose-300' : ''">{{ warn.message }}</p>
            </div>
          </div>
        </div>

        <!-- 3. 지표별 점수 막대 그래프 (MATCH_013) -->
        <div class="mb-8">
          <h3 class="text-lg font-bold mb-5 flex items-center justify-between" :class="isDark ? 'text-white' : 'text-gray-900'">
            5대 판단 지표 세부 결과
            <button @click="openEditMode" 
                    class="text-xs font-medium px-3 py-1.5 rounded-md transition-colors"
                    :class="isDark ? 'bg-slate-700 text-slate-300 hover:bg-slate-600' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'">
              점수 보정 (MATCH_014)
            </button>
          </h3>
          
          <div class="space-y-4">
            <div v-for="meta in metricsMeta" :key="meta.key" class="flex items-center gap-4">
              <div class="w-28 text-sm font-medium shrink-0" :class="isDark ? 'text-slate-300' : 'text-gray-700'">
                {{ meta.label }}
                <span class="text-[10px] ml-1 opacity-50">{{ meta.weight }}%</span>
              </div>
              <div class="flex-1 h-3 rounded-full overflow-hidden" :class="isDark ? 'bg-slate-700' : 'bg-gray-100'">
                <div class="h-full rounded-full transition-all duration-700"
                     :class="meta.color"
                     :style="{ width: `${selectedProposal.scores[meta.key]}%` }"></div>
              </div>
              <div class="w-8 text-right text-sm font-bold" :class="isDark ? 'text-slate-200' : 'text-gray-900'">
                {{ selectedProposal.scores[meta.key] }}
              </div>
            </div>
          </div>
        </div>

        <!-- 점수 보정 입력 폼 (MATCH_014) -->
        <div v-if="isEditMode" class="mb-8 p-5 rounded-xl border"
             :class="isDark ? 'bg-[#1e1e2d] border-[#8B5CF6]/50' : 'bg-violet-50 border-[#8B5CF6]/50'">
          <h4 class="text-sm font-bold mb-3" :class="isDark ? 'text-[#A78BFA]' : 'text-[#8B5CF6]'">시스템 종합 점수 수동 보정</h4>
          <div class="flex gap-4 items-start">
            <div class="w-24">
              <label class="block text-xs mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">보정 점수</label>
              <input type="number" v-model="editForm.score" max="100" min="0" step="0.1"
                     class="w-full rounded-md border p-2 text-sm focus:outline-none focus:border-[#8B5CF6]"
                     :class="isDark ? 'bg-[#262637] border-[#37374f] text-white' : 'bg-white border-gray-300'">
            </div>
            <div class="flex-1">
              <label class="block text-xs mb-1" :class="isDark ? 'text-slate-400' : 'text-gray-500'">보정 사유 (필수)</label>
              <input type="text" v-model="editForm.reason" placeholder="점수를 변경하는 구체적인 사유를 입력하세요."
                     class="w-full rounded-md border p-2 text-sm focus:outline-none focus:border-[#8B5CF6]"
                     :class="isDark ? 'bg-[#262637] border-[#37374f] text-white' : 'bg-white border-gray-300'">
            </div>
            <div class="flex items-end h-[58px]">
               <button @click="saveCorrection" class="bg-[#8B5CF6] text-white px-4 py-2 rounded-md text-sm font-bold hover:bg-violet-600 transition-colors">
                 저장
               </button>
            </div>
          </div>
        </div>

        <!-- 4. 최종 추천 사유 (MATCH_013) -->
        <div class="p-5 rounded-xl border" :class="isDark ? 'bg-[#1e1e2d] border-[#37374f]' : 'bg-gray-50 border-gray-200'">
          <h4 class="text-sm font-bold mb-2 flex items-center gap-2" :class="isDark ? 'text-white' : 'text-gray-900'">
            <svg class="w-4 h-4 text-[#8B5CF6]" fill="currentColor" viewBox="0 0 20 20"><path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/></svg>
            AI 최종 추천 사유
          </h4>
          <p class="text-sm leading-relaxed" :class="isDark ? 'text-slate-300' : 'text-gray-700'">
            {{ selectedProposal.recommendationReason }}
          </p>
        </div>

        <!-- 점수 보정 이력 표시 (MATCH_014) -->
        <div v-if="selectedProposal.correctionHistory.length > 0" class="mt-6 pt-6 border-t" :class="isDark ? 'border-[#37374f]' : 'border-gray-100'">
           <h4 class="text-xs font-bold mb-3" :class="isDark ? 'text-slate-400' : 'text-gray-500'">점수 보정 이력</h4>
           <ul class="space-y-2">
             <li v-for="(history, idx) in selectedProposal.correctionHistory" :key="idx"
                 class="text-[11px] flex items-center gap-2" :class="isDark ? 'text-slate-400' : 'text-gray-500'">
               <span class="bg-gray-200 dark:bg-slate-700 text-gray-700 dark:text-slate-300 px-1.5 py-0.5 rounded">{{ history.date }}</span>
               <span>{{ history.user }}님이 <span class="line-through">{{ history.before }}점</span>에서 <strong>{{ history.after }}점</strong>으로 변경:</span>
               <span class="italic text-gray-600 dark:text-slate-300">"{{ history.reason }}"</span>
             </li>
           </ul>
        </div>

      </div>
    </div>
  </div>
</template>