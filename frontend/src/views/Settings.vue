<script setup>
import { reactive, onMounted } from 'vue'
import { usePlannerStore } from '@/stores/planner'
import { getSettings } from '@/api/settings/index.js'

const store = usePlannerStore()

const notificationItems = {
    task: { label: '업무 알림', desc: '신규 업무 생성, 배정, 상태 변경 알림' },
    qa: { label: 'QA 알림', desc: '검수 결과 및 버그 수정 요청 알림' },
    ai: { label: 'AI 분석 알림', desc: '리스크 감지 및 업무 가이드 생성 알림' }
  }

const settings = reactive({
  darkMode: false, // 다크 모드 상태 추가
  notifications: {
    task: true,
    qa: true,
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
    const res = await getSettings()
    settings.value = res
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
  <section class="grid gap-6 p-6 max-w-4xl mx-auto transition-colors duration-300">
    <div 
      class="rounded-[26px] border shadow-sm overflow-hidden transition-all"
      :class="settings.darkMode ? 'bg-[#1e1e2d] border-[#2d2d3f]' : 'bg-white border-slate-200'"
    >
      <div 
        class="px-8 py-6 border-b flex items-center justify-between transition-colors"
        :class="settings.darkMode ? 'border-[#2d2d3f] bg-[#252537]' : 'border-slate-100 bg-slate-50/50'"
      >
        <div>
          <h3 class="text-lg font-bold" :class="settings.darkMode ? 'text-white' : 'text-slate-900'">다크 모드 설정</h3>
          <p class="text-sm" :class="settings.darkMode ? 'text-slate-400' : 'text-slate-500'">화면 테마를 어둡게 전환하여 눈의 피로를 줄입니다.</p>
        </div>
        
        <button 
          type="button" 
          @click="toggleDarkMode"
          class="relative inline-flex h-[30px] w-[54px] shrink-0 cursor-pointer rounded-full border-4 border-transparent transition-colors duration-200 ease-in-out focus:outline-none"
          :style="{ backgroundColor: settings.darkMode ? '#59c36d' : '#e2e8f0' }"
        >
          <span 
            :class="[settings.darkMode ? 'translate-x-6' : 'translate-x-0', 'flex items-center justify-center h-[22px] w-[22px] transform rounded-full bg-white shadow-lg transition duration-200']"
          ></span>
        </button>
      </div>

      <div 
        class="px-8 py-6 border-b transition-colors"
        :class="settings.darkMode ? 'border-[#2d2d3f] bg-[#252537]' : 'border-slate-100 bg-slate-50/50'"
      >
        <h3 class="text-lg font-bold" :class="settings.darkMode ? 'text-white' : 'text-slate-900'">알림 수신 설정</h3>
        <p class="text-sm" :class="settings.darkMode ? 'text-slate-400' : 'text-slate-500'">수신하고 싶은 알림 유형을 선택하세요.</p>
      </div>
      
      <div class="divide-y transition-colors" :class="settings.darkMode ? 'divide-[#2d2d3f]' : 'divide-slate-100'">
        <div 
          v-for="(info, key) in notificationItems" 
          :key="key"
          class="px-8 py-6 flex items-center justify-between transition-colors"
          :class="settings.darkMode ? 'hover:bg-[#252537]/50' : 'hover:bg-slate-50/30'"
        >
          <div>
            <strong class="block text-[15px] font-bold mb-1" :class="settings.darkMode ? 'text-slate-200' : 'text-slate-900'">{{ info.label }}</strong>
            <p class="text-sm font-medium" :class="settings.darkMode ? 'text-slate-500' : 'text-slate-400'">{{ info.desc }}</p>
          </div>
          
          <button 
            type="button" 
            @click="updateNotification(key)"
            class="relative inline-flex h-[30px] w-[54px] shrink-0 cursor-pointer rounded-full border-4 border-transparent transition-colors duration-200"
            :style="{ backgroundColor: settings.notifications[key] ? '#59c36d' : (settings.darkMode ? '#3f3f56' : '#e2e8f0') }"
          >
            <span :class="[settings.notifications[key] ? 'translate-x-6' : 'translate-x-0', 'inline-block h-[22px] w-[22px] transform rounded-full bg-white shadow-lg transition duration-200']" />
          </button>
        </div>
      </div>
    </div>
  </section>
</template>


<style scoped>
section {
  font-family: 'Pretendard', system-ui, sans-serif;
  /* 페이지 전체 부드러운 전환 효과 */
  transition: background-color 0.3s ease;
}

/* 다크모드 시 카드에 헤더와 유사한 글로우 효과 추가 */
.bg-white.rounded-\[26px\] {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

:deep(.settings-card-dark) {
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

/* 스위치 버튼에 약간의 입체감 부여 */
button span {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 모바일 대응 */
@media (max-width: 640px) {
  .px-8 {
    padding-left: 1.25rem;
    padding-right: 1.25rem;
  }
}
</style>