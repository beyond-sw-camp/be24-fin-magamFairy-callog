<template>
  <div class="op" @click="activeIconPicker = null">

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
          <i class="ph ph-pencil-simple"></i>편집
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

    <!-- Hero -->
    <header class="op-hero">
      <div class="op-hero__left">
        <div v-if="(!editMode && rfpCode) || editMode" class="op-hero__badges">
          <code v-if="!editMode && rfpCode" class="op-rfp">{{ rfpCode }}</code>
          <input
            v-if="editMode"
            v-model="editDraft.rfpCode"
            class="op-input op-input--inline"
            placeholder="RFP 코드 (예: RFP-2026-045)"
          />
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
            <i class="ph ph-user-circle"></i>담당: {{ ownerDisplay }}
            <em v-if="ownerEmail" class="op-hero__meta-sub">· {{ ownerEmail }}</em>
          </span>
          <span><i class="ph ph-eye"></i>공개 범위: 인증 사용자</span>
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
        </div>
      </div>
    </header>

    <!-- Hero KPI Strip (WYSIWYG) -->
    <div v-if="editMode || heroKpis.length" class="kpi-strip">
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
          <i class="ph ph-x"></i>
        </button>
      </div>
      <button v-if="editMode" type="button" class="kpi-card kpi-card--add" @click="addKpi">
        <i class="ph ph-plus" style="color:var(--primary);font-size:18px"></i>
        <span style="color:var(--text-3);font-size:11px">KPI 추가</span>
      </button>
    </div>

    <!-- Tab Nav -->
    <nav class="op-tabs" aria-label="페이지 섹션">
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
    <div class="op-body">

      <!-- Main Content -->
      <main class="op-main">

        <!-- ───── 상세 정보 탭 ───── -->
        <div v-show="activeTab === 'detail'">

        <!-- 캠페인 개요 (WYSIWYG) -->
        <section class="card">
          <div class="edit-card-head">
            <h2 class="card__title"><i class="ph ph-info"></i>캠페인 개요</h2>
            <button v-if="editMode" type="button" class="btn-add" @click="addOverviewItem">
              <i class="ph ph-plus"></i>항목 추가
            </button>
          </div>
          <p v-if="!editMode" class="op-lead">{{ campaignSummary }}</p>
          <textarea v-else v-model="editDraft.campaignSummary" class="op-lead ghost-textarea" rows="3" placeholder="캠페인 소개 요약 내용을 입력하세요..."></textarea>
          <div class="overview-grid">
            <div
              v-for="(item, i) in (editMode ? editDraft.overviewItems : overviewItems)"
              :key="i"
              class="overview-cell"
              :class="{'overview-cell--editable': editMode}"
            >
              <template v-if="editMode">
                <input v-model="item.label" class="overview-cell__label ghost-input ghost-input--label" placeholder="항목명" />
                <input v-model="item.value" class="overview-cell__value ghost-input ghost-input--value" placeholder="내용" />
                <button type="button" class="wysiwyg-del wysiwyg-del--cell" @click="removeOverviewItem(i)" title="삭제">
                  <i class="ph ph-x"></i>
                </button>
              </template>
              <template v-else>
                <div class="overview-cell__label">{{ item.label }}</div>
                <div class="overview-cell__value">{{ item.value }}</div>
              </template>
            </div>
          </div>
        </section>

        <!-- 제공 자산 / 기대 역할 (WYSIWYG) -->
        <div class="two-col">
          <section class="card">
            <div class="edit-card-head">
              <h2 class="card__title"><i class="ph ph-gift"></i>한화 제공 자산</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addAsset"><i class="ph ph-plus"></i>추가</button>
            </div>
            <ul class="asset-list">
              <li
                v-for="(a, i) in (editMode ? editDraft.hanwhaAssets : hanwhaAssets)"
                :key="i"
                class="asset-item"
                :class="{'asset-item--editable': editMode}"
              >
                <div style="position:relative;flex-shrink:0">
                  <span
                    class="asset-icon"
                    :class="{'asset-icon--clickable': editMode}"
                    @click.stop="editMode && (activeIconPicker = activeIconPicker === 'hanwha-'+i ? null : 'hanwha-'+i)"
                  ><i :class="`ph ph-${a.icon || 'star'}`"></i></span>
                  <div v-if="editMode && activeIconPicker === 'hanwha-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': a.icon === ic}"
                        @click="a.icon = ic; activeIconPicker = null" :title="ic">
                        <i :class="`ph ph-${ic}`"></i>
                      </button>
                    </div>
                  </div>
                </div>
                <div style="flex:1;min-width:0">
                  <input v-if="editMode" v-model="a.title" class="asset-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <strong v-else class="asset-item__title">{{ a.title }}</strong>
                  <textarea v-if="editMode" v-model="a.desc" class="asset-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="asset-item__desc">{{ a.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--float" @click="removeAsset(i)">
                  <i class="ph ph-trash"></i>
                </button>
              </li>
            </ul>
          </section>
          <section class="card">
            <div class="edit-card-head">
              <h2 class="card__title"><i class="ph ph-handshake"></i>파트너 기대 역할</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addRole"><i class="ph ph-plus"></i>추가</button>
            </div>
            <ul class="asset-list">
              <li
                v-for="(r, i) in (editMode ? editDraft.partnerRoles : partnerRoles)"
                :key="i"
                class="asset-item"
                :class="{'asset-item--editable': editMode}"
              >
                <div style="position:relative;flex-shrink:0">
                  <span
                    class="asset-icon"
                    :class="{'asset-icon--clickable': editMode}"
                    @click.stop="editMode && (activeIconPicker = activeIconPicker === 'role-'+i ? null : 'role-'+i)"
                  ><i :class="`ph ph-${r.icon || 'star'}`"></i></span>
                  <div v-if="editMode && activeIconPicker === 'role-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': r.icon === ic}"
                        @click="r.icon = ic; activeIconPicker = null" :title="ic">
                        <i :class="`ph ph-${ic}`"></i>
                      </button>
                    </div>
                  </div>
                </div>
                <div style="flex:1;min-width:0">
                  <input v-if="editMode" v-model="r.title" class="asset-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <strong v-else class="asset-item__title">{{ r.title }}</strong>
                  <textarea v-if="editMode" v-model="r.desc" class="asset-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="asset-item__desc">{{ r.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--float" @click="removeRole(i)">
                  <i class="ph ph-trash"></i>
                </button>
              </li>
            </ul>
          </section>
        </div>

        <!-- 타깃 고객 + 참여 가치 (WYSIWYG) -->
        <section class="card card--split">
          <div class="split-pane split-pane--l">
            <h2 class="card__title"><i class="ph ph-target"></i>타깃 고객 프로필</h2>
            <dl class="target-dl">
              <dt>핵심 세그먼트</dt>
              <dd>
                <span v-if="!editMode">{{ targetSegment }}</span>
                <input v-else v-model="editDraft.targetSegment" class="ghost-input" placeholder="핵심 세그먼트" />
              </dd>
              <dt>고객 성향 / 관심사</dt>
              <dd>
                <div class="tag-row">
                  <template v-if="!editMode">
                    <span v-for="t in customerTags" :key="t" class="tag">{{ t }}</span>
                  </template>
                  <template v-else>
                    <span v-for="(t, i) in editDraft.customerTags" :key="i" class="tag tag--editable">
                      <input v-model="editDraft.customerTags[i]" class="tag-ghost-input" placeholder="태그" />
                      <button type="button" class="tag-del-btn" @click="removeTag(i)"><i class="ph ph-x"></i></button>
                    </span>
                    <button type="button" class="tag tag--add" @click="addTag">
                      <i class="ph ph-plus"></i>추가
                    </button>
                  </template>
                </div>
              </dd>
              <dt>예상 모객 규모</dt>
              <dd>
                <span v-if="!editMode">{{ targetScale }}</span>
                <input v-else v-model="editDraft.targetScale" class="ghost-input" placeholder="예상 모객 규모" />
              </dd>
            </dl>
          </div>
          <div class="split-pane split-pane--r">
            <div class="edit-card-head">
              <h2 class="card__title"><i class="ph ph-trend-up"></i>파트너 참여 가치</h2>
              <button v-if="editMode" type="button" class="btn-add" @click="addValue"><i class="ph ph-plus"></i>추가</button>
            </div>
            <div class="value-list">
              <div
                v-for="(v, i) in (editMode ? editDraft.partnerValues : partnerValues)"
                :key="i"
                class="value-item"
                :class="{'value-item--editable': editMode}"
              >
                <div style="position:relative;flex-shrink:0">
                  <div
                    class="value-icon"
                    :class="[`value-icon--${v.tone}`, {'value-icon--clickable': editMode}]"
                    @click.stop="editMode && (activeIconPicker = activeIconPicker === 'value-'+i ? null : 'value-'+i)"
                  ><i :class="`ph ph-${v.icon || 'star'}`"></i></div>
                  <div v-if="editMode && activeIconPicker === 'value-'+i" class="icon-picker-popup" @click.stop>
                    <div class="icon-chips" style="margin-bottom:8px">
                      <button v-for="ic in ICON_SUGGESTIONS" :key="ic" type="button"
                        class="icon-chip" :class="{'icon-chip--on': v.icon === ic}"
                        @click="v.icon = ic" :title="ic">
                        <i :class="`ph ph-${ic}`"></i>
                      </button>
                    </div>
                    <div class="tone-chips">
                      <button v-for="tone in ['primary','info','success','warning','purple']" :key="tone"
                        type="button"
                        :class="['tone-chip', `tone-chip--${tone}`, {'tone-chip--on': v.tone === tone}]"
                        @click="v.tone = tone; activeIconPicker = null"></button>
                    </div>
                  </div>
                </div>
                <div style="flex:1;min-width:0">
                  <input v-if="editMode" v-model="v.title" class="value-item__title ghost-input ghost-input--bold" placeholder="제목" />
                  <h4 v-else class="value-item__title">{{ v.title }}</h4>
                  <textarea v-if="editMode" v-model="v.desc" class="value-item__desc ghost-textarea" rows="2" placeholder="설명"></textarea>
                  <p v-else class="value-item__desc">{{ v.desc }}</p>
                </div>
                <button v-if="editMode" type="button" class="wysiwyg-del wysiwyg-del--float" @click="removeValue(i)">
                  <i class="ph ph-trash"></i>
                </button>
              </div>
            </div>
          </div>
        </section>

        </div>
        <!-- ───── /상세 정보 탭 ───── -->

        <!-- ───── 모집 일정 탭 ───── -->
        <div v-show="activeTab === 'schedule'">

        <!-- 진행 일정 타임라인 (WYSIWYG) -->
        <section class="card">
          <div class="tl-header">
            <div>
              <h2 class="card__title"><i class="ph ph-calendar-blank"></i>진행 일정 및 타임라인</h2>
              <p class="card__sub">캠페인 런칭 전 주요 일정입니다. 마감 기한을 엄수해 주시기 바랍니다.</p>
            </div>
            <div class="tl-header__right">
              <div class="tl-legend">
                <span v-for="l in legend" :key="l.label" class="legend-item">
                  <span class="legend-dot" :style="{ background: l.color }"></span>{{ l.label }}
                </span>
              </div>
              <button v-if="editMode" type="button" class="btn-add" @click="addEvent"><i class="ph ph-plus"></i>일정 추가</button>
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
                      <i class="ph ph-video-camera"></i>{{ ev.detail.method }}
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
                    <button type="button" class="btn-del" @click="removeEvent(i)"><i class="ph ph-trash"></i></button>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </section>

        <!-- 제출 안내 (WYSIWYG) -->
        <section class="card">
          <h2 class="card__title"><i class="ph ph-upload-simple"></i>제출 안내 및 양식</h2>
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
                  <i :class="`ph ph-${doc.icon}`" :style="{ color: doc.color }"></i>
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
                  <i :class="`ph ph-${doc.icon}`" :style="{ color: doc.color, fontSize: '16px', flexShrink: 0 }"></i>
                  <input v-model="doc.color" type="color" class="op-color-input" />
                  <input v-model="doc.label" class="submission-doc__label ghost-input" placeholder="서류 이름" />
                  <label class="op-check"><input type="checkbox" v-model="doc.required" />필수</label>
                  <button type="button" class="wysiwyg-del" @click="removeDoc(i)"><i class="ph ph-trash"></i></button>
                </template>
              </li>
              <li v-if="editMode" class="submission-doc submission-doc--add" @click="addDoc">
                <i class="ph ph-plus" style="color:var(--primary)"></i>
                <span class="submission-doc__label" style="color:var(--primary)">서류 추가</span>
              </li>
            </ul>
          </div>
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
            <i class="ph ph-paper-plane-right"></i>공식 제안서 제출
          </button>
        </div>

        <!-- 첨부 자료실 — 항목이 있을 때만 노출 -->
        <div v-if="attachedFiles && attachedFiles.length > 0" class="card">
          <h3 class="card__title-sm"><i class="ph ph-folder"></i>첨부 자료실</h3>
          <div class="file-list">
            <div
              v-for="f in attachedFiles"
              :key="f.name"
              class="file-item"
              :class="{ 'file-item--locked': f.locked }"
            >
              <div class="file-icon" :class="`file-icon--${f.tone}`">
                <i :class="`ph ph-${f.icon}`"></i>
              </div>
              <div class="file-info">
                <div class="file-name">{{ f.name }}</div>
                <div class="file-size">{{ f.size }}</div>
              </div>
              <i :class="`ph ph-${f.locked ? 'lock-key' : 'download-simple'} file-action`"></i>
            </div>
          </div>
        </div>

        <!-- 심사 평가 기준 (매칭 5축 가중치) — 내부 사용자(HQ/AFFILIATE)와 편집 권한자만 노출 (P1) -->
        <div v-if="isInternalViewer || canEdit" class="card">
          <h3 class="card__title-sm">
            <i class="ph ph-scales"></i>심사 평가 기준 (매칭 가중치)
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
              <div class="contact-info__row"><i class="ph ph-envelope-simple"></i>{{ contactInfo?.email ?? '-' }}</div>
              <div class="contact-info__row"><i class="ph ph-phone"></i>{{ contactInfo?.phone ?? '-' }}</div>
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
import { ref, computed, onMounted, watch } from 'vue'
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

// fallback mockup — API에서 비어있을 때 보여줄 기본값
const FALLBACK = {
  hanwhaAssets: [
    { icon: 'device-mobile', title: '앱/온라인 채널 노출', desc: '한화온 앱 메인 배너 및 기획전 페이지 노출 (예상 트래픽: 50만/월)' },
    { icon: 'crown', title: 'VIP 고객 베이스', desc: '리조트 회원권 보유 VIP 타깃 e-DM 및 알림톡 발송 (10만 건)' },
    { icon: 'storefront', title: '오프라인 공간 활용', desc: '전국 12개 리조트 로비/객실 내 홍보물 비치 및 팝업 공간 제공' },
    { icon: 'ticket', title: '객실/티켓 자산', desc: '파트너사 이벤트용 리조트 숙박권 및 워터파크 이용권 지원' },
  ],
  partnerRoles: [
    { icon: 'star', title: '단독 혜택 제공', desc: '한화 패키지 이용객 대상 독점 할인 또는 한정판 굿즈/서비스 제공' },
    { icon: 'megaphone', title: '상호 마케팅 채널 지원', desc: '파트너사 온/오프라인 채널을 통한 공동 캠페인 홍보' },
    { icon: 'users-three', title: '운영 리소스 투입', desc: '제휴 서비스 제공을 위한 CS 채널 및 운영 인력 확보' },
    { icon: 'image', title: '콘텐츠 에셋 제작', desc: '기획전 구성에 필요한 브랜드 이미지 및 프로모션 소재 제공' },
  ],
  customerTags: ['프리미엄 레저', '키즈 에듀테인먼트', '편리한 이동', '미식 여행'],
  partnerValues: [
    { icon: 'crosshair', tone: 'primary', title: '고소득 구매력 타깃 확보', desc: '리조트 회원 및 프리미엄 객실 투숙객 대상의 고효율 마케팅' },
    { icon: 'hand-coins', tone: 'info', title: '브랜드 인지도 및 세일즈 증대', desc: '제휴 상품을 통한 직접적인 매출 발생 (예상 전환율 15%)' },
  ],
  timelineEvents: [
    { id: 1, color: 'gray', done: true, title: '모집 공고 오픈', date: '미정' },
    { id: 2, color: 'yellow', urgent: true, title: '제안서 제출 마감', date: '미정', tag: '중요', tagColor: 'red' },
    { id: 3, color: 'purple', title: '최종 파트너 선정 발표', date: '미정' },
  ],
  submissionDocs: [
    { icon: 'file-pdf', color: '#EF4444', required: true, label: '1. 제휴 제안서' },
    { icon: 'file-xls', color: '#22C55E', required: true, label: '2. 비용/혜택 구조 및 예상 KPI 산출표' },
    { icon: 'file-text', color: '#3B82F6', required: false, label: '3. 회사 소개서 및 레퍼런스' },
  ],
  attachedFiles: [],
  contactInfo: { name: '담당자 미지정', team: '', email: '-', phone: '-' },
  heroKpis: [
    { label: '예상 월 노출', value: '50만+' },
    { label: '예상 전환율', value: '15%' },
    { label: '파트너 모집', value: '5개 내외' },
  ],
  targetSegment: '3040 유자녀 가족 (초등학생 이하 자녀 동반)',
  targetScale: '캠페인 기간 내 패키지 구매자 약 15,000팀 (4인 기준 6만 명)',
  submissionInfo: {
    name: '제안서 온라인 제출',
    desc: '우측의 \'제안서 제출\' 버튼을 클릭하여 웹 폼 작성 및 파일 업로드',
    limit: '최대 파일 크기: 50MB (PDF, ZIP 권장)',
  },
}

const ICON_SUGGESTIONS = [
  'device-mobile', 'crown', 'storefront', 'ticket', 'star', 'megaphone',
  'users-three', 'image', 'gift', 'buildings', 'crosshair', 'hand-coins',
  'chart-bar', 'target', 'trend-up', 'handshake', 'broadcast', 'trophy',
]

const legend = [
  { label: '안내/설명회', color: '#60A5FA' },
  { label: '제출 마감', color: '#FBBF24' },
  { label: '심사/발표', color: '#C084FC' },
  { label: '운영 시작', color: '#34D399' },
]

// Campaign 기본 필드 매핑
const campaignName = computed(() => introData.value?.campaignName ?? '캠페인 소개')
const campaignSummary = computed(() =>
  introData.value?.campaignSummary
    ?? '아직 캠페인 소개 내용이 등록되지 않았습니다. 편집 모드에서 내용을 입력해 주세요.'
)
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
const overviewItems = computed(() => {
  const raw = introData.value?.overviewItems
  if (Array.isArray(raw) && raw.length > 0) return raw
  if (raw && Array.isArray(raw.list) && raw.list.length > 0) return raw.list
  const items = [
    { label: '캠페인 이름', value: campaignName.value },
    { label: '담당자', value: ownerLoginId.value },
    { label: '캠페인 상태', value: campaignStatus.value },
    { label: '제안 마감', value: formatDate(recruitDeadline.value) },
  ]
  if (primaryGoal.value) items.push({ label: '주 목표', value: primaryGoal.value })
  if (assetName.value) items.push({ label: '자산명', value: assetName.value })
  if (campaignMethods.value?.length) items.push({ label: '캠페인 방식', value: campaignMethods.value.join(', ') })
  return items
})

const targetSegment = computed(() => introData.value?.targetSegment ?? FALLBACK.targetSegment)
const targetScale = computed(() => introData.value?.targetScale ?? FALLBACK.targetScale)
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
function addOverviewItem() { editDraft.value.overviewItems.push({ label: '', value: '' }) }
function removeOverviewItem(i) { editDraft.value.overviewItems.splice(i, 1) }
function addAsset() { editDraft.value.hanwhaAssets.push({ icon: 'star', title: '', desc: '' }) }
function removeAsset(i) { editDraft.value.hanwhaAssets.splice(i, 1) }
function addRole() { editDraft.value.partnerRoles.push({ icon: 'handshake', title: '', desc: '' }) }
function removeRole(i) { editDraft.value.partnerRoles.splice(i, 1) }
function addTag() { editDraft.value.customerTags.push('') }
function removeTag(i) { editDraft.value.customerTags.splice(i, 1) }
function addValue() { editDraft.value.partnerValues.push({ icon: 'chart-bar', tone: 'primary', title: '', desc: '' }) }
function removeValue(i) { editDraft.value.partnerValues.splice(i, 1) }
function addEvent() {
  editDraft.value.timelineEvents.push({
    id: Date.now(), color: 'gray', done: false, urgent: false,
    title: '', date: '', tag: '', tagColor: '',
    detailMethod: '', detailText: '', docsText: '', note: '',
  })
}
function removeEvent(i) { editDraft.value.timelineEvents.splice(i, 1) }
function addDoc() { editDraft.value.submissionDocs.push({ icon: 'file-text', color: '#3B82F6', required: false, label: '' }) }
function removeDoc(i) { editDraft.value.submissionDocs.splice(i, 1) }
function addKpi() { editDraft.value.heroKpis.push({ label: '', value: '' }) }
function removeKpi(i) { editDraft.value.heroKpis.splice(i, 1) }

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
    campaignSummary: introData.value?.campaignSummary ?? '',
    overviewItems: JSON.parse(JSON.stringify(overviewItems.value)),
    heroKpis: JSON.parse(JSON.stringify(heroKpis.value)),
    hanwhaAssets: JSON.parse(JSON.stringify(hanwhaAssets.value)),
    partnerRoles: JSON.parse(JSON.stringify(partnerRoles.value)),
    customerTags: [...customerTags.value],
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
      campaignSummary: editDraft.value.campaignSummary || null,
      overviewItems:  editDraft.value.overviewItems,
      heroKpis:       editDraft.value.heroKpis,
      hanwhaAssets:   editDraft.value.hanwhaAssets,
      partnerRoles:   editDraft.value.partnerRoles,
      customerTags:   editDraft.value.customerTags,
      targetSegment:  editDraft.value.targetSegment || null,
      targetScale:    editDraft.value.targetScale   || null,
      partnerValues:  editDraft.value.partnerValues,
      timelineEvents: tlPayload,
      submissionDocs: editDraft.value.submissionDocs,
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

onMounted(() => loadIntro(route.params.campaignId))

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
.op-hero__meta i { font-size: 15px; color: var(--text-4); }
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

/* ─── Hero KPI Strip ─────────────────────────────────────────── */
.kpi-strip {
  display: flex;
  gap: 12px;
  margin-bottom: 28px;
}
.kpi-card {
  flex: 1;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  text-align: center;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.15s;
}
.kpi-card:hover { box-shadow: var(--shadow-md); }
.kpi-card__value {
  font-size: 26px;
  font-weight: 900;
  color: var(--primary);
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
  margin-bottom: 4px;
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
.card__title i { color: var(--primary); font-size: 18px; }
.card__title-sm {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 16px;
}
.card__title-sm i { color: var(--text-3); font-size: 16px; }
.card__sub { font-size: 13px; color: var(--text-3); margin-top: -14px; margin-bottom: 20px; line-height: 1.5; }

/* Split card */
.card--split {
  display: flex;
  padding: 0;
  overflow: hidden;
}
.split-pane {
  padding: 28px;
  flex: 1;
}
.split-pane--r {
  background: var(--surface-muted);
  border-left: 1px solid var(--border);
}

/* CTA card */
.card--cta {
  border-color: var(--primary-m);
  background: var(--surface);
}

/* ─── Two column grid ────────────────────────────────────────── */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
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

/* ─── Asset List ─────────────────────────────────────────────── */
.asset-list { display: flex; flex-direction: column; gap: 20px; list-style: none; padding: 0; margin: 0; }
.asset-item { display: flex; gap: 12px; }
.asset-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  border: 1px solid var(--border);
  color: var(--text-2);
  font-size: 16px;
  margin-top: 1px;
}
.asset-item__title {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-1);
  margin-bottom: 3px;
}
.asset-item__desc { font-size: 12px; color: var(--text-3); line-height: 1.55; }

/* ─── Target DL ──────────────────────────────────────────────── */
.target-dl { display: flex; flex-direction: column; gap: 16px; }
.target-dl dt {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-4);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 4px;
}
.target-dl dd { font-size: 13px; color: var(--text-2); line-height: 1.5; margin: 0; }
.tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 2px; }
.tag {
  padding: 3px 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-2);
}

/* ─── Value List ─────────────────────────────────────────────── */
.value-list { display: flex; flex-direction: column; gap: 20px; }
.value-item { display: flex; gap: 14px; }
.value-icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-top: 1px;
}
.value-icon--primary { background: var(--primary-s); color: var(--primary); }
.value-icon--info { background: var(--info-s); color: var(--info); }
.value-item__title { font-size: 13px; font-weight: 700; color: var(--text-1); margin-bottom: 4px; }
.value-item__desc { font-size: 12px; color: var(--text-3); line-height: 1.6; }

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
.submission-doc i { font-size: 18px; flex-shrink: 0; }
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
.contact-info__row i { font-size: 13px; }

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
.wysiwyg-del--float {
  position: absolute;
  top: -6px;
  right: -6px;
  opacity: 0;
  transition: opacity 0.15s;
  z-index: 2;
}
.asset-item--editable:hover .wysiwyg-del--float,
.value-item--editable:hover .wysiwyg-del--float { opacity: 1; }
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

/* ─── Editable Item Wrappers ─────────────────────────────────────── */
.asset-item--editable {
  position: relative;
  border-radius: var(--radius-md);
  padding: 6px;
  margin: -6px;
  transition: background 0.12s;
}
.asset-item--editable:hover { background: rgba(99, 102, 241, 0.04); }
.value-item--editable {
  position: relative;
  border-radius: var(--radius-md);
  padding: 4px;
  margin: -4px;
  transition: background 0.12s;
}
.value-item--editable:hover { background: rgba(99, 102, 241, 0.04); }
.overview-cell--editable { position: relative; }

/* Clickable icon hint */
.asset-icon--clickable, .value-icon--clickable {
  cursor: pointer;
  transition: transform 0.12s, box-shadow 0.12s;
}
.asset-icon--clickable:hover { transform: scale(1.08); box-shadow: 0 2px 8px rgba(99,102,241,0.3); }
.value-icon--clickable:hover { transform: scale(1.08); box-shadow: 0 2px 8px rgba(99,102,241,0.3); }

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
