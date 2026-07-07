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
  const notice = route.query.notice;

  if (!token) {
    router.replace("/auth/login");
    return;
  }

  localStorage.setItem("ACCESS_TOKEN", token);
  localStorage.setItem("USERNAME", username);

  const roleList = roles ? roles.split(",") : [];

  localStorage.setItem("USER_ROLE", JSON.stringify(roleList));

  localStorage.setItem(
    "USER_PERMISSIONS",
    JSON.stringify(
      permissions && permissions.length
        ? permissions.split(",")
        : []
    )
  );

  window.dispatchEvent(new Event("auth-changed"));

  // Nếu backend báo tài khoản vừa được tự động liên kết vào tài khoản
  // đã tồn tại từ trước -> lưu tạm vào sessionStorage để trang đích
  // (thường là trang chủ) đọc và hiển thị thông báo cho người dùng.
  // Dùng sessionStorage thay vì query param vì sẽ redirect sang route khác ngay sau đây.
  if (notice === "account_linked") {
    sessionStorage.setItem(
      "PENDING_NOTICE",
      JSON.stringify({
        type: "success",
        title: "Đã liên kết tài khoản",
        message:
          "Email này đã có tài khoản trên hệ thống. Chúng tôi đã tự động liên kết đăng nhập mạng xã hội vào tài khoản đó.",
      })
    );
  }

  if (roleList.includes("ROLE_ADMIN") || roleList.includes("ROLE_STAFF")) {
    router.replace("/admin/dashboard");
  } else {
    router.replace("/");
  }
});
</script>

<style scoped>
.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  font-size: 16px;
  color: #777;
}
</style>