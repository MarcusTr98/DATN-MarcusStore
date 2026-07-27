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
    linear-gradient(100deg, rgba(232, 242, 255, 0.82), rgba(255, 255, 255, 0) 46%),
    #fff;
  box-shadow: 0 8px 26px rgba(15, 35, 64, 0.06);
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
  border: 1px solid #c5d9f1;
  border-radius: 12px;
  color: #1559a5;
  background: #fff;
  box-shadow: 0 4px 12px rgba(21, 89, 165, 0.08);
  font-size: 13px;
  font-weight: 800;
  transition: 0.2s ease;
}

.admin-page-header__actions :deep(button:hover:not(:disabled)) {
  border-color: #1769ca;
  background: #f4f9ff;
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
