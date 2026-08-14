<template>
  <div class="skug-page">
    <div class="skug-container">
      <!-- HEADER CHUẨN MASTER DATA -->
      <div class="skug-header">
        <div class="skug-header-left">
          <div class="skug-header-icon">
            <i class="fa-solid fa-layer-group"></i>
          </div>
          <div>
            <p class="skug-breadcrumb">Sản phẩm</p>
            <h1 class="skug-title">
              Tạo Biến thể SKU
              <span class="skug-badge"><span class="skug-badge-dot"></span>Master Data</span>
            </h1>
            <p class="skug-subtitle">Tạo và quản lý các biến thể SKU theo thuộc tính sản phẩm</p>
          </div>
        </div>

        <div class="skug-header-right">
          <div class="skug-steps">
            <div
              class="skug-step"
              :class="{ 'is-active': currentStep >= 1, 'is-done': currentStep > 1 }"
            >
              <span class="skug-step-num">1</span><span class="skug-step-label">Chọn SP</span>
            </div>
            <div class="skug-step-line" :class="{ 'is-active': currentStep > 1 }"></div>
            <div
              class="skug-step"
              :class="{ 'is-active': currentStep >= 2, 'is-done': currentStep > 2 }"
            >
              <span class="skug-step-num">2</span
              ><span class="skug-step-label">Chọn thuộc tính</span>
            </div>
            <div class="skug-step-line" :class="{ 'is-active': currentStep > 2 }"></div>
            <div class="skug-step" :class="{ 'is-active': currentStep >= 3 }">
              <span class="skug-step-num">3</span><span class="skug-step-label">Điền & Lưu</span>
            </div>
          </div>
        </div>
      </div>

      <!-- BƯỚC 1: CHỌN SẢN PHẨM -->
      <div class="skug-card" :class="{ 'is-collapsed': currentStep > 1 }">
        <div class="skug-card-header" @click="currentStep > 1 && (currentStep = 1)">
          <div class="skug-card-title-group">
            <span class="skug-step-badge">01</span>
            <div>
              <h3 class="skug-card-title">Sản phẩm gốc</h3>
              <p class="skug-card-subtitle">Chọn sản phẩm bạn muốn tạo biến thể SKU</p>
            </div>
          </div>
          <div v-if="selectedProduct" class="skug-selected-summary">
            <span class="skug-selected-tag">{{ selectedProduct.productName }}</span>
          </div>
        </div>

        <div class="skug-card-body" v-show="currentStep === 1">
          <div class="skug-toolbar">
            <div class="skug-search">
              <input
                v-model="searchQuery"
                placeholder="Nhập tên sản phẩm cần tìm..."
                @keyup.enter="handleSearch"
              />
              <button class="skug-btn-search" @click="handleSearch" title="Tìm kiếm">
                <i class="fa-solid fa-search"></i>
              </button>
            </div>

            <div>
              <select
                v-model="filterStatus"
                @change="handleFilterChange"
                class="skug-status-select"
              >
                <option value="all">Tất cả sản phẩm</option>
                <option value="no_sku">Chưa có biến thể</option>
                <option value="has_sku">Đã có biến thể</option>
              </select>
            </div>
          </div>

          <div class="skug-product-grid">
            <div
              v-for="p in products"
              :key="p.productId"
              class="skug-product-option"
              :class="{ 'is-active': selectedProductId === p.productId }"
              @click="selectProduct(p)"
            >
              <div class="skug-product-check">
                <i
                  v-if="selectedProductId === p.productId"
                  class="fa-solid fa-check"
                  style="color: #fff; font-size: 12px"
                ></i>
              </div>
              <div class="skug-product-thumb">
                {{ p.productName ? p.productName.charAt(0).toUpperCase() : 'P' }}
              </div>
              <div class="skug-product-info">
                <p class="skug-product-name">{{ p.productName }}</p>
                <p class="skug-product-brand">{{ p.brand }}</p>
              </div>
            </div>
            <div v-if="products.length === 0" class="skug-empty">
              <p>Không tìm thấy sản phẩm nào phù hợp.</p>
            </div>
          </div>

          <div class="skug-pagination" v-if="totalPages > 1">
            <button
              class="skug-btn-page"
              :disabled="currentPage === 0"
              @click="changePage(currentPage - 1)"
            >
              ← Trước
            </button>
            <span class="skug-page-info">Trang {{ currentPage + 1 }} / {{ totalPages }}</span>
            <button
              class="skug-btn-page"
              :disabled="currentPage >= totalPages - 1"
              @click="changePage(currentPage + 1)"
            >
              Sau →
            </button>
          </div>

          <div class="skug-card-footer">
            <button
              class="skug-btn-primary"
              :disabled="!selectedProductId"
              @click="currentStep = 2"
            >
              Tiếp tục →
            </button>
          </div>
        </div>
      </div>

      <!-- BẢNG SKU ĐÃ TỒN TẠI TRONG CSDL -->
      <div class="skug-card" v-if="selectedProductId && existingSkus.length > 0">
        <div class="skug-card-header no-pointer">
          <div class="skug-card-title-group">
            <span class="skug-step-badge" style="background: #ecfdf5; color: #10b981">
              <i class="fa-solid fa-database"></i>
            </span>
            <div>
              <h3 class="skug-card-title">SKU Đang Hoạt Động ({{ existingSkus.length }})</h3>
              <p class="skug-card-subtitle">
                Các biến thể đã tồn tại. Hệ thống sẽ khóa nếu bạn tạo trùng thuộc tính với bảng này!
              </p>
            </div>
          </div>
        </div>
        <div class="skug-card-body">
          <div class="skug-table-wrap">
            <table class="skug-table">
              <thead>
                <tr>
                  <th>Ảnh biến thể</th>
                  <th>Tổ hợp biến thể</th>
                  <th>Mã SKU</th>
                  <th style="width: 140px">Giá niêm yết (₫)</th>
                  <th style="width: 140px">Giá bán (₫)</th>
                  <th>Khối lượng</th>
                  <th>Tồn kho</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(sku, idx) in existingSkus" :key="sku.skuId" class="skug-row">
                  <td>
                    <button
                      type="button"
                      class="skug-sku-image"
                      :class="{ 'has-image': sku.skuImageUrl }"
                      title="Tải ảnh đại diện biến thể"
                      @click="openSkuImageModal(sku)"
                    >
                      <img v-if="sku.skuImageUrl" :src="sku.skuImageUrl" :alt="sku.skuCode" />
                      <i v-else class="fa-regular fa-image"></i>
                      <span>{{ sku.skuImageUrl ? 'Đổi ảnh' : 'Thêm ảnh' }}</span>
                    </button>
                  </td>
                  <td>
                    <div class="skug-variant-cell">
                      <span class="skug-variant-label">
                        {{
                          sku.attributeValues
                            ? sku.attributeValues.map((v) => v.valueString).join(' / ')
                            : '---'
                        }}
                      </span>
                    </div>
                  </td>
                  <td>
                    <code style="color: #db2777; font-weight: bold">{{ sku.skuCode }}</code>
                  </td>
                  <td style="color: #94a3b8; text-decoration: line-through">
                    {{ formatMoney(sku.originalPrice || sku.price) }}
                  </td>
                  <td style="font-weight: 600">{{ formatMoney(sku.price) }}</td>
                  <td>
                    <strong>{{ formatWeight(sku.weightGram) }}</strong>
                    <small class="skug-stock-source">Dùng tính phí GHN</small>
                  </td>
                  <td>
                    <span class="skug-stock-readonly">{{ sku.stockQuantity }}</span>
                    <small class="skug-stock-source">Quản lý tại Kho</small>
                  </td>
                  <td>
                    <div class="skug-row-actions">
                      <button
                        class="skug-btn-row-edit"
                        @click="openEditSku(sku, idx)"
                        title="Chỉnh giá SKU"
                      >
                        <i class="fa-solid fa-pen"></i>
                      </button>
                      <button
                        class="skug-btn-row-del"
                        @click="confirmDeleteSku(sku.skuId, idx)"
                        title="Vô hiệu hóa SKU"
                      >
                        <i class="fa-solid fa-trash"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- BƯỚC 2: CHỌN THUỘC TÍNH -->
      <div
        class="skug-card"
        :class="{ 'is-collapsed': currentStep !== 2, 'is-locked': currentStep < 2 }"
      >
        <div class="skug-card-header" @click="currentStep > 2 && (currentStep = 2)">
          <div class="skug-card-title-group">
            <span class="skug-step-badge" :class="{ 'is-dim': currentStep < 2 }">02</span>
            <div>
              <h3 class="skug-card-title">Chọn Thuộc tính & Giá trị</h3>
              <p class="skug-card-subtitle">Tick vào các giá trị muốn tạo biến thể</p>
            </div>
          </div>
          <div v-if="currentStep > 2" class="skug-selected-summary">
            <span class="skug-selected-tag">{{ getTotalSelected() }} giá trị đã chọn</span>
            <span class="skug-edit-hint">Nhấn để thay đổi</span>
          </div>
        </div>

        <div class="skug-card-body" v-show="currentStep === 2">
          <div class="skug-attrs-section">
            <div v-for="attr in attributes" :key="attr.attributeId" class="skug-attr-block">
              <div class="skug-attr-block-header">
                <label class="skug-attr-toggle">
                  <input
                    type="checkbox"
                    :checked="isAttributeSelected(attr.attributeId)"
                    @change="toggleAttribute(attr.attributeId)"
                  />
                  <span class="skug-toggle-track"><span class="skug-toggle-thumb"></span></span>
                  <span class="skug-attr-block-name">{{ attr.attributeName }}</span>
                </label>
                <span class="skug-attr-value-count">
                  {{ getSelectedValueCount(attr.attributeId) }}/{{
                    getAttrValues(attr.attributeId).length
                  }}
                  đã chọn
                </span>
              </div>

              <div class="skug-values-row" v-if="isAttributeSelected(attr.attributeId)">
                <button
                  v-for="val in getAttrValues(attr.attributeId)"
                  :key="val.valueId"
                  class="skug-value-tag"
                  :class="{ 'is-active': isValueSelected(val.valueId) }"
                  @click="toggleValue(val)"
                >
                  <span class="skug-value-dot" :style="getColorStyle(val.valueString)"></span>
                  {{ val.valueString }}
                  <i
                    v-if="isValueSelected(val.valueId)"
                    class="fa-solid fa-check"
                    style="font-size: 10px; margin-left: 4px"
                  ></i>
                </button>
              </div>
              <p v-else class="skug-attr-hint">Bật để chọn giá trị cho thuộc tính này</p>
            </div>
          </div>

          <div class="skug-preview-count" v-if="getTotalSelected() > 0">
            Hệ thống sẽ sinh ra <strong>{{ getCartesianCount() }} biến thể</strong>
          </div>

          <div class="skug-card-footer">
            <button class="skug-btn-cancel" @click="currentStep = 1">← Quay lại</button>
            <button
              class="skug-btn-primary"
              :disabled="getTotalSelected() === 0"
              @click="generateVariantsAndNext"
            >
              Tạo Ma trận SKU →
            </button>
          </div>
        </div>
      </div>

      <!-- BƯỚC 3: MA TRẬN SKU MỚI -->
      <div class="skug-card" :class="{ 'is-locked': currentStep < 3 }">
        <div class="skug-card-header no-pointer">
          <div class="skug-card-title-group">
            <span class="skug-step-badge" :class="{ 'is-dim': currentStep < 3 }">03</span>
            <div>
              <h3 class="skug-card-title">
                Ma trận SKU
                <span v-if="generatedSkus.length" class="skug-selected-tag"
                  >{{ generatedSkus.length }} biến thể</span
                >
              </h3>
              <p class="skug-card-subtitle">
                Điền giá, khối lượng đóng gói và chỉnh sửa mã SKU trước khi lưu
              </p>
            </div>
          </div>
        </div>

        <div class="skug-card-body" v-if="currentStep === 3 && generatedSkus.length > 0">
          <div class="skug-bulk-bar">
            <div class="skug-bulk-label">
              <i class="fa-solid fa-wand-magic-sparkles"></i> Áp dụng hàng loạt
            </div>
            <div class="skug-bulk-inputs">
              <div class="skug-bulk-field">
                <label
                  >Giá niêm yết (₫)
                  <span style="font-weight: normal; color: #888">(Tùy chọn)</span></label
                >
                <input
                  v-model="bulkOriginalPrice"
                  type="number"
                  min="1"
                  step="1"
                  placeholder="Trống = Giá bán"
                  class="skug-bulk-input"
                />
              </div>
              <div class="skug-bulk-field">
                <label>Giá bán (₫)</label>
                <input
                  v-model="bulkPrice"
                  type="number"
                  min="1"
                  step="1"
                  placeholder="VD: 25000000"
                  class="skug-bulk-input"
                />
              </div>
              <div class="skug-bulk-field">
                <label>Khối lượng (gram)</label>
                <input
                  v-model="bulkWeightGram"
                  type="number"
                  min="1"
                  max="50000"
                  step="1"
                  placeholder="VD: 500"
                  class="skug-bulk-input"
                />
              </div>
              <button class="skug-btn-bulk" @click="applyBulkSettings">Áp dụng tất cả</button>
            </div>
          </div>

          <div class="skug-table-wrap">
            <table class="skug-table">
              <thead>
                <tr>
                  <th>Biến thể</th>
                  <th>Mã SKU</th>
                  <th style="width: 140px">Giá niêm yết (₫)</th>
                  <th style="width: 140px">Giá bán (₫)</th>
                  <th style="width: 140px">Khối lượng (g)</th>
                  <th>Khởi tạo tồn kho</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(sku, index) in generatedSkus"
                  :key="index"
                  class="skug-row"
                  :class="{ 'is-duplicate-row': isComboExists(sku.valueIds) }"
                >
                  <td>
                    <div class="skug-variant-cell">
                      <div class="skug-variant-dots">
                        <span
                          v-for="(val, vi) in sku.comboValues"
                          :key="vi"
                          class="skug-variant-dot"
                          :style="getColorStyle(val)"
                          :title="val"
                        ></span>
                      </div>
                      <span class="skug-variant-label">{{ sku.variantName }}</span>
                      <span v-if="isComboExists(sku.valueIds)" class="skug-badge-duplicate">
                        <i class="fa-solid fa-triangle-exclamation"></i> Đã có trong DB
                      </span>
                    </div>
                  </td>

                  <td>
                    <input
                      v-model="sku.skuCode"
                      @input="clearFieldError(index, 'skuCode')"
                      class="skug-table-input is-mono"
                      :class="{
                        'is-invalid':
                          fieldErrors[`skus[${index}].skuCode`] ||
                          isDuplicateSku(sku.skuCode, index),
                      }"
                      placeholder="VD: IP16-BLK"
                    />
                    <div class="skug-error-text" v-if="isDuplicateSku(sku.skuCode, index)">
                      Mã SKU bị trùng lặp!
                    </div>
                    <div class="skug-error-text" v-else-if="fieldErrors[`skus[${index}].skuCode`]">
                      {{ fieldErrors[`skus[${index}].skuCode`] }}
                    </div>
                  </td>

                  <td>
                    <input
                      v-model="sku.originalPrice"
                      @input="clearFieldError(index, 'originalPrice')"
                      type="number"
                      class="skug-table-input is-muted"
                      :class="{ 'is-invalid': fieldErrors[`skus[${index}].originalPrice`] }"
                      placeholder="Trống = Giá bán"
                      min="1"
                      step="1"
                    />
                  </td>

                  <td>
                    <input
                      v-model="sku.price"
                      @input="clearFieldError(index, 'price')"
                      type="number"
                      class="skug-table-input"
                      :class="{ 'is-invalid': fieldErrors[`skus[${index}].price`] }"
                      placeholder="0"
                      min="1"
                      step="1"
                    />
                    <div class="skug-error-text" v-if="fieldErrors[`skus[${index}].price`]">
                      {{ fieldErrors[`skus[${index}].price`] }}
                    </div>
                  </td>

                  <td>
                    <input
                      v-model="sku.weightGram"
                      @input="clearFieldError(index, 'weightGram')"
                      type="number"
                      class="skug-table-input"
                      :class="{ 'is-invalid': fieldErrors[`skus[${index}].weightGram`] }"
                      min="1"
                      max="50000"
                      step="1"
                    />
                    <div class="skug-error-text" v-if="fieldErrors[`skus[${index}].weightGram`]">
                      {{ fieldErrors[`skus[${index}].weightGram`] }}
                    </div>
                  </td>

                  <td>
                    <span class="skug-zero-stock">0</span>
                    <small class="skug-stock-source"
                      >Nhập hàng tại module Kho sau khi tạo SKU</small
                    >
                  </td>

                  <td>
                    <button
                      class="skug-btn-row-del"
                      @click="generatedSkus.splice(index, 1)"
                      title="Xóa dòng này"
                    >
                      <i class="fa-solid fa-xmark"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="hasAnyDuplicate" class="skug-duplicate-warning">
            <i class="fa-solid fa-triangle-exclamation"></i>
            Phát hiện các biến thể đã tồn tại hoặc Mã SKU bị trùng. Vui lòng đổi Mã SKU hoặc xóa các
            dòng bị bôi đỏ!
          </div>
          <div v-else-if="hasInvalidGeneratedSku" class="skug-duplicate-warning">
            <i class="fa-solid fa-triangle-exclamation"></i>
            Vui lòng nhập đủ mã SKU, giá bán và khối lượng; giá niêm yết không được thấp hơn giá bán.
          </div>

          <div class="skug-card-footer">
            <button class="skug-btn-cancel" @click="currentStep = 2">← Quay lại</button>
            <button
              class="skug-btn-primary"
              @click="saveAllSkus"
              :disabled="isSaving || hasAnyDuplicate || hasInvalidGeneratedSku"
            >
              <span v-if="isSaving" class="skug-spinner"></span>
              {{ isSaving ? 'Đang lưu...' : `Lưu ${generatedSkus.length} SKU vào Database` }}
            </button>
          </div>
        </div>
      </div>

      <!-- Marcus thêm: modal sửa giá SKU, không cho màn giá ghi đè tồn kho/IMEI. -->
      <Transition name="modal">
        <div v-if="editModal.show" class="skug-modal-backdrop" @click.self="closeEditSku">
          <form class="skug-modal skug-price-modal" @submit.prevent="saveSkuPrices">
            <div class="skug-price-modal__header">
              <div>
                <p class="skug-price-modal__eyebrow">THÔNG TIN BÁN VÀ VẬN CHUYỂN</p>
                <h4>{{ editModal.sku?.skuCode }}</h4>
                <p>{{ editVariantLabel }}</p>
              </div>
              <button type="button" class="skug-modal-close" @click="closeEditSku">×</button>
            </div>

            <div class="skug-modal-body skug-price-form">
              <div class="skug-price-note">
                <i class="fa-solid fa-circle-info"></i>
                <span
                  >Giá mới áp dụng cho lần mua tiếp theo. Đơn hàng đã tạo vẫn giữ nguyên giá đã
                  chốt.</span
                >
              </div>

              <label class="skug-form-field">
                <span>Giá niêm yết <b>*</b></span>
                <div class="skug-money-input">
                  <input
                    v-model.number="editModal.originalPrice"
                    type="number"
                    min="1"
                    max="9999999999999.99"
                    step="1"
                    inputmode="numeric"
                    @input="editModal.error = ''"
                  />
                  <span>₫</span>
                </div>
                <small>Giá dùng để tham chiếu và hiển thị mức giảm.</small>
              </label>

              <label class="skug-form-field">
                <span>Giá bán hiện tại <b>*</b></span>
                <div class="skug-money-input">
                  <input
                    v-model.number="editModal.price"
                    type="number"
                    min="1"
                    max="9999999999999.99"
                    step="1"
                    inputmode="numeric"
                    @input="editModal.error = ''"
                  />
                  <span>₫</span>
                </div>
                <small>Giá bán thường, không phải giá Flash Sale hoặc Voucher.</small>
              </label>

              <label class="skug-form-field">
                <span>Khối lượng đóng gói <b>*</b></span>
                <div class="skug-money-input">
                  <input
                    v-model.number="editModal.weightGram"
                    type="number"
                    min="1"
                    max="50000"
                    step="1"
                    inputmode="numeric"
                    @input="editModal.error = ''"
                  />
                  <span>g</span>
                </div>
                <small>Checkout cộng khối lượng các SKU để tính phí và tạo vận đơn GHN.</small>
              </label>

              <div class="skug-price-preview" :class="{ 'has-error': priceEditError }">
                <span>Mức giảm hiển thị</span>
                <strong>{{ editDiscountLabel }}</strong>
              </div>
              <p v-if="priceEditError" class="skug-form-error">{{ priceEditError }}</p>
            </div>

            <div class="skug-modal-footer skug-price-actions">
              <button type="button" class="skug-btn-cancel" @click="closeEditSku">Hủy</button>
              <button
                type="submit"
                class="skug-btn-primary"
                :disabled="editModal.saving || !!priceEditError"
              >
                <span v-if="editModal.saving" class="skug-spinner"></span>
                {{ editModal.saving ? 'Đang lưu...' : 'Lưu thông tin SKU' }}
              </button>
            </div>
          </form>
        </div>
      </Transition>

      <!-- Marcus thêm: modal ảnh SKU. Một file có thể dùng chung cho các dung
           lượng cùng màu mà không tạo ảnh trùng trong Product_Images. -->
      <Transition name="modal">
        <div v-if="imageModal.show" class="skug-modal-backdrop" @click.self="closeSkuImageModal">
          <form class="skug-modal skug-image-modal" @submit.prevent="saveSkuImage">
            <div class="skug-price-modal__header">
              <div>
                <p class="skug-price-modal__eyebrow">ẢNH ĐẠI DIỆN BIẾN THỂ</p>
                <h4>{{ imageModal.sku?.skuCode }}</h4>
                <p>{{ imageVariantLabel }}</p>
              </div>
              <button type="button" class="skug-modal-close" @click="closeSkuImageModal">×</button>
            </div>
            <div class="skug-modal-body skug-image-form">
              <div class="skug-image-preview">
                <img
                  v-if="imageModal.preview || imageModal.sku?.skuImageUrl"
                  :src="imageModal.preview || imageModal.sku.skuImageUrl"
                  alt="Xem trước ảnh biến thể"
                />
                <div v-else class="skug-image-placeholder"><i class="fa-regular fa-image"></i></div>
              </div>
              <div class="skug-image-options">
                <div class="skug-image-picker">
                  <span>Ảnh biến thể <b>*</b></span>
                  <label class="skug-file-control">
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      @change="onSkuImageSelected"
                    />
                    <span class="skug-file-button"
                      ><i class="fa-solid fa-upload"></i> Chọn ảnh</span
                    >
                    <span class="skug-file-name">{{
                      imageModal.file?.name || 'Chưa chọn tệp'
                    }}</span>
                  </label>
                  <small>JPG, PNG hoặc WebP · tối đa 5 MB</small>
                </div>
                <label v-if="sameColorSkuIds.length > 1" class="skug-image-apply">
                  <input v-model="imageModal.applySameColor" type="checkbox" />
                  <span
                    >Áp dụng cho <b>{{ sameColorSkuIds.length }} SKU cùng màu</b> (các bản dung
                    lượng khác).</span
                  >
                </label>
                <p class="skug-image-note">
                  Ảnh được dùng tại chi tiết sản phẩm, giỏ hàng và Checkout. Thư viện ảnh chung của
                  sản phẩm không bị thay đổi.
                </p>
                <p v-if="imageModal.error" class="skug-form-error">{{ imageModal.error }}</p>
              </div>
            </div>
            <div class="skug-modal-footer skug-price-actions">
              <button type="button" class="skug-btn-cancel" @click="closeSkuImageModal">Hủy</button>
              <button
                type="submit"
                class="skug-btn-primary"
                :disabled="imageModal.saving || !imageModal.file"
              >
                <span v-if="imageModal.saving" class="skug-spinner"></span>
                {{ imageModal.saving ? 'Đang tải...' : 'Lưu ảnh biến thể' }}
              </button>
            </div>
          </form>
        </div>
      </Transition>

      <!-- Modal Thông báo -->
      <Transition name="modal">
        <div
          class="skug-modal-backdrop"
          v-if="alertModal.show"
          @click.self="alertModal.show = false"
        >
          <div class="skug-modal skug-alert-modal">
            <div class="skug-modal-body skug-text-center">
              <div class="skug-alert-icon" :class="alertModal.type">
                <span v-if="alertModal.type === 'success'">✓</span>
                <span v-else-if="alertModal.type === 'error'">✕</span>
                <span v-else>!</span>
              </div>
              <h4 class="skug-alert-title">
                {{ alertModal.type === 'success' ? 'Thành công' : 'Thông báo' }}
              </h4>
              <p class="skug-alert-message">{{ alertModal.message }}</p>
            </div>
            <div class="skug-modal-footer skug-justify-center">
              <button
                v-if="alertModal.isConfirm"
                class="skug-btn-cancel"
                @click="alertModal.show = false"
              >
                Hủy
              </button>
              <button
                v-if="alertModal.isConfirm"
                class="skug-btn-primary"
                style="background: #ef4444"
                @click="executeDelete"
              >
                Đồng ý xóa
              </button>
              <button v-else class="skug-btn-primary" @click="alertModal.show = false">Đóng</button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/utils/api'
import '@/assets/css/sku.css'

// ── STATE ──
const currentStep = ref(1)
const isSaving = ref(false)
const fieldErrors = ref({})

// Phân trang & Lọc
const products = ref([])
const searchQuery = ref('')
const filterStatus = ref('all')
const currentPage = ref(0)
const totalPages = ref(0)

const selectedProductId = ref('')
const selectedProduct = ref(null)

const attributes = ref([])
const attributeValues = ref({})
const selectedAttributeIds = ref(new Set())
const selectedValueIds = ref(new Set())

const generatedSkus = ref([])
const bulkPrice = ref('')
const bulkOriginalPrice = ref('') // BIẾN GIÁ GỐC HÀNG LOẠT
// Marcus thêm: 500g là giá trị khởi tạo dễ nhận biết, Admin phải kiểm tra lại
// theo khối lượng đóng gói thực tế trước khi bán.
const bulkWeightGram = ref(500)
const alertModal = ref({ show: false, message: '', type: 'success' })
const existingSkus = ref([])
const editModal = ref({
  show: false,
  saving: false,
  sku: null,
  index: -1,
  originalPrice: '',
  price: '',
  weightGram: 500,
  error: '',
})

const imageModal = ref({
  show: false,
  saving: false,
  sku: null,
  file: null,
  preview: '',
  applySameColor: true,
  error: '',
})

const imageVariantLabel = computed(() => {
  const values = imageModal.value.sku?.attributeValues || []
  return values.length ? values.map((value) => value.valueString).join(' / ') : 'Biến thể mặc định'
})

const colorAttribute = computed(() =>
  attributes.value.find((attribute) => {
    const name = String(attribute.attributeName || '').toLowerCase()
    return name.includes('màu') || name.includes('color')
  }),
)

const sameColorSkuIds = computed(() => {
  const sku = imageModal.value.sku
  if (!sku) return []
  const colorValueIds = new Set(
    getAttrValues(colorAttribute.value?.attributeId).map((value) => value.valueId),
  )
  const selectedColor = (sku.attributeValues || []).find((value) =>
    colorValueIds.has(value.valueId),
  )
  if (!selectedColor) return [sku.skuId]
  return existingSkus.value
    .filter((item) =>
      (item.attributeValues || []).some((value) => value.valueId === selectedColor.valueId),
    )
    .map((item) => item.skuId)
})

const openSkuImageModal = (sku) => {
  imageModal.value = {
    show: true,
    saving: false,
    sku,
    file: null,
    preview: '',
    applySameColor: true,
    error: '',
  }
}

const closeSkuImageModal = () => {
  if (imageModal.value.saving) return
  if (imageModal.value.preview) URL.revokeObjectURL(imageModal.value.preview)
  imageModal.value.show = false
}

const onSkuImageSelected = (event) => {
  const file = event.target.files?.[0]
  imageModal.value.error = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    imageModal.value.error = 'Ảnh biến thể chỉ hỗ trợ JPG, PNG hoặc WebP.'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    imageModal.value.error = 'Ảnh biến thể không được vượt quá 5 MB.'
    return
  }
  if (imageModal.value.preview) URL.revokeObjectURL(imageModal.value.preview)
  imageModal.value.file = file
  imageModal.value.preview = URL.createObjectURL(file)
}

const saveSkuImage = async () => {
  if (!imageModal.value.file || !imageModal.value.sku) return
  imageModal.value.saving = true
  imageModal.value.error = ''
  try {
    const targetIds = imageModal.value.applySameColor
      ? sameColorSkuIds.value
      : [imageModal.value.sku.skuId]
    const formData = new FormData()
    targetIds.forEach((skuId) => formData.append('skuIds', skuId))
    formData.append('file', imageModal.value.file)
    const response = await api.post('/admin/skus/images', formData)
    const updatedSkus = response.data?.data || []
    const updatedById = new Map(updatedSkus.map((sku) => [sku.skuId, sku.skuImageUrl]))
    existingSkus.value = existingSkus.value.map((sku) =>
      updatedById.has(sku.skuId) ? { ...sku, skuImageUrl: updatedById.get(sku.skuId) } : sku,
    )
    imageModal.value.saving = false
    closeSkuImageModal()
    showAlert(`Đã cập nhật ảnh cho ${updatedSkus.length || targetIds.length} SKU.`, 'success')
  } catch (error) {
    imageModal.value.error = error.response?.data?.message || 'Không thể tải ảnh biến thể.'
  } finally {
    imageModal.value.saving = false
  }
}

const editVariantLabel = computed(() => {
  const values = editModal.value.sku?.attributeValues || []
  return values.length ? values.map((value) => value.valueString).join(' / ') : 'Biến thể mặc định'
})

const priceEditError = computed(() => {
  const originalPrice = Number(editModal.value.originalPrice)
  const price = Number(editModal.value.price)
  const weightGram = Number(editModal.value.weightGram)
  if (!Number.isFinite(originalPrice) || originalPrice <= 0) return 'Giá niêm yết phải lớn hơn 0.'
  if (!Number.isFinite(price) || price <= 0) return 'Giá bán phải lớn hơn 0.'
  if (price > originalPrice) return 'Giá bán không được lớn hơn giá niêm yết.'
  if (!Number.isInteger(weightGram) || weightGram < 1 || weightGram > 50000) {
    return 'Khối lượng SKU phải từ 1 đến 50.000 gram.'
  }
  return editModal.value.error || ''
})

const editDiscountLabel = computed(() => {
  if (priceEditError.value) return 'Chưa hợp lệ'
  const originalPrice = Number(editModal.value.originalPrice)
  const price = Number(editModal.value.price)
  if (price === originalPrice) return 'Không giảm giá'
  return `Giảm ${Math.round(((originalPrice - price) * 100) / originalPrice)}%`
})

const openEditSku = (sku, index) => {
  editModal.value = {
    show: true,
    saving: false,
    sku,
    index,
    originalPrice: Number(sku.originalPrice || sku.price || 0),
    price: Number(sku.price || 0),
    weightGram: Number(sku.weightGram || 500),
    error: '',
  }
}

const closeEditSku = () => {
  if (editModal.value.saving) return
  editModal.value.show = false
}

const saveSkuPrices = async () => {
  if (priceEditError.value || !editModal.value.sku) return
  editModal.value.saving = true
  try {
    const payload = {
      originalPrice: Number(editModal.value.originalPrice),
      price: Number(editModal.value.price),
      weightGram: Number(editModal.value.weightGram),
    }
    const response = await api.put(`/admin/skus/${editModal.value.sku.skuId}`, payload)
    const updated = response.data?.data || { ...editModal.value.sku, ...payload }
    existingSkus.value.splice(editModal.value.index, 1, updated)
    editModal.value.show = false
    showAlert(`Đã cập nhật giá SKU ${updated.skuCode || editModal.value.sku.skuCode}.`, 'success')
  } catch (error) {
    const validation = error.response?.data?.data
    editModal.value.error =
      validation?._global ||
      validation?.priceRangeValid ||
      validation?.price ||
      validation?.originalPrice ||
      error.response?.data?.message ||
      'Không thể cập nhật giá SKU.'
  } finally {
    editModal.value.saving = false
  }
}

const skuToDelete = ref(null) // SKU đang chờ xác nhận xóa
const confirmDeleteSku = (skuId, index) => {
  skuToDelete.value = { skuId, index } // Lưu tạm thông tin
  alertModal.value = {
    show: true,
    message:
      'Bạn chắc chắn muốn vô hiệu hóa SKU này khỏi hệ thống? Dữ liệu đơn hàng liên quan có thể bị ảnh hưởng.',
    type: 'error',
    isConfirm: true, // Cờ báo hiệu đây là Modal xác nhận xóa
  }
}
// FORMAT TIỀN TỆ
const formatMoney = (value) => {
  return new Intl.NumberFormat('vi-VN').format(value || 0) + '₫'
}

const formatWeight = (value) => `${new Intl.NumberFormat('vi-VN').format(value || 500)} g`

// ── QUẢN LÝ DỮ LIỆU CŨ ──
watch(selectedProductId, async (newVal) => {
  if (!newVal) {
    existingSkus.value = []
    return
  }
  try {
    const res = await api.get(`/admin/skus/product/${newVal}`)
    existingSkus.value = res.data?.data || []
  } catch (error) {
    console.error('Lỗi khi tải danh sách SKU hiện có:', error)
  }
})

const executeDelete = async () => {
  if (!skuToDelete.value) return
  const { skuId, index } = skuToDelete.value

  try {
    await api.delete(`/admin/skus/${skuId}`)
    existingSkus.value.splice(index, 1)
    alertModal.value.show = false
    showAlert('Đã vô hiệu hóa SKU thành công!', 'success')
  } catch (e) {
    alertModal.value.show = false
    showAlert('Lỗi khi vô hiệu hóa: ' + (e.response?.data?.message || e.message), 'error')
  } finally {
    skuToDelete.value = null
  }
}

// ── THUẬT TOÁN KIỂM TRA TRÙNG LẶP ──
const isComboExists = (valueIds) => {
  const normalizedIds = [...(valueIds || [])].map(Number).sort((a, b) => a - b)
  return existingSkus.value.some((sku) => {
    if (!sku.attributeValues) return false
    const existingIds = sku.attributeValues.map((value) => Number(value.valueId)).sort((a, b) => a - b)
    return existingIds.length === normalizedIds.length
      && existingIds.every((id, index) => id === normalizedIds[index])
  })
}

const clearFieldError = (index, fieldName) => {
  const key = `skus[${index}].${fieldName}`
  if (fieldErrors.value[key]) delete fieldErrors.value[key]
}

const isDuplicateSku = (code, currentIndex) => {
  if (!code) return false
  const normalizedCode = String(code).trim().toUpperCase()
  const inNew =
    generatedSkus.value.findIndex((sku, index) =>
      index !== currentIndex && String(sku.skuCode || '').trim().toUpperCase() === normalizedCode) !== -1
  const inDb = existingSkus.value.some(
    (sku) => String(sku.skuCode || '').trim().toUpperCase() === normalizedCode,
  )
  return inNew || inDb
}

const hasAnyDuplicate = computed(() => {
  const codes = generatedSkus.value
    .map((sku) => String(sku.skuCode || '').trim().toUpperCase())
    .filter(Boolean)
  const hasDuplicateCode = new Set(codes).size !== codes.length
  const hasExistingCombo = generatedSkus.value.some((sku) => isComboExists(sku.valueIds))
  return hasDuplicateCode || hasExistingCombo
})

const hasInvalidGeneratedSku = computed(() => generatedSkus.value.some((sku) => {
  const code = String(sku.skuCode || '').trim()
  const price = Number(sku.price)
  const originalPrice = sku.originalPrice === '' || sku.originalPrice == null
    ? price
    : Number(sku.originalPrice)
  const weightGram = Number(sku.weightGram)
  return !/^[A-Za-z0-9._-]+$/.test(code)
    || !Number.isFinite(price) || price <= 0
    || !Number.isFinite(originalPrice) || originalPrice < price
    || !Number.isInteger(weightGram) || weightGram < 1 || weightGram > 50000
    || !Array.isArray(sku.valueIds) || sku.valueIds.length === 0
}))

// ── API CALLS CƠ BẢN ──
const fetchProducts = async () => {
  try {
    const res = await api.get('/admin/product', {
      params: {
        page: currentPage.value,
        size: 8,
        keyword: searchQuery.value,
        filter: filterStatus.value,
      },
    })
    products.value = res.data.data?.content || res.data.data || []
    totalPages.value = res.data.data?.totalPages || 0
  } catch (error) {
    showAlert('Lỗi tải sản phẩm: ' + error.message, 'error')
  }
}

const handleSearch = () => {
  currentPage.value = 0
  fetchProducts()
}
const handleFilterChange = () => {
  currentPage.value = 0
  fetchProducts()
}
const changePage = (page) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page
    fetchProducts()
  }
}

const fetchAttributesAndValues = async () => {
  try {
    const attrRes = await api.get('/admin/attributes')
    attributes.value = attrRes.data?.data || []
    await Promise.all(
      attributes.value.map(async (attr) => {
        try {
          const valRes = await api.get(`/admin/attribute-values/attribute/${attr.attributeId}`)
          attributeValues.value[attr.attributeId] = valRes.data?.data || []
        } catch {
          attributeValues.value[attr.attributeId] = []
        }
      }),
    )
  } catch {
    showAlert('Lỗi tải thuộc tính', 'error')
  }
}

onMounted(async () => {
  await Promise.all([fetchProducts(), fetchAttributesAndValues()])
})

// ── THAO TÁC FORM ──
const selectProduct = (p) => {
  if (selectedProductId.value !== p.productId) {
    // Marcus sửa: không mang ma trận/giá/thuộc tính của Product trước sang
    // Product vừa chọn.
    currentStep.value = 1
    generatedSkus.value = []
    selectedAttributeIds.value = new Set()
    selectedValueIds.value = new Set()
    fieldErrors.value = {}
    bulkPrice.value = ''
    bulkOriginalPrice.value = ''
    bulkWeightGram.value = 500
  }
  selectedProductId.value = p.productId
  selectedProduct.value = p
}
const getAttrValues = (attrId) => attributeValues.value[attrId] || []
const isAttributeSelected = (attrId) => selectedAttributeIds.value.has(attrId)
const isValueSelected = (valueId) => selectedValueIds.value.has(valueId)
const getSelectedValueCount = (attrId) =>
  getAttrValues(attrId).filter((v) => selectedValueIds.value.has(v.valueId)).length
const getTotalSelected = () => selectedValueIds.value.size

const toggleAttribute = (attrId) => {
  const set = new Set(selectedAttributeIds.value)
  if (set.has(attrId)) {
    set.delete(attrId)
    const vids = new Set(selectedValueIds.value)
    getAttrValues(attrId).forEach((v) => vids.delete(v.valueId))
    selectedValueIds.value = vids
  } else {
    set.add(attrId)
  }
  selectedAttributeIds.value = set
}

const toggleValue = (val) => {
  const vids = new Set(selectedValueIds.value)
  if (vids.has(val.valueId)) vids.delete(val.valueId)
  else vids.add(val.valueId)
  selectedValueIds.value = vids
}

const getCartesianCount = () => {
  const perAttr = attributes.value
    .filter((a) => isAttributeSelected(a.attributeId))
    .map((a) => getAttrValues(a.attributeId).filter((v) => isValueSelected(v.valueId)).length)
    .filter((c) => c > 0)
  return perAttr.length ? perAttr.reduce((a, b) => a * b, 1) : 0
}

// ── THUẬT TOÁN GEN MÃ SKU ──
const getInitials = (str) => {
  if (!str) return 'PRD'
  return str
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()
    .split(' ')
    .filter((w) => w.length > 0)
    .map((w) => (/\d/.test(w) ? w.substring(0, 4) : w[0]))
    .join('')
}

const generateValueCode = (val) => {
  const clean = val
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[đĐ]/g, 'D')
    .replace(/[^a-zA-Z0-9 ]/g, '')
    .toUpperCase()
  const words = clean.split(' ').filter((w) => w.length > 0)
  if (words.length === 0) return ''
  if (words.length === 1 && /\d/.test(words[0])) return words[0].substring(0, 5)
  if (words.length === 1) return words[0].substring(0, 3)
  let code = ''
  for (let i = 0; i < words.length - 1; i++) code += words[i][0]
  code += words[words.length - 1].substring(0, 2)
  return code
}

const cartesian = (args) => args.reduce((a, b) => a.flatMap((x) => b.map((y) => [...x, y])), [[]])

const generateVariantsAndNext = () => {
  const selectedAttrs = attributes.value
    .filter((a) => isAttributeSelected(a.attributeId))
    .map((a) => ({
      ...a,
      chosenValues: getAttrValues(a.attributeId).filter((v) => isValueSelected(v.valueId)),
    }))
    .filter((a) => a.chosenValues.length > 0)

  if (selectedAttrs.length === 0) return

  const product = selectedProduct.value
  const baseCode = product ? getInitials(product.productName) : 'PRD'
  const valueCombos = cartesian(selectedAttrs.map((a) => a.chosenValues))

  generatedSkus.value = valueCombos.map((combo) => ({
    variantName: combo.map((v) => v.valueString).join(' / '),
    skuCode: `${baseCode}-${combo.map((v) => generateValueCode(v.valueString)).join('-')}`,
    originalPrice: bulkOriginalPrice.value || '', // ÁP DỤNG GIÁ GỐC TỪ BULK BAR
    price: bulkPrice.value || '',
    weightGram: Number(bulkWeightGram.value) || 500,
    comboValues: combo.map((v) => v.valueString),
    valueIds: combo.map((v) => v.valueId),
  }))
  currentStep.value = 3
}

const applyBulkSettings = () => {
  generatedSkus.value.forEach((sku) => {
    if (bulkOriginalPrice.value !== '') sku.originalPrice = Number(bulkOriginalPrice.value)
    if (bulkPrice.value !== '') sku.price = Number(bulkPrice.value)
    if (bulkWeightGram.value !== '') sku.weightGram = Number(bulkWeightGram.value)
  })
}

// ── LƯU LÊN BACKEND ──
const saveAllSkus = async () => {
  if (!selectedProductId.value || generatedSkus.value.length === 0
    || hasAnyDuplicate.value || hasInvalidGeneratedSku.value) return
  isSaving.value = true
  fieldErrors.value = {}

  try {
    const payload = {
      productId: selectedProductId.value,
      skus: generatedSkus.value.map((sku) => ({
        skuCode: String(sku.skuCode || '').trim().toUpperCase(),
        originalPrice: sku.originalPrice ? Number(sku.originalPrice) : null, // GỬI GIÁ GỐC LÊN JAVA
        price: Number(sku.price),
        weightGram: Number(sku.weightGram),
        valueIds: sku.valueIds,
      })),
    }
    await api.post('/admin/skus/batch', payload)
    showAlert(`Đã lưu ${generatedSkus.value.length} SKU thành công!`, 'success')

    // Tự động load lại danh sách DB để hiển thị ngay
    const res = await api.get(`/admin/skus/product/${selectedProductId.value}`)
    existingSkus.value = res.data?.data || []

    generatedSkus.value = []
    selectedValueIds.value = new Set()
    selectedAttributeIds.value = new Set()
    currentStep.value = 1
  } catch (error) {
    if (error.response?.status === 400 && error.response?.data?.data) {
      fieldErrors.value = error.response.data.data
      showAlert('Vui lòng kiểm tra lại các trường báo đỏ trong bảng!', 'error')
    } else {
      showAlert(error.response?.data?.message || 'Có lỗi khi lưu SKU vào CSDL', 'error')
    }
  } finally {
    isSaving.value = false
  }
}

const showAlert = (msg, type = 'success') => {
  alertModal.value = { show: true, message: msg, type, isConfirm: false }
}

const colorKeywords = {
  đen: '#1a1a2e',
  black: '#1a1a2e',
  trắng: '#f0f0f0',
  white: '#f0f0f0',
  hồng: '#f472b6',
  pink: '#f472b6',
  đỏ: '#ef4444',
  red: '#ef4444',
  'xanh lam': '#3b82f6',
  blue: '#3b82f6',
  'xanh lá': '#22c55e',
  green: '#22c55e',
  vàng: '#eab308',
  gold: '#eab308',
  titan: '#8b8fa8',
  titanium: '#8b8fa8',
  xám: '#6b7280',
  gray: '#6b7280',
  tím: '#a855f7',
  purple: '#a855f7',
}
const getColorStyle = (valueStr) => {
  if (!valueStr) return {}
  const lower = valueStr.toLowerCase()
  for (const [key, color] of Object.entries(colorKeywords)) {
    if (lower.includes(key)) return { background: color }
  }
  return { background: 'linear-gradient(135deg, #fce7f3, #fbcfe8)' }
}
</script>
