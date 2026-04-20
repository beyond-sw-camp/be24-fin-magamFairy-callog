export const operationDomainTabs = [
  {
    value: 'workload',
    label: '업무관리',
    requirementRange: 'WORKLOAD_001 ~ WORKLOAD_003',
  },
  {
    value: 'customers',
    label: '고객관리',
    requirementRange: 'CUSTOMER_001 ~ CUSTOMER_005',
  },
  {
    value: 'tasknotes',
    label: '업무일지',
    requirementRange: 'TASKNOTE_001 ~ TASKNOTE_002',
  },
]

export const operationRoleOptions = [
  { value: 'admin', label: '관리자' },
  { value: 'user', label: '사용자' },
]

export const contentGeneratorBlueprints = [
  {
    title: '브랜드 월간 소식 정리',
    customer: '켈로그 본사',
    contentType: '기본 생성',
    summary: '운영 허브에서 자동 배분 규칙으로 생성한 기본 콘텐츠 카드입니다.',
    description: '월간 운영 흐름에 맞춰 우선순위 요일과 팀 부하를 고려해 자동 배정되었습니다.',
    priority: 'medium',
    assigneeId: 'jiwon',
    palette: { accent: '#2f80ed', surface: '#e8f2ff', text: '#1f5cb7' },
  },
  {
    title: '고객 사례 카드뉴스',
    customer: '에듀브릿지',
    contentType: '기본 생성',
    summary: '고객 사례를 카드뉴스 포맷으로 분리해 제작하는 작업입니다.',
    description: '브랜드 메시지와 QA 포인트를 한 장씩 끊어 제작할 수 있도록 기본 구조를 생성했습니다.',
    priority: 'high',
    assigneeId: 'hayoon',
    palette: { accent: '#00b8d9', surface: '#e5fbff', text: '#0f768d' },
  },
  {
    title: 'QA 후속 반영 패키지',
    customer: '애드링크 리테일',
    contentType: '기본 생성',
    summary: 'QA 이후 수정 포인트를 묶어 운영 보드로 연결하는 카드입니다.',
    description: '수정 요청과 재검토 체크리스트를 함께 담아 후속 배포가 이어지도록 구성했습니다.',
    priority: 'critical',
    assigneeId: 'taeyoung',
    palette: { accent: '#df5f75', surface: '#ffebef', text: '#b24356' },
  },
  {
    title: '내부 회의 아웃라인 정리',
    customer: '내부 보드',
    contentType: '기본 생성',
    summary: '내부 회의 결과를 다시 콘텐츠 카드로 연결하는 기본 작업입니다.',
    description: '회의록 승인 이후 후속 작업으로 바로 이어질 수 있게 핵심 액션을 카드화합니다.',
    priority: 'medium',
    assigneeId: 'jaewon',
    palette: { accent: '#7c4dff', surface: '#f0eaff', text: '#5330c8' },
  },
  {
    title: '주간 발행 체크 포인트',
    customer: '아이러닝',
    contentType: '기본 생성',
    summary: '주간 발행 전 체크해야 할 포인트를 패널 형태로 묶은 카드입니다.',
    description: '요일별 발행 분산과 마감 위험도를 함께 확인할 수 있도록 기본 생성되었습니다.',
    priority: 'low',
    assigneeId: 'minseo',
    palette: { accent: '#59c36d', surface: '#e9faee', text: '#2a8c49' },
  },
]

export const customerSeed = [
  {
    id: 'customer-kellogg',
    name: '켈로그 본사',
    ownerId: 'hayoon',
    segment: '식품 브랜드',
    email: 'brand.kr@kellogg.com',
    tone: '브랜드 톤 가이드 엄수',
    health: 'stable',
    lastQaUpdate: '2026-04-14',
    tags: ['브랜드', '리포트', 'QA 중요'],
    traits: ['승인 속도 빠름', '표현 가이드 엄격', '월간 보고 빈도 높음'],
    memo:
      '월요일 오전 공유본을 선호하며, 수정 요청이 있을 경우 QA 항목을 별도 체크리스트로 분리해 전달하는 것을 좋아합니다.',
    history: [
      {
        id: 'history-kellogg-01',
        type: 'update',
        title: '고객 톤 가이드 업데이트',
        detail: '브랜드 표현 금칙어와 CTA 규칙을 최신 가이드에 맞춰 정리했습니다.',
        createdAt: '2026-04-14 09:20',
      },
      {
        id: 'history-kellogg-02',
        type: 'qa',
        title: 'QA 리스트 기반 메모 반영',
        detail: '월간 리포트 요청 시 승인 담당자를 반드시 첫 줄에 표기하도록 메모가 추가되었습니다.',
        createdAt: '2026-04-13 17:40',
      },
    ],
  },
  {
    id: 'customer-edu',
    name: '에듀브릿지',
    ownerId: 'jaewon',
    segment: '교육 서비스',
    email: 'ops@edubridge.kr',
    tone: '성과 중심, 빠른 검토 선호',
    health: 'watch',
    lastQaUpdate: '2026-04-12',
    tags: ['교육', '성과형', '랜딩페이지'],
    traits: ['수치 중심 카피 선호', '속도 우선', '랜딩 수정 빈도 높음'],
    memo:
      '광고 소재 변경 주기가 빠르기 때문에 QA 요청이 들어오면 추천 담당자를 함께 제시해주는 흐름이 효율적입니다.',
    history: [
      {
        id: 'history-edu-01',
        type: 'create',
        title: '신규 고객 메모 정리',
        detail: '회의록과 기존 운영 노트를 합쳐 기본 고객 프로필을 만들었습니다.',
        createdAt: '2026-04-12 13:10',
      },
    ],
  },
  {
    id: 'customer-adlink',
    name: '애드링크 리테일',
    ownerId: 'minseo',
    segment: '리테일 마케팅',
    email: 'client@adlink-retail.com',
    tone: '실행 속도 우선, 전달 문장 짧게',
    health: 'risk',
    lastQaUpdate: '2026-04-15',
    tags: ['리테일', '드래그 수정', '긴급'],
    traits: ['긴급 수정 잦음', '짧은 카피 선호', '발행 일정 유동적'],
    memo:
      '담당자 변경과 일정 이동이 잦아서 카드 히스토리와 알림 연동이 중요합니다. 승인 요청은 관리자에게 먼저 올려야 합니다.',
    history: [
      {
        id: 'history-adlink-01',
        type: 'risk',
        title: '마감 위험 고객으로 표시',
        detail: '최근 2주 동안 긴급 일정 이동이 반복되어 위험 태그가 추가되었습니다.',
        createdAt: '2026-04-15 08:30',
      },
    ],
  },
  {
    id: 'customer-ilearning',
    name: '아이러닝',
    ownerId: 'hayoon',
    segment: '에듀테크',
    email: 'support@ilearning.kr',
    tone: '설명형 문장, 친절한 가이드 선호',
    health: 'stable',
    lastQaUpdate: '2026-04-11',
    tags: ['에듀테크', '콘텐츠 생성', '주간 운영'],
    traits: ['장문 설명 허용', '템플릿 활용도 높음', '주간 피드백 선호'],
    memo:
      '기본 생성 카드와 주간 일정 점검 카드를 자주 요청합니다. 업무 일지 쪽에서 개인/팀 일정 공유가 잘 맞는 고객입니다.',
    history: [
      {
        id: 'history-ilearning-01',
        type: 'update',
        title: '고객 선호 포맷 추가',
        detail: '주간 공지형 포맷을 별도 템플릿 선호 목록에 등록했습니다.',
        createdAt: '2026-04-11 15:50',
      },
    ],
  },
]

export const customerSuggestionSeed = [
  {
    id: 'suggestion-kellogg-01',
    customerId: 'customer-kellogg',
    source: 'QA 리스트 4월 2주차',
    title: '리포트 요청 시 승인자 표기 자동화',
    summary:
      'QA 기록에서 승인 담당자 누락이 반복되어, 요청 초안에 기본 승인자 표기를 먼저 붙이는 것이 좋다는 제안입니다.',
    suggestedTags: ['승인 플로우', '리포트'],
    suggestedTraits: ['승인자 명시 선호'],
    suggestedMemo:
      '월간 보고 카드 생성 시 첫 줄에 승인 담당자와 공유 마감 시간을 자동 기입하도록 제안합니다.',
    createdAt: '2026-04-15 10:20',
    status: 'pending',
  },
  {
    id: 'suggestion-edu-01',
    customerId: 'customer-edu',
    source: 'QA 리스트 4월 1주차',
    title: '추천 담당자 자동 노출',
    summary:
      '수정 요청이 빈번한 랜딩 작업은 QA 저장 시 추천 담당자를 함께 보여주는 흐름이 적합하다는 분석 결과입니다.',
    suggestedTags: ['QA 자동화', '추천 담당자'],
    suggestedTraits: ['추천 담당자 수용도 높음'],
    suggestedMemo:
      'QA 업무 저장 시 추천 담당자와 수정 범위를 한 블록으로 묶어 보여주면 처리 속도가 빨라질 가능성이 높습니다.',
    createdAt: '2026-04-14 18:05',
    status: 'pending',
  },
]

export const qaRequestSeed = [
  {
    id: 'qa-request-01',
    customerId: 'customer-edu',
    source: '카카오톡 플러스 채널',
    title: '랜딩 페이지 카피 수정 요청',
    sourceMessage:
      '어제 공유한 랜딩 초안에서 첫 섹션 문장을 좀 더 성과 중심으로 바꿔 주세요. 상담 신청 CTA는 더 강하게 보였으면 좋겠어요.',
    aiSummary:
      '고객은 랜딩 첫 문단의 메시지를 성과형 표현으로 전환하고 CTA 강조를 원합니다. 기존 고객 메모 기준으로 추천 담당자는 Jiwon Park입니다.',
    targetTaskId: 'CONTENTLIST_003',
    targetTaskLabel: '속성 설정/수정',
    recommendedAssigneeId: 'jiwon',
    requestedChanges: [
      '첫 섹션 헤드라인 성과형 표현으로 수정',
      'CTA 버튼 문구를 신청 중심으로 변경',
      '기존 고객 톤과 수치 표현 가이드 재점검',
    ],
    dueDate: '2026-04-18',
    status: 'draft',
    linkedTaskId: null,
  },
  {
    id: 'qa-request-02',
    customerId: 'customer-adlink',
    source: '카카오톡 플러스 채널',
    title: '주간 프로모션 카드 긴급 수정',
    sourceMessage:
      '이번 주 프로모션 문구가 바뀌어서 카드뉴스 2장, 4장 카피를 빠르게 교체해야 해요. 일정이 타이트해서 우선순위만 먼저 정리 부탁드립니다.',
    aiSummary:
      '긴급 카피 변경 요청이며, 일정 위험도가 높습니다. 드래그 수정과 재배정이 필요한 카드로 판단되어 Minseo Oh를 추천 담당자로 추출했습니다.',
    targetTaskId: 'CONTENTLIST_004',
    targetTaskLabel: '날짜/담당자 드래그 수정',
    recommendedAssigneeId: 'minseo',
    requestedChanges: [
      '프로모션 카드뉴스 2장, 4장 카피 교체',
      '주간 발행 일정 재배치 필요',
      '긴급 플래그와 관리자 승인 요청 동시 생성',
    ],
    dueDate: '2026-04-16',
    status: 'submitted',
    linkedTaskId: null,
  },
]

export const meetingNoteSeed = [
  {
    id: 'meeting-note-01',
    title: '업무 회의: 4월 발행 구조 조정',
    organizerId: 'jaewon',
    meetingDate: '2026-04-15',
    customer: '내부 보드',
    attendees: [
      { memberId: 'jaewon', approved: true },
      { memberId: 'jiwon', approved: true },
      { memberId: 'sumin', approved: false },
      { memberId: 'minseo', approved: false },
    ],
    transcript:
      '월간 발행 일정이 금요일에 몰려 있어 월, 수, 금을 중심으로 재분산하기로 합의함. 고객 QA 요청은 추천 담당자를 함께 보여주는 방식으로 정리 필요.',
    aiSummary:
      '회의 결과, 기본 콘텐츠 생성 규칙을 월-수-금 우선으로 적용하고 QA 요청은 담당자 추천과 함께 카드화하는 프로세스를 운영 허브에 반영해야 합니다.',
    suggestedTaskTitle: '4월 발행 구조 재정렬',
    suggestedDescription:
      '회의 승인 이후 월간 발행 일정을 재분산하고 QA 추천 담당자 노출 패널을 보강하는 후속 작업입니다.',
    linkedTaskId: null,
  },
]

export const taskNoteSeed = {
  completedMarks: ['CONTENTLIST_001'],
  statusRequests: [
    {
      id: 'status-request-01',
      taskId: 'CONTENTLIST_007',
      nextStatus: 'review',
      reason: '변경 로그 구조가 정리되어 다음 단계 검토가 필요합니다.',
      state: 'pending',
      createdAt: '2026-04-15 11:10',
    },
  ],
}
