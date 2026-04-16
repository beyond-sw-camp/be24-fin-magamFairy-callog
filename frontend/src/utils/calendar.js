const DAY_MS = 24 * 60 * 60 * 1000

export const WEEKDAY_LABELS = ['월', '화', '수', '목', '금', '토', '일']

function toDate(value) {
  if (value instanceof Date) {
    return new Date(value.getFullYear(), value.getMonth(), value.getDate(), 12)
  }

  const [year, month, day] = String(value).split('-').map(Number)
  return new Date(year, month - 1, day, 12)
}

export function todayKey() {
  return formatDateKey(new Date())
}

export function formatDateKey(value) {
  const date = toDate(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function addDays(value, amount) {
  const date = toDate(value)
  date.setDate(date.getDate() + amount)
  return formatDateKey(date)
}

export function addMonths(value, amount) {
  const date = toDate(value)
  date.setMonth(date.getMonth() + amount)
  return formatDateKey(date)
}

export function startOfMonth(value) {
  const date = toDate(value)
  date.setDate(1)
  return formatDateKey(date)
}

export function endOfMonth(value) {
  const date = toDate(startOfMonth(value))
  date.setMonth(date.getMonth() + 1)
  date.setDate(0)
  return formatDateKey(date)
}

export function startOfWeek(value) {
  const date = toDate(value)
  const weekday = (date.getDay() + 6) % 7
  date.setDate(date.getDate() - weekday)
  return formatDateKey(date)
}

export function endOfWeek(value) {
  return addDays(startOfWeek(value), 6)
}

export function differenceInDays(later, earlier) {
  return Math.round((toDate(later) - toDate(earlier)) / DAY_MS)
}

export function compareDateKeys(left, right) {
  if (left === right) {
    return 0
  }

  return left > right ? 1 : -1
}

export function isDateInRange(dateKey, startKey, endKey) {
  return compareDateKeys(dateKey, startKey) >= 0 && compareDateKeys(dateKey, endKey) <= 0
}

export function overlaps(startA, endA, startB, endB) {
  return compareDateKeys(startA, endB) <= 0 && compareDateKeys(endA, startB) >= 0
}

export function getWeekDays(value) {
  const start = startOfWeek(value)

  return Array.from({ length: 7 }, (_, index) => {
    const key = addDays(start, index)
    const date = toDate(key)

    return {
      key,
      dayNumber: date.getDate(),
      weekdayLabel: WEEKDAY_LABELS[index],
      isToday: key === todayKey(),
    }
  })
}

export function getMonthWeeks(value) {
  const monthStart = startOfMonth(value)
  const monthEnd = endOfMonth(value)
  const gridStart = startOfWeek(monthStart)
  const finalWeekStart = startOfWeek(monthEnd)
  const totalWeeks = differenceInDays(finalWeekStart, gridStart) / 7 + 1
  const monthIndex = toDate(monthStart).getMonth()
  const year = toDate(monthStart).getFullYear()

  return Array.from({ length: totalWeeks }, (_, weekIndex) => {
    const weekStart = addDays(gridStart, weekIndex * 7)

    return Array.from({ length: 7 }, (_, dayIndex) => {
      const key = addDays(weekStart, dayIndex)
      const date = toDate(key)

      return {
        key,
        dayNumber: date.getDate(),
        weekdayLabel: WEEKDAY_LABELS[dayIndex],
        isToday: key === todayKey(),
        isCurrentMonth: date.getMonth() === monthIndex && date.getFullYear() === year,
      }
    })
  })
}

export function formatMonthLabel(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
  }).format(toDate(value))
}

export function formatWeekLabel(value) {
  const start = toDate(startOfWeek(value))
  const end = toDate(endOfWeek(value))

  const startText = new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
  }).format(start)
  const endText = new Intl.DateTimeFormat('ko-KR', {
    month: start.getMonth() === end.getMonth() ? undefined : 'numeric',
    day: 'numeric',
  }).format(end)

  return `${startText} - ${endText}`
}

export function formatShortDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'numeric',
    day: 'numeric',
  }).format(toDate(value))
}

export function formatLongDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(toDate(value))
}

export function buildLaneSegments(tasks, rangeStart, rangeEnd) {
  const filteredTasks = tasks
    .filter((task) => overlaps(task.startDate, task.dueDate, rangeStart, rangeEnd))
    .sort((left, right) => {
      const startCompare = compareDateKeys(left.startDate, right.startDate)

      if (startCompare !== 0) {
        return startCompare
      }

      return compareDateKeys(left.dueDate, right.dueDate)
    })

  const laneEnds = []
  const segments = []

  filteredTasks.forEach((task) => {
    const segmentStart =
      compareDateKeys(task.startDate, rangeStart) < 0 ? rangeStart : task.startDate
    const segmentEnd = compareDateKeys(task.dueDate, rangeEnd) > 0 ? rangeEnd : task.dueDate
    let lane = 0

    while (laneEnds[lane] && compareDateKeys(segmentStart, laneEnds[lane]) <= 0) {
      lane += 1
    }

    laneEnds[lane] = segmentEnd

    segments.push({
      task,
      lane,
      startOffset: differenceInDays(segmentStart, rangeStart),
      span: differenceInDays(segmentEnd, segmentStart) + 1,
      segmentStart,
      segmentEnd,
    })
  })

  return {
    laneCount: laneEnds.length || 1,
    segments,
  }
}

export function getRangeDays(currentDate, viewMode) {
  if (viewMode === 'week') {
    return getWeekDays(currentDate)
  }

  return getMonthWeeks(currentDate).flat()
}

export function getDuration(task) {
  return differenceInDays(task.dueDate, task.startDate) + 1
}
