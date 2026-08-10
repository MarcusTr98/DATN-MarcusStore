<template>
  <div class="cp-page">
    <div class="cp-card">

      <div class="cp-logo">
        <div class="cp-logo-box" :class="{ 'has-site-logo': siteLogoUrl }">
          <img v-if="siteLogoUrl" :src="siteLogoUrl" :alt="siteName" class="site-logo-image" />
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round"
            stroke-linejoin="round" style="width:22px;height:22px;">
            <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
            <line x1="12" y1="18" x2="12.01" y2="18" />
          </svg>
        </div>
        <span class="cp-logo-name">{{ siteName }}</span>
      </div>

      <p class="cp-title">Đổi mật khẩu</p>
      <p class="cp-sub">Nhập mật khẩu hiện tại và mật khẩu mới của bạn</p>

      <div class="cp-field">
        <label class="cp-label">Mật khẩu hiện tại</label>
        <div class="cp-input-wrap">
          <input :type="show.current ? 'text' : 'password'" v-model="form.currentPassword"
            placeholder="Nhập mật khẩu hiện tại" class="cp-input" />
          <span class="cp-eye" @click="show.current = !show.current">
            <svg v-if="!show.current" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px;">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
              stroke-linejoin="round" style="width:18px;height:18px;">
              <line x1="17.94" y1="11.12" x2="12" y2="17.06" />
              <line x1="6.06" y1="6.06" x2="1" y2="1" />
              <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
              <path d="M5.61 5.61A13.526 13.526 0 0 0 1 12s4 8 11 8a9.74 9.74 0 0 0 5.39-1.61" />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </span>
        </div>
      </div>

      <div class="cp-field">
        <label class="cp-label">Mật khẩu mới</label>
        <div class="cp-input-wrap">
          <input :type="show.new ? 'text' : 'password'" v-model="form.newPassword" placeholder="Ít nhất 6 ký tự"
            class="cp-input" />
          <span class="cp-eye" @click="show.new = !show.new">
            <svg v-if="!show.new" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px;">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
              stroke-linejoin="round" style="width:18px;height:18px;">
              <line x1="17.94" y1="11.12" x2="12" y2="17.06" />
              <line x1="6.06" y1="6.06" x2="1" y2="1" />
              <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
              <path d="M5.61 5.61A13.526 13.526 0 0 0 1 12s4 8 11 8a9.74 9.74 0 0 0 5.39-1.61" />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </span>
        </div>
        <div class="cp-strength-bars" v-if="form.newPassword">
          <div class="cp-bar" :class="strengthClass(1)"></div>
          <div class="cp-bar" :class="strengthClass(2)"></div>
          <div class="cp-bar" :class="strengthClass(3)"></div>
        </div>
        <p class="cp-strength-label" v-if="form.newPassword">{{ strengthLabel }}</p>
      </div>

      <div class="cp-field">
        <label class="cp-label">Xác nhận mật khẩu mới</label>
        <div class="cp-input-wrap">
          <input :type="show.confirm ? 'text' : 'password'" v-model="form.confirmPassword"
            placeholder="Nhập lại mật khẩu mới" class="cp-input" />
          <span class="cp-eye" @click="show.confirm = !show.confirm">
            <svg v-if="!show.confirm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" style="width:18px;height:18px;">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"
              stroke-linejoin="round" style="width:18px;height:18px;">
              <line x1="17.94" y1="11.12" x2="12" y2="17.06" />
              <line x1="6.06" y1="6.06" x2="1" y2="1" />
              <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
              <path d="M5.61 5.61A13.526 13.526 0 0 0 1 12s4 8 11 8a9.74 9.74 0 0 0 5.39-1.61" />
              <line x1="1" y1="1" x2="23" y2="23" />
            </svg>
          </span>
        </div>
      </div>

      <hr class="cp-divider" />

      <button class="cp-btn" @click="submit" :disabled="loading">
        {{ loading ? "Đang xử lý..." : "Xác nhận đổi mật khẩu" }}
      </button>

      <div class="cp-notice">
        <div class="cp-dot"></div>
        <span>Hệ thống sẽ tự động đăng xuất sau khi đổi mật khẩu. Vui lòng đăng nhập lại bằng mật khẩu mới.</span>
      </div>

    </div>
  </div>

  <BaseModal :visible="modalVisible" :type="modalType" :title="modalTitle" :message="modalMessage"
    @close="closeModal" />
</template>
<script setup>
import { changePassword } from "@/api/authApi";
import BaseModal from "@/components/BaseModal.vue";
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useSettings } from "@/composables/useSettings";

const { siteName, siteLogoUrl, fetchSettings } = useSettings();

const router = useRouter();
const loading = ref(false);
const form = ref({ currentPassword: "", newPassword: "", confirmPassword: "" });
const show = ref({ current: false, new: false, confirm: false });
const modalVisible = ref(false);
const modalType = ref("error");
const modalTitle = ref("");
const modalMessage = ref("");
const afterClose = ref(null);

const strengthScore = computed(() => {
  const v = form.value.newPassword;
  if (!v) return 0;
  let s = 0;
  if (v.length >= 6) s++;
  if (/[A-Z]/.test(v) && /[0-9]/.test(v)) s++;
  if (/[^A-Za-z0-9]/.test(v)) s++;
  return s;
});

const strengthLabel = computed(() => ["", "Yếu", "Trung bình", "Mạnh"][strengthScore.value] || "");

const strengthClass = (bar) => {
  const s = strengthScore.value;
  if (s === 0 || bar > s) return "";
  if (s === 1) return "weak";
  if (s === 2) return "medium";
  return "strong";
};

const showModal = (type, title, message, callback = null) => {
  modalType.value = type;
  modalTitle.value = title;
  modalMessage.value = message;
  modalVisible.value = true;
  afterClose.value = callback;
};

const closeModal = () => {
  modalVisible.value = false;
  if (afterClose.value) { afterClose.value(); afterClose.value = null; }
};

onMounted(() => {
  fetchSettings();
  if (!localStorage.getItem("ACCESS_TOKEN")) router.replace("/auth/login");
});

const submit = async () => {
  if (!form.value.currentPassword.trim()) {
    return showModal(
      "error",
      "Thiếu mật khẩu hiện tại",
      "Vui lòng nhập mật khẩu hiện tại."
    );
  }


  if (!form.value.newPassword.trim()) {
    return showModal(
      "error",
      "Thiếu mật khẩu mới",
      "Vui lòng nhập mật khẩu mới."
    );
  }


  if (!form.value.confirmPassword.trim()) {
    return showModal(
      "error",
      "Thiếu xác nhận mật khẩu",
      "Vui lòng nhập lại mật khẩu mới."
    );
  }


  if (form.value.newPassword.length < 6) {
    return showModal(
      "error",
      "Mật khẩu quá ngắn",
      "Mật khẩu mới phải có ít nhất 6 ký tự."
    );
  }

  try {
    loading.value = true;

    const response = await changePassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
      confirmPassword: form.value.confirmPassword
    });

    showModal(
      "success",
      "Đổi mật khẩu thành công",
      response.data?.message ||
        "Mật khẩu của bạn đã được cập nhật thành công.",
      () => {
        localStorage.removeItem("ACCESS_TOKEN");
        router.replace("/auth/login");
      }
    );

  } catch (error) {

    const status = error.response?.status;
    const message =
      error.response?.data?.message ||
      error.response?.data?.error ||
      "Có lỗi xảy ra. Vui lòng thử lại.";

    // Token hết hạn
    if (status === 401 || status === 403) {
      localStorage.removeItem("ACCESS_TOKEN");

      return showModal(
        "error",
        "Phiên đăng nhập hết hạn",
        "Vui lòng đăng nhập lại để tiếp tục.",
        () => {
          router.replace("/auth/login");
        }
      );
    }

    if (
      message.includes("current password") ||
      message.includes("Mật khẩu hiện tại")
    ) {
      return showModal(
        "error",
        "Mật khẩu hiện tại không đúng",
        message
      );
    }


    if (
      message.includes("trùng mật khẩu cũ") ||
      message.includes("must not match")
    ) {
      return showModal(
        "error",
        "Mật khẩu mới không hợp lệ",
        message
      );
    }

    if (
      message.includes("xác nhận không khớp") ||
      message.toLowerCase().includes("confirm")
    ) {
      return showModal(
        "error",
        "Xác nhận mật khẩu không khớp",
        message
      );
    }


    if (
      message.includes("new password") ||
      message.includes("Mật khẩu mới")
    ) {
      return showModal(
        "error",
        "Mật khẩu mới không hợp lệ",
        message
      );
    }

    showModal(
      "error",
      "Đổi mật khẩu thất bại",
      message
    );

  } finally {
    loading.value = false;
  }
}
</script>
<style scoped>
.cp-page {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  box-sizing: border-box;
}

.cp-card {
  width: 420px;
  padding: 36px 32px 32px;
  background: #fff;
  border-radius: 20px;
  border: 0.5px solid #e8e8e8;
}

.cp-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
}

.cp-logo-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #d70018;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cp-logo-box.has-site-logo {
  padding: 6px;
  background: #ffffff;
  border: 1px solid #fee2e2;
  box-shadow: 0 4px 12px rgba(215, 0, 24, 0.1);
}

.cp-logo-box.has-site-logo .site-logo-image {
  border-radius: 6px;
}

.cp-logo-name {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
}

.cp-title {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
  color: #1a1a1a;
  margin: 0 0 6px;
}

.cp-sub {
  font-size: 13px;
  color: #999;
  text-align: center;
  margin: 0 0 28px;
}

.cp-field {
  margin-bottom: 16px;
}

.cp-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #444;
  margin-bottom: 6px;
}

.cp-input-wrap {
  position: relative;
}

.cp-input {
  width: 100%;
  height: 48px;
  padding: 0 44px 0 14px;
  border: 1.5px solid #ececec;
  border-radius: 12px;
  font-size: 14px;
  background: #fafafa;
  color: #1a1a1a;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s, background 0.15s;
}

.cp-input:focus {
  border-color: #d70018;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(215, 0, 24, 0.07);
}

.cp-input::placeholder {
  color: #bbb;
}

.cp-eye {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #bbb;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: color 0.15s;
}

.cp-eye:hover {
  color: #555;
}

.cp-strength-bars {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.cp-bar {
  flex: 1;
  height: 3px;
  border-radius: 2px;
  background: #ebebeb;
  transition: background 0.2s;
}

.cp-bar.weak {
  background: #d70018;
}

.cp-bar.medium {
  background: #f5a623;
}

.cp-bar.strong {
  background: #22a757;
}

.cp-strength-label {
  font-size: 11px;
  margin: 4px 0 0;
  color: #888;
}

.cp-divider {
  border: none;
  border-top: 1px solid #f0f0f0;
  margin: 22px 0;
}

.cp-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  background: #d70018;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.cp-btn:hover:not(:disabled) {
  background: #b8001a;
}

.cp-btn:active {
  transform: scale(0.99);
}

.cp-btn:disabled {
  background: #e5e5e5;
  color: #aaa;
  cursor: not-allowed;
}

.cp-notice {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fff5f5;
  border: 1px solid #ffd5d5;
  border-radius: 10px;
  padding: 11px 13px;
  margin-top: 14px;
}

.cp-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d70018;
  margin-top: 5px;
  flex-shrink: 0;
}

.cp-notice span {
  font-size: 12.5px;
  color: #c00016;
  line-height: 1.55;
}
</style>
