<script setup>
import { computed, ref } from 'vue'

const tabs = [
  { value: 'library', label: '\uD504\uB808\uC784 \uB77C\uC774\uBE0C\uB7EC\uB9AC', hint: '\uC804\uCCB4 \uAE30\uC900' },
  { value: 'reference', label: '\uB808\uD37C\uB7F0\uC2A4 \uAE30\uBC18 \uC0DD\uC131', hint: '\uC0DD\uC131 \uD6C4\uBCF4' },
  { value: 'standard', label: '\uD45C\uC900 \uD504\uB808\uC784', hint: '\uACF5\uC2DD \uAE30\uC900' },
]

const workTypes = [
  { value: 'all', label: '\uC804\uCCB4' },
  { value: 'planning', label: '\uAE30\uD68D' },
  { value: 'workorder', label: '\uC6CC\uD06C\uC624\uB354' },
  { value: 'review', label: '\uAC80\uC218 \uC694\uCCAD' },
  { value: 'report', label: '\uACB0\uACFC \uBCF4\uACE0' },
]

const statusLabels = {
  approved: '\uC2B9\uC778 \uC644\uB8CC',
  draft: '\uCD08\uC548',
  review: '\uAC80\uC218 \uC911',
}

const sourceLabels = {
  basic: '\uAE30\uBCF8 \uC81C\uACF5',
  reference: '\uB808\uD37C\uB7F0\uC2A4 \uC0DD\uC131',
}

const frames = [
  {
    id: 'frame-vip-talk',
    name: 'VIP \uCD08\uCCAD \uC54C\uB9BC\uD1A1 \uCEA0\uD398\uC778 \uD504\uB808\uC784',
    type: 'review',
    typeLabel: '\uAC80\uC218 \uC694\uCCAD',
    source: 'basic',
    status: 'approved',
    standard: true,
    version: 'v2.1',
    updatedAt: '2026.04.24',
    campaigns: 12,
    completion: 100,
    summary: 'VIP \uACE0\uAC1D\uAD70 \uB300\uC0C1 \uC54C\uB9BC\uD1A1\uC5D0 \uD544\uC694\uD55C \uD544\uC218 \uBB38\uAD6C, \uAE08\uC9C0 \uD45C\uD604, \uC2B9\uC778\uC790\uB97C \uACE0\uC815\uD569\uB2C8\uB2E4.',
    purpose: '\uBA54\uC2DC\uC9C0\uB85C \uD769\uC5B4\uC9C0\uB294 \uD53C\uB4DC\uBC31\uC744 \uC904\uC774\uACE0 \uC2B9\uC778 \uC774\uB825\uC744 \uCEA0\uD398\uC778 \uC548\uC5D0 \uB0A8\uAE41\uB2C8\uB2E4.',
    recommend: '\uC6CC\uD06C\uC624\uB354 \uC0C1\uD0DC\uAC00 \uC791\uC131 \uC911\uC5D0\uC11C \uAC80\uC218 \uC694\uCCAD\uC73C\uB85C \uBCC0\uACBD\uB420 \uB54C \uC790\uB3D9 \uC801\uC6A9',
    required: ['\uB300\uC0C1 \uACE0\uAC1D\uAD70', '\uD575\uC2EC \uD61C\uD0DD', '\uBC1C\uC1A1 \uC77C\uC815', '\uBC84\uD2BC \uB9C1\uD06C'],
    rules: ['\uBCF8\uBB38 900\uC790 \uC774\uB0B4', '\uBC84\uD2BC \uCD5C\uB300 2\uAC1C', '\uD61C\uD0DD \uC870\uAC74 \uD45C\uAE30 \uD544\uC218'],
    review: ['\uB2F4\uB2F9\uC790 \uAC80\uC218 \uC694\uCCAD', '\uAC80\uC218\uC790 \uC218\uC815/\uC2B9\uC778', '\uBCF8\uC0AC \uCD5C\uC885 \uC2B9\uC778', '\uCD5C\uC885\uBCF8 \uC7A0\uAE08'],
    approvers: ['\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uB2F4\uB2F9', '\uAC24\uB7EC\uB9AC\uC544 CRM \uB2F4\uB2F9'],
    history: ['v2.1: \uBC84\uD2BC \uB9C1\uD06C \uAC80\uC218 \uD56D\uBAA9 \uCD94\uAC00', 'v2.0: VIP \uC138\uADF8\uBA3C\uD2B8 \uAE30\uC900 \uCD94\uAC00', 'v1.5: \uAE08\uC9C0 \uD45C\uD604 \uBAA9\uB85D \uC815\uB9AC'],
  },
  {
    id: 'frame-banner-workorder',
    name: '\uACF5\uB3D9 \uBC30\uB108 \uC81C\uC791 \uC6CC\uD06C\uC624\uB354 \uD504\uB808\uC784',
    type: 'workorder',
    typeLabel: '\uC6CC\uD06C\uC624\uB354',
    source: 'basic',
    status: 'approved',
    standard: true,
    version: 'v1.8',
    updatedAt: '2026.04.22',
    campaigns: 9,
    completion: 92,
    summary: '\uBC30\uB108 \uC81C\uC791 \uC694\uCCAD \uC2DC \uC0AC\uC774\uC988, CTA, \uC81C\uCD9C \uD30C\uC77C, \uB514\uC790\uC778 \uAC80\uC218 \uAE30\uC900\uC744 \uD1B5\uC77C\uD569\uB2C8\uB2E4.',
    purpose: '\uBCF8\uC0AC, \uD611\uB825\uC0AC, \uB300\uD589\uC0AC\uAC00 \uAC19\uC740 \uC81C\uC791 \uC694\uCCAD \uAE30\uC900\uC73C\uB85C \uC6C0\uC9C1\uC774\uAC8C \uD569\uB2C8\uB2E4.',
    recommend: '\uBC30\uB108/\uB514\uC790\uC778 \uC5C5\uBB34 \uC0DD\uC131 \uC2DC \uC6B0\uC120 \uCD94\uCC9C',
    required: ['\uBC30\uB108 \uC704\uCE58', '\uC0AC\uC774\uC988', 'CTA', '\uC774\uBBF8\uC9C0 \uB808\uD37C\uB7F0\uC2A4'],
    rules: ['PC 1920x640', 'MO 750x900', '\uC6D0\uBCF8 \uD30C\uC77C \uC81C\uCD9C', '\uB85C\uACE0 \uC138\uC774\uD504\uC874 \uC900\uC218'],
    review: ['\uC81C\uC791 \uC694\uCCAD', '\uB514\uC790\uC778 \uAC80\uC218', '\uBB38\uAD6C \uAC80\uC218', '\uC2B9\uC778 \uC644\uB8CC'],
    approvers: ['\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uB2F4\uB2F9', '\uB300\uD589\uC0AC AD'],
    history: ['v1.8: \uBAA8\uBC14\uC77C \uBC30\uB108 \uADDC\uACA9 \uCD94\uAC00', 'v1.6: \uC6D0\uBCF8 \uD30C\uC77C \uC81C\uCD9C \uAE30\uC900 \uCD94\uAC00', 'v1.2: CTA \uAC80\uC218 \uAE30\uC900 \uCD94\uAC00'],
  },
  {
    id: 'frame-premium-ref',
    name: '\uD504\uB9AC\uBBF8\uC5C4 \uB77C\uC774\uD504\uC2A4\uD0C0\uC77C \uCEA0\uD398\uC778 \uD504\uB808\uC784',
    type: 'planning',
    typeLabel: '\uAE30\uD68D',
    source: 'reference',
    status: 'review',
    standard: false,
    version: 'v0.9',
    updatedAt: '2026.04.26',
    campaigns: 1,
    completion: 76,
    summary: '\uC774\uC804 VIP \uCD08\uCCAD \uCEA0\uD398\uC778 \uB808\uD37C\uB7F0\uC2A4\uC5D0\uC11C \uACE0\uAC1D\uAD70, \uD61C\uD0DD, \uCC44\uB110, \uC5ED\uD560 \uAD6C\uC870\uB97C \uCD94\uCD9C\uD588\uC2B5\uB2C8\uB2E4.',
    purpose: '\uC131\uACF5\uD55C \uACF5\uB3D9 \uCEA0\uD398\uC778\uC758 \uAD6C\uC870\uB97C \uB2E4\uC74C \uCEA0\uD398\uC778 \uC2DC\uC791 \uAE30\uC900\uC73C\uB85C \uC7AC\uC0AC\uC6A9\uD569\uB2C8\uB2E4.',
    recommend: '\uD504\uB9AC\uBBF8\uC5C4 \uACE0\uAC1D\uAD70\uACFC \uBCF5\uC218 \uD611\uB825\uC0AC\uAC00 \uD3EC\uD568\uB41C \uCEA0\uD398\uC778\uC5D0 \uCD94\uCC9C',
    required: ['\uCEA0\uD398\uC778 \uBAA9\uC801', '\uCC38\uC5EC \uD611\uB825\uC0AC', '\uACE0\uAC1D \uC138\uADF8\uBA3C\uD2B8', '\uD575\uC2EC \uD61C\uD0DD'],
    rules: ['\uD611\uB825\uC0AC\uBCC4 \uCC45\uC784\uC790 \uC9C0\uC815', '\uC131\uACFC \uC9C0\uD45C 3\uAC1C \uC774\uD558', '\uC77C\uC815 \uC8FC\uCC28 \uB2E8\uC704 \uD45C\uAE30'],
    review: ['AI \uCD08\uC548', '\uBCF8\uC0AC \uAC80\uD1A0', '\uD611\uB825\uC0AC \uD655\uC778', '\uC800\uC7A5'],
    approvers: ['\uBCF8\uC0AC PM', '\uD611\uB825\uC0AC \uAD00\uB9AC\uC790'],
    history: ['v0.9: \uC131\uACFC \uC9C0\uD45C \uD56D\uBAA9 \uBCF4\uAC15', 'v0.6: \uD611\uB825\uC0AC \uC5ED\uD560 \uBD84\uB9AC', 'v0.3: \uB808\uD37C\uB7F0\uC2A4 \uCD08\uC548 \uC0DD\uC131'],
  },
]

const activeTab = ref('library')
const activeType = ref('all')
const selectedFrameId = ref(frames[0].id)

const libraryFrames = computed(() => {
  if (activeTab.value === 'standard') return frames.filter((frame) => frame.standard)
  if (activeTab.value === 'reference') return frames.filter((frame) => frame.source === 'reference')
  return frames
})

const filteredFrames = computed(() => {
  if (activeType.value === 'all') return libraryFrames.value
  return libraryFrames.value.filter((frame) => frame.type === activeType.value)
})

const selectedFrame = computed(() => {
  const available = filteredFrames.value
  return available.find((frame) => frame.id === selectedFrameId.value) ?? available[0] ?? frames[0]
})

const checkItems = computed(() => [
  { label: '\uD544\uC218 \uD56D\uBAA9', value: selectedFrame.value.required.length + '\uAC1C' },
  { label: '\uAC80\uC218 \uAE30\uC900', value: selectedFrame.value.review.length + '\uAC1C' },
  { label: '\uC2B9\uC778\uC790', value: selectedFrame.value.approvers.length + '\uBA85' },
  { label: '\uC0AC\uC6A9 \uCEA0\uD398\uC778', value: selectedFrame.value.campaigns + '\uAC74' },
])

function selectTab(tab) {
  activeTab.value = tab
  activeType.value = 'all'
  selectedFrameId.value = libraryFrames.value[0]?.id ?? frames[0].id
}

function selectType(type) {
  activeType.value = type
  selectedFrameId.value = filteredFrames.value[0]?.id ?? frames[0].id
}
</script>

<template>
  <section class="campaign-frame-console">
    <aside class="frame-left">
      <div class="brand-card">
        <div class="brand-mark">F</div>
        <div>
          <p>CAMPAIGN FRAME</p>
          <h2>캠페인 프레임 관리</h2>
        </div>
      </div>
      <section class="definition-card">
        <p>정의</p>
        <strong>캠페인 프레임은 공동 캠페인의 실행 기준입니다.</strong>
        <span>필수 항목, 금지 표현, 제출 규격, 승인 라우팅을 구조화합니다.</span>
      </section>
      <section class="side-panel">
        <div class="side-title">
          <h3>프레임 유형</h3>
          <button type="button">+ 새 프레임</button>
        </div>
        <button
          v-for="type in workTypes"
          :key="type.value"
          type="button"
          :class="['type-row', { active: activeType === type.value }]"
          @click="selectType(type.value)"
        >
          <span>{{ type.label }}</span>
          <em>{{ type.value === 'all' ? frames.length : frames.filter((frame) => frame.type === type.value).length }}</em>
        </button>
      </section>
      <section class="frame-stack">
        <button
          v-for="frame in filteredFrames"
          :key="frame.id"
          type="button"
          :class="['library-card', { active: selectedFrame.id === frame.id }]"
          @click="selectedFrameId = frame.id"
        >
          <span>{{ frame.typeLabel }}</span>
          <strong>{{ frame.name }}</strong>
          <small>{{ sourceLabels[frame.source] }} · {{ frame.version }} · {{ frame.campaigns }}회 사용</small>
        </button>
      </section>
    </aside>

    <main class="frame-main">
      <section class="hero-card">
        <div>
          <p>CONTROL STANDARD</p>
          <h1>{{ selectedFrame.name }}</h1>
          <span>{{ selectedFrame.summary }}</span>
        </div>
        <div class="completion-card">
          <span>프레임 완성도</span>
          <strong>{{ selectedFrame.completion }}%</strong>
          <em>{{ selectedFrame.standard ? '공식 프레임' : '검토 프레임' }} · {{ selectedFrame.updatedAt }}</em>
        </div>
      </section>
      <section class="top-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="['top-tab', { active: activeTab === tab.value }]"
          @click="selectTab(tab.value)"
        >
          <strong>{{ tab.label }}</strong>
          <span>{{ tab.hint }}</span>
        </button>
      </section>
      <section class="summary-grid">
        <article>
          <p>PURPOSE</p>
          <h3>이 프레임의 역할</h3>
          <span>{{ selectedFrame.purpose }}</span>
        </article>
        <article>
          <p>AUTO RECOMMEND</p>
          <h3>추천 조건</h3>
          <span>{{ selectedFrame.recommend }}</span>
        </article>
        <article>
          <p>APPLIES TO</p>
          <h3>적용 대상</h3>
          <div class="tag-list">
            <em>{{ selectedFrame.typeLabel }}</em>
            <em>{{ sourceLabels[selectedFrame.source] }}</em>
            <em>{{ statusLabels[selectedFrame.status] }}</em>
          </div>
        </article>
      </section>
      <section class="detail-grid">
        <article class="large-panel">
          <div class="panel-head">
            <div>
              <p>VERSION HISTORY</p>
              <h3>버전 이력</h3>
            </div>
            <span>{{ selectedFrame.version }}</span>
          </div>
          <ul class="soft-list">
            <li v-for="item in selectedFrame.history" :key="item">{{ item }}</li>
          </ul>
        </article>
        <article class="large-panel">
          <div class="panel-head">
            <div>
              <p>STRUCTURE</p>
              <h3>필수 구조</h3>
            </div>
          </div>
          <div class="mini-grid">
            <div>
              <strong>필수 입력</strong>
              <ul>
                <li v-for="item in selectedFrame.required" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div>
              <strong>제출 기준</strong>
              <ul>
                <li v-for="item in selectedFrame.rules" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </article>
      </section>
    </main>

    <aside class="frame-right">
      <section class="approval-card">
        <p>APPROVAL ROUTE</p>
        <h3>승인 라우팅</h3>
        <ol>
          <li v-for="(step, index) in selectedFrame.review" :key="step">
            <span>{{ index + 1 }}</span>
            <strong>{{ step }}</strong>
          </li>
        </ol>
      </section>
      <section class="right-panel">
        <p>FRAME CHECK</p>
        <h3>프레임 품질 점검</h3>
        <dl>
          <div v-for="item in checkItems" :key="item.label">
            <dt>{{ item.label }}</dt>
            <dd>{{ item.value }}</dd>
          </div>
        </dl>
      </section>
      <section class="right-panel">
        <p>ACTIONS</p>
        <h3>운영 액션</h3>
        <button type="button" class="dark-action">캠페인에 적용</button>
        <button type="button">공식 프레임 지정</button>
        <button type="button">새 버전 저장</button>
      </section>
    </aside>
  </section>
</template>

<style scoped>
.campaign-frame-console {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 308px;
  gap: 16px;
  color: var(--text-primary);
}

.frame-left,
.frame-main,
.frame-right {
  display: grid;
  align-content: start;
  gap: 16px;
}

.brand-card,
.definition-card,
.side-panel,
.library-card,
.hero-card,
.top-tab,
.summary-grid article,
.large-panel,
.approval-card,
.right-panel {
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: var(--panel-color);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.brand-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #111827;
  color: #fff;
  font-weight: 900;
}

.brand-card p,
.definition-card p,
.hero-card p,
.summary-grid p,
.panel-head p,
.approval-card p,
.right-panel p {
  margin: 0;
  color: #7b8497;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.brand-card h2,
.side-title h3,
.summary-grid h3,
.panel-head h3,
.approval-card h3,
.right-panel h3 {
  margin: 0;
}

.definition-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: linear-gradient(135deg, #111827, #1f2937 55%, #4a281e);
  color: #fff;
}

.definition-card strong {
  font-size: 17px;
  line-height: 1.45;
}

.definition-card span,
.hero-card span {
  color: rgba(255, 255, 255, 0.75);
  font-size: 13px;
  line-height: 1.5;
}

.summary-grid span {
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.side-panel,
.large-panel,
.right-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.side-title,
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.side-title button {
  border: 0;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 900;
}

.type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-muted);
  padding: 0 12px;
  color: var(--text-primary);
  font-weight: 800;
}

.type-row.active,
.library-card.active,
.top-tab.active {
  border-color: #ff8a3d;
  background: #fff7ed;
}

.type-row em,
.library-card span {
  color: #f97316;
  font-style: normal;
  font-weight: 900;
}

.frame-stack {
  display: grid;
  gap: 10px;
}

.library-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  text-align: left;
  color: var(--text-primary);
}

.library-card strong {
  font-size: 15px;
}

.library-card small {
  color: #64748b;
  font-weight: 800;
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 186px;
  padding: 24px;
  background: linear-gradient(135deg, #111827, #1f2937);
  color: #fff;
}

.hero-card h1 {
  margin: 10px 0 12px;
  font-size: clamp(32px, 4vw, 56px);
  line-height: 1.05;
}

.completion-card {
  min-width: 196px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  padding: 18px;
}

.completion-card span,
.completion-card em {
  display: block;
  color: rgba(255, 255, 255, 0.72);
  font-style: normal;
  font-weight: 800;
}

.completion-card strong {
  display: block;
  margin: 8px 0;
  font-size: 38px;
}

.top-tabs,
.summary-grid,
.detail-grid {
  display: grid;
  gap: 12px;
}

.top-tabs,
.summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.top-tab {
  display: grid;
  gap: 8px;
  padding: 18px;
  text-align: left;
  color: var(--text-primary);
}

.top-tab span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.summary-grid article {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-list em {
  border-radius: 999px;
  background: #eef2f7;
  color: #475467;
  padding: 6px 10px;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.detail-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.panel-head span {
  border-radius: 999px;
  background: #eef2f7;
  color: #475467;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 900;
}

.soft-list,
.mini-grid ul {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.soft-list li,
.mini-grid div {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-muted);
  padding: 12px;
  color: #475467;
  font-size: 13px;
  font-weight: 700;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-grid strong {
  display: block;
  margin-bottom: 10px;
}

.mini-grid li {
  color: #64748b;
  font-size: 13px;
}

.approval-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: linear-gradient(135deg, #111827, #1f2937 55%, #4a281e);
  color: #fff;
}

.approval-card ol {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.approval-card li {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  padding: 12px;
}

.approval-card li span {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #f97316;
  color: #fff;
  font-weight: 900;
}

.right-panel dl {
  display: grid;
  gap: 10px;
  margin: 0;
}

.right-panel dl div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-muted);
  padding: 12px;
}

.right-panel dt {
  color: #64748b;
  font-weight: 800;
}

.right-panel dd {
  margin: 0;
  font-weight: 900;
}

.right-panel button {
  min-height: 42px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--panel-muted);
  color: var(--text-primary);
  font-weight: 900;
}

.right-panel .dark-action {
  border-color: #111827;
  background: #111827;
  color: #fff;
}

@media (max-width: 1280px) {
  .campaign-frame-console {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .frame-right {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .campaign-frame-console,
  .top-tabs,
  .summary-grid,
  .detail-grid,
  .frame-right,
  .mini-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
