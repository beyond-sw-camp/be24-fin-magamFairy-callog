import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePartnershipsStore = defineStore('partnerships', () => {
  // 백엔드 API 연결 전까지 빈 상태로 시작
  const programs = ref([])
  const recruitingMilestones = ref([])
  const operationsMilestones = ref([])

  const selectedProgramId = ref(null)

  function selectProgram(programId) {
    selectedProgramId.value = programId
  }

  const selectedProgram = () =>
    programs.value.find((p) => p.id === selectedProgramId.value) ?? null

  return {
    programs,
    recruitingMilestones,
    operationsMilestones,
    selectedProgramId,
    selectProgram,
    selectedProgram,
  }
})
