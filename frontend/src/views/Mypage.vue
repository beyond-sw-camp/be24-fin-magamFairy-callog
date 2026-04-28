<script setup>
import { computed, ref, watch } from 'vue';
import { usePlannerStore } from '@/stores/planner';

const plannerStore = usePlannerStore();

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const adminUserIds = ['jaewon'];

function createMemberId() {
  return `member-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function cloneGroups(groups) {
  return groups.map((group) => ({
    ...group,
    members: group.members.map((member) => ({ ...member })),
  }));
}

function createProfileSeed(userId) {
  const profileMap = {
    jaewon: {
      name: '김재원',
      contact: '010-1234-5678',
      email: 'jaewon@callog.com',
    },
    jiwon: {
      name: '박지원',
      contact: '010-2345-6789',
      email: 'jiwon@callog.com',
    },
    sumin: {
      name: '이수민',
      contact: '010-3456-7890',
      email: 'sumin@callog.com',
    },
    minseo: {
      name: '오민서',
      contact: '010-4567-8901',
      email: 'minseo@callog.com',
    },
    taeyoung: {
      name: '임태영',
      contact: '010-5678-9012',
      email: 'taeyoung@callog.com',
    },
    hayoon: {
      name: '정하윤',
      contact: '010-6789-0123',
      email: 'hayoon@callog.com',
    },
  };

  return {
    ...(profileMap[userId] ?? {
      name: '사용자',
      contact: '010-0000-0000',
      email: `${userId}@callog.com`,
    }),
  };
}

function createTeamGroupsSeed() {
  return [
    {
      id: 1,
      name: '콘텐츠 기획 그룹',
      members: [
        { id: 'group-1-member-1', name: '김재원', email: 'jaewon@callog.com' },
        { id: 'group-1-member-2', name: '박지원', email: 'jiwon@callog.com' },
      ],
    },
    {
      id: 2,
      name: '콘텐츠 제작 그룹',
      members: [
        { id: 'group-2-member-1', name: '이수민', email: 'sumin@callog.com' },
        { id: 'group-2-member-2', name: '오민서', email: 'minseo@callog.com' },
      ],
    },
    {
      id: 3,
      name: '품질 관리 그룹',
      members: [
        { id: 'group-3-member-1', name: '임태영', email: 'taeyoung@callog.com' },
        { id: 'group-3-member-2', name: '정하윤', email: 'hayoon@callog.com' },
      ],
    },
  ];
}

function createTeamInfoSeed() {
  return {
    teamName: 'CALLOG 운영팀',
    description: '콘텐츠 운영 일정과 팀 협업 흐름을 관리하는 기본 조직입니다.',
    department: '마케팅본부',
  };
}

const currentUserId = computed(() => plannerStore.currentUserId ?? 'jaewon');
const isAdmin = computed(() => adminUserIds.includes(currentUserId.value));
const roleLabel = computed(() => (isAdmin.value ? '관리자' : '사용자'));

const activeTab = ref('profile');

const savedProfile = ref(createProfileSeed(currentUserId.value));
const profileForm = ref({ ...savedProfile.value });
const profileError = ref('');
const profileMessage = ref('');

const savedTeamGroups = ref(createTeamGroupsSeed());
const teamGroupsForm = ref(cloneGroups(savedTeamGroups.value));
const inviteForm = ref({
  groupId: 1,
  target: '',
});
const teamGroupError = ref('');
const teamGroupMessage = ref('');
const inviteDispatchLogs = ref([]);

const savedTeamInfo = ref(createTeamInfoSeed());
const teamInfoForm = ref({ ...savedTeamInfo.value });
const teamInfoMessage = ref('');

const profileInitial = computed(() => savedProfile.value.name.trim().slice(0, 1) || 'U');
const totalTeamMembers = computed(() =>
  savedTeamGroups.value.reduce((total, group) => total + group.members.length, 0),
);
const profileCompletion = computed(() => {
  const fields = [savedProfile.value.name, savedProfile.value.contact, savedProfile.value.email];
  return Math.round((fields.filter((field) => field.trim()).length / fields.length) * 100);
});
const mypageStats = computed(() => [
  {
    label: '프로필 완성도',
    value: `${profileCompletion.value}%`,
    caption: '이름, 연락처, 이메일 기준',
    positive: profileCompletion.value === 100,
  },
  {
    label: '현재 권한',
    value: roleLabel.value,
    caption: isAdmin.value ? '팀 관리 기능 사용 가능' : '개인 정보 수정 가능',
  },
  {
    label: '팀 그룹',
    value: isAdmin.value ? `${savedTeamGroups.value.length}` : '-',
    caption: isAdmin.value ? `${totalTeamMembers.value}명 등록` : '관리자 전용',
  },
  {
    label: '소속 부서',
    value: isAdmin.value ? savedTeamInfo.value.department : '개인',
    caption: savedTeamInfo.value.teamName,
  },
]);

const tabOptions = computed(() => {
  const items = [
    {
      value: 'profile',
      label: '개인 정보 수정',
      description: '이름, 연락처, 이메일을 수정합니다.',
    },
  ];

  if (isAdmin.value) {
    items.push(
      {
        value: 'team-groups',
        label: '팀 그룹 관리',
        description: '현재 팀 그룹, 팀원 초대, 팀원 추방을 관리합니다.',
      },
      {
        value: 'team-info',
        label: '팀 정보 수정',
        description: '팀명, 팀 설명, 담당 부서를 수정합니다.',
      },
    );
  }

  return items;
});

watch(
  tabOptions,
  (items) => {
    if (!items.some((item) => item.value === activeTab.value)) {
      activeTab.value = 'profile';
    }
  },
  { immediate: true },
);

watch(
  currentUserId,
  (nextUserId) => {
    savedProfile.value = createProfileSeed(nextUserId);
    profileForm.value = { ...savedProfile.value };
    profileError.value = '';
    profileMessage.value = '';

    savedTeamGroups.value = createTeamGroupsSeed();
    teamGroupsForm.value = cloneGroups(savedTeamGroups.value);
    inviteForm.value = {
      groupId: savedTeamGroups.value[0]?.id ?? 1,
      target: '',
    };
    teamGroupError.value = '';
    teamGroupMessage.value = '';
    inviteDispatchLogs.value = [];

    savedTeamInfo.value = createTeamInfoSeed();
    teamInfoForm.value = { ...savedTeamInfo.value };
    teamInfoMessage.value = '';
  },
  { immediate: true },
);

function handleProfileSave() {
  profileError.value = '';
  profileMessage.value = '';

  const nextEmail = profileForm.value.email.trim();

  if (!emailPattern.test(nextEmail)) {
    profileError.value = '올바른 이메일 형식을 입력해 주세요.';
    return;
  }

  savedProfile.value = {
    name: profileForm.value.name.trim(),
    contact: profileForm.value.contact.trim(),
    email: nextEmail,
  };

  profileForm.value = { ...savedProfile.value };
  profileMessage.value = '개인 정보가 저장되어 즉시 반영되었습니다.';
}

function handleInviteDraft() {
  teamGroupError.value = '';
  teamGroupMessage.value = '';

  const target = inviteForm.value.target.trim();
  const selectedGroup = teamGroupsForm.value.find((group) => group.id === inviteForm.value.groupId);

  if (!target) {
    teamGroupError.value = '초대할 이름 또는 이메일을 입력해 주세요.';
    return;
  }

  if (!selectedGroup) {
    teamGroupError.value = '초대할 팀 그룹을 선택해 주세요.';
    return;
  }

  if (target.includes('@') && !emailPattern.test(target)) {
    teamGroupError.value = '이메일로 초대할 경우 올바른 형식으로 입력해 주세요.';
    return;
  }

  selectedGroup.members.push({
    id: createMemberId(),
    name: target.includes('@') ? target.split('@')[0] : target,
    email: target.includes('@') ? target : '',
    inviteTarget: target,
    isDraftInvite: true,
  });

  teamGroupMessage.value = `${selectedGroup.name}에 초대 대상을 추가했습니다. 저장하면 알림과 이메일이 함께 발송됩니다.`;
  inviteForm.value.target = '';
}

function handleMemberRemove(groupId, memberId) {
  const selectedGroup = teamGroupsForm.value.find((group) => group.id === groupId);
  const selectedMember = selectedGroup?.members.find((member) => member.id === memberId);

  if (!selectedGroup || !selectedMember) {
    return;
  }

  selectedGroup.members = selectedGroup.members.filter((member) => member.id !== memberId);
  teamGroupError.value = '';
  teamGroupMessage.value = `${selectedMember.name}님을 ${selectedGroup.name}에서 추방 대상으로 반영했습니다. 저장하면 즉시 적용됩니다.`;
}

function handleTeamGroupsSave() {
  if (!isAdmin.value) {
    return;
  }

  teamGroupError.value = '';
  teamGroupMessage.value = '';

  if (teamGroupsForm.value.some((group) => !group.name.trim())) {
    teamGroupError.value = '팀 그룹명은 비워둘 수 없습니다.';
    return;
  }

  const dispatchLogs = [];
  const nextGroups = teamGroupsForm.value.map((group) => ({
    ...group,
    name: group.name.trim(),
    members: group.members.map((member) => {
      const target = member.inviteTarget ?? member.email ?? member.name;

      if (member.isDraftInvite) {
        dispatchLogs.push(
          {
            id: `${member.id}-notification`,
            target,
            channel: '알림',
            status: '발송 완료',
          },
          {
            id: `${member.id}-email`,
            target,
            channel: '이메일',
            status: '발송 완료',
          },
        );
      }

      return {
        id: member.id,
        name: member.name.trim() || target,
        email: member.email,
      };
    }),
  }));

  savedTeamGroups.value = cloneGroups(nextGroups);
  teamGroupsForm.value = cloneGroups(savedTeamGroups.value);
  inviteDispatchLogs.value = [...dispatchLogs, ...inviteDispatchLogs.value].slice(0, 12);
  teamGroupMessage.value = '팀 그룹 변경 사항이 저장되어 즉시 반영되었습니다.';
}

function handleTeamInfoSave() {
  if (!isAdmin.value) {
    return;
  }

  savedTeamInfo.value = {
    teamName: teamInfoForm.value.teamName.trim(),
    description: teamInfoForm.value.description.trim(),
    department: teamInfoForm.value.department.trim(),
  };

  teamInfoForm.value = { ...savedTeamInfo.value };
  teamInfoMessage.value = '팀 정보가 저장되어 팀 전체에 즉시 반영되었습니다.';
}
</script>

<template>
  <section class="mypage-dashboard">
    <header class="mypage-hero">
      <div class="mypage-hero__profile">
        <div class="mypage-avatar">{{ profileInitial }}</div>
        <div>
          <p class="mypage-eyebrow">CALLOG MY PAGE</p>
          <h2>마이페이지</h2>
          <p>계정 정보와 팀 관리 흐름을 한 화면에서 확인하고 수정합니다.</p>
        </div>
      </div>
      <span class="mypage-status">{{ roleLabel }}</span>
    </header>

    <section class="mypage-stats" aria-label="내 계정 요약">
      <article v-for="stat in mypageStats" :key="stat.label" class="mypage-stat-card">
        <p>{{ stat.label }}</p>
        <strong>{{ stat.value }}</strong>
        <span :class="{ 'mypage-stat-card__caption--positive': stat.positive }">
          {{ stat.caption }}
        </span>
      </article>
    </section>

    <section class="mypage-layout">
      <aside class="mypage-sidebar">
        <article class="mypage-panel mypage-profile-card">
          <div class="mypage-profile-card__avatar">{{ profileInitial }}</div>
          <div>
            <strong>{{ savedProfile.name }}</strong>
            <p>{{ savedProfile.email }}</p>
            <span>{{ savedProfile.contact }}</span>
          </div>
        </article>

        <article class="mypage-panel">
          <div class="mypage-panel__header">
            <h3>메뉴</h3>
          </div>
          <div class="mypage-tab-list">
            <button
              v-for="tab in tabOptions"
              :key="tab.value"
              type="button"
              class="mypage-tab"
              :class="{ 'mypage-tab--active': activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              <strong>{{ tab.label }}</strong>
              <span>{{ tab.description }}</span>
            </button>
          </div>
        </article>
      </aside>

      <main class="mypage-main">
        <article class="mypage-panel mypage-panel--main">
          <template v-if="activeTab === 'profile'">
            <div class="mypage-panel__header mypage-panel__header--stacked">
              <p>MYPAGE_001</p>
              <h3>개인 정보 수정</h3>
              <span>이름, 연락처, 이메일을 수정할 수 있습니다.</span>
            </div>

            <div class="mypage-form-grid mypage-form-grid--two">
              <label class="mypage-field">
                <span>이름</span>
                <input v-model="profileForm.name" type="text" />
              </label>

              <label class="mypage-field">
                <span>연락처</span>
                <input v-model="profileForm.contact" type="text" />
              </label>

              <label class="mypage-field mypage-field--full">
                <span>이메일</span>
                <input v-model="profileForm.email" type="email" />
              </label>
            </div>

            <p v-if="profileError" class="mypage-alert mypage-alert--danger">{{ profileError }}</p>
            <p v-if="profileMessage" class="mypage-alert mypage-alert--success">{{ profileMessage }}</p>

            <article class="mypage-summary-box">
              <h4>현재 반영된 정보</h4>
              <dl>
                <div>
                  <dt>이름</dt>
                  <dd>{{ savedProfile.name }}</dd>
                </div>
                <div>
                  <dt>연락처</dt>
                  <dd>{{ savedProfile.contact }}</dd>
                </div>
                <div>
                  <dt>이메일</dt>
                  <dd>{{ savedProfile.email }}</dd>
                </div>
              </dl>
            </article>

            <div class="mypage-actions">
              <button type="button" class="mypage-primary-button" @click="handleProfileSave">
                저장
              </button>
            </div>
          </template>

          <template v-else-if="activeTab === 'team-groups'">
            <div class="mypage-panel__header mypage-panel__header--stacked">
              <p>MYPAGE_002</p>
              <h3>팀 그룹 관리</h3>
              <span>팀 그룹을 수정하고, 초대 대상과 팀원 구성을 관리합니다.</span>
            </div>

            <div class="mypage-team-layout">
              <div class="mypage-group-list">
                <article v-for="group in teamGroupsForm" :key="group.id" class="mypage-sub-panel">
                  <div class="mypage-group-head">
                    <label class="mypage-field">
                      <span>팀 그룹명</span>
                      <input v-model="group.name" type="text" />
                    </label>
                    <strong>현재 팀원 {{ group.members.length }}명</strong>
                  </div>

                  <div class="mypage-member-list">
                    <div v-for="member in group.members" :key="member.id" class="mypage-member-row">
                      <div>
                        <p>{{ member.name }}</p>
                        <span>{{ member.email || member.inviteTarget || '이름으로 초대 예정' }}</span>
                      </div>
                      <button type="button" class="mypage-danger-button" @click="handleMemberRemove(group.id, member.id)">
                        추방
                      </button>
                    </div>
                  </div>
                </article>
              </div>

              <aside class="mypage-side-stack">
                <article class="mypage-sub-panel">
                  <div class="mypage-panel__header mypage-panel__header--compact">
                    <h3>팀원 초대</h3>
                    <span>저장 시 알림과 이메일이 발송됩니다.</span>
                  </div>

                  <label class="mypage-field">
                    <span>초대할 팀 그룹</span>
                    <select v-model="inviteForm.groupId">
                      <option v-for="group in teamGroupsForm" :key="group.id" :value="group.id">
                        {{ group.name }}
                      </option>
                    </select>
                  </label>

                  <label class="mypage-field">
                    <span>이름 또는 이메일</span>
                    <input v-model="inviteForm.target" type="text" placeholder="예: 홍길동 또는 gildong@callog.com" />
                  </label>

                  <button type="button" class="mypage-secondary-button" @click="handleInviteDraft">
                    초대 목록에 추가
                  </button>
                </article>

                <article v-if="inviteDispatchLogs.length" class="mypage-sub-panel">
                  <div class="mypage-panel__header mypage-panel__header--compact">
                    <h3>초대 발송 결과</h3>
                  </div>
                  <div class="mypage-log-list">
                    <div v-for="log in inviteDispatchLogs" :key="log.id">
                      <strong>{{ log.channel }}</strong>
                      <span>{{ log.target }} · {{ log.status }}</span>
                    </div>
                  </div>
                </article>
              </aside>
            </div>

            <p v-if="teamGroupError" class="mypage-alert mypage-alert--danger">{{ teamGroupError }}</p>
            <p v-if="teamGroupMessage" class="mypage-alert mypage-alert--success">{{ teamGroupMessage }}</p>

            <div class="mypage-actions">
              <button type="button" class="mypage-primary-button" @click="handleTeamGroupsSave">
                저장
              </button>
            </div>
          </template>

          <template v-else>
            <div class="mypage-panel__header mypage-panel__header--stacked">
              <p>MYPAGE_003</p>
              <h3>팀 정보 수정</h3>
              <span>팀명, 팀 설명, 담당 부서를 수정할 수 있습니다.</span>
            </div>

            <div class="mypage-form-grid">
              <label class="mypage-field">
                <span>팀명</span>
                <input v-model="teamInfoForm.teamName" type="text" />
              </label>

              <label class="mypage-field">
                <span>팀 설명</span>
                <textarea v-model="teamInfoForm.description" rows="4" />
              </label>

              <label class="mypage-field">
                <span>담당 부서</span>
                <input v-model="teamInfoForm.department" type="text" />
              </label>
            </div>

            <p v-if="teamInfoMessage" class="mypage-alert mypage-alert--success">{{ teamInfoMessage }}</p>

            <article class="mypage-summary-box">
              <h4>현재 반영된 팀 정보</h4>
              <dl>
                <div>
                  <dt>팀명</dt>
                  <dd>{{ savedTeamInfo.teamName }}</dd>
                </div>
                <div>
                  <dt>팀 설명</dt>
                  <dd>{{ savedTeamInfo.description }}</dd>
                </div>
                <div>
                  <dt>담당 부서</dt>
                  <dd>{{ savedTeamInfo.department }}</dd>
                </div>
              </dl>
            </article>

            <div class="mypage-actions">
              <button type="button" class="mypage-primary-button" @click="handleTeamInfoSave">
                저장
              </button>
            </div>
          </template>
        </article>
      </main>
    </section>
  </section>
</template>

<style scoped>
.mypage-dashboard {
  display: flex;
  width: 100%;
  max-width: var(--content-max-width);
  flex-direction: column;
  gap: 16px;
  margin: 0 auto;
}

.mypage-hero,
.mypage-panel,
.mypage-stat-card,
.mypage-sub-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal);
}

.mypage-hero {
  display: flex;
  min-height: 104px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
}

.mypage-hero__profile {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 16px;
}

.mypage-avatar,
.mypage-profile-card__avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--color-primary-500);
  color: #ffffff;
  font-weight: 800;
}

.mypage-avatar {
  width: 54px;
  height: 54px;
  font-size: 20px;
}

.mypage-eyebrow {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

.mypage-hero h2 {
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

.mypage-hero p:last-child {
  margin-top: 6px;
  color: var(--muted-text);
  font-size: 14px;
}

.mypage-status,
.mypage-group-head strong {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: var(--radius-full);
  background: var(--color-primary-500);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.mypage-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.mypage-stat-card {
  min-height: 128px;
  padding: 22px 24px;
}

.mypage-stat-card p {
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 600;
}

.mypage-stat-card strong {
  display: block;
  margin: 8px 0 4px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mypage-stat-card span {
  color: var(--subtle-text);
  font-size: 13px;
}

.mypage-stat-card__caption--positive {
  color: var(--color-success) !important;
  font-weight: 600;
}

.mypage-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
}

.mypage-sidebar,
.mypage-side-stack,
.mypage-group-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mypage-panel {
  padding: 20px;
}

.mypage-panel--main {
  min-height: 560px;
}

.mypage-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.mypage-panel__header--stacked,
.mypage-panel__header--compact {
  display: block;
}

.mypage-panel__header p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.mypage-panel__header h3 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.mypage-panel__header--stacked h3 {
  margin-top: 4px;
}

.mypage-panel__header span {
  display: block;
  margin-top: 6px;
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.5;
}

.mypage-profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
}

.mypage-profile-card__avatar {
  width: 42px;
  height: 42px;
  font-size: 16px;
}

.mypage-profile-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.mypage-profile-card p,
.mypage-profile-card span {
  display: block;
  margin-top: 3px;
  color: var(--muted-text);
  font-size: 13px;
}

.mypage-tab-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mypage-tab {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  text-align: left;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.mypage-tab:hover,
.mypage-tab--active {
  border-color: var(--color-primary-300);
  background: var(--color-primary-500);
  color: #ffffff;
}

.mypage-tab strong {
  display: block;
  font-size: 14px;
  font-weight: 700;
}

.mypage-tab span {
  display: block;
  margin-top: 4px;
  color: currentColor;
  font-size: 12px;
  line-height: 1.45;
  opacity: 0.72;
}

.mypage-form-grid {
  display: grid;
  gap: 16px;
}

.mypage-form-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mypage-field {
  display: grid;
  gap: 8px;
}

.mypage-field--full {
  grid-column: 1 / -1;
}

.mypage-field span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.mypage-field input,
.mypage-field select,
.mypage-field textarea {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  padding: 11px 13px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  outline: none;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.mypage-field textarea {
  min-height: 112px;
  resize: vertical;
}

.mypage-field input:focus,
.mypage-field select:focus,
.mypage-field textarea:focus {
  border-color: var(--color-primary-300);
  background: var(--control-focus-color);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary-500) 14%, transparent);
}

.mypage-summary-box,
.mypage-sub-panel {
  padding: 18px;
  background: var(--panel-muted);
}

.mypage-summary-box {
  margin-top: 20px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}

.mypage-summary-box h4 {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.mypage-summary-box dl {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.mypage-summary-box div {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
}

.mypage-summary-box dt {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

.mypage-summary-box dd {
  min-width: 0;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.mypage-team-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.mypage-group-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 12px;
}

.mypage-member-list,
.mypage-log-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.mypage-member-row,
.mypage-log-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
}

.mypage-member-row p {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.mypage-member-row span,
.mypage-log-list span {
  display: block;
  margin-top: 3px;
  color: var(--muted-text);
  font-size: 12px;
}

.mypage-log-list strong {
  color: var(--text-primary);
  font-size: 13px;
}

.mypage-alert {
  margin-top: 16px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
}

.mypage-alert--danger {
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.mypage-alert--success {
  background: color-mix(in srgb, var(--color-success) 12%, var(--panel-color));
  color: var(--color-success);
}

.mypage-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.mypage-primary-button,
.mypage-secondary-button,
.mypage-danger-button {
  min-height: 36px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.mypage-primary-button {
  padding: 0 18px;
  background: var(--color-primary-500);
  color: #ffffff;
}

.mypage-primary-button:hover {
  background: var(--color-primary-600);
}

.mypage-secondary-button {
  width: 100%;
  margin-top: 14px;
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.mypage-secondary-button:hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
}

.mypage-danger-button {
  padding: 0 12px;
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

@media (max-width: 1100px) {
  .mypage-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .mypage-layout,
  .mypage-team-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .mypage-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .mypage-hero__profile {
    align-items: flex-start;
  }

  .mypage-stats,
  .mypage-form-grid--two,
  .mypage-group-head {
    grid-template-columns: 1fr;
  }

  .mypage-summary-box div {
    grid-template-columns: 1fr;
  }

  .mypage-member-row,
  .mypage-log-list div {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
