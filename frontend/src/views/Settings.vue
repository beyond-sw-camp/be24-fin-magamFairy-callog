<script setup>
import { ref } from 'vue';

// 요구사항: 각 설정 항목은 현재 적용된 상태값을 기본값으로 표시한다.
const settings = ref({
  theme: 'light',
  notifications: {
    push: true,
    email: false
  }
});

const saveSettings = () => {
  // 실제 저장 로직 (API 호출 등)
  alert('설정이 저장되었습니다!');
  console.log('Saved Data:', settings.value);
};
</script>

<template>
  <div class="settings-container">
    <header class="settings-header">
      <h1>시스템 설정</h1>
      <p class="subtitle">개인 환경 및 알림 설정을 관리하세요.</p>
    </header>

    <main class="settings-content">
      <section class="settings-card">
        <h2 class="section-title">🎨 테마 및 UI 설정</h2>
        <div class="setting-item">
          <div class="info">
            <span class="label">기본 테마</span>
            <p class="description">애플리케이션의 메인 색상을 선택합니다.</p>
          </div>
          <div class="control">
            <select v-model="settings.theme" class="mint-select">
              <option value="light">화이트 (Light)</option>
              <option value="dark">다크 (Dark)</option>
              <option value="system">시스템 설정 따름</option>
            </select>
          </div>
        </div>
      </section>

      <section class="settings-card">
        <h2 class="section-title">🔔 알림 수신 설정</h2>
        <div class="setting-item">
          <div class="info">
            <span class="label">푸시 알림</span>
            <p class="description">브라우저 데스크톱 알림을 받습니다.</p>
          </div>
          <div class="control">
            <label class="mint-switch">
              <input type="checkbox" v-model="settings.notifications.push">
              <span class="slider"></span>
            </label>
          </div>
        </div>

        <div class="setting-item">
          <div class="info">
            <span class="label">이메일 마케팅</span>
            <p class="description">새로운 업데이트 정보를 이메일로 수신합니다.</p>
          </div>
          <div class="control">
            <label class="mint-switch">
              <input type="checkbox" v-model="settings.notifications.email">
              <span class="slider"></span>
            </label>
          </div>
        </div>
      </section>

      <footer class="settings-footer">
        <button @click="saveSettings" class="btn-save">설정 저장</button>
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* 화이트 & 민트 테마 스타일 정의 */
.settings-container {
  max-width: 600px;
  margin: 40px auto;
  padding: 20px;
  font-family: 'Pretendard', sans-serif;
  background-color: #ffffff;
  color: #333;
}

.settings-header {
  margin-bottom: 30px;
  border-bottom: 2px solid #f0fdfa; /* 매우 연한 민트 */
  padding-bottom: 15px;
}

.settings-header h1 {
  font-size: 24px;
  color: #0f766e; /* 딥 민트 */
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.settings-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 18px;
  margin-bottom: 15px;
  color: #14b8a6; /* 메인 민트 */
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.setting-item:last-child {
  border-bottom: none;
}

.label {
  font-weight: 600;
  display: block;
}

.description {
  font-size: 13px;
  color: #888;
}

/* 민트 포인트 컨트롤 스타일 */
.mint-select {
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #14b8a6;
  outline: none;
  cursor: pointer;
}

.mint-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 22px;
}

.mint-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0; left: 0; right: 0; bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 34px;
}

input:checked + .slider {
  background-color: #14b8a6; /* 스위치 온 상태 민트 */
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider:before {
  transform: translateX(22px);
}

.btn-save {
  width: 100%;
  padding: 12px;
  background-color: #14b8a6;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-save:hover {
  background-color: #0d9488;
}
</style>