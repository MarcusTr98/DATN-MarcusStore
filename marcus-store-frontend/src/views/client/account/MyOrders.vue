<template>
  <div class="container py-5">
    <h4 class="fw-bold text-dark mb-4">
      <i class="fas fa-box-open me-2 text-danger"></i>Đơn hàng của tôi
    </h4>

    <div class="table-responsive bg-white rounded-3 shadow-sm border p-3">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light">
          <tr class="small text-secondary">
            <th>MÃ ĐƠN HÀNG</th>
            <th>NGÀY ĐẶT</th>
            <th>PHƯƠNG THỨC</th>
            <th>TỔNG TIỀN</th>
            <th>TRẠNG THÁI</th>
            <th>THAO TÁC</th>
          </tr>
        </thead>
        <tbody class="small text-dark">
          <tr>
            <td class="fw-bold text-danger">#MS-903214</td>
            <td>2026-05-27 14:20</td>
            <td>Thanh toán COD</td>
            <td class="fw-bold">32.000.000 ₫</td>
            <td>
              <span
                class="badge bg-warning-subtle text-warning border border-warning rounded-pill px-2 py-1"
                >Chờ xử lý</span
              >
            </td>
            <td>
              <router-link
                to="/my-orders/903214"
                class="btn btn-light btn-sm rounded-pill text-primary border px-3"
              >
                Chi tiết
              </router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
