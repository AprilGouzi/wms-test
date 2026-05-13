<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getInventory, getWarehouses, type InventoryItem, type Warehouse } from '@/api'

const keyword = ref('')
const warehouseId = ref<number | undefined>()
const loading = ref(false)
const inventoryList = ref<InventoryItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const warehouses = ref<Warehouse[]>([])

// 防抖计时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 加载仓库列表
const loadWarehouses = async () => {
  try {
    const res = await getWarehouses()
    warehouses.value = res.data
  } catch (e: any) {
    console.error('加载仓库失败', e)
  }
}

// 加载库存数据
const loadInventory = async () => {
  loading.value = true
  try {
    const res = await getInventory({
      keyword: keyword.value || undefined,
      warehouseId: warehouseId.value || undefined,
      page: page.value,
      pageSize: pageSize.value,
    })
    inventoryList.value = res.data.list
    total.value = res.data.total
  } catch (e: any) {
    console.error('加载库存失败', e)
  } finally {
    loading.value = false
  }
}

// 防抖搜索
const debounceSearch = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    page.value = 1
    loadInventory()
  }, 300)
}

// 监听搜索词变化
watch(keyword, () => {
  debounceSearch()
})

// 监听仓库变化
watch(warehouseId, () => {
  page.value = 1
  loadInventory()
})

// 表格行样式：库存数量低于10高亮为红色
const getRowStyle = (row: InventoryItem) => {
  if (row.quantity < 10) {
    return { color: 'red' }
  }
  return {}
}

// 格式化时间
const formatTime = (timeStr: string) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  loadWarehouses()
  loadInventory()
})
</script>

<template>
  <div>
    <h3>库存查询</h3>

    <!-- 搜索栏 -->
    <div style="display: flex; gap: 12px; margin-bottom: 16px">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称/SKU..."
        style="width: 300px"
        clearable
        @keyup.enter="loadInventory"
        @clear="loadInventory"
      />
      <el-select
        v-model="warehouseId"
        placeholder="选择仓库"
        clearable
        style="width: 200px"
      >
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-button type="primary" @click="loadInventory">查询</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="inventoryList"
      v-loading="loading"
      border
      stripe
      :row-style="(row: InventoryItem) => getRowStyle(row)"
    >
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="sku" label="SKU" width="150" />
      <el-table-column prop="locationCode" label="库位编码" width="150" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="库存数量" width="100">
        <template #default="{ row }">
          <span :style="{ color: row.quantity < 10 ? 'red' : 'inherit' }">{{ row.quantity }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.updatedAt) }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 16px; text-align: right">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadInventory"
      />
    </div>

    <el-empty v-if="!loading && inventoryList.length === 0" description="暂无库存数据，请先完成入库操作" />
  </div>
</template>
