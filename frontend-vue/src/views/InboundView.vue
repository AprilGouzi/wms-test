<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createInboundOrder, getProducts, getWarehouses, getLocations, type Product, type Warehouse, type Location } from '@/api'

const supplierName = ref('')
const items = ref<any[]>([])
const submitting = ref(false)
const products = ref<Product[]>([])
const warehouses = ref<Warehouse[]>([])

const loadingProducts = ref(false)
const loadingWarehouses = ref(false)
const locationCache = ref<Record<number, Location[]>>({})

// 加载商品列表
const loadProducts = async (keyword?: string) => {
  loadingProducts.value = true
  try {
    const res = await getProducts(keyword)
    products.value = res.data
  } catch (e: any) {
    console.error('加载商品失败', e)
  } finally {
    loadingProducts.value = false
  }
}

// 加载仓库列表
const loadWarehouses = async () => {
  loadingWarehouses.value = true
  try {
    const res = await getWarehouses()
    warehouses.value = res.data
  } catch (e: any) {
    console.error('加载仓库失败', e)
  } finally {
    loadingWarehouses.value = false
  }
}

// 加载库位列表
const loadLocations = async (warehouseId: number) => {
  if (!locationCache.value[warehouseId]) {
    try {
      const res = await getLocations(warehouseId)
      locationCache.value[warehouseId] = res.data
    } catch (e: any) {
      console.error('加载库位失败', e)
    }
  }
}

onMounted(() => {
  loadProducts()
  loadWarehouses()
})

// 添加明细行
const addItem = () => {
  items.value.push({
    productId: undefined,
    quantity: 1,
    locationCode: '',
    warehouseId: undefined,
  })
}

// 删除明细行
const removeItem = (index: number) => {
  items.value.splice(index, 1)
}

// 提交入库单
const handleSubmit = async () => {
  if (!supplierName.value.trim()) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  if (items.value.length === 0) {
    ElMessage.warning('请添加至少一条入库明细')
    return
  }
  // 校验每个明细
  for (const item of items.value) {
    if (!item.productId) {
      ElMessage.warning('请选择商品')
      return
    }
    if (!item.locationCode) {
      ElMessage.warning('请选择库位')
      return
    }
    if (!item.quantity || item.quantity <= 0) {
      ElMessage.warning('请输入有效的数量')
      return
    }
  }

  submitting.value = true
  try {
    await createInboundOrder({
      supplierName: supplierName.value,
      items: items.value.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        locationCode: item.locationCode,
      })),
    })
    ElMessage.success('入库单创建成功')
    // 重置表单
    supplierName.value = ''
    items.value = []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '创建入库单失败')
  } finally {
    submitting.value = false
  }
}

// 监听仓库变化，加载库位
const onWarehouseChange = (warehouseId: number, index: number) => {
  items.value[index].locationCode = ''
  loadLocations(warehouseId)
}

// 获取库位列表
const getLocationsByWarehouse = (warehouseId: number) => {
  return locationCache.value[warehouseId] || []
}
</script>

<template>
  <div>
    <h3>入库管理</h3>

    <el-form label-width="100px" style="max-width: 900px">
      <el-form-item label="供应商名称" required>
        <el-input v-model="supplierName" placeholder="请输入供应商名称" style="width: 300px" />
      </el-form-item>

      <el-form-item label="入库明细">
        <el-button type="primary" @click="addItem">+ 添加明细</el-button>
      </el-form-item>
    </el-form>

    <!-- 明细列表 -->
    <div v-for="(item, index) in items" :key="index" style="margin-bottom: 12px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
      <el-select
        v-model="item.productId"
        placeholder="选择商品"
        filterable
        remote
        :remote-method="(q: string) => loadProducts(q)"
        :loading="loadingProducts"
        style="width: 200px"
        clearable
      >
        <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>

      <el-select
        v-model="item.warehouseId"
        placeholder="选择仓库"
        style="width: 150px"
        clearable
        @change="(v: number) => onWarehouseChange(v, index)"
      >
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>

      <el-select
        v-model="item.locationCode"
        placeholder="选择库位"
        style="width: 150px"
        clearable
        :disabled="!item.warehouseId"
      >
        <el-option v-for="loc in getLocationsByWarehouse(item.warehouseId)" :key="loc.id" :label="loc.code" :value="loc.code" />
      </el-select>

      <el-input-number v-model="item.quantity" :min="1" style="width: 120px" />

      <el-button type="danger" size="small" @click="removeItem(index)">删除</el-button>
    </div>

    <el-button type="success" :loading="submitting" @click="handleSubmit" :disabled="items.length === 0" style="margin-top: 16px">
      提交入库单
    </el-button>

    <el-empty v-if="items.length === 0" description="添加明细" />
  </div>
</template>
