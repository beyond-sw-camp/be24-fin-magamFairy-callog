<script setup>
import { computed } from 'vue'
import { useOperationsStore } from '@/stores/operations'
import { usePlannerStore } from '@/stores/planner'
import { formatShortDate } from '@/utils/calendar'

const store = useOperationsStore()
const planner = usePlannerStore()

const panelClass =
  'grid gap-[0.75rem] rounded-[6px] border border-[color:var(--border-strong)] bg-[linear-gradient(180deg,color-mix(in_srgb,var(--panel-color)_96%,white),var(--panel-color))] px-[0.95rem] py-[0.95rem] shadow-[inset_0_1px_0_rgba(255,255,255,0.72),0_8px_20px_rgba(15,23,42,0.05)]'
const toolbarButtonClass =
  'inline-flex min-h-8 items-center justify-center rounded-[4px] px-3.5 text-sm font-bold text-[color:var(--muted-text)] transition duration-200'
const toolbarWrapClass =
  'inline-flex items-center gap-1 rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_82%,white)] p-[3px] shadow-[inset_0_1px_0_rgba(255,255,255,0.58)]'
const cardClass =
  'grid gap-[0.45rem] rounded-[5px] border border-[color:var(--border-strong)] px-[0.9rem] py-[0.9rem] shadow-[inset_0_1px_0_rgba(255,255,255,0.56)]'
const infoCardClass =
  'grid gap-[0.85rem] rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_76%,white)] px-[0.9rem] py-[0.9rem] shadow-[inset_0_1px_0_rgba(255,255,255,0.56)]'
const chipClass =
  'inline-flex min-h-[1.95rem] items-center justify-center rounded-[4px] border border-[color:var(--border-strong)] px-3 text-[0.78rem] font-bold'
const primaryButtonClass =
  'inline-flex min-h-[2.5rem] items-center justify-center gap-2 rounded-[5px] border border-[color:color-mix(in_srgb,var(--accent-color)_50%,var(--border-strong))] bg-[linear-gradient(180deg,color-mix(in_srgb,var(--accent-color)_88%,white),var(--accent-color))] px-4 text-sm font-semibold text-white transition duration-200 hover:-translate-y-px hover:shadow-[0_10px_22px_rgba(15,23,42,0.14)] disabled:cursor-not-allowed disabled:opacity-45 disabled:hover:translate-y-0'
const softButtonClass =
  'inline-flex min-h-[2.45rem] items-center justify-center gap-2 rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_82%,white)] px-4 text-sm font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:shadow-[0_8px_20px_rgba(15,23,42,0.08)]'
const ghostButtonClass =
  'inline-flex min-h-[2.45rem] items-center justify-center gap-2 rounded-[5px] border border-[color:var(--border-strong)] bg-transparent px-4 text-sm font-semibold text-[color:var(--text-primary)] transition duration-200 hover:-translate-y-px hover:bg-[color:color-mix(in_srgb,var(--panel-muted)_76%,white)]'
const focusStyle = {
  borderColor: 'color-mix(in srgb, var(--accent-color) 34%, var(--border-strong))',
  boxShadow: '0 0 0 3px color-mix(in srgb, var(--accent-color) 10%, transparent)',
}

const selectedTeamTaskMember = computed(() =>
  store.findMember(store.selectedTeamTask?.assigneeId ?? null),
)

function openPlannerTask(taskId) {
  if (!taskId) {
    return
  }

  planner.openTask(taskId)
}

function chipStyle(tone = 'neutral') {
  if (tone === 'healthy') {
    return {
      background: 'color-mix(in srgb, var(--success-color) 12%, white)',
      borderColor: 'color-mix(in srgb, var(--success-color) 30%, var(--border-strong))',
      color: '#287b47',
    }
  }

  return {
    background: 'var(--panel-muted)',
    borderColor: 'var(--border-strong)',
    color: 'var(--muted-text)',
  }
}

function personalTaskCardStyle(task) {
  const accent = task.palette?.accent ?? '#2f80ed'

  return {
    borderColor: `color-mix(in srgb, ${accent} 22%, var(--border-strong))`,
    background: `linear-gradient(180deg, color-mix(in srgb, ${accent} 8%, white), var(--panel-color)), var(--panel-color)`,
  }
}

function teamTaskStyle(isActive) {
  if (!isActive) {
    return {
      background: 'color-mix(in srgb, var(--panel-muted) 74%, white)',
    }
  }

  return {
    borderColor: 'color-mix(in srgb, var(--accent-color) 34%, var(--border-strong))',
    background: 'color-mix(in srgb, var(--accent-color) 10%, var(--panel-color))',
  }
}
</script>

<template>
  <section class="grid gap-[0.85rem]">
    <div
      class="flex items-center justify-between gap-[0.75rem] max-[980px]:flex-col max-[980px]:items-start"
    >
      <h3 class="text-base font-semibold text-[color:var(--text-primary)]">
        {{ store.tasknoteMode === 'personal' ? '개인' : '팀' }}
      </h3>

      <div class="flex flex-wrap items-center gap-2">
        <div :class="toolbarWrapClass" role="tablist" aria-label="업무일지 보기 전환">
          <button
            type="button"
            :class="[
              toolbarButtonClass,
              store.tasknoteMode === 'personal'
                ? 'bg-[color:color-mix(in_srgb,var(--panel-color)_96%,white)] text-[color:var(--text-primary)] shadow-[inset_0_1px_0_rgba(255,255,255,0.72),0_4px_12px_rgba(15,23,42,0.06)]'
                : '',
            ]"
            @click="store.setTasknoteMode('personal')"
          >
            개인
          </button>
          <button
            type="button"
            :class="[
              toolbarButtonClass,
              store.tasknoteMode === 'team'
                ? 'bg-[color:color-mix(in_srgb,var(--panel-color)_96%,white)] text-[color:var(--text-primary)] shadow-[inset_0_1px_0_rgba(255,255,255,0.72),0_4px_12px_rgba(15,23,42,0.06)]'
                : '',
            ]"
            @click="store.setTasknoteMode('team')"
          >
            팀
          </button>
        </div>

        <div v-if="store.tasknoteMode === 'personal'" :class="toolbarWrapClass">
          <button
            type="button"
            :class="[
              toolbarButtonClass,
              store.tasknoteListFilter === 'all'
                ? 'bg-[color:color-mix(in_srgb,var(--panel-color)_96%,white)] text-[color:var(--text-primary)] shadow-[inset_0_1px_0_rgba(255,255,255,0.72),0_4px_12px_rgba(15,23,42,0.06)]'
                : '',
            ]"
            @click="store.setTasknoteListFilter('all')"
          >
            전체
          </button>
          <button
            type="button"
            :class="[
              toolbarButtonClass,
              store.tasknoteListFilter === 'completed'
                ? 'bg-[color:color-mix(in_srgb,var(--panel-color)_96%,white)] text-[color:var(--text-primary)] shadow-[inset_0_1px_0_rgba(255,255,255,0.72),0_4px_12px_rgba(15,23,42,0.06)]'
                : '',
            ]"
            @click="store.setTasknoteListFilter('completed')"
          >
            완료
          </button>
        </div>
      </div>
    </div>

    <div
      v-if="store.tasknoteMode === 'personal'"
      class="grid gap-[0.85rem] [grid-template-columns:minmax(0,1.15fr)_minmax(300px,340px)] max-[1380px]:grid-cols-1"
    >
      <article
        :class="panelClass"
        :style="store.focusTarget === 'tasknotes-list' ? focusStyle : null"
      >
        <header
          class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
        >
          <h4 class="text-base font-semibold text-[color:var(--text-primary)]">내 업무 카드</h4>
        </header>

        <div class="grid gap-[0.85rem]">
          <article
            v-for="task in store.filteredPersonalTaskNotes"
            :key="task.id"
            :class="cardClass"
            :style="personalTaskCardStyle(task)"
          >
            <div
              class="flex items-start justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
            >
              <div class="grid gap-1">
                <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                  task.title
                }}</strong>
                <p class="text-sm text-[color:var(--muted-text)]">
                  {{ task.customer }} · {{ task.contentType }}
                </p>
              </div>

              <div class="flex flex-wrap gap-2">
                <span
                  :class="chipClass"
                  :style="chipStyle(task.isMarkedComplete ? 'healthy' : 'neutral')"
                >
                  {{ task.isMarkedComplete ? '완료 표시' : planner.statusLabels[task.status] }}
                </span>
                <span :class="chipClass" :style="chipStyle()">{{
                  planner.priorityLabels[task.priority]
                }}</span>
              </div>
            </div>

            <div
              class="flex items-center justify-between gap-3 text-sm text-[color:var(--muted-text)] max-[980px]:flex-col max-[980px]:items-start"
            >
              <span>{{ formatShortDate(task.dueDate) }}</span>
              <small>{{ task.timeRange }}</small>
            </div>

            <div
              class="flex items-center justify-end gap-3 max-[980px]:flex-col max-[980px]:items-stretch"
            >
              <button :class="softButtonClass" type="button" @click="openPlannerTask(task.id)">
                카드 열기
              </button>

              <template v-if="store.activeRole !== 'admin'">
                <button
                  :class="ghostButtonClass"
                  type="button"
                  @click="store.requestNextStatus(task.id)"
                >
                  다음 단계 요청
                </button>
                <button
                  :class="primaryButtonClass"
                  type="button"
                  @click="store.toggleTaskCompletion(task.id)"
                >
                  {{ task.isMarkedComplete ? '완료 취소' : '완료 표시' }}
                </button>
              </template>
            </div>
          </article>

          <div
            v-if="!store.filteredPersonalTaskNotes.length"
            class="rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_78%,white)] px-4 py-4 text-sm text-[color:var(--muted-text)] shadow-[inset_0_1px_0_rgba(255,255,255,0.52)]"
          >
            표시할 업무가 없습니다.
          </div>
        </div>
      </article>

      <aside
        :class="panelClass"
        :style="store.focusTarget === 'tasknotes-requests' ? focusStyle : null"
      >
        <header
          class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
        >
          <h4 class="text-base font-semibold text-[color:var(--text-primary)]">요청 큐</h4>

          <button
            v-if="store.activeRole === 'admin'"
            :class="primaryButtonClass"
            type="button"
            :disabled="!store.pendingStatusRequests[0]"
            @click="store.applyStatusRequest(store.pendingStatusRequests[0]?.id)"
          >
            요청 반영
          </button>
        </header>

        <div class="grid gap-[0.85rem]">
          <article
            v-for="request in store.pendingStatusRequests"
            :key="request.id"
            :class="infoCardClass"
            :style="
              store.pendingStatusRequests[0]?.id === request.id
                ? focusStyle
                : { background: 'color-mix(in srgb, var(--panel-muted) 74%, white)' }
            "
          >
            <div
              class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
            >
              <div class="grid gap-1">
                <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                  request.task?.title ?? '연결 작업 없음'
                }}</strong>
                <p class="text-sm text-[color:var(--muted-text)]">{{ request.createdAt }}</p>
              </div>
              <span :class="chipClass" :style="chipStyle()">{{
                planner.statusLabels[request.nextStatus]
              }}</span>
            </div>
          </article>

          <div
            v-if="!store.pendingStatusRequests.length"
            class="rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_78%,white)] px-4 py-4 text-sm text-[color:var(--muted-text)] shadow-[inset_0_1px_0_rgba(255,255,255,0.52)]"
          >
            대기 중인 요청이 없습니다.
          </div>
        </div>
      </aside>
    </div>

    <div
      v-else
      class="grid gap-[0.85rem] [grid-template-columns:minmax(0,1.15fr)_minmax(300px,340px)] max-[1380px]:grid-cols-1"
    >
      <article
        :class="panelClass"
        :style="store.focusTarget === 'tasknotes-team' ? focusStyle : null"
      >
        <header
          class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
        >
          <h4 class="text-base font-semibold text-[color:var(--text-primary)]">팀 일정 보드</h4>
        </header>

        <div class="grid grid-cols-2 gap-[0.85rem] max-[980px]:grid-cols-1">
          <article
            v-for="group in store.teamTaskGroups"
            :key="group.id"
            class="grid gap-[0.85rem] rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_76%,white)] px-[0.9rem] py-[0.9rem] shadow-[inset_0_1px_0_rgba(255,255,255,0.56)]"
          >
            <header
              class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
            >
              <div class="flex items-center justify-start gap-3">
                <span
                  class="grid h-10 w-10 place-items-center rounded-[5px] text-sm font-extrabold text-white"
                  :style="{ background: group.accent }"
                >
                  {{ group.initials }}
                </span>
                <div class="grid gap-1">
                  <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                    group.name
                  }}</strong>
                  <p class="text-sm text-[color:var(--muted-text)]">{{ group.role }}</p>
                </div>
              </div>
              <span :class="chipClass" :style="chipStyle()">{{ group.tasks.length }}건</span>
            </header>

            <div class="grid gap-[0.85rem]">
              <button
                v-for="task in group.tasks"
                :key="task.id"
                type="button"
                class="grid gap-1 rounded-[5px] border border-[color:var(--border-strong)] px-4 py-3 text-left shadow-[inset_0_1px_0_rgba(255,255,255,0.56)] transition duration-200 hover:-translate-y-px hover:shadow-[0_8px_20px_rgba(15,23,42,0.08)]"
                :style="teamTaskStyle(store.selectedTeamTask?.id === task.id)"
                @click="store.setSelectedTeamTask(task.id)"
              >
                <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                  task.title
                }}</strong>
                <small class="text-xs text-[color:var(--muted-text)]"
                  >{{ formatShortDate(task.dueDate) }} ·
                  {{ planner.statusLabels[task.status] }}</small
                >
              </button>
            </div>
          </article>
        </div>
      </article>

      <aside :class="panelClass">
        <header
          class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
        >
          <h4 class="text-base font-semibold text-[color:var(--text-primary)]">
            {{ store.selectedTeamTask?.title ?? '일정 조정' }}
          </h4>

          <button
            v-if="store.selectedTeamTask"
            :class="softButtonClass"
            type="button"
            @click="openPlannerTask(store.selectedTeamTask.id)"
          >
            캘린더에서 보기
          </button>
        </header>

        <div v-if="store.selectedTeamTask" class="grid gap-[0.85rem]">
          <article :class="infoCardClass">
            <small class="text-xs text-[color:var(--muted-text)]">담당자</small>
            <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
              selectedTeamTaskMember?.name
            }}</strong>
            <p class="text-sm text-[color:var(--muted-text)]">{{ selectedTeamTaskMember?.role }}</p>
          </article>

          <div :class="infoCardClass">
            <div
              class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
            >
              <span class="text-sm text-[color:var(--muted-text)]">시작일</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                formatShortDate(store.selectedTeamTask.startDate)
              }}</strong>
            </div>
            <div
              class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
            >
              <span class="text-sm text-[color:var(--muted-text)]">마감일</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                formatShortDate(store.selectedTeamTask.dueDate)
              }}</strong>
            </div>
            <div
              class="flex items-center justify-between gap-3 max-[980px]:flex-col max-[980px]:items-start"
            >
              <span class="text-sm text-[color:var(--muted-text)]">상태</span>
              <strong class="text-sm font-semibold text-[color:var(--text-primary)]">{{
                planner.statusLabels[store.selectedTeamTask.status]
              }}</strong>
            </div>
          </div>

          <div
            v-if="store.activeRole === 'admin'"
            class="grid gap-[0.85rem] rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_76%,white)] px-[0.9rem] py-[0.9rem] shadow-[inset_0_1px_0_rgba(255,255,255,0.56)]"
          >
            <div
              class="flex items-center justify-stretch gap-3 max-[980px]:flex-col max-[980px]:items-stretch"
            >
              <button
                :class="ghostButtonClass"
                type="button"
                @click="store.shiftTeamTask(store.selectedTeamTask.id, -1)"
              >
                하루 앞당기기
              </button>
              <button
                :class="ghostButtonClass"
                type="button"
                @click="store.shiftTeamTask(store.selectedTeamTask.id, 1)"
              >
                하루 미루기
              </button>
            </div>

            <div class="flex flex-wrap gap-2">
              <button
                v-for="member in store.members"
                :key="member.id"
                type="button"
                :class="chipClass"
                :style="store.selectedTeamTask.assigneeId === member.id ? focusStyle : chipStyle()"
                @click="store.reassignTeamTask(store.selectedTeamTask.id, member.id)"
              >
                {{ member.name }}
              </button>
            </div>
          </div>

          <div
            v-else
            class="rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_78%,white)] px-4 py-4 text-sm text-[color:var(--muted-text)] shadow-[inset_0_1px_0_rgba(255,255,255,0.52)]"
          >
            일정 조정은 관리자 전용입니다.
          </div>
        </div>

        <div
          v-else
          class="rounded-[5px] border border-[color:var(--border-strong)] bg-[color:color-mix(in_srgb,var(--panel-muted)_78%,white)] px-4 py-4 text-sm text-[color:var(--muted-text)] shadow-[inset_0_1px_0_rgba(255,255,255,0.52)]"
        >
          팀 업무를 선택해 주세요.
        </div>
      </aside>
    </div>
  </section>
</template>
