<template>
  <div class="ai-config-container">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <h2 class="page-title">AI 模型配置</h2>
      <el-button type="primary" @click="handleAdd">新增配置</el-button>
    </div>

    <!-- 配置列表 -->
   <!-- 配置列表 -->
<el-card class="config-card">
  <el-table :data="configList" border stripe style="width: 100%">
    <!-- 固定窄列：ID -->
    <el-table-column prop="id" label="ID" width="70" align="center" />
    
    <!-- 固定列：模型名称 -->
    <el-table-column prop="modelName" label="模型名称" width="150" show-overflow-tooltip />
    
    <!-- 固定列：昵称 -->
    <el-table-column prop="nickname" label="昵称" width="120" show-overflow-tooltip />
    
    <!-- 固定列：模型类型 -->
    <el-table-column prop="modelType" label="模型类型" width="130" align="center">
      <template #default="scope">
        <el-tag :type="getModelTypeTag(scope.row.modelType)">{{ getModelTypeName(scope.row.modelType) }}</el-tag>
      </template>
    </el-table-column>
    
    <!-- 自适应长列：API地址 -->
    <el-table-column prop="apiUrl" label="API地址" min-width="360" show-overflow-tooltip />
    
    <!-- 固定列：API密钥（建议脱敏，无需太宽） -->
    <el-table-column prop="apiKey" label="API密钥" width="180" show-overflow-tooltip>
      <template #default="scope">
        {{ scope.row.apiKey ? scope.row.apiKey.substring(0,6) + '****' + scope.row.apiKey.slice(-4) : '-' }}
      </template>
    </el-table-column>
    
    <!-- 固定窄列：温度 -->
    <el-table-column prop="temperature" label="温度" width="90" align="center" />
    
    <!-- 固定列：最大Token -->
    <el-table-column prop="maxTokens" label="最大Token" width="110" align="center" />
    
    <!-- 固定列：状态 -->
    <el-table-column prop="isEnabled" label="状态" width="90" align="center">
      <template #default="scope">
        <el-tag :type="scope.row.isEnabled === '1' ? 'success' : 'info'">
              {{ scope.row.isEnabled === '1' ? '已启用' : '未启用' }}
        </el-tag>
      </template>
    </el-table-column>
    
    <!-- 固定右侧操作列 -->
    <el-table-column label="操作" width="220" fixed="right" align="center">
      <template #default="scope">
        <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
        <el-button type="text" size="small" @click="handleEnable(scope.row)">
          {{ scope.row.isEnabled === '1' ? '禁用' : '启用' }}
        </el-button>
        <el-button type="text" size="small" @click="handleTest(scope.row)">测试</el-button>
        <el-button type="text" size="small" danger @click="handleDelete(scope.row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="isEdit ? '编辑配置' : '新增配置'" v-model="dialogVisible" width="600px">
      <el-form :model="form" :rules="isEdit ? editRules : addRules" ref="formRef" label-width="120px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入模型昵称" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="form.modelType" placeholder="请选择模型类型">
            <el-option label="OpenAI协议" value="openai" />
            <el-option label="阿里云通义千问" value="dashscope" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="请输入API地址" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="请输入API密钥" />
        </el-form-item>
      
        <el-form-item label="系统提示词" prop="systemPrompt">
          <el-input v-model="form.systemPrompt" type="textarea" :rows="3" placeholder="请输入系统提示词" />
        </el-form-item>
        <el-form-item label="温度参数" prop="temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="最大Token" prop="maxTokens">
          <el-input-number v-model="form.maxTokens" :min="1" :max="10000" :step="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试弹窗 -->
    <el-dialog title="测试配置连通性" v-model="testDialogVisible" width="600px">
      <el-form :model="testForm" label-width="100px">
        <el-form-item label="测试问题">
          <el-input v-model="testForm.message" type="textarea" :rows="3" placeholder="请输入测试问题" />
        </el-form-item>
        <el-form-item label="模型响应">
          <el-input v-model="testResult" type="textarea" :rows="5" readonly />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="handleTestSubmit">发送测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAiConfigList,
  addAiConfig,
  updateAiConfig,
  deleteAiConfig,
  enableAiConfig,
  testAiConfig,
  type AiConfig,
  type AiConfigForm
} from '../../api/ai'

// 配置列表
const configList = ref<AiConfig[]>([])

// 弹窗状态
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const form = reactive<AiConfigForm>({
  modelName: '',
  modelType: '',
  apiUrl: '',
  apiKey: '',
  nickname: '',
  systemPrompt: '',
  temperature: 0.7,
  maxTokens: 800
})

// 表单校验规则（新增时）
const addRules: FormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  apiUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }],
  apiKey: [{ required: true, message: '请输入API密钥', trigger: 'blur' }],
  temperature: [{ required: true, message: '请输入温度参数', trigger: 'blur' }],
  maxTokens: [{ required: true, message: '请输入最大Token数', trigger: 'blur' }]
}

// 表单校验规则（编辑时）
const editRules: FormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  apiUrl: [{ required: true, message: '请输入API地址', trigger: 'blur' }],
  temperature: [{ required: true, message: '请输入温度参数', trigger: 'blur' }],
  maxTokens: [{ required: true, message: '请输入最大Token数', trigger: 'blur' }]
}

// 测试相关
const testDialogVisible = ref(false)
const testLoading = ref(false)
const testForm = reactive({
  message: '你好，请介绍一下你自己'
})
const testResult = ref('')
const currentTestConfig = ref<AiConfig | null>(null)

// 当前编辑ID
const currentEditId = ref<number | null>(null)

/**
 * 页面加载时获取配置列表
 */
onMounted(() => {
  loadConfigList()
})

/**
 * 加载配置列表
 */
async function loadConfigList() {
  try {
    const response = await getAiConfigList()
    configList.value = response.data
  } catch (error) {
    ElMessage.error('加载配置列表失败')
  }
}

/**
 * 获取模型类型标签样式
 */
function getModelTypeTag(type: string): string {
  switch (type) {
    case 'openai':
      return 'primary'
    case 'dashscope':
      return 'success'
    default:
      return 'info'
  }
}

/**
 * 获取模型类型名称
 */
function getModelTypeName(type: string): string {
  switch (type) {
    case 'openai':
      return 'OpenAI协议'
    case 'dashscope':
      return '阿里云通义千问'
    default:
      return type
  }
}

/**
 * 打开新增弹窗
 */
function handleAdd() {
  isEdit.value = false
  currentEditId.value = null
  resetForm()
  dialogVisible.value = true
}

/**
 * 打开编辑弹窗
 */
function handleEdit(config: AiConfig) {
  isEdit.value = true
  currentEditId.value = config.id
  form.modelName = config.modelName
  form.modelType = config.modelType
  form.apiUrl = config.apiUrl
  form.apiKey = ''
  form.nickname = config.nickname || ''
  form.systemPrompt = config.systemPrompt || ''
  form.temperature = config.temperature
  form.maxTokens = config.maxTokens
  dialogVisible.value = true
}

/**
 * 重置表单
 */
function resetForm() {
  form.modelName = ''
  form.modelType = ''
  form.apiUrl = ''
  form.apiKey = ''
  form.nickname = ''
  form.systemPrompt = ''
  form.temperature = 0.7
  form.maxTokens = 800
}

/**
 * 提交表单
 */
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    submitLoading.value = true

    if (isEdit.value && currentEditId.value) {
      await updateAiConfig(currentEditId.value, form)
      ElMessage.success('更新成功')
    } else {
      await addAiConfig(form)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    loadConfigList()
  } catch (error) {
    // 表单校验失败
  } finally {
    submitLoading.value = false
  }
}

/**
 * 启用/禁用配置
 */
async function handleEnable(config: AiConfig) {
  try {
    await enableAiConfig(config.id)
    ElMessage.success(config.isEnabled === 1 ? '禁用成功' : '启用成功')
    loadConfigList()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

/**
 * 打开测试弹窗
 */
function handleTest(config: AiConfig) {
  currentTestConfig.value = config
  testForm.message = '你好，请介绍一下你自己'
  testResult.value = ''
  testDialogVisible.value = true
}

/**
 * 发送测试请求
 */
async function handleTestSubmit() {
  if (!currentTestConfig.value) return

  testLoading.value = true

  try {
    const response = await testAiConfig(testForm.message)
    testResult.value = response.data.response
  } catch (error: any) {
    testResult.value = error.message || '测试失败'
  } finally {
    testLoading.value = false
  }
}

/**
 * 删除配置
 */
async function handleDelete(config: AiConfig) {
  try {
    await ElMessageBox.confirm(`确定要删除配置「${config.modelName}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteAiConfig(config.id)
    ElMessage.success('删除成功')
    loadConfigList()
  } catch (error) {
    // 用户取消删除
  }
}
</script>

<style scoped>
.ai-config-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  margin: 0;
}

.config-card {
  height: calc(100vh - 180px);
}
</style>