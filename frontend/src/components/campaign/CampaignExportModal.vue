<script setup>
import { computed, ref } from 'vue'
import { ExportCampaignCsv, ExportCampaignPdf } from '@/api/campaigns'

const props = defineProps({
  campaignId: { type: [String, Number], required: true },
  campaignName: { type: String, default: '' },
})
const emit = defineEmits(['close'])

const FORMATS = [
  { id: 'summary', label: 'Executive Summary', desc: '한 페이지 임원·이해관계자 공유용 (PDF)', enabled: true, badge: '권장', kind: 'pdf' },
  { id: 'full',    label: 'Full Performance Report', desc: '다중 페이지 상세 보고서 (PDF)', enabled: true, kind: 'pdf' },
  { id: 'csv',     label: 'Excel 데이터 (CSV)', desc: '엑셀에서 열 수 있는 표 형식', enabled: true, kind: 'csv' },
]

const SECTIONS = [
  { id: 'campaign', label: '캠페인 정보', desc: '이름·기간·목적·태그' },
  { id: 'members', label: '팀 멤버', desc: '참여자·역할·소속' },
  { id: 'tasks', label: '업무 목록', desc: 'task 상태·담당·마감일' },
  { id: 'kpi', label: 'KPI 지표', desc: '전체 KPI 측정 결과' },
  { id: 'esg', label: 'ESG 지표', desc: 'ESG 카테고리 KPI만' },
]

const format = ref('summary')
const selectedSections = ref(SECTIONS.map((s) => s.id))
const downloading = ref(false)
const error = ref('')

const isCsv = computed(() => FORMATS.find((f) => f.id === format.value)?.kind === 'csv')
const isPdf = computed(() => FORMATS.find((f) => f.id === format.value)?.kind === 'pdf')

function toggleSection(id) {
  const i = selectedSections.value.indexOf(id)
  if (i === -1) selectedSections.value.push(id)
  else selectedSections.value.splice(i, 1)
}
function selectAll() { selectedSections.value = SECTIONS.map((s) => s.id) }
function selectNone() { selectedSections.value = [] }

const canDownload = computed(() => {
  if (downloading.value) return false
  if (isCsv.value) return selectedSections.value.length > 0
  return true
})

const buttonLabel = computed(() => {
  if (downloading.value) return '생성 중…'
  if (isCsv.value) return 'CSV 다운로드'
  if (format.value === 'summary') return 'Summary PDF 다운로드'
  return 'Full Report PDF 다운로드'
})

async function handleDownload() {
  if (!canDownload.value) return
  downloading.value = true
  error.value = ''
  try {
    const result = isCsv.value
      ? await ExportCampaignCsv(props.campaignId, selectedSections.value)
      : await ExportCampaignPdf(props.campaignId, format.value)
    triggerDownload(result.blob, result.fileName)
    emit('close')
  } catch (e) {
    const status = e?.response?.status
    if (status === 404) error.value = '캠페인을 찾을 수 없습니다.'
    else if (status === 401 || status === 403) error.value = '권한이 없습니다.'
    else if (status === 500) error.value = 'PDF 생성에 실패했습니다. 잠시 후 다시 시도해주세요.'
    else error.value = '내보내기 중 오류가 발생했습니다.'
  } finally {
    downloading.value = false
  }
}

function triggerDownload(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
</script>

<template>
  <Teleport to="body">
    <div class="ex-overlay" role="presentation" @click.self="emit('close')">
      <section class="ex-shell" role="dialog" aria-modal="true" aria-labelledby="exTitle">
        <header class="ex-head">
          <div>
            <div class="ex-eyebrow">CAMPAIGN · 내보내기</div>
            <h2 id="exTitle" class="ex-title">{{ campaignName || '캠페인 내보내기' }}</h2>
          </div>
          <button class="ex-close" aria-label="닫기" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </header>

        <div class="ex-body">
          <!-- 1. 포맷 선택 -->
          <section class="ex-section">
            <h3 class="ex-section__title">① 파일 포맷</h3>
            <div class="ex-format-list">
              <label
                v-for="f in FORMATS"
                :key="f.id"
                class="ex-format"
                :class="{
                  'ex-format--active': format === f.id,
                  'ex-format--disabled': !f.enabled,
                }"
              >
                <input
                  type="radio"
                  name="ex-format"
                  :value="f.id"
                  v-model="format"
                  :disabled="!f.enabled"
                />
                <div class="ex-format__copy">
                  <div class="ex-format__top">
                    <strong>{{ f.label }}</strong>
                    <em
                      v-if="f.badge"
                      class="ex-format__badge"
                      :class="{ 'ex-format__badge--soft': !f.enabled }"
                    >{{ f.badge }}</em>
                    <em class="ex-format__kind">{{ f.kind.toUpperCase() }}</em>
                  </div>
                  <small>{{ f.desc }}</small>
                </div>
              </label>
            </div>
          </section>

          <!-- 2. 포함 섹션 (CSV에서만 표시) -->
          <section v-if="isCsv" class="ex-section">
            <div class="ex-section__head">
              <h3 class="ex-section__title">② 포함할 섹션</h3>
              <div class="ex-section__quick">
                <button type="button" class="ex-quick" @click="selectAll">전체 선택</button>
                <span class="ex-quick-sep">·</span>
                <button type="button" class="ex-quick" @click="selectNone">전체 해제</button>
              </div>
            </div>
            <div class="ex-checks">
              <label
                v-for="s in SECTIONS"
                :key="s.id"
                class="ex-check"
                :class="{ 'ex-check--on': selectedSections.includes(s.id) }"
              >
                <input
                  type="checkbox"
                  :checked="selectedSections.includes(s.id)"
                  @change="toggleSection(s.id)"
                />
                <div class="ex-check__copy">
                  <strong>{{ s.label }}</strong>
                  <small>{{ s.desc }}</small>
                </div>
              </label>
            </div>
          </section>

          <!-- 2. PDF 안내 (PDF 선택 시) -->
          <section v-if="isPdf" class="ex-section">
            <h3 class="ex-section__title">② 포함 내용</h3>
            <div class="ex-info-card">
              <template v-if="format === 'summary'">
                <strong>1페이지 PDF에 다음이 포함됩니다</strong>
                <ul>
                  <li>캠페인 헤더 (이름·기간·상태)</li>
                  <li>업무 현황 4개 요약 카드</li>
                  <li>KPI 게이지 + 상위 3개 지표</li>
                  <li>ESG 점수 게이지 + 등급(AAA~CCC)</li>
                </ul>
              </template>
              <template v-else>
                <strong>다중 페이지 PDF에 다음이 포함됩니다</strong>
                <ul>
                  <li>1쪽: Executive Summary (요약)</li>
                  <li>2쪽: KPI 전체 지표 테이블</li>
                  <li>3쪽: 업무 목록 (최대 30건)</li>
                  <li>4쪽: 팀 멤버 + ESG 지표 상세</li>
                </ul>
              </template>
            </div>
          </section>

          <p v-if="error" class="ex-error">{{ error }}</p>
        </div>

        <footer class="ex-foot">
          <div class="ex-foot__hint">
            <template v-if="isCsv">
              <span>{{ selectedSections.length }}개 섹션 선택</span>
              <span class="ex-foot__sep">·</span>
              <span>예상 ~{{ Math.max(1, selectedSections.length * 6) }}KB</span>
            </template>
            <template v-else>
              <span>{{ format === 'summary' ? '1페이지' : '4페이지' }} PDF</span>
              <span class="ex-foot__sep">·</span>
              <span>예상 ~{{ format === 'summary' ? 80 : 200 }}KB</span>
            </template>
          </div>
          <div class="ex-foot__actions">
            <button type="button" class="ex-btn ex-btn--ghost" @click="emit('close')">취소</button>
            <button
              type="button"
              class="ex-btn ex-btn--primary"
              :disabled="!canDownload"
              @click="handleDownload"
            >
              <svg v-if="!downloading" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M7 10l5 5 5-5M12 15V3"/>
              </svg>
              {{ buttonLabel }}
            </button>
          </div>
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.ex-overlay {
  position: fixed; inset: 0; z-index: 100;
  display: flex; align-items: center; justify-content: center;
  padding: 28px;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(2px);
}

.ex-shell {
  width: min(580px, 100%);
  max-height: min(800px, calc(100vh - 56px));
  display: flex; flex-direction: column;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.32);
  overflow: hidden;
  color: var(--text-primary);
}

.ex-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  gap: 16px; padding: 22px 24px 16px;
  border-bottom: 1px solid var(--border-color);
}
.ex-eyebrow {
  font-size: 11px; font-weight: 800; letter-spacing: 0.08em;
  color: var(--color-primary-600); text-transform: uppercase;
}
.ex-title { margin: 6px 0 0; font-size: 19px; font-weight: 850; letter-spacing: -0.3px; }
.ex-close {
  display: inline-grid; place-items: center;
  width: 34px; height: 34px;
  border: 1px solid var(--border-color); border-radius: var(--radius-sm);
  background: var(--panel-muted); color: var(--text-secondary);
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast);
}
.ex-close:hover { background: var(--panel-color); color: var(--text-primary); }

.ex-body { padding: 18px 24px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 20px; }

.ex-section { display: flex; flex-direction: column; gap: 10px; }
.ex-section__head { display: flex; align-items: center; justify-content: space-between; }
.ex-section__title {
  font-size: 13px; font-weight: 800; color: var(--text-primary);
  letter-spacing: -0.1px;
}
.ex-section__quick { display: flex; align-items: center; gap: 6px; font-size: 11px; }
.ex-quick {
  font-size: 11px; font-weight: 700; color: var(--color-primary-600);
  cursor: pointer; background: none; border: none; padding: 0;
}
.ex-quick:hover { color: var(--color-primary-700); }
.ex-quick-sep { color: var(--border-strong); }

/* Format selection */
.ex-format-list { display: flex; flex-direction: column; gap: 8px; }
.ex-format {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}
.ex-format:hover:not(.ex-format--disabled) { border-color: var(--border-strong); background: var(--panel-muted); }
.ex-format--active {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 6%, var(--panel-color));
}
.ex-format--disabled { opacity: 0.55; cursor: not-allowed; }
.ex-format input[type="radio"] { margin-top: 3px; accent-color: var(--color-primary-500); }
.ex-format__copy { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.ex-format__top { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.ex-format__top strong { font-size: 13px; font-weight: 800; color: var(--text-primary); }
.ex-format__copy small { font-size: 11px; color: var(--muted-text); }

.ex-format__badge {
  display: inline-flex; align-items: center;
  font-size: 10px; font-style: normal; font-weight: 800;
  border-radius: var(--radius-sm);
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  padding: 1px 6px; letter-spacing: 0.4px;
}
.ex-format__badge--soft {
  background: var(--panel-muted);
  color: var(--muted-text);
}
.ex-format__kind {
  font-size: 9px; font-style: normal; font-weight: 800;
  border: 1px solid var(--border-color);
  color: var(--muted-text);
  border-radius: 3px; padding: 1px 5px;
  letter-spacing: 0.5px;
  margin-left: auto;
}

/* Section checkboxes */
.ex-checks { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.ex-check {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}
.ex-check:hover { border-color: var(--border-strong); background: var(--panel-muted); }
.ex-check--on {
  border-color: var(--color-primary-500);
  background: color-mix(in srgb, var(--color-primary-500) 5%, var(--panel-color));
}
.ex-check input[type="checkbox"] { margin-top: 2px; accent-color: var(--color-primary-500); }
.ex-check__copy { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.ex-check__copy strong { font-size: 12px; font-weight: 800; color: var(--text-primary); }
.ex-check__copy small { font-size: 10px; color: var(--muted-text); }

/* PDF info card */
.ex-info-card {
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  font-size: 12px;
  color: var(--text-secondary);
}
.ex-info-card strong {
  display: block;
  font-size: 12px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.ex-info-card ul {
  margin: 0;
  padding-left: 18px;
  display: flex; flex-direction: column; gap: 3px;
}
.ex-info-card li { font-size: 11.5px; color: var(--text-secondary); }

.ex-error {
  margin: 0;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
  font-size: 12px;
  font-weight: 700;
}

/* Footer */
.ex-foot {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; padding: 14px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--panel-muted);
}
.ex-foot__hint { font-size: 12px; color: var(--muted-text); display: flex; align-items: center; gap: 6px; font-weight: 600; }
.ex-foot__sep { color: var(--border-strong); }
.ex-foot__actions { display: flex; gap: 8px; }

.ex-btn {
  display: inline-flex; align-items: center; gap: 6px;
  height: 36px; padding: 0 14px;
  border-radius: var(--radius-md);
  font-size: 13px; font-weight: 800;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}
.ex-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.ex-btn--ghost {
  background: var(--panel-color); color: var(--text-primary);
  border: 1px solid var(--border-color);
}
.ex-btn--ghost:hover:not(:disabled) { background: var(--panel-muted); border-color: var(--border-strong); }

.ex-btn--primary {
  background: var(--color-primary-500); color: #fff;
  border: 1px solid var(--color-primary-500);
}
.ex-btn--primary:hover:not(:disabled) { background: var(--color-primary-600); border-color: var(--color-primary-600); }

@media (max-width: 540px) {
  .ex-checks { grid-template-columns: 1fr; }
}
</style>
