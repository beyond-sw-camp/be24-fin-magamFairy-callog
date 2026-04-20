<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  addDays,
  formatLongDate,
  formatMonthLabel,
  formatShortDate,
  getMonthWeeks,
  getWeekDays,
} from '@/utils/calendar'
import { usePlannerStore } from '@/stores/planner'

const BOARD_COLS = 6
const BOARD_ROWS = 3
const BOARD_GAP = 14
const MOVE_HIT = 26
const BOARD_STORAGE_KEY = 'marketing-board-layout-v3'
const NOTE_STORAGE_KEY = 'marketing-board-note-v2'

const defaultBoardState = {
  order: ['calendar', 'performance', 'notes', 'table', 'meetings', 'aiBrief', 'riskAlert'],
  tiles: {
    calendar: { col: 1, row: 1, size: '3x1' },
    performance: { col: 4, row: 1, size: '3x1' },
    notes: { col: 1, row: 2, size: '2x1' },
    table: { col: 3, row: 2, size: '3x2' },
    meetings: { col: 6, row: 2, size: '1x1' },
    aiBrief: { col: 1, row: 3, size: '2x1' },
    riskAlert: { col: 6, row: 3, size: '1x1' },
  },
}

const allowedSizes = {
  calendar: ['1x1', '1x2', '2x2', '3x1', '3x3'],
  performance: ['1x1', '1x2', '2x2', '3x1', '3x3'],
  notes: ['1x1', '1x2', '2x1', '2x2', '3x1'],
  table: ['1x1', '2x1', '2x2', '3x1', '3x2', '3x3'],
  meetings: ['1x1', '1x2', '2x1', '2x2'],
  aiBrief: ['1x1', '1x2', '2x1', '2x2'],
  riskAlert: ['1x1', '1x2', '2x1', '2x2'],
}

const tileMeta = {
  calendar: {
    eyebrow: '캘린더',
    title: '일정 그리드',
    accent: '#2563eb',
    description: '오늘, 주간, 월간 흐름을 크기에 따라 다르게 보여줍니다.',
  },
  performance: {
    eyebrow: '성과 분석',
    title: '사람별 성과',
    accent: '#ef4444',
    description: '평균, 완료율, 리스크를 보드 크기에 맞춰 압축합니다.',
  },
  notes: {
    eyebrow: '업무일지',
    title: '오늘과 다음 업무',
    accent: '#0ea5e9',
    description: '오늘 요약과 다음 액션을 짧고 길게 바꿔 보여줍니다.',
  },
  table: {
    eyebrow: '메인 테이블',
    title: '요약과 상세',
    accent: '#f59e0b',
    description: '작업과 담당자 요약을 타일 크기에 맞게 정리합니다.',
  },
  meetings: {
    eyebrow: '회의',
    title: '회의와 메모',
    accent: '#8b5cf6',
    description: '회의 일정과 메모를 간단한 보드로 묶어 보여줍니다.',
  },
  aiBrief: {
    eyebrow: 'AI 브리핑',
    title: 'AI 브리핑',
    accent: '#ec4899',
    description: '위험 신호와 다음 액션을 자동으로 묶어 보여줍니다.',
  },
  riskAlert: {
    eyebrow: '리스크 알림',
    title: '리스크 알림',
    accent: '#f97316',
    description: '마감 임박, 지연, 병목을 빠르게 확인합니다.',
  },
}

const dockTabs = [
  { key: 'ai', label: 'AI', iconType: 'gpt', tone: 'pink' },
  { key: 'tasknote', label: 'TaskNote', iconType: 'note', tone: 'amber' },
  { key: 'meetings', label: '회의', iconType: 'chat', tone: 'sky' },
]

const svgIcons = {
  gpt: `<svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <circle cx="12" cy="12" r="9" fill="currentColor" opacity="0.12"></circle>
    <text x="12" y="15" text-anchor="middle" font-family="Arial, sans-serif" font-size="7" font-weight="800" fill="currentColor">GPT</text>
  </svg>`,
  note: `<svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M7 4.5h7.2L18.5 8.8V19a1.5 1.5 0 0 1-1.5 1.5H7A1.5 1.5 0 0 1 5.5 19V6A1.5 1.5 0 0 1 7 4.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"></path>
    <path d="M14.2 4.5V8.8H18.5" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"></path>
    <path d="M8.5 11h7M8.5 14h7M8.5 17h4.2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
  </svg>`,
  chat: `<svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M5.5 5.5h13a2.5 2.5 0 0 1 2.5 2.5v5a2.5 2.5 0 0 1-2.5 2.5H11.2L7 18.8V15.5H5.5A2.5 2.5 0 0 1 3 13V8a2.5 2.5 0 0 1 2.5-2.5Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"></path>
    <circle cx="8.25" cy="10.5" r="1" fill="currentColor"></circle>
    <circle cx="12" cy="10.5" r="1" fill="currentColor"></circle>
    <circle cx="15.75" cy="10.5" r="1" fill="currentColor"></circle>
  </svg>`,
  close: `<svg viewBox="0 0 24 24" class="h-5 w-5" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M6 6l12 12M18 6 6 18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
  </svg>`,
}

const meetingItems = [
  { time: '오전 11:30', title: '상반기 결산 보고', detail: '핵심 지표와 다음 분기 예산 조정안을 함께 검토합니다.', mode: '오프라인' },
  { time: '오후 03:00', title: '외부 에이전시 미팅', detail: '신규 광고 크리에이티브 시안과 피드백을 점검합니다.', mode: 'Google Meet' },
  { time: '오후 04:30', title: 'QBR 브리핑', detail: '리드 확보와 전환 흐름을 바탕으로 다음 액션을 정리합니다.', mode: '온라인' },
]

const tableStatusTones = {
  planned: '#9aa7bd',
  in_progress: '#59c36d',
  review: '#f5b64e',
  at_risk: '#df5f75',
  done: '#2f80ed',
}

const tablePriorityTones = {
  low: '#a6d73f',
  medium: '#f7a8a8',
  high: '#e468c8',
  critical: '#ef5b5b',
}

const store = usePlannerStore()
const selectedDate = computed({
  get: () => store.currentDate,
  set: (value) => {
    store.currentDate = value
  },
})

const selectedMemberId = ref(store.members[0]?.id ?? '')
const taskNoteDraft = ref('')
const activeRail = ref('ai')
const toolModalOpen = ref(false)

const boardRef = ref(null)
const tileElements = {}
const boardMetrics = reactive({
  width: 0,
  height: 0,
  cellWidth: 0,
  cellHeight: 0,
  pitchX: 0,
  pitchY: 0,
})

const tileOrder = ref([...defaultBoardState.order])
const hiddenTileIds = ref([])
const tileState = reactive({
  calendar: { ...defaultBoardState.tiles.calendar },
  performance: { ...defaultBoardState.tiles.performance },
  notes: { ...defaultBoardState.tiles.notes },
  table: { ...defaultBoardState.tiles.table },
  meetings: { ...defaultBoardState.tiles.meetings },
  aiBrief: { ...defaultBoardState.tiles.aiBrief },
  riskAlert: { ...defaultBoardState.tiles.riskAlert },
})

const dragPreview = ref(null)
const hoverState = ref({ tileId: null, zone: 'none' })
const sizeModalState = ref({ open: false, tileId: null })
const tileMenuState = ref({ open: false, tileId: null })
const boardManagerOpen = ref(false)

const selectedCalendarTasks = computed(() =>
  store.tasks
    .filter((task) => task.startDate <= selectedDate.value && task.dueDate >= selectedDate.value)
    .sort((left, right) => right.progress - left.progress || left.dueDate.localeCompare(right.dueDate))
)

const selectedDateTasks = computed(() => selectedCalendarTasks.value.slice(0, 4))

const selectedCalendarLabel = computed(() => formatLongDate(selectedDate.value))

const upcomingTasks = computed(() =>
  [...store.tasks]
    .filter((task) => task.startDate > selectedDate.value)
    .sort((left, right) => left.startDate.localeCompare(right.startDate))
    .slice(0, 4),
)

const weekDays = computed(() =>
  getWeekDays(selectedDate.value).map((day) => ({
    ...day,
    tasks: store.tasks.filter((task) => task.startDate <= day.key && task.dueDate >= day.key),
  })),
)

const monthWeeks = computed(() =>
  getMonthWeeks(selectedDate.value).map((week) =>
    week.map((day) => ({
      ...day,
      tasks: store.tasks.filter((task) => task.startDate <= day.key && task.dueDate >= day.key),
    })),
  ),
)

const teamMetrics = computed(() => {
  const tasks = store.tasks
  const total = tasks.length || 1
  const done = tasks.filter((task) => task.status === 'done').length
  const atRisk = tasks.filter((task) => task.status === 'at_risk').length
  const averageProgress = Math.round(tasks.reduce((sum, task) => sum + task.progress, 0) / total)
  const completionRate = Math.round((done / total) * 100)
  const dueSoon = tasks.filter(
    (task) =>
      task.status !== 'done' &&
      task.dueDate >= selectedDate.value &&
      task.dueDate <= addDays(selectedDate.value, 7),
  ).length

  return { averageProgress, completionRate, dueSoon, atRisk }
})

const performanceRows = computed(() =>
  [...store.members]
    .map((member) => {
      const tasks = store.tasks.filter((task) =>
        [task.assigneeId, task.plannerId, task.designerId, task.publisherId, task.supervisorId].includes(member.id),
      )
      const done = tasks.filter((task) => task.status === 'done').length
      const inProgress = tasks.filter((task) => task.status === 'in_progress').length
      const atRisk = tasks.filter((task) => task.status === 'at_risk').length
      const avgProgress = tasks.length ? Math.round(tasks.reduce((sum, task) => sum + task.progress, 0) / tasks.length) : 0
      const completionRate = tasks.length ? Math.round((done / tasks.length) * 100) : 0
      const score = Math.max(0, Math.min(100, Math.round(avgProgress * 0.6 + completionRate * 0.4 - atRisk * 7)))
      return { member, tasks, done, inProgress, atRisk, avgProgress, completionRate, score }
    })
    .sort((left, right) => right.score - left.score),
)

const selectedMemberRow = computed(
  () => performanceRows.value.find((row) => row.member.id === selectedMemberId.value) ?? performanceRows.value[0] ?? null,
)

const topRiskMember = computed(
  () => [...performanceRows.value].sort((left, right) => right.atRisk - left.atRisk || left.score - right.score)[0] ?? null,
)

const aiSuggestions = computed(() => {
  const name = topRiskMember.value?.member.name ?? '선택한 담당자'
  return [
    `${name}의 리스크 체크가 우선입니다.`,
    `팀 평균 진행률은 ${teamMetrics.value.averageProgress}%입니다.`,
    `7일 이내 마감 예정 업무는 ${teamMetrics.value.dueSoon}건입니다.`,
  ]
})

const tableGroups = computed(() => {
  const bucket = new Map()
  const focusMonth = selectedDate.value.slice(0, 7)

  store.tasks.forEach((task) => {
    const monthKey = task.startDate.slice(0, 7)
    if (!bucket.has(monthKey)) {
      bucket.set(monthKey, { key: monthKey, label: formatMonthLabel(`${monthKey}-01`), tasks: [] })
    }
    bucket.get(monthKey).tasks.push(task)
  })

  return [...bucket.values()]
    .map((group) => ({
      ...group,
      tasks: [...group.tasks].sort(
        (left, right) => left.dueDate.localeCompare(right.dueDate) || right.progress - left.progress,
      ),
    }))
    .sort((left, right) => {
      if (left.key === focusMonth) return -1
      if (right.key === focusMonth) return 1
      return left.key.localeCompare(right.key)
    })
})

const tableFocusGroup = computed(
  () => tableGroups.value.find((group) => group.key === selectedDate.value.slice(0, 7)) ?? tableGroups.value[0] ?? null,
)

const tablePreviewTasks = computed(() => tableFocusGroup.value?.tasks.slice(0, 6) ?? [])
const tableHiddenCount = computed(() => Math.max(0, (tableFocusGroup.value?.tasks.length ?? 0) - tablePreviewTasks.value.length))
const currentManagedTileIds = computed(() => tileOrder.value.filter((tileId) => tileState[tileId]))

function cloneBoardState(source) {
  return Object.fromEntries(
    Object.entries(source).map(([key, value]) => [key, { col: value.col, row: value.row, size: value.size }]),
  )
}

function cloneBoardLayout(layout) {
  return {
    order: [...layout.order],
    tiles: Object.fromEntries(
      Object.entries(layout.tiles).map(([key, value]) => [
        key,
        {
          col: value.col,
          row: value.row,
          size: value.size,
          width: value.width,
          height: value.height,
        },
      ]),
    ),
  }
}

function layoutToBoardState(layout) {
  return Object.fromEntries(
    Object.entries(layout.tiles).map(([key, value]) => [key, { col: value.col, row: value.row, size: value.size }]),
  )
}

function mergeVisibleOrder(fullOrder, visibleOrder, hiddenIds) {
  const nextVisible = [...visibleOrder]
  const hiddenSet = new Set(hiddenIds)

  return fullOrder.map((tileId) => {
    if (hiddenSet.has(tileId)) {
      return tileId
    }

    return nextVisible.shift() ?? tileId
  })
}

function parseSpan(token) {
  const [width, height] = token.split('x').map(Number)
  return { width, height }
}

function spanToken(width, height) {
  return `${width}x${height}`
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function getPlacementSearchRows(tiles) {
  const totalHeight = Object.values(tiles).reduce((sum, tile) => sum + parseSpan(tile.size).height, 0)
  return Math.max(BOARD_ROWS, totalHeight + 2)
}

function createGrid(rowCount) {
  return Array.from({ length: rowCount }, () => Array.from({ length: BOARD_COLS }, () => null))
}

function canPlace(grid, col, row, width, height) {
  if (col < 1 || row < 1 || col + width - 1 > BOARD_COLS || row + height - 1 > grid.length) {
    return false
  }

  for (let y = row; y < row + height; y += 1) {
    for (let x = col; x < col + width; x += 1) {
      if (grid[y - 1][x - 1]) {
        return false
      }
    }
  }

  return true
}

function occupy(grid, id, col, row, width, height) {
  for (let y = row; y < row + height; y += 1) {
    for (let x = col; x < col + width; x += 1) {
      grid[y - 1][x - 1] = id
    }
  }
}

function findNearestPlacement(size, grid, preferred = null, maxRows = grid.length) {
  const candidates = []
  for (let row = 1; row <= maxRows - size.height + 1; row += 1) {
    for (let col = 1; col <= BOARD_COLS - size.width + 1; col += 1) {
      if (!canPlace(grid, col, row, size.width, size.height)) {
        continue
      }

      const distance = preferred
        ? Math.abs(col - preferred.col) + Math.abs(row - preferred.row)
        : (row - 1) * BOARD_COLS + col

      candidates.push({ col, row, distance })
    }
  }

  candidates.sort((left, right) => left.distance - right.distance || left.row - right.row || left.col - right.col)
  return candidates[0] ?? null
}

function findColumnPlacement(size, grid, preferredCol, maxRows = grid.length) {
  const col = clamp(preferredCol ?? 1, 1, BOARD_COLS - size.width + 1)

  for (let row = 1; row <= maxRows - size.height + 1; row += 1) {
    if (canPlace(grid, col, row, size.width, size.height)) {
      return { col, row }
    }
  }

  return null
}

function findCompactPlacement(size, grid, preferred = null, maxRows = grid.length) {
  if (preferred) {
    const verticalPlacement = findColumnPlacement(size, grid, preferred.col, maxRows)
    if (verticalPlacement) {
      return verticalPlacement
    }
  }

  return findNearestPlacement(size, grid, preferred, maxRows)
}

function findPreviewPlacement(size, grid, preferred = null, maxRows = grid.length) {
  if (preferred) {
    const exactPlacement = clampPlacement(preferred.col, preferred.row, size, maxRows)
    if (canPlace(grid, exactPlacement.col, exactPlacement.row, size.width, size.height)) {
      return exactPlacement
    }
  }

  return findNearestPlacement(size, grid, preferred, maxRows)
}

function buildBoardLayout(order, sourceTiles, fixedTileId = null, fixedOverride = null) {
  const desiredTiles = cloneBoardState(sourceTiles)
  if (fixedTileId && fixedOverride) {
    desiredTiles[fixedTileId] = { ...desiredTiles[fixedTileId], ...fixedOverride }
  }

  const searchRows = getPlacementSearchRows(desiredTiles)
  const grid = createGrid(searchRows)
  const resolved = {}
  const resolvedOrder = []

  const placeTile = (id, placement, size) => {
    occupy(grid, id, placement.col, placement.row, size.width, size.height)
    resolved[id] = {
      col: placement.col,
      row: placement.row,
      size: spanToken(size.width, size.height),
      width: size.width,
      height: size.height,
    }
    resolvedOrder.push(id)
  }

  const fixedId = fixedTileId && desiredTiles[fixedTileId] ? fixedTileId : null
  if (fixedId) {
    const fixedTile = desiredTiles[fixedId]
    const fixedSize = parseSpan(fixedTile.size)
    const placement = findCompactPlacement(fixedSize, grid, { col: fixedTile.col, row: fixedTile.row }, searchRows)
    if (!placement) {
      return null
    }
    placeTile(fixedId, placement, fixedSize)
  }

  for (const id of order) {
    if (id === fixedId || !desiredTiles[id]) {
      continue
    }

    const tile = desiredTiles[id]
    const size = parseSpan(tile.size)
    const placement = findCompactPlacement(size, grid, { col: tile.col, row: tile.row }, searchRows)
    if (!placement) {
      return null
    }
    placeTile(id, placement, size)
  }

  return {
    order: resolvedOrder,
    tiles: resolved,
  }
}

function buildPreviewLayout(order, sourceTiles, fixedTileId = null, fixedOverride = null) {
  const desiredTiles = cloneBoardState(sourceTiles)
  if (fixedTileId && fixedOverride) {
    desiredTiles[fixedTileId] = { ...desiredTiles[fixedTileId], ...fixedOverride }
  }

  const searchRows = getPlacementSearchRows(desiredTiles)
  const grid = createGrid(searchRows)
  const resolved = {}
  const resolvedOrder = []

  const placeTile = (id, placement, size) => {
    occupy(grid, id, placement.col, placement.row, size.width, size.height)
    resolved[id] = {
      col: placement.col,
      row: placement.row,
      size: spanToken(size.width, size.height),
      width: size.width,
      height: size.height,
    }
    resolvedOrder.push(id)
  }

  const fixedId = fixedTileId && desiredTiles[fixedTileId] ? fixedTileId : null
  if (fixedId) {
    const fixedTile = desiredTiles[fixedId]
    const fixedSize = parseSpan(fixedTile.size)
    const placement = findPreviewPlacement(fixedSize, grid, { col: fixedTile.col, row: fixedTile.row }, searchRows)
    if (!placement) {
      return null
    }
    placeTile(fixedId, placement, fixedSize)
  }

  for (const id of order) {
    if (id === fixedId || !desiredTiles[id]) {
      continue
    }

    const tile = desiredTiles[id]
    const size = parseSpan(tile.size)
    const placement = findNearestPlacement(size, grid, { col: tile.col, row: tile.row }, searchRows)
    if (!placement) {
      return null
    }
    placeTile(id, placement, size)
  }

  return {
    order: resolvedOrder,
    tiles: resolved,
  }
}

const activeTileOrder = computed(() => tileOrder.value.filter((tileId) => !hiddenTileIds.value.includes(tileId)))

const settledLayout = computed(
  () => buildBoardLayout(activeTileOrder.value, tileState) ?? buildBoardLayout(defaultBoardState.order, defaultBoardState.tiles),
)
const activeLayout = computed(() => dragPreview.value?.layout ?? settledLayout.value)

function applyResolvedLayout(layout) {
  if (!layout) {
    return
  }

  tileOrder.value = mergeVisibleOrder(tileOrder.value, layout.order, hiddenTileIds.value)
  Object.entries(layout.tiles).forEach(([id, placement]) => {
    tileState[id] = { col: placement.col, row: placement.row, size: placement.size }
  })
}

function normalizeBoardLayout() {
  const layout = buildBoardLayout(activeTileOrder.value, tileState)
  if (!layout) {
    return
  }

  applyResolvedLayout(layout)
  persistBoardLayout()
}

function getBoardHeight(layout, cellWidth) {
  const fallbackHeight = cellWidth * BOARD_ROWS + BOARD_GAP * (BOARD_ROWS - 1)
  if (!layout) {
    return fallbackHeight
  }

  const maxBottom = Math.max(
    ...Object.values(layout.tiles).map((placement) => placement.row + placement.height - 1),
    1,
  )

  const contentHeight = maxBottom * cellWidth + BOARD_GAP * Math.max(0, maxBottom - 1)
  return contentHeight
}

const boardCanvasHeight = computed(() => {
  const width = boardMetrics.width || 1200
  const cellWidth = boardMetrics.cellWidth || (width - BOARD_GAP * (BOARD_COLS - 1)) / BOARD_COLS
  return getBoardHeight(activeLayout.value, cellWidth)
})

const boardCanvasStyle = computed(() => ({
  height: `${boardCanvasHeight.value}px`,
  '--board-pitch-x': `${boardMetrics.pitchX || (boardMetrics.cellWidth || 0) + BOARD_GAP}px`,
  '--board-pitch-y': `${boardMetrics.pitchY || (boardMetrics.cellHeight || boardMetrics.cellWidth || 0) + BOARD_GAP}px`,
}))

function boardMetricsSafe() {
  const width = boardMetrics.width || 1200
  const cellWidth = boardMetrics.cellWidth || (width - BOARD_GAP * (BOARD_COLS - 1)) / BOARD_COLS
  const cellHeight = boardMetrics.cellHeight || cellWidth
  return {
    width,
    height: boardCanvasHeight.value,
    cellWidth,
    cellHeight,
    pitchX: cellWidth + BOARD_GAP,
    pitchY: cellHeight + BOARD_GAP,
  }
}

function placementPixels(placement) {
  const metrics = boardMetricsSafe()
  const x = (placement.col - 1) * metrics.pitchX
  const y = (placement.row - 1) * metrics.pitchY
  const width = placement.width * metrics.cellWidth + (placement.width - 1) * BOARD_GAP
  const height = placement.height * metrics.cellHeight + (placement.height - 1) * BOARD_GAP
  return { x, y, width, height }
}

function placementCenter(placement) {
  const box = placementPixels(placement)
  return { x: box.x + box.width / 2, y: box.y + box.height / 2 }
}

function tilePlacement(tileId) {
  return activeLayout.value.tiles[tileId] ?? settledLayout.value.tiles[tileId]
}

function tileSpanToken(tileId) {
  return tilePlacement(tileId)?.size ?? tileState[tileId].size
}

function tileModeLabel(tileId, sizeToken) {
  const labels = {
    calendar: { '1x1': '오늘', '1x2': '주간', '2x2': '작은 달력', '3x1': '주간 달력', '3x3': '월간 달력' },
    performance: { '1x1': '핵심', '1x2': '상위 3명', '2x2': '팀 카드', '3x1': '스코어보드', '3x3': '상세 랭킹' },
    notes: { '1x1': '오늘/다음', '1x2': '일정 목록', '2x1': '짧은 메모', '2x2': '업무 보드', '3x1': '일일 요약' },
    table: { '1x1': '핵심 요약', '2x1': '5개 항목', '2x2': '요약 테이블', '3x1': '테이블 폭', '3x2': '확장 테이블', '3x3': '전체 목록' },
    meetings: { '1x1': '다음 회의', '1x2': '2개 회의', '2x1': '회의록', '2x2': '일정 보드' },
    aiBrief: { '1x1': 'AI', '1x2': '브리핑', '2x1': '추천', '2x2': '상세 AI' },
    riskAlert: { '1x1': '리스크', '1x2': '알림', '2x1': '주의', '2x2': '경고판' },
  }

  return labels[tileId]?.[sizeToken] ?? sizeToken
}

function isTileHidden(tileId) {
  return hiddenTileIds.value.includes(tileId)
}

function isTileVisible(tileId) {
  return tileOrder.value.includes(tileId) && !isTileHidden(tileId)
}

function ensureTileInOrder(tileId) {
  if (tileOrder.value.includes(tileId)) {
    return
  }

  tileOrder.value = [...tileOrder.value, tileId]
}

function setTileVisible(tileId, visible) {
  const next = new Set(hiddenTileIds.value)

  if (visible) {
    ensureTileInOrder(tileId)
    next.delete(tileId)
  } else {
    next.add(tileId)
  }

  hiddenTileIds.value = [...next]
  normalizeBoardLayout()
  closeTileMenu()
}

function openTileMenu(tileId) {
  if (tileMenuState.value.open && tileMenuState.value.tileId === tileId) {
    closeTileMenu()
    return
  }

  tileMenuState.value = {
    open: true,
    tileId,
  }
}

function closeTileMenu() {
  tileMenuState.value = { open: false, tileId: null }
}

function toggleTileHidden(tileId, hidden = true) {
  setTileVisible(tileId, !hidden)
}

function openBoardManager() {
  boardManagerOpen.value = !boardManagerOpen.value
  closeTileMenu()
  closeSizeModal()
}

function closeBoardManager() {
  boardManagerOpen.value = false
}

function tileCursor(tileId) {
  if (dragPreview.value?.tileId === tileId) {
    return 'grabbing'
  }

  const zone = hoverState.value.tileId === tileId ? hoverState.value.zone : 'none'
  if (zone === 'move') return 'grab'
  return 'default'
}

function tileDelay(tileId) {
  if (!dragPreview.value?.originLayout || !dragPreview.value?.layout) {
    return '0ms'
  }

  const from = dragPreview.value.originLayout.tiles[tileId]
  const to = dragPreview.value.layout.tiles[tileId]
  if (!from || !to) {
    return '0ms'
  }

  const distance = Math.abs(from.col - to.col) + Math.abs(from.row - to.row)
  return `${distance * 40}ms`
}

function tileStyle(tileId) {
  const placement = tilePlacement(tileId)
  const box = placementPixels(placement)
  const isDragging = dragPreview.value?.tileId === tileId

  return {
    '--tile-accent': tileMeta[tileId].accent,
    '--tile-delay': tileDelay(tileId),
    '--tile-x': `${box.x}px`,
    '--tile-y': `${box.y}px`,
    '--tile-width': `${box.width}px`,
    '--tile-height': `${box.height}px`,
    '--tile-scale': isDragging ? 1.02 : 1,
    zIndex: isDragging ? 20 : 1,
    opacity: dragPreview.value?.tileId === tileId && dragPreview.value.originLayout ? 0.98 : 1,
    cursor: tileCursor(tileId),
    transitionDelay: tileDelay(tileId),
    willChange: 'transform, width, height, opacity',
  }
}

function tileBodyMode(tileId) {
  return tilePlacement(tileId)?.size ?? '1x1'
}

function ghostStyle(tileId) {
  const placement = dragPreview.value?.originLayout?.tiles[tileId]
  if (!placement) {
    return null
  }

  const box = placementPixels(placement)
  return {
    transform: `translate(${box.x}px, ${box.y}px)`,
    width: `${box.width}px`,
    height: `${box.height}px`,
  }
}

function centerPath(from, to) {
  const dx = (from.x + to.x) / 2
  return `M ${from.x} ${from.y} C ${dx} ${from.y}, ${dx} ${to.y}, ${to.x} ${to.y}`
}

function dropStyle() {
  const placement = dragPreview.value?.previewPlacement
  if (!placement) {
    return null
  }

  const box = placementPixels(placement)
  return {
    transform: `translate(${box.x}px, ${box.y}px)`,
    width: `${box.width}px`,
    height: `${box.height}px`,
  }
}

const motionSegments = computed(() => {
  if (!dragPreview.value?.originLayout || !dragPreview.value?.layout) {
    return []
  }

  return activeTileOrder.value
    .map((tileId) => {
      const fromPlacement = dragPreview.value.originLayout.tiles[tileId]
      const toPlacement = dragPreview.value.layout.tiles[tileId]
      if (!fromPlacement || !toPlacement) {
        return null
      }

      const moved =
        fromPlacement.col !== toPlacement.col ||
        fromPlacement.row !== toPlacement.row ||
        fromPlacement.size !== toPlacement.size

      if (!moved) {
        return null
      }

      return {
        id: tileId,
        from: placementCenter(fromPlacement),
        to: placementCenter(toPlacement),
      }
    })
    .filter(Boolean)
})

function getInteractionZone(tileEl, event) {
  const rect = tileEl.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  const moveRing =
    x <= MOVE_HIT ||
    y <= MOVE_HIT ||
    x >= rect.width - MOVE_HIT ||
    y >= rect.height - MOVE_HIT

  if (moveRing) {
    return 'move'
  }

  return 'none'
}

function openSizeModal(tileId) {
  closeTileMenu()
  closeBoardManager()
  sizeModalState.value = { open: true, tileId }
}

function closeSizeModal() {
  sizeModalState.value = { open: false, tileId: null }
}

function sizeOptionsForTile(tileId) {
  const currentSize = tileSpanToken(tileId)

  return (allowedSizes[tileId] ?? ['1x1']).map((token) => {
    const draftState = cloneBoardState(tileState)
    draftState[tileId] = { ...draftState[tileId], size: token }
    const layout = buildBoardLayout(activeTileOrder.value, draftState)

    return {
      token,
      active: token === currentSize,
      available: Boolean(layout),
    }
  })
}

function applyTileSize(tileId, sizeToken) {
  const draftState = cloneBoardState(tileState)
  draftState[tileId] = { ...draftState[tileId], size: sizeToken }

  const nextLayout = buildBoardLayout(activeTileOrder.value, draftState)
  if (!nextLayout) {
    return
  }

  applyResolvedLayout(nextLayout)
  persistBoardLayout()
  closeSizeModal()
}

function updateHoverState(tileId, event) {
  if (dragPreview.value) {
    return
  }

  const tileEl = tileElements[tileId]
  if (!tileEl) {
    return
  }

  hoverState.value = { tileId, zone: getInteractionZone(tileEl, event) }
}

function clearHoverState(tileId) {
  if (hoverState.value.tileId === tileId) {
    hoverState.value = { tileId: null, zone: 'none' }
  }
}

function clampPlacement(col, row, size, rowLimit = Number.POSITIVE_INFINITY) {
  return {
    col: clamp(col, 1, BOARD_COLS - size.width + 1),
    row: clamp(row, 1, rowLimit - size.height + 1),
  }
}

function buildMovePreview(tileId, sourceState, originLayout, pointerX, pointerY) {
  const tile = originLayout.tiles[tileId]
  if (!tile) {
    return null
  }

  const metrics = boardMetricsSafe()
  const size = parseSpan(tile.size)
  const boardRect = boardRef.value?.getBoundingClientRect()
  if (!boardRect) {
    return null
  }

  const rowLimit = getPlacementSearchRows(sourceState)
  const localX = pointerX - boardRect.left
  const localY = pointerY - boardRect.top
  const desiredCol = clamp(Math.round((localX - dragPreview.value.pointerOffset.x) / metrics.pitchX) + 1, 1, BOARD_COLS - size.width + 1)
  const desiredRow = clamp(Math.round((localY - dragPreview.value.pointerOffset.y) / metrics.pitchY) + 1, 1, rowLimit - size.height + 1)
  const layout = buildPreviewLayout(activeTileOrder.value, sourceState, tileId, {
    col: desiredCol,
    row: desiredRow,
    size: tile.size,
  })

  if (!layout) {
    return null
  }

  return {
    layout,
    placement: layout.tiles[tileId],
    size: tile.size,
  }
}

function onDragPointerMove(event) {
  if (!dragPreview.value || event.pointerId !== dragPreview.value.pointerId) {
    return
  }

  const topEdge = 70
  const bottomEdge = window.innerHeight - 70
  if (event.clientY < topEdge) {
    window.scrollBy({ top: -18, behavior: 'auto' })
  } else if (event.clientY > bottomEdge) {
    window.scrollBy({ top: 18, behavior: 'auto' })
  }

  const originLayout = dragPreview.value.originLayout
  const sourceState = dragPreview.value.originState
  const tileId = dragPreview.value.tileId
  const preview = buildMovePreview(tileId, sourceState, originLayout, event.clientX, event.clientY)

  if (!preview) {
    return
  }

  dragPreview.value = {
    ...dragPreview.value,
    layout: preview.layout,
    previewPlacement: preview.placement,
    previewSize: preview.size,
  }
}

function persistBoardLayout() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(
    BOARD_STORAGE_KEY,
    JSON.stringify({
      order: tileOrder.value,
      tiles: tileState,
      hidden: hiddenTileIds.value,
    }),
  )
}

function finishDrag() {
  if (!dragPreview.value) {
    return
  }

  const previewLayout = dragPreview.value.layout ?? settledLayout.value
  const finalLayout = previewLayout
    ? buildBoardLayout(activeTileOrder.value, layoutToBoardState(previewLayout)) ?? previewLayout
    : null

  if (finalLayout) {
    applyResolvedLayout(finalLayout)
    persistBoardLayout()
  }

  dragPreview.value = null
  hoverState.value = { tileId: null, zone: 'none' }
  window.removeEventListener('pointermove', onDragPointerMove)
  window.removeEventListener('pointerup', finishDrag)
  window.removeEventListener('pointercancel', finishDrag)
  document.body.style.userSelect = ''
}

function startDrag(tileId, event) {
  if (event.button !== 0) {
    return
  }

  const tileEl = tileElements[tileId]
  if (!tileEl) {
    return
  }

  if (event.target.closest('button, a, input, textarea, select')) {
    return
  }

  const zone = getInteractionZone(tileEl, event)
  if (zone === 'none') {
    return
  }

  const originLayout = settledLayout.value
  const originPlacement = originLayout.tiles[tileId]
  if (!originPlacement) {
    return
  }

  event.preventDefault()
  const rect = tileEl.getBoundingClientRect()
  const sourceState = layoutToBoardState(originLayout)

  dragPreview.value = {
    tileId,
    mode: 'move',
    pointerId: event.pointerId,
    startPointer: { x: event.clientX, y: event.clientY },
    pointerOffset: { x: event.clientX - rect.left, y: event.clientY - rect.top },
    originLayout: cloneBoardLayout(originLayout),
    originState: sourceState,
    layout: originLayout,
    previewPlacement: originPlacement,
    previewSize: originPlacement.size,
  }

  window.addEventListener('pointermove', onDragPointerMove)
  window.addEventListener('pointerup', finishDrag)
  window.addEventListener('pointercancel', finishDrag)
  document.body.style.userSelect = 'none'
}

function setTileRef(tileId, element) {
  if (element) {
    tileElements[tileId] = element
  } else {
    delete tileElements[tileId]
  }
}

function selectDate(dateKey) {
  selectedDate.value = dateKey
}

function openTask(taskId) {
  store.openTask(taskId)
}

function openRailModal(key) {
  activeRail.value = key
  toolModalOpen.value = true
}

function closeRailModal() {
  toolModalOpen.value = false
}

function memberName(memberId) {
  return store.findMember(memberId)?.name ?? '미지정'
}

function memberFor(memberId) {
  return store.findMember(memberId)
}

function taskScheduleLabel(task) {
  if (task.startDate === task.dueDate) {
    return formatShortDate(task.dueDate)
  }

  return `${formatShortDate(task.startDate)} - ${formatShortDate(task.dueDate)}`
}

function saveTaskNote() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(NOTE_STORAGE_KEY, taskNoteDraft.value)
}

function loadBoardLayout() {
  if (typeof window === 'undefined') {
    return
  }

  const stored = window.localStorage.getItem(BOARD_STORAGE_KEY)
  if (!stored) {
    persistBoardLayout()
    return
  }

  try {
    const parsed = JSON.parse(stored)
    if (Array.isArray(parsed?.order)) {
      const ordered = parsed.order.filter((id) => tileState[id])
      const missing = defaultBoardState.order.filter((id) => !ordered.includes(id))
      tileOrder.value = [...ordered, ...missing]
    }

    if (parsed?.tiles && typeof parsed.tiles === 'object') {
      Object.entries(parsed.tiles).forEach(([id, value]) => {
        if (!tileState[id] || typeof value !== 'object') {
          return
        }

        tileState[id] = {
          col: clamp(Number(value.col) || tileState[id].col, 1, BOARD_COLS),
          row: Math.max(1, Number(value.row) || tileState[id].row),
          size: allowedSizes[id].includes(value.size) ? value.size : tileState[id].size,
        }
      })
    }

    if (Array.isArray(parsed?.hidden)) {
      hiddenTileIds.value = parsed.hidden.filter((id) => tileState[id])
    }
  } catch {
    persistBoardLayout()
  }
}

function loadTaskNote() {
  if (typeof window === 'undefined') {
    return
  }

  taskNoteDraft.value = window.localStorage.getItem(NOTE_STORAGE_KEY) ?? ''
}

function boardMetricsRefresh() {
  const rect = boardRef.value?.getBoundingClientRect()
  if (!rect) {
    return
  }

  const cellWidth = (rect.width - BOARD_GAP * (BOARD_COLS - 1)) / BOARD_COLS
  const cellHeight = cellWidth

  boardMetrics.width = rect.width
  boardMetrics.height = rect.height
  boardMetrics.cellWidth = cellWidth
  boardMetrics.cellHeight = cellHeight
  boardMetrics.pitchX = cellWidth + BOARD_GAP
  boardMetrics.pitchY = cellHeight + BOARD_GAP
}

let resizeObserver = null

onMounted(() => {
  loadBoardLayout()
  loadTaskNote()
  normalizeBoardLayout()
  boardMetricsRefresh()

  if (boardRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      boardMetricsRefresh()
    })
    resizeObserver.observe(boardRef.value)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  finishDrag()
})

watch(taskNoteDraft, saveTaskNote)
</script>

<template>
  <section class="dashboard-page">
    <header class="dashboard-hero">
      <div class="dashboard-hero__copy">
        <p class="section-eyebrow">MARKETING B2B DASHBOARD</p>
        <h2>마케팅 운영을 한 화면에서 관리합니다</h2>
        <p class="dashboard-hero__desc">
          칸의 테두리에서 드래그하면 이동하고, 오른쪽 위 메뉴에서 숨기기와 크기 변경을 할 수 있습니다. 모든 요약 기능은 6열 3행 보드 안에서만 움직입니다.
        </p>
      </div>

      <div class="dashboard-hero__meta">
        <span>{{ formatLongDate(selectedDate) }}</span>
        <button class="dashboard-hero__meta-menu" type="button" aria-label="보드 관리" @click="openBoardManager">
          ...
        </button>
      </div>
    </header>

    <section class="dashboard-board-shell surface-card" @pointerdown="closeTileMenu">
      <div class="dashboard-board-toolbar">
        <span class="dashboard-board-toolbar__date">{{ formatLongDate(selectedDate) }}</span>
        <button class="dashboard-board-toolbar__menu" type="button" aria-label="Board manager" @click="openBoardManager">
          ...
        </button>
      </div>

      <div class="dashboard-board" ref="boardRef" :style="boardCanvasStyle">
        <svg
          v-if="dragPreview?.layout && dragPreview?.originLayout"
          class="dashboard-board__paths"
          :viewBox="`0 0 ${boardMetrics.width || 1200} ${boardMetrics.height || 600}`"
          preserveAspectRatio="none"
        >
          <path
            v-for="segment in motionSegments"
            :key="segment.id"
            :d="centerPath(segment.from, segment.to)"
            class="dashboard-board__path"
          />
        </svg>

        <div
          v-if="dragPreview?.originLayout && dragPreview.tileId"
          class="dashboard-tile dashboard-tile--ghost"
          :style="ghostStyle(dragPreview.tileId)"
        />

        <div
          v-if="dragPreview?.previewPlacement"
          class="dashboard-tile dashboard-tile--drop"
          :style="dropStyle()"
        >
          <span>예상 위치</span>
        </div>

        <article
          v-for="tileId in activeTileOrder"
          :key="tileId"
          :ref="(el) => setTileRef(tileId, el)"
          class="dashboard-tile"
          :class="[`dashboard-tile--${tileId}`, { 'dashboard-tile--dragging': dragPreview?.tileId === tileId }]"
          :style="tileStyle(tileId)"
          @pointermove="(event) => updateHoverState(tileId, event)"
          @pointerleave="() => clearHoverState(tileId)"
          @pointerdown="(event) => startDrag(tileId, event)"
        >
          <header class="dashboard-tile__head">
            <div class="dashboard-tile__head-top">
              <span class="dashboard-tile__badge">{{ tileModeLabel(tileId, tileSpanToken(tileId)) }}</span>
              <button
                class="dashboard-tile__menu"
                type="button"
                aria-label="칸 메뉴"
                @pointerdown.stop
                @click.stop="openTileMenu(tileId)"
              >
                ...
              </button>
            </div>
            <div class="dashboard-tile__head-copy">
              <p class="dashboard-tile__eyebrow" :style="{ color: tileMeta[tileId].accent }">
                {{ tileMeta[tileId].eyebrow }}
              </p>
              <h3>{{ tileMeta[tileId].title }}</h3>
              <p class="dashboard-tile__desc">{{ tileMeta[tileId].description }}</p>
            </div>
            <div
              v-if="tileMenuState.open && tileMenuState.tileId === tileId"
              class="dashboard-tile__menu-panel surface-card"
              @pointerdown.stop
            >
              <button class="dashboard-tile__menu-item" type="button" @click="toggleTileHidden(tileId, true)">
                숨기기
              </button>
              <button class="dashboard-tile__menu-item" type="button" @click="openSizeModal(tileId)">
                크기 변경
              </button>
            </div>
          </header>

          <div class="dashboard-tile__body dashboard-scrollbar-hidden">
            <template v-if="tileId === 'calendar'">
              <template v-if="tileBodyMode('calendar') === '1x1'">
                <div class="dashboard-stack">
                  <button
                    v-for="task in selectedDateTasks"
                    :key="task.id"
                    class="summary-line"
                    type="button"
                    @click.stop="openTask(task.id)"
                  >
                    <span class="summary-line__dot" :style="{ backgroundColor: task.palette.accent }" />
                    <div class="summary-line__copy">
                      <strong>{{ task.title }}</strong>
                      <span>{{ formatShortDate(task.dueDate) }}</span>
                    </div>
                    <strong class="summary-line__meta">{{ task.progress }}%</strong>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('calendar') === '1x2'">
                <div class="dashboard-stack">
                  <button
                    v-for="day in weekDays.slice(0, 5)"
                    :key="day.key"
                    class="calendar-row"
                    type="button"
                    :class="{ 'calendar-row--active': day.key === selectedDate }"
                    @click.stop="selectDate(day.key)"
                  >
                    <div class="calendar-row__date">
                      <strong>{{ day.dayNumber }}</strong>
                      <span>{{ day.weekdayLabel }}</span>
                    </div>
                    <div class="calendar-row__meta">
                      <span>{{ day.tasks.length }}건</span>
                      <strong>{{ day.tasks[0]?.title ?? '일정 없음' }}</strong>
                    </div>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('calendar') === '2x2'">
                <div class="calendar-panel calendar-panel--stack">
                  <div class="mini-month">
                    <div class="mini-month__weekdays">
                      <span v-for="label in ['월', '화', '수', '목', '금', '토', '일']" :key="label">{{ label }}</span>
                    </div>
                    <div v-for="week in monthWeeks.slice(0, 3)" :key="week[0].key" class="mini-month__week">
                      <button
                        v-for="day in week"
                        :key="day.key"
                        type="button"
                        class="mini-month__day"
                        :class="{
                          'mini-month__day--active': day.key === selectedDate,
                          'mini-month__day--muted': !day.isCurrentMonth,
                        }"
                        @click.stop="selectDate(day.key)"
                      >
                        <strong>{{ day.dayNumber }}</strong>
                        <span :style="{ backgroundColor: day.tasks[0]?.palette.accent ?? 'transparent' }" />
                      </button>
                    </div>
                  </div>

                  <div class="calendar-agenda calendar-agenda--compact">
                    <div class="calendar-agenda__head">
                      <div>
                        <p>{{ selectedCalendarLabel }}</p>
                        <strong>{{ selectedCalendarTasks.length }}건 일정</strong>
                      </div>
                      <span v-if="selectedCalendarTasks.length > 3">{{ selectedCalendarTasks.length - 3 }}건 더 있음</span>
                      <span v-else-if="selectedCalendarTasks.length">선택한 날짜</span>
                      <span v-else>일정 없음</span>
                    </div>

                    <div v-if="selectedCalendarTasks.length" class="calendar-agenda__list">
                      <button
                        v-for="task in selectedCalendarTasks.slice(0, 3)"
                        :key="`calendar-compact-${task.id}`"
                        class="calendar-agenda__item calendar-agenda__item--compact"
                        type="button"
                        @click.stop="openTask(task.id)"
                      >
                        <span class="calendar-agenda__item-dot" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="calendar-agenda__item-copy">
                          <strong>{{ task.title }}</strong>
                          <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }} / {{ taskScheduleLabel(task) }}</span>
                        </div>
                        <strong class="calendar-agenda__item-progress">{{ task.progress }}%</strong>
                      </button>
                    </div>

                    <p v-else class="calendar-agenda__empty">선택한 날짜에 등록된 일정이 없습니다.</p>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('calendar') === '3x1'">
                <div class="calendar-panel calendar-panel--stack">
                  <div class="week-lane">
                    <button
                      v-for="day in weekDays"
                      :key="day.key"
                      class="week-lane__cell"
                      :class="{ 'week-lane__cell--active': day.key === selectedDate }"
                      type="button"
                      @click.stop="selectDate(day.key)"
                    >
                      <p>{{ day.weekdayLabel }}</p>
                      <strong>{{ day.dayNumber }}</strong>
                      <span>{{ day.tasks.length }}건</span>
                    </button>
                  </div>

                  <div class="calendar-agenda calendar-agenda--week">
                    <div class="calendar-agenda__head">
                      <div>
                        <p>{{ selectedCalendarLabel }}</p>
                        <strong>{{ selectedCalendarTasks.length }}건 일정</strong>
                      </div>
                      <span v-if="selectedCalendarTasks.length > 3">{{ selectedCalendarTasks.length - 3 }}건 더 있음</span>
                      <span v-else-if="selectedCalendarTasks.length">주간 일정 보기</span>
                      <span v-else>일정 없음</span>
                    </div>

                    <div v-if="selectedCalendarTasks.length" class="calendar-agenda__list">
                      <button
                        v-for="task in selectedCalendarTasks.slice(0, 3)"
                        :key="`calendar-week-${task.id}`"
                        class="calendar-agenda__item"
                        type="button"
                        @click.stop="openTask(task.id)"
                      >
                        <span class="calendar-agenda__item-dot" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="calendar-agenda__item-copy">
                          <strong>{{ task.title }}</strong>
                          <span>{{ task.summary }}</span>
                        </div>
                        <div class="calendar-agenda__item-side">
                          <small>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</small>
                          <strong>{{ taskScheduleLabel(task) }}</strong>
                        </div>
                      </button>
                    </div>

                    <p v-else class="calendar-agenda__empty">선택한 날짜의 일정이 여기에 리스트로 표시됩니다.</p>
                  </div>
                </div>
              </template>

              <template v-else-if="false">
                <div class="dashboard-table-detail">
                  <div class="dashboard-table-detail__summary">
                    <div>
                      <p>{{ tableFocusGroup?.label ?? '현재 월' }}</p>
                      <strong>{{ tableFocusGroup?.tasks.length ?? 0 }}건</strong>
                    </div>
                    <span>{{ tableHiddenCount }}건 더 있음</span>
                  </div>
                  <div class="dashboard-table-board dashboard-scrollbar-hidden">
                    <div class="dashboard-table-board__head">
                      <span>Task</span>
                      <span>Priority</span>
                      <span>Owner</span>
                      <span>Start</span>
                      <span>Type</span>
                      <span>Status</span>
                      <span>Progress</span>
                      <span>Due</span>
                    </div>
                    <button
                      v-for="task in tableFocusGroup?.tasks ?? []"
                      :key="`table-detail-${task.id}`"
                      class="dashboard-table-board__row"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }}</small>
                        </div>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.priorityLabels[task.priority] ?? task.priority }}
                      </span>
                      <div class="dashboard-table-owner">
                        <span
                          class="dashboard-table-avatar"
                          :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                        >
                          {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                        </span>
                        <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.startDate) }}</span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                      >
                        {{ task.contentType }}
                      </span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.statusLabels[task.status] ?? task.status }}
                      </span>
                      <div class="dashboard-table-progress-shell">
                        <div class="dashboard-table-progress">
                          <span :style="{ width: `${task.progress}%` }" />
                        </div>
                        <strong>{{ task.progress }}%</strong>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else-if="false">
                <div class="dashboard-table-detail">
                  <div class="dashboard-table-detail__summary">
                    <div>
                      <p>{{ tableFocusGroup?.label ?? 'Current month' }}</p>
                      <strong>{{ tableFocusGroup?.tasks.length ?? 0 }} items</strong>
                    </div>
                    <span>{{ tableHiddenCount }} more</span>
                  </div>
                  <div class="dashboard-table-board dashboard-scrollbar-hidden">
                    <div class="dashboard-table-board__head">
                      <span>Task</span>
                      <span>Priority</span>
                      <span>Owner</span>
                      <span>Start</span>
                      <span>Type</span>
                      <span>Status</span>
                      <span>Progress</span>
                      <span>Due</span>
                    </div>
                    <button
                      v-for="task in tableFocusGroup?.tasks ?? []"
                      :key="`table-detail-${task.id}`"
                      class="dashboard-table-board__row"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }}</small>
                        </div>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.priorityLabels[task.priority] ?? task.priority }}
                      </span>
                      <div class="dashboard-table-owner">
                        <span
                          class="dashboard-table-avatar"
                          :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                        >
                          {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                        </span>
                        <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.startDate) }}</span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                      >
                        {{ task.contentType }}
                      </span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.statusLabels[task.status] ?? task.status }}
                      </span>
                      <div class="dashboard-table-progress-shell">
                        <div class="dashboard-table-progress">
                          <span :style="{ width: `${task.progress}%` }" />
                        </div>
                        <strong>{{ task.progress }}%</strong>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else-if="false">
                <div class="dashboard-table-detail">
                  <div class="dashboard-table-detail__summary">
                    <div>
                      <p>{{ tableFocusGroup?.label ?? 'Current month' }}</p>
                      <strong>{{ tableFocusGroup?.tasks.length ?? 0 }} items</strong>
                    </div>
                    <span>{{ tableHiddenCount }} more</span>
                  </div>
                  <div class="dashboard-table-board dashboard-scrollbar-hidden">
                    <div class="dashboard-table-board__head">
                      <span>Task</span>
                      <span>Priority</span>
                      <span>Owner</span>
                      <span>Start</span>
                      <span>Type</span>
                      <span>Status</span>
                      <span>Progress</span>
                      <span>Due</span>
                    </div>
                    <button
                      v-for="task in tableFocusGroup?.tasks ?? []"
                      :key="`table-detail-${task.id}`"
                      class="dashboard-table-board__row"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }}</small>
                        </div>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.priorityLabels[task.priority] ?? task.priority }}
                      </span>
                      <div class="dashboard-table-owner">
                        <span
                          class="dashboard-table-avatar"
                          :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                        >
                          {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                        </span>
                        <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.startDate) }}</span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                      >
                        {{ task.contentType }}
                      </span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.statusLabels[task.status] ?? task.status }}
                      </span>
                      <div class="dashboard-table-progress-shell">
                        <div class="dashboard-table-progress">
                          <span :style="{ width: `${task.progress}%` }" />
                        </div>
                        <strong>{{ task.progress }}%</strong>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="month-view">
                  <div class="month-view__summary">
                    <div>
                      <p>현재 월</p>
                      <strong>{{ formatMonthLabel(selectedDate) }}</strong>
                    </div>
                    <div>
                      <p>선택 일정</p>
                      <strong>{{ selectedCalendarTasks.length }}건</strong>
                    </div>
                  </div>
                  <div class="month-view__calendar dashboard-scrollbar-hidden">
                    <div class="mini-month">
                      <div class="mini-month__weekdays">
                        <span v-for="label in ['월', '화', '수', '목', '금', '토', '일']" :key="label">{{ label }}</span>
                      </div>
                      <div v-for="week in monthWeeks" :key="week[0].key" class="mini-month__week">
                        <button
                          v-for="day in week"
                          :key="day.key"
                          type="button"
                          class="mini-month__day"
                          :class="{
                            'mini-month__day--active': day.key === selectedDate,
                            'mini-month__day--muted': !day.isCurrentMonth,
                          }"
                          @click.stop="selectDate(day.key)"
                        >
                          <strong>{{ day.dayNumber }}</strong>
                          <span :style="{ backgroundColor: day.tasks[0]?.palette.accent ?? 'transparent' }" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <div class="calendar-agenda calendar-agenda--detail">
                    <div class="calendar-agenda__head">
                      <div>
                        <p>{{ selectedCalendarLabel }}</p>
                        <strong>{{ selectedCalendarTasks.length }}건 일정</strong>
                      </div>
                      <span v-if="selectedCalendarTasks.length">선택 날짜 상세</span>
                      <span v-else>일정 없음</span>
                    </div>

                    <div v-if="selectedCalendarTasks.length" class="calendar-agenda__list calendar-agenda__list--detail">
                      <button
                        v-for="task in selectedCalendarTasks"
                        :key="`calendar-detail-${task.id}`"
                        class="calendar-agenda__item calendar-agenda__item--detail"
                        type="button"
                        @click.stop="openTask(task.id)"
                      >
                        <div class="calendar-agenda__item-main">
                          <span class="calendar-agenda__item-dot" :style="{ backgroundColor: task.palette.accent }" />
                          <div class="calendar-agenda__item-copy">
                            <strong>{{ task.title }}</strong>
                            <span>{{ task.requirementId }} / {{ task.summary }}</span>
                          </div>
                        </div>

                        <div class="calendar-agenda__item-pills">
                          <span
                            class="calendar-agenda__item-pill"
                            :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd' }"
                          >
                            {{ store.priorityLabels[task.priority] ?? task.priority }}
                          </span>
                          <span class="calendar-agenda__item-pill" :style="{ backgroundColor: task.palette.accent }">
                            {{ task.contentType }}
                          </span>
                          <span
                            class="calendar-agenda__item-pill"
                            :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd' }"
                          >
                            {{ store.statusLabels[task.status] ?? task.status }}
                          </span>
                        </div>

                        <div class="calendar-agenda__item-meta">
                          <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                          <span>{{ taskScheduleLabel(task) }}</span>
                          <strong>{{ task.progress }}%</strong>
                        </div>
                      </button>
                    </div>

                    <p v-else class="calendar-agenda__empty">선택한 날짜에 등록된 일정이 없습니다.</p>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="tileId === 'performance'">
              <template v-if="tileBodyMode('performance') === '1x1'">
                <div class="metric-triplet">
                  <div class="metric-box">
                    <p>평균 진행률</p>
                    <strong>{{ teamMetrics.averageProgress }}%</strong>
                  </div>
                  <div class="metric-box">
                    <p>완료율</p>
                    <strong>{{ teamMetrics.completionRate }}%</strong>
                  </div>
                  <div class="metric-box">
                    <p>리스크</p>
                    <strong>{{ teamMetrics.atRisk }}</strong>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('performance') === '1x2'">
                <div class="performance-list">
                  <button
                    v-for="row in performanceRows.slice(0, 3)"
                    :key="row.member.id"
                    class="person-row"
                    type="button"
                    @click.stop="selectedMemberId = row.member.id"
                  >
                    <div class="person-row__head">
                      <span class="person-row__avatar" :style="{ backgroundColor: row.member.accent }">{{ row.member.initials }}</span>
                      <div>
                        <strong>{{ row.member.name }}</strong>
                        <p>{{ row.member.role }}</p>
                      </div>
                      <span>{{ row.score }}</span>
                    </div>
                    <div class="person-row__bar"><span :style="{ width: `${row.score}%`, backgroundColor: row.member.accent }" /></div>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('performance') === '2x2'">
                <div class="performance-grid">
                  <button
                    v-for="row in performanceRows.slice(0, 4)"
                    :key="row.member.id"
                    class="performance-card"
                    type="button"
                    :class="{ 'performance-card--active': selectedMemberRow?.member.id === row.member.id }"
                    @click.stop="selectedMemberId = row.member.id"
                  >
                    <div class="person-row__head">
                      <span class="person-row__avatar" :style="{ backgroundColor: row.member.accent }">{{ row.member.initials }}</span>
                      <div>
                        <strong>{{ row.member.name }}</strong>
                        <p>{{ row.member.team }}</p>
                      </div>
                      <span>{{ row.score }}</span>
                    </div>
                    <div class="person-row__bar"><span :style="{ width: `${row.score}%`, backgroundColor: row.member.accent }" /></div>
                    <div class="performance-card__tags">
                      <span>완료 {{ row.done }}</span>
                      <span>진행 {{ row.inProgress }}</span>
                      <span>리스크 {{ row.atRisk }}</span>
                    </div>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('performance') === '3x1'">
                <div class="scoreboard-row">
                  <button
                    v-for="row in performanceRows.slice(0, 5)"
                    :key="row.member.id"
                    class="scoreboard-chip"
                    type="button"
                    @click.stop="selectedMemberId = row.member.id"
                  >
                    <span :style="{ backgroundColor: row.member.accent }">{{ row.member.initials }}</span>
                    <strong>{{ row.member.name }}</strong>
                    <em>{{ row.score }}</em>
                  </button>
                </div>
              </template>

              <template v-else-if="false">
                <div class="dashboard-table-detail">
                  <div class="dashboard-table-detail__summary">
                    <div>
                      <p>{{ tableFocusGroup?.label ?? 'Current month' }}</p>
                      <strong>{{ tableFocusGroup?.tasks.length ?? 0 }} items</strong>
                    </div>
                    <span>{{ tableHiddenCount }} more</span>
                  </div>
                  <div class="dashboard-table-board dashboard-scrollbar-hidden">
                    <div class="dashboard-table-board__head">
                      <span>Task</span>
                      <span>Priority</span>
                      <span>Owner</span>
                      <span>Start</span>
                      <span>Type</span>
                      <span>Status</span>
                      <span>Progress</span>
                      <span>Due</span>
                    </div>
                    <button
                      v-for="task in tableFocusGroup?.tasks ?? []"
                      :key="`table-detail-${task.id}`"
                      class="dashboard-table-board__row"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }}</small>
                        </div>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.priorityLabels[task.priority] ?? task.priority }}
                      </span>
                      <div class="dashboard-table-owner">
                        <span
                          class="dashboard-table-avatar"
                          :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                        >
                          {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                        </span>
                        <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.startDate) }}</span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                      >
                        {{ task.contentType }}
                      </span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.statusLabels[task.status] ?? task.status }}
                      </span>
                      <div class="dashboard-table-progress-shell">
                        <div class="dashboard-table-progress">
                          <span :style="{ width: `${task.progress}%` }" />
                        </div>
                        <strong>{{ task.progress }}%</strong>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="performance-detail">
                  <div class="metric-triplet metric-triplet--wide">
                    <div class="metric-box">
                      <p>평균 진행률</p>
                      <strong>{{ teamMetrics.averageProgress }}%</strong>
                    </div>
                    <div class="metric-box">
                      <p>완료율</p>
                      <strong>{{ teamMetrics.completionRate }}%</strong>
                    </div>
                    <div class="metric-box">
                      <p>7일 내 마감</p>
                      <strong>{{ teamMetrics.dueSoon }}</strong>
                    </div>
                  </div>

                  <div class="performance-list performance-list--detailed">
                    <button
                      v-for="row in performanceRows"
                      :key="row.member.id"
                      class="person-row"
                      type="button"
                      :class="{ 'person-row--active': selectedMemberRow?.member.id === row.member.id }"
                      @click.stop="selectedMemberId = row.member.id"
                    >
                      <div class="person-row__head">
                        <span class="person-row__avatar" :style="{ backgroundColor: row.member.accent }">{{ row.member.initials }}</span>
                        <div>
                          <strong>{{ row.member.name }}</strong>
                          <p>{{ row.member.role }}</p>
                        </div>
                        <span>{{ row.score }}</span>
                      </div>
                      <div class="person-row__bar"><span :style="{ width: `${row.score}%`, backgroundColor: row.member.accent }" /></div>
                      <div class="performance-card__tags">
                        <span>완료 {{ row.done }}</span>
                        <span>진행 {{ row.inProgress }}</span>
                        <span>리스크 {{ row.atRisk }}</span>
                      </div>
                    </button>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="tileId === 'notes'">
              <template v-if="tileBodyMode('notes') === '1x1'">
                <div class="note-compact">
                  <div class="metric-box metric-box--small">
                    <p>오늘</p>
                    <strong>{{ selectedDateTasks.length }}건</strong>
                  </div>
                  <div class="metric-box metric-box--small">
                    <p>다음</p>
                    <strong>{{ upcomingTasks.length }}건</strong>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('notes') === '1x2'">
                <div class="dashboard-stack">
                  <div class="note-card">
                    <p>오늘 업무</p>
                    <strong>{{ selectedDateTasks.length }}건</strong>
                    <span>우선순위와 담당자를 정리합니다.</span>
                  </div>
                  <div class="note-card">
                    <p>다음 업무</p>
                    <strong>{{ upcomingTasks.length }}건</strong>
                    <span>내일 이후 액션을 짧게 묶어둡니다.</span>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('notes') === '2x1'">
                <div class="dashboard-stack">
                  <button v-for="task in selectedDateTasks.slice(0, 3)" :key="task.id" class="summary-line" type="button" @click.stop="openTask(task.id)">
                    <span class="summary-line__dot" :style="{ backgroundColor: task.palette.accent }" />
                    <div class="summary-line__copy">
                      <strong>{{ task.title }}</strong>
                      <span>{{ formatShortDate(task.dueDate) }}</span>
                    </div>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('notes') === '2x2'">
                <div class="notes-board">
                  <div class="note-card">
                    <p>오늘 요약</p>
                    <strong>{{ selectedDateTasks.length }}건</strong>
                    <span>핵심 액션을 먼저 처리합니다.</span>
                  </div>
                  <div class="note-card">
                    <p>다음 요약</p>
                    <strong>{{ upcomingTasks.length }}건</strong>
                    <span>연결되는 후속 업무를 정리합니다.</span>
                  </div>
                  <div class="note-list">
                    <button v-for="task in upcomingTasks.slice(0, 3)" :key="task.id" class="summary-line" type="button" @click.stop="openTask(task.id)">
                      <span class="summary-line__dot" :style="{ backgroundColor: task.palette.accent }" />
                      <div class="summary-line__copy">
                        <strong>{{ task.title }}</strong>
                        <span>{{ formatShortDate(task.startDate) }}</span>
                      </div>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="dashboard-stack">
                  <div class="note-card note-card--wide">
                    <p>오늘의 업무일지</p>
                    <strong>{{ selectedDateTasks.length }}건</strong>
                    <span>오늘 열어볼 카드와 바로 이어질 카드들을 먼저 정리합니다.</span>
                  </div>
                  <div class="note-list">
                    <button
                      v-for="task in selectedDateTasks.concat(upcomingTasks).slice(0, 5)"
                      :key="task.id"
                      class="summary-line"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <span class="summary-line__dot" :style="{ backgroundColor: task.palette.accent }" />
                      <div class="summary-line__copy">
                        <strong>{{ task.title }}</strong>
                        <span>{{ formatShortDate(task.dueDate) }}</span>
                      </div>
                      <strong class="summary-line__meta">{{ task.progress }}%</strong>
                    </button>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="tileId === 'table'">
              <template v-if="tileBodyMode('table') === '1x1'">
                <div class="metric-triplet">
                  <div class="metric-box">
                    <p>월 그룹</p>
                    <strong>{{ tableGroups.length }}</strong>
                  </div>
                  <div class="metric-box">
                    <p>선택 월</p>
                    <strong>{{ tableFocusGroup?.tasks.length ?? 0 }}</strong>
                  </div>
                  <div class="metric-box">
                    <p>표시</p>
                    <strong>{{ tablePreviewTasks.length }}</strong>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('table') === '2x1'">
                <div class="dashboard-table-list dashboard-table-list--compact">
                  <button
                    v-for="task in tablePreviewTasks.slice(0, 5)"
                    :key="task.id"
                    class="dashboard-table-entry dashboard-table-entry--compact"
                    type="button"
                    @click.stop="openTask(task.id)"
                  >
                    <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                    <div class="dashboard-table-entry__primary">
                      <strong>{{ task.title }}</strong>
                      <small>{{ task.requirementId }}</small>
                    </div>
                    <span
                      class="dashboard-table-pill"
                      :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd', color: '#fff' }"
                    >
                      {{ store.priorityLabels[task.priority] ?? task.priority }}
                    </span>
                    <div class="dashboard-table-progress-shell">
                      <div class="dashboard-table-progress">
                        <span :style="{ width: `${task.progress}%` }" />
                      </div>
                      <strong>{{ task.progress }}%</strong>
                    </div>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('table') === '2x2'">
                <div class="dashboard-table-card">
                  <div class="dashboard-table-mini-head">
                    <span>Item</span>
                    <span>Owner</span>
                    <span>Status</span>
                    <span>Due</span>
                  </div>
                  <div class="dashboard-table-list">
                    <button
                      v-for="task in tablePreviewTasks.slice(0, 4)"
                      :key="`table-grid-${task.id}`"
                      class="dashboard-table-entry dashboard-table-entry--grid"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }}</small>
                        </div>
                      </div>
                      <div class="dashboard-table-owner">
                        <span
                          class="dashboard-table-avatar"
                          :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                        >
                          {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                        </span>
                        <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.statusLabels[task.status] ?? task.status }}
                      </span>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('table') === '__legacy_2x2'">
                <div class="table-preview table-preview--grid">
                  <div class="table-head">
                    <span>항목</span>
                    <span>담당자</span>
                    <span>진행률</span>
                  </div>
                  <button v-for="task in tablePreviewTasks.slice(0, 4)" :key="task.id" class="table-row table-row--grid" type="button" @click.stop="openTask(task.id)">
                    <div>
                      <strong>{{ task.title }}</strong>
                      <p>{{ task.requirementId }}</p>
                    </div>
                    <span>{{ memberName(task.assigneeId) }}</span>
                    <span>{{ task.progress }}%</span>
                  </button>
                </div>
              </template>

              <template v-else-if="tileBodyMode('table') === '3x1'">
                <div class="dashboard-table-card">
                  <div class="dashboard-table-mini-head dashboard-table-mini-head--wide">
                    <span>Item</span>
                    <span>Type</span>
                    <span>Progress</span>
                    <span>Due</span>
                  </div>
                  <div class="dashboard-table-list">
                    <button
                      v-for="task in tablePreviewTasks"
                      :key="`table-wide-${task.id}`"
                      class="dashboard-table-entry dashboard-table-entry--wide"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }} · {{ memberName(task.assigneeId) }}</small>
                        </div>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                      >
                        {{ task.contentType }}
                      </span>
                      <div class="dashboard-table-progress-shell">
                        <div class="dashboard-table-progress">
                          <span :style="{ width: `${task.progress}%` }" />
                        </div>
                        <strong>{{ task.progress }}%</strong>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('table') === '__legacy_3x1'">
                <div class="table-preview table-preview--stack">
                  <button v-for="task in tablePreviewTasks" :key="task.id" class="table-row" type="button" @click.stop="openTask(task.id)">
                    <div>
                      <strong>{{ task.title }}</strong>
                      <p>{{ task.requirementId }} · {{ memberName(task.assigneeId) }}</p>
                    </div>
                    <span>{{ task.progress }}%</span>
                  </button>
                </div>
              </template>

              <template v-else-if="['3x2', '3x3'].includes(tileBodyMode('table'))">
                <div class="dashboard-table-detail">
                  <div class="dashboard-table-detail__summary">
                    <div>
                      <p>{{ tableFocusGroup?.label ?? 'Current month' }}</p>
                      <strong>{{ tableFocusGroup?.tasks.length ?? 0 }} items</strong>
                    </div>
                    <span>{{ tableHiddenCount }} more</span>
                  </div>
                  <div class="dashboard-table-board dashboard-scrollbar-hidden">
                    <div class="dashboard-table-board__head">
                      <span>Task</span>
                      <span>Priority</span>
                      <span>Owner</span>
                      <span>Start</span>
                      <span>Type</span>
                      <span>Status</span>
                      <span>Progress</span>
                      <span>Due</span>
                    </div>
                    <button
                      v-for="task in tableFocusGroup?.tasks ?? []"
                      :key="`table-detail-${task.id}`"
                      class="dashboard-table-board__row"
                      type="button"
                      @click.stop="openTask(task.id)"
                    >
                      <div class="dashboard-table-entry__title">
                        <span class="dashboard-table-entry__indicator" :style="{ backgroundColor: task.palette.accent }" />
                        <div class="dashboard-table-entry__primary">
                          <strong>{{ task.title }}</strong>
                          <small>{{ task.requirementId }}</small>
                        </div>
                      </div>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tablePriorityTones[task.priority] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.priorityLabels[task.priority] ?? task.priority }}
                      </span>
                      <div class="dashboard-table-owner">
                        <span
                          class="dashboard-table-avatar"
                          :style="{ backgroundColor: memberFor(task.assigneeId)?.accent ?? '#94a3b8' }"
                        >
                          {{ memberFor(task.assigneeId)?.initials ?? 'NA' }}
                        </span>
                        <span>{{ memberFor(task.assigneeId)?.name ?? memberName(task.assigneeId) }}</span>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.startDate) }}</span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: task.palette.accent, color: '#fff' }"
                      >
                        {{ task.contentType }}
                      </span>
                      <span
                        class="dashboard-table-pill"
                        :style="{ backgroundColor: tableStatusTones[task.status] ?? '#9aa7bd', color: '#fff' }"
                      >
                        {{ store.statusLabels[task.status] ?? task.status }}
                      </span>
                      <div class="dashboard-table-progress-shell">
                        <div class="dashboard-table-progress">
                          <span :style="{ width: `${task.progress}%` }" />
                        </div>
                        <strong>{{ task.progress }}%</strong>
                      </div>
                      <span class="dashboard-table-date">{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('table') === '__legacy_detail'">
                <div class="table-detail">
                  <div class="table-detail__summary">
                    <div>
                      <p>{{ tableFocusGroup?.label ?? '현재 월' }}</p>
                      <strong>{{ tableFocusGroup?.tasks.length ?? 0 }}건</strong>
                    </div>
                    <span>{{ tableHiddenCount }}건 더 있음</span>
                  </div>
                  <div class="table-detail__rows dashboard-scrollbar-hidden">
                    <button v-for="task in tableFocusGroup?.tasks ?? []" :key="task.id" class="table-row table-row--detail" type="button" @click.stop="openTask(task.id)">
                      <div>
                        <strong>{{ task.title }}</strong>
                        <p>{{ task.requirementId }} · {{ memberName(task.assigneeId) }}</p>
                      </div>
                      <span>{{ formatShortDate(task.dueDate) }}</span>
                    </button>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="tileId === 'aiBrief'">
              <template v-if="tileBodyMode('aiBrief') === '1x1'">
                <div class="metric-triplet">
                  <div class="metric-box">
                    <p>우선 리스크</p>
                    <strong>{{ topRiskMember?.member.name ?? '없음' }}</strong>
                  </div>
                  <div class="metric-box">
                    <p>평균 진척</p>
                    <strong>{{ teamMetrics.averageProgress }}%</strong>
                  </div>
                  <div class="metric-box">
                    <p>7일 내</p>
                    <strong>{{ teamMetrics.dueSoon }}</strong>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('aiBrief') === '1x2'">
                <div class="dashboard-stack">
                  <div v-for="suggestion in aiSuggestions.slice(0, 3)" :key="suggestion" class="tool-list__item">
                    {{ suggestion }}
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('aiBrief') === '2x1'">
                <div class="dashboard-stack">
                  <div class="note-card">
                    <p>AI 브리핑</p>
                    <strong>{{ topRiskMember?.member.name ?? '대상 없음' }}</strong>
                    <span>우선 점검과 다음 액션을 정리합니다.</span>
                  </div>
                  <div class="note-card">
                    <p>추천 액션</p>
                    <strong>{{ aiSuggestions[0] }}</strong>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="dashboard-stack">
                  <div class="tool-panel tool-panel--pink">
                    <p class="section-eyebrow">AI 브리핑</p>
                    <h4>위험과 다음 액션을 함께 봅니다</h4>
                    <p>{{ topRiskMember?.member.name ?? '대상 없음' }} 중심으로 정리된 요약입니다.</p>
                  </div>
                  <div class="tool-list">
                    <div v-for="suggestion in aiSuggestions" :key="suggestion" class="tool-list__item">
                      {{ suggestion }}
                    </div>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="tileId === 'riskAlert'">
              <template v-if="tileBodyMode('riskAlert') === '1x1'">
                <div class="metric-triplet">
                  <div class="metric-box">
                    <p>주의</p>
                    <strong>{{ teamMetrics.atRisk }}</strong>
                  </div>
                  <div class="metric-box">
                    <p>마감 임박</p>
                    <strong>{{ teamMetrics.dueSoon }}</strong>
                  </div>
                  <div class="metric-box">
                    <p>완료율</p>
                    <strong>{{ teamMetrics.completionRate }}%</strong>
                  </div>
                </div>
              </template>

              <template v-else-if="tileBodyMode('riskAlert') === '1x2'">
                <div class="dashboard-stack">
                  <article v-for="row in performanceRows.slice(0, 3)" :key="row.member.id" class="meeting-card">
                    <p>{{ row.member.role }}</p>
                    <strong>{{ row.member.name }}</strong>
                    <span>리스크 {{ row.atRisk }} · 점수 {{ row.score }}</span>
                  </article>
                </div>
              </template>

              <template v-else-if="tileBodyMode('riskAlert') === '2x1'">
                <div class="dashboard-stack">
                  <div class="note-card note-card--wide">
                    <p>리스크 요약</p>
                    <strong>{{ teamMetrics.atRisk }}건</strong>
                    <span>마감 7일 이내 업무를 우선 점검합니다.</span>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="dashboard-stack">
                  <div class="tool-panel tool-panel--amber">
                    <p class="section-eyebrow">리스크 알림</p>
                    <h4>지연과 병목을 빠르게 확인합니다</h4>
                    <p>마감 임박, 진행 중, 위험 상태를 한 번에 모아 봅니다.</p>
                  </div>
                  <div class="tool-list">
                    <div v-for="row in performanceRows.slice(0, 4)" :key="row.member.id" class="tool-meeting">
                      <div class="tool-meeting__head">
                        <strong>{{ row.member.name }}</strong>
                        <span>리스크 {{ row.atRisk }}</span>
                      </div>
                      <p>{{ row.member.role }}</p>
                      <small>점수 {{ row.score }} · 완료율 {{ row.completionRate }}%</small>
                    </div>
                  </div>
                </div>
              </template>
            </template>

            <template v-else-if="tileId === 'meetings'">
              <template v-if="tileBodyMode('meetings') === '1x1'">
                <article class="meeting-card">
                  <p>{{ meetingItems[0].time }}</p>
                  <strong>{{ meetingItems[0].title }}</strong>
                  <span>{{ meetingItems[0].mode }}</span>
                </article>
              </template>

              <template v-else-if="tileBodyMode('meetings') === '1x2'">
                <div class="dashboard-stack">
                  <article v-for="meeting in meetingItems.slice(0, 2)" :key="meeting.title" class="meeting-card">
                    <p>{{ meeting.time }}</p>
                    <strong>{{ meeting.title }}</strong>
                    <span>{{ meeting.mode }}</span>
                  </article>
                </div>
              </template>

              <template v-else-if="tileBodyMode('meetings') === '2x1'">
                <div class="meeting-note">
                  <p>회의록</p>
                  <strong>{{ meetingItems.length }}개 회의가 예정되어 있습니다.</strong>
                  <span>시간, 링크, 메모를 한 번에 확인합니다.</span>
                </div>
              </template>

              <template v-else>
                <div class="dashboard-stack">
                  <article v-for="meeting in meetingItems" :key="meeting.title" class="meeting-card meeting-card--wide">
                    <div class="meeting-card__head">
                      <p>{{ meeting.time }}</p>
                      <span>{{ meeting.mode }}</span>
                    </div>
                    <strong>{{ meeting.title }}</strong>
                    <span>{{ meeting.detail }}</span>
                  </article>
                </div>
              </template>
            </template>
          </div>
        </article>
      </div>
    </section>

    <button
      v-for="tab in dockTabs"
      :key="tab.key"
      class="dashboard-dock"
      :class="[
        `dashboard-dock--${tab.tone}`,
        { 'dashboard-dock--active': activeRail === tab.key && toolModalOpen },
      ]"
      type="button"
      @click="openRailModal(tab.key)"
    >
      <span v-html="svgIcons[tab.iconType]" />
      <small>{{ tab.label }}</small>
    </button>

    <Teleport to="body">
      <div v-if="toolModalOpen" class="tool-modal">
        <div class="tool-modal__backdrop" @click="closeRailModal" />
        <section class="tool-modal__dialog surface-card">
          <header class="tool-modal__head">
            <div>
              <p class="section-eyebrow">Quick Modal</p>
              <h3>{{ activeRail === 'ai' ? 'AI 버튼' : activeRail === 'tasknote' ? 'TaskNote' : '회의 페이지' }}</h3>
            </div>
            <button class="tool-modal__close" type="button" aria-label="닫기" @click="closeRailModal">
              <span v-html="svgIcons.close" />
            </button>
          </header>

          <template v-if="activeRail === 'ai'">
            <div class="tool-panel tool-panel--pink">
              <p class="section-eyebrow">AI 브리핑</p>
              <h4>우선 리스크와 다음 액션을 정리합니다</h4>
              <p>{{ topRiskMember?.member.name ?? '선택한 담당자' }}의 우선 점검이 필요합니다.</p>
            </div>
            <div class="tool-list">
              <div v-for="suggestion in aiSuggestions" :key="suggestion" class="tool-list__item">
                {{ suggestion }}
              </div>
            </div>
          </template>

          <template v-else-if="activeRail === 'tasknote'">
            <div class="tool-panel tool-panel--amber">
              <p class="section-eyebrow">TaskNote</p>
              <h4>메모를 바로 적고 저장합니다</h4>
              <p>회의, 일정, 후속 액션을 짧게 남기면 다음에 바로 확인할 수 있습니다.</p>
            </div>
            <textarea
              v-model="taskNoteDraft"
              rows="8"
              class="tool-textarea dashboard-scrollbar-hidden"
              placeholder="오늘의 회의 메모, 우선 점검, 다음 액션을 적어주세요."
            />
          </template>

          <template v-else>
            <div class="tool-panel tool-panel--sky">
              <p class="section-eyebrow">회의 페이지</p>
              <h4>회의 일정과 메모를 연결합니다</h4>
              <p>회의 일정, 참석 링크, 메모를 한 번에 확인할 수 있습니다.</p>
            </div>
            <div class="tool-list">
              <article v-for="meeting in meetingItems" :key="meeting.title" class="tool-meeting">
                <div class="tool-meeting__head">
                  <strong>{{ meeting.title }}</strong>
                  <span>{{ meeting.time }}</span>
                </div>
                <p>{{ meeting.detail }}</p>
                <small>{{ meeting.mode }}</small>
              </article>
            </div>
          </template>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="sizeModalState.open" class="size-modal">
        <div class="size-modal__backdrop" @click="closeSizeModal" />
        <section class="size-modal__dialog surface-card">
          <header class="size-modal__head">
            <div>
              <p class="section-eyebrow">크기 변경</p>
              <h3>{{ tileMeta[sizeModalState.tileId]?.title ?? '요약 기능' }}</h3>
              <p class="size-modal__desc">크기를 선택하면 바로 해당 칸이 바뀝니다.</p>
            </div>
            <button class="size-modal__close" type="button" aria-label="닫기" @click="closeSizeModal">
              <span v-html="svgIcons.close" />
            </button>
          </header>

          <div class="size-modal__grid">
            <button
              v-for="option in sizeOptionsForTile(sizeModalState.tileId)"
              :key="option.token"
              type="button"
              class="size-modal__chip"
              :class="{ 'size-modal__chip--active': option.active, 'size-modal__chip--disabled': !option.available }"
              :disabled="!option.available"
              @click="applyTileSize(sizeModalState.tileId, option.token)"
            >
              <strong>{{ option.token }}</strong>
              <small>{{ tileModeLabel(sizeModalState.tileId, option.token) }}</small>
            </button>
          </div>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="boardManagerOpen" class="manager-modal">
        <div class="manager-modal__backdrop" @click="closeBoardManager" />
        <section class="manager-modal__dialog surface-card">
          <header class="manager-modal__head">
            <div>
              <p class="section-eyebrow">보드 관리</p>
              <h3>숨김과 표시를 조절합니다</h3>
              <p class="manager-modal__desc">보드에서 사라진 기능은 다시 켤 수 있고, 현재 요약 기능만 이곳에서 조절할 수 있습니다.</p>
            </div>
            <button class="manager-modal__close" type="button" aria-label="닫기" @click="closeBoardManager">
              <span v-html="svgIcons.close" />
            </button>
          </header>

          <div class="manager-modal__section">
            <p class="manager-modal__section-title">현재 요약 기능</p>
            <div class="manager-modal__grid">
              <button
                v-for="tileId in currentManagedTileIds"
                :key="tileId"
                type="button"
                class="manager-toggle"
                :class="{ 'manager-toggle--off': isTileHidden(tileId) }"
                @click="toggleTileHidden(tileId, !isTileHidden(tileId))"
              >
                <strong>{{ tileMeta[tileId].title }}</strong>
                <small>{{ isTileHidden(tileId) ? '숨김' : '표시 중' }}</small>
              </button>
            </div>
          </div>

        </section>
      </div>
    </Teleport>
  </section>
</template>

<style>
.dashboard-page {
  display: grid;
  gap: 1rem;
  padding-bottom: 5rem;
}

.dashboard-hero {
  display: none;
}

.dashboard-board-toolbar {
  padding: 0.05rem 0.15rem 0.45rem;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
}

.dashboard-board-toolbar__date {
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
}

.dashboard-board-toolbar__menu {
  width: 3.35rem;
  height: 3.35rem;
  border: 1px solid var(--border-color);
  border-radius: 1rem;
  background: var(--panel-muted);
  color: var(--accent-strong);
  font-size: 1.45rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  display: grid;
  place-items: center;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  cursor: pointer;
}

.dashboard-hero__copy h2 {
  margin-top: 0.4rem;
  font-size: clamp(1.7rem, 2.3vw, 2.45rem);
  line-height: 1.08;
  letter-spacing: -0.05em;
}

.dashboard-hero__desc {
  margin-top: 0.55rem;
  max-width: 58rem;
  color: var(--text-secondary);
}

.dashboard-hero__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 0.45rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 600;
}

.dashboard-hero__meta-menu {
  width: 3.35rem;
  height: 3.35rem;
  border: 1px solid var(--border-color);
  border-radius: 1rem;
  background: var(--panel-muted);
  color: var(--accent-strong);
  font-size: 1.45rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  display: grid;
  place-items: center;
  line-height: 1;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  cursor: pointer;
}

.dashboard-board-shell {
  padding: 0.85rem;
  overflow: visible;
}

.dashboard-board {
  position: relative;
  border-radius: 28px;
  border: 1px solid var(--border-color);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--panel-color) 98%, var(--panel-muted)), var(--panel-color)),
    repeating-linear-gradient(
      to right,
      transparent 0,
      transparent calc(var(--board-pitch-x) - 1px),
      rgba(148, 163, 184, 0.07) calc(var(--board-pitch-x) - 1px),
      rgba(148, 163, 184, 0.07) var(--board-pitch-x)
    ),
    repeating-linear-gradient(
      to bottom,
      transparent 0,
      transparent calc(var(--board-pitch-y) - 1px),
      rgba(148, 163, 184, 0.07) calc(var(--board-pitch-y) - 1px),
      rgba(148, 163, 184, 0.07) var(--board-pitch-y)
    );
  overflow: visible;
}

.dashboard-board__paths {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.dashboard-board__path {
  fill: none;
  stroke: color-mix(in srgb, var(--accent-color) 60%, white);
  stroke-width: 2;
  stroke-dasharray: 7 8;
  opacity: 0.8;
}

.dashboard-tile {
  position: absolute;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  min-width: 0;
  min-height: 0;
  left: 0;
  top: 0;
  width: var(--tile-width);
  height: var(--tile-height);
  overflow: visible;
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--panel-color) 98%, var(--panel-muted)), var(--panel-color)),
    var(--panel-color);
  box-shadow: var(--shadow-soft);
  padding: 0.95rem;
  transform: translate(var(--tile-x), var(--tile-y)) scale(var(--tile-scale, 1));
  transform-origin: center;
  will-change: transform, width, height, opacity, box-shadow;
  transition:
    transform 260ms cubic-bezier(0.2, 0.8, 0.2, 1),
    width 260ms cubic-bezier(0.2, 0.8, 0.2, 1),
    height 260ms cubic-bezier(0.2, 0.8, 0.2, 1),
    box-shadow 220ms ease,
    opacity 220ms ease;
}

.dashboard-tile:hover {
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.08);
}

.dashboard-tile--dragging {
  box-shadow: 0 26px 55px rgba(15, 23, 42, 0.18);
}

.dashboard-tile--ghost {
  pointer-events: none;
  opacity: 0.22;
  border-style: dashed;
  box-shadow: none;
  background: color-mix(in srgb, var(--panel-color) 90%, var(--accent-color));
  z-index: 2;
}

.dashboard-tile--drop {
  pointer-events: none;
  z-index: 3;
  display: grid;
  place-items: center;
  border: 2px dashed color-mix(in srgb, var(--accent-color) 80%, white);
  background: color-mix(in srgb, var(--accent-color) 7%, transparent);
  color: var(--accent-color);
  font-weight: 800;
  letter-spacing: 0.02em;
}

.dashboard-tile__head {
  position: relative;
  display: grid;
  gap: 0.65rem;
}

.dashboard-tile__head-copy {
  min-width: 0;
}

.dashboard-tile__head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.55rem;
}

.dashboard-tile__eyebrow {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.dashboard-tile__head h3 {
  margin-top: 0.25rem;
  font-size: 1.02rem;
  line-height: 1.1;
  letter-spacing: -0.03em;
  font-weight: 800;
}

.dashboard-tile__desc {
  margin-top: 0.35rem;
  color: var(--muted-text);
  font-size: 0.83rem;
  line-height: 1.4;
}

.dashboard-tile__badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  height: 1.8rem;
  padding: 0 0.72rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  color: var(--text-primary);
  font-size: 0.75rem;
  font-weight: 800;
}

.dashboard-tile__menu {
  width: 2.1rem;
  height: 2.1rem;
  border: 1px solid var(--border-color);
  border-radius: 999px;
  background: var(--panel-muted);
  color: var(--muted-text);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 900;
  letter-spacing: 0.1em;
  display: grid;
  place-items: center;
}

.dashboard-tile__menu-panel {
  position: absolute;
  top: 2.9rem;
  right: 0.75rem;
  min-width: 8.5rem;
  padding: 0.4rem;
  display: grid;
  gap: 0.35rem;
  z-index: 15;
}

.dashboard-tile__menu-item {
  border: 0;
  border-radius: 14px;
  background: var(--panel-muted);
  padding: 0.72rem 0.8rem;
  text-align: left;
  font-weight: 700;
  cursor: pointer;
}

.dashboard-tile__body {
  min-height: 0;
  flex: 1;
  overflow: auto;
}

.dashboard-scrollbar-hidden {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.dashboard-scrollbar-hidden::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.dashboard-stack {
  display: grid;
  gap: 0.65rem;
}

.summary-line,
.calendar-row,
.person-row,
.performance-card,
.scoreboard-chip,
.table-row,
.meeting-card {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  color: var(--text-primary);
  text-align: left;
}

.summary-line,
.calendar-row,
.person-row,
.performance-card,
.table-row,
.scoreboard-chip {
  display: grid;
}

.summary-line {
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.7rem;
  padding: 0.75rem 0.85rem;
}

.summary-line__dot {
  width: 0.7rem;
  height: 0.7rem;
  border-radius: 999px;
}

.summary-line__copy {
  min-width: 0;
  display: grid;
  gap: 0.12rem;
}

.summary-line__copy strong {
  font-size: 0.92rem;
}

.summary-line__copy span,
.summary-line__meta {
  font-size: 0.78rem;
  color: var(--muted-text);
}

.summary-line__meta {
  font-weight: 800;
}

.calendar-row {
  grid-template-columns: minmax(4rem, auto) minmax(0, 1fr);
  align-items: center;
  gap: 0.8rem;
  padding: 0.82rem 0.88rem;
}

.calendar-row--active {
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  border-color: color-mix(in srgb, var(--accent-color) 28%, var(--border-color));
}

.calendar-row__date {
  display: grid;
  gap: 0.05rem;
}

.calendar-row__date strong {
  font-size: 1.2rem;
  line-height: 1;
}

.calendar-row__date span,
.calendar-row__meta span {
  font-size: 0.7rem;
  color: var(--muted-text);
  font-weight: 700;
}

.calendar-row__meta {
  display: grid;
  gap: 0.2rem;
  justify-items: end;
}

.calendar-row__meta strong {
  font-size: 0.82rem;
  font-weight: 800;
}

.mini-month {
  display: grid;
  gap: 0.45rem;
}

.mini-month__weekdays,
.mini-month__week {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.35rem;
}

.mini-month__weekdays span {
  font-size: 0.65rem;
  font-weight: 800;
  color: var(--muted-text);
  text-align: center;
}

.mini-month__day {
  min-height: 2.7rem;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: var(--panel-muted);
  display: grid;
  gap: 0.22rem;
  justify-items: center;
  align-content: center;
}

.mini-month__day strong {
  font-size: 0.82rem;
  line-height: 1;
}

.mini-month__day span {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 999px;
}

.mini-month__day--active {
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  border-color: color-mix(in srgb, var(--accent-color) 26%, var(--border-color));
}

.mini-month__day--muted {
  opacity: 0.45;
}

.calendar-panel {
  display: grid;
  gap: 0.7rem;
}

.week-lane {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.45rem;
}

.week-lane__cell {
  min-height: 100%;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.8rem 0.55rem;
  display: grid;
  gap: 0.3rem;
  justify-items: center;
  align-content: center;
  color: var(--text-primary);
  cursor: pointer;
}

.week-lane__cell p {
  font-size: 0.66rem;
  color: var(--muted-text);
  font-weight: 800;
}

.week-lane__cell strong {
  font-size: 1.1rem;
  line-height: 1;
}

.week-lane__cell span {
  font-size: 0.7rem;
  color: var(--muted-text);
}

.week-lane__cell--active {
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  border-color: color-mix(in srgb, var(--accent-color) 26%, var(--border-color));
}

.calendar-agenda {
  display: grid;
  gap: 0.55rem;
  padding: 0.85rem;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--panel-muted) 94%, white), var(--panel-muted));
}

.calendar-agenda--compact {
  padding: 0.75rem;
}

.calendar-agenda--detail {
  min-height: 0;
}

.calendar-agenda__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.calendar-agenda__head p {
  font-size: 0.7rem;
  color: var(--muted-text);
  font-weight: 800;
}

.calendar-agenda__head strong {
  display: block;
  margin-top: 0.12rem;
  font-size: 0.98rem;
  line-height: 1.15;
}

.calendar-agenda__head span {
  flex-shrink: 0;
  font-size: 0.72rem;
  color: var(--muted-text);
  font-weight: 700;
}

.calendar-agenda__list {
  display: grid;
  gap: 0.45rem;
}

.calendar-agenda__list--detail {
  max-height: 16rem;
  overflow: auto;
  padding-right: 0.15rem;
}

.calendar-agenda__item {
  width: 100%;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.7rem;
  padding: 0.78rem 0.82rem;
  border: 1px solid color-mix(in srgb, var(--border-color) 88%, white);
  border-radius: 16px;
  background: color-mix(in srgb, var(--panel-color) 92%, white);
  color: var(--text-primary);
  text-align: left;
}

.calendar-agenda__item--compact {
  padding: 0.7rem 0.75rem;
}

.calendar-agenda__item--detail {
  grid-template-columns: minmax(0, 1.5fr) auto auto;
  align-items: start;
}

.calendar-agenda__item-dot {
  width: 0.72rem;
  height: 0.72rem;
  border-radius: 999px;
}

.calendar-agenda__item-main,
.calendar-agenda__item-copy {
  min-width: 0;
  display: grid;
  gap: 0.16rem;
}

.calendar-agenda__item-main {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: start;
  gap: 0.7rem;
}

.calendar-agenda__item-copy strong {
  font-size: 0.9rem;
  line-height: 1.25;
}

.calendar-agenda__item-copy span,
.calendar-agenda__item-side small,
.calendar-agenda__item-meta span {
  font-size: 0.76rem;
  color: var(--muted-text);
  line-height: 1.35;
}

.calendar-agenda__item-progress,
.calendar-agenda__item-side strong,
.calendar-agenda__item-meta strong {
  font-size: 0.8rem;
  font-weight: 800;
}

.calendar-agenda__item-side {
  display: grid;
  gap: 0.18rem;
  justify-items: end;
  text-align: right;
}

.calendar-agenda__item-pills {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.38rem;
}

.calendar-agenda__item-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 1.75rem;
  padding: 0 0.72rem;
  border-radius: 999px;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 800;
  white-space: nowrap;
}

.calendar-agenda__item-meta {
  display: grid;
  justify-items: end;
  gap: 0.2rem;
  text-align: right;
}

.calendar-agenda__empty {
  border: 1px dashed color-mix(in srgb, var(--border-color) 88%, white);
  border-radius: 16px;
  padding: 0.9rem 0.95rem;
  background: color-mix(in srgb, var(--panel-color) 92%, white);
  color: var(--muted-text);
  font-size: 0.8rem;
  line-height: 1.45;
}

.month-view {
  display: grid;
  gap: 0.8rem;
}

.month-view__summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.65rem;
}

.month-view__summary > div {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.75rem 0.85rem;
}

.month-view__summary p {
  font-size: 0.7rem;
  color: var(--muted-text);
  font-weight: 800;
}

.month-view__summary strong {
  display: block;
  margin-top: 0.15rem;
  font-size: 1rem;
}

.month-view__calendar {
  max-height: 18rem;
  overflow: auto;
}

.metric-triplet {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.55rem;
}

.metric-triplet--wide {
  margin-bottom: 0.75rem;
}

.metric-box {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.7rem 0.8rem;
}

.metric-box--small {
  min-height: 5rem;
}

.metric-box p {
  font-size: 0.68rem;
  color: var(--muted-text);
  font-weight: 800;
}

.metric-box strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.65rem;
  line-height: 1;
}

.performance-list,
.performance-grid,
.performance-detail {
  display: grid;
  gap: 0.65rem;
}

.performance-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.performance-card,
.person-row,
.table-row,
.scoreboard-chip {
  padding: 0.72rem 0.8rem;
  gap: 0.55rem;
}

.person-row,
.performance-card {
  align-items: center;
}

.person-row__head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.7rem;
  align-items: center;
}

.person-row__avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 800;
  display: grid;
  place-items: center;
}

.person-row__head strong {
  display: block;
  font-size: 0.92rem;
}

.person-row__head p {
  margin-top: 0.1rem;
  font-size: 0.72rem;
  color: var(--muted-text);
}

.person-row__head > span {
  font-size: 0.86rem;
  font-weight: 800;
}

.person-row__bar {
  height: 0.4rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.person-row__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.performance-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.performance-card__tags span {
  display: inline-flex;
  align-items: center;
  min-height: 1.55rem;
  padding: 0 0.5rem;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--border-color);
  font-size: 0.68rem;
  font-weight: 800;
  color: var(--text-secondary);
}

.scoreboard-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.scoreboard-chip {
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  width: auto;
  min-width: 9.5rem;
}

.scoreboard-chip span {
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 999px;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 800;
  display: grid;
  place-items: center;
}

.scoreboard-chip strong {
  font-size: 0.78rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scoreboard-chip em {
  font-style: normal;
  font-size: 0.82rem;
  font-weight: 800;
  color: var(--text-primary);
}

.note-compact,
.notes-board {
  display: grid;
  gap: 0.65rem;
}

.note-card {
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: var(--panel-muted);
  padding: 0.8rem 0.85rem;
  display: grid;
  gap: 0.24rem;
}

.note-card p,
.meeting-card p,
.meeting-note p {
  font-size: 0.7rem;
  color: var(--muted-text);
  font-weight: 800;
}

.note-card strong {
  font-size: 1.05rem;
}

.note-card span,
.meeting-card span,
.meeting-note span {
  color: var(--muted-text);
  font-size: 0.8rem;
  line-height: 1.4;
}

.note-card--wide {
  min-height: 6rem;
}

.note-list,
.table-preview {
  display: grid;
  gap: 0.5rem;
}

.table-preview--grid {
  gap: 0.45rem;
}

.table-head,
.table-row--grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(0, 1fr) auto;
  gap: 0.5rem;
  align-items: center;
}

.table-head {
  padding: 0 0.2rem;
}

.table-head span {
  font-size: 0.68rem;
  color: var(--muted-text);
  font-weight: 800;
}

.table-row {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.6rem;
}

.table-row strong {
  display: block;
  font-size: 0.88rem;
}

.table-row p {
  margin-top: 0.1rem;
  font-size: 0.72rem;
  color: var(--muted-text);
}

.table-row span {
  font-size: 0.82rem;
  font-weight: 800;
}

.table-row--detail {
  margin-top: 0.45rem;
}

.table-preview--stack .table-row {
  min-height: 3.2rem;
}

.table-detail {
  display: grid;
  gap: 0.7rem;
}

.table-detail__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.table-detail__summary p {
  font-size: 0.72rem;
  color: var(--muted-text);
  font-weight: 800;
}

.table-detail__summary strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1.05rem;
}

.table-detail__summary span {
  font-size: 0.78rem;
  color: var(--muted-text);
  font-weight: 700;
}

.table-detail__rows {
  max-height: 100%;
  overflow: auto;
}

.dashboard-table-card,
.dashboard-table-detail {
  display: grid;
  gap: 0.72rem;
  min-height: 0;
}

.dashboard-table-list {
  display: grid;
  gap: 0.55rem;
  min-height: 0;
}

.dashboard-table-entry,
.dashboard-table-board__row {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.78rem 0.88rem;
  text-align: left;
  color: var(--text-primary);
}

.dashboard-table-entry {
  display: grid;
  align-items: center;
  gap: 0.55rem;
}

.dashboard-table-entry--compact {
  grid-template-columns: 4px minmax(0, 1.45fr) minmax(64px, auto) minmax(108px, auto);
}

.dashboard-table-entry--grid {
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr) minmax(68px, auto) minmax(46px, auto);
}

.dashboard-table-entry--wide {
  grid-template-columns: minmax(0, 1.7fr) minmax(68px, auto) minmax(108px, auto) minmax(46px, auto);
}

.dashboard-table-mini-head,
.dashboard-table-board__head {
  display: grid;
  align-items: center;
  gap: 0.55rem;
  padding: 0 0.2rem;
  color: var(--muted-text);
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.dashboard-table-mini-head {
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr) minmax(68px, auto) minmax(46px, auto);
}

.dashboard-table-mini-head--wide {
  grid-template-columns: minmax(0, 1.7fr) minmax(68px, auto) minmax(108px, auto) minmax(46px, auto);
}

.dashboard-table-entry__title,
.dashboard-table-owner {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}

.dashboard-table-entry__indicator {
  width: 4px;
  min-height: 40px;
  border-radius: 999px;
  flex-shrink: 0;
}

.dashboard-table-entry__primary {
  min-width: 0;
  display: grid;
  gap: 0.12rem;
}

.dashboard-table-entry__primary strong {
  display: block;
  font-size: 0.9rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dashboard-table-entry__primary small {
  color: var(--muted-text);
  font-size: 0.72rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dashboard-table-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: start;
  min-width: 64px;
  min-height: 1.9rem;
  padding: 0 0.55rem;
  border-radius: 9px;
  font-size: 0.72rem;
  font-weight: 800;
  white-space: nowrap;
}

.dashboard-table-avatar {
  width: 1.7rem;
  height: 1.7rem;
  border-radius: 999px;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.64rem;
  font-weight: 800;
  flex-shrink: 0;
}

.dashboard-table-owner span:last-child,
.dashboard-table-date {
  font-size: 0.76rem;
  font-weight: 700;
  white-space: nowrap;
}

.dashboard-table-owner {
  justify-self: start;
}

.dashboard-table-owner span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dashboard-table-progress-shell {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.32rem;
  min-width: 0;
}

.dashboard-table-progress {
  width: 70px;
  height: 1.2rem;
  border: 1px solid color-mix(in srgb, var(--success-color) 40%, var(--border-color));
  border-radius: 4px;
  background: color-mix(in srgb, var(--success-color) 7%, white);
  overflow: hidden;
  flex-shrink: 0;
}

.dashboard-table-progress span {
  display: block;
  height: 100%;
  background: #478f55;
}

.dashboard-table-progress-shell strong {
  font-size: 0.76rem;
  font-weight: 800;
}

.dashboard-table-detail__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.dashboard-table-detail__summary p {
  font-size: 0.72rem;
  color: var(--muted-text);
  font-weight: 800;
}

.dashboard-table-detail__summary strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 1.05rem;
}

.dashboard-table-detail__summary span {
  font-size: 0.78rem;
  color: var(--muted-text);
  font-weight: 700;
}

.dashboard-table-board {
  display: grid;
  gap: 0.55rem;
  min-height: 0;
  max-height: 100%;
  overflow: auto;
}

.dashboard-table-board__head,
.dashboard-table-board__row {
  grid-template-columns:
    minmax(0, 2.02fr)
    minmax(0, 0.8fr)
    minmax(0, 1.12fr)
    minmax(0, 0.52fr)
    minmax(0, 0.82fr)
    minmax(0, 0.82fr)
    minmax(0, 1.18fr)
    minmax(0, 0.56fr);
}

.dashboard-table-board__head {
  min-width: 0;
}

.dashboard-table-board__row {
  min-width: 0;
  display: grid;
  align-items: center;
  gap: 0.45rem;
  overflow: hidden;
}

.dashboard-table-board__head > *,
.dashboard-table-board__row > * {
  min-width: 0;
}

.dashboard-table-board__head > span {
  overflow: hidden;
  text-overflow: ellipsis;
}

.dashboard-table-entry:hover,
.dashboard-table-board__row:hover {
  background: color-mix(in srgb, var(--panel-muted) 62%, var(--panel-color));
}

.meeting-card,
.meeting-note {
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: var(--panel-muted);
  padding: 0.82rem 0.88rem;
  display: grid;
  gap: 0.24rem;
}

.meeting-card strong {
  font-size: 0.98rem;
}

.meeting-card__head {
  display: flex;
  justify-content: space-between;
  gap: 0.7rem;
}

.meeting-card__head span,
.meeting-card span {
  font-size: 0.76rem;
  color: var(--muted-text);
}

.dashboard-dock {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  width: 3.1rem;
  height: 3.1rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  box-shadow: var(--shadow-soft);
  display: grid;
  place-items: center;
  gap: 0.15rem;
  z-index: 70;
  transition:
    transform 180ms ease,
    box-shadow 180ms ease,
    background-color 180ms ease;
}

.dashboard-dock:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 35px rgba(15, 23, 42, 0.16);
}

.dashboard-dock span {
  color: currentColor;
}

.dashboard-dock small {
  font-size: 0.55rem;
  font-weight: 800;
  line-height: 1;
}

.dashboard-dock--pink {
  color: #ec4899;
}

.dashboard-dock--amber {
  color: #d97706;
}

.dashboard-dock--sky {
  color: #0284c7;
}

.dashboard-dock--active {
  box-shadow: 0 18px 35px rgba(15, 23, 42, 0.24);
}

.dashboard-dock:nth-of-type(2) {
  right: 5.1rem;
}

.dashboard-dock:nth-of-type(3) {
  right: 9.2rem;
}

.tool-modal {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.tool-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(8px);
}

.tool-modal__dialog {
  position: relative;
  z-index: 1;
  width: min(34rem, calc(100vw - 2rem));
  max-height: min(80vh, 44rem);
  overflow: auto;
  padding: 1rem;
}

.tool-modal__head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.75rem;
}

.tool-modal__head h3 {
  margin-top: 0.15rem;
  font-size: 1.35rem;
  line-height: 1.1;
}

.tool-modal__close {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  display: grid;
  place-items: center;
}

.size-modal {
  position: fixed;
  inset: 0;
  z-index: 95;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.size-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(8px);
}

.size-modal__dialog {
  position: relative;
  z-index: 1;
  width: min(28rem, calc(100vw - 2rem));
  max-height: min(78vh, 36rem);
  overflow: auto;
  padding: 1rem;
}

.size-modal__head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.75rem;
}

.size-modal__head h3 {
  margin-top: 0.15rem;
  font-size: 1.3rem;
  line-height: 1.1;
}

.size-modal__desc {
  margin-top: 0.35rem;
  color: var(--muted-text);
  font-size: 0.85rem;
}

.size-modal__close {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  display: grid;
  place-items: center;
}

.size-modal__grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(86px, 1fr));
  gap: 0.65rem;
}

.size-modal__chip {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.85rem 0.75rem;
  text-align: left;
  display: grid;
  gap: 0.2rem;
  cursor: pointer;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease,
    border-color 160ms ease,
    background-color 160ms ease;
}

.size-modal__chip:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.08);
}

.size-modal__chip strong {
  font-size: 0.96rem;
}

.size-modal__chip small {
  color: var(--muted-text);
  font-size: 0.74rem;
}

.size-modal__chip--active {
  border-color: color-mix(in srgb, var(--accent-color) 48%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent-color) 22%, transparent);
}

.size-modal__chip--disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.manager-modal {
  position: fixed;
  inset: 0;
  z-index: 96;
  display: grid;
  place-items: center;
  padding: 1rem;
}

.manager-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(8px);
}

.manager-modal__dialog {
  position: relative;
  z-index: 1;
  width: min(42rem, calc(100vw - 2rem));
  max-height: min(84vh, 48rem);
  overflow: auto;
  padding: 1rem;
}

.manager-modal__head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.75rem;
}

.manager-modal__head h3 {
  margin-top: 0.15rem;
  font-size: 1.35rem;
  line-height: 1.1;
}

.manager-modal__desc {
  margin-top: 0.35rem;
  color: var(--muted-text);
  font-size: 0.85rem;
}

.manager-modal__close {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  display: grid;
  place-items: center;
}

.manager-modal__section {
  margin-top: 1rem;
}

.manager-modal__section-title {
  margin-bottom: 0.55rem;
  color: var(--muted-text);
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.manager-modal__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.65rem;
}

.manager-toggle {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.85rem 0.9rem;
  text-align: left;
  display: grid;
  gap: 0.2rem;
  cursor: pointer;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease,
    border-color 160ms ease,
    background-color 160ms ease;
}

.manager-toggle:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.08);
}

.manager-toggle strong {
  font-size: 0.96rem;
}

.manager-toggle small {
  color: var(--muted-text);
  font-size: 0.74rem;
}

.manager-toggle--off {
  border-color: color-mix(in srgb, var(--border-color) 70%, var(--accent-color));
  background: color-mix(in srgb, var(--accent-color) 10%, var(--panel-muted));
}

.tool-panel {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 22px;
}

.tool-panel--pink {
  background: #fff1f7;
}

.tool-panel--amber {
  background: #fff7ed;
}

.tool-panel--sky {
  background: #eff6ff;
}

.tool-panel h4 {
  margin-top: 0.35rem;
  font-size: 1.1rem;
  line-height: 1.25;
}

.tool-panel p {
  margin-top: 0.35rem;
  color: var(--text-secondary);
}

.tool-list {
  margin-top: 0.85rem;
  display: grid;
  gap: 0.55rem;
}

.tool-list__item,
.tool-meeting {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-muted);
  padding: 0.8rem 0.9rem;
}

.tool-meeting {
  display: grid;
  gap: 0.3rem;
}

.tool-meeting__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.tool-meeting__head strong {
  font-size: 0.92rem;
}

.tool-meeting__head span,
.tool-meeting small {
  color: var(--muted-text);
  font-size: 0.76rem;
}

.tool-textarea {
  margin-top: 0.85rem;
  width: 100%;
  min-height: 15rem;
  border: 1px solid var(--border-color);
  border-radius: 22px;
  background: var(--panel-muted);
  padding: 0.95rem;
  resize: vertical;
  outline: none;
}

@media (max-width: 1200px) {
  .dashboard-board {
    min-width: 1040px;
  }

  .dashboard-board-shell {
    overflow-x: auto;
  }
}

@media (max-width: 760px) {
  .dashboard-hero,
  .dashboard-board-shell {
    padding: 0.8rem;
  }

  .dashboard-board-toolbar {
    padding-bottom: 0.35rem;
  }

  .dashboard-dock {
    right: 0.75rem;
    bottom: 0.75rem;
  }

  .dashboard-dock:nth-of-type(2) {
    right: 4.55rem;
  }

  .dashboard-dock:nth-of-type(3) {
    right: 8.35rem;
  }
}
</style>
