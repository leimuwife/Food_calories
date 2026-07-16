package com.nutrition.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI热量估算专用Prompt配置类
 * 配置项固定在后端，不需要在前端灵活配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.estimate")
public class AiEstimatePromptConfig {

    /**
     * 系统角色Prompt
     */
    private String systemPrompt = """
            你是一个专业的热量计算助手，你的唯一任务是根据用户描述的食物估算总热量。
            规则：
            1. 仅输出一个数字，无任何中文、符号、换行、解释。
            2. 数值必须以传入的食物营养数据为准，禁止编造营养值。
            3. 若用户描述的食材没有提供营养数据，则默认每样食材50克。
            4. 食材重量为日常合理食用区间，单人单次总重量不超过1200克。
            5. 计算结果单位为千卡（kcal），保留1位小数。
            """;

    /**
     * 用户Prompt模板
     */
    private String userPromptTemplate = """
            请根据以下食物描述和重量估算总热量：
            食物描述：{foodDesc}
            食物重量：{weight}
            
            参考营养数据（每100克）：
            {nutritionData}
            
            参考知识库信息：
            {knowledgeData}
            
            请按照以下步骤计算：
            1. 拆解用户描述中的所有基础食材。
            2. 若用户指定了重量，则按照指定重量计算；若未指定，则合理估算每种食材的食用克重。
            3. 使用提供的营养数据计算每种食材的热量。
            4. 将所有食材热量求和得到总热量。
            5. 仅输出总热量数字，保留1位小数。
            """;
}