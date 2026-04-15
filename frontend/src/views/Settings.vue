<script setup>
import { reactive, onMounted } from 'vue'

// 1. 상태 관리 (초기값은 API에서 받아오는 것을 가정)
const settings = reactive({
  isDarkMode: false,
  notifications: {
    task: true,
    qa: false,
    ai: true,
    critical: true // 고정값
  }
})

// 2. 초기 로드 시 설정값 동기화
onMounted(async () => {
  try {
    // TODO: 실제 환경에서는 사용자 계정 API를 호출하여 설정값을 덮어씌웁니다.
    // const response = await api.get('/user/settings')
    // Object.assign(settings, response.data)
    
    // 로컬 시스템 설정이나 이전 저장 상태를 확인하여 다크모드 초기화
    if (localStorage.getItem('theme') === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
      settings.isDarkMode = true
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  } catch (error) {
    console.error('설정 정보를 불러오는데 실패했습니다.', error)
  }
})

// 3. 다크모드 토글 및 즉시 저장 (SETTING_003)
const toggleDarkMode = async () => {
  settings.isDarkMode = !settings.isDarkMode
  
  // 화면 즉시 반영
  if (settings.isDarkMode) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }

  // 백엔드 즉시 동기화
  await syncSettingToServer('isDarkMode', settings.isDarkMode)
}

// 4. 알림 설정 토글 및 즉시 저장 (SETTING_004)
const updateNotification = async (type) => {
  settings.notifications[type] = !settings.notifications[type]
  await syncSettingToServer(`notifications.${type}`, settings.notifications[type])
}

// 5. 서버 동기화 모의 함수
const syncSettingToServer = async (key, value) => {
  try {
    // TODO: 실제 백엔드 연동 코드 (Spring Boot Controller 향)
    // await api.patch('/user/settings', { [key]: value })
    console.log(`[API Call] 설정 업데이트 됨 - ${key}: ${value}`)
  } catch (error) {
    console.error('설정 저장 실패:', error)
    // 에러 시 롤백 로직 추가 가능
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900 py-10 px-4 sm:px-6 lg:px-8 transition-colors duration-200">
    <div class="max-w-3xl mx-auto space-y-8">
      
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">환경 설정</h1>
        <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">
          Callog의 테마와 알림 수신 여부를 개인 환경에 맞게 관리하세요.
        </p>
      </div>

      <section class="bg-white dark:bg-gray-800 shadow-sm sm:rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
        <div class="px-4 py-5 sm:px-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-medium leading-6 text-gray-900 dark:text-white">테마 및 UI 설정</h2>
        </div>
        <div class="px-4 py-5 sm:p-6 space-y-6">
          <div class="flex items-center justify-between">
            <div>
              <label class="text-sm font-medium text-gray-900 dark:text-gray-200">다크 모드</label>
              <p class="text-sm text-gray-500 dark:text-gray-400">시스템 전체 UI를 어두운 테마로 변경합니다.</p>
            </div>
            <button 
              type="button" 
              @click="toggleDarkMode"
              :class="[settings.isDarkMode ? 'bg-indigo-600' : 'bg-gray-200 dark:bg-gray-600', 'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2 dark:focus:ring-offset-gray-900']"
              role="switch" 
              :aria-checked="settings.isDarkMode"
            >
              <span class="sr-only">다크 모드 설정</span>
              <span 
                :class="[settings.isDarkMode ? 'translate-x-5' : 'translate-x-0', 'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out']"
              ></span>
            </button>
          </div>
        </div>
      </section>

      <section class="bg-white dark:bg-gray-800 shadow-sm sm:rounded-lg overflow-hidden border border-gray-200 dark:border-gray-700">
        <div class="px-4 py-5 sm:px-6 border-b border-gray-200 dark:border-gray-700">
          <h2 class="text-lg font-medium leading-6 text-gray-900 dark:text-white">알림 수신 설정</h2>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">수신하고 싶은 알림 유형을 선택하세요.</p>
        </div>
        
        <ul role="list" class="divide-y divide-gray-200 dark:divide-gray-700">
          
          <li class="px-4 py-5 sm:p-6 flex items-center justify-between">
            <div>
              <label class="text-sm font-medium text-gray-900 dark:text-gray-200">업무 알림</label>
              <p class="text-sm text-gray-500 dark:text-gray-400">신규 업무 생성, 배정, 상태 변경 알림</p>
            </div>
            <button 
              type="button" 
              @click="updateNotification('task')"
              :class="[settings.notifications.task ? 'bg-indigo-600' : 'bg-gray-200 dark:bg-gray-600', 'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2 dark:focus:ring-offset-gray-900']"
            >
              <span :class="[settings.notifications.task ? 'translate-x-5' : 'translate-x-0', 'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out']"></span>
            </button>
          </li>

          <li class="px-4 py-5 sm:p-6 flex items-center justify-between">
            <div>
              <label class="text-sm font-medium text-gray-900 dark:text-gray-200">QA 알림</label>
              <p class="text-sm text-gray-500 dark:text-gray-400">검수 결과 및 버그 수정 요청 알림</p>
            </div>
            <button 
              type="button" 
              @click="updateNotification('qa')"
              :class="[settings.notifications.qa ? 'bg-indigo-600' : 'bg-gray-200 dark:bg-gray-600', 'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2 dark:focus:ring-offset-gray-900']"
            >
              <span :class="[settings.notifications.qa ? 'translate-x-5' : 'translate-x-0', 'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out']"></span>
            </button>
          </li>

          <li class="px-4 py-5 sm:p-6 flex items-center justify-between">
            <div>
              <label class="text-sm font-medium text-gray-900 dark:text-gray-200">AI 분석 알림</label>
              <p class="text-sm text-gray-500 dark:text-gray-400">리스크 감지 및 업무 가이드 생성 알림</p>
            </div>
            <button 
              type="button" 
              @click="updateNotification('ai')"
              :class="[settings.notifications.ai ? 'bg-indigo-600' : 'bg-gray-200 dark:bg-gray-600', 'relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2 dark:focus:ring-offset-gray-900']"
            >
              <span :class="[settings.notifications.ai ? 'translate-x-5' : 'translate-x-0', 'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out']"></span>
            </button>
          </li>

          <li class="px-4 py-5 sm:p-6 flex items-center justify-between opacity-75">
            <div>
              <div class="flex items-center gap-2">
                <label class="text-sm font-medium text-gray-900 dark:text-gray-200">중요 알림</label>
                <span class="inline-flex items-center rounded-md bg-red-50 px-2 py-1 text-xs font-medium text-red-700 ring-1 ring-inset ring-red-600/10 dark:bg-red-400/10 dark:text-red-400 dark:ring-red-400/20">필수</span>
              </div>
              <p class="text-sm text-gray-500 dark:text-gray-400">마감 임박, 업무 지연 등 크리티컬한 알림은 해제할 수 없습니다.</p>
            </div>
            <button 
              type="button" 
              disabled
              class="bg-indigo-600 opacity-60 cursor-not-allowed relative inline-flex h-6 w-11 flex-shrink-0 rounded-full border-2 border-transparent"
            >
              <span class="translate-x-5 pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0"></span>
            </button>
          </li>

        </ul>
      </section>
      
    </div>
  </div>
</template>