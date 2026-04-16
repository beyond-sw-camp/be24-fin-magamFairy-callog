<script setup>
import { computed } from 'vue'
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()

const reportSummary = computed(() => {
  return store.reports.map((report, index) => ({
    ...report,
    accent: ['#2f80ed', '#df5f75', '#59c36d'][index % 3],
  }))
})
</script>

<template>
  <section class="reports-view">
    <div class="reports-view__header">
      <div>
        <p class="section-eyebrow">리포트</p>
        <h2>요약 지표와 마감 현황이 현재 필터 상태와 연결된 채로 함께 표시됩니다.</h2>
      </div>
    </div>

    <div class="reports-view__grid">
      <article
        v-for="report in reportSummary"
        :key="report.id"
        class="reports-view__card"
        :style="{ '--report-accent': report.accent }"
      >
        <strong>{{ report.metric }}</strong>
        <h3>{{ report.title }}</h3>
        <p>{{ report.detail }}</p>
      </article>
    </div>

    <article class="reports-view__table">
      <div class="reports-view__table-head">
        <strong>현재 보이는 보드 항목</strong>
        <span>{{ store.filteredTasks.length }}</span>
      </div>

      <div class="reports-view__rows">
        <div v-for="task in store.filteredTasks" :key="task.id" class="reports-view__row">
          <div>
            <strong>{{ task.title }}</strong>
            <p>{{ task.requirementId }} · {{ task.customer }}</p>
          </div>
          <span>{{ task.dueDate }}</span>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.reports-view {
  display: grid;
  gap: 1rem;
}

.reports-view__header h2 {
  margin-top: 0.35rem;
  font-size: 1.5rem;
  line-height: 1.3;
}

.reports-view__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.reports-view__card,
.reports-view__table {
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: var(--panel-color);
  box-shadow: var(--shadow-soft);
  padding: 1.2rem;
}

.reports-view__card {
  background:
    linear-gradient(
      180deg,
      color-mix(in srgb, var(--report-accent) 12%, var(--panel-color)),
      var(--panel-color)
    ),
    var(--panel-color);
}

.reports-view__card strong {
  font-size: 1.8rem;
}

.reports-view__card p,
.reports-view__row p,
.reports-view__table-head span,
.reports-view__row span {
  color: var(--muted-text);
}

.reports-view__table-head,
.reports-view__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.reports-view__rows {
  display: grid;
  gap: 0.8rem;
  margin-top: 1rem;
}

.reports-view__row {
  padding: 0.9rem 1rem;
  border-radius: 18px;
  background: var(--panel-muted);
}

@media (max-width: 980px) {
  .reports-view__grid {
    grid-template-columns: 1fr;
  }
}
</style>
