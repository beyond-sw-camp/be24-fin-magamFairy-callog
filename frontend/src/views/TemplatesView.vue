<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'

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
let skipNextWatch = false // 히스토리 복원 시 watch 중복 실행 방지 플래그
let historyTimeout = null

const saveHistory = (immediate = false) => {
  const pushState = () => {
    const state = JSON.stringify({
      content: templateData.value.content,
      inputs: inputValues.value,
      variables: templateData.value.variables,
      orderedKeys: orderedVariableKeys.value,
    })

    // 이전 상태와 동일하면 저장하지 않음
    if (historyIndex.value >= 0 && appHistory.value[historyIndex.value] === state) return

    // 현재 인덱스 이후의 미래 히스토리(Redo)는 잘라냄
    appHistory.value = appHistory.value.slice(0, historyIndex.value + 1)
    appHistory.value.push(state)
    historyIndex.value++

    // 히스토리는 최대 50개까지만 유지
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
    // 타이핑 중 과도한 저장을 막기 위해 300ms 디바운스 적용
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

// 글로벌 단축키 (Ctrl+Z, Ctrl+Y, Cmd+Shift+Z)
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
// ==========================================

const orderedVariableKeys = ref([]) // UI 렌더링 순서를 고정하기 위한 배열
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

// 변수 순서 고정 로직 (추가/삭제 시 제자리 유지)
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

// 모든 상태 변화를 감지하여 히스토리 저장
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

// 마케팅 채널 라이브러리 더미 데이터
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
  {
    id: 3,
    type: '유튜브 커뮤니티',
    title: '구독자 소통 / 이벤트 공지',
    desc: '유튜브 구독자 대상 친근한 소통 포맷',
    tags: ['소통', '유튜브'],
    content: `[인사말] [채널명] 구독자 여러분!!\n여러분의 사랑과 관심 덕분에 저희 채널이 하루가 다르게 쑥쑥 성장하고 있습니다. 항상 응원해주셔서 진심으로 엎드려 감사드립니다! 🙇‍♂️🙇‍♀️\n\n오늘은 여러분이 지난 영상 댓글에서 정말 많이 요청해주셨던 바로 그 소식을 들고 왔습니다.\n이번 주에 구독자 여러분만을 위해 뼈를 깎아 준비한 역대급 [이벤트명]을 드디어 오픈합니다! 🎉🔥\n\n🚨 [이벤트내용] 🚨\n\n놓치면 정말 후회할 만한 혜택들이 쏟아지니까요, 영상 알림 설정 꼭 해두시고 커뮤니티도 자주 확인해 주세요.\n\n👇 자세한 참여 방법 및 혜택 링크는 아래를 꾹 눌러주세요! 👇\n👉 [링크]\n\n여러분과 함께 만들어가는 채널인 만큼, 앞으로도 퀄리티 높은 영상과 빵빵한 혜택으로 보답하겠습니다.\n구독, 좋아요, 알림 설정 잊지 않으셨죠?\n다음 영상에서 만나요! 안녕~ 👋`,
    variables: { 인사말: '', 채널명: '', 이벤트명: '', 이벤트내용: '', 링크: '' },
  },
  {
    id: 4,
    type: '카카오 알림톡',
    title: '재방문 유도 / 혜택 안내',
    desc: '기존 고객에게 발송하는 깔끔한 정보 전달형',
    tags: ['CRM', '알림톡'],
    content: `[안내] [상호명]에서 챙겨드리는 특별한 시크릿 혜택 🎁\n\n안녕하세요, [고객명]님!\n저희 [상호명]을 이용해주셨던 소중한 인연을 기억하며, 감사의 마음을 담아 [고객명]님만을 위한 [혜택명] 쿠폰을 몰래 넣어드렸습니다.\n\n환절기 건강 꼭 유의하시고, 기분 전환이 필요하실 때 언제든 방문해 주세요.\n\n▶ 발급된 혜택 상세\n- 혜택 내용: [상세혜택]\n- 사용 조건: [사용조건]\n- 쿠폰 기한: [만료일] 자정까지\n\n▶ 사용 방법 안내\n[사용방법안내]\n\n아래 '혜택 확인하기' 버튼을 눌러 지금 바로 내 쿠폰함에서 혜택을 확인해 보세요!\n예약이 필요하신 경우 링크를 통해 간편하게 일정 등록도 가능합니다.`,
    variables: {
      상호명: '',
      고객명: '',
      혜택명: '',
      상세혜택: '',
      사용조건: '',
      만료일: '',
      사용방법안내: '',
    },
  },
])

const handleAIGenerate = () => {
  isGenerating.value = true
  addLog(`[${selectedFormat.value}] AI 템플릿 생성을 시작합니다.`, 'info')

  setTimeout(() => {
    let generatedTemplate = {}

    if (selectedFormat.value === '네이버 블로그') {
      generatedTemplate = {
        title: '가을 이벤트 블로그 정보성 글',
        type: '네이버 블로그',
        content: `안녕하세요. 언제나 고객님의 아름다움을 최우선으로 생각하는 [상호명]입니다.\n\n최근 아침저녁으로 찬바람이 매섭게 불면서, 거울을 볼 때마다 푸석해지고 당기는 피부 때문에 스트레스를 호소하시는 [타겟고객] 분들의 문의가 폭주하고 있습니다.\n\n### 가을철, 내 피부가 급격히 건조해지는 진짜 이유\n가을철의 큰 일교차와 뚝 떨어진 습도는 우리 피부의 1차 방어선인 피부 장벽을 무너뜨립니다. 수분이 쉽게 증발하는 환경이 되다 보니, 평소 쓰던 스킨케어 제품만으로는 보습이 채워지지 않는 것이죠.\n\n### 해결책: [상호명]의 베스트셀러 '[서비스명]'\n저희 [상호명]에서 수년간 부동의 1위를 지키고 있는 '[서비스명]'은 겉도는 수분이 아닌, 진피층까지 촘촘하게 수분을 채워주는 딥 모이스처라이징 케어입니다. 1회만 받아보셔도 다음 날 화장 먹는 느낌이 다를 거라고 자부합니다.\n\n🎉 블로그 이웃 한정, 가을맞이 특별 이벤트\n현재 이 블로그 글을 캡처해서 방문해 주시는 분들께 무려 [할인율]% 할인을 적용해 드리는 깜짝 이벤트를 진행 중입니다!\n\n또한, 첫 방문이신 분들께는 홈케어로도 유지하실 수 있도록 [사은품]도 듬뿍 챙겨드리고 있으니 이 기회를 절대 놓치지 마세요.\n\n편하신 시간대로 아래 링크에서 예약해 주시면, 꼼꼼하게 상담 도와드리겠습니다.\n\n예약 및 문의: [예약링크]`,
        variables: {
          상호명: '마감요정 뷰티샵',
          타겟고객: '2030 여성',
          서비스명: '수분촉촉 케어',
          할인율: '30',
          사은품: '고급 수분크림 샘플 3종 세트',
          예약링크: 'linktr.ee/beauty',
        },
      }
    } else if (selectedFormat.value === '카카오 알림톡') {
      generatedTemplate = {
        title: '가을 이벤트 알림톡 발송용',
        type: '카카오 알림톡',
        content: `[안내] [상호명] 가을맞이 시크릿 쿠폰 도착 🎁\n\n안녕하세요, 고객님!\n유독 건조한 올가을, 피부 컨디션은 안녕하신가요?\n\n고객님의 촉촉한 가을을 위해 저희 샵에서 제일 인기 있는 '[서비스명]' 파격 할인 쿠폰을 챙겨드렸습니다.\n\n▶ 메인 혜택: [서비스명] [할인율]% 즉시 할인\n▶ 추가 특별 혜택: [사은품]\n▶ 이벤트 진행 기간: [이벤트기간]\n\n찬 바람에 지친 피부, 더 늦기 전에 꽉 찬 수분으로 달래주세요.\n아래 하단 버튼을 눌러 원하시는 날짜에 빠르게 예약하실 수 있습니다!`,
        variables: {
          상호명: '마감요정 뷰티샵',
          서비스명: '수분촉촉 케어',
          할인율: '30',
          사은품: '첫 방문자 한정 맞춤 앰플 샘플 증정',
          이벤트기간: '다음주 수요일부터 선착순 마감 시까지',
        },
      }
    } else if (selectedFormat.value === '유튜브 커뮤니티') {
      generatedTemplate = {
        title: '가을 이벤트 공지용 (커뮤니티)',
        type: '유튜브 커뮤니티',
        content: `안녕하세요, [채널명] 구독자 여러분! 🍂\n\n아침저녁으로 쌀쌀해진 날씨에 다들 감기 안 걸리고 잘 지내고 계시죠?\n오늘은 여러분이 정말 손꼽아 기다리시던 핫한 소식을 들고 왔습니다!\n\n드디어 다음 주 수요일부터 가을맞이 특별 이벤트를 오픈합니다! 🎉\n구독자분들이 댓글로 가장 많이 요청하셨던 저희 샵 1위 코스, '[서비스명]'을 무려 [할인율]%나 팍팍 깎아드립니다.\n\n특히나 이 게시물을 보고 오시는 [타겟고객] 찐팬 여러분 중 첫 방문이신 분들께는, 제가 사비 털어서 준비한 [사은품]도 현장에서 쏘겠습니다!! 😎\n\n🗓 진행 기간: [이벤트기간]\n✅ 예약 방법: [예약링크]\n\n매번 빠른 속도로 마감되는 거 아시죠? 얼른 링크 타고 예약부터 걸어두세요!\n항상 저희 채널 사랑해 주셔서 너무 감사드립니다. 사랑해요 여러분! 👍`,
        variables: {
          채널명: '뷰티마스터 요정',
          서비스명: '수분촉촉 케어',
          할인율: '30',
          타겟고객: '2030 여성',
          사은품: '스페셜 쿨링 마스크팩 증정',
          이벤트기간: '다음주 수요일 ~ 10월 말까지',
          예약링크: 'linktr.ee/beauty',
        },
      }
    } else {
      generatedTemplate = {
        title: '가을 시즌 타겟 이벤트 (인스타용)',
        type: '인스타그램 피드',
        content: `🍂 찬 바람이 불기 시작할 때, 가장 먼저 무너지는 피부 장벽!\n매일 아침 거울 보며 건조하고 푸석한 피부 때문에 스트레스 받지 않으신가요? 😢\n\n트렌드를 아는 [타겟고객] 고객님들을 위해 [상호명]이 야심 차게 준비한 가을 맞이 특별 수분 충전 이벤트! 💧\n\n수많은 고객님들의 찐 후기가 증명하는 저희 샵 부동의 베스트 코스,\n✨ '[서비스명]'을 무려 [할인율]% OFF ✨ 가격으로 모십니다.\n\n단 한 번의 관리만으로도 메이크업 밀착력이 달라지는 마법을 경험해보세요.\n\n🎁 여기서 끝이 아니에요! (특별 혜택)\n이번 이벤트 기간 내 첫 방문해주시는 고객님들께는 홈케어를 완벽하게 도와줄 [사은품]을 100% 챙겨드립니다.\n\n오직 가을 시즌에만 만날 수 있는 파격 혜택,\n자리가 한정되어 있으니 지금 바로 프로필 상단 링크를 클릭해 예약해주세요! 👇\n[예약링크]\n\n#가을이벤트 #수분관리 #[상호명] #뷰티스타그램 #이벤트그램 #환절기피부관리`,
        variables: {
          타겟고객: '2030 여성',
          상호명: '마감요정 뷰티샵',
          서비스명: '수분촉촉 케어',
          할인율: '30',
          사은품: '고급 수분크림 샘플 세트',
          예약링크: 'linktr.ee/beauty',
        },
      }
    }

    templateData.value = generatedTemplate
    inputValues.value = { ...generatedTemplate.variables }
    orderedVariableKeys.value = Object.keys(generatedTemplate.variables)

    isGenerating.value = false
    isSimulatorOpen.value = false
    isEditingTemplate.value = false
    activeTab.value = 'write'

    addLog('AI 템플릿 생성이 완료되었습니다.', 'success')
    resetHistory() // 히스토리 초기화
  }, 1200)
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
  resetHistory() // 히스토리 초기화
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

const refineContentWithAI = (keyToRemove, contentBefore) => {
  return new Promise((resolve) => {
    isRefining.value = true
    addLog(`AI가 문맥 교정 중입니다...`, 'info')

    setTimeout(() => {
      let refinedContent = contentBefore
      refinedContent = refinedContent.replace(/안녕하세요,\s*입니다\./g, '안녕하세요.')
      refinedContent = refinedContent.replace(/안녕하세요\s*입니다\./g, '안녕하세요.')
      refinedContent = refinedContent.replace(/✨\s*✨/g, '')
      refinedContent = refinedContent.replace(/\[\s*\]/g, '')
      refinedContent = refinedContent.replace(/\(\s*\)/g, '')
      refinedContent = refinedContent.replace(/\s+(은|는|이|가|을|를|과|와)\s+/g, ' ')
      refinedContent = refinedContent.replace(/\n\s*\n\s*\n/g, '\n\n')

      templateData.value.content = refinedContent.trim()
      isRefining.value = false
      addLog(`AI 문맥 교정 완료`, 'success')
      resolve()
    }, 1200)
  })
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
        ? `<span class="bg-yellow-300 text-yellow-900 rounded px-1 transition-all duration-200 shadow-sm border-b-2 border-yellow-500 font-bold">${escapedValStr}</span>`
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
    class="grid gap-4 p-4 font-sans text-slate-800 bg-slate-50 min-h-screen relative overflow-hidden"
  >
    <!-- Header Area -->
    <div class="bg-white rounded-2xl p-6 shadow-sm border border-slate-200 z-10 relative">
      <div class="flex flex-col md:flex-row md:items-start justify-between gap-4 mb-4">
        <div>
          <div class="flex items-center gap-2 mb-2">
            <svg
              style="width: 24px; height: 24px; flex-shrink: 0"
              class="text-cyan-500"
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
            <h2 class="text-3xl font-bold tracking-tight text-slate-900">AI 템플릿 매니저 PRO</h2>
          </div>
          <p class="text-slate-500 text-sm">
            실시간 플랫폼 시뮬레이터를 활용하여 마케팅 콘텐츠를 기획하세요.
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <div
            v-if="templateData.type"
            class="flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700 border border-slate-200"
          >
            <svg
              style="width: 14px; height: 14px; flex-shrink: 0"
              class="text-cyan-600"
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
            class="flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600 shadow-sm"
          >
            <span>진행률:</span>
            <strong class="text-slate-900">{{ completionRate }}%</strong>
            <svg
              v-if="completionRate === 100"
              style="width: 14px; height: 14px; flex-shrink: 0"
              class="text-cyan-500"
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
            class="flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-600 shadow-sm"
          >
            <span class="relative flex h-2 w-2">
              <span
                class="animate-ping absolute inline-flex h-full w-full rounded-full bg-cyan-300 opacity-75"
              ></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-cyan-500"></span>
            </span>
            <span>실시간 동기화</span>
          </div>
        </div>
      </div>

      <div
        class="flex flex-col md:flex-row md:items-center justify-between gap-4 mt-2 border-t border-slate-100 pt-4"
      >
        <div class="flex p-1 bg-slate-100 rounded-full border border-slate-200 gap-1 w-fit">
          <button
            @click="activeTab = 'write'"
            class="px-5 py-1.5 rounded-full text-sm font-medium transition-all flex items-center gap-2"
            :class="
              activeTab === 'write'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            "
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
              <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
              <rect width="18" height="6" x="3" y="3" rx="2" ry="2" />
              <rect width="6" height="12" x="3" y="9" rx="2" ry="2" />
            </svg>
            템플릿 작성
          </button>
          <button
            @click="activeTab = 'library'"
            class="px-5 py-1.5 rounded-full text-sm font-medium transition-all flex items-center gap-2"
            :class="
              activeTab === 'library'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            "
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
              <path d="m16 6 4 14" />
              <path d="M12 6v14" />
              <path d="M8 8v12" />
              <path d="M4 4v16" />
            </svg>
            라이브러리
          </button>
        </div>

        <div class="flex items-center gap-2">
          <!-- 작업 로그 버튼 -->
          <button
            @click="isLogModalOpen = true"
            class="inline-flex items-center gap-1.5 rounded-lg bg-white border border-slate-200 px-4 py-2 text-sm font-bold text-slate-700 transition hover:bg-slate-50 shadow-sm"
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
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            작업 로그
          </button>
          <button
            @click="addLog('초안이 성공적으로 저장되었습니다.', 'success')"
            class="inline-flex items-center gap-1.5 rounded-lg bg-slate-900 px-4 py-2 text-sm font-bold text-white transition hover:bg-slate-800 shadow-sm"
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
    <div class="flex flex-col lg:flex-row gap-4 items-start w-full z-10 relative">
      <!-- Left Sidebar (입력 및 AI 도우미) -->
      <aside class="w-full lg:w-96 flex-shrink-0 flex flex-col gap-4">
        <article
          class="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm flex flex-col h-80"
        >
          <div class="flex items-center gap-2 mb-4 border-b border-slate-100 pb-3">
            <svg
              style="width: 18px; height: 18px; flex-shrink: 0"
              class="text-cyan-500"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            <h2 class="text-base font-bold text-slate-900">고객 기획 요청 원본</h2>
          </div>
          <textarea
            v-model="clientRequest"
            class="flex-1 w-full bg-slate-50 border border-slate-200 rounded-xl p-3 text-sm text-slate-700 focus:border-cyan-500 focus:bg-white outline-none resize-none transition-colors custom-scrollbar"
            placeholder="고객의 요청 내용을 여기에 자유롭게 입력하세요..."
          ></textarea>
        </article>

        <article class="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm">
          <div class="flex items-center gap-2 mb-4 border-b border-slate-100 pb-3">
            <svg
              style="width: 18px; height: 18px; flex-shrink: 0"
              class="text-cyan-500"
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
            <h3 class="text-base font-bold text-slate-900">AI 템플릿 도우미</h3>
          </div>

          <div class="mb-5">
            <label class="block text-xs font-semibold text-slate-500 mb-2">출력 포맷 지정</label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="fmt in [
                  '인스타그램 피드',
                  '네이버 블로그',
                  '유튜브 커뮤니티',
                  '카카오 알림톡',
                ]"
                :key="fmt"
                @click="selectedFormat = fmt"
                class="px-3 py-1.5 rounded-xl border text-sm font-medium transition-all"
                :class="
                  selectedFormat === fmt
                    ? 'bg-cyan-50 border-cyan-500 text-cyan-700 shadow-sm'
                    : 'bg-white border-slate-200 text-slate-500 hover:bg-slate-50'
                "
              >
                {{ fmt }}
              </button>
            </div>
          </div>

          <button
            @click="handleAIGenerate"
            :disabled="isGenerating || isRefining"
            class="flex w-full items-center justify-center gap-2 rounded-xl bg-cyan-500 px-4 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-cyan-600 disabled:cursor-not-allowed disabled:opacity-70"
          >
            <template v-if="isGenerating">
              <svg
                style="width: 18px; height: 18px; flex-shrink: 0"
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
                style="width: 18px; height: 18px; flex-shrink: 0"
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
              선택한 포맷으로 AI 생성
            </template>
          </button>
        </article>
      </aside>

      <!-- Right Area (라이브러리 OR 에디터/시뮬레이터) -->
      <main
        class="bg-white rounded-2xl border border-slate-200 shadow-sm min-w-0 flex-1 w-full p-5 flex flex-col lg:h-[calc(100vh-180px)] lg:min-h-[640px]"
      >
        <!-- Library View -->
        <template v-if="activeTab === 'library'">
          <div class="flex flex-col h-full min-h-0">
            <div class="mb-5 border-b border-slate-100 pb-4 shrink-0">
              <h3 class="text-lg font-bold text-slate-900">마케팅 템플릿 라이브러리</h3>
              <p class="mt-1 text-sm text-slate-500">채널별로 자주 사용하는 구조를 불러오세요.</p>
            </div>

            <div class="flex-1 overflow-y-auto custom-scrollbar pr-2 pb-4">
              <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">
                <div
                  v-for="tpl in templateLibrary"
                  :key="tpl.id"
                  class="group flex flex-col justify-between rounded-xl border border-slate-200 bg-slate-50 p-5 transition-all hover:border-cyan-500 hover:shadow-md hover:bg-white"
                >
                  <div>
                    <div class="mb-4 flex items-start justify-between gap-3">
                      <div>
                        <span
                          class="rounded-md bg-white border border-slate-200 px-2 py-1 text-xs font-semibold text-slate-600"
                        >
                          {{ tpl.type }}
                        </span>
                        <h4 class="mt-3 text-base font-bold text-slate-900">{{ tpl.title }}</h4>
                        <p class="mt-1 text-sm text-slate-500">{{ tpl.desc }}</p>
                      </div>
                      <div
                        class="rounded-xl bg-white border border-slate-200 p-2 text-slate-400 group-hover:bg-cyan-50 group-hover:border-cyan-200 group-hover:text-cyan-600 transition-colors"
                      >
                        <svg
                          style="width: 20px; height: 20px; flex-shrink: 0"
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
                    <div class="mb-5 flex flex-wrap gap-1.5">
                      <span
                        v-for="tag in tpl.tags"
                        :key="tag"
                        class="rounded-md bg-slate-200/50 px-2.5 py-1 text-xs text-slate-600 font-medium"
                      >
                        #{{ tag }}
                      </span>
                    </div>
                  </div>
                  <button
                    @click="handleLoadTemplate(tpl)"
                    class="w-full rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-bold text-white transition-all group-hover:bg-cyan-500 shadow-sm"
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
          <div class="flex flex-col xl:flex-row gap-5 flex-1 min-h-0">
            <!-- 1. 입력 항목 폼 -->
            <div
              class="w-full xl:w-1/3 flex flex-col border border-slate-200 rounded-2xl bg-white shadow-sm overflow-hidden h-[450px] xl:h-full shrink-0 xl:shrink relative"
            >
              <div
                class="bg-slate-50 p-4 border-b border-slate-200 flex justify-between items-center shrink-0"
              >
                <div class="flex items-center gap-2">
                  <svg
                    style="width: 18px; height: 18px; flex-shrink: 0"
                    class="text-cyan-600"
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
                  <h3 class="font-bold text-slate-900">입력 항목</h3>
                </div>
                <span
                  v-if="templateData.title"
                  class="bg-white border border-slate-200 px-2 py-1 rounded-lg text-xs font-bold text-slate-500"
                >
                  {{ totalCount }}개 변수
                </span>
              </div>

              <div class="flex-1 overflow-y-auto p-5 custom-scrollbar">
                <div
                  v-if="!templateData.content"
                  class="flex h-full flex-col items-center justify-center rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center"
                >
                  <svg
                    style="width: 32px; height: 32px; flex-shrink: 0"
                    class="mb-3 text-slate-400"
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
                  <p class="text-sm font-bold text-slate-600">선택된 템플릿이 없습니다.</p>
                </div>

                <div v-else class="flex flex-col gap-4">
                  <div class="rounded-xl bg-cyan-50 p-4 border border-cyan-100 mb-2">
                    <div class="text-xs font-bold text-cyan-600 mb-1">{{ templateData.type }}</div>
                    <div class="text-base font-bold text-slate-900">{{ templateData.title }}</div>
                  </div>

                  <div class="pb-5 border-b border-slate-100 mb-2">
                    <label class="mb-2 block text-sm font-bold text-slate-500"
                      >새로운 변수 추가</label
                    >
                    <div class="flex items-center gap-2">
                      <input
                        type="text"
                        v-model="newVariableName"
                        @keydown.enter="addVariable"
                        placeholder="항목명 (예: 연락처)"
                        class="flex-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-2.5 text-sm outline-none transition-colors focus:border-cyan-500 focus:bg-white focus:ring-2 focus:ring-cyan-500/20"
                      />
                      <button
                        @click="addVariable"
                        class="flex items-center justify-center gap-1 rounded-xl bg-slate-800 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-slate-700 shadow-sm"
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
                          <path d="M5 12h14" />
                          <path d="M12 5v14" />
                        </svg>
                        추가
                      </button>
                    </div>
                  </div>

                  <!-- 변수 렌더링 영역 -->
                  <div
                    v-for="key in orderedVariableKeys"
                    :key="key"
                    class="transition-all duration-300 relative group"
                  >
                    <div
                      @mouseenter="hoveredVariable = key"
                      @mouseleave="hoveredVariable = null"
                      class="rounded-xl border border-slate-200 bg-white p-3.5 shadow-sm hover:border-cyan-400 transition-colors"
                    >
                      <div class="flex justify-between items-center mb-2.5">
                        <label
                          class="text-[13px] font-bold text-slate-700 flex items-center gap-1.5"
                        >
                          <span
                            class="w-1.5 h-1.5 rounded-full transition-colors duration-300"
                            :class="isFilled(key) ? 'bg-cyan-500' : 'bg-slate-200'"
                          ></span>
                          {{ key }}
                        </label>
                        <button
                          @click="removeVariable(key)"
                          :disabled="isRefining"
                          class="text-slate-300 hover:text-rose-500 opacity-0 group-hover:opacity-100 transition-opacity focus:opacity-100 p-1 disabled:opacity-0"
                          title="변수 삭제"
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
                          :placeholder="`${key} 내용을 입력하세요`"
                          :disabled="isRefining"
                          class="w-full rounded-lg border-0 bg-slate-50 px-3.5 py-2.5 text-[14px] outline-none transition-colors focus:ring-2 focus:ring-cyan-500/20 focus:bg-white text-slate-800 disabled:opacity-50"
                        />
                        <div
                          v-if="isFilled(key)"
                          class="absolute right-3 top-1/2 -translate-y-1/2 text-cyan-500"
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
                            <circle cx="12" cy="12" r="10" />
                            <path d="m9 12 2 2 4-4" />
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
                class="absolute inset-0 bg-white/50 backdrop-blur-[2px] z-30 flex items-center justify-center pointer-events-none"
              ></div>
            </div>

            <!-- 2. 메인 화면 - 템플릿 보기/편집 영역 -->
            <div
              class="flex-1 flex flex-col border border-slate-200 rounded-2xl bg-white shadow-sm overflow-hidden h-[600px] xl:h-full shrink-0 xl:shrink"
            >
              <div
                class="bg-white p-4 border-b border-slate-200 flex justify-between items-center shrink-0 z-20"
              >
                <div class="flex items-center gap-2">
                  <svg
                    style="width: 18px; height: 18px; flex-shrink: 0"
                    class="text-cyan-600"
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
                  <h3 class="font-bold text-slate-900">
                    {{ isEditingTemplate ? '템플릿 구조 원본 편집' : '결과물 미리보기' }}
                  </h3>
                </div>

                <div class="flex items-center gap-2">
                  <!-- 에디터 패널 내부 Undo/Redo 버튼 -->
                  <div
                    class="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg border border-slate-200 mr-2"
                  >
                    <button
                      @click="undo"
                      :disabled="!canUndo || isRefining"
                      class="p-1 rounded-md text-slate-500 hover:bg-white hover:text-slate-900 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-500 transition-all shadow-sm disabled:shadow-none"
                      title="실행 취소 (Ctrl+Z)"
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
                        <path d="M3 7v6h6" />
                        <path d="M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13" />
                      </svg>
                    </button>
                    <button
                      @click="redo"
                      :disabled="!canRedo || isRefining"
                      class="p-1 rounded-md text-slate-500 hover:bg-white hover:text-slate-900 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-500 transition-all shadow-sm disabled:shadow-none"
                      title="다시 실행 (Ctrl+Y)"
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
                        <path d="M21 7v6h-6" />
                        <path d="M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3l3 2.7" />
                      </svg>
                    </button>
                  </div>

                  <button
                    @click="toggleEditTemplate"
                    :disabled="!templateData.content || isRefining"
                    class="rounded-lg border px-3 py-2 text-sm font-bold flex items-center gap-1.5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    :class="
                      isEditingTemplate
                        ? 'bg-cyan-500 text-white border-cyan-600 shadow-sm'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                    "
                  >
                    <template v-if="isEditingTemplate">
                      <svg
                        style="width: 16px; height: 16px; flex-shrink: 0"
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
                      편집 완료
                    </template>
                    <template v-else>
                      <svg
                        style="width: 16px; height: 16px; flex-shrink: 0"
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
                      원본 편집
                    </template>
                  </button>

                  <button
                    @click="openSimulator"
                    :disabled="!templateData.content || isRefining"
                    class="rounded-lg border px-3 py-2 text-sm font-bold flex items-center gap-1.5 transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-slate-900 text-white border-slate-900 shadow-sm hover:bg-slate-800"
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
                      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                    플랫폼 미리보기
                  </button>
                </div>
              </div>

              <div
                class="flex-1 bg-cyan-50/30 flex items-center justify-center p-5 overflow-y-auto relative"
              >
                <div v-if="!templateData.content" class="text-center text-slate-400">
                  <svg
                    style="width: 48px; height: 48px; flex-shrink: 0"
                    class="mb-4 mx-auto opacity-20"
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
                  <p class="text-sm font-bold text-slate-600 mb-1">에디터 대기 중</p>
                </div>

                <div
                  v-else-if="isEditingTemplate"
                  class="absolute inset-5 rounded-xl bg-white border border-cyan-400 overflow-hidden shadow-sm flex flex-col"
                >
                  <div
                    class="bg-cyan-50 px-4 py-2 border-b border-cyan-100 flex justify-between shrink-0"
                  >
                    <span class="text-xs font-bold text-cyan-700"
                      >대괄호 [변수명]을 사용해 템플릿 구조를 변경할 수 있습니다. (저장 시 자동
                      갱신됩니다)</span
                    >
                  </div>
                  <textarea
                    v-model="templateData.content"
                    :disabled="isRefining"
                    class="flex-1 w-full resize-none p-6 text-[15px] leading-relaxed text-slate-800 outline-none custom-scrollbar disabled:opacity-50"
                    placeholder="템플릿 원본을 작성해 주세요..."
                  ></textarea>
                </div>

                <div
                  v-else
                  class="absolute inset-5 rounded-xl bg-white border border-slate-200 overflow-y-auto shadow-sm p-8 custom-scrollbar"
                >
                  <!-- AI 교정 중 표시되는 오버레이 (미리보기 화면 위) -->
                  <div
                    v-if="isRefining"
                    class="absolute inset-0 bg-white/70 backdrop-blur-sm z-10 flex flex-col items-center justify-center transition-all duration-300"
                  >
                    <svg
                      style="width: 40px; height: 40px; flex-shrink: 0"
                      class="text-cyan-500 animate-spin mb-4"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                    </svg>
                    <span class="text-[15px] font-bold text-slate-700"
                      >AI가 문맥을 자연스럽게 수정하고 있습니다...</span
                    >
                  </div>

                  <div
                    class="whitespace-pre-wrap leading-[1.9] text-[15px] text-slate-800 font-medium"
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
          class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
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
            class="relative w-full max-w-[450px] bg-slate-100 h-full flex flex-col shadow-2xl border-l border-slate-200 z-10"
            @click.stop
          >
            <div
              class="bg-white px-5 py-4 border-b border-slate-200 flex justify-between items-center shrink-0"
            >
              <div class="flex items-center gap-2">
                <svg
                  style="width: 20px; height: 20px; flex-shrink: 0"
                  class="text-cyan-500"
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
                <h3 class="text-lg font-bold text-slate-900">플랫폼 미리보기</h3>
              </div>
              <button
                @click="isSimulatorOpen = false"
                class="p-2 text-slate-400 hover:text-slate-700 bg-slate-50 hover:bg-slate-200 rounded-lg transition-colors"
              >
                <svg
                  style="width: 20px; height: 20px; flex-shrink: 0"
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
              class="flex-1 p-6 flex items-center justify-center overflow-y-auto custom-scrollbar"
            >
              <div
                class="bg-white overflow-hidden relative shrink-0 mx-auto"
                style="
                  width: 340px;
                  height: 640px;
                  border-radius: 2.5rem;
                  border: 12px solid #1e293b;
                  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
                "
              >
                <div
                  class="absolute top-0 left-1/2 transform -translate-x-1/2 h-6 bg-[#1e293b] rounded-b-xl z-50 flex items-center justify-center"
                  style="width: 120px"
                >
                  <div class="bg-[#0f172a] rounded-full" style="width: 40px; height: 6px"></div>
                </div>

                <div class="w-full h-full overflow-y-auto custom-scrollbar relative pt-6 bg-white">
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
                          style="width: 24px; height: 24px; flex-shrink: 0"
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
                        <span class="font-bold text-base">{{ currentAuthor }}</span>
                      </div>
                      <svg
                        style="width: 24px; height: 24px; flex-shrink: 0"
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
                            width: 34px;
                            height: 34px;
                          "
                        >
                          <div
                            class="w-full h-full bg-white rounded-full border-2 border-white overflow-hidden flex items-center justify-center"
                          >
                            <div class="w-full h-full bg-gray-200"></div>
                          </div>
                        </div>
                        <span class="font-bold text-[13px]">{{ currentAuthor }}</span>
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
                        <circle cx="12" cy="12" r="1" />
                        <circle cx="19" cy="12" r="1" />
                        <circle cx="5" cy="12" r="1" />
                      </svg>
                    </div>
                    <div
                      class="w-full aspect-square bg-gray-100 flex flex-col items-center justify-center text-gray-400"
                    >
                      <svg
                        style="width: 48px; height: 48px; flex-shrink: 0"
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
                      <span class="text-sm font-bold">이미지 영역</span>
                    </div>
                    <div class="flex items-center justify-between px-3 py-3">
                      <div class="flex gap-4">
                        <svg
                          style="width: 26px; height: 26px; flex-shrink: 0"
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
                          style="width: 26px; height: 26px; flex-shrink: 0; transform: scaleX(-1)"
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
                          style="width: 26px; height: 26px; flex-shrink: 0"
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
                        style="width: 24px; height: 24px; flex-shrink: 0"
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
                      <span class="font-bold text-[13px]">좋아요 1,234개</span>
                    </div>
                    <div class="px-3 pb-6 text-[13px] leading-relaxed whitespace-pre-wrap">
                      <span class="font-bold mr-2">{{ currentAuthor }}</span>
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
                          style="width: 24px; height: 24px; flex-shrink: 0"
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
                      <div class="font-extrabold text-[#03c75a] text-xl tracking-tighter">blog</div>
                      <div style="width: 24px"></div>
                    </div>
                    <div class="px-5 pt-8 pb-6">
                      <div class="text-xs font-bold text-gray-400 mb-2">
                        {{ currentAuthor }}의 블로그
                      </div>
                      <h1 class="text-xl font-bold leading-tight mb-5">
                        {{ templateData.title || '제목이 입력되지 않았습니다.' }}
                      </h1>
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2">
                          <div
                            class="rounded-full bg-gray-200"
                            style="width: 36px; height: 36px"
                          ></div>
                          <div>
                            <div class="text-[13px] font-bold">{{ currentAuthor }}</div>
                            <div class="text-[11px] text-gray-400">1시간 전</div>
                          </div>
                        </div>
                        <button
                          class="flex items-center gap-1 text-xs font-bold text-[#03c75a] bg-[#03c75a]/10 px-3 py-1.5 rounded-full"
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
                            <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <line x1="19" x2="19" y1="8" y2="14" />
                            <line x1="22" x2="16" y1="11" y2="11" />
                          </svg>
                          이웃추가
                        </button>
                      </div>
                    </div>
                    <div class="border-t border-gray-100 mx-5 mb-6"></div>
                    <div
                      class="px-5 pb-10 text-[15px] leading-[1.8] whitespace-pre-wrap text-gray-700"
                      v-html="renderedContentHtml"
                    ></div>
                    <div class="border-t border-gray-100 px-5 py-4 flex gap-4">
                      <button class="flex items-center gap-1.5 text-sm font-bold text-gray-500">
                        <svg
                          style="width: 20px; height: 20px; flex-shrink: 0"
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
                      <button class="flex items-center gap-1.5 text-sm font-bold text-gray-500">
                        <svg
                          style="width: 20px; height: 20px; flex-shrink: 0"
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
                        style="width: 24px; height: 24px; flex-shrink: 0"
                        class="mr-4"
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
                      <span class="font-bold text-lg">{{ currentAuthor }}</span>
                    </div>
                    <div class="flex text-sm font-bold border-b border-gray-800 text-gray-400">
                      <div class="px-4 py-3">홈</div>
                      <div class="px-4 py-3">동영상</div>
                      <div class="px-4 py-3 text-white border-b-2 border-white">커뮤니티</div>
                    </div>
                    <div class="p-4">
                      <div class="flex items-center justify-between mb-3">
                        <div class="flex items-center gap-3">
                          <div
                            class="rounded-full bg-gray-700"
                            style="width: 40px; height: 40px"
                          ></div>
                          <div>
                            <div class="text-[14px] font-bold">{{ currentAuthor }}</div>
                            <div class="text-[12px] text-gray-400">2시간 전</div>
                          </div>
                        </div>
                        <svg
                          style="width: 20px; height: 20px; flex-shrink: 0"
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
                        class="text-[14px] leading-relaxed mb-4 whitespace-pre-wrap"
                        v-html="renderedContentHtml"
                      ></div>
                      <div class="flex items-center gap-6 mt-4">
                        <div class="flex items-center gap-2 text-gray-300">
                          <svg
                            style="width: 20px; height: 20px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M7 10v12" />
                            <path
                              d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2h0a3.13 3.13 0 0 1 3 3.88Z"
                            />
                          </svg>
                          <span class="text-[13px] font-bold">4.2천</span>
                        </div>
                        <div class="text-gray-300">
                          <svg
                            style="width: 20px; height: 20px; flex-shrink: 0"
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
                        <div class="flex items-center gap-2 text-gray-300 ml-auto">
                          <svg
                            style="width: 20px; height: 20px; flex-shrink: 0"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="2"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="m3 21 1.9-5.7a8.5 8.5 0 1 1 3.8 3.8z" />
                          </svg>
                          <span class="text-[13px] font-bold">128</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- Kakao UI -->
                  <div
                    v-else-if="templateData.type === '카카오 알림톡'"
                    class="min-h-full font-sans text-[13px] flex flex-col"
                    style="background-color: #b2c7d9"
                  >
                    <div
                      class="px-4 py-3 flex items-center gap-4 sticky top-0 z-10 border-b border-[#a0b8cc]"
                      style="background-color: rgba(178, 199, 217, 0.95)"
                    >
                      <svg
                        style="width: 24px; height: 24px; flex-shrink: 0"
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
                      <span class="font-bold text-base text-gray-900">{{ currentAuthor }}</span>
                    </div>
                    <div class="p-4 flex-1">
                      <div class="flex justify-center mb-5">
                        <span
                          class="text-white text-[11px] font-bold px-4 py-1.5 rounded-full shadow-sm"
                          style="background-color: rgba(0, 0, 0, 0.2)"
                          >2026년 10월 14일 수요일</span
                        >
                      </div>
                      <div class="flex gap-2">
                        <div
                          class="rounded-2xl flex items-center justify-center shrink-0 shadow-sm text-yellow-900 font-extrabold text-xs"
                          style="width: 40px; height: 40px; background-color: #fee500"
                        >
                          TALK
                        </div>
                        <div class="flex flex-col gap-1 w-full max-w-[260px]">
                          <span class="text-[12px] font-bold text-gray-700 ml-1">{{
                            currentAuthor
                          }}</span>
                          <div
                            class="bg-white rounded-xl rounded-tl-none p-4 shadow-sm text-gray-800"
                          >
                            <div
                              class="flex items-center gap-1.5 mb-3 pb-3 border-b border-gray-100"
                            >
                              <span
                                class="text-[10px] font-extrabold px-1.5 py-0.5 rounded-sm"
                                style="background-color: #fee500; color: #391b1b"
                                >알림톡</span
                              >
                              <span class="text-[11px] text-gray-500 font-bold"
                                >카카오 인증마크</span
                              >
                            </div>
                            <div
                              class="whitespace-pre-wrap leading-relaxed text-[13px]"
                              v-html="renderedContentHtml"
                            ></div>
                            <button
                              class="w-full mt-4 py-2.5 bg-gray-100 rounded-lg text-[13px] font-bold text-gray-700 hover:bg-gray-200 transition-colors"
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
          class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          @click="isLogModalOpen = false"
        ></div>
        <!-- 패널 -->
        <div
          class="relative bg-white rounded-2xl shadow-xl w-full max-w-md max-h-[80vh] flex flex-col transform transition-transform animate-in zoom-in-95"
        >
          <div class="px-5 py-4 border-b border-slate-200 flex justify-between items-center">
            <h3 class="text-lg font-bold text-slate-900 flex items-center gap-2">
              <svg
                style="width: 18px; height: 18px; flex-shrink: 0"
                class="text-slate-500"
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
              작업 로그 내역
            </h3>
            <button
              @click="isLogModalOpen = false"
              class="p-1 text-slate-400 hover:text-slate-700 bg-slate-50 hover:bg-slate-200 rounded-lg transition-colors"
            >
              <svg
                style="width: 18px; height: 18px; flex-shrink: 0"
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
              class="p-8 text-center text-sm text-slate-500 font-medium"
            >
              아직 기록된 로그가 없습니다.
            </div>
            <ul v-else class="space-y-1">
              <li
                v-for="log in logs"
                :key="log.id"
                class="px-4 py-3 rounded-xl flex items-start gap-3 hover:bg-slate-50 transition-colors"
              >
                <span class="text-[11px] font-bold text-slate-400 shrink-0 mt-0.5">{{
                  log.time
                }}</span>
                <span
                  class="text-sm font-medium text-slate-700"
                  :class="{
                    'text-cyan-600': log.type === 'success',
                    'text-rose-500': log.type === 'warning',
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
  border-radius: 6px;
}
.custom-scrollbar:hover::-webkit-scrollbar-thumb {
  background: #94a3b8;
}
</style>
