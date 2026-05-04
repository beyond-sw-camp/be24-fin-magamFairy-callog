import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '@/layout/DefaultLayout.vue'
import FirstLayout from '@/layout/FirstLayout.vue'
import { useAuthStore } from '@/stores/useAuthStore'

const routes = [
  {
    path: '/',
    component: DefaultLayout,
    redirect: { name: 'dashboard' },
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/HqDashboardView.vue'),
        meta: {
          title: '메인',
          section: '본사 통합 대시보드',
        },
      },
      {
        path: 'campaign-folder',
        name: 'campaign-folder',
        component: () => import('@/views/CampaignFolderView.vue'),
        meta: {
          title: '캠페인 보관함',
          section: '완료 캠페인 보관함',
        },
      },
      {
        path: 'campaigns/:campaignId',
        name: 'campaign-detail',
        component: () => import('@/views/CampaignDetailView.vue'),
        meta: {
          title: '캠페인 상세',
          section: '캠페인 운영 보드',
        },
      },
      {
        path: 'content/new',
        name: 'content-create',
        component: () => import('@/views/ContentEditorView.vue'),
        meta: { title: '새 콘텐츠 작성' }
      },
      {
        path: 'content/:contentId?',
        alias: 'content-editor',
        name: 'content-editor',
        component: () => import('@/views/ContentEditorView.vue'),
        meta: {
          title: '콘텐츠 편집',
          section: '콘텐츠 워크스페이스',
        },
      },
      {
        path: 'calendar',
        name: 'calendar',
        component: () => import('@/views/OverView.vue'),
        meta: {
          title: '캘린더',
          section: '운영 플래너',
        },
      },
      {
        path: 'team-board',
        name: 'team-board',
        component: () => import('@/views/TeamBoardView.vue'),
        meta: {
          title: '팀 보드',
          section: '전체 협업 팀보드',
        },
      },
      {
        path: 'overview',
        name: 'overview',
        component: () => import('@/views/OverView.vue'),
        meta: {
          title: '오버뷰',
          section: '전체 일정 모아보기',
        },
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/Settings.vue'),
        meta: {
          title: '설정',
          section: '설정',
        },
      },
      {
        path: 'usercreate',
        name: 'user-provisioning',
        component: () => import('@/views/UserProvisioningView.vue'),
        meta: {
          requiresAccountCreator: true,
          title: '인사관리',
          section: '계정 생성 · 계정 삭제 · 비밀번호 재발급',
        },
      },
      {
        path: 'frames',
        name: 'frames',
        component: () => import('@/views/FramesView.vue'),
        meta: {
          title: '캠페인 프레임',
          section: '??? ?? ??',
        
        },
      },
      {
        path: '/references',
        name: 'references',
        component: () => import('@/views/ReferencesView.vue'),
        meta: {
          title: '레퍼런스',
          section: '캠페인 레퍼런스',
        },
      },
      {
        path: 'mypage',
        name: 'mypage',
        component: () => import('@/views/Mypage.vue'),
        meta: {
          title: '내 정보',
          section: '내 정보',
        },
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('@/views/NotificationCenter.vue'),
        meta: {
          title: '알림 센터',
          section: '알림 전체를 확인하고 관리하는 곳',
        },
      },
      {
        path: 'matching',
        name: 'matching',
        component: () => import('@/views/MatchOverview.vue'),
        meta: {
          title: '매칭',
          section: '매치 엔진',
        },
      },
    ],
  },
  {
    path: '/user',
    component: FirstLayout,
    redirect: { name: 'login' },
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/Login.vue'),
        meta: {
          guestOnly: true,
          title: '로그인',
          section: '로그인',
        },
      },
      {
        path: 'signup',
        name: 'partner-signup',
        component: () => import('@/views/SignupView.vue'),
        meta: {
          title: '제휴사 회원가입',
        },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.isHydrated) {
    authStore.restore()
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiresAccountCreator = to.matched.some((record) => record.meta.requiresAccountCreator)
  const guestOnly = to.matched.some((record) => record.meta.guestOnly)

  if (requiresAuth || requiresAccountCreator) {
    const isAuthenticated = await authStore.ensureAuthenticated()

    if (!isAuthenticated) {
      return {
        name: 'login',
        query: {
          redirect: to.fullPath,
        },
      }
    }
  }

  if ((requiresAuth || requiresAccountCreator) && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (requiresAccountCreator && !authStore.canCreateUsers) {
    return { name: 'dashboard' }
  }

  if (guestOnly && authStore.hasFreshAccessToken()) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
