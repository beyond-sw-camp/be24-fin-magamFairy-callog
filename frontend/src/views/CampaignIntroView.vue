<template>
  <div class="op" @click="closeIconPicker()">
    <div class="reading-progress" :style="{ width: scrollProgress + '%' }"></div>

    <!-- Breadcrumb + Edit toggle -->
    <div class="op-topbar">
      <nav class="op-breadcrumb" aria-label="breadcrumb">
        <router-link
          :to="{ name: 'campaign-detail', params: { campaignId: route.params.campaignId } }"
          class="op-breadcrumb__item op-breadcrumb__item--link"
        >{{ campaignName }}</router-link>
        <span class="op-breadcrumb__sep">›</span>
        <span
          class="op-breadcrumb__item"
          :class="{ 'op-breadcrumb__item--current': !editMode }"
          :aria-current="!editMode ? 'page' : undefined"
        >캠페인 소개</span>
        <template v-if="editMode">
          <span class="op-breadcrumb__sep">›</span>
          <span class="op-breadcrumb__item op-breadcrumb__item--current" aria-current="page">편집</span>
        </template>
      </nav>
      <div class="op-topbar__actions">
        <button v-if="!editMode && canEdit" class="btn btn--ghost btn--sm" @click="enterEdit">
          <iconify-icon icon="fluent-emoji:pencil"></iconify-icon>편집
        </button>
        <template v-else-if="editMode && canEdit">
          <button class="btn btn--ghost btn--sm" :disabled="saving" @click="cancelEdit">취소</button>
          <button class="btn btn--primary btn--sm" :disabled="saving" @click="saveEdit">
            {{ saving ? '저장 중...' : '저장' }}
          </button>
        </template>
      </div>
    </div>

    <p v-if="errorMsg" class="op-error-banner">{{ errorMsg }}</p>

    <!-- Skeleton Loader (initial load) -->
    <div v-if="loading && !introData" class="skeleton-stack" aria-busy="true" aria-label="페이지 로딩 중">
      <div class="skeleton skeleton--hero"></div>
      <div class="skeleton-strip">
        <div class="skeleton skeleton--card"></div>
        <div class="skeleton skeleton--card"></div>
        <div class="skeleton skeleton--card"></div>
      </div>
      <div class="skeleton skeleton--kpi"></div>
      <div class="skeleton skeleton--block"></div>
      <div class="skeleton skeleton--block" style="height:200px"></div>
    </div>

    <!-- Hero -->
    <header v-show="!loading || introData" class="op-hero">
      <div class="op-hero__left">
        <div v-if="(!editMode && rfpCode) || editMode" class="op-hero__badges">
          <code v-if="!editMode && rfpCode" class="op-rfp">{{ rfpCode }}</code>
          <input
            v-if="editMode"
            v-model="editDraft.rfpCode"
            class="op-input op-input--inline"
            placeholder="RFP 코드 (예: RFP-2026-045)"
          />
          <span v-if="!editMode" class="trust-badge" title="한화 공식 RFP — 인증된 캠페인 제안서">
            <iconify-icon icon="fluent-emoji:check-mark-button"></iconify-icon>
            Verified by 한화
          </span>
        </div>
        <div class="op-hero__title-row">
          <button
            type="button"
            class="btn-back"
            @click="editMode ? cancelEdit() : router.push({ name: 'campaign-detail', params: { campaignId: route.params.campaignId } })"
            aria-label="뒤로 가기"
          >←</button>
          <h1 class="op-hero__title">{{ campaignName }}</h1>
          <span class="badge" :class="`badge--${statusToTone(campaignStatus)}`">{{ statusToLabel(campaignStatus) }}</span>
        </div>
        <div class="op-hero__meta">
          <span>
            <iconify-icon icon="fluent-emoji:bust-in-silhouette"></iconify-icon>담당: {{ ownerDisplay }}
            <em v-if="ownerEmail" class="op-hero__meta-sub">· {{ ownerEmail }}</em>
          </span>
          <span><iconify-icon icon="fluent-emoji:eyes"></iconify-icon>공개 범위: 인증 사용자</span>
        </div>
      </div>

      <div class="op-hero__right">
        <div class="deadline-box">
          <div class="deadline-box__top">
            <div class="deadline-box__text">
              <div class="deadline-box__label">제안 마감까지</div>
              <div v-if="!editMode" class="deadline-box__date">{{ formatDate(recruitDeadline) }}</div>
              <input
                v-else
                v-model="editDraft.recruitDeadline"
                type="datetime-local"
                class="op-input op-input--datetime"
              />
            </div>
            <div v-if="!editMode" class="deadline-box__dday">{{ computeDday(recruitDeadline) }}</div>
          </div>
          <div v-if="!editMode && deadlineProgress !== null" class="deadline-progress">
            <div class="deadline-progress__track">
              <div class="deadline-progress__fill" :style="{ width: deadlineProgress + '%' }"></div>
            </div>
            <div class="deadline-progress__labels">
              <span>캠페인 시작</span>
              <span class="deadline-progress__pct">{{ deadlineProgress }}% 경과</span>
              <span>모집 마감</span>
            </div>
          </div>
          <div v-if="!editMode" class="deadline-box__social">
            <span class="deadline-box__social-dot"></span>
            <iconify-icon icon="fluent-emoji:eyes"></iconify-icon>
            지금까지 <strong>{{ viewCount.toLocaleString() }}회</strong> 조회됨
          </div>
        </div>
      </div>
    </header>

    <!-- Campaign Essentials (주 목표 / 자산명 / 캠페인 방식) -->
    <div v-if="(primaryGoal || assetName || campaignMethods?.length) && (!loading || introData)" id="sec-essentials" class="essentials-strip">
      <div v-if="primaryGoal" class="essentials-card">
        <div class="essentials-card__head">
          <iconify-icon icon="fluent-emoji:bullseye"></iconify-icon>
          <span>주 목표</span>
        </div>
        <div class="essentials-card__value">{{ primaryGoal }}</div>
      </div>
      <div v-if="assetName" class="essentials-card">
        <div class="essentials-card__head">
          <iconify-icon icon="fluent-emoji:wrapped-gift"></iconify-icon>
          <span>자산명</span>
        </div>
        <div class="essentials-card__value">{{ assetName }}</div>
      </div>
      <div v-if="campaignMethods?.length" class="essentials-card">
        <div class="essentials-card__head">
          <iconify-icon icon="fluent-emoji:rocket"></iconify-icon>
          <span>캠페인 방식</span>
        </div>
        <div class="essentials-card__value essentials-card__value--tags">
          <span v-for="m in campaignMethods" :key="m" class="essentials-tag">{{ m }}</span>
        </div>
      </div>
    </div>

    <!-- Hero KPI Strip (WYSIWYG) -->
    <div v-if="(editMode || heroKpis.length) && (!loading || introData)" class="kpi-strip">
      <div
        v-for="(kpi, i) in (editMode ? editDraft.heroKpis : heroKpis)"
        :key="i"
        class="kpi-card"
        :class="{'kpi-card--editable': editMode}"
      >
        <input v-if="editMode" v-model="kpi.value" class="kpi-card__value ghost-input ghost-input--center" placeholder="50만+" />
        <div v-else class="kpi-card__value">{{ kpi.value }}</div>
        <input v-if="editMode" v-model="kpi.label" class="kpi-card__label ghost-input ghost-input--center" placeholder="예상 월 노출" />
        <div v-else class="kpi-card__label">{{ kpi.label }}</div>
        <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--kpi" @click="removeKpi(i)">
          <iconify-icon icon="fluent-emoji:cross-mark"></iconify-icon>
        </button>
      </div>
      <button v-if="editMode" type="button" class="kpi-card kpi-card--add" @click="addKpi">
        <iconify-icon icon="fluent-emoji:plus" style="font-size:18px"></iconify-icon>
        <span style="color:var(--text-3);font-size:11px">KPI 추가</span>
      </button>
    </div>

    <!-- Tab Nav -->
    <nav v-show="!loading || introData" class="op-tabs" aria-label="페이지 섹션">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="op-tab"
        :class="{ 'op-tab--active': activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
        <span v-if="tab.badge" class="op-tab__badge">{{ tab.badge }}</span>
      </button>
    </nav>

    <!-- Body -->
    <div v-show="!loading || introData" class="op-body">

      <!-- Main Content -->
      <main class="op-main">

        <!-- ───── 상세 정보 탭 ───── -->
        <div v-show="activeTab === 'detail'">

        <!-- 4 통합 섹션 (2×2 그리드) -->
        <div id="sec-info" class="info-grid">

          <!-- 1) 한화 제공 자산 -->
          <section class="card info-card">
            <div class="info-card__head">
              <h2 class="card__title"><iconify-icon icon="fluent-emoji:wrapped-gift"></iconify-icon>한화 제공 자산</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addAsset"><iconify-icon icon="fluent-emoji:plus"></iconify-icon>추가</button>
            </div>
            <ul class="info-list">
              <li
                v-for="(a, i) in (editMode ? editDraft.hanwhaAssets : hanwhaAssets)"
                :key="i"
                class="info-item"
                :class="{'info-item--editable': editMode}"
              >
                <div class="info-item__icon-wrap">
                  <span class="info-item__icon" :class="{'info-item__icon--clickable': editMode}"
                    @click.stop="editMode && toggleIconPicker('hanwha-'+i)">
                    <iconify-icon :icon="`fluent-emoji:${a.icon || 'sparkles'}`"></iconify-icon>
                  </span>
                  <div v-if="editMode && activeIconPicker === 'hanwha-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': a.icon === ic}"
                        @click="a.icon = ic; closeIconPicker()" :title="ic">
                        <iconify-icon :icon="`fluent-emoji:${ic}`"></iconify-icon>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="info-item__body">
                  <input v-if="editMode" v-model="a.title" class="info-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <strong v-else class="info-item__title">{{ a.title }}</strong>
                  <textarea v-if="editMode" v-model="a.desc" class="info-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="info-item__desc">{{ a.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--inline" @click="removeAsset(i)" title="삭제">
                  <iconify-icon icon="fluent-emoji:wastebasket"></iconify-icon>
                </button>
              </li>
            </ul>
          </section>

          <!-- 2) 파트너 기대 역할 -->
          <section class="card info-card">
            <div class="info-card__head">
              <h2 class="card__title"><iconify-icon icon="fluent-emoji:handshake"></iconify-icon>파트너 기대 역할</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addRole"><iconify-icon icon="fluent-emoji:plus"></iconify-icon>추가</button>
            </div>
            <ul class="info-list">
              <li
                v-for="(r, i) in (editMode ? editDraft.partnerRoles : partnerRoles)"
                :key="i"
                class="info-item"
                :class="{'info-item--editable': editMode}"
              >
                <div class="info-item__icon-wrap">
                  <span class="info-item__icon" :class="{'info-item__icon--clickable': editMode}"
                    @click.stop="editMode && toggleIconPicker('role-'+i)">
                    <iconify-icon :icon="`fluent-emoji:${r.icon || 'sparkles'}`"></iconify-icon>
                  </span>
                  <div v-if="editMode && activeIconPicker === 'role-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': r.icon === ic}"
                        @click="r.icon = ic; closeIconPicker()" :title="ic">
                        <iconify-icon :icon="`fluent-emoji:${ic}`"></iconify-icon>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="info-item__body">
                  <input v-if="editMode" v-model="r.title" class="info-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <strong v-else class="info-item__title">{{ r.title }}</strong>
                  <textarea v-if="editMode" v-model="r.desc" class="info-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="info-item__desc">{{ r.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--inline" @click="removeRole(i)" title="삭제">
                  <iconify-icon icon="fluent-emoji:wastebasket"></iconify-icon>
                </button>
              </li>
            </ul>
          </section>

          <!-- 3) 타깃 고객 프로필 -->
          <section class="card info-card">
            <div class="info-card__head">
              <h2 class="card__title"><iconify-icon icon="fluent-emoji:bullseye"></iconify-icon>타깃 고객 프로필</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addCustomer"><iconify-icon icon="fluent-emoji:plus"></iconify-icon>추가</button>
            </div>
            <ul class="info-list">
              <li
                v-for="(c, i) in (editMode ? editDraft.customerItems : customerItems)"
                :key="i"
                class="info-item"
                :class="{'info-item--editable': editMode}"
              >
                <div class="info-item__icon-wrap">
                  <span class="info-item__icon" :class="{'info-item__icon--clickable': editMode}"
                    @click.stop="editMode && toggleIconPicker('customer-'+i)">
                    <iconify-icon :icon="`fluent-emoji:${c.icon || 'sparkles'}`"></iconify-icon>
                  </span>
                  <div v-if="editMode && activeIconPicker === 'customer-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': c.icon === ic}"
                        @click="c.icon = ic; closeIconPicker()" :title="ic">
                        <iconify-icon :icon="`fluent-emoji:${ic}`"></iconify-icon>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="info-item__body">
                  <input v-if="editMode" v-model="c.title" class="info-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <strong v-else class="info-item__title">{{ c.title }}</strong>
                  <textarea v-if="editMode" v-model="c.desc" class="info-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="info-item__desc">{{ c.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--inline" @click="removeCustomer(i)" title="삭제">
                  <iconify-icon icon="fluent-emoji:wastebasket"></iconify-icon>
                </button>
              </li>
              <li v-if="!editMode && !customerItems.length" class="info-item">
                <div class="info-item__body">
                  <p class="info-item__desc" style="color:var(--text-4)">등록된 항목이 없습니다.</p>
                </div>
              </li>
            </ul>
          </section>

          <!-- 4) 파트너 참여 가치 -->
          <section class="card info-card">
            <div class="info-card__head">
              <h2 class="card__title"><iconify-icon icon="fluent-emoji:chart-increasing"></iconify-icon>파트너 참여 가치</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addValue"><iconify-icon icon="fluent-emoji:plus"></iconify-icon>추가</button>
            </div>
            <ul class="info-list">
              <li
                v-for="(v, i) in (editMode ? editDraft.partnerValues : partnerValues)"
                :key="i"
                class="info-item"
                :class="{'info-item--editable': editMode}"
              >
                <div class="info-item__icon-wrap">
                  <span class="info-item__icon" :class="{'info-item__icon--clickable': editMode}"
                    @click.stop="editMode && toggleIconPicker('value-'+i)">
                    <iconify-icon :icon="`fluent-emoji:${v.icon || 'sparkles'}`"></iconify-icon>
                  </span>
                  <div v-if="editMode && activeIconPicker === 'value-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': v.icon === ic}"
                        @click="v.icon = ic; closeIconPicker()" :title="ic">
                        <iconify-icon :icon="`fluent-emoji:${ic}`"></iconify-icon>
                      </button>
                    </div>
                  </div>
                </div>
                <div class="info-item__body">
                  <input v-if="editMode" v-model="v.title" class="info-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <strong v-else class="info-item__title">{{ v.title }}</strong>
                  <textarea v-if="editMode" v-model="v.desc" class="info-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="info-item__desc">{{ v.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--inline" @click="removeValue(i)" title="삭제">
                  <iconify-icon icon="fluent-emoji:wastebasket"></iconify-icon>
                </button>
              </li>
            </ul>
          </section>

        </div>

        </div>
        <!-- ───── /상세 정보 탭 ───── -->

        <!-- ───── 모집 일정 탭 ───── -->
        <div v-show="activeTab === 'schedule'">

        <!-- 진행 일정 타임라인 (WYSIWYG) -->
        <section id="sec-timeline" class="card">
          <div class="tl-header">
            <div>
              <h2 class="card__title"><iconify-icon icon="fluent-emoji:calendar"></iconify-icon>진행 일정 및 타임라인</h2>
              <p class="card__sub">캠페인 런칭 전 주요 일정입니다. 마감 기한을 엄수해 주시기 바랍니다.</p>
            </div>
            <div class="tl-header__right">
              <div class="tl-legend">
                <span v-for="l in legend" :key="l.label" class="legend-item">
                  <span class="legend-dot" :style="{ background: l.color }"></span>{{ l.label }}
                </span>
              </div>
              <button v-if="editMode" type="button" class="btn-add" @click="addEvent"><iconify-icon icon="fluent-emoji:plus"></iconify-icon>일정 추가</button>
            </div>
          </div>

          <div class="tl-track">
            <div
              v-for="(ev, i) in (editMode ? editDraft.timelineEvents : timelineEventsView)"
              :key="ev.id ?? i"
              class="tl-item"
              :class="{ 'tl-item--done': ev.done, 'tl-item--urgent': ev.urgent }"
            >
              <div class="tl-node-col">
                <div class="tl-node" :class="`tl-node--${ev.color ?? 'gray'}`">
                  <i v-if="ev.done && !editMode" class="ph-bold ph-check"></i>
                  <span v-if="ev.urgent && !editMode" class="tl-pulse"></span>
                </div>
              </div>
              <div class="tl-content">
                <div class="tl-row">
                  <h4 class="tl-title" :class="{ 'tl-title--urgent': ev.urgent }">
                    <input v-if="editMode" v-model="ev.title" class="ghost-input ghost-input--bold" style="font-size:15px" placeholder="일정 제목" />
                    <template v-else>
                      {{ ev.title }}
                      <span v-if="ev.tag" class="tl-tag" :class="`tl-tag--${ev.tagColor}`">{{ ev.tag }}</span>
                    </template>
                  </h4>
                  <input v-if="editMode" type="date" v-model="ev.date" class="tl-date tl-date--gray ghost-input--date" />
                  <span v-else class="tl-date" :class="`tl-date--${ev.color}`">{{ ev.displayDate }}</span>
                </div>
                <!-- 뷰: detail/docs/note -->
                <template v-if="!editMode">
                  <div v-if="ev.detail" class="tl-detail">
                    <p v-if="ev.detail.method" class="tl-detail__method">
                      <iconify-icon icon="fluent-emoji:movie-camera"></iconify-icon>{{ ev.detail.method }}
                    </p>
                    <p class="tl-detail__text">{{ ev.detail.text }}</p>
                  </div>
                  <div v-if="ev.docs && ev.docs.length" class="tl-docs">
                    <p class="tl-docs__title">제출 서류:</p>
                    <ul><li v-for="doc in ev.docs" :key="doc">{{ doc }}</li></ul>
                  </div>
                  <p v-if="ev.note" class="tl-note">{{ ev.note }}</p>
                </template>
                <!-- 편집: 컴팩트 컨트롤바 + 상세 설정 펼침 -->
                <template v-else>
                  <div class="tl-edit-bar">
                    <select v-model="ev.color" class="op-select op-select--sm">
                      <option value="gray">회색</option>
                      <option value="yellow">노랑 (마감)</option>
                      <option value="purple">보라 (심사)</option>
                      <option value="green">초록 (운영)</option>
                      <option value="blue">파랑 (안내)</option>
                      <option value="red">빨강 (긴급)</option>
                    </select>
                    <label class="op-check"><input type="checkbox" v-model="ev.done" /> 완료</label>
                    <label class="op-check"><input type="checkbox" v-model="ev.urgent" /> 긴급</label>
                    <details class="tl-extra-details" style="flex:1">
                      <summary class="tl-extra-summary">상세 설정</summary>
                      <div class="tl-extra-body">
                        <div class="tl-edit-row2">
                          <label class="op-field"><span class="op-field__label">태그 텍스트</span>
                            <input v-model="ev.tag" class="op-input" placeholder="예: 중요" /></label>
                          <label class="op-field"><span class="op-field__label">태그 색상</span>
                            <select v-model="ev.tagColor" class="op-select">
                              <option value="">없음</option><option value="red">빨강</option>
                              <option value="blue">파랑</option><option value="green">초록</option>
                              <option value="yellow">노랑</option>
                            </select></label>
                        </div>
                        <label class="op-field"><span class="op-field__label">회의 방식</span>
                          <input v-model="ev.detailMethod" class="op-input" placeholder="예: 화상 회의" /></label>
                        <label class="op-field"><span class="op-field__label">상세 설명</span>
                          <textarea v-model="ev.detailText" class="op-input op-input--area" rows="2" placeholder="일정 상세 내용"></textarea></label>
                        <label class="op-field"><span class="op-field__label">제출 서류 (줄 구분)</span>
                          <textarea v-model="ev.docsText" class="op-input op-input--area" rows="2" placeholder="서류1&#10;서류2"></textarea></label>
                        <label class="op-field"><span class="op-field__label">비고</span>
                          <input v-model="ev.note" class="op-input" placeholder="비고" /></label>
                      </div>
                    </details>
                    <button type="button" class="btn-del" @click="removeEvent(i)"><iconify-icon icon="fluent-emoji:wastebasket"></iconify-icon></button>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </section>

        <!-- 제출 안내 (WYSIWYG) -->
        <section id="sec-submission" class="card">
          <h2 class="card__title"><iconify-icon icon="fluent-emoji:outbox-tray"></iconify-icon>제출 안내 및 양식</h2>
          <div class="submission-box">
            <div class="submission-box__head">
              <div>
                <template v-if="!editMode">
                  <div class="submission-box__name">{{ submissionInfo.name }}</div>
                  <div class="submission-box__desc">{{ submissionInfo.desc }}</div>
                </template>
                <template v-else>
                  <input v-model="editDraft.submissionInfo.name" class="submission-box__name ghost-input" placeholder="제출 방법 이름" />
                  <input v-model="editDraft.submissionInfo.desc" class="submission-box__desc ghost-input" placeholder="설명" />
                </template>
              </div>
              <span v-if="!editMode" class="submission-box__limit">{{ submissionInfo.limit }}</span>
              <input v-else v-model="editDraft.submissionInfo.limit" class="submission-box__limit ghost-input" style="max-width:180px;text-align:right" placeholder="파일 제한 안내" />
            </div>
            <h4 class="submission-docs__title">필수 제출 서류</h4>
            <ul class="submission-docs">
              <li
                v-for="(doc, i) in (editMode ? editDraft.submissionDocs : submissionDocs)"
                :key="i"
                class="submission-doc"
                :class="{'submission-doc--editable': editMode}"
              >
                <template v-if="!editMode">
                  <iconify-icon :icon="`fluent-emoji:${doc.icon}`"></iconify-icon>
                  <span class="submission-doc__label">{{ doc.label }}</span>
                  <span class="req-badge" :class="{ 'req-badge--opt': !doc.required }">{{ doc.required ? '필수' : '선택' }}</span>
                </template>
                <template v-else>
                  <select v-model="doc.icon" class="op-select op-select--sm" style="width:72px">
                    <option value="file-pdf">PDF</option><option value="file-xls">엑셀</option>
                    <option value="file-text">텍스트</option><option value="file-doc">Word</option>
                    <option value="file-zip">ZIP</option><option value="file-ppt">PPT</option>
                    <option value="file">파일</option>
                  </select>
                  <iconify-icon :icon="`fluent-emoji:${doc.icon}`" :style="{ fontSize: '16px', flexShrink: 0 }"></iconify-icon>
                  <input v-model="doc.color" type="color" class="op-color-input" />
                  <input v-model="doc.label" class="submission-doc__label ghost-input" placeholder="서류 이름" />
                  <label class="op-check"><input type="checkbox" v-model="doc.required" />필수</label>
                  <button type="button" class="wysiwyg-del" @click="removeDoc(i)"><iconify-icon icon="fluent-emoji:wastebasket"></iconify-icon></button>
                </template>
              </li>
              <li v-if="editMode" class="submission-doc submission-doc--add" @click="addDoc">
                <iconify-icon icon="fluent-emoji:plus"></iconify-icon>
                <span class="submission-doc__label" style="color:var(--primary)">서류 추가</span>
              </li>
            </ul>
          </div>
        </section>

        <!-- 자주 묻는 질문 (FAQ) -->
        <section v-if="!editMode" id="sec-faq" class="card faq-card">
          <h2 class="card__title"><iconify-icon icon="fluent-emoji:thinking-face"></iconify-icon>자주 묻는 질문</h2>
          <ul class="faq-list">
            <li v-for="(item, i) in faqList" :key="i" class="faq-item" :class="{'faq-item--open': openFaqs.has(i)}">
              <button type="button" class="faq-question" @click="toggleFaq(i)">
                <span class="faq-question__q">Q.</span>
                <span class="faq-question__text">{{ item.q }}</span>
                <iconify-icon class="faq-caret" icon="fluent-emoji:down-arrow"></iconify-icon>
              </button>
              <div v-if="openFaqs.has(i)" class="faq-answer">
                <span class="faq-answer__a">A.</span>
                <p>{{ item.a }}</p>
              </div>
            </li>
          </ul>
        </section>

        </div>
        <!-- ───── /모집 일정 탭 ───── -->

      </main>

      <!-- Sidebar -->
      <aside class="op-sidebar">

        <!-- CTA — PM이 아닌 사용자(=파트너 후보)에게만 제휴 제안 노출 -->
        <div v-if="!canEdit" class="card card--cta">
          <h3 class="cta-title">제휴 제안하기</h3>
          <p class="cta-desc">상세 요건을 확인하셨다면 기한 내에 제안서를 제출해 주세요.</p>
          <button class="btn btn--primary btn--block" @click="goToProposal">
            <iconify-icon icon="fluent-emoji:envelope-with-arrow"></iconify-icon>공식 제안서 제출
          </button>
        </div>

        <!-- 첨부 자료실 — 항목이 있을 때만 노출 -->
        <div v-if="attachedFiles && attachedFiles.length > 0" class="card">
          <h3 class="card__title-sm"><iconify-icon icon="fluent-emoji:file-folder"></iconify-icon>첨부 자료실</h3>
          <div class="file-list">
            <div
              v-for="f in attachedFiles"
              :key="f.name"
              class="file-item"
              :class="{ 'file-item--locked': f.locked }"
            >
              <div class="file-icon" :class="`file-icon--${f.tone}`">
                <iconify-icon :icon="`fluent-emoji:${f.icon}`"></iconify-icon>
              </div>
              <div class="file-info">
                <div class="file-name">{{ f.name }}</div>
                <div class="file-size">{{ f.size }}</div>
              </div>
              <iconify-icon :icon="`fluent-emoji:${f.locked ? 'locked' : 'down-arrow'}`" class="file-action"></iconify-icon>
            </div>
          </div>
        </div>

        <!-- 심사 평가 기준 (매칭 5축 가중치) — 내부 사용자(HQ/AFFILIATE)와 편집 권한자만 노출 (P1) -->
        <div v-if="isInternalViewer || canEdit" class="card">
          <h3 class="card__title-sm">
            <iconify-icon icon="fluent-emoji:balance-scale"></iconify-icon>심사 평가 기준 (매칭 가중치)
            <span class="op-internal-tag" aria-label="내부 전용">내부</span>
          </h3>
          <!-- 보기 모드 -->
          <div v-if="!editMode && hasAnyWeight" class="criteria-list">
            <div v-for="c in matchWeights" :key="c.label" class="criteria-item">
              <div class="criteria-row">
                <span class="criteria-label">{{ c.label }}</span>
                <strong class="criteria-pct">{{ c.value ?? 0 }}%</strong>
              </div>
              <div class="criteria-track">
                <div class="criteria-fill" :style="{ width: (c.value ?? 0) + '%' }"></div>
              </div>
            </div>
          </div>
          <p v-else-if="!editMode" class="op-empty-hint">매칭 가중치가 아직 설정되지 않았습니다.</p>
          <!-- 편집 모드 -->
          <div v-if="editMode" class="criteria-edit">
            <label class="op-field">
              <span class="op-field__label">고객 적합도 (%)</span>
              <input v-model.number="editDraft.weightCustomer" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">수익 효과 (%)</span>
              <input v-model.number="editDraft.weightRevenue" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">비용 구조 (%)</span>
              <input v-model.number="editDraft.weightCost" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">운영 부담 (%)</span>
              <input v-model.number="editDraft.weightOperation" type="number" min="0" max="100" class="op-input" />
            </label>
            <label class="op-field">
              <span class="op-field__label">브랜드 적합 (%)</span>
              <input v-model.number="editDraft.weightBrand" type="number" min="0" max="100" class="op-input" />
            </label>
          </div>
        </div>

        <!-- 담당자 문의 -->
        <div class="contact-card">
          <div class="contact-label">담당자 문의</div>
          <!-- 보기 모드 -->
          <template v-if="!editMode">
            <div class="contact-person">
              <div class="contact-avatar">{{ (contactInfo?.name ?? '?').charAt(0) }}</div>
              <div>
                <div class="contact-name">{{ contactInfo?.name ?? '담당자 미지정' }}</div>
                <div class="contact-team">{{ contactInfo?.team ?? '' }}</div>
              </div>
            </div>
            <div class="contact-info">
              <div class="contact-info__row"><iconify-icon icon="fluent-emoji:envelope"></iconify-icon>{{ contactInfo?.email ?? '-' }}</div>
              <div class="contact-info__row"><iconify-icon icon="fluent-emoji:telephone-receiver"></iconify-icon>{{ contactInfo?.phone ?? '-' }}</div>
            </div>
          </template>
          <!-- 편집 모드 -->
          <div v-else class="contact-edit">
            <label class="op-field">
              <span class="op-field__label">담당자명</span>
              <input v-model="editDraft.contactInfo.name" class="op-input" placeholder="홍길동 리드" />
            </label>
            <label class="op-field">
              <span class="op-field__label">소속 팀</span>
              <input v-model="editDraft.contactInfo.team" class="op-input" placeholder="○○사업팀" />
            </label>
            <label class="op-field">
              <span class="op-field__label">이메일</span>
              <input v-model="editDraft.contactInfo.email" type="email" class="op-input" placeholder="example@hanwha.com" />
            </label>
            <label class="op-field">
              <span class="op-field__label">전화번호</span>
              <input v-model="editDraft.contactInfo.phone" class="op-input" placeholder="02-1234-5678" />
            </label>
          </div>
        </div>

      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { GetCampaignIntro, UpdateCampaignIntro } from '@/api/campaigns'

const route = useRoute()
const router = useRouter()

const activeTab = ref('detail')
const introData = ref(null)
const loading = ref(true)
const errorMsg = ref('')

// 인라인 편집 상태
const editMode = ref(false)
const editDraft = ref(null)
const saving = ref(false)
const activeIconPicker = ref(null)

// 권한 (P0) — backend가 응답에 canEdit / isInternalViewer 채워줌. 미응답 시 false (안전 default)
const canEdit = computed(() => Boolean(introData.value?.canEdit))
const isInternalViewer = computed(() => Boolean(introData.value?.isInternalViewer))


const tabs = [
  { id: 'detail', label: '상세 정보' },
  { id: 'schedule', label: '모집 일정' },
]

// API에서 비어있을 때 보여줄 기본값 (모두 빈 상태로 시작)
const FALLBACK = {
  hanwhaAssets: [],
  partnerRoles: [],
  customerTags: [],
  partnerValues: [],
  timelineEvents: [],
  submissionDocs: [],
  attachedFiles: [],
  contactInfo: { name: '', team: '', email: '', phone: '' },
  heroKpis: [],
  targetSegment: '',
  targetScale: '',
  submissionInfo: { name: '', desc: '', limit: '' },
}

const ICON_SUGGESTIONS = [
  'bullseye', 'megaphone', 'crown', 'hotel', 'fork-and-knife-with-plate',
  'airplane', 'shopping-bags', 'play-button', 'wrapped-gift', 'credit-card',
  'mobile-phone', 'chart-increasing', 'star', 'handshake', 'money-bag',
  'bar-chart', 'trophy', 'ticket', 'office-building', 'framed-picture',
  'busts-in-silhouette', 'loudspeaker', 'rocket', 'sparkles',
]

const legend = [
  { label: '안내/설명회', color: '#60A5FA' },
  { label: '제출 마감', color: '#FBBF24' },
  { label: '심사/발표', color: '#C084FC' },
  { label: '운영 시작', color: '#34D399' },
]

const faqList = [
  { q: '제안서 양식이 따로 있나요?', a: '담당자에게 요청하시면 표준 양식 PDF를 보내드립니다. 자유 양식도 허용되지만 비교 평가의 편의를 위해 권장합니다.' },
  { q: '한 회사가 여러 영역에 동시 지원할 수 있나요?', a: '가능합니다. 단, 영역별로 별도 제안서를 제출해야 하며 각 영역의 매칭 가중치가 따로 평가됩니다.' },
  { q: '심사 결과는 어떻게 통보되나요?', a: '1차 서류 심사는 등록 이메일로, 2차 발표 심사는 별도 일정 안내 후 최종 결과는 공식 메일로 통보됩니다.' },
  { q: '계약 후 캠페인 일정 조정이 가능한가요?', a: '킥오프 미팅에서 양 측 합의 하에 조정 가능하나 한화 측 마케팅 일정상 큰 폭 변경은 어려울 수 있습니다.' },
  { q: '비용 부담은 어떻게 나누나요?', a: '기본 운영비는 한화에서, 파트너사 측의 단독 혜택/굿즈는 파트너 부담입니다. 세부 사항은 제안서 검토 후 협의합니다.' },
]
const openFaqs = ref(new Set())
function toggleFaq(i) {
  if (openFaqs.value.has(i)) openFaqs.value.delete(i)
  else openFaqs.value.add(i)
  openFaqs.value = new Set(openFaqs.value)
}


// Campaign 기본 필드 매핑
const campaignName = computed(() => introData.value?.campaignName ?? '캠페인 소개')
const campaignStatus = computed(() => introData.value?.campaignStatus ?? '준비중')
const ownerLoginId = computed(() => introData.value?.ownerLoginId ?? '미지정')
const ownerName = computed(() => introData.value?.ownerName ?? null)
const ownerEmail = computed(() => introData.value?.ownerEmail ?? null)
const ownerDepartment = computed(() => introData.value?.ownerDepartment ?? null)
// 표시용 — 이름이 있으면 "이름 (부서)" 또는 "이름", 없으면 loginId fallback
const ownerDisplay = computed(() => {
  if (ownerName.value) {
    return ownerDepartment.value
      ? `${ownerName.value} (${ownerDepartment.value})`
      : ownerName.value
  }
  return ownerLoginId.value
})
const rfpCode = computed(() => introData.value?.rfpCode ?? '')
const recruitDeadline = computed(() => introData.value?.recruitDeadline ?? null)

// JSON 필드 (없으면 fallback)
function pickList(value, fallback) {
  if (Array.isArray(value)) return value
  if (value && Array.isArray(value.list)) return value.list
  return fallback
}
const hanwhaAssets = computed(() => pickList(introData.value?.hanwhaAssets, FALLBACK.hanwhaAssets))
const partnerRoles = computed(() => pickList(introData.value?.partnerRoles, FALLBACK.partnerRoles))
const customerTags = computed(() => pickList(introData.value?.customerTags, FALLBACK.customerTags))
const partnerValues = computed(() => pickList(introData.value?.partnerValues, FALLBACK.partnerValues))
const timelineEvents = computed(() => pickList(introData.value?.timelineEvents, FALLBACK.timelineEvents))
const submissionDocs = computed(() => pickList(introData.value?.submissionDocs, FALLBACK.submissionDocs))
const attachedFiles = computed(() => pickList(introData.value?.attachedFiles, FALLBACK.attachedFiles))
const heroKpis = computed(() => pickList(introData.value?.heroKpis, FALLBACK.heroKpis))

// 캠페인 생성 시 입력한 정보 (읽기 전용, 개요 그리드에 표시)
const primaryGoal = computed(() => introData.value?.primaryGoal ?? null)
const assetName = computed(() => introData.value?.assetName ?? null)
const campaignMethods = computed(() => introData.value?.campaignMethods ?? null)
const campaignStartDate = computed(() => introData.value?.campaignStartDate ?? null)

// 누적 조회 수 (PM 팀 자기 페이지 조회는 백엔드에서 제외)
const viewCount = computed(() => introData.value?.viewCount ?? 0)

// 마감 진행률 — 캠페인 시작일 ~ 모집 마감일 기준
const deadlineProgress = computed(() => {
  if (!recruitDeadline.value) return null
  const end = new Date(recruitDeadline.value)
  if (Number.isNaN(end.getTime())) return null
  const now = new Date()
  const start = campaignStartDate.value
    ? new Date(campaignStartDate.value)
    : new Date(end.getTime() - 60 * 86400000)
  const total = end - start
  if (total <= 0) return null
  const elapsed = now - start
  return Math.max(0, Math.min(100, Math.round((elapsed / total) * 100)))
})
// 담당자 문의 칸 — 사용자가 입력한 contactInfo 우선, 비어있는 필드는 캠페인 생성자(owner) 정보로 자동 fallback
const contactInfo = computed(() => {
  const raw = introData.value?.contactInfo ?? {}
  return {
    name:  raw.name  || ownerName.value  || ownerLoginId.value || FALLBACK.contactInfo.name,
    team:  raw.team  || ownerDepartment.value || FALLBACK.contactInfo.team,
    email: raw.email || ownerEmail.value || FALLBACK.contactInfo.email,
    phone: raw.phone || FALLBACK.contactInfo.phone,
  }
})

// 캠페인 개요 그리드 — 저장된 customItems 우선, 없으면 Campaign 기본 필드 + 생성 시 입력 정보
const targetSegment = computed(() => introData.value?.targetSegment ?? FALLBACK.targetSegment)
const targetScale = computed(() => introData.value?.targetScale ?? FALLBACK.targetScale)

// 타깃 고객 항목 — 저장된 customerItems 우선, 없으면 segment/scale/tags로 초기 시드
const customerItems = computed(() => {
  const raw = introData.value?.customerItems
  if (Array.isArray(raw) && raw.length > 0) return raw
  if (raw && Array.isArray(raw.list) && raw.list.length > 0) return raw.list
  const items = []
  if (targetSegment.value) {
    items.push({ icon: 'bullseye', title: '핵심 세그먼트', desc: targetSegment.value })
  }
  if (targetScale.value) {
    items.push({ icon: 'busts-in-silhouette', title: '예상 모객 규모', desc: targetScale.value })
  }
  if (customerTags.value?.length) {
    items.push({ icon: 'label', title: '관심사 / 성향', desc: customerTags.value.join(', ') })
  }
  return items
})

const submissionInfo = computed(() => ({
  name:  introData.value?.submissionInfo?.name  ?? FALLBACK.submissionInfo.name,
  desc:  introData.value?.submissionInfo?.desc  ?? FALLBACK.submissionInfo.desc,
  limit: introData.value?.submissionInfo?.limit ?? FALLBACK.submissionInfo.limit,
}))

// 뷰 모드에서 보여줄 타임라인 — detail/docs 중첩 구조로 복원
const timelineEventsView = computed(() => {
  const raw = pickList(introData.value?.timelineEvents, FALLBACK.timelineEvents)
  return raw.map(ev => ({
    ...ev,
    displayDate: ev.date || '미정',
    detail: ev.detail ?? ((ev.detailText || ev.detailMethod)
      ? { method: ev.detailMethod, text: ev.detailText }
      : null),
    docs: ev.docs ?? (ev.docsText ? ev.docsText.split('\n').filter(Boolean) : []),
  }))
})

// 매칭 5축 weight (사이드바)
const matchWeights = computed(() => [
  { label: '고객 적합도', value: introData.value?.weightCustomer },
  { label: '수익 효과', value: introData.value?.weightRevenue },
  { label: '비용 구조', value: introData.value?.weightCost },
  { label: '운영 부담', value: introData.value?.weightOperation },
  { label: '브랜드 적합', value: introData.value?.weightBrand },
])
const hasAnyWeight = computed(() =>
  matchWeights.value.some(w => w.value != null && w.value > 0)
)

// 헬퍼
function statusToTone(status) {
  if (!status) return 'info'
  if (['recruiting', '모집중', 'active', 'in_progress'].includes(status)) return 'success'
  if (['closed', '종료', 'completed'].includes(status)) return 'muted'
  return 'info'
}

function statusToLabel(status) {
  const map = {
    draft: '초안',
    in_progress: '진행중',
    recruiting: '모집중',
    active: '진행중',
    closed: '종료',
    completed: '완료',
    planned: '예정',
    at_risk: '위험',
    review: '검토중',
  }
  return map[status] || status || '준비중'
}
function formatDate(dt) {
  if (!dt) return '미정'
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return '미정'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}.${m}.${day}`
}
function computeDday(dt) {
  if (!dt) return '-'
  const target = new Date(dt)
  if (Number.isNaN(target.getTime())) return '-'
  const diff = Math.ceil((target - new Date()) / 86400000)
  return diff >= 0 ? `D-${diff}` : `D+${-diff}`
}
function goToProposal() {
  router.push({ name: 'campaign-proposal-new', params: { campaignId: route.params.campaignId } })
}

// ─── 편집 헬퍼 ───────────────────────────────────────────────────
function addAsset() { editDraft.value.hanwhaAssets.push({ icon: 'sparkles', title: '', desc: '' }) }
function removeAsset(i) { editDraft.value.hanwhaAssets.splice(i, 1) }
function addRole() { editDraft.value.partnerRoles.push({ icon: 'handshake', title: '', desc: '' }) }
function removeRole(i) { editDraft.value.partnerRoles.splice(i, 1) }
function addTag() { editDraft.value.customerTags.push('') }
function removeTag(i) { editDraft.value.customerTags.splice(i, 1) }
function addValue() { editDraft.value.partnerValues.push({ icon: 'bar-chart', tone: 'primary', title: '', desc: '' }) }
function removeValue(i) { editDraft.value.partnerValues.splice(i, 1) }
function addCustomer() { editDraft.value.customerItems.push({ icon: 'sparkles', title: '', desc: '' }) }
function removeCustomer(i) { editDraft.value.customerItems.splice(i, 1) }
function addEvent() {
  editDraft.value.timelineEvents.push({
    id: Date.now(), color: 'gray', done: false, urgent: false,
    title: '', date: '', tag: '', tagColor: '',
    detailMethod: '', detailText: '', docsText: '', note: '',
  })
}
function removeEvent(i) { editDraft.value.timelineEvents.splice(i, 1) }
function addDoc() { editDraft.value.submissionDocs.push({ icon: 'page-facing-up', color: '#3B82F6', required: false, label: '' }) }
function removeDoc(i) { editDraft.value.submissionDocs.splice(i, 1) }
function addKpi() { editDraft.value.heroKpis.push({ label: '', value: '' }) }
function removeKpi(i) { editDraft.value.heroKpis.splice(i, 1) }

function toggleIconPicker(key) {
  activeIconPicker.value = activeIconPicker.value === key ? null : key
}
function closeIconPicker() {
  activeIconPicker.value = null
}

// ─── 인라인 편집 함수 ─────────────────────────────────────────────
function enterEdit() {
  if (!canEdit.value) {
    window.alert('편집 권한이 없습니다. (PM 조직의 매니저/총괄 매니저만 가능)')
    return
  }
  const tlDraft = timelineEventsView.value.map(ev => ({
    id: ev.id,
    color: ev.color ?? 'gray',
    done: Boolean(ev.done),
    urgent: Boolean(ev.urgent),
    title: ev.title ?? '',
    date: ev.date ?? '',
    tag: ev.tag ?? '',
    tagColor: ev.tagColor ?? '',
    detailMethod: ev.detail?.method ?? '',
    detailText: ev.detail?.text ?? '',
    docsText: Array.isArray(ev.docs) ? ev.docs.join('\n') : '',
    note: ev.note ?? '',
  }))
  editDraft.value = {
    rfpCode: introData.value?.rfpCode ?? '',
    recruitDeadline: toDatetimeLocalValue(introData.value?.recruitDeadline),
    contactInfo: {
      name: introData.value?.contactInfo?.name ?? '',
      team: introData.value?.contactInfo?.team ?? '',
      email: introData.value?.contactInfo?.email ?? '',
      phone: introData.value?.contactInfo?.phone ?? '',
    },
    weightCustomer: introData.value?.weightCustomer ?? 0,
    weightRevenue: introData.value?.weightRevenue ?? 0,
    weightCost: introData.value?.weightCost ?? 0,
    weightOperation: introData.value?.weightOperation ?? 0,
    weightBrand: introData.value?.weightBrand ?? 0,
    heroKpis: JSON.parse(JSON.stringify(heroKpis.value)),
    hanwhaAssets: JSON.parse(JSON.stringify(hanwhaAssets.value)),
    partnerRoles: JSON.parse(JSON.stringify(partnerRoles.value)),
    customerTags: [...customerTags.value],
    customerItems: JSON.parse(JSON.stringify(customerItems.value)),
    targetSegment: targetSegment.value,
    targetScale: targetScale.value,
    partnerValues: JSON.parse(JSON.stringify(partnerValues.value)),
    timelineEvents: tlDraft,
    submissionDocs: JSON.parse(JSON.stringify(submissionDocs.value)),
    submissionInfo: { ...submissionInfo.value },
  }
  editMode.value = true
}

function cancelEdit() {
  editDraft.value = null
  editMode.value = false
  activeIconPicker.value = null
}

async function saveEdit() {
  if (!editDraft.value) return
  if (!canEdit.value) { errorMsg.value = '편집 권한이 없습니다.'; return }
  saving.value = true
  errorMsg.value = ''
  try {
    const tlPayload = editDraft.value.timelineEvents.map(ev => ({
      id: ev.id,
      color: ev.color,
      done: ev.done,
      urgent: ev.urgent,
      title: ev.title,
      date: ev.date || '미정',
      tag: ev.tag || undefined,
      tagColor: ev.tagColor || undefined,
      detail: (ev.detailMethod || ev.detailText)
        ? { method: ev.detailMethod || undefined, text: ev.detailText || undefined }
        : undefined,
      docs: ev.docsText ? ev.docsText.split('\n').filter(Boolean) : undefined,
      note: ev.note || undefined,
    }))
    const wrapList = (arr) => ({ list: Array.isArray(arr) ? arr : [] })
    const payload = {
      rfpCode: editDraft.value.rfpCode || null,
      recruitDeadline: editDraft.value.recruitDeadline
        ? new Date(editDraft.value.recruitDeadline).toISOString()
        : null,
      contactInfo: { ...editDraft.value.contactInfo },
      weightCustomer: Number(editDraft.value.weightCustomer) || null,
      weightRevenue:  Number(editDraft.value.weightRevenue)  || null,
      weightCost:     Number(editDraft.value.weightCost)     || null,
      weightOperation:Number(editDraft.value.weightOperation)|| null,
      weightBrand:    Number(editDraft.value.weightBrand)    || null,
      heroKpis:       wrapList(editDraft.value.heroKpis),
      hanwhaAssets:   wrapList(editDraft.value.hanwhaAssets),
      partnerRoles:   wrapList(editDraft.value.partnerRoles),
      customerTags:   wrapList(editDraft.value.customerTags),
      customerItems:  wrapList(editDraft.value.customerItems),
      targetSegment:  editDraft.value.targetSegment || null,
      targetScale:    editDraft.value.targetScale   || null,
      partnerValues:  wrapList(editDraft.value.partnerValues),
      timelineEvents: wrapList(tlPayload),
      submissionDocs: wrapList(editDraft.value.submissionDocs),
      submissionInfo: editDraft.value.submissionInfo,
    }
    await UpdateCampaignIntro(route.params.campaignId, payload)
    introData.value = await GetCampaignIntro(route.params.campaignId)
    editMode.value = false
    editDraft.value = null
  } catch (e) {
    errorMsg.value = e?.response?.data?.message ?? e?.message ?? '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

function toDatetimeLocalValue(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return ''
  // YYYY-MM-DDTHH:mm 형식 (datetime-local input 표준)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadIntro(campaignId) {
  if (!campaignId) return
  loading.value = true
  errorMsg.value = ''
  editMode.value = false
  editDraft.value = null
  try {
    introData.value = await GetCampaignIntro(campaignId)
    const status = introData.value?.campaignStatus ?? ''
    const isRecruiting = ['recruiting', '모집중'].includes(status)
    if (!isRecruiting && !introData.value?.canEdit) {
      router.replace({ name: 'campaign-detail', params: { campaignId } })
      return
    }
  } catch (e) {
    errorMsg.value = e?.message ?? '소개 페이지를 불러오지 못했습니다.'
    introData.value = null
  } finally {
    loading.value = false
  }
}

// 스크롤 진행률 (페이지 상단 progress bar)
const scrollProgress = ref(0)
function updateScrollProgress() {
  const max = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = max > 0 ? Math.min(100, (window.scrollY / max) * 100) : 0
}

onMounted(() => {
  loadIntro(route.params.campaignId)
  window.addEventListener('scroll', updateScrollProgress, { passive: true })
  updateScrollProgress()
})
onUnmounted(() => {
  window.removeEventListener('scroll', updateScrollProgress)
})

// 라우트 param 변경 시 자동 재요청 (component reuse 케이스)
watch(() => route.params.campaignId, (next) => {
  if (next) loadIntro(next)
})
</script>

<style scoped>
/* ─── Design Tokens (전역 테마와 연결) ─────────────────────────── */
.op {
  --bg: var(--app-bg);
  --surface: var(--panel-color);
  --surface-muted: var(--panel-muted);
  --border: var(--border-color);
  --border-mid: var(--border-strong);

  --text-1: var(--text-primary);
  --text-2: var(--text-secondary);
  --text-3: var(--muted-text);
  --text-4: var(--subtle-text);

  --primary: var(--color-primary-500);
  --primary-h: var(--color-primary-600);
  --primary-s: var(--color-primary-100);
  --primary-m: var(--color-primary-200);

  --success: var(--color-success);
  --success-s: var(--color-success-light);
  --success-t: var(--color-success-dark);
  --warning: var(--color-warning);
  --warning-s: var(--color-warning-light);
  --warning-t: var(--color-warning-dark);
  --danger: var(--color-danger);
  --danger-s: var(--color-danger-light);
  --danger-t: var(--color-danger-dark);
  --info: var(--color-info);
  --info-s: var(--color-info-light);
  --info-t: var(--color-info-dark);
  --purple: var(--color-primary-400);
  --purple-s: var(--color-primary-50);
  --purple-t: var(--color-primary-700);
  --emerald: var(--color-success);
  --emerald-s: var(--color-success-light);
  --emerald-t: var(--color-success-dark);

  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;

  --shadow-sm: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.08);

  font-family: 'Pretendard', 'Noto Sans KR', -apple-system, sans-serif;
  background: var(--bg);
  color: var(--text-1);
  padding: 32px 40px 80px;
  min-height: 100vh;
}

/* ─── Skeleton Loader (initial load) ─────────────────────────── */
.skeleton-stack {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-top: 8px;
}
.skeleton {
  background: linear-gradient(
    90deg,
    var(--surface-muted) 0%,
    var(--border) 50%,
    var(--surface-muted) 100%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
  border-radius: var(--radius-md);
}
.skeleton--hero { height: 110px; }
.skeleton--card { flex: 1; height: 90px; }
.skeleton--kpi { height: 96px; }
.skeleton--block { height: 320px; }
.skeleton-strip {
  display: flex;
  gap: 12px;
}
@keyframes skeleton-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ─── Reading progress bar (페이지 상단) ──────────────────────── */
.reading-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary), var(--color-primary-400, #A78BFA));
  z-index: 1000;
  transition: width 0.1s linear;
  border-radius: 0 2px 2px 0;
  pointer-events: none;
}

/* ─── Breadcrumb ─────────────────────────────────────────────── */
.op-breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 24px;
  font-size: 13px;
}
.op-breadcrumb__item {
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.15s;
}
.op-breadcrumb__item:hover { color: var(--text-1); }
.op-breadcrumb__item--current {
  color: var(--text-1);
  font-weight: 600;
  cursor: default;
}
.op-breadcrumb__item--link {
  text-decoration: none;
  color: var(--text-3);
}
.op-breadcrumb__item--link:hover { color: var(--text-1); }

.op-hero__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.op-hero__title-row .op-hero__title {
  margin-bottom: 0;
  flex: 1;
  min-width: 0;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-color);
  background: var(--surface-1, transparent);
  color: var(--text-secondary);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.btn-back:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}
.op-breadcrumb__sep { color: var(--text-4); font-size: 11px; }

/* ─── Hero ───────────────────────────────────────────────────── */
.op-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: 32px;
}
.op-hero__left { flex: 1; }
.op-hero__badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.op-hero__title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-1);
  letter-spacing: -0.03em;
  line-height: 1.3;
  margin-bottom: 14px;
}
.op-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: var(--text-3);
}
.op-hero__meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.op-hero__meta i,
.op-hero__meta iconify-icon { font-size: 15px; color: var(--text-4); }
.op-hero__meta-sub {
  margin-left: 4px;
  font-size: 11px; font-weight: 600;
  color: var(--text-3);
  font-style: normal;
}

.op-hero__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
  min-width: 320px;
}
.op-hero__actions {
  display: flex;
  gap: 8px;
  width: 100%;
}

/* Deadline Box */
.deadline-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  padding: 14px 18px;
  background: var(--primary-s);
  border: 1px solid var(--primary-m);
  border-radius: var(--radius-md);
}
.deadline-box__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.deadline-box__label {
  font-size: 11px;
  font-weight: 600;
  color: var(--primary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 3px;
}
.deadline-box__date {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}
.deadline-box__dday {
  font-size: 28px;
  font-weight: 900;
  color: var(--primary);
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}
.deadline-progress__track {
  height: 6px;
  background: var(--primary-m);
  border-radius: 99px;
  overflow: hidden;
}
.deadline-progress__fill {
  height: 100%;
  background: var(--primary);
  border-radius: 99px;
  transition: width 0.4s ease;
}
.deadline-progress__labels {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: var(--primary);
  opacity: 0.7;
  margin-top: 3px;
  font-variant-numeric: tabular-nums;
}
.deadline-progress__pct { font-weight: 700; }
.deadline-box__social {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px dashed var(--primary-m);
  font-size: 11px;
  color: var(--primary);
  font-weight: 600;
}
.deadline-box__social strong { font-weight: 800; }
.deadline-box__social iconify-icon { font-size: 14px; }
.deadline-box__social-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--success, #22C55E);
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.2);
  animation: pulse-dot 1.6s ease-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.45; }
}

/* ─── Dark mode 보정 (deadline-box 밝은 보라 → 차분한 톤) ────── */
:root[data-theme='dark'] .deadline-box {
  background: rgba(139, 92, 246, 0.10);
  border-color: rgba(139, 92, 246, 0.26);
}
:root[data-theme='dark'] .deadline-box__label,
:root[data-theme='dark'] .deadline-box__dday,
:root[data-theme='dark'] .deadline-progress__labels,
:root[data-theme='dark'] .deadline-box__social {
  color: #c4b5fd;
}
:root[data-theme='dark'] .deadline-box__date {
  color: var(--text-1);
}
:root[data-theme='dark'] .deadline-progress__track {
  background: rgba(139, 92, 246, 0.18);
}
:root[data-theme='dark'] .deadline-progress__fill {
  background: #a78bfa;
}
:root[data-theme='dark'] .deadline-box__social {
  border-top-color: rgba(139, 92, 246, 0.22);
}

/* KEY METRIC 카드: 라이트 라벤더 → 다크 네이비 그라데이션이 어색 → 부드러운 보라 톤으로 */
:root[data-theme='dark'] .kpi-card:first-child {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.18) 0%, var(--surface) 100%);
  border-color: rgba(139, 92, 246, 0.30);
}
:root[data-theme='dark'] .kpi-card:first-child::before {
  color: #c4b5fd;
}
:root[data-theme='dark'] .kpi-card__value {
  color: #c4b5fd;
}
:root[data-theme='dark'] .kpi-card--add:hover {
  background: rgba(139, 92, 246, 0.12);
  border-color: rgba(139, 92, 246, 0.45);
}

/* ─── Campaign Essentials (주 목표 / 자산명 / 캠페인 방식) ─────── */
.essentials-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.essentials-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-left: 3px solid var(--primary);
  border-radius: var(--radius-md);
  padding: 14px 18px;
  box-shadow: var(--shadow-sm);
}
.essentials-card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 6px;
}
.essentials-card__head iconify-icon { font-size: 16px; }
.essentials-card__value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  line-height: 1.45;
}
.essentials-card__value--tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.essentials-tag {
  display: inline-block;
  padding: 2px 9px;
  background: var(--primary-s);
  color: var(--primary);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

/* ─── Hero KPI Strip (Bento Grid) ─────────────────────────────── */
.kpi-strip {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}
.kpi-card {
  flex: 1 1 140px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  text-align: center;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.15s, transform 0.15s;
  position: relative;
}
.kpi-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.kpi-card:first-child {
  flex: 2 1 280px;
  background: linear-gradient(135deg, var(--primary-s) 0%, var(--surface) 100%);
  border-color: var(--primary-m);
}
.kpi-card:first-child::before {
  content: '⭐ KEY METRIC';
  position: absolute;
  top: 8px;
  left: 12px;
  font-size: 9px;
  font-weight: 800;
  color: var(--primary);
  letter-spacing: 0.08em;
}
.kpi-card__value {
  font-size: 26px;
  font-weight: 900;
  color: var(--primary);
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  margin-bottom: 4px;
}
.kpi-card:first-child .kpi-card__value {
  font-size: 38px;
  margin-top: 6px;
}
.kpi-card__label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ─── Badges ─────────────────────────────────────────────────── */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}
.badge--success {
  background: var(--success-s);
  color: var(--success-t);
  border-color: #6EE7B7;
}
.badge--info {
  background: var(--info-s);
  color: var(--info-t);
  border-color: #93C5FD;
}
.badge--muted {
  background: var(--surface-muted);
  color: var(--text-3);
  border-color: var(--border);
}
.op-empty-hint {
  color: var(--text-3);
  font-size: 13px;
  padding: 8px 0;
  text-align: center;
}


/* P1 — 내부 전용 영역 표시 칩 (심사 가중치 등) */
.op-internal-tag {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 7px;
  font-size: 10px; font-weight: 800;
  letter-spacing: 0.04em;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.10);
  color: #B42C2C;
  border: 1px solid rgba(220, 38, 38, 0.28);
  vertical-align: middle;
}
:root[data-theme='dark'] .op-internal-tag {
  background: rgba(248, 113, 113, 0.16);
  color: #FCA5A5;
  border-color: rgba(248, 113, 113, 0.32);
}

/* ─── Topbar (Breadcrumb + Edit toggle) ──────────────────────── */
.op-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.op-topbar .op-breadcrumb { margin-bottom: 0; }
.op-topbar__actions {
  display: flex;
  gap: 8px;
}

.op-error-banner {
  margin-bottom: 16px;
  padding: 10px 14px;
  border: 1px solid var(--danger);
  background: var(--danger-s);
  color: var(--danger-t);
  border-radius: var(--radius-md);
  font-size: 13px;
}

/* ─── Inline Edit Inputs ──────────────────────────────────────── */
.op-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  color: var(--text-1);
  font-size: 13px;
  font-family: inherit;
  transition: border-color 0.15s;
}
.op-input:focus {
  outline: none;
  border-color: var(--primary);
}
.op-input--inline {
  width: auto;
  min-width: 220px;
  padding: 4px 8px;
  font-size: 12px;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}
.op-input--datetime {
  width: 100%;
}

.op-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}
.op-field__label {
  font-size: 12px;
  color: var(--text-3);
  font-weight: 600;
}

.criteria-edit,
.contact-edit {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.btn--sm {
  min-height: 30px;
  padding: 0 12px;
  font-size: 12px;
}
.op-rfp {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
  color: var(--text-3);
  background: var(--surface-muted);
  border: 1px solid var(--border);
  padding: 2px 8px;
  border-radius: 4px;
}
.trust-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  font-size: 11px;
  font-weight: 700;
  color: #15803D;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 999px;
  letter-spacing: -0.01em;
}
.trust-badge iconify-icon { font-size: 13px; }
:root[data-theme='dark'] .trust-badge {
  color: #86EFAC;
  background: rgba(74, 222, 128, 0.16);
  border-color: rgba(74, 222, 128, 0.32);
}

/* ─── Buttons ────────────────────────────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  white-space: nowrap;
  border: 1px solid transparent;
}
.btn--primary {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 1px 4px rgba(139,92,246,0.25);
}
.btn--primary:hover { background: var(--primary-h); }
.btn--ghost {
  background: var(--surface);
  color: var(--text-2);
  border-color: var(--border);
}
.btn--ghost:hover { background: var(--surface-muted); }
.btn--block { width: 100%; justify-content: center; padding: 11px 14px; font-size: 14px; }
.btn--sm { padding: 6px 12px; font-size: 12px; }
.btn-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}
.btn-link:hover { color: var(--primary-h); }
.btn-link-sm {
  font-size: 12px;
  color: var(--text-3);
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
  transition: color 0.15s;
}
.btn-link-sm:hover { color: var(--text-1); }

/* ─── Tab Nav ────────────────────────────────────────────────── */
.op-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--border);
  margin-bottom: 32px;
  background: var(--bg);
  padding-top: 4px;
}
.op-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: none;
  border: none;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-3);
  cursor: pointer;
  transition: color 0.15s;
}
.op-tab::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: transparent;
  transition: background 0.15s;
}
.op-tab--active { color: var(--primary); }
.op-tab--active::after { background: var(--primary); }
.op-tab__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--border);
  color: var(--text-2);
  font-size: 11px;
  font-weight: 700;
  border-radius: 9px;
}
.op-tab--active .op-tab__badge {
  background: var(--primary-m);
  color: var(--primary);
}

/* ─── Body Layout ────────────────────────────────────────────── */
.op-body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 28px;
  align-items: start;
}
.op-main { display: flex; flex-direction: column; gap: 24px; min-width: 0; }
.op-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 52px;
}

/* ─── Cards ──────────────────────────────────────────────────── */
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 28px 28px 24px;
  box-shadow: var(--shadow-sm);
}
.card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 20px;
  letter-spacing: -0.01em;
}
.card__title i,
.card__title iconify-icon { font-size: 20px; }
.card__title-sm {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 16px;
}
.card__title-sm i,
.card__title-sm iconify-icon { font-size: 16px; }
.card__sub { font-size: 13px; color: var(--text-3); margin-top: -14px; margin-bottom: 20px; line-height: 1.5; }


/* CTA card */
.card--cta {
  border-color: var(--primary-m);
  background: var(--surface);
}

/* ─── 통합 정보 그리드 (한화 자산 / 파트너 역할 / 타깃 / 가치) ── */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}
.info-card {
  display: flex;
  flex-direction: column;
}
.info-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 14px;
}
.info-card__head .card__title { margin-bottom: 0; }
.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}
.info-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.info-item--editable {
  position: relative;
  border-radius: var(--radius-md);
  padding: 6px;
  margin: -6px;
  transition: background 0.12s;
}
.info-item--editable:hover { background: rgba(99, 102, 241, 0.04); }
.info-item__icon-wrap {
  position: relative;
  flex-shrink: 0;
}
.info-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  font-size: 20px;
  margin-top: 1px;
}
.info-item__icon iconify-icon { font-size: 20px; }
.info-item__icon--clickable {
  cursor: pointer;
  transition: transform 0.12s, box-shadow 0.12s;
}
.info-item__icon--clickable:hover {
  transform: scale(1.08);
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.info-item__body {
  flex: 1;
  min-width: 0;
}
.info-item__title {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 3px;
  line-height: 1.35;
}
.info-item__desc {
  font-size: 12.5px;
  color: var(--text-3);
  line-height: 1.6;
  margin: 0;
}
@media (max-width: 900px) {
  .info-grid { grid-template-columns: 1fr; }
}

/* ─── Overview Grid ──────────────────────────────────────────── */
.op-lead {
  font-size: 14px;
  line-height: 1.75;
  color: var(--text-2);
  margin-bottom: 24px;
}
.overview-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.overview-cell {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
}
.overview-cell__label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-4);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 5px;
}
.overview-cell__value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
  line-height: 1.4;
}

.tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 2px; }

.tag--persona {
  background: var(--surface);
  border: 1px solid var(--primary-m);
  color: var(--primary);
  font-weight: 600;
}
.tag {
  padding: 3px 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-2);
}

/* ─── FAQ Accordion ──────────────────────────────────────────── */
.faq-list { list-style: none; padding: 0; margin: 8px 0 0; display: flex; flex-direction: column; gap: 8px; }
.faq-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--surface);
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.faq-item--open { border-color: var(--primary-m); box-shadow: 0 2px 8px rgba(99,102,241,0.08); }
.faq-question {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 14px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font: inherit;
}
.faq-question:hover { background: var(--surface-muted); }
.faq-question__q {
  font-size: 14px;
  font-weight: 900;
  color: var(--primary);
  flex-shrink: 0;
  margin-top: 1px;
}
.faq-question__text {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  line-height: 1.5;
}
.faq-caret {
  flex-shrink: 0;
  font-size: 14px;
  transition: transform 0.2s;
}
.faq-item--open .faq-caret { transform: rotate(180deg); }
.faq-answer {
  display: flex;
  gap: 10px;
  padding: 0 16px 14px;
  border-top: 1px dashed var(--border);
  padding-top: 12px;
}
.faq-answer__a {
  font-size: 14px;
  font-weight: 900;
  color: var(--text-4);
  flex-shrink: 0;
}
.faq-answer p {
  margin: 0;
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.7;
}

/* ─── Timeline ───────────────────────────────────────────────── */
.tl-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}
.tl-legend { display: flex; gap: 14px; flex-shrink: 0; margin-top: 4px; }
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-3);
}
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }

.tl-track {
  display: flex;
  flex-direction: column;
  padding-left: 16px;
  border-left: 2px solid var(--border);
  gap: 0;
}
.tl-item {
  display: flex;
  gap: 20px;
  padding-bottom: 32px;
  position: relative;
}
.tl-item:last-child { padding-bottom: 0; }
.tl-item--done { opacity: 0.55; }

.tl-node-col {
  position: relative;
  flex-shrink: 0;
  margin-left: -27px;
}
.tl-node {
  position: relative;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2.5px solid var(--border-mid);
  background: var(--surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  color: var(--text-3);
  z-index: 1;
  margin-top: 2px;
}
.tl-node--gray  { border-color: #9CA3AF; background: #F9FAFB; }
.tl-node--blue  { border-color: #60A5FA; }
.tl-node--yellow { border-color: #FBBF24; }
.tl-node--purple { border-color: #C084FC; }
.tl-node--emerald { border-color: #34D399; }

.tl-item--urgent .tl-node {
  border-color: var(--warning);
  background: var(--warning);
  width: 22px;
  height: 22px;
  margin-top: 1px;
  box-shadow: 0 0 0 4px rgba(245,158,11,0.15);
}
.tl-pulse {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 2px solid var(--warning);
  animation: pulse-ring 1.5s ease-out infinite;
  pointer-events: none;
}
@keyframes pulse-ring {
  0%   { opacity: 0.8; transform: scale(1); }
  100% { opacity: 0; transform: scale(1.6); }
}

.tl-content { flex: 1; min-width: 0; }
.tl-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}
.tl-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  letter-spacing: -0.01em;
}
.tl-title--urgent { color: #92400E; }
.tl-date {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  padding: 3px 10px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}
.tl-date--gray   { background: var(--surface-muted); color: var(--text-3); border: 1px solid var(--border); }
.tl-date--blue   { background: var(--info-s);    color: var(--info);    border: 1px solid #BFDBFE; }
.tl-date--yellow { background: var(--warning-s); color: var(--warning-t); border: 1px solid #FDE68A; }
.tl-date--purple { background: var(--purple-s);  color: var(--purple-t); border: 1px solid #DDD6FE; }
.tl-date--emerald { background: var(--emerald-s); color: var(--emerald-t); border: 1px solid #A7F3D0; }

.tl-tag {
  display: inline-flex;
  align-items: center;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 3px;
  border: 1px solid transparent;
}
.tl-tag--blue   { background: var(--info-s);    color: var(--info-t);    border-color: #BFDBFE; }
.tl-tag--yellow { background: var(--warning-s); color: var(--warning-t); border-color: #FDE68A; }
.tl-tag--red    { background: var(--danger-s);  color: var(--danger-t);  border-color: #FECACA; }
.tl-tag--purple { background: var(--purple-s);  color: var(--purple-t);  border-color: #DDD6FE; }
.tl-tag--emerald { background: var(--emerald-s); color: var(--emerald-t); border-color: #A7F3D0; }

.tl-item--urgent .tl-content {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: var(--radius-md);
  padding: 14px 16px;
  margin-top: -2px;
}
.tl-detail {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 8px;
}
.tl-detail__method {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 6px;
}
.tl-detail__text { font-size: 12px; color: var(--text-3); line-height: 1.6; margin: 0; }
.tl-docs {
  background: #FFFBEB;
  border: 1px solid #FDE68A;
  border-radius: var(--radius-md);
  padding: 12px 14px;
  margin-top: 8px;
}
.tl-item--urgent .tl-docs {
  background: rgba(255,255,255,0.6);
  border-color: #FCD34D;
}
.tl-docs__title { font-size: 12px; font-weight: 700; color: var(--text-2); margin-bottom: 6px; }
.tl-docs ul { list-style: disc; padding-left: 16px; margin: 0; }
.tl-docs li { font-size: 12px; color: var(--text-3); line-height: 1.7; }
.tl-note { font-size: 13px; color: var(--text-3); margin-top: 6px; line-height: 1.55; }

/* ─── Submission ─────────────────────────────────────────────── */
.submission-box {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.submission-box__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
}
.submission-box__name { font-size: 14px; font-weight: 700; color: var(--text-1); margin-bottom: 3px; }
.submission-box__desc { font-size: 12px; color: var(--text-3); }
.submission-box__limit { font-size: 12px; color: var(--text-3); white-space: nowrap; margin-top: 2px; }
.submission-docs__title {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-2);
  padding: 14px 20px 10px;
}
.submission-docs { list-style: none; padding: 0 12px 12px; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.submission-doc {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--surface);
  border-radius: var(--radius-sm);
}
.submission-doc i,
.submission-doc iconify-icon { font-size: 18px; flex-shrink: 0; }
.submission-doc__label { font-size: 13px; color: var(--text-2); flex: 1; }
.req-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 3px;
  background: var(--danger-s);
  color: var(--danger-t);
  flex-shrink: 0;
}
.req-badge--opt { background: var(--surface-muted); color: var(--text-3); }

/* ─── Q&A ────────────────────────────────────────────────────── */
.qa-hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.qa-hd .card__title { margin-bottom: 0; }
.qa-list { display: flex; flex-direction: column; gap: 8px; }
.qa-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.qa-q {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 16px;
  background: var(--surface-muted);
  cursor: pointer;
  transition: background 0.12s;
  user-select: none;
}
.qa-q:hover { background: var(--border); background: #F3F4F6; }
.qa-q__mark {
  font-size: 14px;
  font-weight: 900;
  color: var(--primary);
  flex-shrink: 0;
  margin-top: 1px;
}
.qa-q__body { flex: 1; min-width: 0; }
.qa-q__meta { display: flex; align-items: center; gap: 8px; margin-bottom: 5px; }
.qa-status {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 3px;
}
.qa-status--done { background: var(--success-s); color: var(--success-t); }
.qa-status--wait { background: var(--surface); border: 1px solid var(--border); color: var(--text-3); }
.qa-from { font-size: 11px; color: var(--text-4); }
.qa-q__text { font-size: 13px; font-weight: 600; color: var(--text-1); line-height: 1.5; }
.qa-caret {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-4);
  margin-top: 3px;
  transition: transform 0.2s;
}
.qa-caret--open { transform: rotate(-180deg); }
.qa-a {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  background: var(--surface);
  border-top: 1px solid var(--border);
}
.qa-a__mark {
  font-size: 14px;
  font-weight: 900;
  color: var(--text-4);
  flex-shrink: 0;
  margin-top: 1px;
}
.qa-a p { font-size: 13px; color: var(--text-2); line-height: 1.7; margin: 0; }
.qa-more { text-align: center; margin-top: 16px; }

/* Q&A Transition */
.qa-slide-enter-active, .qa-slide-leave-active { transition: all 0.2s ease; overflow: hidden; }
.qa-slide-enter-from, .qa-slide-leave-to { opacity: 0; max-height: 0; }
.qa-slide-enter-to, .qa-slide-leave-from { opacity: 1; max-height: 200px; }

/* ─── Sidebar: CTA ───────────────────────────────────────────── */
.cta-title { font-size: 15px; font-weight: 800; color: var(--text-1); margin-bottom: 6px; letter-spacing: -0.02em; }
.cta-desc { font-size: 12px; color: var(--text-3); margin-bottom: 14px; line-height: 1.5; }
.cta-sub { display: flex; gap: 8px; margin-top: 8px; }
.cta-sub .btn { flex: 1; justify-content: center; }

/* ─── Sidebar: Files ─────────────────────────────────────────── */
.file-list { display: flex; flex-direction: column; gap: 6px; }
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.12s, border-color 0.12s;
  text-decoration: none;
  color: inherit;
}
.file-item:hover { background: var(--surface-muted); }
.file-item:hover .file-name { color: var(--primary); }
.file-item--locked { opacity: 0.65; border-style: dashed; cursor: default; }
.file-item--locked:hover { background: transparent; }
.file-item--locked:hover .file-name { color: inherit; }
.file-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.file-icon--red   { background: var(--danger-s);   color: var(--danger); }
.file-icon--blue  { background: var(--info-s);     color: var(--info); }
.file-icon--green { background: var(--success-s);  color: var(--success); }
.file-icon--gray  { background: var(--surface-muted); color: var(--text-3); border: 1px solid var(--border); }
.file-info { flex: 1; min-width: 0; }
.file-name { font-size: 13px; font-weight: 600; color: var(--text-1); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; transition: color 0.12s; }
.file-size { font-size: 11px; color: var(--text-4); margin-top: 2px; }
.file-action { font-size: 16px; color: var(--text-4); flex-shrink: 0; }

/* ─── Sidebar: Criteria ──────────────────────────────────────── */
.criteria-list { display: flex; flex-direction: column; gap: 14px; }
.criteria-item {}
.criteria-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}
.criteria-label { font-size: 12px; color: var(--text-2); }
.criteria-pct { font-size: 13px; font-weight: 800; color: var(--text-1); font-variant-numeric: tabular-nums; }
.criteria-track {
  height: 6px;
  background: var(--primary-s);
  border-radius: 99px;
  overflow: hidden;
}
.criteria-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 99px;
  transition: width 0.4s ease;
}

/* ─── Sidebar: Contact ───────────────────────────────────────── */
.contact-card {
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
}
.contact-label {
  font-size: 10px;
  font-weight: 800;
  color: var(--text-4);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 12px;
}
.contact-person {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.contact-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--primary-s);
  border: 2px solid var(--primary-m);
  color: var(--primary);
  font-size: 14px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.contact-name { font-size: 14px; font-weight: 700; color: var(--text-1); }
.contact-team { font-size: 11px; color: var(--text-4); margin-top: 2px; }
.contact-info { display: flex; flex-direction: column; gap: 6px; }
.contact-info__row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--text-3);
}
.contact-info__row i,
.contact-info__row iconify-icon { font-size: 13px; }

/* ─── Edit mode shared ───────────────────────────────────────── */
.edit-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  gap: 8px;
}
.edit-section { }
.edit-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.edit-section__label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-3);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.edit-rows { display: flex; flex-direction: column; gap: 6px; }
.edit-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.op-input--area {
  height: auto;
  resize: vertical;
  padding-top: 7px;
  padding-bottom: 7px;
  line-height: 1.5;
}
.op-input--icon { width: 120px; flex-shrink: 0; font-size: 12px; }
.op-select {
  height: 34px;
  padding: 0 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-1);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  outline: none;
}
.op-select--sm { height: 28px; font-size: 12px; }
.op-check {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-2);
  cursor: pointer;
  white-space: nowrap;
}
.op-color-input {
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 2px;
  cursor: pointer;
  background: none;
  flex-shrink: 0;
}
.btn-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
  background: rgba(var(--primary-rgb, 99 102 241) / 0.08);
  border: 1px solid rgba(var(--primary-rgb, 99 102 241) / 0.2);
  color: var(--primary);
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.btn-add:hover { background: rgba(var(--primary-rgb, 99 102 241) / 0.15); }
.btn-del {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: rgba(239, 68, 68, 0.07);
  border: 1px solid rgba(239, 68, 68, 0.18);
  color: #EF4444;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.12s;
}
.btn-del:hover { background: rgba(239, 68, 68, 0.15); }

/* ─── 자산 편집 ──────────────────────────────────────────────── */
.asset-edit-list { display: flex; flex-direction: column; gap: 12px; }
.asset-edit-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 12px;
}
.icon-editor {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.asset-icon--sm {
  width: 28px !important;
  height: 28px !important;
  font-size: 13px !important;
  border-radius: 7px !important;
  flex-shrink: 0;
}
.icon-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 3px;
}
.icon-chip {
  width: 24px;
  height: 24px;
  border-radius: 5px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text-3);
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.1s;
}
.icon-chip:hover { background: var(--surface-muted); color: var(--text-1); }
.icon-chip--on { background: var(--primary-s); border-color: var(--primary); color: var(--primary); }

/* ─── 타깃 태그 편집 ─────────────────────────────────────────── */
.tag-edit-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.tag-edit-item { display: flex; align-items: center; gap: 3px; }

/* ─── 타임라인 편집 ──────────────────────────────────────────── */
.tl-header__right { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.tl-edit-list { display: flex; flex-direction: column; gap: 10px; }
.tl-edit-item {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.tl-edit-item__bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--surface-muted);
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
}
.tl-edit-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tl-edit-fields { padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.tl-edit-row2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

/* ─── 제출 안내 편집 ─────────────────────────────────────────── */
.submission-edit { display: flex; flex-direction: column; gap: 0; }
.doc-edit-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  border-radius: 8px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  margin-bottom: 6px;
  flex-wrap: wrap;
}

@media (max-width: 600px) {
  .tl-edit-row2 { grid-template-columns: 1fr; }
}

/* ─── WYSIWYG Ghost Inputs ─────────────────────────────────────── */
.ghost-input, .ghost-textarea {
  background: transparent;
  border: none;
  outline: none;
  font: inherit;
  color: inherit;
  padding: 2px 5px;
  margin: -2px -5px;
  border-radius: 4px;
  width: 100%;
  display: block;
  transition: background 0.12s, box-shadow 0.12s;
  line-height: inherit;
}
.ghost-input:hover, .ghost-textarea:hover {
  background: rgba(99, 102, 241, 0.06);
}
.ghost-input:focus, .ghost-textarea:focus {
  background: rgba(99, 102, 241, 0.08);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.22);
  outline: none;
}
.ghost-textarea { resize: vertical; min-height: 1.5em; }
.ghost-input--bold { font-weight: 700; }
.ghost-input--label {
  font-size: 11px !important;
  font-weight: 600 !important;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-4) !important;
}
.ghost-input--value { font-size: 14px !important; font-weight: 600 !important; }
.ghost-input--center { text-align: center; }
.ghost-input--date {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px !important;
  margin: 0 !important;
  border-radius: 4px;
  background: var(--surface-muted) !important;
  border: 1px solid var(--border) !important;
  white-space: nowrap;
  flex-shrink: 0;
  height: 28px;
  width: auto;
  cursor: pointer;
}
.ghost-input--date:focus {
  border-color: var(--primary) !important;
  background: var(--surface) !important;
}

/* ─── WYSIWYG Delete Buttons ───────────────────────────────────── */
.wysiwyg-del {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #EF4444;
  cursor: pointer;
  font-size: 10px;
  transition: background 0.12s;
  flex-shrink: 0;
}
.wysiwyg-del:hover { background: rgba(239, 68, 68, 0.18); }
.wysiwyg-del--inline {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}
.wysiwyg-del--cell {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.overview-cell--editable:hover .wysiwyg-del--cell { opacity: 1; }
.wysiwyg-del--kpi {
  position: absolute;
  top: 4px;
  right: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.kpi-card--editable { position: relative; }
.kpi-card--editable:hover .wysiwyg-del--kpi { opacity: 1; }

/* ─── Icon Picker Popup ──────────────────────────────────────────── */
.icon-picker-popup {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 200;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 8px;
  box-shadow: var(--shadow-md);
  width: 210px;
}

.overview-cell--editable { position: relative; }

/* ─── Tag WYSIWYG ────────────────────────────────────────────────── */
.tag--editable {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding-right: 5px;
}
.tag-ghost-input {
  background: transparent;
  border: none;
  outline: none;
  font: inherit;
  color: inherit;
  width: 68px;
  min-width: 30px;
  line-height: inherit;
}
.tag-del-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.12);
  border: none;
  color: #EF4444;
  cursor: pointer;
  font-size: 9px;
  padding: 0;
  flex-shrink: 0;
}
.tag--add {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  background: transparent;
  border: 1px dashed var(--border);
  color: var(--text-3);
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  transition: all 0.12s;
}
.tag--add:hover { border-color: var(--primary); color: var(--primary); }

/* ─── Tone Color Chips ───────────────────────────────────────────── */
.tone-chips { display: flex; gap: 5px; }
.tone-chip {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color 0.12s, transform 0.1s;
}
.tone-chip:hover { transform: scale(1.15); }
.tone-chip--on { border-color: var(--text-1) !important; }
.tone-chip--primary { background: var(--primary); }
.tone-chip--info { background: var(--info); }
.tone-chip--success { background: var(--success); }
.tone-chip--warning { background: var(--warning); }
.tone-chip--purple { background: #A855F7; }

/* ─── Timeline WYSIWYG Edit Bar ──────────────────────────────────── */
.tl-edit-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 7px 10px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  flex-wrap: wrap;
}
.tl-extra-details { flex: 1; min-width: 0; }
.tl-extra-summary {
  font-size: 11px;
  color: var(--primary);
  cursor: pointer;
  font-weight: 600;
  list-style: none;
  padding: 2px 0;
  user-select: none;
}
.tl-extra-summary::-webkit-details-marker { display: none; }
.tl-extra-body {
  padding: 10px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* ─── Submission WYSIWYG ─────────────────────────────────────────── */
.submission-doc--editable { flex-wrap: nowrap; gap: 6px; }
.submission-doc--add {
  cursor: pointer;
  border: 1px dashed var(--border);
  background: transparent;
  transition: all 0.12s;
}
.submission-doc--add:hover { border-color: var(--primary); background: var(--primary-s); }

/* ─── KPI strip add card ─────────────────────────────────────────── */
.kpi-card--add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  border-style: dashed;
  background: transparent;
  min-width: 90px;
  transition: all 0.12s;
}
.kpi-card--add:hover { border-color: var(--primary); background: var(--primary-s); }
</style>
