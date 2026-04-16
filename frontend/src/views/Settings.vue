<script setup>
import { reactive, onMounted } from 'vue'

const settings = reactive({ //gg
  notifications: {
    task: true,
    qa: false,
    ai: true,
    critical: true
  }
})

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
    <div class="bg-white rounded-[26px] p-8 shadow-sm border border-slate-200">
      <div class="mb-2">
        <div class="flex items-center gap-3 mb-2">
          <h2 class="text-3xl font-bold tracking-tight text-slate-900">환경 설정</h2>
          <div class="p-2 rounded-full bg-slate-50 border border-slate-100 text-slate-400">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
        </div>
        <p class="text-slate-500 text-sm leading-relaxed">
          Callog의 알림 수신 여부를 개인 업무 환경에 맞게 최적화하세요.
        </p>
      </div>
    </div>

    <div class="bg-white rounded-[26px] border border-slate-200 shadow-sm overflow-hidden">
      <div class="px-8 py-6 border-b border-slate-100 bg-slate-50/50">
        <h3 class="text-lg font-bold text-slate-900">다크 모드 설정</h3>
        <p class="text-sm text-slate-500">다크 모드를 토글합니다.</p>
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
              :class="[settings.notifications[key] ? 'translate-x-6' : 'translate-x-0', 'pointer-events-none inline-block h-[30px] w-[30px] transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out']"
            />
          </button>
        </div>

      </div>
    </div>

    <div class="flex flex-col items-center gap-4">
      <div class="h-px w-16 bg-slate-200" />
      <p class="text-center text-xs font-medium text-slate-400 leading-relaxed">
        설정값은 변경 즉시 계정에 저장되며 모든 기기에 동기화됩니다.<br/>
        도움이 필요하시면 고객 지원 센터로 문의해 주세요.
      </p>
    </div>
  </section>
</template>

<style scoped>
/* 기존 테마의 부드러운 폰트 및 베이스 스타일 유지 */
section {
  font-family: 'Pretendard', system-ui, sans-serif;
}
</style>