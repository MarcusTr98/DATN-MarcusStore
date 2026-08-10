<template>
  <header class="admin-page-header">
    <div class="admin-page-header__identity">
      <span class="admin-page-header__icon">
        <i :class="icon"></i>
      </span>
      <div>
        <span class="admin-page-header__eyebrow">
          <i v-if="eyebrowIcon" :class="eyebrowIcon"></i>
          {{ eyebrow }}
        </span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </div>

    <div v-if="$slots.actions" class="admin-page-header__actions">
      <slot name="actions"></slot>
    </div>
  </header>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, required: true },
  eyebrowIcon: { type: String, default: '' },
  title: { type: String, required: true },
  description: { type: String, required: true },
  icon: { type: String, required: true },
})
</script>

<style scoped>
.admin-page-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  min-height: 132px;
  padding: 24px 28px;
  border: 1px solid #c7dcef;
  border-radius: 20px;
  background:
    radial-gradient(circle at 88% 10%, rgba(96, 165, 250, 0.16), transparent 24%),
    linear-gradient(110deg, #eaf4ff 0%, #f4f9ff 52%, #ffffff 100%);
  box-shadow: 0 10px 28px rgba(30, 91, 156, 0.09);
}

.admin-page-header::after {
  content: '';
  position: absolute;
  right: 7%;
  top: -82px;
  width: 190px;
  height: 190px;
  border: 34px solid rgba(23, 105, 202, 0.04);
  border-radius: 50%;
  pointer-events: none;
}

.admin-page-header__identity {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.admin-page-header__icon {
  display: grid;
  place-items: center;
  flex: 0 0 58px;
  height: 58px;
  border: 1px solid #cfe1f8;
  border-radius: 17px;
  color: #1769ca;
  background: #eaf3ff;
  box-shadow: inset 0 0 0 5px rgba(255, 255, 255, 0.58);
  font-size: 25px;
}

.admin-page-header__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 5px;
  color: #1769ca;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-page-header h1 {
  margin: 0;
  color: #10233f;
  font-size: clamp(23px, 2.2vw, 29px);
  font-weight: 800;
  line-height: 1.25;
}

.admin-page-header p {
  max-width: 720px;
  margin: 6px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.55;
}

.admin-page-header__actions {
  position: relative;
  z-index: 1;
  flex: 0 0 auto;
}

.admin-page-header__actions :deep(button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 42px;
  padding: 0 17px;
  border: 1px solid #1769ca;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(135deg, #2478d4, #1769ca);
  box-shadow: 0 7px 16px rgba(23, 105, 202, 0.2);
  font-size: 13px;
  font-weight: 800;
  transition: 0.2s ease;
}

.admin-page-header__actions :deep(button:hover:not(:disabled)) {
  border-color: #1259ad;
  background: linear-gradient(135deg, #1d6ec7, #1259ad);
  transform: translateY(-1px);
}

.admin-page-header__actions :deep(button:disabled) {
  cursor: not-allowed;
  opacity: 0.55;
}

@media (max-width: 720px) {
  .admin-page-header {
    align-items: flex-start;
    flex-direction: column;
    padding: 22px;
  }

  .admin-page-header__identity {
    align-items: flex-start;
  }

  .admin-page-header__actions,
  .admin-page-header__actions :deep(button) {
    width: 100%;
  }
}
</style>
