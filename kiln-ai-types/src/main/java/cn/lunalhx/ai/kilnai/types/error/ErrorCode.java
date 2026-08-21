package cn.lunalhx.ai.kilnai.types.error;

public enum ErrorCode {

    /**
     * 请求本身不合法：缺少必填字段，或命令带了不该带的参数
     * （例如 retry_requested 带了业务字段）。映射为 HTTP 400。
     */
    INVALID_ARGUMENT,

    /**
     * 目标 Flow 不存在。映射为 HTTP 404。
     */
    FLOW_NOT_FOUND,

    /**
     * 目标复习任务不存在。映射为 HTTP 404。
     */
    REVIEW_NOT_FOUND,

    /**
     * 命令与当前已提交状态冲突：interactionVersion 过期、attemptId
     * 不是当前交互指向的 attempt、当前边界不允许这个命令，或同一
     * 学习者同一概念已有未完成 Flow（ADR-0070）。映射为 HTTP 409，
     * 客户端应重新读取最新交互后再操作。
     */
    CONFLICT,

    /**
     * 提交格式合法但当前状态不接受（例如 Attempt 不可提交），
     * 或参数校验失败。映射为 HTTP 422。
     */
    UNPROCESSABLE,

    /**
     * 模型生成失败，没能产出契约合法的任务或教学内容；没有持久化
     * 任何痕迹，可以用原 Idempotency-Key 重试。映射为 HTTP 503。
     */
    SERVICE_UNAVAILABLE,

    /**
     * 模型输出即使经过唯一一次修复仍违反契约，或冻结的模型配置
     * 解析失败；系统 fail-closed，属于内部错误。映射为 HTTP 500，
     * 不向学习者暴露。
     */
    MODEL_CONTRACT_INVALID
}
