import { useState } from 'react'
import { Layout, message } from 'antd'
import { PlusOutlined, SettingOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ChatPanel from './components/ChatPanel'
import ConversationList from './components/ConversationList'
import ThoughtSteps from './components/ThoughtSteps'
import Settings from './components/Settings'
import { API_CONFIG } from './config/api'
import './App.css'

const { Header, Content, Sider } = Layout

function App() {
  const [conversations, setConversations] = useState<Array<{
    id: string
    title: string
    createdAt: string
    backendConversationId?: string  // 后端返回的真实 conversationId
    messages: Array<{
      id: string
      role: 'user' | 'assistant'
      content: string
      timestamp: string
      thoughtSteps?: any[]
      success?: boolean
      tokenStats?: {
        totalTokens: number
        inputTokens: number
        outputTokens: number
      }
    }>
  }>>([])

  const [currentConversationId, setCurrentConversationId] = useState<string | null>(null)
  const [settingsVisible, setSettingsVisible] = useState(false)

  const handleSendMessage = async (content: string) => {

    // 创建或获取对话
    let conversationId = currentConversationId
    // 获取后端返回的真实 conversationId
    let backendConversationId: string | undefined = conversations.find(c => c.id === conversationId)?.backendConversationId

    if (!conversationId) {
      const newConversation = {
        id: Date.now().toString(),
        title: content.slice(0, 30) + (content.length > 30 ? '...' : ''),
        createdAt: new Date().toISOString(),
        messages: []
      }

      setConversations(prev => [newConversation, ...prev])
      conversationId = newConversation.id
      setCurrentConversationId(conversationId)
    }

    // 添加用户消息
    const userMessage = {
      id: Date.now().toString(),
      role: 'user' as const,
      content,
      timestamp: new Date().toISOString()
    }

    setConversations(prev => prev.map(conv => {
      if (conv.id === conversationId) {
        return {
          ...conv,
          messages: [...conv.messages, userMessage]
        }
      }
      return conv
    }))

    try {
      // 调用 API
      const response = await fetch(`${API_CONFIG.baseUrl}/api/agent/execute`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          task: content,
          conversationId: backendConversationId  // 使用后端返回的 conversationId
        })
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()

      // 保存后端返回的 conversationId
      if (data.conversationId && !backendConversationId) {
        setConversations(prev => prev.map(conv => {
          if (conv.id === conversationId) {
            return { ...conv, backendConversationId: data.conversationId }
          }
          return conv
        }))
        backendConversationId = data.conversationId
      }

      // 添加助手消息
      const assistantMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant' as const,
        content: data.finalAnswer || data.errorMessage || '抱歉，出现问题',
        timestamp: new Date().toISOString(),
        thoughtSteps: data.thoughtSteps || [],
        success: data.success,
        tokenStats: data.tokenStats || undefined
      }

      setConversations(prev => prev.map(conv => {
        if (conv.id === conversationId) {
          return {
            ...conv,
            messages: [...conv.messages, assistantMessage]
          }
        }
        return conv
      }))

    } catch (error) {
      console.error('Error sending message:', error)
      message.error('发送失败，请检查网络连接')

      // 添加错误消息
      const errorMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant' as const,
        content: `错误：${error instanceof Error ? error.message : '未知错误'}`,
        timestamp: new Date().toISOString()
      }

      setConversations(prev => prev.map(conv => {
        if (conv.id === conversationId) {
          return {
            ...conv,
            messages: [...conv.messages, errorMessage]
          }
        }
        return conv
      }))
    }
  }

  const handleNewConversation = () => {
    setCurrentConversationId(null)
  }

  const handleSelectConversation = (id: string) => {
    setCurrentConversationId(id)
  }

  const handleDeleteConversation = (id: string) => {
    setConversations(prev => prev.filter(conv => conv.id !== id))
    if (currentConversationId === id) {
      setCurrentConversationId(null)
    }
  }

  const currentConversation = conversations.find(c => c.id === currentConversationId)

  const headerButtonStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '8px 16px',
    border: '1px solid #e5e7eb',
    borderRadius: 12,
    background: 'white',
    color: '#4b5563',
    fontSize: 14,
    fontWeight: 500,
    cursor: 'pointer',
    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
    boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
  }

  const headerButtonHoverStyle = {
    background: '#f9fafb',
    borderColor: '#d1d5db',
    transform: 'translateY(-1px)',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.08)'
  }

  return (
    <Layout style={{ height: '100vh', background: '#f5f7fa' }}>
      <Header style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 28px',
        height: 64,
        background: 'white',
        borderBottom: '1px solid #e5e7eb',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)',
        position: 'relative',
        zIndex: 10
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <div style={{
              width: 36,
              height: 36,
              borderRadius: 10,
              background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 8px rgba(59, 130, 246, 0.3)'
            }}>
              <ThunderboltOutlined style={{ color: 'white', fontSize: 18 }} />
            </div>
            <div>
              <div style={{
                fontSize: 18,
                fontWeight: 700,
                background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text',
                letterSpacing: '-0.5px'
              }}>
                AI Agent Demo
              </div>
              <div style={{ fontSize: 11, color: '#9ca3af', fontWeight: 500, letterSpacing: '0.3px' }}>
                基于 ReAct 模式的智能助手
              </div>
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button
            onClick={handleNewConversation}
            style={headerButtonStyle}
            onMouseEnter={(e) => {
              Object.assign(e.currentTarget.style, headerButtonHoverStyle)
            }}
            onMouseLeave={(e) => {
              Object.assign(e.currentTarget.style, headerButtonStyle)
            }}
          >
            <PlusOutlined style={{ fontSize: 14 }} />
            新对话
          </button>
          <button
            onClick={() => setSettingsVisible(true)}
            style={headerButtonStyle}
            onMouseEnter={(e) => {
              Object.assign(e.currentTarget.style, headerButtonHoverStyle)
            }}
            onMouseLeave={(e) => {
              Object.assign(e.currentTarget.style, headerButtonStyle)
            }}
          >
            <SettingOutlined style={{ fontSize: 14 }} />
            设置
          </button>
        </div>
      </Header>

      <Layout>
        <Sider width={300} style={{
          background: 'white',
          borderRight: '1px solid #e5e7eb',
          overflow: 'hidden'
        }}>
          <ConversationList
            conversations={conversations}
            currentId={currentConversationId}
            onSelect={handleSelectConversation}
            onDelete={handleDeleteConversation}
          />
        </Sider>

        <Content style={{ display: 'flex', background: '#f5f7fa' }}>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <ChatPanel
              messages={currentConversation?.messages || []}
              onSendMessage={handleSendMessage}
              loading={false}
            />
          </div>

          <div style={{
            width: 380,
            borderLeft: '1px solid #e5e7eb',
            background: 'white',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden'
          }}>
            <div style={{
              padding: '20px 24px',
              borderBottom: '1px solid #e5e7eb',
              background: 'linear-gradient(to bottom, #fafbfc, #ffffff)'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                fontSize: 15,
                fontWeight: 600,
                color: '#1f2937'
              }}>
                <div style={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
                  boxShadow: '0 0 8px rgba(59, 130, 246, 0.5)'
                }} />
                思考过程
              </div>
            </div>
            <ThoughtSteps
              steps={currentConversation?.messages
                .filter(m => m.role === 'assistant')
                .flatMap(m => m.thoughtSteps || []) || []
              }
            />
          </div>
        </Content>
      </Layout>

      <Settings
        visible={settingsVisible}
        onClose={() => setSettingsVisible(false)}
      />
    </Layout>
  )
}

export default App
