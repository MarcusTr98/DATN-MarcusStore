<template>
  <Transition name="mega-fade">
    <div
      v-if="isOpen"
      class="mega-menu"
      @mouseenter="$emit('mouseenter')"
      @mouseleave="onMegaLeave"
    >
      <div class="container">
        <div class="mega-inner">
          <!-- Sidebar: danh sách danh mục cha -->
          <div class="mega-sidebar">
            <div v-if="loadingParents" class="mega-loading">
              <i class="fas fa-spinner fa-spin"></i> Đang tải danh mục...
            </div>

            <template v-else>
              <button
                v-for="parent in parentCategories"
                :key="parent.categoryId"
                type="button"
                class="mega-parent-item"
                :class="{ active: hoveredId === parent.categoryId }"
                @mouseenter="onHoverParent(parent)"
                @focus="onHoverParent(parent)"
                @click="navigateToParent(parent)"
              >
                <span class="mega-parent-name">{{ parent.categoryName }}</span>
                <i class="fas fa-chevron-right mega-parent-arrow"></i>
              </button>
            </template>
          </div>

          <!-- Content: danh sách danh mục con của parent đang hover -->
          <div class="mega-content">
            <template v-if="hoveredParent">
              <div class="mega-content-head">
                <router-link
                  :to="`/category/${hoveredParent.slug}`"
                  class="mega-content-title"
                  @click="$emit('navigate')"
                >
                  {{ hoveredParent.categoryName }}
                  <i class="fas fa-arrow-right ms-2"></i>
                </router-link>
              </div>

              <div
                v-if="loadingChildren"
                class="mega-loading mega-loading-inline"
              >
                <i class="fas fa-spinner fa-spin"></i> Đang tải...
              </div>

              <div
                v-else-if="(childrenOfHovered || []).length === 0"
                class="mega-empty"
              >
                Chưa có danh mục con cho mục này.
              </div>

              <div v-else class="mega-children-grid">
                <router-link
                  v-for="child in childrenOfHovered"
                  :key="child.categoryId"
                  :to="`/category/${child.slug}`"
                  class="mega-child-item"
                  @click="$emit('navigate')"
                >
                  <span class="mega-child-name">{{ child.categoryName }}</span>
                </router-link>
              </div>
            </template>

            <div v-else class="mega-placeholder">
              <i class="fas fa-hand-pointer"></i>
              <p>Di chuột vào danh mục bên trái để xem chi tiết</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

defineProps({
  isOpen: { type: Boolean, default: false },
})

const emit = defineEmits(['navigate', 'mouseenter', 'mouseleave'])

const router = useRouter()

const parentCategories = ref([])
const loadingParents = ref(false)

const childrenMap = ref({})
const loadingChildrenSet = ref(new Set())

const hoveredId = ref(null)

const hoveredParent = computed(() =>
  parentCategories.value.find((p) => p.categoryId === hoveredId.value),
)

const childrenOfHovered = computed(() =>
  hoveredId.value ? childrenMap.value[hoveredId.value] : [],
)

async function fetchParents() {
  loadingParents.value = true
  try {
    const res = await api.get('/client/categories/main')
    parentCategories.value = res.data?.data ?? []
  } catch (err) {
    console.error('[CategoryMegaMenu] Lỗi khi tải danh mục cha:', err)
    parentCategories.value = []
  } finally {
    loadingParents.value = false
  }
}

async function onHoverParent(parent) {
  if (!parent || hoveredId.value === parent.categoryId) return
  hoveredId.value = parent.categoryId

  // Cache: đã có rồi thì không gọi lại
  if (childrenMap.value[parent.categoryId]) return

  loadingChildrenSet.value.add(parent.categoryId)
  try {
    const res = await api.get(
      `/client/categories/${parent.categoryId}/children`,
    )
    childrenMap.value[parent.categoryId] = res.data?.data ?? []
  } catch (err) {
    console.error(
      `[CategoryMegaMenu] Lỗi khi tải danh mục con của "${parent.categoryName}":`,
      err,
    )
    childrenMap.value[parent.categoryId] = []
  } finally {
    loadingChildrenSet.value.delete(parent.categoryId)
  }
}

function onMegaLeave() {
  hoveredId.value = null
  emit('mouseleave')
}

function navigateToParent(parent) {
  if (parent?.slug) {
    router.push(`/category/${parent.slug}`)
    emit('navigate')
  }
}

const loadingChildren = computed(() =>
  hoveredId.value
    ? loadingChildrenSet.value.has(hoveredId.value)
    : false,
)

onMounted(fetchParents)
</script>

<style scoped>
.mega-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #ffffff;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.12);
  border-top: 1px solid #f0f0f0;
  z-index: 1030;
  max-height: 70vh;
  overflow: hidden;
}

.mega-inner {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: 360px;
  max-height: calc(70vh - 1px);
  overflow: hidden;
}

/* ---- Sidebar (parent) ---- */
.mega-sidebar {
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
  padding: 14px 10px;
  overflow-y: auto;
}

.mega-parent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #2b2b2b;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, color 0.18s ease, transform 0.18s ease;
  margin-bottom: 4px;
}

.mega-parent-item:hover,
.mega-parent-item.active {
  background: #d70018;
  color: #ffffff;
  transform: translateX(2px);
}

.mega-parent-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mega-parent-arrow {
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.mega-parent-item:hover .mega-parent-arrow,
.mega-parent-item.active .mega-parent-arrow {
  opacity: 1;
  transform: translateX(2px);
}

/* ---- Content (children) ---- */
.mega-content {
  padding: 18px 24px;
  overflow-y: auto;
}

.mega-content-head {
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

.mega-content-title {
  font-size: 16px;
  font-weight: 700;
  color: #d70018;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.mega-content-title:hover {
  color: #b80015;
}

.mega-children-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 8px 16px;
}

.mega-child-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13.5px;
  color: #333;
  text-decoration: none;
  background: #fff;
  border: 1px solid #f1f1f1;
  transition: all 0.15s ease;
}

.mega-child-item:hover {
  background: #fff5f5;
  border-color: #ffc8c8;
  color: #d70018;
  transform: translateY(-1px);
}

.mega-child-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mega-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #999;
  font-size: 14px;
  text-align: center;
}

.mega-placeholder i {
  font-size: 36px;
  color: #ddd;
}

.mega-loading {
  padding: 18px;
  font-size: 13.5px;
  color: #777;
  text-align: center;
}

.mega-loading-inline {
  padding: 30px 0;
}

.mega-empty {
  padding: 30px 0;
  font-size: 13.5px;
  color: #999;
  text-align: center;
}

/* ---- Transition fade ---- */
.mega-fade-enter-active,
.mega-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.mega-fade-enter-from,
.mega-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ---- Responsive: dưới lg đổi sang 1 cột ---- */
@media (max-width: 991px) {
  .mega-menu {
    max-height: 65vh;
  }

  .mega-inner {
    grid-template-columns: 1fr;
    min-height: auto;
    max-height: calc(65vh - 1px);
  }

  .mega-sidebar {
    border-right: none;
    border-bottom: 1px solid #f0f0f0;
    max-height: 40vh;
    overflow-y: auto;
  }

  .mega-content {
    max-height: 40vh;
    overflow-y: auto;
  }
}
</style>
