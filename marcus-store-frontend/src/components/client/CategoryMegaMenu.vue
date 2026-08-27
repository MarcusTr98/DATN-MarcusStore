<template>
  <Transition name="mega-fade">
    <ul
      v-if="isOpen"
      class="dropdown-menu ms-dropdown category-dropdown-menu show"
    >
      <!-- Loading -->
      <li v-if="loadingParents" class="dropdown-loading">
        <i class="fas fa-spinner fa-spin"></i> Đang tải...
      </li>

      <!-- Danh sách danh mục -->
      <template v-else>
        <li
          v-for="parent in parentCategories"
          :key="parent.categoryId"
          class="category-dropdown-item"
        >
          <!-- Danh mục cha: click vào text/icon sẽ expand children -->
          <div
            class="category-dropdown-parent"
            :class="{ expanded: expandedId === parent.categoryId }"
            @click="toggleExpand(parent)"
          >
            <i class="fas fa-folder-open category-icon"></i>
            <span class="category-dropdown-name">{{ parent.categoryName }}</span>
            <button
              class="category-dropdown-toggle"
              type="button"
              aria-label="Mở rộng danh mục con"
              @click.stop="toggleExpand(parent)"
            >
              <i :class="expandedId === parent.categoryId ? 'fas fa-chevron-up' : 'fas fa-chevron-down'"></i>
            </button>
          </div>

          <!-- Danh mục con - hiện khi expand -->
          <ul v-if="expandedId === parent.categoryId" class="category-dropdown-children">
            <li v-if="loadingChildrenSet.has(parent.categoryId)" class="dropdown-loading">
              <i class="fas fa-spinner fa-spin"></i>
            </li>
            <template v-else>
              <li
                v-for="child in childrenMap[parent.categoryId]"
                :key="child.categoryId"
              >
                <router-link
                  :to="`/category/${child.slug}`"
                  class="category-dropdown-child"
                  @click.stop="closeMenu"
                >
                  <i class="fas fa-chevron-right child-icon"></i>
                  <span>{{ child.categoryName }}</span>
                </router-link>
              </li>
              <li v-if="!childrenMap[parent.categoryId]?.length" class="category-dropdown-child-empty">
                Chưa có danh mục con
              </li>
            </template>
          </ul>
        </li>
      </template>
    </ul>
  </Transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

defineProps({
  isOpen: { type: Boolean, default: false },
})

const emit = defineEmits(['navigate'])

const parentCategories = ref([])
const loadingParents = ref(false)
const childrenMap = ref({})
const loadingChildrenSet = ref(new Set())
const expandedId = ref(null)

async function fetchParents() {
  loadingParents.value = true
  try {
    const res = await api.get('/client/categories/main')
    parentCategories.value = res.data?.data ?? []
  } catch (err) {
    console.error('[CategoryDropdown] Lỗi khi tải danh mục cha:', err)
    parentCategories.value = []
  } finally {
    loadingParents.value = false
  }
}

async function toggleExpand(parent) {
  if (expandedId.value === parent.categoryId) {
    expandedId.value = null
    return
  }

  expandedId.value = parent.categoryId

  if (childrenMap.value[parent.categoryId]) return

  loadingChildrenSet.value.add(parent.categoryId)
  try {
    const res = await api.get(`/client/categories/${parent.categoryId}/children`)
    childrenMap.value[parent.categoryId] = res.data?.data ?? []
  } catch (err) {
    console.error(`[CategoryDropdown] Lỗi khi tải danh mục con của "${parent.categoryName}":`, err)
    childrenMap.value[parent.categoryId] = []
  } finally {
    loadingChildrenSet.value.delete(parent.categoryId)
  }
}

onMounted(fetchParents)

function closeMenu() {
  emit('navigate')
}
</script>

<style scoped>
/* Override dropdown-menu để hiển thị đúng */
.category-dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 260px;
  max-height: 60vh;
  overflow-y: auto;
  margin-top: 0 !important;
  border: none !important;
  border-radius: 12px !important;
  padding: 6px 0;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
}

.dropdown-loading {
  padding: 12px 16px;
  color: #888;
  font-size: 13px;
  text-align: center;
}

/* ---- Parent Item ---- */
.category-dropdown-item {
  position: relative;
}

.category-dropdown-parent {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-radius: 8px;
  margin: 2px 6px;
  gap: 10px;
}

.category-dropdown-parent:hover {
  background: #fff5f5;
}

.category-dropdown-parent.expanded {
  background: #fff5f5;
  color: #d70018;
}

.category-dropdown-name {
  flex: 1;
  font-weight: 600;
  font-size: 13px;
  color: #333;
}

.category-dropdown-parent:hover .category-dropdown-name,
.category-dropdown-parent.expanded .category-dropdown-name {
  color: #d70018;
}

.category-icon {
  color: #d70018;
  font-size: 14px;
  width: 16px;
  text-align: center;
}

.category-dropdown-toggle {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #999;
  font-size: 10px;
  transition: all 0.15s;
}

.category-dropdown-toggle:hover {
  color: #666;
}

/* ---- Children ---- */
.category-dropdown-children {
  list-style: none;
  padding: 0 0 4px 46px;
  margin: 0;
  background: #fafafa;
  border-radius: 0 0 8px 8px;
}

.category-dropdown-child {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px 7px 6px;
  border-radius: 6px;
  text-decoration: none;
  color: #555;
  font-size: 13px;
  transition: all 0.15s;
}

.category-dropdown-child:hover {
  background: #fff;
  color: #d70018;
}

.child-icon {
  font-size: 8px;
  color: #ccc;
}

.category-dropdown-child-empty {
  padding: 6px 12px 6px 6px;
  color: #999;
  font-size: 12px;
  font-style: italic;
}

/* ---- Transition ---- */
.mega-fade-enter-active,
.mega-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.mega-fade-enter-from,
.mega-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
