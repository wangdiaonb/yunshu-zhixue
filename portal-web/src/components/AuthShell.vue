<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    width?: string
  }>(),
  {
    subtitle: '',
    width: '440px'
  }
)
</script>

<template>
  <div class="auth-shell">
    <div class="auth-bg" aria-hidden="true">
      <span class="orb orb-1" />
      <span class="orb orb-2" />
    </div>

    <div class="auth-body">
      <router-link to="/" class="auth-brand">
        <span class="brand-mark" aria-hidden="true" />
        <span>云枢智学</span>
      </router-link>

      <section class="auth-card" :style="{ maxWidth: width }">
        <header class="auth-header">
          <h1>{{ title }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </header>
        <slot />
      </section>

      <p class="auth-footer">
        <router-link to="/">返回首页</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-shell {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: var(--ys-brand-gradient);
  overflow: hidden;
}

.auth-bg .orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.45;
}

.orb-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  left: -100px;
  background: #9db4ff;
}

.orb-2 {
  width: 360px;
  height: 360px;
  right: -80px;
  bottom: -120px;
  background: #e0b3ff;
}

.auth-body {
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 26px;
  font-weight: 700;
  color: #fff;
}

.auth-brand .brand-mark {
  font-size: 30px;
}

.auth-card {
  width: 100%;
  background: #fff;
  border-radius: var(--ys-radius-lg);
  box-shadow: var(--ys-shadow-lg);
  padding: 32px;
}

.auth-header {
  margin-bottom: 24px;
}

.auth-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--ys-text-primary);
}

.auth-header p {
  margin-top: 8px;
  color: var(--ys-text-secondary);
  font-size: 13px;
}

.auth-footer {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.auth-footer a {
  color: #fff;
  text-decoration: underline;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 24px 20px;
  }
}
</style>
