<script setup>
import { computed, ref } from 'vue'

const t = {
  eyebrow: '\uCEA0\uD398\uC778 \uC2E4\uD589 \uAE30\uC900',
  title: '\uCEA0\uD398\uC778 \uD504\uB808\uC784 \uB77C\uC774\uBE0C\uB7EC\uB9AC',
  desc: '\uBCF8\uC0AC, \uD611\uB825\uC0AC, \uB300\uD589\uC0AC\uAC00 \uAC19\uC740 \uAE30\uC900\uC73C\uB85C \uCEA0\uD398\uC778 \uBB38\uC11C\uB97C \uC791\uC131\uD558\uACE0 \uAC80\uC218\uD558\uB294 \uC2E4\uD589 \uAD6C\uC870\uC785\uB2C8\uB2E4.',
  all: '\uC804\uCCB4',
  newFrame: '\uC0C8 \uD504\uB808\uC784',
  aiDraft: 'AI \uCD08\uC548',
  standardZone: '\uD45C\uC900 \uD504\uB808\uC784\uC874',
  referenceZone: '\uB808\uD37C\uB7F0\uC2A4 \uAE30\uBC18 \uD504\uB808\uC784\uC874',
  draftZone: '\uAC80\uD1A0 \uC911 \uD504\uB808\uC784\uC874',
  standardDesc: '\uBCF8\uC0AC \uC2B9\uC778\uC744 \uAC70\uCE5C \uACF5\uC2DD \uAE30\uC900',
  referenceDesc: '\uC644\uB8CC \uCEA0\uD398\uC778 \uC0B0\uCD9C\uBB3C\uC5D0\uC11C \uCD94\uCD9C\uD55C \uC7AC\uC0AC\uC6A9 \uAD6C\uC870',
  draftDesc: '\uC544\uC9C1 \uAC80\uD1A0\uB098 \uC2B9\uC778\uC774 \uD544\uC694\uD55C \uD504\uB808\uC784',
  detail: '\uD504\uB808\uC784 \uAD6C\uC131',
  structureInfo: '\uD504\uB808\uC784 \uAD6C\uC131 \uBCF4\uAE30',
  close: '\uB2EB\uAE30',
  purpose: '\uC0AC\uC6A9 \uBAA9\uC801',
  required: '\uD544\uC218 \uC785\uB825 \uD56D\uBAA9',
  tone: '\uC0AC\uB0B4 \uD1A4\uC564\uB9E4\uB108',
  banned: '\uAE08\uC9C0\uC5B4 / \uAE08\uC9C0 \uD45C\uD604',
  evidence: '\uADFC\uAC70 / \uC81C\uCD9C \uC790\uB8CC',
  channel: '\uCC44\uB110 / \uC81C\uCD9C \uADDC\uACA9',
  review: '\uAC80\uC218 \uB2E8\uACC4',
  approver: '\uC2B9\uC778 \uAD8C\uD55C',
  reuse: '\uC7AC\uC0AC\uC6A9 \uAE30\uC900',
  judge: 'AI \uD310\uC0AC',
  judgeDesc: '\uBB38\uC11C\uB97C \uC62C\uB9AC\uBA74 \uC120\uD0DD\uD55C \uD504\uB808\uC784\uC758 \uD544\uC218 \uD56D\uBAA9, \uD1A4\uC564\uB9E4\uB108, \uAE08\uC9C0\uC5B4, \uC81C\uCD9C \uADDC\uACA9\uC744 \uAE30\uC900\uC73C\uB85C \uC801\uD569\uC131\uC744 \uD310\uC815\uD569\uB2C8\uB2E4.',
  upload: '\uBB38\uC11C \uC5C5\uB85C\uB4DC',
  waiting: '\uAC80\uD1A0 \uB300\uAE30',
  pass: '\uC801\uD569',
  caution: '\uBCF4\uC644 \uD544\uC694',
  apply: '\uCEA0\uD398\uC778\uC5D0 \uC801\uC6A9',
  makeStandard: '\uD45C\uC900\uC73C\uB85C \uC2B9\uACA9',
  createFromReference: '\uB808\uD37C\uB7F0\uC2A4\uC5D0\uC11C \uC0DD\uC131',
}

const frames = [
  {
    id: 'vip-talk-standard',
    zone: 'standard',
    type: '\uAC80\uC218 \uC694\uCCAD',
    name: 'VIP \uCD08\uCCAD \uC54C\uB9BC\uD1A1 \uD45C\uC900 \uD504\uB808\uC784',
    status: '\uACF5\uC2DD',
    version: 'v2.1',
    used: 18,
    updatedAt: '2026.04.24',
    score: 96,
    summary: 'VIP \uACE0\uAC1D \uB300\uC0C1 \uC54C\uB9BC\uD1A1\uC758 \uBB38\uAD6C, \uD61C\uD0DD \uD45C\uAE30, \uAC80\uC218 \uB77C\uC6B0\uD305\uC744 \uD45C\uC900\uD654\uD569\uB2C8\uB2E4.',
    purpose: '\uCD08\uCCAD \uCF58\uD150\uCE20\uAC00 \uBE0C\uB79C\uB4DC \uD1A4\uACFC \uD61C\uD0DD \uACE0\uC9C0 \uAE30\uC900\uC744 \uB3D9\uC2DC\uC5D0 \uB9CC\uC871\uD558\uB3C4\uB85D \uAD00\uB9AC',
    required: ['\uB300\uC0C1 \uACE0\uAC1D\uAD70', '\uCD08\uCCAD \uD61C\uD0DD', '\uC774\uC6A9 \uAE30\uAC04', '\uBC84\uD2BC \uB9C1\uD06C', '\uC720\uC758\uC0AC\uD56D'],
    tone: ['\uD504\uB9AC\uBBF8\uC5C4\uD558\uB418 \uACFC\uC7A5\uD558\uC9C0 \uC54A\uC74C', '\uD61C\uD0DD\uBCF4\uB2E4 \uACE0\uAC1D \uACBD\uD5D8\uC744 \uBA3C\uC800 \uC124\uBA85', '\uBCF8\uC0AC \uD1B5\uD569 \uBA54\uC2DC\uC9C0\uC640 \uD45C\uD604 \uC77C\uCE58'],
    banned: ['\uCD5C\uACE0', '\uBB34\uC870\uAC74', '\uB2E8\uB3C5 \uD61C\uD0DD', '\uD655\uC815 \uC218\uC775', '\uACFC\uB3C4\uD55C \uAE34\uAE09\uC131'],
    evidence: ['\uCD5C\uC885 \uBB38\uAD6C \uD30C\uC77C', '\uD61C\uD0DD \uC870\uAC74 \uADFC\uAC70', '\uBC1C\uC1A1 \uB300\uC0C1 \uC138\uADF8\uBA3C\uD2B8', '\uAC80\uC218 \uC694\uCCAD \uB9C1\uD06C'],
    channel: ['\uC54C\uB9BC\uD1A1 \uBCF8\uBB38 900\uC790 \uC774\uB0B4', '\uBC84\uD2BC \uCD5C\uB300 2\uAC1C', '\uD61C\uD0DD \uC870\uAC74 \uD45C\uAE30 \uD544\uC218'],
    review: ['\uB2F4\uB2F9\uC790 \uCD08\uC548', '\uD611\uB825\uC0AC \uD655\uC778', '\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uAC80\uC218', '\uCD5C\uC885 \uC2B9\uC778'],
    approver: ['\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uB9E4\uB2C8\uC800', '\uD611\uB825\uC0AC CRM \uB2F4\uB2F9'],
    reuse: ['\uB3D9\uC77C VIP \uB300\uC0C1 \uCEA0\uD398\uC778', '\uC54C\uB9BC\uD1A1 \uCC44\uB110', '\uD61C\uD0DD \uC548\uB0B4\uD615 \uCF58\uD150\uCE20'],
  },
  {
    id: 'banner-standard',
    zone: 'standard',
    type: '\uC6CC\uD06C\uC624\uB354',
    name: '\uACF5\uB3D9 \uBC30\uB108 \uC81C\uC791 \uD45C\uC900 \uD504\uB808\uC784',
    status: '\uACF5\uC2DD',
    version: 'v1.8',
    used: 11,
    updatedAt: '2026.04.22',
    score: 92,
    summary: '\uBC30\uB108 \uC81C\uC791 \uC694\uCCAD\uC5D0 \uD544\uC694\uD55C \uC0AC\uC774\uC988, CTA, \uB85C\uACE0 \uC0AC\uC6A9, \uC81C\uCD9C \uD30C\uC77C \uAE30\uC900\uC744 \uACE0\uC815\uD569\uB2C8\uB2E4.',
    purpose: '\uB300\uD589\uC0AC\uC640 \uD611\uB825\uC0AC\uAC00 \uCC98\uC74C\uBD80\uD130 \uAC19\uC740 \uC81C\uC791 \uC694\uCCAD \uAE30\uC900\uC73C\uB85C \uC0B0\uCD9C\uBB3C\uC744 \uB9CC\uB4E4\uB3C4\uB85D \uAD00\uB9AC',
    required: ['\uB178\uCD9C \uC704\uCE58', '\uC0AC\uC774\uC988', 'CTA', '\uC774\uBBF8\uC9C0 \uB808\uD37C\uB7F0\uC2A4', '\uCD5C\uC885 \uC0AC\uC6A9 \uCC44\uB110'],
    tone: ['\uBA54\uC2DC\uC9C0\uB294 \uC9E7\uACE0 \uBA85\uD655\uD558\uAC8C', '\uD61C\uD0DD\uC740 \uC870\uAC74\uACFC \uD568\uAED8 \uD45C\uAE30', '\uBE0C\uB79C\uB4DC \uC2DC\uC120 \uD750\uB984 \uC720\uC9C0'],
    banned: ['\uB85C\uACE0 \uBCC0\uD615', '\uC800\uD574\uC0C1\uB3C4 \uC774\uBBF8\uC9C0', '\uD61C\uD0DD \uC870\uAC74 \uC0DD\uB7B5', '\uBC84\uD2BC \uC5C6\uB294 CTA'],
    evidence: ['\uC81C\uC791 \uC694\uCCAD\uC11C', '\uC0AC\uC774\uC988 \uBA85\uC138', '\uB85C\uACE0 \uC6D0\uBCF8', '\uCD5C\uC885 PSD/Figma \uB9C1\uD06C'],
    channel: ['PC 1920x640', 'MO 750x900', '\uC6D0\uBCF8 \uD30C\uC77C \uC81C\uCD9C', '\uB85C\uACE0 \uC138\uC774\uD504\uC874 \uC900\uC218'],
    review: ['\uC81C\uC791 \uC694\uCCAD', '\uB514\uC790\uC778 \uAC80\uC218', '\uBB38\uAD6C \uAC80\uC218', '\uC2B9\uC778 \uC644\uB8CC'],
    approver: ['\uBCF8\uC0AC \uBE0C\uB79C\uB4DC \uB2F4\uB2F9', '\uB300\uD589\uC0AC AD'],
    reuse: ['\uACF5\uB3D9 \uBC30\uB108', '\uB79C\uB529 \uD788\uC5B4\uB85C', '\uC774\uBCA4\uD2B8 \uB178\uCD9C \uC601\uC5ED'],
  },
  {
    id: 'premium-reference',
    zone: 'reference',
    type: '\uAE30\uD68D',
    name: '\uD504\uB9AC\uBBF8\uC5C4 \uB77C\uC774\uD504\uC2A4\uD0C0\uC77C \uB808\uD37C\uB7F0\uC2A4 \uD504\uB808\uC784',
    status: '\uC2B9\uC778 \uD6C4\uBCF4',
    version: 'v0.9',
    used: 1,
    updatedAt: '2026.04.26',
    score: 78,
    summary: '\uC644\uB8CC\uB41C VIP \uD504\uB9AC\uBDF0 \uCEA0\uD398\uC778\uC5D0\uC11C \uACE0\uAC1D\uAD70, \uD61C\uD0DD, \uD611\uB825\uC0AC \uC5ED\uD560\uC744 \uCD94\uCD9C\uD55C \uD504\uB808\uC784\uC785\uB2C8\uB2E4.',
    purpose: '\uC131\uACF5 \uCEA0\uD398\uC778\uC758 \uC2E4\uD589 \uAD6C\uC870\uB97C \uB2E4\uC74C \uACF5\uB3D9 \uCEA0\uD398\uC778\uC758 \uCD08\uAE30 \uAE30\uD68D \uAE30\uC900\uC73C\uB85C \uC7AC\uC0AC\uC6A9',
    required: ['\uCEA0\uD398\uC778 \uBAA9\uC801', '\uCC38\uC5EC \uD611\uB825\uC0AC', '\uACE0\uAC1D \uC138\uADF8\uBA3C\uD2B8', '\uD575\uC2EC \uD61C\uD0DD', '\uC131\uACFC \uC9C0\uD45C'],
    tone: ['\uACC4\uC5F4\uC0AC\uBCC4 \uD61C\uD0DD\uC744 \uD558\uB098\uC758 \uACBD\uD5D8\uC73C\uB85C \uC5F0\uACB0', '\uC2E4\uD589 \uC8FC\uCCB4\uBCF4\uB2E4 \uACE0\uAC1D \uAC00\uCE58 \uC911\uC2EC', '\uD504\uB9AC\uBBF8\uC5C4 \uD1A4 \uC720\uC9C0'],
    banned: ['\uD611\uB825\uC0AC \uAC04 \uC6B0\uC5F4 \uD45C\uD604', '\uD655\uC815\uB418\uC9C0 \uC54A\uC740 \uD61C\uD0DD', '\uBE44\uAD50 \uAD11\uACE0\uC131 \uBB38\uAD6C'],
    evidence: ['\uC774\uC804 \uCEA0\uD398\uC778 \uACB0\uACFC \uBCF4\uACE0\uC11C', '\uCD5C\uC885 \uC0B0\uCD9C\uBB3C', '\uAC80\uC218 \uB85C\uADF8', '\uD611\uB825\uC0AC \uC5ED\uD560\uD45C'],
    channel: ['\uC77C\uC815\uC740 \uC8FC\uCC28 \uB2E8\uC704\uB85C \uD45C\uAE30', '\uC131\uACFC \uC9C0\uD45C 3\uAC1C \uC774\uD558', '\uD611\uB825\uC0AC\uBCC4 \uCC45\uC784\uC790 \uD544\uC218'],
    review: ['AI \uCD08\uC548', '\uBCF8\uC0AC PM \uAC80\uD1A0', '\uD611\uB825\uC0AC \uD655\uC778', '\uD45C\uC900 \uC2B9\uACA9 \uD310\uB2E8'],
    approver: ['\uBCF8\uC0AC PM', '\uD611\uB825\uC0AC \uAD00\uB9AC\uC790'],
    reuse: ['\uD504\uB9AC\uBBF8\uC5C4 \uACE0\uAC1D \uB300\uC0C1', '\uBCF5\uC218 \uD611\uB825\uC0AC \uCC38\uC5EC', '\uC624\uD504\uB77C\uC778 \uD589\uC0AC \uC5F0\uACC4'],
  },
  {
    id: 'report-draft',
    zone: 'draft',
    type: '\uACB0\uACFC \uBCF4\uACE0',
    name: '\uC131\uACFC \uAE30\uB85D \uBC0F \uC7AC\uC0AC\uC6A9 \uD504\uB808\uC784',
    status: '\uAC80\uD1A0 \uC911',
    version: 'v0.4',
    used: 2,
    updatedAt: '2026.04.23',
    score: 64,
    summary: '\uC885\uB8CC\uB41C \uCEA0\uD398\uC778\uC758 \uC131\uACFC, \uC774\uC288, \uCD5C\uC885 \uC0B0\uCD9C\uBB3C\uC744 \uB2E4\uC74C \uCEA0\uD398\uC778 \uC790\uC0B0\uC73C\uB85C \uC800\uC7A5\uD569\uB2C8\uB2E4.',
    purpose: '\uCEA0\uD398\uC778 \uC885\uB8CC \uD6C4 \uCD5C\uC885 \uC790\uB8CC\uC640 \uC131\uACFC \uADFC\uAC70\uB97C \uCD95\uC801\uD574 \uD504\uB808\uC784 \uC0DD\uC131 \uC18C\uC2A4\uB85C \uC0AC\uC6A9',
    required: ['\uC131\uACFC \uC9C0\uD45C', '\uCD5C\uC885 \uC0B0\uCD9C\uBB3C', '\uAC80\uC218 \uC774\uC288', '\uC7AC\uC0AC\uC6A9 \uD56D\uBAA9'],
    tone: ['\uC131\uACFC\uB294 \uADFC\uAC70 \uC911\uC2EC\uC73C\uB85C \uAE30\uB85D', '\uC774\uC288\uB294 \uC6D0\uC778\uACFC \uC870\uCE58\uB97C \uBD84\uB9AC', '\uC7AC\uC0AC\uC6A9 \uAC00\uB2A5\uD55C \uAE30\uC900\uB9CC \uBA85\uD655\uD788 \uD45C\uAE30'],
    banned: ['\uADFC\uAC70 \uC5C6\uB294 \uC131\uACFC \uD45C\uD604', '\uAC1C\uC778 \uD0D3 \uC911\uC2EC \uC774\uC288', '\uCD5C\uC885\uBCF8 \uB9C1\uD06C \uB204\uB77D'],
    evidence: ['\uCD5C\uC885\uBCF8 \uB9C1\uD06C', '\uC131\uACFC \uB370\uC774\uD130', '\uAC80\uC218 \uC774\uC288 \uB85C\uADF8', '\uD68C\uACE0 \uBA54\uBAA8'],
    channel: ['\uC9C0\uD45C \uADFC\uAC70 \uD544\uC218', '\uC774\uC288\uBCC4 \uC6D0\uC778/\uC870\uCE58 \uBD84\uB9AC', '\uD30C\uC77C \uB9C1\uD06C \uC720\uD6A8\uC131 \uD655\uC778'],
    review: ['\uACB0\uACFC \uC785\uB825', '\uC131\uACFC \uD655\uC778', '\uB808\uD37C\uB7F0\uC2A4 \uBD84\uB958', '\uBCF4\uAD00'],
    approver: ['\uBCF8\uC0AC PM', '\uB370\uC774\uD130 \uB2F4\uB2F9'],
    reuse: ['\uACB0\uACFC \uBCF4\uACE0', '\uB808\uD37C\uB7F0\uC2A4 \uC800\uC7A5', 'AI \uD504\uB808\uC784 \uD559\uC2B5 \uC18C\uC2A4'],
  },
]

const selectedFrameId = ref(frames[0].id)
const selectedZone = ref('all')
const uploadedFileName = ref('')
const showStructureInfo = ref(false)

const zones = computed(() => [
  { id: 'standard', title: t.standardZone, desc: t.standardDesc, icon: 'verified', frames: frames.filter((frame) => frame.zone === 'standard') },
  { id: 'reference', title: t.referenceZone, desc: t.referenceDesc, icon: 'auto_awesome', frames: frames.filter((frame) => frame.zone === 'reference') },
  { id: 'draft', title: t.draftZone, desc: t.draftDesc, icon: 'rate_review', frames: frames.filter((frame) => frame.zone === 'draft') },
])

const visibleZones = computed(() => {
  if (selectedZone.value === 'all') return zones.value
  return zones.value.filter((zone) => zone.id === selectedZone.value)
})

const selectedFrame = computed(() => frames.find((frame) => frame.id === selectedFrameId.value) ?? frames[0])

const structureGroups = computed(() => [
  { title: t.required, icon: 'fact_check', items: selectedFrame.value.required },
  { title: t.tone, icon: 'record_voice_over', items: selectedFrame.value.tone },
  { title: t.banned, icon: 'block', items: selectedFrame.value.banned },
  { title: t.evidence, icon: 'attach_file', items: selectedFrame.value.evidence },
  { title: t.channel, icon: 'rule', items: selectedFrame.value.channel },
  { title: t.reuse, icon: 'history', items: selectedFrame.value.reuse },
])

const judgeItems = computed(() => [
  { label: t.required, result: selectedFrame.value.score >= 80 ? t.pass : t.caution },
  { label: t.tone, result: selectedFrame.value.score >= 70 ? t.pass : t.caution },
  { label: t.banned, result: selectedFrame.value.banned.length ? t.caution : t.pass },
  { label: t.channel, result: selectedFrame.value.channel.length >= 3 ? t.pass : t.caution },
])

function selectFrame(frame) {
  selectedFrameId.value = frame.id
  showStructureInfo.value = false
}

function updateFile(event) {
  uploadedFileName.value = event.target.files?.[0]?.name ?? ''
}
</script>

<template>
  <section class="frames-page">
    <header class="frames-hero">
      <div>
        <p class="section-eyebrow">{{ t.eyebrow }}</p>
        <h2>{{ t.title }}</h2>
        <span>{{ t.desc }}</span>
      </div>
      <div class="hero-actions">
        <button type="button" class="soft-action">
          <span class="material-symbols-outlined">auto_awesome</span>
          {{ t.aiDraft }}
        </button>
        <button type="button" class="primary-action">
          <span class="material-symbols-outlined">add</span>
          {{ t.newFrame }}
        </button>
      </div>
    </header>

    <section class="review-strip" aria-label="frame review steps">
      <article class="review-metric">
        <h3>{{ t.review }}</h3>
        <ol>
          <li v-for="(step, index) in selectedFrame.review" :key="step">
            <span>{{ index + 1 }}</span>
            {{ step }}
          </li>
        </ol>
      </article>
    </section>

    <section class="frames-layout">
      <main class="library-column">
        <div class="library-head">
          <div>
            <p class="section-eyebrow">FRAME LIBRARY</p>
            <h3>{{ t.title }}</h3>
          </div>
          <div class="zone-filter" aria-label="frame zone filter">
            <button type="button" :class="{ active: selectedZone === 'all' }" @click="selectedZone = 'all'">
              {{ t.all }} ({{ frames.length }})
            </button>
            <button
              v-for="zone in zones"
              :key="zone.id"
              type="button"
              :class="{ active: selectedZone === zone.id }"
              @click="selectedZone = zone.id"
            >
              {{ zone.title }} ({{ zone.frames.length }})
            </button>
          </div>
        </div>

        <section v-for="zone in visibleZones" :key="zone.id" class="frame-zone">
          <div class="zone-head">
            <span class="material-symbols-outlined">{{ zone.icon }}</span>
            <div>
              <h4>{{ zone.title }}</h4>
              <p>{{ zone.desc }}</p>
            </div>
            <strong>{{ zone.frames.length }}</strong>
          </div>

          <div class="zone-card-grid">
            <button
              v-for="frame in zone.frames"
              :key="frame.id"
              type="button"
              class="frame-card"
              :class="{ active: selectedFrame.id === frame.id }"
              @click="selectFrame(frame)"
            >
              <span>{{ frame.type }} / {{ frame.status }}</span>
              <strong>{{ frame.name }}</strong>
              <p>{{ frame.summary }}</p>
              <div>
                <small>{{ frame.version }} / {{ frame.updatedAt }}</small>
                <em>{{ frame.used }} used</em>
              </div>
            </button>
          </div>
        </section>
      </main>

      <aside class="judge-panel">
        <div class="judge-head">
          <span class="material-symbols-outlined">gavel</span>
          <div>
            <p class="section-eyebrow">{{ t.judge }}</p>
            <h3>{{ t.judge }}</h3>
          </div>
        </div>
        <p class="judge-desc">{{ t.judgeDesc }}</p>

        <label class="upload-box">
          <input type="file" @change="updateFile" />
          <span class="material-symbols-outlined">upload_file</span>
          <strong>{{ uploadedFileName || t.upload }}</strong>
          <small>{{ selectedFrame.name }}</small>
        </label>

        <div class="judge-score">
          <span>{{ uploadedFileName ? t.caution : t.waiting }}</span>
          <strong>{{ uploadedFileName ? selectedFrame.score : '--' }}</strong>
        </div>

        <dl class="judge-checks">
          <div v-for="item in judgeItems" :key="item.label">
            <dt>{{ item.label }}</dt>
            <dd :class="{ caution: item.result === t.caution }">{{ uploadedFileName ? item.result : t.waiting }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <section class="detail-grid" :class="{ 'detail-grid--single': !showStructureInfo }">
      <article class="selected-summary">
        <div class="summary-top">
          <div>
            <span>{{ selectedFrame.type }} / {{ selectedFrame.status }}</span>
            <h3>{{ selectedFrame.name }}</h3>
            <p>{{ selectedFrame.purpose }}</p>
          </div>
          <div class="summary-actions">
            <button
              type="button"
              class="info-button"
              :aria-label="t.structureInfo"
              @click="showStructureInfo = !showStructureInfo"
            >
              <span class="material-symbols-outlined">info</span>
            </button>
            <div class="score-pill">
              <small>FRAME SCORE</small>
              <strong>{{ selectedFrame.score }}</strong>
            </div>
          </div>
        </div>

        <div class="approver-list">
          <h4>{{ t.approver }}</h4>
          <span v-for="item in selectedFrame.approver" :key="item">{{ item }}</span>
        </div>
      </article>

      <article v-if="showStructureInfo" class="structure-panel">
        <div class="panel-title">
          <div>
            <p class="section-eyebrow">{{ t.detail }}</p>
            <h3>{{ t.detail }}</h3>
          </div>
          <button type="button" class="info-button" :aria-label="t.close" @click="showStructureInfo = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <div class="structure-card-grid">
          <section v-for="group in structureGroups" :key="group.title" class="structure-card">
            <div>
              <span class="material-symbols-outlined">{{ group.icon }}</span>
              <h4>{{ group.title }}</h4>
            </div>
            <ul>
              <li v-for="item in group.items" :key="item">{{ item }}</li>
            </ul>
          </section>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.frames-page {
  display: flex;
  width: 100%;
  max-width: var(--content-max-width);
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
  color: var(--text-primary);
}

.frames-hero,
.review-strip,
.library-column,
.judge-panel,
.selected-summary,
.structure-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.frames-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
}

.frames-hero h2,
.library-head h3,
.judge-head h3,
.summary-top h3,
.panel-title h3 {
  margin: 4px 0 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0;
}

.frames-hero span:not(.material-symbols-outlined),
.frame-card p,
.zone-head p,
.judge-desc,
.summary-top p,
.upload-box small {
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.55;
}

.hero-actions {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}

.primary-action,
.soft-action {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0 14px;
  font-weight: 800;
}

.primary-action {
  border-color: transparent;
  background: var(--accent-color);
  color: #fff;
}

.soft-action {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.review-strip {
  padding: 14px 16px;
}

.review-metric {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 16px;
}

.frame-card span,
.summary-top span,
.score-pill small {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.review-metric h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 900;
  white-space: nowrap;
}

.review-metric ol {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.review-metric li {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 6px;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 0 9px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.review-metric li span {
  display: grid;
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--accent-color);
  color: #fff;
  font-size: 11px;
}

.frames-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
}

.library-column,
.judge-panel,
.selected-summary,
.structure-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.library-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.zone-filter {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.zone-filter button {
  min-height: 32px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  padding: 0 12px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.zone-filter button.active {
  border-color: color-mix(in srgb, var(--accent-color) 35%, var(--border-color));
  background: var(--badge-bg);
  color: var(--badge-text);
}

.frame-zone {
  display: grid;
  gap: 10px;
}

.zone-head {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.zone-head > span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--accent-soft);
  color: var(--accent-strong);
}

.zone-head h4,
.zone-head p,
.structure-card h4,
.approver-list h4 {
  margin: 0;
}

.zone-head strong {
  display: inline-flex;
  min-width: 28px;
  min-height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 12px;
}

.zone-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.frame-card {
  display: grid;
  gap: 8px;
  min-height: 148px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 14px;
  text-align: left;
}

.frame-card.active {
  border-color: color-mix(in srgb, var(--accent-color) 45%, var(--border-color));
  background: color-mix(in srgb, var(--accent-color) 8%, var(--panel-color));
}

.frame-card strong {
  font-size: 15px;
  line-height: 1.35;
}

.frame-card div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
}

.frame-card small,
.frame-card em {
  color: var(--muted-text);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.judge-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.judge-head > span {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-100);
  color: var(--accent-strong);
}

.upload-box {
  display: grid;
  min-height: 138px;
  place-items: center;
  gap: 6px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 18px;
  text-align: center;
  cursor: pointer;
}

.upload-box input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.upload-box .material-symbols-outlined {
  color: var(--accent-strong);
  font-size: 30px;
}

.judge-score {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: var(--radius-md);
  background: var(--color-primary-50);
  padding: 14px;
  color: var(--color-primary-800);
}

.judge-score span {
  font-size: 12px;
  font-weight: 800;
}

.judge-score strong {
  font-size: 30px;
}

.judge-checks {
  display: grid;
  gap: 8px;
  margin: 0;
}

.judge-checks div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 10px 12px;
}

.judge-checks dt {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.judge-checks dd {
  margin: 0;
  color: var(--success-color);
  font-size: 12px;
  font-weight: 900;
}

.judge-checks dd.caution {
  color: var(--warning-color);
}

.detail-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.detail-grid--single {
  grid-template-columns: 1fr;
}

.summary-top {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 14px;
}

.summary-actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.info-button {
  display: inline-grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--accent-strong);
  cursor: pointer;
}

.score-pill {
  display: grid;
  min-width: 82px;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 10px;
}

.score-pill strong {
  color: var(--accent-strong);
  font-size: 28px;
}

.approver-list {
  display: grid;
  gap: 10px;
}

.approver-list {
  grid-template-columns: 1fr;
}

.approver-list span {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-full);
  background: var(--panel-muted);
  padding: 7px 10px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.structure-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.structure-card {
  display: grid;
  gap: 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 14px;
}

.structure-card > div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.structure-card .material-symbols-outlined {
  color: var(--accent-strong);
  font-size: 20px;
}

.structure-card ul {
  display: grid;
  gap: 7px;
  margin: 0;
  padding-left: 18px;
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.45;
}

@media (max-width: 1180px) {
  .frames-layout,
  .detail-grid,
  .structure-card-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .frames-hero,
  .library-head,
  .summary-top {
    flex-direction: column;
  }

  .hero-actions,
  .zone-filter {
    justify-content: flex-start;
    width: 100%;
  }

  .zone-card-grid,
  .review-metric,
  .review-metric ol {
    grid-template-columns: 1fr;
  }
}
</style>
