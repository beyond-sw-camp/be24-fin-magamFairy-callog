<script setup>
import { ref, computed } from 'vue';

const events = ref([])
const props = defineProps({
  eventsData: {
    type: Array,
    required: true,
    default: () => []
  }
})
events.value = props.eventsData;

</script>

<template>
  <div class="w-full bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
    <table class="min-w-full divide-y divide-gray-200">
      <thead class="bg-gray-50 border-t-2 border-[#8B5CF6]">
        <tr>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold text-gray-700">
            PM사명
          </th>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold text-gray-700">
            캠페인명
          </th>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold text-gray-700">
            시작일
          </th>
          <th scope="col" class="px-6 py-4 text-left text-sm font-bold text-gray-700">
            종료일
          </th>
        </tr>
      </thead>
      
      <tbody class="divide-y divide-gray-100 bg-white">
        <tr v-for="event in events" :key="event.id" class="hover:bg-gray-50 hover:text-[#8B5CF6] transition-colors duration-150 group">
          <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 group-hover:text-[#8B5CF6]">
            {{ event.projectManager }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 group-hover:text-[#8B5CF6]">
            {{ event.title }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
            {{ event.start }}
          </td>
          <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
            {{ event.end }}
          </td>
        </tr>

        <tr v-if="events.length === 0">
          <td colspan="4" class="px-6 py-16 text-center text-gray-400 text-sm">
            등록된 공정 일정이 없습니다.
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
