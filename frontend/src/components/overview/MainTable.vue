<script setup>
import { ref, computed, watch } from 'vue';
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

// 다크모드 여부 계산
const isDark = computed(() => store.theme === 'dark');

const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})

// props 데이터 동기화
const events = ref(props.eventsData);
watch(() => props.eventsData, (newVal) => {
  events.value = newVal;
});
</script>

<template>
  <div class="w-full transition-colors duration-300 rounded-xl border overflow-hidden shadow-sm"
       :class="isDark ? 'bg-[#1e1e2d] border-[#2d2d3f]' : 'bg-white border-gray-200'">
    
    <table class="min-w-full divide-y transition-colors duration-300"
           :class="isDark ? 'divide-[#2d2d3f]' : 'divide-gray-200'">
      
      <thead class="transition-colors duration-300 border-t-2 border-[#8B5CF6]"
             :class="isDark ? 'bg-[#2d2d3f]' : 'bg-gray-50'">
        <tr>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold transition-colors"
              :class="isDark ? 'text-slate-300' : 'text-gray-700'">
            PM사명
          </th>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold transition-colors"
              :class="isDark ? 'text-slate-300' : 'text-gray-700'">
            캠페인명
          </th>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold transition-colors"
              :class="isDark ? 'text-slate-300' : 'text-gray-700'">
            시작일
          </th>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold transition-colors"
              :class="isDark ? 'text-slate-300' : 'text-gray-700'">
            종료일
          </th>
        </tr>
      </thead>
      
      <tbody class="divide-y transition-colors duration-300"
             :class="isDark ? 'divide-[#2d2d3f] bg-[#1e1e2d]' : 'divide-gray-100 bg-white'">
        
        <tr v-for="event in events" :key="event.id" 
            class="transition-colors duration-150 group"
            :class="isDark ? 'hover:bg-[#2d2d3f]/50' : 'hover:bg-gray-50 hover:text-[#8B5CF6]'">
          
          <td class="px-6 py-4 whitespace-nowrap text-sm font-medium transition-colors"
              :class="isDark ? 'text-slate-200 group-hover:text-[#A78BFA]' : 'text-gray-900 group-hover:text-[#8B5CF6]'">
            {{ event.projectManager }}
          </td>
          
          <td class="px-6 py-4 whitespace-nowrap text-sm font-medium transition-colors"
              :class="isDark ? 'text-slate-200 group-hover:text-[#A78BFA]' : 'text-gray-900 group-hover:text-[#8B5CF6]'">
            {{ event.title }}
          </td>
          
          <td class="px-6 py-4 whitespace-nowrap text-sm transition-colors"
              :class="isDark ? 'text-slate-400' : 'text-gray-600'">
            {{ event.start }}
          </td>
          
          <td class="px-6 py-4 whitespace-nowrap text-sm transition-colors"
              :class="isDark ? 'text-slate-400' : 'text-gray-600'">
            {{ event.end }}
          </td>
        </tr>

        <tr v-if="events.length === 0">
          <td colspan="4" class="px-6 py-16 text-center text-sm transition-colors"
              :class="isDark ? 'text-slate-500' : 'text-gray-400'">
            등록된 공정 일정이 없습니다.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
/* 부드러운 색상 전환을 위한 유틸리티 */
.transition-colors {
  transition-property: background-color, border-color, color;
}
</style>