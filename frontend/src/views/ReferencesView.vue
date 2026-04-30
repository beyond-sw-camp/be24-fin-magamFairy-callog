<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  createReference,
  deleteReference,
  getReferenceById,
  getReferences,
  updateReference,
} from '@/api/references/index.js'

const initialReferences = [
  {
    id: 1,
    type: 'image',
    title: '멤버십 론칭 배너 레퍼런스',
    description: '혜택 금액, 기간, CTA가 한 화면에서 바로 읽히는 프로모션 배너 구조입니다.',
    tags: ['배너', 'CTA', '혜택표기'],
    url: 'https://images.unsplash.com/photo-1558655146-9f40138edfeb?auto=format&fit=crop&q=80&w=900',
    thumbnail: 'https://images.unsplash.com/photo-1558655146-9f40138edfeb?auto=format&fit=crop&q=80&w=900',
    date: '2026-04-27',
    channel: '랜딩',
    objective: '전환',
    status: '검토완료',
  },
  {
    id: 2,
    type: 'link',
    title: '공동 프로모션 랜딩 흐름',
    description: '파트너 로고, 쿠폰 조건, 하단 FAQ를 연결하는 랜딩 페이지 흐름 참고 자료입니다.',
    tags: ['랜딩', 'FAQ', '공동캠페인'],
    url: 'https://example.com/promotion-flow',
    date: '2026-04-25',
    channel: '랜딩',
    objective: '전환',
    status: '참고',
  },
  {
    id: 3,
    type: 'video',
    title: '숏폼 티저 컷 편집톤',
    description: '첫 3초 후킹과 자막 리듬을 확인하기 좋은 15초 숏폼 레퍼런스입니다.',
    tags: ['숏폼', '자막', '후킹'],
    url: 'https://example.com/shorts-reference',
    thumbnail: 'https://images.unsplash.com/photo-1611162617474-5b21e879e113?auto=format&fit=crop&q=80&w=900',
    date: '2026-04-23',
    channel: 'SNS',
    objective: '인지',
    status: '참고',
  },
  {
    id: 4,
    type: 'image',
    title: '알림톡 쿠폰 안내 이미지',
    description: '작은 화면에서도 기간, 조건, 버튼 문구가 무너지지 않는 모바일 우선 이미지입니다.',
    tags: ['알림톡', '쿠폰', '모바일'],
    url: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&q=80&w=900',
    thumbnail: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&q=80&w=900',
    date: '2026-04-20',
    channel: '메시지',
    objective: '재방문',
    status: '사용중',
  },
  {
    id: 5,
    type: 'link',
    title: '금지 표현 대체 문구 모음',
    description: '보장형 표현, 과장 혜택 문구를 캠페인 심사 기준에 맞춰 바꾼 문장 모음입니다.',
    tags: ['심사', '문구', '리스크'],
    url: 'https://example.com/copy-risk-check',
    date: '2026-04-18',
    channel: '공통',
    objective: '리스크관리',
    status: '필수',
  },
]

const typeOptions = [
  { value: 'all', label: '전체', icon: 'dashboard' },
  { value: 'link', label: '링크', icon: 'link' },
  { value: 'image', label: '이미지', icon: 'image' },
  { value: 'video', label: '영상', icon: 'play_circle' },
]

const channels = ['전체', '랜딩', 'SNS', '메시지', '공통']
const objectives = ['전체', '전환', '인지', '재방문', '리스크관리']
const statuses = ['전체', '필수', '사용중', '검토완료', '참고']

const references = ref([...initialReferences])
const activeType = ref('all')
const searchQuery = ref('')
const selectedChannel = ref('전체')
const selectedObjective = ref('전체')
const selectedStatus = ref('전체')
const modalState = ref({ isOpen: false, mode: 'create', data: null })
const formData = ref(createBlankForm())
const tagInput = ref('')

function createBlankForm() {
  return {
    type: 'link',
    title: '',
    url: '',
    thumbnail: '',
    description: '',
    tags: [],
    channel: '랜딩',
    objective: '전환',
    status: '참고',
  }
}

function normalizeReference(item, index = 0) {
  const fallback = initialReferences[index % initialReferences.length]

  return {
    ...fallback,
    ...item,
    id: item.id ?? Date.now() + index,
    type: item.type || fallback.type || 'link',
    title: item.title || '제목 없는 레퍼런스',
    description: item.description || '',
    tags: Array.isArray(item.tags) ? item.tags : [],
    url: item.url || '',
    thumbnail: item.thumbnail || item.url || fallback.thumbnail || fallback.url || '',
    date: item.date || item.createdAt?.slice?.(0, 10) || new Date().toISOString().slice(0, 10),
    channel: item.channel || inferField(item.tags, channels, fallback.channel),
    objective: item.objective || inferField(item.tags, objectives, fallback.objective),
    status: item.status || inferField(item.tags, statuses, fallback.status),
  }
}

function inferField(tags = [], options, fallback) {
  return tags.find((tag) => options.includes(tag)) || fallback
}

async function fetchReferencesData() {
  try {
    const response = await getReferences()
    let apiList = response?.data

    if (apiList && typeof apiList === 'object' && !Array.isArray(apiList)) {
      apiList = apiList.data || apiList.content || apiList.result || []
    }

    if (Array.isArray(apiList) && apiList.length > 0) {
      references.value = apiList.map(normalizeReference)
    }
  } catch (error) {
    console.warn('레퍼런스 API 연결 실패. 로컬 기본 데이터를 사용합니다.', error)
  }
}

onMounted(fetchReferencesData)

const filteredReferences = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()

  return references.value
    .map(normalizeReference)
    .filter((item) => activeType.value === 'all' || item.type === activeType.value)
    .filter((item) => selectedChannel.value === '전체' || item.channel === selectedChannel.value)
    .filter((item) => selectedObjective.value === '전체' || item.objective === selectedObjective.value)
    .filter((item) => selectedStatus.value === '전체' || item.status === selectedStatus.value)
    .filter((item) => {
      if (!query) return true

      return [item.title, item.description, item.channel, item.objective, item.status, ...item.tags]
        .filter(Boolean)
        .some((text) => text.toLowerCase().includes(query))
    })
    .sort((a, b) => new Date(b.date) - new Date(a.date))
})

const isViewMode = computed(() => modalState.value.mode === 'view')

const activeFilters = computed(() =>
  [
    activeType.value !== 'all' && `유형: ${typeLabel(activeType.value)}`,
    selectedChannel.value !== '전체' && `채널: ${selectedChannel.value}`,
    selectedObjective.value !== '전체' && `목적: ${selectedObjective.value}`,
    selectedStatus.value !== '전체' && `상태: ${selectedStatus.value}`,
  ].filter(Boolean),
)

function typeLabel(type) {
  return typeOptions.find((item) => item.value === type)?.label || type
}

function resetFilters() {
  activeType.value = 'all'
  selectedChannel.value = '전체'
  selectedObjective.value = '전체'
  selectedStatus.value = '전체'
  searchQuery.value = ''
}

async function openModal(mode, item = null) {
  tagInput.value = ''

  if (mode === 'create') {
    formData.value = createBlankForm()
    modalState.value = { isOpen: true, mode, data: null }
    return
  }

  if (!item) return

  let viewData = normalizeReference(item)

  if (mode === 'view') {
    try {
      const response = await getReferenceById(item.id)
      let apiData = response?.data
      if (apiData && typeof apiData === 'object' && !Array.isArray(apiData)) {
        apiData = apiData.data || apiData.result || apiData
      }
      viewData = normalizeReference({ ...viewData, ...apiData, id: item.id })
    } catch (error) {
      console.warn('상세 조회 API 연결 실패. 목록 데이터를 사용합니다.', error)
    }
  }

  formData.value = JSON.parse(JSON.stringify(viewData))
  modalState.value = { isOpen: true, mode, data: viewData }
}

function closeModal() {
  modalState.value = { isOpen: false, mode: 'create', data: null }
}

function switchModeToEdit() {
  modalState.value.mode = 'edit'
}

async function handleSave() {
  if (!formData.value.title || !formData.value.url) return

  const saveData = normalizeReference({
    ...formData.value,
    date: formData.value.date || new Date().toISOString().slice(0, 10),
  })

  try {
    if (modalState.value.mode === 'create') {
      await createReference(saveData)
    } else {
      await updateReference(saveData.id, saveData)
    }

    await fetchReferencesData()
    closeModal()
  } catch (error) {
    console.warn('저장 API 연결 실패. 현재 화면 데이터에만 반영합니다.', error)

    if (modalState.value.mode === 'create') {
      references.value = [{ ...saveData, id: Date.now() }, ...references.value]
    } else {
      references.value = references.value.map((item) => (item.id === saveData.id ? saveData : item))
    }

    closeModal()
  }
}

async function handleDelete(id) {
  if (!id || !confirm('이 레퍼런스를 삭제할까요?')) return

  try {
    await deleteReference(id)
    await fetchReferencesData()
  } catch (error) {
    console.warn('삭제 API 연결 실패. 현재 화면 데이터에서만 제거합니다.', error)
    references.value = references.value.filter((item) => item.id !== id)
  }

  closeModal()
}

function handleTagAdd(event) {
  event.preventDefault()
  const value = tagInput.value.trim()
  if (value && !formData.value.tags.includes(value)) {
    formData.value.tags.push(value)
  }
  tagInput.value = ''
}

function removeFormTag(tagToRemove) {
  formData.value.tags = formData.value.tags.filter((tag) => tag !== tagToRemove)
}
</script>

<template>
  <section class="reference-room">
    <header class="reference-hero">
      <div class="reference-hero__copy">
        <h2>캠페인 레퍼런스실</h2>
        <span>링크, 이미지, 영상, 문구 기준을 빠르게 찾고 관리합니다.</span>
      </div>

      <button type="button" class="primary-action" @click="openModal('create')">
        <span class="material-symbols-outlined" aria-hidden="true">add</span>
        레퍼런스 추가
      </button>
    </header>

    <section class="reference-toolbar" aria-label="레퍼런스 필터">
      <div class="type-segments">
        <button
          v-for="type in typeOptions"
          :key="type.value"
          type="button"
          :class="{ 'type-segments__item--active': activeType === type.value }"
          @click="activeType = type.value"
        >
          <span class="material-symbols-outlined" aria-hidden="true">{{ type.icon }}</span>
          {{ type.label }}
        </button>
      </div>

      <div class="reference-search">
        <span class="material-symbols-outlined" aria-hidden="true">search</span>
        <input v-model="searchQuery" type="search" placeholder="제목, 설명, 태그 검색" />
      </div>

      <div class="filter-grid">
        <label>
          <span>채널</span>
          <select v-model="selectedChannel">
            <option v-for="channel in channels" :key="channel">{{ channel }}</option>
          </select>
        </label>

        <label>
          <span>목적</span>
          <select v-model="selectedObjective">
            <option v-for="objective in objectives" :key="objective">{{ objective }}</option>
          </select>
        </label>

        <label>
          <span>상태</span>
          <select v-model="selectedStatus">
            <option v-for="status in statuses" :key="status">{{ status }}</option>
          </select>
        </label>
      </div>
    </section>

    <div v-if="activeFilters.length || searchQuery" class="active-filter-row">
      <span v-for="filter in activeFilters" :key="filter">{{ filter }}</span>
      <span v-if="searchQuery">검색: {{ searchQuery }}</span>
      <button type="button" @click="resetFilters">초기화</button>
    </div>

    <section class="library-section">
      <div class="section-head">
        <div>
          <p class="section-eyebrow">Library</p>
          <h3>레퍼런스 목록</h3>
        </div>
        <span class="result-count">{{ filteredReferences.length }}개</span>
      </div>

      <div v-if="filteredReferences.length" class="reference-grid">
        <article
          v-for="item in filteredReferences"
          :key="item.id"
          class="reference-card"
          :class="`reference-card--${item.type}`"
          role="button"
          tabindex="0"
          @click="openModal('view', item)"
          @keydown.enter.prevent="openModal('view', item)"
          @keydown.space.prevent="openModal('view', item)"
        >
          <button type="button" class="reference-card__preview" @click.stop="openModal('view', item)">
            <img v-if="item.type !== 'link' && item.thumbnail" :src="item.thumbnail" :alt="item.title" />
            <span v-else class="material-symbols-outlined" aria-hidden="true">
              {{ item.type === 'video' ? 'play_circle' : item.type === 'image' ? 'image' : 'link' }}
            </span>
          </button>

          <div class="reference-card__body">
            <div class="reference-card__meta">
              <span>{{ typeLabel(item.type) }}</span>
              <em>{{ item.status }}</em>
            </div>
            <h4>{{ item.title }}</h4>
            <p>{{ item.description }}</p>
            <div class="reference-card__info">
              <span>{{ item.channel }}</span>
              <span>{{ item.objective }}</span>
              <span>{{ item.date }}</span>
            </div>
            <div class="tag-row">
              <span v-for="tag in item.tags" :key="tag">#{{ tag }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-state">
        <span class="material-symbols-outlined" aria-hidden="true">manage_search</span>
        <strong>조건에 맞는 레퍼런스가 없습니다.</strong>
        <button type="button" @click="resetFilters">필터 초기화</button>
      </div>
    </section>

    <transition name="modal-fade">
      <div v-if="modalState.isOpen" class="reference-modal" role="dialog" aria-modal="true">
        <button type="button" class="reference-modal__backdrop" aria-label="닫기" @click="closeModal"></button>

        <div class="reference-modal__dialog">
          <header class="reference-modal__head">
            <div>
              <p class="section-eyebrow">{{ isViewMode ? 'Detail' : 'Edit' }}</p>
              <h3>{{ modalState.mode === 'create' ? '레퍼런스 추가' : isViewMode ? '레퍼런스 상세' : '레퍼런스 수정' }}</h3>
            </div>
            <div class="modal-actions">
              <button v-if="isViewMode" type="button" title="수정" @click="switchModeToEdit">
                <span class="material-symbols-outlined" aria-hidden="true">edit</span>
              </button>
              <button v-if="isViewMode" type="button" title="삭제" @click="handleDelete(modalState.data.id)">
                <span class="material-symbols-outlined" aria-hidden="true">delete</span>
              </button>
              <button type="button" title="닫기" @click="closeModal">
                <span class="material-symbols-outlined" aria-hidden="true">close</span>
              </button>
            </div>
          </header>

          <div v-if="isViewMode" class="reference-detail">
            <div class="detail-preview">
              <img
                v-if="modalState.data.type !== 'link' && modalState.data.thumbnail"
                :src="modalState.data.thumbnail"
                :alt="modalState.data.title"
              />
              <span v-else class="material-symbols-outlined" aria-hidden="true">link</span>
            </div>
            <div class="detail-copy">
              <div class="reference-card__meta">
                <span>{{ typeLabel(modalState.data.type) }}</span>
                <em>{{ modalState.data.status }}</em>
              </div>
              <h4>{{ modalState.data.title }}</h4>
              <p>{{ modalState.data.description || '설명이 등록되지 않았습니다.' }}</p>
              <div class="reference-card__info">
                <span>{{ modalState.data.channel }}</span>
                <span>{{ modalState.data.objective }}</span>
                <span>{{ modalState.data.date }}</span>
              </div>
              <div class="tag-row">
                <span v-for="tag in modalState.data.tags" :key="tag">#{{ tag }}</span>
              </div>
              <a :href="modalState.data.url" target="_blank" rel="noreferrer" class="primary-action">
                <span class="material-symbols-outlined" aria-hidden="true">open_in_new</span>
                원본 열기
              </a>
            </div>
          </div>

          <form v-else id="reference-form" class="reference-form" @submit.prevent="handleSave">
            <div class="type-choice">
              <label v-for="type in typeOptions.filter((item) => item.value !== 'all')" :key="type.value">
                <input v-model="formData.type" type="radio" :value="type.value" />
                <span class="material-symbols-outlined" aria-hidden="true">{{ type.icon }}</span>
                {{ type.label }}
              </label>
            </div>

            <label class="field">
              <span>제목 *</span>
              <input v-model="formData.title" type="text" required placeholder="레퍼런스 제목" />
            </label>

            <label class="field">
              <span>URL *</span>
              <input v-model="formData.url" type="url" required placeholder="https://..." />
            </label>

            <label class="field">
              <span>썸네일 URL</span>
              <input v-model="formData.thumbnail" type="url" placeholder="이미지 또는 영상 썸네일 주소" />
            </label>

            <div class="form-grid">
              <label class="field">
                <span>채널</span>
                <select v-model="formData.channel">
                  <option v-for="channel in channels.filter((item) => item !== '전체')" :key="channel">{{ channel }}</option>
                </select>
              </label>
              <label class="field">
                <span>목적</span>
                <select v-model="formData.objective">
                  <option v-for="objective in objectives.filter((item) => item !== '전체')" :key="objective">{{ objective }}</option>
                </select>
              </label>
              <label class="field">
                <span>상태</span>
                <select v-model="formData.status">
                  <option v-for="status in statuses.filter((item) => item !== '전체')" :key="status">{{ status }}</option>
                </select>
              </label>
            </div>

            <label class="field">
              <span>설명</span>
              <textarea v-model="formData.description" rows="4" placeholder="어떤 점을 참고하면 좋은지 적어주세요."></textarea>
            </label>

            <div class="field">
              <span>태그</span>
              <div class="tag-editor">
                <div v-if="formData.tags.length" class="tag-row">
                  <span v-for="tag in formData.tags" :key="tag">
                    #{{ tag }}
                    <button type="button" @click="removeFormTag(tag)">
                      <span class="material-symbols-outlined" aria-hidden="true">close</span>
                    </button>
                  </span>
                </div>
                <div class="tag-editor__input">
                  <input v-model="tagInput" type="text" placeholder="태그 입력 후 Enter" @keydown.enter="handleTagAdd" />
                  <button type="button" @click="handleTagAdd">추가</button>
                </div>
              </div>
            </div>
          </form>

          <footer class="reference-modal__footer">
            <button type="button" class="ghost-action" @click="closeModal">닫기</button>
            <button v-if="!isViewMode" type="submit" form="reference-form" class="primary-action">저장</button>
          </footer>
        </div>
      </div>
    </transition>
  </section>
</template>

<style scoped>
.reference-room {
  display: grid;
  width: 100%;
  gap: 12px;
  margin: 0 auto;
}

.reference-hero,
.reference-toolbar,
.reference-card,
.empty-state,
.reference-modal__dialog {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.reference-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
}

.reference-hero__copy h2 {
  color: var(--text-primary);
  font-size: 21px;
  font-weight: 950;
  line-height: 1.2;
}

.reference-hero__copy span {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.primary-action,
.ghost-action {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 14px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.primary-action {
  border: 1px solid transparent;
  background: var(--text-primary);
  color: var(--panel-color);
}

.ghost-action {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.primary-action .material-symbols-outlined,
.ghost-action .material-symbols-outlined {
  font-size: 18px;
}

.result-count,
.reference-card__info {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.reference-toolbar {
  display: grid;
  grid-template-columns: auto minmax(240px, 1fr) minmax(390px, auto);
  gap: 10px;
  align-items: end;
  padding: 10px 12px;
}

.type-segments {
  display: flex;
  gap: 6px;
  padding: 4px;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.type-segments button {
  display: inline-flex;
  min-height: 34px;
  align-items: center;
  gap: 5px;
  padding: 0 11px;
  border-radius: 6px;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.type-segments .material-symbols-outlined {
  font-size: 18px;
}

.type-segments__item--active {
  background: var(--panel-color);
  color: var(--text-primary) !important;
  box-shadow: var(--shadow-sm);
}

.reference-search {
  position: relative;
}

.reference-search .material-symbols-outlined {
  position: absolute;
  left: 12px;
  top: 50%;
  color: var(--muted-text);
  font-size: 19px;
  transform: translateY(-50%);
}

.reference-search input,
.filter-grid select,
.field input,
.field select,
.field textarea,
.tag-editor__input input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  color: var(--text-primary);
  outline: none;
}

.reference-search input {
  min-height: 42px;
  padding: 0 12px 0 39px;
  font-size: 13px;
  font-weight: 700;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(118px, 1fr));
  gap: 8px;
}

.filter-grid label,
.field {
  display: grid;
  gap: 6px;
}

.filter-grid label > span,
.field > span {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 900;
}

.filter-grid select,
.field input,
.field select {
  min-height: 38px;
  padding: 0 10px;
  font-size: 13px;
  font-weight: 800;
}

.field textarea {
  min-height: 110px;
  padding: 11px 12px;
  resize: vertical;
}

.active-filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.active-filter-row span,
.tag-row span,
.reference-card__meta span,
.reference-card__meta em {
  display: inline-flex;
  min-height: 21px;
  align-items: center;
  gap: 4px;
  padding: 0 7px;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: 900;
}

.active-filter-row button {
  color: var(--muted-text);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
}

.section-head,
.reference-card__meta,
.reference-modal__head,
.reference-modal__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.section-head h3 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 950;
}

.reference-card__meta em {
  width: fit-content;
  background: var(--badge-bg);
  color: var(--badge-text);
  font-style: normal;
}

.reference-card h4,
.detail-copy h4 {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 950;
  line-height: 1.32;
}

.reference-card p,
.detail-copy p {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.reference-card p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.library-section {
  display: grid;
  gap: 12px;
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 272px));
  gap: 10px;
  align-items: start;
  justify-content: start;
}

.reference-card {
  overflow: hidden;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;
}

.reference-card:hover {
  border-color: var(--border-strong);
  box-shadow: 0 10px 24px rgba(19, 35, 68, 0.08);
  transform: translateY(-1px);
}

.reference-card__preview,
.detail-preview {
  display: grid;
  width: 100%;
  aspect-ratio: 16 / 6.6;
  place-items: center;
  background: linear-gradient(135deg, var(--panel-muted), var(--panel-subtle));
  color: var(--accent-color);
  cursor: pointer;
  overflow: hidden;
}

.reference-card__preview img,
.detail-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.reference-card__preview .material-symbols-outlined,
.detail-preview .material-symbols-outlined,
.empty-state .material-symbols-outlined {
  font-size: 34px;
}

.reference-card--link .reference-card__preview {
  display: none;
}

.reference-card--link .reference-card__body {
  min-height: 148px;
}

.reference-card--link .reference-card__body::before {
  content: 'link';
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  background: var(--accent-soft);
  color: var(--accent-strong);
  font-family: 'Material Symbols Outlined';
  font-size: 18px;
  font-weight: normal;
  font-feature-settings: 'liga';
}

.reference-card__body {
  display: grid;
  gap: 7px;
  padding: 10px 12px;
}

.reference-card__info,
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.empty-state {
  display: grid;
  min-height: 260px;
  place-items: center;
  gap: 10px;
  padding: 28px;
  color: var(--muted-text);
  text-align: center;
}

.empty-state strong {
  color: var(--text-primary);
}

.empty-state button {
  color: var(--accent-strong);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.reference-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 20px;
}

.reference-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
}

.reference-modal__dialog {
  position: relative;
  z-index: 1;
  display: flex;
  width: min(760px, calc(100vw - 40px));
  max-height: min(86vh, 780px);
  flex-direction: column;
  overflow: hidden;
}

.reference-modal__head,
.reference-modal__footer {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
}

.reference-modal__footer {
  border-top: 1px solid var(--border-color);
  border-bottom: 0;
  background: var(--panel-muted);
}

.reference-modal__head h3 {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 950;
}

.modal-actions {
  display: flex;
  gap: 6px;
}

.modal-actions button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
}

.modal-actions .material-symbols-outlined {
  font-size: 19px;
}

.reference-detail,
.reference-form {
  display: grid;
  gap: 16px;
  overflow: auto;
  padding: 16px;
}

.detail-copy {
  display: grid;
  gap: 11px;
}

.detail-copy .primary-action {
  justify-self: start;
}

.type-choice {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.type-choice label {
  display: flex;
  min-height: 54px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.type-choice input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.type-choice label:has(input:checked) {
  border-color: color-mix(in srgb, var(--accent-color) 46%, var(--border-color));
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.tag-editor {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
}

.tag-editor .tag-row span button {
  display: inline-flex;
  color: currentColor;
  cursor: pointer;
}

.tag-editor .tag-row .material-symbols-outlined {
  font-size: 14px;
}

.tag-editor__input {
  display: flex;
  gap: 8px;
}

.tag-editor__input input {
  min-height: 36px;
  padding: 0 10px;
  border-color: transparent;
  background: var(--panel-color);
}

.tag-editor__input button {
  min-width: 60px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 160ms ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1120px) {
  .reference-toolbar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .reference-hero,
  .section-head,
  .reference-modal__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .reference-hero .primary-action,
  .reference-modal__footer .primary-action,
  .reference-modal__footer .ghost-action {
    width: 100%;
  }

  .reference-grid,
  .filter-grid,
  .form-grid,
  .type-choice {
    grid-template-columns: 1fr;
  }

  .type-segments {
    overflow-x: auto;
  }
}
</style>
