<script setup>
import { ref, computed, onMounted } from 'vue'
import { getNoti, confirm } from '@/api/notifications/index.js'
import { formatRelativeTime } from '@/utils/datechange.js'

// 1. 알림 데이터 (시스템 로그 및 팀 활동)
const notifications = ref([
  {
    id: 1,
    type: 'qa',
    title: '알림 제목 1',
    message: '노드(Laptop-Worker-01)가 정상적으로 클러스터에 합류했습니다.',
    time: '방금 전',
    isRead: false,
  },
  {
    id: 2,
    type: 'ai',
    title: '템플릿 문맥 교정 완료',
    message: '인스타그램 피드 템플릿의 문맥 교정이 성공적으로 완료되었습니다.',
    time: '12분 전',
    isRead: false,
  },
  {
    id: 3,
    type: 'task',
    title: 'Git 충돌 주의 알림',
    message: 'Magam Fairy 프로젝트의 dev 브랜치에서 컨플릭트 위험이 감지되었습니다.',
    time: '45분 전',
    isRead: true,
  },
  {
    id: 4,
    type: 'task',
    title: 'Longhorn 스토리지 용량 부족',
    message: '현재 스토리지 사용량이 90%를 초과했습니다. 볼륨 정리가 필요합니다.',
    time: '2시간 전',
    isRead: false,
  }
])

const getNotifications = async () => {
  try{
    const res = getNoti()
    notifications.value = res.data;
  }
  catch(e){
    console.error(e);
  }
  finally{
    const res = {
      success:"true",
      status:2000,
      message:"통신 성공",
      data:
      [
        {
        idx: 34897,
        type: "qa",
        created_at: "2026-04-21T10:04:13Z",
        title: "알림 제목 1",
        message: "알림 내용 1",
        isRead: false
        },
        {
        idx: 78354,
        type: "ai",
        created_at: "2026-04-12T12:04:13Z",
        title: "알림 제목2",
        message: "알림 내용 2",
        isRead: false
        },
        {
        idx: 54876,
        type: "task",
        created_at: "2026-04-05T12:04:13Z",
        title: "알림 제목 3",
        message: "알림 내용 3",
        isRead: false
        },
        {
        idx: 45453,
        type: "task",
        created_at: "2026-03-20T12:04:13Z",
        title: "알림 제목 4",
        message: "알림 내용 4",
        isRead: false
        }
      ]
    }
    notifications.value = res.data;
  }
}

const filter = ref('전체') // '전체' | '미확인' | '시스템' | '팀 활동'

// 2. 로직 처리
const filteredNotifications = computed(() => {
  let list = notifications.value
  if (filter.value === '미확인') list = list.filter(n => !n.isRead)
  else if (filter.value !== '전체') list = list.filter(n => n.type === filter.value)
  return list
})

const markAsRead = async (id) => {
  const target = notifications.value.find(n => n.id === id)
  try{
    await confirm(notifications.value[id].idx);
  }
  catch(e){
    console.error(e)
  }
  if (target) target.isRead = true
}

const deleteNotification = (id) => {
  notifications.value = notifications.value.filter(n => n.id !== id)
}

const markAllAsRead = async () => {
  for(let i = 0 ; i < notifications.value.length ; i++){
    try {
        await confirm(notifications.value[i].idx);
    }
    catch(e){
        console.error(e)
    }
  }
  notifications.value.forEach(n => n.isRead = true)
}

// 3. 타입별 컬러 맵핑
const typeStyles = {
  success: 'bg-cyan-500',
  info: 'bg-blue-400',
  warning: 'bg-amber-400',
  error: 'bg-rose-500'
}

onMounted(() => {
  getNotifications();
})
</script>

<template>
  <div class="p-6 bg-slate-50 min-h-screen font-sans text-slate-800">
    <header class="max-w-4xl mx-auto mb-6 bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
      <div class="flex items-center justify-between">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <svg class="w-6 h-6 text-cyan-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
            <h2 class="text-2xl font-bold text-slate-900">알림 센터</h2>
          </div>
          <p class="text-sm text-slate-500">시스템과 팀의 주요 업데이트를 실시간으로 확인하세요.</p>
        </div>
        <button 
          @click="markAllAsRead"
          class="text-xs font-bold text-cyan-600 hover:bg-cyan-50 px-3 py-2 rounded-lg transition-colors"
        >
          모두 읽음 처리
        </button>
      </div>

      <div class="flex gap-2 mt-6">
        <button 
          v-for="tab in ['전체', '미확인', 'ai', 'qa', 'task']" 
          :key="tab"
          @click="filter = tab"
          class="px-4 py-1.5 rounded-full text-xs font-bold transition-all border"
          :class="filter === tab ? 'bg-slate-900 text-white border-slate-900 shadow-sm' : 'bg-white text-slate-500 border-slate-200 hover:bg-slate-50'"
        >
          {{ tab.toUpperCase() }}
          <span v-if="tab === '미확인'" class="ml-1 text-cyan-400">{{ notifications.filter(n => !n.isRead).length }}</span>
        </button>
      </div>
    </header>

    <main class="max-w-4xl mx-auto flex flex-col gap-3">
      <transition-group 
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 translate-y-4"
        leave-active-class="transition duration-200 ease-in"
        leave-to-class="opacity-0 scale-95"
      >
        <article 
          v-for="n in filteredNotifications" 
          :key="n.id"
          class="relative bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all overflow-hidden flex"
          :class="{'opacity-70': n.isRead}"
        >
          <div :class="typeStyles[n.type]" class="w-1.5 shrink-0"></div>

          <div class="p-5 flex-1 flex items-start gap-4">
            <div class="p-2 rounded-xl bg-slate-50 text-slate-400 shrink-0">
              <svg v-if="n.type === '시스템'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
              <svg v-else-if="n.type === 'AI 도우미'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
              <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <h4 class="font-bold text-slate-900 truncate">{{ n.title }}</h4>
                <span v-if="!n.isRead" class="w-1.5 h-1.5 rounded-full bg-cyan-500"></span>
              </div>
              <p class="text-sm text-slate-600 leading-relaxed">{{ n.message }}</p>
              <div class="flex items-center gap-3 mt-3">
                <span class="text-[11px] font-extrabold text-slate-400 uppercase tracking-wider">{{ n.type }}</span>
                <span class="text-[11px] font-medium text-slate-300">{{ formatRelativeTime(n.created_at) }}</span>
              </div>
            </div>

            <div class="flex items-center gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
              <button 
                @click="markAsRead(n.id)" 
                v-if="!n.isRead"
                class="p-2 text-slate-400 hover:text-cyan-500 transition-colors"
                title="읽음 처리"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
              </button>
              <button 
                @click="deleteNotification(n.id)"
                class="p-2 text-slate-400 hover:text-rose-500 transition-colors"
                title="삭제"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              </button>
            </div>
          </div>
        </article>
      </transition-group>

      <div v-if="filteredNotifications.length === 0" class="py-20 text-center">
        <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4 text-slate-300">
          <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg>
        </div>
        <p class="text-slate-500 font-bold">표시할 알림이 없습니다.</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.group:hover .group-hover\:opacity-100 {
  opacity: 1;
}
/* transition-group을 위한 스타일 */
.v-move {
  transition: transform 0.4s ease;
}
</style>