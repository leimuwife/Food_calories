import request, { ApiResponse } from '../utils/request'

/**
 * 知识库文档信息
 */
export interface KnowledgeDocument {
  /** 文档ID */
  id: number
  /** 文档名称 */
  fileName: string
  /** 文件大小（字节） */
  fileSize: number
  /** 文件大小（可读格式） */
  fileSizeText: string
  /** 上传时间 */
  uploadTime: string
  /** 文件MD5标识 */
  fileMd5: string
  /** 附件在线访问地址（OSS） */
  fileUrl: string
}

/**
 * 上传结果
 */
export interface UploadResult {
  /** 上传成功标识 */
  success: boolean
  /** 是否重复文件 */
  duplicate: boolean
  /** 上传后的文档信息 */
  document: KnowledgeDocument
}

/**
 * 获取知识库文档列表
 * @param params 查询参数
 */
export function getKnowledgeList(params?: {
  keyword?: string
  pageNum?: number
  pageSize?: number
}): Promise<ApiResponse<{ records: KnowledgeDocument[]; total: number }>> {
  return request.get('/rag/knowledge/list', { params })
}

/**
 * 上传知识库文档
 * @param formData 包含文件的FormData
 */
export function uploadKnowledge(formData: FormData): Promise<ApiResponse<UploadResult>> {
  return request.post('/rag/knowledge/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * 删除知识库文档
 * @param id 文档ID
 */
export function deleteKnowledge(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/rag/knowledge/${id}`)
}
