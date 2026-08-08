package com.nutrition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nutrition.entity.RagKnowledgeDocument;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * RAG知识库文档Mapper
 */
@Mapper
public interface RagKnowledgeDocumentMapper extends BaseMapper<RagKnowledgeDocument> {

    /**
     * 物理删除指定file_md5且delete_flag=1的旧记录
     * 用于清理遗留的失败记录，避免唯一索引(file_md5+delete_flag)冲突
     * 注意：使用原生SQL绕过MyBatis-Plus逻辑删除拦截
     *
     * @param fileMd5 文件MD5
     * @return 删除行数
     */
    @Delete("DELETE FROM rag_knowledge_document WHERE file_md5 = #{fileMd5} AND delete_flag = 1")
    int physicalDeleteByMd5(@Param("fileMd5") String fileMd5);

    /**
     * 根据ID查询文档（忽略逻辑删除条件，用于Python回调等场景）
     * 注意：使用原生SQL绕过MyBatis-Plus逻辑删除拦截
     *
     * @param id 文档ID
     * @return 文档实体
     */
    @Select("SELECT * FROM rag_knowledge_document WHERE id = #{id}")
    RagKnowledgeDocument selectByIdIgnoreDeleteFlag(@Param("id") Long id);
}
