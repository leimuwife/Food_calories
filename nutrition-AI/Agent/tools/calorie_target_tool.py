"""工具一：calorie_target_suggest - 每日热量目标建议

功能：根据用户身高、体重、性别、活动量计算BMR基础代谢率，
分别输出维持体重热量、温和减脂热量区间、快速减脂热量区间。
返回人性化中文字符串，供大模型直接拼装回答。
"""
from loguru import logger

from constants.global_constants import AgentConstants

# ---- 工具元信息（注册到tool_registry使用） ----
TOOL_NAME = "calorie_target_suggest"
TOOL_DESCRIPTION = (
    "当用户需要获取适合自己的每日摄入热量、减脂热量、维持体重热量的时候调用本工具。"
    "可以接收用户的身高、体重、性别、日常运动量；"
    "运动量参数为选填，如果用户没有提供则默认普通轻度活动。"
    "工具内部计算BMR基础代谢率，分别计算维持体重所需热量、温和减脂安全热量区间、快速减脂热量区间，"
    "最终返回人性化的文字结果，附带各项热量说明以及简单的饮食建议。"
)
TOOL_PARAMETERS = [
    {"name": "height", "type": "float", "required": True, "description": "用户身高，单位cm"},
    {"name": "weight", "type": "float", "required": True, "description": "用户体重，单位kg"},
    {"name": "gender", "type": "str", "required": True, "description": "性别，取值为男/女"},
    {"name": "activity_level", "type": "str", "required": False,
     "description": "日常运动量，可选：久坐办公、轻度活动、中度活动、高强度体力运动；不传默认久坐办公"},
]
TOOL_RETURN_FORMAT = "纯文本字符串，包含BMR、维持热量、温和减脂区间、高强度减脂区间及饮食建议"


def calorie_target_suggest(height: float, weight: float, gender: str,
                           activity_level: str = None) -> str:
    """
    计算每日热量目标建议

    Args:
        height: 身高(cm)
        weight: 体重(kg)
        gender: 性别（男/女）
        activity_level: 活动量等级（可选）

    Returns:
        通俗易懂的中文字符串，包含各项热量目标与饮食建议
    """
    # 1. 参数校验：关键参数缺失时返回提示文本告知AI向用户收集
    missing = []
    if height is None or height <= 0:
        missing.append("身高")
    if weight is None or weight <= 0:
        missing.append("体重")
    if gender is None or gender.strip() == "":
        missing.append("性别")

    if missing:
        logger.warning("calorie_target_suggest 参数缺失: {}", missing)
        return f"缺少必要参数：{'、'.join(missing)}。{AgentConstants.MSG_MISSING_PARAMS}"

    # 2. 活动系数匹配
    activity = activity_level or AgentConstants.DEFAULT_ACTIVITY_LEVEL
    factor = AgentConstants.ACTIVITY_FACTORS.get(activity)
    if factor is None:
        logger.warning("calorie_target_suggest 未识别活动量: {}, 使用默认值", activity)
        activity = AgentConstants.DEFAULT_ACTIVITY_LEVEL
        factor = AgentConstants.ACTIVITY_FACTORS[activity]
        activity_note = AgentConstants.MSG_INVALID_ACTIVITY
    else:
        activity_note = ""

    # 3. BMR计算（Mifflin-St Jeor公式）
    # 用户未提供年龄时使用默认估算年龄
    age = AgentConstants.DEFAULT_AGE
    age_note = "（用户未提供年龄，按成年普通年龄25岁估算）"

    gender_str = gender.strip()
    if gender_str == "男":
        bmr = 10 * weight + 6.25 * height - 5 * age + 5
    elif gender_str == "女":
        bmr = 10 * weight + 6.25 * height - 5 * age - 161
    else:
        logger.warning("calorie_target_suggest 未识别性别: {}", gender)
        return f"性别参数无效（应为男或女）。{AgentConstants.MSG_MISSING_PARAMS}"

    bmr = round(bmr)
    maintenance = round(bmr * factor)
    mild_min = round((maintenance - AgentConstants.MILD_DEFICIT_MAX) * AgentConstants.MILD_DEFICIT_FACTOR)
    mild_max = round((maintenance - AgentConstants.MILD_DEFICIT_MIN) * AgentConstants.MILD_DEFICIT_FACTOR)
    aggressive = round((maintenance - AgentConstants.AGGRESSIVE_DEFICIT) * AgentConstants.AGGRESSIVE_DEFICIT_FACTOR)

    logger.info("热量目标计算: height={}, weight={}, gender={}, activity={}, BMR={}, maintenance={}",
                height, weight, gender_str, activity, bmr, maintenance)

    # 4. 拼装人性化文本结果
    lines = [
        f"根据您的身高{height:.0f}cm、体重{weight:.1f}kg、性别{gender_str}，计算结果如下：",
        age_note,
        f"基础代谢率(BMR)：{bmr} 大卡/天",
        f"日常活动量：{activity}",
        "",
        f"1. 维持体重：每日摄入约 {maintenance} 大卡",
        f"2. 温和减脂（每周减重约0.25-0.5kg）：每日摄入 {mild_min}-{mild_max} 大卡",
        f"3. 高强度减脂（每周减重约0.5-0.75kg）：每日摄入约 {aggressive} 大卡",
        "",
        "饮食建议：",
        f"- 蛋白质摄入量建议不低于体重(kg)×1.2g，即约{weight * 1.2:.0f}g/天",
        "- 碳水化合物选择粗粮、全谷物，避免精制糖",
        "- 脂肪以不饱和脂肪酸为主，控制总脂肪摄入在总热量的30%以下",
        "- 建议少食多餐，每日3正餐+1-2次健康加餐",
    ]

    if activity_note:
        lines.append(f"\n注：{activity_note}")

    lines.append("\n温馨提示：以上数据基于Mifflin-St Jeor公式估算，实际热量需求因人而异，仅供参考。")
    return "\n".join(lines)
