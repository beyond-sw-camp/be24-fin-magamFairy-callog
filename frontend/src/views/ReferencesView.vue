<script setup>
import { ref, computed, onMounted } from 'vue'

// 실제 백엔드 연동을 위한 API import (경로에 맞게 수정하여 사용하시면 됩니다)
import { 
  getReferences, 
  getReferenceById, 
  createReference, 
  updateReference, 
  deleteReference 
} from '@/api/references/index.js'

// --- [테스트용 더미 데이터 세팅 (5개)] ---
const initialReferences = [
  {
    id: 1,
    type: 'image',
    title: 'A고객사 가을 프로모션 배너',
    description: '따뜻한 색감을 강조한 가을 시즌 배너 레퍼런스입니다. 텍스트 배치와 여백을 참고하기 좋습니다.',
    tags: ['A고객사', '광고', '배너'],
    url: 'https://images.unsplash.com/photo-1605814423851-dfcf85ce5eab?auto=format&fit=crop&q=80&w=400',
    date: '2026-04-10',
  },
  {
    id: 2,
    type: 'link',
    title: 'B고객사 공식 브랜드 사이트',
    description: '미니멀한 디자인과 부드러운 스크롤 인터랙션이 돋보이는 랜딩페이지 참고용입니다.',
    tags: ['B고객사', '웹사이트', '미니멀'],
    url: 'https://images.unsplash.com/photo-1499951360447-b19be8fe80f5?auto=format&fit=crop&q=80&w=400',
    date: '2026-04-12',
  },
  {
    id: 3,
    type: 'video',
    title: 'C고객사 신제품 바이럴 영상',
    description: 'SNS 숏폼 형태의 빠른 템포 편집과 트렌디한 자막 스타일링 레퍼런스',
    tags: ['C고객사', '동영상', '숏폼'],
    url: 'https://example.com/video1',
    thumbnail: 'https://images.unsplash.com/photo-1611162617474-5b21e879e113?auto=format&fit=crop&q=80&w=400',
    date: '2026-04-15',
  },
  {
    id: 4,
    type: 'image',
    title: 'A고객사 SNS 썸네일 템플릿',
    description: '인스타그램 피드용으로 제작된 통일감 있는 템플릿 세트입니다.',
    tags: ['A고객사', 'SNS', '템플릿'],
    url: 'https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?auto=format&fit=crop&q=80&w=400',
    date: '2026-04-16',
  },
  {
    id: 5,
    type: 'link',
    title: '성공적인 블로그 콘텐츠 구조 분석',
    description: '고객 유입을 300% 늘렸던 타사 블로그 포스트의 글쓰기 구조 분석',
    tags: ['블로그', '콘텐츠 구조', '성공사례'],
    url: 'https://images.unsplash.com/photo-1432821596592-e2c18b78144f?auto=format&fit=crop&q=80&w=400',
    date: '2026-04-18',
  },
]

// --- [필터 분류 데이터] ---
const clientTags = ['A고객사', 'B고객사', 'C고객사']
const typeTags = ['광고', '배너', '웹사이트', '미니멀', '동영상', '숏폼', 'SNS', '템플릿', '블로그', '콘텐츠 구조', '성공사례']

// --- [상태 관리] ---
const references = ref([...initialReferences]) // 초기값을 더미데이터로 둬서 깜빡임 방지
const activeZone = ref('all') // all, link, image, video
const searchQuery = ref('')

// 선택된 태그 상태
const selectedClients = ref([])
const selectedTypes = ref([])

// 드롭다운 필터 토글 상태
const showClientFilters = ref(false)
const showTypeFilters = ref(false)

// 모달 상태 (등록, 상세, 수정)
const modalState = ref({ isOpen: false, mode: 'create', data: null })
const formData = ref({ type: 'link', title: '', url: '', description: '', tags: [] })
const tagInput = ref('')

// --- [API 연동: 데이터 불러오기] ---
const fetchReferencesData = async () => {
  try {
    const response = await getReferences()
    if (response && response.data) {
      let apiList = response.data
      
      // 백엔드 응답이 { data: [...] } 형태로 래핑되어 있을 경우를 위한 안전 추출
      if (apiList && typeof apiList === 'object' && !Array.isArray(apiList)) {
        apiList = apiList.data || apiList.content || apiList.result || []
      }
      
      if (Array.isArray(apiList) && apiList.length > 0) {
        references.value = apiList
      }
    }
  } catch (error) {
    console.warn('목록 API 연동 실패 (백엔드 미연결). 임시 테스트 데이터를 유지합니다.', error)
    if (references.value.length === 0) {
      references.value = [...initialReferences]
    }
  }
}

onMounted(() => {
  fetchReferencesData()
})

// --- [Computed 속성] ---
const filteredReferences = computed(() => {
  let result = references.value

  if (activeZone.value !== 'all') {
    result = result.filter((item) => item.type === activeZone.value)
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(
      (item) =>
        item.title.toLowerCase().includes(query) ||
        item.description?.toLowerCase().includes(query) ||
        item.tags?.some((tag) => tag.toLowerCase().includes(query)),
    )
  }

  // 고객사 필터
  if (selectedClients.value.length > 0) {
    result = result.filter((item) => selectedClients.value.some((tag) => item.tags?.includes(tag)))
  }

  // 콘텐츠 유형 필터
  if (selectedTypes.value.length > 0) {
    result = result.filter((item) => selectedTypes.value.some((tag) => item.tags?.includes(tag)))
  }

  return result.sort((a, b) => new Date(b.date) - new Date(a.date))
})

const isViewMode = computed(() => modalState.value.mode === 'view')

// --- [메서드: 필터 및 태그 관리] ---
const getTagCount = (tag) => {
  return references.value.filter(item => item.tags?.includes(tag)).length
}

const toggleClientFilterPopup = () => {
  showClientFilters.value = !showClientFilters.value
  showTypeFilters.value = false
}

const toggleTypeFilterPopup = () => {
  showTypeFilters.value = !showTypeFilters.value
  showClientFilters.value = false
}

const closeAllFilterPopups = () => {
  showClientFilters.value = false
  showTypeFilters.value = false
}

const toggleClientTag = (tag) => {
  if (selectedClients.value.includes(tag)) {
    selectedClients.value = selectedClients.value.filter((t) => t !== tag)
  } else {
    selectedClients.value.push(tag)
  }
}

const toggleTypeTag = (tag) => {
  if (selectedTypes.value.includes(tag)) {
    selectedTypes.value = selectedTypes.value.filter((t) => t !== tag)
  } else {
    selectedTypes.value.push(tag)
  }
}

const toggleTagFromCard = (tag) => {
  if (clientTags.includes(tag)) toggleClientTag(tag)
  else if (typeTags.includes(tag)) toggleTypeTag(tag)
}

const clearAllFilters = () => {
  selectedClients.value = []
  selectedTypes.value = []
}

// --- [메서드: 모달 제어 및 단일 상세 조회 API 연동 버그 수정] ---
const openModal = async (mode, item = null) => {
  tagInput.value = ''
  
  if (mode === 'create') {
    formData.value = { type: 'link', title: '', url: '', description: '', tags: item?.tags || [] }
    if (item?.title) formData.value.title = item.title
    modalState.value = { isOpen: true, mode, data: null }
  } else if (item) {
    let viewData = JSON.parse(JSON.stringify(item))

    if (mode === 'view') {
      try {
        const response = await getReferenceById(item.id)
        if (response && response.data) {
          let apiData = response.data
          
          // 백엔드 응답 구조가 { data: { id: ... } } 형태로 래핑되어 있을 경우 안전하게 추출
          if (apiData && typeof apiData === 'object' && !Array.isArray(apiData)) {
            if ('data' in apiData) apiData = apiData.data
            else if ('result' in apiData) apiData = apiData.result
          }
          
          // 기존 목록의 데이터에 API로 가져온 새 데이터를 병합하되, 
          // id가 undefined로 덮어씌워지는 것을 방지하기 위해 item.id를 명시적으로 보장합니다.
          viewData = { ...viewData, ...apiData, id: item.id } 
        }
      } catch (error) {
        console.warn(`상세 조회 API (id: ${item.id}) 실패. 임시로 로컬 목록 데이터를 사용합니다.`, error)
      }
    } 
    
    // tags 배열 방어 로직 (데이터가 없어서 undefined가 되는 경우 방지)
    if (!Array.isArray(viewData.tags)) viewData.tags = []

    formData.value = JSON.parse(JSON.stringify(viewData))
    modalState.value = { isOpen: true, mode, data: viewData }
  }
}

const closeModal = () => {
  modalState.value.isOpen = false
  setTimeout(() => {
    modalState.value = { isOpen: false, mode: 'create', data: null }
  }, 200)
}

const switchModeToEdit = () => {
  modalState.value.mode = 'edit'
}

// --- [API 연동: 저장/삭제 로직] ---
const handleSave = async () => {
  if (!formData.value.title || !formData.value.url) return

  const saveData = {
    ...formData.value,
    date: formData.value.date || new Date().toISOString().split('T')[0],
  }

  try {
    if (modalState.value.mode === 'create') {
      await createReference(saveData)
    } else {
      if (!saveData.id) throw new Error("ID 값이 유실되었습니다.");
      await updateReference(saveData.id, saveData)
    }
    // 성공 시 목록 새로고침
    await fetchReferencesData()
    closeModal()
  } catch (error) {
    console.warn('저장 API 실패. 로컬 테스트 데이터에 임시 반영합니다.', error)
    // 백엔드 미연결 시 화면 동작 테스트를 위한 Fallback
    if (modalState.value.mode === 'create') {
      saveData.id = Date.now()
      references.value.unshift(saveData)
    } else {
      const index = references.value.findIndex((r) => r.id === formData.value.id)
      if (index !== -1) references.value[index] = { ...formData.value }
    }
    closeModal()
  }
}

const handleDelete = async (id) => {
  if (!id) {
    alert('유효하지 않은 데이터입니다.');
    return;
  }
  
  if (confirm('이 레퍼런스를 삭제하시겠습니까?')) {
    try {
      await deleteReference(id)
      await fetchReferencesData()
      if (modalState.value.data?.id === id) closeModal()
    } catch (error) {
      console.warn('삭제 API 실패. 로컬 테스트 데이터에서 임시 삭제합니다.', error)
      // 백엔드 미연결 시 화면 동작 테스트를 위한 Fallback
      references.value = references.value.filter((ref) => ref.id !== id)
      if (modalState.value.data?.id === id) closeModal()
    }
  }
}

// 템플릿 작성으로 이동하는 로직
const goToTemplate = (refData) => {
  console.log(`'${refData.title}' 레퍼런스를 바탕으로 템플릿 작성을 시작합니다.`)
  // 실제 구현에서는 router.push 등 라우팅 로직 추가
  if (modalState.value.isOpen) {
    closeModal()
  }
}

// 폼 태그 조작
const handleTagAdd = (e) => {
  e.preventDefault()
  const val = tagInput.value.trim()
  if (val && !formData.value.tags.includes(val)) {
    formData.value.tags.push(val)
  }
  tagInput.value = ''
}
const removeFormTag = (tagToRemove) => {
  formData.value.tags = formData.value.tags.filter((t) => t !== tagToRemove)
}
</script>

<template>
  <section class="flex flex-col h-screen font-sans text-slate-800 bg-slate-50/50 relative overflow-hidden">
    
    <!-- Main Content -->
    <main class="flex-1 overflow-y-auto p-8 pb-12 transition-all duration-500 custom-scrollbar relative z-10">
      <div class="max-w-[1400px] mx-auto space-y-6">

        <!-- 상단 컨트롤 및 필터 영역 -->
        <article class="bg-white border border-slate-200/80 rounded-3xl p-6 shadow-sm shrink-0">
          
          <!-- 1열: 탭 네비게이션 & 검색/등록 버튼 -->
          <div class="flex flex-col xl:flex-row xl:items-center justify-between gap-4">
            
            <!-- 탭 네비게이션 (Pill Style) -->
            <div class="flex p-1.5 bg-slate-100/80 rounded-xl border border-slate-200/60 gap-1 w-fit shrink-0">
              <button
                v-for="tab in [
                  { id: 'all', label: '전체보기' },
                  { id: 'link', label: '링크' },
                  { id: 'image', label: '이미지' },
                  { id: 'video', label: '동영상' },
                ]"
                :key="tab.id"
                @click="activeZone = tab.id"
                class="px-5 py-2 rounded-lg text-[14px] font-extrabold transition-all flex items-center gap-2"
                :class="
                  activeZone === tab.id
                    ? 'bg-white text-slate-900 shadow-sm ring-1 ring-black/5'
                    : 'text-slate-500 hover:text-slate-700 hover:bg-slate-200/50'
                "
              >
                <svg v-if="tab.id === 'link'" style="width: 16px; height: 16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" /></svg>
                <svg v-else-if="tab.id === 'image'" style="width: 16px; height: 16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" /></svg>
                <svg v-else-if="tab.id === 'video'" style="width: 16px; height: 16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="23 7 16 12 23 17 23 7" /><rect x="1" y="5" width="15" height="14" rx="2" ry="2" /></svg>
                {{ tab.label }}
              </button>
            </div>

            <!-- 검색바 & 새 레퍼런스 버튼 -->
            <div class="flex flex-col sm:flex-row items-center gap-3 w-full xl:w-auto">
              <!-- 검색바 -->
              <div class="relative w-full sm:w-80">
                <svg style="width: 18px; height: 18px" class="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="11" cy="11" r="8" />
                  <path d="m21 21-4.3-4.3" />
                </svg>
                <input
                  type="text"
                  v-model="searchQuery"
                  placeholder="레퍼런스 검색 (제목, 설명, 태그)"
                  class="w-full bg-slate-50 border border-slate-200/80 shadow-sm rounded-xl py-2.5 pl-11 pr-4 text-[14px] focus:bg-white focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/10 outline-none transition-all placeholder:text-slate-400"
                />
                <button
                  v-if="searchQuery"
                  @click="searchQuery = ''"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 bg-slate-200/50 hover:bg-slate-200 p-1 rounded-full transition-colors"
                >
                  <svg style="width: 14px; height: 14px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>

              <!-- 새 레퍼런스 버튼 -->
              <button
                @click="openModal('create')"
                class="w-full sm:w-auto flex items-center justify-center gap-2 bg-slate-900 hover:bg-slate-800 text-white px-6 py-2.5 rounded-xl text-[14px] font-bold shadow-md shadow-slate-900/10 transition-all hover:-translate-y-0.5 whitespace-nowrap"
              >
                <svg style="width: 18px; height: 18px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                새 레퍼런스
              </button>
            </div>
          </div>

          <!-- 2열: 미니멀 드롭다운 필터 영역 -->
          <div class="flex items-center justify-between gap-3 relative z-20 w-full flex-wrap border-t border-slate-100 mt-5 pt-5">
            <!-- 백그라운드 클릭 시 드롭다운 닫기용 오버레이 -->
            <div v-if="showClientFilters || showTypeFilters" @click="closeAllFilterPopups" class="fixed inset-0 z-30"></div>

            <!-- 선택된 태그 목록 (Pills) -->
            <div class="flex flex-wrap gap-2 relative z-40 order-last xl:order-first w-full xl:w-auto xl:justify-end pt-2 xl:pt-0">
              <span v-for="tag in [...selectedClients, ...selectedTypes]" :key="'sel-'+tag"
                class="flex items-center gap-1 bg-emerald-500 border border-emerald-500 text-white px-3 py-1.5 rounded-lg text-[12px] font-extrabold shadow-sm"
              >
                {{ tag }}
                <button @click="toggleTagFromCard(tag)" class="text-white/70 hover:text-white transition-colors ml-1">
                  <svg style="width: 14px; height: 14px;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </span>
              <button v-if="selectedClients.length > 0 || selectedTypes.length > 0"
                @click="clearAllFilters"
                class="text-[13px] text-slate-400 hover:text-slate-700 font-bold px-2 py-1.5 transition-colors whitespace-nowrap underline underline-offset-4"
              >
                초기화
              </button>
            </div>

            <!-- 구분선 -->
            <div v-if="selectedClients.length > 0 || selectedTypes.length > 0" class="hidden xl:block w-[1px] h-5 bg-slate-300 mx-1 order-2"></div>

            <!-- 드롭다운 버튼 그룹 -->
            <div class="flex gap-2.5 relative z-40 order-1 xl:order-last ml-auto xl:ml-0">
              
              <!-- 1. 고객사 필터 드롭다운 -->
              <div class="relative">
                <button @click="toggleClientFilterPopup"
                  class="flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-300 rounded-[10px] hover:bg-slate-50 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
                >
                  <svg style="width: 14px; height: 14px;" class="text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
                  <span class="text-[13px] font-bold text-slate-700">고객사 필터</span>
                  <span v-if="selectedClients.length > 0" class="bg-emerald-100 text-emerald-700 text-[10px] font-extrabold px-1.5 py-0.5 rounded-md ml-0.5">{{ selectedClients.length }}</span>
                  <svg :class="showClientFilters ? 'rotate-180' : ''" style="width: 12px; height: 12px;" class="text-slate-400 transition-transform duration-200 ml-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                </button>

                <transition enter-active-class="transition ease-out duration-150" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-100" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
                  <div v-if="showClientFilters" class="absolute top-full right-0 mt-2 w-72 bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200/80 p-4">
                    <h4 class="text-[11px] font-extrabold text-slate-400 mb-3 uppercase tracking-wider text-left">고객사 선택</h4>
                    <div class="flex flex-wrap gap-2">
                      <button v-for="tag in clientTags" :key="tag"
                        @click="toggleClientTag(tag)"
                        class="px-3.5 py-1.5 rounded-[10px] text-[13px] font-bold transition-all flex items-center gap-1.5 border-2 border-slate-200"
                        :class="selectedClients.includes(tag) ? 'bg-emerald-500 border-slate-900 text-white shadow-[0px_2px_0px_rgba(15,23,42,1)] translate-y-[-1px]' : 'bg-white text-slate-600 hover:bg-slate-50 hover:border-slate-300'"
                      >
                        {{ tag }}
                        <span class="text-[11px] font-medium" :class="selectedClients.includes(tag) ? 'text-white/90' : 'text-slate-400'">{{ getTagCount(tag) }}</span>
                      </button>
                    </div>
                  </div>
                </transition>
              </div>

              <!-- 2. 콘텐츠 필터 드롭다운 -->
              <div class="relative">
                <button @click="toggleTypeFilterPopup"
                  class="flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-300 rounded-[10px] hover:bg-slate-50 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
                >
                  <svg style="width: 14px; height: 14px;" class="text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
                  <span class="text-[13px] font-bold text-slate-700">콘텐츠 필터</span>
                  <span v-if="selectedTypes.length > 0" class="bg-emerald-100 text-emerald-700 text-[10px] font-extrabold px-1.5 py-0.5 rounded-md ml-0.5">{{ selectedTypes.length }}</span>
                  <svg :class="showTypeFilters ? 'rotate-180' : ''" style="width: 12px; height: 12px;" class="text-slate-400 transition-transform duration-200 ml-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                </button>

                <transition enter-active-class="transition ease-out duration-150" enter-from-class="transform opacity-0 scale-95" enter-to-class="transform opacity-100 scale-100" leave-active-class="transition ease-in duration-100" leave-from-class="transform opacity-100 scale-100" leave-to-class="transform opacity-0 scale-95">
                  <div v-if="showTypeFilters" class="absolute top-full right-0 mt-2 w-[340px] bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200/80 p-4">
                    <h4 class="text-[11px] font-extrabold text-slate-400 mb-3 uppercase tracking-wider text-left">콘텐츠 유형 선택</h4>
                    <div class="flex flex-wrap gap-2">
                      <button v-for="tag in typeTags" :key="tag"
                        @click="toggleTypeTag(tag)"
                        class="px-3.5 py-1.5 rounded-[10px] text-[13px] font-bold transition-all flex items-center gap-1.5 border-2 border-slate-200"
                        :class="selectedTypes.includes(tag) ? 'bg-emerald-500 border-slate-900 text-white shadow-[0px_2px_0px_rgba(15,23,42,1)] translate-y-[-1px]' : 'bg-white text-slate-600 hover:bg-slate-50 hover:border-slate-300'"
                      >
                        {{ tag }}
                        <span class="text-[11px] font-medium" :class="selectedTypes.includes(tag) ? 'text-white/90' : 'text-slate-400'">{{ getTagCount(tag) }}</span>
                      </button>
                    </div>
                  </div>
                </transition>
              </div>

            </div>
          </div>
        </article>

        <!-- 리스트 렌더링 (Grid View) -->
        <div
          v-if="filteredReferences.length > 0"
          class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-6"
        >
          <div
            v-for="item in filteredReferences"
            :key="item.id"
            class="group flex flex-col bg-white rounded-2xl border border-slate-200/80 overflow-hidden transition-all duration-300 hover:-translate-y-1.5 hover:shadow-xl hover:shadow-emerald-900/5 hover:border-emerald-300 cursor-pointer"
            @click="openModal('view', item)"
          >
            <!-- 썸네일 영역 (16:9 비율 고정) -->
            <div class="w-full aspect-[4/3] relative bg-slate-100 flex items-center justify-center overflow-hidden border-b border-slate-100">
              <template v-if="item.type === 'image' || item.type === 'video' || item.type === 'link'">
                <img
                  :src="item.thumbnail || item.url"
                  alt=""
                  class="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  loading="lazy"
                />
                <div
                  v-if="item.type === 'video'"
                  class="absolute inset-0 bg-slate-900/30 flex items-center justify-center opacity-90 group-hover:opacity-100 transition-opacity"
                >
                  <div class="bg-white/95 p-3 rounded-full shadow-lg backdrop-blur-sm group-hover:scale-110 transition-transform">
                    <svg style="width: 24px; height: 24px" class="text-slate-900 ml-1" viewBox="0 0 24 24" fill="currentColor">
                      <polygon points="5 3 19 12 5 21 5 3" />
                    </svg>
                  </div>
                </div>
              </template>
              <div
                v-else
                class="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 text-slate-300"
              >
                <svg style="width: 48px; height: 48px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                  <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                </svg>
              </div>

              <!-- 배지 -->
              <div class="absolute top-3 left-3 flex items-center gap-1 z-10">
                <span class="rounded-lg bg-white/95 backdrop-blur px-2.5 py-1.5 text-[11px] font-extrabold text-slate-800 shadow-sm flex items-center gap-1.5 uppercase tracking-wider">
                  <svg v-if="item.type === 'link'" style="width: 14px; height: 14px" class="text-blue-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" /></svg>
                  <svg v-else-if="item.type === 'image'" style="width: 14px; height: 14px" class="text-emerald-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" /></svg>
                  <svg v-else-if="item.type === 'video'" style="width: 14px; height: 14px" class="text-rose-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="23 7 16 12 23 17 23 7" /><rect x="1" y="5" width="15" height="14" rx="2" ry="2" /></svg>
                  {{ item.type }}
                </span>
              </div>
            </div>

            <!-- 콘텐츠 영역 -->
            <div class="p-5 flex flex-col flex-1 relative">
              <h4 class="text-[16px] font-extrabold text-slate-800 line-clamp-2 mb-2 group-hover:text-emerald-600 transition-colors leading-snug">
                {{ item.title }}
              </h4>
              <p class="text-[13px] text-slate-500 line-clamp-2 mb-4 flex-1 leading-relaxed">
                {{ item.description }}
              </p>
              
              <!-- 카드 하단 태그 및 템플릿 이동 버튼 -->
              <div class="flex items-center justify-between mt-auto border-t border-slate-100 pt-4">
                <!-- 태그 목록 -->
                <div class="flex flex-wrap gap-1.5 flex-1 pr-2">
                  <span
                    v-for="tag in (item.tags || []).slice(0, 3)"
                    :key="tag"
                    @click.stop="toggleTagFromCard(tag)"
                    class="rounded-lg bg-slate-100 border border-slate-200/50 px-2 py-1 text-[11px] text-slate-600 font-bold hover:bg-slate-200 hover:text-slate-800 transition-colors"
                    :class="(clientTags.includes(tag) && selectedClients.includes(tag)) || (typeTags.includes(tag) && selectedTypes.includes(tag)) ? '!bg-emerald-500 !text-white' : ''"
                    >#{{ tag }}</span
                  >
                  <span v-if="(item.tags || []).length > 3" class="text-xs text-slate-400 font-bold px-1 py-1">+{(item.tags || []).length - 3}</span>
                </div>
                
                <!-- 템플릿 작성 버튼 (카드 우측 하단 퀵 액션) -->
                <button
                  @click.stop="goToTemplate(item)"
                  class="flex-shrink-0 bg-white border border-slate-200 text-slate-400 hover:text-emerald-600 hover:border-emerald-300 hover:bg-emerald-50 p-2 rounded-xl transition-all shadow-sm group/btn"
                  title="이 레퍼런스로 템플릿 작성"
                >
                  <svg style="width: 16px; height: 16px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
        
        <div v-else class="flex flex-col items-center justify-center h-full text-center p-12 border-2 border-dashed border-slate-200 rounded-3xl opacity-80 min-h-[400px]">
          <svg style="width: 48px; height: 48px;" class="text-slate-400 mb-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <rect width="18" height="18" x="3" y="3" rx="2" ry="2" /><line x1="9" y1="3" x2="9" y2="21" />
          </svg>
          <p class="text-[16px] font-bold text-slate-600 mb-1">검색 결과가 없습니다</p>
          <p class="text-[14px] text-slate-500">다른 키워드나 필터 조건을 시도해 보세요.</p>
        </div>

      </div>
    </main>

    <!-- Global Modal (등록, 수정, 조회) -->
    <transition
      enter-active-class="transition-opacity duration-300"
      leave-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div
        v-if="modalState.isOpen"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
      >
        <!-- 배경 -->
        <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="closeModal"></div>

        <!-- 모달 창 -->
        <div
          class="relative bg-white rounded-3xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col transform transition-transform animate-in zoom-in-95 overflow-hidden"
        >
          <!-- 헤더 -->
          <div
            class="px-8 py-6 border-b border-slate-100 flex justify-between items-center bg-white shrink-0 z-10"
          >
            <h3 class="text-xl font-extrabold text-slate-800 flex items-center gap-3">
              <div
                class="p-2 rounded-xl"
                :class="
                  modalState.mode === 'create'
                    ? 'bg-emerald-50'
                    : modalState.mode === 'edit'
                      ? 'bg-yellow-50'
                      : 'bg-slate-50'
                "
              >
                <svg
                  v-if="modalState.mode === 'create'"
                  style="width: 20px; height: 20px"
                  class="text-emerald-500"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                <svg
                  v-else-if="modalState.mode === 'edit'"
                  style="width: 20px; height: 20px"
                  class="text-yellow-500"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                </svg>
                <svg
                  v-else
                  style="width: 20px; height: 20px"
                  class="text-slate-500"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <line x1="9" y1="3" x2="9" y2="21" />
                </svg>
              </div>
              {{
                modalState.mode === 'create'
                  ? '새 레퍼런스 등록'
                  : modalState.mode === 'edit'
                    ? '레퍼런스 수정'
                    : '레퍼런스 상세'
              }}
            </h3>
            <div class="flex items-center gap-2">
              <template v-if="isViewMode">
                <button
                  @click="switchModeToEdit"
                  class="p-2.5 text-slate-400 hover:text-emerald-600 bg-white hover:bg-emerald-50 border border-slate-200 rounded-xl transition"
                  title="수정"
                >
                  <svg
                    style="width: 18px; height: 18px"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z" />
                  </svg>
                </button>
                <button
                  @click="handleDelete(modalState.data.id)"
                  class="p-2.5 text-slate-400 hover:text-rose-600 bg-white hover:bg-rose-50 border border-slate-200 rounded-xl transition mr-2"
                  title="삭제"
                >
                  <svg
                    style="width: 18px; height: 18px"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <polyline points="3 6 5 6 21 6" />
                    <path
                      d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
                    />
                  </svg>
                </button>
              </template>
              <button
                @click="closeModal"
                class="p-2.5 text-slate-400 hover:text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition"
              >
                <svg
                  style="width: 20px; height: 20px"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M18 6 6 18" />
                  <path d="m6 6 12 12" />
                </svg>
              </button>
            </div>
          </div>

          <!-- Body (내용) -->
          <div class="flex-1 overflow-y-auto p-8 bg-slate-50/50 custom-scrollbar">
            <!-- View 모드 -->
            <div v-if="isViewMode" class="space-y-8">
              <div>
                <div class="flex items-center gap-3 mb-3">
                  <span
                    class="text-[11px] font-extrabold text-white bg-slate-800 px-3 py-1.5 rounded-lg uppercase tracking-wide shadow-sm"
                    >{{ modalState.data.type }}</span
                  >
                  <span class="text-sm text-slate-400 font-bold">{{ modalState.data.date }}</span>
                </div>
                <h1 class="text-3xl font-extrabold text-slate-900 leading-tight">
                  {{ modalState.data.title }}
                </h1>
              </div>

              <!-- 미리보기 박스 -->
              <div
                class="rounded-3xl border border-slate-200/80 bg-white overflow-hidden shadow-sm flex items-center justify-center min-h-[300px]"
              >
                <img
                  v-if="modalState.data.type === 'image'"
                  :src="modalState.data.url"
                  class="w-full max-h-[500px] object-cover"
                />

                <div
                  v-else-if="modalState.data.type === 'video'"
                  class="relative w-full aspect-video bg-black"
                >
                  <img
                    :src="modalState.data.thumbnail || modalState.data.url"
                    class="absolute inset-0 w-full h-full object-cover opacity-60"
                  />
                  <div class="absolute inset-0 flex items-center justify-center">
                    <div
                      class="w-20 h-20 bg-white/25 backdrop-blur-md rounded-full flex items-center justify-center border border-white/50 shadow-2xl cursor-pointer hover:bg-white/40 transition hover:scale-110"
                    >
                      <svg
                        style="width: 40px; height: 40px"
                        class="text-white ml-2"
                        viewBox="0 0 24 24"
                        fill="currentColor"
                      >
                        <polygon points="5 3 19 12 5 21 5 3" />
                      </svg>
                    </div>
                  </div>
                </div>

                <div
                  v-else-if="modalState.data.type === 'link'"
                  class="p-16 text-center w-full bg-slate-50"
                >
                  <div
                    class="w-20 h-20 bg-white rounded-2xl shadow-sm flex items-center justify-center mx-auto mb-6 border border-slate-200"
                  >
                    <svg
                      style="width: 32px; height: 32px"
                      class="text-emerald-500"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                      <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                    </svg>
                  </div>
                  <a
                    :href="modalState.data.url"
                    target="_blank"
                    class="inline-flex items-center gap-2 bg-emerald-500 text-white px-6 py-3.5 rounded-xl font-bold hover:bg-emerald-600 transition-all shadow-md shadow-emerald-500/20"
                  >
                    외부 링크 방문하기
                    <svg
                      style="width: 18px; height: 18px"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                      <polyline points="15 3 21 3 21 9" />
                      <line x1="10" y1="14" x2="21" y2="3" />
                    </svg>
                  </a>
                </div>
              </div>

              <div>
                <h4 class="text-[15px] font-extrabold text-slate-800 mb-3">상세 설명</h4>
                <p
                  class="text-[16px] leading-loose text-slate-600 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-sm whitespace-pre-wrap"
                >
                  {{ modalState.data.description || '설명이 등록되지 않았습니다.' }}
                </p>
              </div>

              <div>
                <h4 class="text-[15px] font-extrabold text-slate-800 mb-3">태그 정보</h4>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="tag in modalState.data.tags"
                    :key="tag"
                    class="bg-white border border-slate-200 text-slate-700 px-4 py-2 rounded-xl text-sm font-bold shadow-sm"
                  >
                    #{{ tag }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Create / Edit 모드 -->
            <form v-else id="reference-form" @submit.prevent="handleSave" class="space-y-8">
              <!-- 유형 선택 -->
              <div>
                <label class="block text-[15px] font-extrabold text-slate-800 mb-3"
                  >자료 유형</label
                >
                <div class="grid grid-cols-3 gap-4">
                  <label
                    v-for="type in ['link', 'image', 'video']"
                    :key="type"
                    class="relative flex flex-col items-center justify-center p-5 border-2 rounded-2xl cursor-pointer transition-all shadow-sm"
                    :class="
                      formData.type === type
                        ? 'border-emerald-500 bg-emerald-50/50 text-emerald-700'
                        : 'border-slate-200 bg-white text-slate-400 hover:border-slate-300 hover:bg-slate-50'
                    "
                  >
                    <input type="radio" v-model="formData.type" :value="type" class="hidden" />
                    <svg
                      v-if="type === 'link'"
                      style="width: 28px; height: 28px"
                      class="mb-3"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                      <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                    </svg>
                    <svg
                      v-if="type === 'image'"
                      style="width: 28px; height: 28px"
                      class="mb-3"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                      <circle cx="8.5" cy="8.5" r="1.5" />
                      <polyline points="21 15 16 10 5 21" />
                    </svg>
                    <svg
                      v-if="type === 'video'"
                      style="width: 28px; height: 28px"
                      class="mb-3"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <polygon points="23 7 16 12 23 17 23 7" />
                      <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
                    </svg>
                    <span
                      class="text-[15px] font-bold"
                      :class="formData.type === type ? 'text-emerald-700' : 'text-slate-600'"
                      >{{ type === 'link' ? '링크' : type === 'image' ? '이미지' : '동영상' }}</span
                    >
                  </label>
                </div>
              </div>

              <!-- 입력 필드들 -->
              <div class="space-y-6 bg-white p-6 rounded-3xl border border-slate-200/80 shadow-sm">
                <div>
                  <label class="block text-sm font-bold text-slate-700 mb-2.5"
                    >제목 <span class="text-rose-500">*</span></label
                  >
                  <input
                    type="text"
                    v-model="formData.title"
                    required
                    placeholder="레퍼런스 제목을 입력하세요."
                    class="w-full bg-slate-50 border border-slate-200 rounded-xl px-5 py-3.5 text-[15px] focus:bg-white focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/10 outline-none transition font-medium placeholder:text-slate-400"
                  />
                </div>

                <div>
                  <label class="block text-sm font-bold text-slate-700 mb-2.5"
                    >URL 또는 파일 주소 <span class="text-rose-500">*</span></label
                  >
                  <input
                    type="url"
                    v-model="formData.url"
                    required
                    placeholder="https://..."
                    class="w-full bg-slate-50 border border-slate-200 rounded-xl px-5 py-3.5 text-[15px] focus:bg-white focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/10 outline-none transition font-medium placeholder:text-slate-400"
                  />
                </div>

                <div>
                  <label class="block text-sm font-bold text-slate-700 mb-2.5">상세 설명</label>
                  <textarea
                    v-model="formData.description"
                    rows="4"
                    placeholder="무엇을 참고해야 하는지 적어주세요."
                    class="w-full bg-slate-50 border border-slate-200 rounded-xl px-5 py-3.5 text-[15px] focus:bg-white focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/10 outline-none transition resize-none custom-scrollbar font-medium placeholder:text-slate-400 leading-relaxed"
                  ></textarea>
                </div>

                <div>
                  <label class="block text-sm font-bold text-slate-700 mb-2.5">태그</label>
                  <div
                    class="border border-slate-200 bg-slate-50 rounded-xl p-3 focus-within:bg-white focus-within:border-emerald-400 focus-within:ring-4 focus-within:ring-emerald-500/10 transition"
                  >
                    <div class="flex flex-wrap gap-2 mb-3" v-if="formData.tags.length > 0">
                      <span
                        v-for="tag in formData.tags"
                        :key="tag"
                        class="flex items-center gap-1.5 bg-slate-800 text-white px-3 py-1.5 rounded-lg text-[13px] font-bold shadow-sm"
                      >
                        {{ tag }}
                        <button
                          type="button"
                          @click="removeFormTag(tag)"
                          class="text-slate-300 hover:text-white transition-colors"
                        >
                          <svg
                            style="width: 14px; height: 14px"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <line x1="18" y1="6" x2="6" y2="18" />
                            <line x1="6" y1="6" x2="18" y2="18" />
                          </svg>
                        </button>
                      </span>
                    </div>
                    <div class="flex gap-2">
                      <input
                        type="text"
                        v-model="tagInput"
                        @keydown.enter="handleTagAdd"
                        placeholder="태그를 입력하고 Enter를 누르세요"
                        class="flex-1 bg-transparent text-[15px] outline-none px-3 py-2 font-medium placeholder:text-slate-400"
                      />
                      <button
                        type="button"
                        @click="handleTagAdd"
                        class="bg-white border border-slate-200 hover:border-slate-300 text-slate-700 px-4 py-2 rounded-lg text-sm font-bold transition shadow-sm"
                      >
                        추가
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </div>

          <!-- Footer (저장 버튼 / 뷰 모드 시 템플릿 이동) -->
          <div
            class="p-6 bg-white border-t border-slate-100 flex justify-end gap-3 shrink-0 z-10"
          >
            <template v-if="isViewMode">
              <button
                type="button"
                @click="closeModal"
                class="px-6 py-3 text-slate-600 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl text-[15px] font-bold transition shadow-sm"
              >
                닫기
              </button>
              <button
                @click="goToTemplate(modalState.data)"
                class="px-8 py-3 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-[15px] font-bold transition shadow-md shadow-slate-900/20 flex items-center gap-2"
              >
                <svg style="width: 18px; height: 18px" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M12 20h9" />
                  <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                </svg>
                이 레퍼런스로 템플릿 작성
              </button>
            </template>
            <template v-else>
              <button
                type="button"
                @click="closeModal"
                class="px-6 py-3 text-slate-600 bg-white border border-slate-200 hover:bg-slate-50 rounded-xl text-[15px] font-bold transition shadow-sm"
              >
                취소
              </button>
              <button
                type="submit"
                form="reference-form"
                class="px-8 py-3 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-[15px] font-bold transition shadow-md shadow-emerald-500/20"
              >
                저장하기
              </button>
            </template>
          </div>
        </div>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 8px;
}
.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background: #94a3b8;
}
</style>