import { Typography } from 'antd'
import { InfoCircleOutlined, GithubOutlined, BookOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

interface SettingsProps {
  visible: boolean
  onClose: () => void
}

export default function Settings({ visible, onClose }: SettingsProps) {
  if (!visible) return null

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'rgba(0, 0, 0, 0.5)',
        backdropFilter: 'blur(4px)',
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        animation: 'fadeIn 0.2s ease-out'
      }}
      onClick={onClose}
    >
      <div
        style={{
          background: '#fff',
          borderRadius: 16,
          width: 480,
          maxWidth: '90vw',
          padding: 32,
          boxShadow: '0 20px 60px rgba(0, 0, 0, 0.15)',
          animation: 'fadeIn 0.3s ease-out'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* 头部 */}
        <div style={{ marginBottom: 28 }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            marginBottom: 8
          }}>
            <div style={{
              width: 40,
              height: 40,
              borderRadius: 12,
              background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <InfoCircleOutlined style={{ color: 'white', fontSize: 20 }} />
            </div>
            <Title level={3} style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>
              关于
            </Title>
          </div>
          <Text type="secondary" style={{ fontSize: 14, marginLeft: 52 }}>
            了解 AI Agent Demo
          </Text>
        </div>

        {/* 内容 */}
        <div style={{
          background: '#f9fafb',
          borderRadius: 12,
          padding: 20,
          marginBottom: 24
        }}>
          <div style={{ marginBottom: 20 }}>
            <Text strong style={{ fontSize: 15, color: '#1f2937', display: 'block', marginBottom: 8 }}>
              AI Agent Demo
            </Text>
            <Text type="secondary" style={{ fontSize: 13, lineHeight: '1.6' }}>
              基于 ReAct 模式的智能助手，能够理解和执行复杂任务
            </Text>
          </div>

          <div style={{ marginBottom: 20 }}>
            <Text strong style={{ fontSize: 15, color: '#1f2937', display: 'block', marginBottom: 8 }}>
              技术特点
            </Text>
            <ul style={{ margin: 0, paddingLeft: 16, fontSize: 13, color: '#4b5563', lineHeight: '1.8' }}>
              <li>ReAct 推理模式</li>
              <li>MCP 协议支持</li>
              <li>工具调用能力</li>
              <li>对话历史管理</li>
            </ul>
          </div>

          <div>
            <Text strong style={{ fontSize: 15, color: '#1f2937', display: 'block', marginBottom: 8 }}>
              资源链接
            </Text>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <a
                href="https://github.com"
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '8px 14px',
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: 8,
                  fontSize: 13,
                  color: '#374151',
                  textDecoration: 'none',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = '#3b82f6'
                  e.currentTarget.style.color = '#3b82f6'
                  e.currentTarget.style.transform = 'translateY(-1px)'
                  e.currentTarget.style.boxShadow = '0 2px 8px rgba(59, 130, 246, 0.2)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = '#e5e7eb'
                  e.currentTarget.style.color = '#374151'
                  e.currentTarget.style.transform = 'translateY(0)'
                  e.currentTarget.style.boxShadow = 'none'
                }}
              >
                <GithubOutlined style={{ fontSize: 14 }} />
                GitHub 仓库
              </a>
              <a
                href="#"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '8px 14px',
                  background: 'white',
                  border: '1px solid #e5e7eb',
                  borderRadius: 8,
                  fontSize: 13,
                  color: '#374151',
                  textDecoration: 'none',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.borderColor = '#3b82f6'
                  e.currentTarget.style.color = '#3b82f6'
                  e.currentTarget.style.transform = 'translateY(-1px)'
                  e.currentTarget.style.boxShadow = '0 2px 8px rgba(59, 130, 246, 0.2)'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.borderColor = '#e5e7eb'
                  e.currentTarget.style.color = '#374151'
                  e.currentTarget.style.transform = 'translateY(0)'
                  e.currentTarget.style.boxShadow = 'none'
                }}
              >
                <BookOutlined style={{ fontSize: 14 }} />
                使用文档
              </a>
            </div>
          </div>
        </div>

        {/* 底部按钮 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12 }}>
          <button
            onClick={onClose}
            style={{
              padding: '10px 24px',
              background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
              color: 'white',
              border: 'none',
              borderRadius: 10,
              fontSize: 14,
              fontWeight: 500,
              cursor: 'pointer',
              boxShadow: '0 2px 8px rgba(59, 130, 246, 0.3)',
              transition: 'all 0.2s'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.transform = 'translateY(-2px)'
              e.currentTarget.style.boxShadow = '0 4px 16px rgba(59, 130, 246, 0.4)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.transform = 'translateY(0)'
              e.currentTarget.style.boxShadow = '0 2px 8px rgba(59, 130, 246, 0.3)'
            }}
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  )
}
