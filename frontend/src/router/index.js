import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import CalendarView from '@/views/CalendarView.vue'
import TasksView from '@/views/TasksView.vue'
import TemplatesView from '@/views/TemplatesView.vue'
import ReportsView from '@/views/ReportsView.vue'
import Settings from '@/views/Settings.vue'

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
  ],
})

export default router
