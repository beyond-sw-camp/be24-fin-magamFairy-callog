<script setup>
import { reactive, onMounted } from 'vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

const settings = reactive({
  darkMode: false, // 다크 모드 상태 추가
  notifications: {
    task: true,
    qa: false,
    ai: true,
    critical: true
  }
})

if (store.theme=='dark'){
  settings.darkMode = true;
}

// 다크 모드 토글 함수
const toggleDarkMode = () => {
  settings.darkMode = !settings.darkMode
  store.toggleTheme()
  syncSettingToServer('darkMode', settings.darkMode)
}

onMounted(async () => {
  try {
    console.log('설정 정보를 불러왔습니다.')
  } catch (error) {
    console.error('설정 정보를 불러오는데 실패했습니다.', error)
  }
})

const updateNotification = async (type) => {
  if (type === 'critical') return 
  settings.notifications[type] = !settings.notifications[type]
  await syncSettingToServer(`notifications.${type}`, settings.notifications[type])
}

const syncSettingToServer = async (key, value) => {
  try {
    console.log(`[API Call] 설정 업데이트 됨 - ${key}: ${value}`)
  } catch (error) {
    console.error('설정 저장 실패:', error)
  }
}
</script>

<template>
  <section class="grid gap-6 p-6 max-w-4xl mx-auto">
    <div class="bg-white rounded-[26px] border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-8 py-6 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
        <div>
          <h3 class="text-lg font-bold text-slate-900">다크 모드 설정</h3>
          <p class="text-sm text-slate-500">화면 테마를 어둡게 전환하여 눈의 피로를 줄입니다.</p>
        </div>
        
        <button 
          type="button" 
          @click="toggleDarkMode"
          class="relative inline-flex h-[30px] w-[54px] shrink-0 cursor-pointer rounded-full border-4 border-transparent transition-colors duration-200 ease-in-out focus:outline-none"
          :style="{ backgroundColor: settings.darkMode ? '#59c36d' : '#e2e8f0' }"
        >
          <span 
            :class="[settings.darkMode ? 'translate-x-6' : 'translate-x-0', 'pointer-events-none flex items-center justify-center h-[22px] w-[22px] transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out mt-[0px]']"
          >
          </span>
        </button>
      </div>

      <div class="px-8 py-6 border-b border-slate-100 bg-slate-50/50">
        <h3 class="text-lg font-bold text-slate-900">알림 수신 설정</h3>
        <p class="text-sm text-slate-500">수신하고 싶은 알림 유형을 선택하세요.</p>
      </div>
      
      <div class="divide-y divide-slate-100">
        <div 
          v-for="(info, key) in {
            task: { label: '업무 알림', desc: '신규 업무 생성, 배정, 상태 변경 알림' },
            qa: { label: 'QA 알림', desc: '검수 결과 및 버그 수정 요청 알림' },
            ai: { label: 'AI 분석 알림', desc: '리스크 감지 및 업무 가이드 생성 알림' }
          }" 
          :key="key"
          class="px-8 py-6 flex items-center justify-between hover:bg-slate-50/30 transition-colors"
        >
          <div>
            <strong class="block text-[15px] font-bold text-slate-900 mb-1">{{ info.label }}</strong>
            <p class="text-sm text-slate-400 font-medium">{{ info.desc }}</p>
          </div>
          
          <button 
            type="button" 
            @click="updateNotification(key)"
            class="relative inline-flex h-[30px] w-[54px] shrink-0 cursor-pointer rounded-full border-4 border-transparent transition-colors duration-200 ease-in-out focus:outline-none"
            :style="{ backgroundColor: settings.notifications[key] ? '#59c36d' : '#e2e8f0' }"
          >
            <span 
              :class="[settings.notifications[key] ? 'translate-x-6' : 'translate-x-0', 'pointer-events-none inline-block h-[22px] w-[22px] transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out']"
            />
          </button>
        </div>
      </div>
    </div>

    <div class="flex flex-col items-center gap-4 h-20" />
  </section>
</template>

<style scoped>
/* 기존 테마의 부드러운 폰트 및 베이스 스타일 유지 */
section {
  font-family: 'Pretendard', system-ui, sans-serif;
}
</style>