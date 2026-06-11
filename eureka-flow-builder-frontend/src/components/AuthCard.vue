<template>
  <div class="auth-page">
    <div class="auth-bg-grid" aria-hidden="true"></div>
    <div class="auth-glow" aria-hidden="true"></div>
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <img src="@/assets/shark.svg" alt="Logo" class="auth-logo" />
          <h1 class="auth-title">Eureka Flow Builder</h1>
          <p class="auth-slogan">零代码，生成完整应用</p>
        </div>
        <div class="auth-divider" aria-hidden="true"></div>
        <div class="auth-body">
          <slot />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
  min-height: calc(100vh - 64px - 72px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  overflow: hidden;
  background: linear-gradient(
    125deg,
    #020617 0%,
    #0f172a 28%,
    #1e1b4b 55%,
    #0c4a6e 82%,
    #06b6d4 120%
  );
}

.auth-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.06) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 80% 70% at 50% 40%, black 20%, transparent 75%);
  pointer-events: none;
}

.auth-glow {
  position: absolute;
  top: -30%;
  right: -8%;
  width: 480px;
  height: 480px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.22) 0%, transparent 70%);
  pointer-events: none;
}

.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 440px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  padding: 36px 32px 32px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  backdrop-filter: blur(12px);
}

.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-logo {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
}

.auth-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
  color: #0f172a;
  letter-spacing: 0.01em;
}

.auth-slogan {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.auth-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e2e8f0 20%, #e2e8f0 80%, transparent);
  margin-bottom: 28px;
}

.auth-body :deep(.ant-form-item) {
  margin-bottom: 20px;
}

.auth-body :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.auth-body :deep(.ant-input-lg),
.auth-body :deep(.ant-input-affix-wrapper-lg) {
  border-radius: 10px;
  padding: 8px 12px;
}

.auth-body :deep(.ant-input-affix-wrapper-lg) {
  padding: 0 12px;
}

.auth-body :deep(.ant-input-affix-wrapper-lg .ant-input) {
  padding: 8px 0;
}

.auth-body :deep(.auth-tips) {
  margin-bottom: 20px;
  color: #64748b;
  font-size: 14px;
  text-align: right;
}

.auth-body :deep(.auth-tips a) {
  color: #1890ff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.auth-body :deep(.auth-tips a:hover) {
  color: #06b6d4;
}

.auth-body :deep(.auth-submit-btn) {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #0ea5e9 0%, #6366f1 100%);
  border: none;
  transition: all 0.25s ease;
}

.auth-body :deep(.auth-submit-btn:hover:not(:disabled)) {
  background: linear-gradient(135deg, #0284c7 0%, #4f46e5 100%);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.35);
}

@media (max-width: 768px) {
  .auth-page {
    padding: 32px 16px;
    min-height: calc(100vh - 64px - 100px);
  }

  .auth-card {
    padding: 28px 20px 24px;
    border-radius: 14px;
  }

  .auth-title {
    font-size: 20px;
  }
}

@media (max-width: 375px) {
  .auth-page {
    padding: 24px 12px;
  }

  .auth-card {
    padding: 24px 16px 20px;
  }
}
</style>
