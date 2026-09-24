<template>
  <div class="users-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" :loading="loading" @click="loadUsers">刷新</el-button>
    </div>

    <el-card class="users-card">
      <el-table v-loading="loading" :data="userList" border stripe style="width: 100%">
        <el-table-column prop="id" label="用户ID" width="190" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="160" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" width="150" show-overflow-tooltip />
        <el-table-column prop="password" label="密码" min-width="180" show-overflow-tooltip>
          <template #default="scope">
            <span class="password-text">{{ scope.row.password }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="修改时间" width="180" show-overflow-tooltip />
        <el-table-column prop="deleteFlag" label="delete_flag" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'danger'">
              {{ scope.row.deleteFlag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'">
              {{ scope.row.enabled ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.enabled"
              type="danger"
              link
              @click="changeStatus(scope.row, false)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              type="success"
              link
              @click="changeStatus(scope.row, true)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, prev, pager, next"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminUserPage,
  updateAdminUserStatus,
  type AdminUser,
} from '../../api/admin'

const loading = ref(false)
const userList = ref<AdminUser[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

onMounted(() => {
  loadUsers()
})

/**
 * 分页加载注册用户。
 */
async function loadUsers() {
  loading.value = true
  try {
    const response = await getAdminUserPage(pageNum.value, pageSize.value)
    userList.value = response.data.records || []
    total.value = Number(response.data.total) || 0
  } catch (error) {
    ElMessage.error('用户列表加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 切换用户启用和禁用状态。
 *
 * @param user 用户数据
 * @param enabled 是否启用
 */
async function changeStatus(user: AdminUser, enabled: boolean) {
  const action = enabled ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户“${user.username}”吗？`, '用户状态确认', {
      type: enabled ? 'success' : 'warning',
    })
    await updateAdminUserStatus(user.id, enabled)
    ElMessage.success(`用户已${action}`)
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

/**
 * 切换分页。
 *
 * @param page 新页码
 */
function handlePageChange(page: number) {
  pageNum.value = page
  loadUsers()
}
</script>

<style scoped>
.users-container {
  min-height: 100vh;
  padding: 24px;
  box-sizing: border-box;
  background: #f5faff;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 22px;
}

.users-card {
  border-radius: 8px;
}

.password-text {
  color: #d03050;
  font-weight: 600;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
