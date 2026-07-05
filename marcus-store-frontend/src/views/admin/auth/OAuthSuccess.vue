<template>
  <div class="loading">
    Đang đăng nhập...
  </div>
</template>

<script setup>
import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();

onMounted(() => {
  const token = route.query.token;
  const username = route.query.username;
  const roles = route.query.roles;
  const permissions = route.query.permissions;

  if (!token) {
    router.replace("/auth/login");
    return;
  }

  localStorage.setItem("ACCESS_TOKEN", token);
  localStorage.setItem("USERNAME", username);

  localStorage.setItem(
    "USER_ROLE",
    JSON.stringify(roles ? roles.split(",") : [])
  );

  localStorage.setItem(
    "USER_PERMISSIONS",
    JSON.stringify(
      permissions && permissions.length
        ? permissions.split(",")
        : []
    )
  );

  window.dispatchEvent(new Event("auth-changed"));

  router.replace("/");
});
</script>