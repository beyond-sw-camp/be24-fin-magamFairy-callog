<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'

import { generateAITemplate, refineAITemplate, saveTemplateDraft } from '@/api/templates/index.js'

const isGenerating = ref(false)
const isRefining = ref(false) // AI 문맥 교정 상태
const activeTab = ref('write') // 'write' | 'library'
const selectedFormat = ref('인스타그램 피드') // AI 생성용 포맷 선택
const isSimulatorOpen = ref(false) // 우측 슬라이드 시뮬레이터 상태
const isEditingTemplate = ref(false) // 템플릿 원본 수정 모드 상태

// 작업 로그 상태 관리
const logs = ref([])
const isLogModalOpen = ref(false)

const addLog = (message, type = 'info') => {
  const now = new Date()
  const timeString = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  logs.value.unshift({ id: Date.now(), message, time: timeString, type })
}

// ==========================================
// 글로벌 Undo / Redo 상태 관리 로직
// ==========================================
const appHistory = ref([])
const historyIndex = ref(-1)
let skipNextWatch = false
let historyTimeout = null

const saveHistory = (immediate = false) => {
  const pushState = () => {
    const state = JSON.stringify({
      content: templateData.value.content,
      inputs: inputValues.value,
      variables: templateData.value.variables,
      orderedKeys: orderedVariableKeys.value,
    })

    if (historyIndex.value >= 0 && appHistory.value[historyIndex.value] === state) return

    appHistory.value = appHistory.value.slice(0, historyIndex.value + 1)
    appHistory.value.push(state)
    historyIndex.value++

    if (appHistory.value.length > 50) {
      appHistory.value.shift()
      historyIndex.value--
    }
  }

  if (immediate) {
    if (historyTimeout) clearTimeout(historyTimeout)
    pushState()
  } else {
    if (historyTimeout) clearTimeout(historyTimeout)
    historyTimeout = setTimeout(pushState, 300)
  }
}

const resetHistory = () => {
  appHistory.value = []
  historyIndex.value = -1
  saveHistory(true)
}

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < appHistory.value.length - 1)

const undo = async () => {
  if (canUndo.value && !isRefining.value) {
    skipNextWatch = true
    historyIndex.value--
    const state = JSON.parse(appHistory.value[historyIndex.value])
    templateData.value.content = state.content
    templateData.value.variables = state.variables
    inputValues.value = state.inputs
    orderedVariableKeys.value = state.orderedKeys
    addLog('실행 취소(Undo)', 'info')
    await nextTick()
    setTimeout(() => {
      skipNextWatch = false
    }, 10)
  }
}

const redo = async () => {
  if (canRedo.value && !isRefining.value) {
    skipNextWatch = true
    historyIndex.value++
    const state = JSON.parse(appHistory.value[historyIndex.value])
    templateData.value.content = state.content
    templateData.value.variables = state.variables
    inputValues.value = state.inputs
    orderedVariableKeys.value = state.orderedKeys
    addLog('다시 실행(Redo)', 'info')
    await nextTick()
    setTimeout(() => {
      skipNextWatch = false
    }, 10)
  }
}

const handleKeydown = (e) => {
  if (e.ctrlKey || e.metaKey) {
    if (e.key.toLowerCase() === 'z') {
      e.preventDefault()
      if (e.shiftKey) {
        redo()
      } else {
        undo()
      }
    } else if (e.key.toLowerCase() === 'y') {
      e.preventDefault()
      redo()
    }
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const orderedVariableKeys = ref([])
const hoveredVariable = ref(null)

const clientRequest = ref(
  `사장님, 다음주 수요일부터 가을맞이 이벤트 진행하려고 합니다.\n대상은 2030 여성이고, 우리 뷰티샵에서 제일 인기있는 '수분촉촉 케어' 30% 할인 들어간다고 인스타에 올려주세요.\n글은 좀 감성적으로 예쁘게 써주시고, 마지막에 예약 링크 꼭 넣어주세요! 아, 그리고 첫 방문자한테는 샘플 증정한다는 내용도 추가해주세요.`,
)

const templateData = ref({
  title: '',
  type: '',
  content: '',
  variables: {},
})

const inputValues = ref({})
const newVariableName = ref('')

watch(
  () => templateData.value.variables,
  (vars) => {
    const currentKeys = Object.keys(vars)
    orderedVariableKeys.value = orderedVariableKeys.value.filter((k) => currentKeys.includes(k))
    currentKeys.forEach((k) => {
      if (!orderedVariableKeys.value.includes(k)) {
        orderedVariableKeys.value.push(k)
      }
    })
  },
  { deep: true, immediate: true },
)

watch(
  [
    () => templateData.value.content,
    () => templateData.value.variables,
    () => inputValues.value,
    () => orderedVariableKeys.value,
  ],
  () => {
    if (skipNextWatch) return
    saveHistory(false)
  },
  { deep: true },
)

const templateLibrary = ref([
  {
    id: 1,
    type: '인스타그램 피드',
    title: '시즌 할인 프로모션',
    desc: 'SNS 피드용 감성적인 이벤트 안내 포맷',
    tags: ['이벤트', 'SNS'],
    content: `✨ [이벤트명] ✨\n\n안녕하세요, [상호명]입니다.\n항상 저희 샵을 사랑해주시는 고객님들을 위해 이번 시즌 특별한 혜택을 준비했어요! 💖\n\n환절기에 접어들며 피부 고민, 혹은 스타일링 고민 많으셨죠?\n우리 샵에서 가장 사랑받는 베스트 코스를 파격적인 혜택으로 만나보실 수 있는 기회입니다.\n\n👇 아래 혜택을 꼭 확인해주세요 👇\n\n🎁 [혜택내용]\n\n단골 고객님들의 만족도가 가장 높은 프로그램들로만 꽉꽉 채워 넣었답니다.\n이번 기회에 나를 위한 확실한 힐링 타임을 가져보시는 건 어떨까요? 🥰\n\n[추가안내사항]\n\n🗓 진행 기간: [진행기간]\n📍 위치: [위치안내]\n💌 예약 및 문의: [예약링크]\n\n인기가 많은 프로그램이라 조기 마감될 수 있으니, 서둘러 예약해주세요!\n오늘도 [상호명]과 함께 아름다운 하루 보내시길 바랍니다. 🌸\n\n#[상호명] #[키워드] #이벤트그램 #시즌이벤트`,
    variables: {
      이벤트명: '',
      상호명: '',
      혜택내용: '',
      추가안내사항: '',
      진행기간: '',
      위치안내: '',
      예약링크: '',
      키워드: '',
    },
  },
  {
    id: 2,
    type: '네이버 블로그',
    title: '전문가 칼럼 / 정보성 포스팅',
    desc: '신뢰감을 주는 정보 제공형 긴 글 포맷',
    tags: ['정보성', '블로그'],
    content: `안녕하세요. [상호명]입니다.\n\n최근 [트렌드/이슈]에 대해 궁금해하시고 문의를 주시는 분들이 정말 많아졌습니다.\n아무래도 날씨가 변하고 시즌이 바뀌면서 관련된 고민이 깊어지시는 것 같은데요.\n\n그래서 오늘은 [주제]에 대해 전문가의 시선으로 아주 자세하고 알기 쉽게 정리해 드리는 시간을 가져보려고 합니다.\n끝까지 읽어보시면 분명 큰 도움이 되실 겁니다!\n\n---\n\n### 1. [소제목1]\n[내용1]\n많은 분들이 이 부분에서 실수를 하시곤 하는데요, 올바른 방법을 숙지하는 것이 가장 중요합니다.\n\n### 2. [소제목2]\n[내용2]\n이 원리를 이해하신다면 앞으로 혼자서도 충분히 관리해 나가실 수 있을 거예요.\n\n### 3. [상호명]만의 특별한 솔루션\n물론 혼자서 관리하기 버거울 때도 있습니다. 그럴 때 [상호명]에서는 [강점]을 바탕으로 고객님 개인의 상태에 맞춘 1:1 최상의 서비스를 제공해 드리고 있습니다.\n수많은 후기가 증명하는 저희만의 노하우를 직접 경험해 보세요.\n\n---\n\n더 궁금한 점이 있으시거나 상세한 전문가 상담이 필요하시다면 언제든 아래 연락처로 문의 남겨주시길 바랍니다.\n항상 친절하고 상세하게 답변해 드리겠습니다.\n\n📞 문의 및 예약: [연락처]\n📱 카카오톡 채널: [카톡채널명]`,
    variables: {
      상호명: '',
      '트렌드/이슈': '',
      주제: '',
      소제목1: '',
      내용1: '',
      소제목2: '',
      내용2: '',
      강점: '',
      연락처: '',
      카톡채널명: '',
    },
  },
])

// =========================================================
// 1. 백엔드 연동: AI 템플릿 생성
// =========================================================
const handleAIGenerate = async () => {
  isGenerating.value = true
  addLog(`[${selectedFormat.value}] AI 템플릿 생성을 시작합니다.`, 'info')

  try {
    const response = await generateAITemplate({
      prompt: clientRequest.value,
      format: selectedFormat.value,
    })

    const generatedTemplate = response.data

    templateData.value = generatedTemplate
    inputValues.value = { ...generatedTemplate.variables }
    orderedVariableKeys.value = Object.keys(generatedTemplate.variables)

    isGenerating.value = false
    isSimulatorOpen.value = false
    isEditingTemplate.value = false
    activeTab.value = 'write'

    addLog('AI 템플릿 생성이 완료되었습니다.', 'success')
    resetHistory()
  } catch (error) {
    console.error('AI 생성 에러:', error)
    addLog('AI 생성 요청에 실패했습니다.', 'warning')
    isGenerating.value = false
  }
}

// =========================================================
// 2. 백엔드 연동: 초안 저장
// =========================================================
const handleSaveDraft = async () => {
  addLog('서버에 초안 저장을 시도합니다...', 'info')
  try {
    await saveTemplateDraft({
      format: selectedFormat.value,
      templateData: templateData.value,
      inputValues: inputValues.value,
      clientRequest: clientRequest.value,
    })
    addLog('초안이 성공적으로 저장되었습니다.', 'success')
  } catch (error) {
    console.error('저장 에러:', error)
    addLog('초안 저장에 실패했습니다.', 'warning')
  }
}

// =========================================================
// 3. 백엔드 연동: AI 문맥 교정
// =========================================================
const refineContentWithAI = async (keyToRemove, contentBefore) => {
  isRefining.value = true
  addLog(`AI가 문맥 교정 중입니다...`, 'info')

  try {
    const response = await refineAITemplate({
      content: contentBefore,
      removedKey: keyToRemove,
    })

    templateData.value.content = response.data.refinedContent || response.data
    isRefining.value = false
    addLog(`AI 문맥 교정 완료`, 'success')
  } catch (error) {
    console.error('AI 교정 에러:', error)
    addLog('문맥 교정에 실패하여 원본(빈칸)을 유지합니다.', 'warning')

    // 에러 시 괄호만 지운 상태 유지
    templateData.value.content = contentBefore.trim()
    isRefining.value = false
  }
}

const handleLoadTemplate = (tpl) => {
  templateData.value = {
    title: tpl.title,
    type: tpl.type,
    content: tpl.content,
    variables: { ...tpl.variables },
  }

  const emptyValues = {}
  Object.keys(tpl.variables).forEach((key) => (emptyValues[key] = ''))
  inputValues.value = emptyValues
  orderedVariableKeys.value = Object.keys(tpl.variables)

  isSimulatorOpen.value = false
  isEditingTemplate.value = false
  activeTab.value = 'write'

  addLog(`라이브러리에서 '${tpl.title}' 템플릿을 불러왔습니다.`, 'info')
  resetHistory()
}

const toggleEditTemplate = () => {
  if (isEditingTemplate.value) {
    const matches = templateData.value.content.match(/\[([^\]]+)\]/g)
    if (matches) {
      matches.forEach((match) => {
        const key = match.slice(1, -1)
        if (!(key in templateData.value.variables)) {
          templateData.value.variables[key] = ''
          inputValues.value[key] = ''
          addLog(`새로운 변수 '[${key}]'가 자동 감지되어 추가되었습니다.`, 'success')
        }
      })
    }
  }
  isEditingTemplate.value = !isEditingTemplate.value
}

const openSimulator = () => {
  if (templateData.value.content) {
    const matches = templateData.value.content.match(/\[([^\]]+)\]/g)
    if (matches) {
      matches.forEach((match) => {
        const key = match.slice(1, -1)
        if (!(key in templateData.value.variables)) {
          templateData.value.variables[key] = ''
          inputValues.value[key] = ''
        }
      })
    }
  }
  isSimulatorOpen.value = true
  addLog('플랫폼 시뮬레이터를 열었습니다.', 'info')
}

const addVariable = () => {
  const key = newVariableName.value.trim()
  if (!key) return
  if (key in templateData.value.variables) {
    alert('이미 존재하는 항목입니다.')
    return
  }
  templateData.value.variables[key] = ''
  inputValues.value[key] = ''
  templateData.value.content += `\n[${key}]`
  newVariableName.value = ''
  addLog(`새 변수 '[${key}]'를 수동으로 추가했습니다.`, 'success')
}

const removeVariable = async (keyToRemove) => {
  delete templateData.value.variables[keyToRemove]
  delete inputValues.value[keyToRemove]

  const escapedKey = keyToRemove.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`\\[${escapedKey}\\]`, 'g')
  const tempContent = templateData.value.content.replace(regex, '')

  templateData.value.content = tempContent
  addLog(`변수 '${keyToRemove}' 삭제됨. AI 문맥 교정을 시작합니다.`, 'warning')

  skipNextWatch = true
  await refineContentWithAI(keyToRemove, tempContent)
  skipNextWatch = false

  saveHistory(true)
}

const escapeHtml = (text) => {
  if (!text) return ''
  return text
    .toString()
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const renderedContentHtml = computed(() => {
  if (!templateData.value.content) return ''

  let result = escapeHtml(templateData.value.content)

  Object.keys(templateData.value.variables).forEach((key) => {
    const val = inputValues.value[key] || `[${key}]`
    const escapedKeyStr = escapeHtml(key).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const escapedValStr = escapeHtml(val)

    const replacement =
      hoveredVariable.value === key
        ? `<span class="bg-blue-50 text-blue-900 border-b-2 border-blue-300 font-semibold px-0.5">${escapedValStr}</span>`
        : escapedValStr

    result = result.replace(new RegExp(`\\[${escapedKeyStr}\\]`, 'g'), replacement)
  })

  return result
})

const filledCount = computed(() => {
  return Object.keys(templateData.value.variables).filter((key) => {
    const val = inputValues.value[key]
    return val && val.trim().length > 0
  }).length
})

const totalCount = computed(() => Object.keys(templateData.value.variables).length)
const completionRate = computed(() => {
  return totalCount.value === 0 ? 0 : Math.round((filledCount.value / totalCount.value) * 100)
})

const isFilled = (key) => {
  const val = inputValues.value[key]
  return !!(val && val.trim().length > 0)
}

const currentAuthor = computed(() => {
  return (
    inputValues.value['상호명'] ||
    inputValues.value['채널명'] ||
    inputValues.value['블로거명'] ||
    '브랜드명'
  )
})
</script>

<template>
  <section
    class="grid gap-3 p-4 font-sans text-slate-800 bg-slate-100 min-h-screen relative overflow-hidden"
  >
    <!-- Header Area -->
    <div class="bg-white rounded-md p-5 border border-slate-300 z-10 relative">
      <div class="flex flex-col md:flex-row md:items-start justify-between gap-4 mb-4">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <svg
              style="width: 20px; height: 20px; flex-shrink: 0"
              class="text-slate-800"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
              <polyline points="14 2 14 8 20 8" />
              <line x1="16" x2="8" y1="13" y2="13" />
              <line x1="16" x2="8" y1="17" y2="17" />
              <line x1="10" x2="8" y1="9" y2="9" />
            </svg>
            <h2 class="text-xl font-bold tracking-tight text-slate-900">
              템플릿 관리 시스템 (CMS)
            </h2>
          </div>
          <p class="text-slate-500 text-sm">
            문맥 교정 AI와 연동하여 채널별 마케팅 템플릿을 작성하고 관리합니다.
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <div
            v-if="templateData.type"
            class="flex items-center gap-1.5 rounded-sm bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700 border border-slate-300"
          >
            <svg
              style="width: 14px; height: 14px; flex-shrink: 0"
              class="text-slate-600"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            {{ templateData.type }}
          </div>
          <div
            class="flex items-center gap-1.5 rounded-sm border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-700"
          >
            <span>변수 작성률:</span>
            <strong class="text-slate-900">{{ completionRate }}%</strong>
            <svg
              v-if="completionRate === 100"
              style="width: 14px; height: 14px; flex-shrink: 0"
              class="text-slate-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <path d="m9 12 2 2 4-4" />
            </svg>
          </div>
          <div
            class="flex items-center gap-1.5 rounded-sm border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-700"
          >
            <span class="w-2 h-2 rounded-full bg-blue-500"></span>
            <span>데이터 동기화 활성</span>
          </div>
        </div>
      </div>

      <div
        class="flex flex-col md:flex-row md:items-center justify-between gap-4 mt-2 border-t border-slate-200 pt-4"
      >
        <div class="flex bg-slate-100 rounded-sm border border-slate-300 p-0.5 w-fit">
          <button
            @click="activeTab = 'write'"
            class="px-4 py-1.5 rounded-sm text-sm transition-all flex items-center gap-2"
            :class="
              activeTab === 'write'
                ? 'bg-slate-800 text-white font-medium'
                : 'text-slate-600 hover:text-slate-800 hover:bg-slate-200'
            "
          >
            <svg
              style="width: 14px; height: 14px; flex-shrink: 0"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
              <rect width="18" height="6" x="3" y="3" rx="2" ry="2" />
              <rect width="6" height="12" x="3" y="9" rx="2" ry="2" />
            </svg>
            템플릿 작성
          </button>
          <button
            @click="activeTab = 'library'"
            class="px-4 py-1.5 rounded-sm text-sm transition-all flex items-center gap-2"
            :class="
              activeTab === 'library'
                ? 'bg-slate-800 text-white font-medium'
                : 'text-slate-600 hover:text-slate-800 hover:bg-slate-200'
            "
          >
            <svg
              style="width: 14px; height: 14px; flex-shrink: 0"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="m16 6 4 14" />
              <path d="M12 6v14" />
              <path d="M8 8v12" />
              <path d="M4 4v16" />
            </svg>
            라이브러리
          </button>
        </div>

        <div class="flex items-center gap-2">
          <button
            @click="isLogModalOpen = true"
            class="inline-flex items-center gap-1.5 rounded-sm bg-white border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          >
            <svg
              style="width: 14px; height: 14px; flex-shrink: 0"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            작업 로그
          </button>
          <!-- ✨ 저장 함수 교체: handleSaveDraft ✨ -->
          <button
            @click="handleSaveDraft"
            class="inline-flex items-center gap-1.5 rounded-sm bg-slate-800 border border-slate-800 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-slate-700"
          >
            <svg
              style="width: 14px; height: 14px; flex-shrink: 0"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
              <polyline points="17 21 17 13 7 13 7 21" />
              <polyline points="7 3 7 8 15 8" />
            </svg>
            초안 저장
          </button>
        </div>
      </div>
    </div>

    <!-- Main Content Grid -->
    <div class="flex flex-col lg:flex-row gap-3 items-start w-full z-10 relative">
      <!-- Left Sidebar (입력 및 AI 도우미) -->
      <aside class="w-full lg:w-80 flex-shrink-0 flex flex-col gap-3">
        <article class="bg-white border border-slate-300 rounded-md p-4 flex flex-col h-[300px]">
          <div class="flex items-center gap-2 mb-3 border-b border-slate-200 pb-2">
            <svg
              style="width: 16px; height: 16px; flex-shrink: 0"
              class="text-slate-600"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            <h2 class="text-sm font-semibold text-slate-800">프롬프트 원본</h2>
          </div>
          <textarea
            v-model="clientRequest"
            class="flex-1 w-full bg-slate-50 border border-slate-300 rounded-sm p-3 text-sm text-slate-700 focus:ring-1 focus:ring-slate-500 focus:border-slate-500 outline-none resize-none transition-colors custom-scrollbar"
            placeholder="기획 요청 내용을 입력하세요..."
          ></textarea>
        </article>

        <article class="bg-white border border-slate-300 rounded-md p-4">
          <div class="flex items-center gap-2 mb-3 border-b border-slate-200 pb-2">
            <svg
              style="width: 16px; height: 16px; flex-shrink: 0"
              class="text-slate-600"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path
                d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"
              />
            </svg>
            <h3 class="text-sm font-semibold text-slate-800">AI 생성 도우미</h3>
          </div>

          <div class="mb-4">
            <label class="block text-xs font-medium text-slate-500 mb-2">출력 타겟 포맷</label>
            <div class="flex flex-wrap gap-1.5">
              <button
                v-for="fmt in [
                  '인스타그램 피드',
                  '네이버 블로그',
                  '유튜브 커뮤니티',
                  '카카오 알림톡',
                ]"
                :key="fmt"
                @click="selectedFormat = fmt"
                class="px-2.5 py-1.5 rounded-sm border text-xs font-medium transition-all"
                :class="
                  selectedFormat === fmt
                    ? 'bg-slate-800 border-slate-800 text-white'
                    : 'bg-white border-slate-300 text-slate-600 hover:bg-slate-100'
                "
              >
                {{ fmt }}
              </button>
            </div>
          </div>

          <button
            @click="handleAIGenerate"
            :disabled="isGenerating || isRefining"
            class="flex w-full items-center justify-center gap-1.5 rounded-sm bg-slate-800 border border-slate-800 px-3 py-2 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <template v-if="isGenerating">
              <svg
                style="width: 14px; height: 14px; flex-shrink: 0"
                class="animate-spin"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
                <path d="M3 3v5h5" />
              </svg>
              생성 중...
            </template>
            <template v-else>
              <svg
                style="width: 14px; height: 14px; flex-shrink: 0"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path
                  d="m21.64 3.64-1.28-1.28a1.21 1.21 0 0 0-1.72 0L2.36 18.64a1.21 1.21 0 0 0 0 1.72l1.28 1.28a1.2 1.2 0 0 0 1.72 0L21.64 5.36a1.2 1.2 0 0 0 0-1.72Z"
                />
                <path d="m14 7 3 3" />
              </svg>
              선택 포맷으로 AI 생성
            </template>
          </button>
        </article>
      </aside>

      <!-- Right Area (라이브러리 OR 에디터/시뮬레이터) -->
      <main
        class="bg-white rounded-md border border-slate-300 min-w-0 flex-1 w-full p-4 flex flex-col lg:h-[calc(100vh-170px)] lg:min-h-[640px]"
      >
        <!-- Library View -->
        <template v-if="activeTab === 'library'">
          <div class="flex flex-col h-full min-h-0">
            <div class="mb-4 border-b border-slate-200 pb-3 shrink-0">
              <h3 class="text-sm font-semibold text-slate-800">마케팅 템플릿 라이브러리</h3>
              <p class="mt-1 text-xs text-slate-500">사전 정의된 구조화 템플릿 목록입니다.</p>
            </div>

            <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-2">
              <div class="grid grid-cols-1 xl:grid-cols-2 gap-3">
                <div
                  v-for="tpl in templateLibrary"
                  :key="tpl.id"
                  class="group flex flex-col justify-between rounded-md border border-slate-300 bg-slate-50 p-4 transition-all hover:border-slate-400 hover:bg-white"
                >
                  <div>
                    <div class="mb-3 flex items-start justify-between gap-3">
                      <div>
                        <span
                          class="rounded-sm bg-white border border-slate-300 px-2 py-0.5 text-[11px] font-medium text-slate-600"
                        >
                          {{ tpl.type }}
                        </span>
                        <h4 class="mt-2 text-sm font-semibold text-slate-800">{{ tpl.title }}</h4>
                        <p class="mt-1 text-xs text-slate-500">{{ tpl.desc }}</p>
                      </div>
                      <div
                        class="rounded-md bg-white border border-slate-300 p-1.5 text-slate-400 group-hover:bg-slate-100 group-hover:text-slate-600 transition-colors"
                      >
                        <svg
                          style="width: 16px; height: 16px; flex-shrink: 0"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path
                            d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                          />
                          <polyline points="14 2 14 8 20 8" />
                        </svg>
                      </div>
                    </div>
                    <div class="mb-4 flex flex-wrap gap-1.5">
                      <span
                        v-for="tag in tpl.tags"
                        :key="tag"
                        class="rounded-sm border border-slate-200 bg-white px-1.5 py-0.5 text-[10px] text-slate-500 font-medium"
                      >
                        #{{ tag }}
                      </span>
                    </div>
                  </div>
                  <button
                    @click="handleLoadTemplate(tpl)"
                    class="w-full rounded-sm bg-white border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 transition-all hover:bg-slate-50 hover:text-slate-900"
                  >
                    이 템플릿 사용
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- Write View (입력 항목 & 템플릿 에디터 동시 노출) -->
        <template v-else>
          <div class="flex flex-col xl:flex-row gap-4 flex-1 min-h-0">
            <!-- 1. 입력 항목 폼 -->
            <div
              class="w-full xl:w-1/3 flex flex-col border border-slate-300 rounded-md bg-white overflow-hidden h-[450px] xl:h-full shrink-0 xl:shrink relative"
            >
              <div
                class="bg-slate-50 p-3 border-b border-slate-300 flex justify-between items-center shrink-0"
              >
                <div class="flex items-center gap-2">
                  <svg
                    style="width: 16px; height: 16px; flex-shrink: 0"
                    class="text-slate-600"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path
                      d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                    />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" x2="8" y1="13" y2="13" />
                    <line x1="16" x2="8" y1="17" y2="17" />
                    <line x1="10" x2="8" y1="9" y2="9" />
                  </svg>
                  <h3 class="font-semibold text-sm text-slate-800">입력 파라미터</h3>
                </div>
                <span
                  v-if="templateData.title"
                  class="bg-white border border-slate-300 px-1.5 py-0.5 rounded-sm text-[10px] font-medium text-slate-500"
                >
                  {{ totalCount }}개 변수
                </span>
              </div>

              <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
                <div
                  v-if="!templateData.content"
                  class="flex h-full flex-col items-center justify-center rounded-md border border-dashed border-slate-300 bg-slate-50 p-6 text-center"
                >
                  <svg
                    style="width: 24px; height: 24px; flex-shrink: 0"
                    class="mb-2 text-slate-400"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
                    <path d="M9 3v18" />
                    <path d="m16 15-3-3 3-3" />
                  </svg>
                  <p class="text-xs font-medium text-slate-500">템플릿이 선택되지 않았습니다.</p>
                </div>

                <div v-else class="flex flex-col gap-3">
                  <div class="rounded-md bg-slate-100 p-3 border border-slate-200 mb-1">
                    <div class="text-[10px] font-medium text-slate-500 mb-0.5">
                      {{ templateData.type }}
                    </div>
                    <div class="text-sm font-semibold text-slate-800">{{ templateData.title }}</div>
                  </div>

                  <div class="pb-4 border-b border-slate-200 mb-1">
                    <label class="mb-1.5 block text-xs font-medium text-slate-600"
                      >수동 파라미터 추가</label
                    >
                    <div class="flex items-center gap-1.5">
                      <input
                        type="text"
                        v-model="newVariableName"
                        @keydown.enter="addVariable"
                        placeholder="키(Key) 명칭"
                        class="flex-1 w-full rounded-sm border border-slate-300 bg-white px-3 py-1.5 text-xs outline-none transition-colors focus:ring-1 focus:ring-slate-500 focus:border-slate-500"
                      />
                      <button
                        @click="addVariable"
                        class="flex items-center justify-center rounded-sm bg-slate-200 border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-300"
                      >
                        <svg
                          style="width: 12px; height: 12px; flex-shrink: 0; margin-right: 2px"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path d="M5 12h14" />
                          <path d="M12 5v14" />
                        </svg>
                        추가
                      </button>
                    </div>
                  </div>

                  <!-- 변수 렌더링 영역 -->
                  <div
                    v-for="(key, index) in orderedVariableKeys"
                    :key="key"
                    class="transition-all duration-200 relative group"
                  >
                    <div
                      @mouseenter="hoveredVariable = key"
                      @mouseleave="hoveredVariable = null"
                      class="rounded-md border border-slate-300 bg-white p-3 hover:border-slate-400 transition-colors"
                    >
                      <div class="flex justify-between items-center mb-1.5">
                        <label class="text-xs font-medium text-slate-700 flex items-center gap-1.5">
                          <span
                            class="flex items-center justify-center w-3.5 h-3.5 rounded-[3px] text-[9px] transition-colors duration-200"
                            :class="
                              isFilled(key)
                                ? 'bg-slate-600 text-white'
                                : 'bg-slate-200 text-slate-500'
                            "
                          >
                            {{ index + 1 }}
                          </span>
                          {{ key }}
                        </label>
                        <button
                          @click="removeVariable(key)"
                          :disabled="isRefining"
                          class="text-slate-400 hover:text-rose-600 opacity-0 group-hover:opacity-100 transition-opacity focus:opacity-100 p-0.5 disabled:opacity-0"
                          title="삭제"
                        >
                          <svg
                            style="width: 12px; height: 12px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M3 6h18" />
                            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
                            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
                          </svg>
                        </button>
                      </div>
                      <div class="relative">
                        <input
                          type="text"
                          v-model="inputValues[key]"
                          :placeholder="`값(Value) 입력`"
                          :disabled="isRefining"
                          class="w-full rounded-sm border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs outline-none transition-colors focus:bg-white focus:border-slate-400 focus:ring-1 focus:ring-slate-400 text-slate-800 disabled:opacity-50"
                        />
                        <div
                          v-if="isFilled(key)"
                          class="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400"
                        >
                          <svg
                            style="width: 12px; height: 12px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M20 6 9 17l-5-5" />
                          </svg>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 입력폼 AI 교정 진행 중 오버레이 방지막 -->
              <div
                v-if="isRefining"
                class="absolute inset-0 bg-white/60 backdrop-blur-[1px] z-30 flex items-center justify-center pointer-events-none"
              ></div>
            </div>

            <!-- 2. 메인 화면 - 템플릿 보기/편집 영역 -->
            <div
              class="flex-1 flex flex-col border border-slate-300 rounded-md bg-white overflow-hidden h-[600px] xl:h-full shrink-0 xl:shrink"
            >
              <div
                class="bg-slate-50 p-3 border-b border-slate-300 flex justify-between items-center shrink-0 z-20"
              >
                <div class="flex items-center gap-2">
                  <svg
                    style="width: 16px; height: 16px; flex-shrink: 0"
                    class="text-slate-600"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path
                      d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"
                    />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="16" x2="8" y1="13" y2="13" />
                    <line x1="16" x2="8" y1="17" y2="17" />
                    <line x1="10" x2="8" y1="9" y2="9" />
                  </svg>
                  <h3 class="font-semibold text-sm text-slate-800">
                    {{ isEditingTemplate ? '소스 코드 편집' : '렌더링 결과' }}
                  </h3>
                </div>

                <div class="flex items-center gap-1.5">
                  <!-- 에디터 패널 내부 Undo/Redo 버튼 -->
                  <div
                    class="flex items-center gap-0.5 bg-white p-0.5 rounded-sm border border-slate-300 mr-1"
                  >
                    <button
                      @click="undo"
                      :disabled="!canUndo || isRefining"
                      class="p-1 rounded-[2px] text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-30 disabled:hover:bg-transparent transition-all"
                      title="실행 취소 (Ctrl+Z)"
                    >
                      <svg
                        style="width: 12px; height: 12px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="M3 7v6h6" />
                        <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13" />
                      </svg>
                    </button>
                    <button
                      @click="redo"
                      :disabled="!canRedo || isRefining"
                      class="p-1 rounded-[2px] text-slate-500 hover:bg-slate-100 hover:text-slate-800 disabled:opacity-30 disabled:hover:bg-transparent transition-all"
                      title="다시 실행 (Ctrl+Y)"
                    >
                      <svg
                        style="width: 12px; height: 12px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="M21 7v6h-6" />
                        <path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3l3 2.7" />
                      </svg>
                    </button>
                  </div>

                  <button
                    @click="toggleEditTemplate"
                    :disabled="!templateData.content || isRefining"
                    class="rounded-sm border px-2.5 py-1.5 text-xs font-medium flex items-center gap-1.5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    :class="
                      isEditingTemplate
                        ? 'bg-slate-800 text-white border-slate-800'
                        : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-100'
                    "
                  >
                    <template v-if="isEditingTemplate">
                      <svg
                        style="width: 12px; height: 12px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="M20 6 9 17l-5-5" />
                      </svg>
                      편집 완료
                    </template>
                    <template v-else>
                      <svg
                        style="width: 12px; height: 12px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z" />
                        <path d="m15 5 4 4" />
                      </svg>
                      구조 편집
                    </template>
                  </button>

                  <button
                    @click="openSimulator"
                    :disabled="!templateData.content || isRefining"
                    class="rounded-sm border px-2.5 py-1.5 text-xs font-medium flex items-center gap-1.5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-white text-slate-700 border-slate-300 hover:bg-slate-100"
                  >
                    <svg
                      style="width: 12px; height: 12px; flex-shrink: 0"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    프리뷰 열기
                  </button>
                </div>
              </div>

              <div
                class="flex-1 bg-slate-100 flex items-center justify-center p-4 overflow-y-auto relative"
              >
                <div v-if="!templateData.content" class="text-center text-slate-400">
                  <svg
                    style="width: 32px; height: 32px; flex-shrink: 0"
                    class="mb-3 mx-auto opacity-30"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
                    <rect width="18" height="6" x="3" y="3" rx="2" ry="2" />
                    <rect width="6" height="12" x="3" y="9" rx="2" ry="2" />
                  </svg>
                  <p class="text-xs font-medium text-slate-500 mb-1">문서가 비어있습니다.</p>
                </div>

                <div
                  v-else-if="isEditingTemplate"
                  class="absolute inset-4 rounded-sm bg-white border border-slate-400 overflow-hidden shadow-sm flex flex-col"
                >
                  <div
                    class="bg-slate-100 px-3 py-1.5 border-b border-slate-300 flex justify-between shrink-0"
                  >
                    <span class="text-[10px] font-medium text-slate-600"
                      >대괄호 [변수명]을 사용해 데이터 바인딩 위치를 지정하세요.</span
                    >
                  </div>
                  <textarea
                    v-model="templateData.content"
                    :disabled="isRefining"
                    class="flex-1 w-full resize-none p-5 text-[14px] leading-relaxed text-slate-800 outline-none custom-scrollbar disabled:opacity-50 font-mono"
                    placeholder="템플릿 구조를 작성해 주세요..."
                  ></textarea>
                </div>

                <div
                  v-else
                  class="absolute inset-4 rounded-sm bg-white border border-slate-300 overflow-y-auto shadow-sm p-6 custom-scrollbar"
                >
                  <!-- AI 교정 중 표시되는 오버레이 -->
                  <div
                    v-if="isRefining"
                    class="absolute inset-0 bg-white/70 backdrop-blur-[1px] z-10 flex flex-col items-center justify-center transition-all duration-200"
                  >
                    <svg
                      style="width: 24px; height: 24px; flex-shrink: 0"
                      class="text-slate-600 animate-spin mb-3"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                    </svg>
                    <span class="text-[13px] font-medium text-slate-700"
                      >AI 문맥 최적화 진행 중...</span
                    >
                  </div>

                  <div
                    class="whitespace-pre-wrap leading-[1.8] text-[14px] text-slate-800"
                    v-html="renderedContentHtml"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </main>
    </div>

    <!-- 플랫폼 시뮬레이터 (우측 슬라이드 오버 패널) -->
    <transition
      enter-active-class="transition-opacity duration-300 ease-out"
      leave-active-class="transition-opacity duration-200 ease-in"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div v-if="isSimulatorOpen" class="fixed inset-0 z-50 flex justify-end">
        <div
          class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          @click="isSimulatorOpen = false"
        ></div>

        <transition
          enter-active-class="transition-transform duration-300 ease-out"
          leave-active-class="transition-transform duration-200 ease-in"
          enter-from-class="translate-x-full"
          leave-to-class="translate-x-full"
          appear
        >
          <div
            class="relative w-full max-w-[420px] bg-slate-50 h-full flex flex-col shadow-2xl border-l border-slate-300 z-10"
            @click.stop
          >
            <div
              class="bg-white px-5 py-4 border-b border-slate-200 flex justify-between items-center shrink-0"
            >
              <div class="flex items-center gap-2">
                <svg
                  style="width: 16px; height: 16px; flex-shrink: 0"
                  class="text-slate-600"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
                  <rect width="18" height="6" x="3" y="3" rx="2" ry="2" />
                  <rect width="6" height="12" x="3" y="9" rx="2" ry="2" />
                </svg>
                <h3 class="text-sm font-semibold text-slate-800">플랫폼 타겟 렌더링</h3>
              </div>
              <button
                @click="isSimulatorOpen = false"
                class="p-1.5 text-slate-400 hover:text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-sm transition-colors"
              >
                <svg
                  style="width: 16px; height: 16px; flex-shrink: 0"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <path d="M18 6 6 18" />
                  <path d="m6 6 12 12" />
                </svg>
              </button>
            </div>

            <div
              class="flex-1 p-5 flex items-center justify-center overflow-y-auto custom-scrollbar"
            >
              <div
                class="bg-white overflow-hidden relative shrink-0 mx-auto"
                style="
                  width: 320px;
                  height: 600px;
                  border-radius: 2rem;
                  border: 10px solid #1e293b;
                  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
                "
              >
                <div
                  class="absolute top-0 left-1/2 transform -translate-x-1/2 h-5 bg-[#1e293b] rounded-b-xl z-50 flex items-center justify-center"
                  style="width: 100px"
                >
                  <div class="bg-[#0f172a] rounded-full" style="width: 30px; height: 4px"></div>
                </div>

                <div class="w-full h-full overflow-y-auto custom-scrollbar relative pt-5 bg-white">
                  <!-- Instagram UI -->
                  <div
                    v-if="templateData.type === '인스타그램 피드'"
                    class="bg-white text-black min-h-full"
                  >
                    <div
                      class="flex items-center justify-between px-3 py-3 border-b border-gray-200"
                    >
                      <div class="flex items-center gap-4">
                        <svg
                          style="width: 20px; height: 20px; flex-shrink: 0"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path d="m12 19-7-7 7-7" />
                          <path d="M19 12H5" />
                        </svg>
                        <span class="font-bold text-sm">{{ currentAuthor }}</span>
                      </div>
                      <svg
                        style="width: 20px; height: 20px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
                      </svg>
                    </div>
                    <div class="flex items-center justify-between px-3 py-2">
                      <div class="flex items-center gap-2">
                        <div
                          class="rounded-full p-[2px]"
                          style="
                            background: linear-gradient(
                              45deg,
                              #f09433 0%,
                              #e6683c 25%,
                              #dc2743 50%,
                              #cc2366 75%,
                              #bc1888 100%
                            );
                            width: 30px;
                            height: 30px;
                          "
                        >
                          <div
                            class="w-full h-full bg-white rounded-full border-2 border-white overflow-hidden flex items-center justify-center"
                          >
                            <div class="w-full h-full bg-gray-200"></div>
                          </div>
                        </div>
                        <span class="font-bold text-xs">{{ currentAuthor }}</span>
                      </div>
                      <svg
                        style="width: 16px; height: 16px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <circle cx="12" cy="12" r="1" />
                        <circle cx="19" cy="12" r="1" />
                        <circle cx="5" cy="12" r="1" />
                      </svg>
                    </div>
                    <div
                      class="w-full aspect-square bg-gray-100 flex flex-col items-center justify-center text-gray-400"
                    >
                      <svg
                        style="width: 40px; height: 40px; flex-shrink: 0"
                        class="mb-2 opacity-30"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
                        <circle cx="9" cy="9" r="2" />
                        <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" />
                      </svg>
                      <span class="text-xs font-medium">에셋 영역</span>
                    </div>
                    <div class="flex items-center justify-between px-3 py-3">
                      <div class="flex gap-3">
                        <svg
                          style="width: 22px; height: 22px; flex-shrink: 0"
                          class="hover:text-red-500 transition-colors"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path
                            d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
                          />
                        </svg>
                        <svg
                          style="width: 22px; height: 22px; flex-shrink: 0; transform: scaleX(-1)"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path d="m3 21 1.9-5.7a8.5 8.5 0 1 1 3.8 3.8z" />
                        </svg>
                        <svg
                          style="width: 22px; height: 22px; flex-shrink: 0"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path d="m22 2-7 20-4-9-9-4Z" />
                          <path d="M22 2 11 13" />
                        </svg>
                      </div>
                      <svg
                        style="width: 20px; height: 20px; flex-shrink: 0"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z" />
                      </svg>
                    </div>
                    <div class="px-3 pb-1">
                      <span class="font-semibold text-[11px]">좋아요 1,234개</span>
                    </div>
                    <div class="px-3 pb-6 text-[12px] leading-relaxed whitespace-pre-wrap">
                      <span class="font-semibold mr-2">{{ currentAuthor }}</span>
                      <span v-html="renderedContentHtml"></span>
                    </div>
                  </div>

                  <!-- Blog UI -->
                  <div
                    v-else-if="templateData.type === '네이버 블로그'"
                    class="bg-white text-gray-800 min-h-full"
                  >
                    <div
                      class="flex items-center justify-between px-4 py-3 border-b border-gray-100"
                    >
                      <div class="flex gap-4 text-gray-500">
                        <svg
                          style="width: 20px; height: 20px; flex-shrink: 0"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <line x1="4" x2="20" y1="12" y2="12" />
                          <line x1="4" x2="20" y1="6" y2="6" />
                          <line x1="4" x2="20" y1="18" y2="18" />
                        </svg>
                      </div>
                      <div class="font-extrabold text-[#03c75a] text-lg tracking-tighter">blog</div>
                      <div style="width: 20px"></div>
                    </div>
                    <div class="px-5 pt-6 pb-4">
                      <div class="text-[10px] font-bold text-gray-400 mb-1.5">
                        {{ currentAuthor }}의 블로그
                      </div>
                      <h1 class="text-lg font-bold leading-tight mb-4">
                        {{ templateData.title || '제목이 입력되지 않았습니다.' }}
                      </h1>
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                          <div
                            class="rounded-full bg-gray-200"
                            style="width: 30px; height: 30px"
                          ></div>
                          <div>
                            <div class="text-[11px] font-bold">{{ currentAuthor }}</div>
                            <div class="text-[9px] text-gray-400">1시간 전</div>
                          </div>
                        </div>
                        <button
                          class="flex items-center gap-1 text-[10px] font-bold text-[#03c75a] bg-[#03c75a]/10 px-2 py-1 rounded-sm"
                        >
                          <svg
                            style="width: 10px; height: 10px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <line x1="19" x2="19" y1="8" y2="14" />
                            <line x1="22" x2="16" y1="11" y2="11" />
                          </svg>
                          이웃추가
                        </button>
                      </div>
                    </div>
                    <div class="border-t border-gray-100 mx-5 mb-5"></div>
                    <div
                      class="px-5 pb-8 text-[13px] leading-[1.8] whitespace-pre-wrap text-gray-700"
                      v-html="renderedContentHtml"
                    ></div>
                    <div class="border-t border-gray-100 px-5 py-3 flex gap-4">
                      <button class="flex items-center gap-1 text-xs font-medium text-gray-500">
                        <svg
                          style="width: 16px; height: 16px; flex-shrink: 0"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path
                            d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
                          />
                        </svg>
                        <span>12</span>
                      </button>
                      <button class="flex items-center gap-1 text-xs font-medium text-gray-500">
                        <svg
                          style="width: 16px; height: 16px; flex-shrink: 0"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <path d="m3 21 1.9-5.7a8.5 8.5 0 1 1 3.8 3.8z" />
                        </svg>
                        <span>댓글 4</span>
                      </button>
                    </div>
                  </div>

                  <!-- Youtube UI -->
                  <div
                    v-else-if="templateData.type === '유튜브 커뮤니티'"
                    class="min-h-full font-sans"
                    style="background-color: #0f0f0f; color: white"
                  >
                    <div class="flex items-center px-4 py-3 border-b border-gray-800">
                      <svg
                        style="width: 20px; height: 20px; flex-shrink: 0"
                        class="mr-3"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="m12 19-7-7 7-7" />
                        <path d="M19 12H5" />
                      </svg>
                      <span class="font-medium text-base">{{ currentAuthor }}</span>
                    </div>
                    <div class="flex text-xs font-medium border-b border-gray-800 text-gray-400">
                      <div class="px-3 py-2.5">홈</div>
                      <div class="px-3 py-2.5">동영상</div>
                      <div class="px-3 py-2.5 text-white border-b-2 border-white">커뮤니티</div>
                    </div>
                    <div class="p-4">
                      <div class="flex items-center justify-between mb-3">
                        <div class="flex items-center gap-2.5">
                          <div
                            class="rounded-full bg-gray-700"
                            style="width: 32px; height: 32px"
                          ></div>
                          <div>
                            <div class="text-[12px] font-medium">{{ currentAuthor }}</div>
                            <div class="text-[10px] text-gray-400">2시간 전</div>
                          </div>
                        </div>
                        <svg
                          style="width: 16px; height: 16px; flex-shrink: 0"
                          class="text-gray-400"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        >
                          <circle cx="12" cy="12" r="1" />
                          <circle cx="12" cy="5" r="1" />
                          <circle cx="12" cy="19" r="1" />
                        </svg>
                      </div>
                      <div
                        class="text-[13px] leading-relaxed mb-4 whitespace-pre-wrap"
                        v-html="renderedContentHtml"
                      ></div>
                      <div class="flex items-center gap-5 mt-3">
                        <div class="flex items-center gap-1.5 text-gray-300">
                          <svg
                            style="width: 16px; height: 16px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M7 10v12" />
                            <path
                              d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0-1.79-1.11L12 2h0a3.13 3.13 0 0 1 3 3.88Z"
                            />
                          </svg>
                          <span class="text-[11px]">4.2천</span>
                        </div>
                        <div class="text-gray-300">
                          <svg
                            style="width: 16px; height: 16px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M17 14V2" />
                            <path
                              d="M9 18.12 10 14H4.17a2 2 0 0 1-1.92-2.56l2.33-8A2 2 0 0 1 6.5 2H20a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-2.76a2 2 0 0 0-1.79 1.11L12 22h0a3.13 3.13 0 0 1-3-3.88Z"
                            />
                          </svg>
                        </div>
                        <div class="flex items-center gap-1.5 text-gray-300 ml-auto">
                          <svg
                            style="width: 16px; height: 16px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="m3 21 1.9-5.7a8.5 8.5 0 1 1 3.8 3.8z" />
                          </svg>
                          <span class="text-[11px]">128</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Kakao UI -->
                  <div
                    v-else-if="templateData.type === '카카오 알림톡'"
                    class="min-h-full font-sans text-[12px] flex flex-col"
                    style="background-color: #b2c7d9"
                  >
                    <div
                      class="px-3 py-2.5 flex items-center gap-3 sticky top-0 z-10 border-b border-[#a0b8cc]"
                      style="background-color: rgba(178, 199, 217, 0.95)"
                    >
                      <svg
                        style="width: 20px; height: 20px; flex-shrink: 0"
                        class="text-gray-800"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <path d="m12 19-7-7 7-7" />
                        <path d="M19 12H5" />
                      </svg>
                      <span class="font-semibold text-sm text-gray-900">{{ currentAuthor }}</span>
                    </div>
                    <div class="p-4 flex-1">
                      <div class="flex justify-center mb-4">
                        <span
                          class="text-white text-[10px] font-medium px-3 py-1 rounded-full shadow-sm"
                          style="background-color: rgba(0, 0, 0, 0.2)"
                          >2026년 10월 14일 수요일</span
                        >
                      </div>
                      <div class="flex gap-2">
                        <div
                          class="rounded-xl flex items-center justify-center shrink-0 shadow-sm text-yellow-900 font-extrabold text-[10px]"
                          style="width: 32px; height: 32px; background-color: #fee500"
                        >
                          TALK
                        </div>
                        <div class="flex flex-col gap-1 w-full max-w-[240px]">
                          <span class="text-[11px] font-medium text-gray-700 ml-1">{{
                            currentAuthor
                          }}</span>
                          <div
                            class="bg-white rounded-md rounded-tl-none p-3.5 shadow-sm text-gray-800"
                          >
                            <div
                              class="flex items-center gap-1.5 mb-2.5 pb-2.5 border-b border-gray-100"
                            >
                              <span
                                class="text-[9px] font-extrabold px-1.5 py-0.5 rounded-sm"
                                style="background-color: #fee500; color: #391b1b"
                                >알림톡</span
                              >
                              <span class="text-[10px] text-gray-500 font-medium"
                                >카카오 인증마크</span
                              >
                            </div>
                            <div
                              class="whitespace-pre-wrap leading-relaxed text-[12px]"
                              v-html="renderedContentHtml"
                            ></div>
                            <button
                              class="w-full mt-3 py-2 bg-gray-100 rounded-sm text-[12px] font-medium text-gray-700 hover:bg-gray-200 transition-colors"
                            >
                              자세히 보기
                            </button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </transition>

    <!-- 작업 로그 모달창 (가운데 팝업) -->
    <transition
      enter-active-class="transition-opacity duration-200"
      leave-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      leave-to-class="opacity-0"
    >
      <div v-if="isLogModalOpen" class="fixed inset-0 z-[60] flex items-center justify-center p-4">
        <!-- 배경 -->
        <div
          class="absolute inset-0 bg-slate-900/50 backdrop-blur-sm"
          @click="isLogModalOpen = false"
        ></div>
        <!-- 패널 -->
        <div
          class="relative bg-white rounded-md shadow-xl border border-slate-300 w-full max-w-md max-h-[80vh] flex flex-col transform transition-transform animate-in zoom-in-95"
        >
          <div
            class="px-4 py-3 border-b border-slate-200 flex justify-between items-center bg-slate-50 rounded-t-md"
          >
            <h3 class="text-sm font-semibold text-slate-800 flex items-center gap-2">
              <svg
                style="width: 14px; height: 14px; flex-shrink: 0"
                class="text-slate-600"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              시스템 작업 로그
            </h3>
            <button
              @click="isLogModalOpen = false"
              class="p-1 text-slate-500 hover:text-slate-800 hover:bg-slate-200 rounded-sm transition-colors"
            >
              <svg
                style="width: 16px; height: 16px; flex-shrink: 0"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
            </button>
          </div>
          <div class="flex-1 overflow-y-auto p-2 custom-scrollbar">
            <div
              v-if="logs.length === 0"
              class="p-8 text-center text-xs text-slate-500 font-medium"
            >
              기록된 로그가 없습니다.
            </div>
            <ul v-else class="space-y-0.5">
              <li
                v-for="log in logs"
                :key="log.id"
                class="px-3 py-2 rounded-sm flex items-start gap-3 hover:bg-slate-50 transition-colors"
              >
                <span class="text-[10px] font-mono text-slate-400 shrink-0 mt-0.5">{{
                  log.time
                }}</span>
                <span
                  class="text-[13px] font-medium text-slate-700"
                  :class="{
                    'text-blue-600': log.type === 'success',
                    'text-rose-600': log.type === 'warning',
                  }"
                >
                  {{ log.message }}
                </span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
}
.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background: #94a3b8;
}

/* 선택(하이라이트) 디자인 차분하게 변경 */
.bg-blue-50 {
  background-color: #eff6ff;
}
.text-blue-900 {
  color: #1e3a8a;
}
.border-blue-300 {
  border-color: #93c5fd;
}
</style>
