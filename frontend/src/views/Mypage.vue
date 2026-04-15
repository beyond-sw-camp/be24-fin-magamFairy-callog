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
  <section class="relative min-h-screen overflow-hidden bg-slate-50">
    <div class="pointer-events-none absolute inset-x-0 top-0 h-72 bg-gradient-to-b from-sky-200/60 via-blue-100/35 to-transparent"></div>

    <div class="relative mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 md:p-6">
      <header class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">CALLOG MY PAGE</p>
            <h1 class="mt-2 text-3xl font-bold tracking-tight text-slate-900 md:text-4xl">마이페이지</h1>
            <p class="mt-3 max-w-2xl text-sm leading-6 text-slate-500">
              요구사항 정의서 기준으로 개인 정보 수정과 관리자 전용 팀 관리 기능만 포함한 화면입니다.
            </p>
          </div>

          <div class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
            <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">현재 권한</span>
            <strong class="text-sm font-semibold text-slate-900">{{ roleLabel }}</strong>
          </div>
        </div>
      </header>

      <div class="grid gap-4 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside class="grid gap-4">
          <article class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <p class="text-sm font-semibold text-slate-900">{{ savedProfile.name }}</p>
            <p class="mt-2 text-sm text-slate-500">{{ savedProfile.contact }}</p>
            <p class="mt-1 text-sm text-slate-500">{{ savedProfile.email }}</p>
          </article>

          <article class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 class="text-sm font-semibold uppercase tracking-[0.22em] text-slate-400">메뉴</h2>

            <div class="mt-4 grid gap-2">
              <button
                v-for="tab in tabOptions"
                :key="tab.value"
                type="button"
                class="rounded-xl border px-4 py-3 text-left transition"
                :class="activeTab === tab.value ? 'border-slate-900 bg-slate-900 text-white' : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-white'"
                @click="activeTab = tab.value"
              >
                <strong class="block text-sm font-semibold">{{ tab.label }}</strong>
                <span class="mt-1 block text-xs leading-5" :class="activeTab === tab.value ? 'text-slate-300' : 'text-slate-400'">
                  {{ tab.description }}
                </span>
              </button>
            </div>
          </article>
        </aside>

        <main>
          <article class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm md:p-8">
            <template v-if="activeTab === 'profile'">
              <div class="border-b border-slate-200 pb-6">
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">MYPAGE_001</p>
                <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">개인 정보 수정</h2>
                <p class="mt-2 text-sm text-slate-500">
                  이름, 연락처, 이메일만 수정할 수 있으며 비밀번호 변경은 별도 기능으로 분리됩니다.
                </p>
              </div>

              <div class="mt-6 grid gap-4 md:grid-cols-2">
                <label class="grid gap-2">
                  <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">이름</span>
                  <input
                    v-model="profileForm.name"
                    type="text"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </label>

                <label class="grid gap-2">
                  <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">연락처</span>
                  <input
                    v-model="profileForm.contact"
                    type="text"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </label>

                <label class="grid gap-2 md:col-span-2">
                  <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">이메일</span>
                  <input
                    v-model="profileForm.email"
                    type="email"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </label>
              </div>

              <p v-if="profileError" class="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-600">
                {{ profileError }}
              </p>
              <p v-if="profileMessage" class="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
                {{ profileMessage }}
              </p>

              <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-5">
                <h3 class="text-sm font-semibold text-slate-900">현재 반영된 정보</h3>
                <div class="mt-3 grid gap-2 text-sm text-slate-600">
                  <p>이름: {{ savedProfile.name }}</p>
                  <p>연락처: {{ savedProfile.contact }}</p>
                  <p>이메일: {{ savedProfile.email }}</p>
                </div>
              </div>

              <div class="mt-6 flex justify-end">
                <button
                  type="button"
                  class="inline-flex items-center justify-center rounded-xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
                  @click="handleProfileSave"
                >
                  저장
                </button>
              </div>
            </template>

            <template v-else-if="activeTab === 'team-groups'">
              <div class="border-b border-slate-200 pb-6">
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">MYPAGE_002</p>
                <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">팀 그룹 관리</h2>
                <p class="mt-2 text-sm text-slate-500">
                  관리자는 현재 팀 그룹을 수정하고, 이름 또는 이메일로 팀원을 초대하거나 팀원을 추방할 수 있습니다.
                </p>
              </div>

              <div class="mt-6 grid gap-4 xl:grid-cols-[minmax(0,1fr)_320px]">
                <div class="grid gap-4">
                  <article
                    v-for="group in teamGroupsForm"
                    :key="group.id"
                    class="rounded-2xl border border-slate-200 bg-slate-50 p-5"
                  >
                    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <label class="grid flex-1 gap-2">
                        <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">팀 그룹명</span>
                        <input
                          v-model="group.name"
                          type="text"
                          class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:ring-4 focus:ring-slate-900/5"
                        />
                      </label>

                      <div class="rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-600">
                        현재 팀원 {{ group.members.length }}명
                      </div>
                    </div>

                    <div class="mt-4 grid gap-2">
                      <div
                        v-for="member in group.members"
                        :key="member.id"
                        class="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
                      >
                        <div>
                          <p class="text-sm font-semibold text-slate-900">{{ member.name }}</p>
                          <p class="mt-1 text-xs text-slate-500">{{ member.email || member.inviteTarget || '이름으로 초대 예정' }}</p>
                        </div>

                        <button
                          type="button"
                          class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-600 transition hover:bg-rose-100"
                          @click="handleMemberRemove(group.id, member.id)"
                        >
                          추방
                        </button>
                      </div>
                    </div>
                  </article>
                </div>

                <div class="grid gap-4">
                  <article class="rounded-2xl border border-slate-200 bg-slate-50 p-5">
                    <h3 class="text-sm font-semibold text-slate-900">팀원 초대</h3>
                    <p class="mt-2 text-sm leading-6 text-slate-500">
                      이름 또는 이메일로 초대 대상을 추가할 수 있으며, 저장 시 알림과 이메일이 함께 발송됩니다.
                    </p>

                    <label class="mt-4 grid gap-2">
                      <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">초대할 팀 그룹</span>
                      <select
                        v-model="inviteForm.groupId"
                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:ring-4 focus:ring-slate-900/5"
                      >
                        <option v-for="group in teamGroupsForm" :key="group.id" :value="group.id">
                          {{ group.name }}
                        </option>
                      </select>
                    </label>

                    <label class="mt-4 grid gap-2">
                      <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">이름 또는 이메일</span>
                      <input
                        v-model="inviteForm.target"
                        type="text"
                        placeholder="예: 홍길동 또는 gildong@callog.com"
                        class="w-full rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:ring-4 focus:ring-slate-900/5"
                      />
                    </label>

                    <button
                      type="button"
                      class="mt-4 inline-flex items-center justify-center rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100"
                      @click="handleInviteDraft"
                    >
                      초대 목록에 추가
                    </button>
                  </article>

                  <article v-if="inviteDispatchLogs.length" class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <h3 class="text-sm font-semibold text-slate-900">초대 발송 결과</h3>

                    <div class="mt-4 grid gap-2">
                      <div
                        v-for="log in inviteDispatchLogs"
                        :key="log.id"
                        class="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600"
                      >
                        <strong class="font-semibold text-slate-900">{{ log.channel }}</strong>
                        <span class="ml-2">{{ log.target }} - {{ log.status }}</span>
                      </div>
                    </div>
                  </article>
                </div>
              </div>

              <p v-if="teamGroupError" class="mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-600">
                {{ teamGroupError }}
              </p>
              <p v-if="teamGroupMessage" class="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
                {{ teamGroupMessage }}
              </p>

              <div class="mt-6 flex justify-end">
                <button
                  type="button"
                  class="inline-flex items-center justify-center rounded-xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
                  @click="handleTeamGroupsSave"
                >
                  저장
                </button>
              </div>
            </template>

            <template v-else>
              <div class="border-b border-slate-200 pb-6">
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">MYPAGE_003</p>
                <h2 class="mt-2 text-2xl font-bold tracking-tight text-slate-900">팀 정보 수정</h2>
                <p class="mt-2 text-sm text-slate-500">
                  관리자는 팀명, 팀 설명, 담당 부서를 수정할 수 있으며 저장 시 팀 전체에 즉시 반영됩니다.
                </p>
              </div>

              <div class="mt-6 grid gap-4">
                <label class="grid gap-2">
                  <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">팀명</span>
                  <input
                    v-model="teamInfoForm.teamName"
                    type="text"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </label>

                <label class="grid gap-2">
                  <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">팀 설명</span>
                  <textarea
                    v-model="teamInfoForm.description"
                    rows="4"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium leading-6 text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </label>

                <label class="grid gap-2">
                  <span class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">담당 부서</span>
                  <input
                    v-model="teamInfoForm.department"
                    type="text"
                    class="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 outline-none transition focus:border-slate-300 focus:bg-white focus:ring-4 focus:ring-slate-900/5"
                  />
                </label>
              </div>

              <p v-if="teamInfoMessage" class="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
                {{ teamInfoMessage }}
              </p>

              <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-5">
                <h3 class="text-sm font-semibold text-slate-900">현재 반영된 팀 정보</h3>
                <div class="mt-3 grid gap-2 text-sm text-slate-600">
                  <p>팀명: {{ savedTeamInfo.teamName }}</p>
                  <p>팀 설명: {{ savedTeamInfo.description }}</p>
                  <p>담당 부서: {{ savedTeamInfo.department }}</p>
                </div>
              </div>

              <div class="mt-6 flex justify-end">
                <button
                  type="button"
                  class="inline-flex items-center justify-center rounded-xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
                  @click="handleTeamInfoSave"
                >
                  저장
                </button>
              </div>
            </template>
          </article>
        </main>
      </div>
    </div>
  </section>
</template>
