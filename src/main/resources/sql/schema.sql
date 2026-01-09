-- Agent Demo 数据库建表脚本
-- PostgreSQL 15+

-- 创建数据库（如果不存在）
-- 注意：在某些环境中，这可能需要单独的命令而不是在同一个脚本中执行
CREATE DATABASE agent_db;

-- 使用数据库
\c agent_db;

-- ============================================================================
-- 安装 pgvector 扩展（用于向量搜索）
-- ============================================================================
-- 注意：需要先在系统中安装 pgvector 扩展
-- Ubuntu/Debian: sudo apt-get install postgresql-15-pgvector
-- 或从源码编译: https://github.com/pgvector/pgvector

CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================================
-- 删除已存在的表（注意：按依赖关系倒序删除）
-- ============================================================================
DROP TABLE IF EXISTS vector_store CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS executions CASCADE;
DROP TABLE IF EXISTS conversations CASCADE;

-- 会话表
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255),
    model_provider VARCHAR(50),
    model_name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 检查约束：状态必须是有效的枚举值
    CONSTRAINT chk_conversation_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

-- 为会话表创建索引
CREATE INDEX idx_conversations_conversation_id ON conversations(conversation_id);
CREATE INDEX idx_conversations_status ON conversations(status);
CREATE INDEX idx_conversations_created_at ON conversations(created_at);

-- 任务执行表
CREATE TABLE executions (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL UNIQUE,
    conversation_id BIGINT NOT NULL,
    task TEXT NOT NULL,
    final_answer TEXT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    steps INTEGER NOT NULL DEFAULT 0,
    duration_ms BIGINT,
    total_tokens BIGINT,
    thought_steps JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 外键约束
    CONSTRAINT fk_executions_conversation FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE,

    -- 检查约束：状态必须是有效的枚举值
    CONSTRAINT chk_execution_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'TIMEOUT'))
);

-- 为执行表创建索引
CREATE INDEX idx_executions_execution_id ON executions(execution_id);
CREATE INDEX idx_executions_conversation_id ON executions(conversation_id);
CREATE INDEX idx_executions_status ON executions(status);
CREATE INDEX idx_executions_created_at ON executions(created_at);

-- 添加表注释
COMMENT ON TABLE conversations IS '会话表，记录用户与Agent的对话会话';
COMMENT ON TABLE executions IS '任务执行表，记录每次任务的执行过程和结果';

-- 添加列注释
COMMENT ON COLUMN conversations.conversation_id IS '会话唯一标识';
COMMENT ON COLUMN conversations.title IS '会话标题，通常使用第一个任务的前50个字符';
COMMENT ON COLUMN conversations.model_provider IS '模型提供商（如：openai、ollama）';
COMMENT ON COLUMN conversations.model_name IS '使用的模型名称';
COMMENT ON COLUMN conversations.status IS '会话状态：ACTIVE-活跃，ARCHIVED-已归档，DELETED-已删除';

COMMENT ON COLUMN executions.execution_id IS '执行唯一标识';
COMMENT ON COLUMN executions.conversation_id IS '关联的会话ID';
COMMENT ON COLUMN executions.task IS '用户输入的任务描述';
COMMENT ON COLUMN executions.final_answer IS 'Agent的最终答案';
COMMENT ON COLUMN executions.success IS '任务是否成功完成';
COMMENT ON COLUMN executions.error_message IS '失败时的错误信息';
COMMENT ON COLUMN executions.status IS '执行状态：IN_PROGRESS-进行中，COMPLETED-已完成，FAILED-失败，TIMEOUT-超时';
COMMENT ON COLUMN executions.steps IS '执行的思考步骤数';
COMMENT ON COLUMN executions.duration_ms IS '总耗时（毫秒）';
COMMENT ON COLUMN executions.total_tokens IS '消耗的Token数量';
COMMENT ON COLUMN executions.thought_steps IS '思考步骤，JSON格式存储';
COMMENT ON COLUMN executions.completed_at IS '任务完成时间';

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为conversations表创建更新时间触发器
CREATE TRIGGER update_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为executions表创建更新时间触发器
CREATE TRIGGER update_executions_updated_at
    BEFORE UPDATE ON executions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 文档表（用于向量搜索示例）
-- ============================================================================
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    metadata VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 为文档表创建索引
CREATE INDEX idx_documents_title ON documents(title);
CREATE INDEX idx_documents_created_at ON documents(created_at);

-- 添加表注释
COMMENT ON TABLE documents IS '文档表，用于演示向量搜索功能';
COMMENT ON COLUMN documents.title IS '文档标题';
COMMENT ON COLUMN documents.content IS '文档内容';
COMMENT ON COLUMN documents.metadata IS '元数据（JSON格式）';

-- 为documents表创建更新时间触发器
CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- Spring AI Vector Store 表（自动创建）
-- ============================================================================
-- 注意：Spring AI 会在首次使用时自动创建 vector_store 表
-- 如果需要手动创建，可以使用以下语句：

/*
CREATE TABLE IF NOT EXISTS vector_store (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536),  -- 1536 是 OpenAI text-embedding-3-small 的维度
    UNIQUE (content, (metadata::text))
);

-- 创建向量索引（用于相似度搜索）
CREATE INDEX ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 添加表注释
COMMENT ON TABLE vector_store IS 'Spring AI 向量存储表';
COMMENT ON COLUMN vector_store.embedding IS '文档的向量表示（1536维）';
COMMENT ON COLUMN vector_store.metadata IS '文档元数据（JSONB格式）';
*/
