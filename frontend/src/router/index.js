import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import CalendarView from '@/views/CalendarView.vue'
import OperationsView from '@/views/OperationsView.vue'
import TasksView from '@/views/TasksView.vue'
import TemplatesView from '@/views/TemplatesView.vue'
import ReportsView from '@/views/ReportsView.vue'
import Settings from '@/views/Settings.vue'
import Mypage from '@/views/Mypage.vue'
import Sidebar from '@/components/Sidebar.vue'
import Signup from '@/views/Signup.vue'
import Login from '@/views/Login.vue'
import ContentEditorView from '@/views/ContentEditorView.vue'



const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/calendar',
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DashboardView,
      meta: {
        title: '대시보드',
        section: '운영 플래너',
      },
    },
    {
      path: '/content/editor/:contentId?',
      alias: '/content-editor',
      name: 'content-editor',
      component: ContentEditorView,
      meta: {
        title: '콘텐츠 편집',
        section: '콘텐츠 워크스페이스',
      },
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: CalendarView,
      meta: {
        title: '캘린더',
        section: '운영 플래너',
      },
    },
        {
      path: '/settings',
      name: 'settings',
      component: Settings,
      meta: {
        title: '설정',
        section: '설정',
      },
    },
    {
      path: '/operations',
      name: 'operations',
      component: OperationsView,
      meta: {
        title: '운영 허브',
        section: '고객 · 업무 오케스트레이션',
      },
    },
    {
      path: '/tasks',
      name: 'tasks',
      component: TasksView,
      meta: {
        title: '작업 보드',
        section: '실행 보드',
      },
    },
    {
      path: '/templates',
      name: 'templates',
      component: TemplatesView,
      meta: {
        title: '템플릿',
        section: '콘텐츠 라이브러리',
      },
    },
    {
      path: '/reports',
      name: 'reports',
      component: ReportsView,
      meta: {
        title: '리포트',
        section: '성과 리뷰',
      },
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: {
        title: '로그인',
        section: '로그인',
      },
    },
    {
      path: '/signup',
      name: 'signup',
      component: Signup,
      meta: {
        title: '회원 가입',
        section: '회원 생성',
      },
    },
    {
      path: '/mypage',
      name: 'mypage',
      component: Mypage,
      meta: {
        title: '내 정보',
        section: '내 정보',
      },
    },
  ],
})

export default router
