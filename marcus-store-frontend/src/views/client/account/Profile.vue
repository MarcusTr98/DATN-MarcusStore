<template>
  <div class="container py-5">
    <div class="row g-4">
      <div class="col-lg-4">
        <div class="card border-0 shadow-sm p-4 text-center rounded-3 bg-white">
          <div class="mb-3">
            <i class="fas fa-user-circle text-secondary display-1"></i>
          </div>
          <h5 class="fw-bold text-dark mb-1">Marcus Tran</h5>
          <span class="badge bg-light text-danger border border-danger rounded-pill px-3"
            >Thành viên Bạc</span
          >
        </div>
      </div>
      <div class="col-lg-8">
        <div class="card border-0 shadow-sm p-4 rounded-3 bg-white">
          <h5 class="fw-bold text-dark mb-4 border-bottom pb-2">Thông tin tài khoản</h5>
          <form>
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Tài khoản đăng nhập</label>
                <input type="text" class="form-control" value="marcustran" disabled />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Họ và tên</label>
                <input type="text" class="form-control" value="Marcus Tran" />
              </div>
            </div>
            <div class="row g-3 mb-4">
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Số điện thoại</label>
                <input type="tel" class="form-control" value="0907640098" />
              </div>
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Email liên hệ</label>
                <input type="email" class="form-control" value="support@marcus.com" disabled />
              </div>
            </div>
            <button
              type="button"
              class="btn btn-danger rounded-pill px-4"
              style="background-color: #e11d1d"
            >
              Lưu thay đổi
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
