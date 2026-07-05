<template>
  <GlobalSpinner />
  <FaqOffcanvas />
  <router-view />
</template>

<script setup>
import GlobalSpinner from '@/components/common/GlobalSpinner.vue'
import FaqOffcanvas from '@/components/client/FaqOffcanvas.vue'
</script>
