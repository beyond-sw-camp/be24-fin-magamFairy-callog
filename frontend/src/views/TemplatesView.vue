<script setup>
import { usePlannerStore } from '@/stores/planner'

const store = usePlannerStore()
</script>

<template>
  <section class="templates-view">
    <div class="templates-view__header">
      <div>
        <p class="section-eyebrow">템플릿 라이브러리</p>
        <h2>
          반복되는 요청을 더 빠르게 처리할 수 있도록 자주 쓰는 구조를 보드 가까이에 모아둡니다.
        </h2>
      </div>
    </div>

    <div class="templates-view__grid">
      <article v-for="template in store.templates" :key="template.id" class="templates-view__card">
        <div class="templates-view__badge">{{ template.type }}</div>
        <h3>{{ template.name }}</h3>
        <p>{{ template.usage }}</p>

        <div class="templates-view__meta">
          <strong>담당 팀</strong>
          <span>{{ template.owner }}</span>
        </div>

        <div class="templates-view__meta">
          <strong>섹션</strong>
          <div class="templates-view__chips">
            <span v-for="section in template.sections" :key="section">{{ section }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.templates-view {
  display: grid;
  gap: 1rem;
}

.templates-view__header h2 {
  margin-top: 0.35rem;
  font-size: 1.5rem;
  line-height: 1.3;
}

.templates-view__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.templates-view__card {
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: var(--panel-color);
  box-shadow: var(--shadow-soft);
  padding: 1.2rem;
  display: grid;
  gap: 0.9rem;
}

.templates-view__badge {
  width: fit-content;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-color) 12%, var(--panel-muted));
  color: var(--accent-strong);
  padding: 0.38rem 0.7rem;
  font-size: 0.78rem;
  font-weight: 700;
}

.templates-view__card p,
.templates-view__meta span {
  color: var(--muted-text);
}

.templates-view__meta {
  display: grid;
  gap: 0.45rem;
}

.templates-view__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.templates-view__chips span {
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  border-radius: 999px;
  padding: 0.42rem 0.7rem;
}

@media (max-width: 760px) {
  .templates-view__grid {
    grid-template-columns: 1fr;
  }
}
</style>
