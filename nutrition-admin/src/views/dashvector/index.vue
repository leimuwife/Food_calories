<template>
  <div class="dashvector-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">知识库更新</h2>
        <p class="page-desc">
          上传文档会自动解析文本并入向量知识库，用于AI食材热量问答检索。
          支持 PDF、DOCX、DOC、TXT、MD、JSON 格式，单文件上限 20MB。
        </p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleOpenUploadDialog">上传知识库文档</el-button>
        <el-button @click="loadDocumentList">刷新列表</el-button>
      </div>
    </div>

    <!-- 文档列表 -->
    <el-card class="list-card">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="请输入文档名称搜索"
          clearable
          style="width: 300px"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 表格 -->
      <el-table
        :data="documentList"
        border
        stripe
        v-loading="tableLoading"
        style="width: 100%"
      >
        <el-table-column prop="fileName" label="文档名称" min-width="260" show-overflow-tooltip />
        <el-table-column prop="fileSizeText" label="文件大小" width="120" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" align="center" />
        <el-table-column prop="fileMd5" label="文件MD5" width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="scope">
            <el-button type="text" size="small" @click="handleDeleteDocument(scope.row)">
              删除文档
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!tableLoading && documentList.length === 0" description="暂无已上传的知识库文档" />

      <!-- 分页 -->
      <div class="pagination-bar" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadDocumentList"
          @current-change="loadDocumentList"
        />
      </div>
    </el-card>

    <!-- 上传文档弹窗 -->
    <el-dialog
      title="上传知识库文档"
      v-model="uploadDialogVisible"
      width="560px"
      @closed="handleUploadDialogClosed"
    >
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        multiple
        :auto-upload="false"
        :limit="20"
        :accept="ALLOWED_EXTENSIONS"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :on-remove="handleFileRemove"
        :before-upload="beforeUpload"
        :file-list="fileList"
      >
        <div class="upload-placeholder">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将文件拖到此处，或<span class="upload-link">点击选择</span></div>
          <div class="upload-hint">支持 PDF、DOCX、DOC、TXT、MD、JSON 格式，单文件 ≤ 20MB</div>
        </div>
      </el-upload>

      <!-- 上传进度条 -->
      <div v-if="uploading" class="upload-progress">
        <el-progress :percentage="uploadProgress" :status="uploadStatus" />
        <p class="progress-text">{{ uploadProgressText }}</p>
      </div>

      <template #footer>
        <el-button @click="uploadDialogVisible = false" :disabled="uploading">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="fileList.length === 0" @click="handleUploadSubmit">
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, UploadFilled } from '@element-plus/icons-vue'
import {
  getKnowledgeList,
  uploadKnowledge,
  deleteKnowledge,
  type KnowledgeDocument,
} from '../../api/dashvector'

// ==================== 允许的文件格式 ====================
const ALLOWED_EXTENSIONS = '.pdf,.docx,.doc,.txt,.md,.json'
const MAX_FILE_SIZE = 20 * 1024 * 1024 // 20MB

// ==================== 文档列表相关 ====================
const documentList = ref<KnowledgeDocument[]>([])
const tableLoading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
let searchTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 页面初始化加载文档列表
 */
onMounted(() => {
  loadDocumentList()
})

/**
 * 加载文档列表
 */
async function loadDocumentList() {
  tableLoading.value = true
  try {
    const response = await getKnowledgeList({
      keyword: searchKeyword.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    documentList.value = response.data.records || []
    total.value = response.data.total || 0
  } catch {
    ElMessage.error('加载文档列表失败')
  } finally {
    tableLoading.value = false
  }
}

/**
 * 搜索防抖处理
 */
function handleSearch() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadDocumentList()
  }, 300)
}

/**
 * 删除文档
 */
async function handleDeleteDocument(doc: KnowledgeDocument) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${doc.fileName}」吗？删除后向量库内对应切片数据将同步清除。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await deleteKnowledge(doc.id)
    ElMessage.success('文档删除成功，向量库切片已同步清除')
    loadDocumentList()
  } catch {
    // 用户取消删除或接口异常
  }
}

// ==================== 上传弹窗相关 ====================
const uploadDialogVisible = ref(false)
const uploadRef = ref()
const fileList = ref<any[]>([])
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref<'success' | 'exception' | 'warning' | ''>('')
const uploadProgressText = ref('')

/**
 * 打开上传弹窗
 */
function handleOpenUploadDialog() {
  fileList.value = []
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadProgressText.value = ''
  uploading.value = false
  uploadDialogVisible.value = true
}

/**
 * 上传弹窗关闭时清理
 */
function handleUploadDialogClosed() {
  fileList.value = []
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadProgressText.value = ''
  uploading.value = false
}

/**
 * 文件选择前的校验（返回 false 拦截添加）
 */
function beforeUpload(file: File): boolean {
  // 校验文件类型
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()
  if (!ALLOWED_EXTENSIONS.split(',').includes(ext || '')) {
    ElMessage.warning(`文件「${file.name}」格式不支持，仅允许 ${ALLOWED_EXTENSIONS} 格式`)
    return false
  }

  // 校验文件大小
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.warning(`文件「${file.name}」超过 20MB 上限，请压缩后重新上传`)
    return false
  }

  return true
}

/**
 * 文件列表变化（beforeUpload 已做拦截，这里仅用于跟踪）
 */
function handleFileChange(_file: any, fileListItems: any[]) {
  fileList.value = fileListItems
}

/**
 * 超出数量限制
 */
function handleExceed() {
  ElMessage.warning('单次最多上传 20 个文件')
}

/**
 * 移除文件
 */
function handleFileRemove(_file: any, fileListItems: any[]) {
  fileList.value = fileListItems
}

/**
 * 提交上传
 */
async function handleUploadSubmit() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''
  let successCount = 0
  let duplicateCount = 0
  let failCount = 0

  for (let i = 0; i < fileList.value.length; i++) {
    const fileItem = fileList.value[i]
    const rawFile = fileItem.raw || fileItem
    uploadProgressText.value = `正在上传：${rawFile.name}（${i + 1}/${fileList.value.length}）`
    uploadProgress.value = Math.round((i / fileList.value.length) * 100)

    try {
      const formData = new FormData()
      formData.append('file', rawFile)

      const response = await uploadKnowledge(formData)

      if (response.data?.duplicate) {
        duplicateCount++
        ElMessage.warning(`文件「${rawFile.name}」已存在知识库，无需重复上传`)
      } else if (response.data?.success) {
        successCount++
      } else {
        failCount++
      }
    } catch {
      failCount++
      ElMessage.error(`文件「${rawFile.name}」上传失败`)
    }

    uploadProgress.value = Math.round(((i + 1) / fileList.value.length) * 100)
  }

  uploading.value = false
  uploadProgress.value = 100
  uploadProgressText.value = '上传完成'

  // 汇总提示
  if (successCount > 0) {
    uploadStatus.value = 'success'
    ElMessage.success(`上传完成：成功 ${successCount} 个${duplicateCount > 0 ? `，重复 ${duplicateCount} 个` : ''}${failCount > 0 ? `，失败 ${failCount} 个` : ''}`)
    uploadDialogVisible.value = false
    loadDocumentList()
  } else if (duplicateCount > 0 && successCount === 0 && failCount === 0) {
    uploadStatus.value = 'warning'
    ElMessage.warning('所选文件均已存在于知识库中')
  } else {
    uploadStatus.value = 'exception'
    ElMessage.error(`上传失败：成功 ${successCount} 个，失败 ${failCount} 个`)
  }
}
</script>

<style scoped>
.dashvector-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.header-left {
  flex: 1;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 13px;
  color: #909399;
  margin: 0;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
  margin-left: 20px;
}

.list-card {
  min-height: 300px;
}

.search-bar {
  margin-bottom: 16px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.upload-area {
  width: 100%;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.upload-link {
  color: #409eff;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.upload-progress {
  margin-top: 16px;
  text-align: center;
}

.progress-text {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
