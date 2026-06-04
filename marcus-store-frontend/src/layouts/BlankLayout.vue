<template>
  <div class="blank-layout">
    <RouterView />
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
</script>

<style scoped>
.blank-layout {
  width: 100%;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  background-color: transparent;
}
</style>
