import { createRouter, createWebHistory } from 'vue-router'
import DefaultLayout from '@/layout/DefaultLayout.vue'
import FirstLayout from '@/layout/FirstLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: DefaultLayout,
      redirect:'dashboard',
      children: [
            {
      path: 'dashboard',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: {
        title: '대시보드',
        section: '운영 플래너',
      },
    },
    {
      path: 'content/editor/:contentId?',
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
      component: () => import('@/views/CalendarView.vue'),
      meta: {
        title: '캘린더',
        section: '운영 플래너',
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
      path: 'operations',
      name: 'operations',
      component: () => import('@/views/OperationsView.vue'),
      meta: {
        title: '운영 허브',
        section: '고객 · 업무 오케스트레이션',
      },
    },
    {
      path: 'tasks',
      name: 'tasks',
      component: () => import('@/views/TasksView.vue'),
      meta: {
        title: '작업 보드',
        section: '실행 보드',
      },
    },
    { //ss
      path: 'templates',
      name: 'templates',
      component: () => import('@/views/TemplatesView.vue'),
      meta: {
        title: '템플릿',
        section: '콘텐츠 라이브러리',
      },
    },
    {
      path: 'reports',
      name: 'reports',
      component: () => import('@/views/ReportsView.vue'),
      meta: {
        title: '리포트',
        section: '성과 리뷰',
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
      ]
    },
    {
      path: '/user',
      component: FirstLayout,
      redirect:'user/login',
      children: [
        {
      path: 'login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: {
        title: '로그인',
        section: '로그인',
      },
    },
    {
      path: 'signup',
      name: 'signup',
      component: () => import('@/views/Signup.vue'),
      meta: {
        title: '회원 가입',
        section: '회원 생성',
      },
    },
      ]
    },
  ],
})

export default router
