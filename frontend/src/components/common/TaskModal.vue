<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()
const route = useRoute()
const router = useRouter()

const isOpen = computed(() => Boolean(store.modalTask))
const isCreateMode = computed(() => store.modalMode === 'create')
const statusOptions = [
  { value: 'planned', label: '계획' },
  { value: 'in_progress', label: '진행 중' },
  { value: 'review', label: '검토' },
  { value: 'done', label: '완료' },
  { value: 'at_risk', label: '위험' },
]
const priorityOptions = [
  { value: 'low', label: '낮음' },
  { value: 'medium', label: '보통' },
  { value: 'high', label: '높음' },
  { value: 'critical', label: '긴급' },
]
const attachmentSizes = {
  pdf: '1.2MB',
  doc: '45KB',
  docx: '45KB',
  xls: '84KB',
  xlsx: '84KB',
  png: '320KB',
  jpg: '410KB',
  jpeg: '410KB',
  ai: '1.8MB',
  pptx: '2.1MB',
  csv: '42KB',
  fig: '690KB',
  md: '12KB',
}

const form = ref(null)
const newAttachment = ref('')
const tagsText = ref('')

function getRouteContentId() {
  const raw = route.params.contentId

  if (Array.isArray(raw)) {
    return raw[0] ?? ''
  }

  return typeof raw === 'string' ? raw : ''
}

function isSameEditorLocation(location) {
  const nextContentId = Array.isArray(location.params?.contentId)
    ? location.params.contentId[0] ?? ''
    : location.params?.contentId ?? ''
  const nextQuery = JSON.stringify(location.query ?? {})
  const currentQuery = JSON.stringify(route.query ?? {})

  return route.name === location.name && getRouteContentId() === nextContentId && currentQuery === nextQuery
}

function goToEditor(location) {
  if (isSameEditorLocation(location)) {
    return
  }

  if (route.name === 'content-editor') {
    void router.replace(location)
    return
  }

  void router.push(location)
}

const parsedTags = computed(() =>
  tagsText.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean),
)

const attachmentCards = computed(() => {
  return (form.value?.attachments ?? []).map((attachment, index) => {
    const type = attachment.split('.').pop()?.toLowerCase() ?? 'file'
    return {
      id: `${attachment}-${index}`,
      name: attachment,
      type,
      size: attachmentSizes[type] ?? '120KB',
      label: type.toUpperCase(),
      tone: ['pdf', 'png', 'jpg', 'jpeg'].includes(type) ? 'warm' : 'cool',
    }
  })
})

function cloneTask(task) {
  return {
    ...task,
    tags: [...(task.tags ?? [])],
    attachments: [...(task.attachments ?? [])],
    history: [...(task.history ?? [])],
  }
}

watch(
  () => store.modalTask,
  (task) => {
    form.value = task ? cloneTask(task) : null
    newAttachment.value = ''
    tagsText.value = task ? (task.tags ?? []).join(', ') : ''
  },
  { immediate: true, deep: true },
)

watch(
  [() => store.taskOpenToken, () => store.createTaskToken],
  ([taskToken, createToken], [prevTaskToken, prevCreateToken]) => {
    if (taskToken !== prevTaskToken && store.selectedTask?.id) {
      goToEditor({
        name: 'content-editor',
        params: { contentId: store.selectedTask.id },
      })
      return
    }

    if (createToken !== prevCreateToken) {
      goToEditor({
        name: 'content-editor',
        params: { contentId: 'new' },
        query: store.createSeedDate ? { date: store.createSeedDate } : {},
      })
    }
  },
)

function memberById(memberId) {
  return store.findMember(memberId)
}

function memberName(memberId) {
  return memberById(memberId)?.name ?? '미지정'
}

function memberInitials(memberId) {
  return memberById(memberId)?.initials ?? 'NA'
}

function memberAccent(memberId) {
  return memberById(memberId)?.accent ?? '#94a3b8'
}

function statusLabel(value) {
  return statusOptions.find((item) => item.value === value)?.label ?? value
}

function priorityLabel(value) {
  return priorityOptions.find((item) => item.value === value)?.label ?? value
}

function closeModal() {
  store.closeTask()
}

function addAttachment() {
  if (!newAttachment.value.trim() || !form.value) {
    return
  }

  form.value.attachments.push(newAttachment.value.trim())
  newAttachment.value = ''
}

function removeAttachment(index) {
  if (!form.value) {
    return
  }

  form.value.attachments.splice(index, 1)
}

function saveTask() {
  if (!form.value) {
    return
  }

  const payload = {
    ...cloneTask(form.value),
    summary:
      form.value.summary?.trim() || form.value.description?.trim().slice(0, 84) || form.value.title,
    tags: parsedTags.value,
    history: [
      ...(form.value.history ?? []),
      isCreateMode.value ? '새 콘텐츠 카드가 생성되었습니다.' : '카드 정보가 수정되었습니다.',
    ],
  }

  if (isCreateMode.value) {
    store.createTask(payload)
    return
  }

  store.updateTask(payload.id, payload)
  store.closeTask()
}

function duplicateTask() {
  if (form.value?.id) {
    store.duplicateTask(form.value.id)
  }
}

function deleteTask() {
  if (form.value?.id) {
    store.deleteTask(form.value.id)
  }
}
</script>

<template>
  <Teleport to="body">
    <div v-if="isOpen && form" class="task-modal">
      <div class="task-modal__backdrop" @click="closeModal" />

      <section class="task-modal__dialog">
        <header class="task-modal__toolbar">
          <div class="task-modal__window">
            <span />
            <span />
            <button type="button">v</button>
          </div>

          <div class="task-modal__toolbar-actions">
            <button class="task-modal__share" type="button">공유</button>
            <button class="task-modal__close" type="button" aria-label="닫기" @click="closeModal">
              X
            </button>
          </div>
        </header>

        <div class="task-modal__body">
          <section class="task-modal__headline">
            <p class="task-modal__eyebrow">{{ isCreateMode ? '새 카드' : form.requirementId }}</p>
            <input v-model="form.title" class="task-modal__title" type="text" />
          </section>

          <section class="task-modal__properties">
            <div class="task-modal__property">
              <span>상태</span>
              <div class="task-modal__control task-modal__control--inline">
                <div class="task-modal__status">
                  <span
                    class="task-modal__status-dot"
                    :class="`task-modal__status-dot--${form.status}`"
                  />
                  <strong>{{ statusLabel(form.status) }}</strong>
                </div>
                <select v-model="form.status">
                  <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>

            <div class="task-modal__property">
              <span>담당자</span>
              <div class="task-modal__control task-modal__control--inline">
                <div class="task-modal__assignee">
                  <span
                    class="task-modal__avatar"
                    :style="{ backgroundColor: memberAccent(form.assigneeId) }"
                  >
                    {{ memberInitials(form.assigneeId) }}
                  </span>
                  <strong>{{ memberName(form.assigneeId) }}</strong>
                </div>
                <select v-model="form.assigneeId">
                  <option v-for="member in store.members" :key="member.id" :value="member.id">
                    {{ member.name }}
                  </option>
                </select>
              </div>
            </div>

            <div class="task-modal__property">
              <span>우선순위</span>
              <div class="task-modal__control task-modal__control--inline">
                <span class="task-modal__priority">{{ priorityLabel(form.priority) }}</span>
                <select v-model="form.priority">
                  <option
                    v-for="option in priorityOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </div>

            <div class="task-modal__property">
              <span>마감일</span>
              <div class="task-modal__control">
                <input v-model="form.dueDate" type="date" />
              </div>
            </div>

            <div class="task-modal__property">
              <span>시간</span>
              <div class="task-modal__control">
                <input v-model="form.timeRange" type="text" placeholder="10:00 - 13:00" />
              </div>
            </div>

            <div class="task-modal__property task-modal__property--tags">
              <span>태그</span>
              <div class="task-modal__control">
                <input v-model="tagsText" type="text" placeholder="디자인, 웹" />
                <div v-if="parsedTags.length" class="task-modal__tags">
                  <span
                    v-for="(tag, index) in parsedTags"
                    :key="tag"
                    class="task-modal__tag"
                    :class="`task-modal__tag--${index % 4}`"
                  >
                    {{ tag }}
                  </span>
                </div>
              </div>
            </div>

            <button class="task-modal__custom-field" type="button">+ 사용자 필드 추가</button>
          </section>

          <div class="task-modal__divider" />

          <section class="task-modal__section">
            <h3>설명</h3>
            <textarea
              v-model="form.description"
              rows="5"
              placeholder="작업 배경, 기대 결과, 다음 액션을 입력하세요."
            />
          </section>

          <section class="task-modal__section">
            <h3>
              첨부파일
              <span>• {{ attachmentCards.length }}</span>
            </h3>

            <div class="task-modal__attachments">
              <article
                v-for="(attachment, index) in attachmentCards"
                :key="attachment.id"
                class="task-modal__attachment"
              >
                <div
                  class="task-modal__file-icon"
                  :class="`task-modal__file-icon--${attachment.tone}`"
                >
                  {{ attachment.label.slice(0, 1) }}
                </div>

                <div class="task-modal__file-copy">
                  <strong>{{ attachment.name }}</strong>
                  <small>{{ attachment.label }} • {{ attachment.size }}</small>
                </div>

                <button type="button" aria-label="첨부 제거" @click="removeAttachment(index)">
                  X
                </button>
              </article>

              <div class="task-modal__attachment-add">
                <input
                  v-model="newAttachment"
                  type="text"
                  placeholder="파일명 또는 링크 추가"
                  @keyup.enter.prevent="addAttachment"
                />
                <button type="button" @click="addAttachment">+</button>
              </div>
            </div>
          </section>
        </div>

        <footer class="task-modal__footer">
          <div class="task-modal__footer-left">
            <button v-if="!isCreateMode" type="button" @click="duplicateTask">복제</button>
            <button
              v-if="!isCreateMode"
              type="button"
              class="task-modal__danger"
              @click="deleteTask"
            >
              삭제
            </button>
          </div>

          <div class="task-modal__footer-actions">
            <button type="button" class="task-modal__secondary" @click="closeModal">취소</button>
            <button type="button" class="task-modal__primary" @click="saveTask">저장</button>
          </div>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.task-modal {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.task-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(10px);
}

.task-modal__dialog {
  position: relative;
  width: min(760px, calc(100vw - 2rem));
  max-height: calc(100vh - 2rem);
  display: flex;
  flex-direction: column;
  border-radius: 28px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  box-shadow: var(--shadow-elevated);
  overflow: hidden;
}

.task-modal__toolbar,
.task-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.25rem;
  background: color-mix(in srgb, var(--panel-color) 94%, var(--panel-muted));
}

.task-modal__toolbar {
  border-bottom: 1px solid var(--border-color);
}

.task-modal__footer {
  border-top: 1px solid var(--border-color);
}

.task-modal__window,
.task-modal__toolbar-actions,
.task-modal__footer-left,
.task-modal__footer-actions {
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.task-modal__window span,
.task-modal__window button {
  width: 0.78rem;
  height: 0.78rem;
  border-radius: 999px;
  border: 0;
  background: var(--border-color);
  color: var(--muted-text);
}

.task-modal__window button {
  width: auto;
  min-width: 1.65rem;
  padding: 0 0.45rem;
  border-radius: 999px;
  font-size: 0.7rem;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  cursor: pointer;
}

.task-modal__share,
.task-modal__close,
.task-modal__footer-left button,
.task-modal__secondary {
  border: 0;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  font-weight: 600;
}

.task-modal__close {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
}

.task-modal__danger {
  color: #dc2626;
}

.task-modal__body {
  overflow: auto;
  padding: 1.8rem 1.6rem 1.6rem;
  display: grid;
  gap: 1.4rem;
}

.task-modal__headline {
  display: grid;
  gap: 0.45rem;
}

.task-modal__eyebrow {
  color: var(--muted-text);
  font-size: 0.76rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.task-modal__title {
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-primary);
  font-size: clamp(1.7rem, 2.6vw, 2.2rem);
  line-height: 1.15;
  font-weight: 700;
  letter-spacing: -0.03em;
  padding: 0;
}

.task-modal__properties {
  display: grid;
  gap: 1rem;
}

.task-modal__property {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.task-modal__property > span {
  color: var(--muted-text);
  font-size: 0.9rem;
  padding-top: 0.8rem;
}

.task-modal__control {
  display: grid;
  gap: 0.65rem;
}

.task-modal__control--inline {
  grid-template-columns: minmax(0, 1fr) 170px;
  align-items: center;
}

.task-modal__control input,
.task-modal__control select,
.task-modal__section textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: var(--panel-muted);
  color: var(--text-primary);
  padding: 0.82rem 0.95rem;
  font: inherit;
}

.task-modal__control select {
  cursor: pointer;
}

.task-modal__status,
.task-modal__assignee {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  min-height: 3rem;
}

.task-modal__status-dot {
  width: 0.52rem;
  height: 0.52rem;
  border-radius: 999px;
  background: #94a3b8;
}

.task-modal__status-dot--planned {
  background: #94a3b8;
}

.task-modal__status-dot--in_progress {
  background: #22c55e;
}

.task-modal__status-dot--review {
  background: #f59e0b;
}

.task-modal__status-dot--done {
  background: #14b8a6;
}

.task-modal__status-dot--at_risk {
  background: #ef4444;
}

.task-modal__avatar {
  width: 1.65rem;
  height: 1.65rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
}

.task-modal__priority {
  width: fit-content;
  min-height: 2rem;
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #edf9ef;
  color: #16a34a;
  padding: 0 0.65rem;
  font-size: 0.76rem;
  font-weight: 700;
}

.task-modal__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.task-modal__tag {
  border-radius: 8px;
  padding: 0.22rem 0.5rem;
  font-size: 0.78rem;
  font-weight: 600;
}

.task-modal__tag--0 {
  background: #fff2e8;
  color: #f97316;
}

.task-modal__tag--1 {
  background: #eaf1ff;
  color: #4f6df5;
}

.task-modal__tag--2 {
  background: #ecfdf5;
  color: #10b981;
}

.task-modal__tag--3 {
  background: #f5f3ff;
  color: #7c3aed;
}

.task-modal__custom-field {
  width: fit-content;
  border: 0;
  background: transparent;
  color: #c0c8d8;
  cursor: pointer;
  padding: 0;
  font-size: 1rem;
}

.task-modal__divider {
  height: 1px;
  background: var(--border-color);
}

.task-modal__section {
  display: grid;
  gap: 0.9rem;
}

.task-modal__section h3 {
  color: #8da0c4;
  font-size: 0.88rem;
  letter-spacing: 0.04em;
}

.task-modal__section h3 span {
  color: #b4bdd0;
  font-weight: 500;
  margin-left: 0.2rem;
}

.task-modal__section textarea {
  resize: vertical;
  min-height: 7rem;
  line-height: 1.65;
}

.task-modal__attachments {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.task-modal__attachment,
.task-modal__attachment-add {
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: color-mix(in srgb, var(--panel-color) 92%, var(--panel-muted));
  min-height: 5.85rem;
}

.task-modal__attachment {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.9rem;
  padding: 0.95rem 1rem;
}

.task-modal__file-icon {
  width: 2.55rem;
  height: 2.55rem;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 0.88rem;
}

.task-modal__file-icon--warm {
  background: #fff1ef;
  color: #ef4444;
}

.task-modal__file-icon--cool {
  background: #edf3ff;
  color: #4f6df5;
}

.task-modal__file-copy {
  min-width: 0;
}

.task-modal__file-copy strong,
.task-modal__file-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-modal__file-copy small {
  color: var(--muted-text);
  margin-top: 0.18rem;
}

.task-modal__attachment button {
  border: 0;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
}

.task-modal__attachment-add {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  padding: 0.8rem;
}

.task-modal__attachment-add input {
  width: 100%;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  background: var(--panel-color);
  color: var(--text-primary);
  padding: 0.9rem 1rem;
  font: inherit;
}

.task-modal__attachment-add button {
  width: 3rem;
  border: 0;
  border-radius: 14px;
  background: #eef2ff;
  color: var(--accent-strong);
  font-size: 1.3rem;
  cursor: pointer;
}

.task-modal__primary {
  border: 0;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--accent-color), var(--accent-strong));
  color: #fff;
  padding: 0.95rem 1.6rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 14px 30px rgba(94, 106, 210, 0.24);
}

.task-modal__secondary {
  color: var(--text-secondary);
}

@media (max-width: 860px) {
  .task-modal__dialog {
    width: min(100%, calc(100vw - 1rem));
    max-height: calc(100vh - 1rem);
  }

  .task-modal__property,
  .task-modal__control--inline,
  .task-modal__attachments {
    grid-template-columns: 1fr;
  }

  .task-modal__property > span {
    padding-top: 0;
  }
}

@media (max-width: 640px) {
  .task-modal {
    padding: 0;
  }

  .task-modal__dialog {
    width: 100vw;
    max-height: 100vh;
    border-radius: 0;
  }

  .task-modal__toolbar,
  .task-modal__footer {
    padding-inline: 1rem;
  }

  .task-modal__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .task-modal__footer-left,
  .task-modal__footer-actions {
    justify-content: space-between;
  }
}
</style>
