import { ref, watch } from 'vue'

/**
 * Zone별 기본 페이지 개인설정 (localStorage 영속, 서버 불필요).
 * key: dashboardZonePrefs = { zone1, zone2, zone3, zone4 }
 *   각 값 = 해당 Zone 이 처음 열릴 때의 페이지 index.
 * + Zone1 P2 토글(오늘의 업무/마감 임박) 기본값도 같은 체계에 포함.
 *
 * 설계서 §대시보드 개인설정 참고. 모듈 단일 인스턴스로 화면 어디서든 동일 상태 공유.
 */
const STORAGE_KEY = 'dashboardZonePrefs'

const DEFAULTS = {
  zone1: 0,
  zone2: 0,
  zone3: 0,
  zone4: 0,
  // Zone1 P2 토글 기본: 'today' | 'deadline'
  zone1P2Mode: 'today',
}

function readPrefs() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...DEFAULTS }
    const obj = JSON.parse(raw)
    return {
      zone1: Number.isInteger(obj?.zone1) ? obj.zone1 : DEFAULTS.zone1,
      zone2: Number.isInteger(obj?.zone2) ? obj.zone2 : DEFAULTS.zone2,
      zone3: Number.isInteger(obj?.zone3) ? obj.zone3 : DEFAULTS.zone3,
      zone4: Number.isInteger(obj?.zone4) ? obj.zone4 : DEFAULTS.zone4,
      zone1P2Mode: obj?.zone1P2Mode === 'deadline' ? 'deadline' : 'today',
    }
  } catch (_) {
    return { ...DEFAULTS }
  }
}

const prefs = ref(readPrefs())

watch(prefs, (v) => {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(v)) } catch (_) { /* noop */ }
}, { deep: true })

export function useDashboardZonePrefs() {
  /** 특정 Zone 의 기본 시작 페이지로 지정 */
  function setZoneDefault(zoneKey, pageIndex) {
    if (!(zoneKey in prefs.value)) return
    prefs.value = { ...prefs.value, [zoneKey]: pageIndex }
  }
  /** Zone1 P2 토글 기본값 지정 */
  function setZone1P2Mode(mode) {
    prefs.value = { ...prefs.value, zone1P2Mode: mode === 'deadline' ? 'deadline' : 'today' }
  }
  return { prefs, setZoneDefault, setZone1P2Mode }
}
