import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePartnershipsStore = defineStore('partnerships', () => {
  const programs = ref([
    {
      id: 'prg-001',
      name: '한화갤러리아 VIP 라운지 제휴',
      stage: 'operating',
      owner: '한화갤러리아',
      startDate: '2026-04-01',
      endDate: '2026-06-30',
    },
    {
      id: 'prg-002',
      name: '한화호텔 리조트 패키지 제휴',
      stage: 'accepted',
      owner: '한화호텔앤드리조트',
      startDate: '2026-05-01',
      endDate: '2026-07-31',
    },
    {
      id: 'prg-003',
      name: '대행사A 콘텐츠 파트너십',
      stage: 'recruiting',
      owner: '대행사A',
      startDate: null,
      endDate: null,
    },
  ])

  const recruitingMilestones = ref([
    {
      id: 'rfp-m1',
      programId: 'prg-003',
      title: '대행사A 제안서 접수 마감',
      date: '2026-05-15',
      status: 'due_soon',
    },
    {
      id: 'rfp-m2',
      programId: 'prg-003',
      title: '파트너십 심사 결과 발표',
      date: '2026-05-22',
      status: 'upcoming',
    },
  ])

  const operationsMilestones = ref([
    {
      id: 'ops-m1',
      programId: 'prg-001',
      title: '갤러리아 VIP 라운지 1차 운영 검토',
      date: '2026-05-20',
      status: 'upcoming',
    },
    {
      id: 'ops-m2',
      programId: 'prg-002',
      title: '리조트 패키지 온라인 오픈',
      date: '2026-05-25',
      status: 'upcoming',
    },
  ])

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
