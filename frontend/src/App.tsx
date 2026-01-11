import { useState, useRef } from 'react'
import { Layout, message } from 'antd'
import { PlusOutlined, SettingOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ChatPanel from './components/ChatPanel'
import ConversationList from './components/ConversationList'
import ThoughtSteps from './components/ThoughtSteps'
import Settings from './components/Settings'
import { useSSE } from './hooks/useSSE'
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
      thoughtStepsData?: any[]  // 流式思考步骤
      toolCalls?: string[]  // 流式工具调用记录
      success?: boolean
      streaming?: boolean  // 是否正在流式生成中
      tokenStats?: {
        totalTokens: number
        inputTokens: number
        outputTokens: number
      }
    }>
  }>>([])

  const [currentConversationId, setCurrentConversationId] = useState<string | null>(null)
  const [settingsVisible, setSettingsVisible] = useState(false)
  const { sendSSE } = useSSE()

  // 存储当前SSE请求的controller，用于中止
  const sseControllerRef = useRef<ReturnType<typeof sendSSE> | null>(null)

  const handleSendMessage = (content: string) => {
    // 中止之前的SSE请求
    if (sseControllerRef.current) {
      sseControllerRef.current.abort()
      sseControllerRef.current = null
    }

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

    // 创建流式助手消息（初始状态）
    const assistantMessageId = (Date.now() + 1).toString()
    const initialAssistantMessage = {
      id: assistantMessageId,
      role: 'assistant' as const,
      content: '',
      timestamp: new Date().toISOString(),
      streaming: true,
      thoughtStepsData: [],
      toolCalls: []
    }

    setConversations(prev => prev.map(conv => {
      if (conv.id === conversationId) {
        return {
          ...conv,
          messages: [...conv.messages, initialAssistantMessage]
        }
      }
      return conv
    }))

    // 发送 SSE 请求并保存 controller
    const controller = sendSSE(content, backendConversationId, {
      onMessage: (msg) => {
        setConversations(prev => prev.map(conv => {
          if (conv.id === conversationId) {
            const messages = [...conv.messages]
            const lastMessage = messages[messages.length - 1]

            if (lastMessage?.id === assistantMessageId) {
              // 更新流式消息
              if (msg.type === 'content') {
                lastMessage.content += msg.data
              } else if (msg.type === 'tool_call') {
                lastMessage.toolCalls = [...(lastMessage.toolCalls || []), `工具调用: ${msg.data}`]
                lastMessage.thoughtStepsData = [
                  ...(lastMessage.thoughtStepsData || []),
                  { type: 'ACTION', description: `调用工具: ${msg.data}` }
                ]
              } else if (msg.type === 'tool_result') {
                lastMessage.toolCalls = [...(lastMessage.toolCalls || []), `工具结果: ${msg.data.substring(0, 100)}...`]
                lastMessage.thoughtStepsData = [
                  ...(lastMessage.thoughtStepsData || []),
                  { type: 'OBSERVATION', description: msg.data.substring(0, 200) }
                ]
              } else if (msg.type === 'error') {
                lastMessage.content += `\n\n错误: ${msg.data}`
                lastMessage.success = false
              }
            }
            return { ...conv, messages }
          }
          return conv
        }))
      },
      onError: (error) => {
        console.error('SSE error:', error)
        message.error('连接中断，请重试')

        setConversations(prev => prev.map(conv => {
          if (conv.id === conversationId) {
            const messages = [...conv.messages]
            const lastMessage = messages[messages.length - 1]

            if (lastMessage?.id === assistantMessageId) {
              lastMessage.content = '错误：' + error.message
              lastMessage.success = false
              lastMessage.streaming = false
            }
            return { ...conv, messages }
          }
          return conv
        }))
        // 清除 controller
        sseControllerRef.current = null
      },
      onComplete: (convId) => {
        setConversations(prev => prev.map(conv => {
          if (conv.id === conversationId) {
            const messages = [...conv.messages]
            const lastMessage = messages[messages.length - 1]

            if (lastMessage?.id === assistantMessageId) {
              lastMessage.streaming = false
              lastMessage.thoughtSteps = lastMessage.thoughtStepsData || []

              // 如果没有内容且有错误，显示失败状态
              if (!lastMessage.content && !lastMessage.success) {
                lastMessage.success = false
                lastMessage.content = '抱歉，未能获取响应'
              } else if (lastMessage.content) {
                lastMessage.success = true
              }
            }

            // 保存后端返回的 conversationId
            if (convId && !conv.backendConversationId) {
              return { ...conv, backendConversationId: convId, messages }
            }
            return { ...conv, messages }
          }
          return conv
        }))
        // 清除 controller
        sseControllerRef.current = null
      }
    })

    sseControllerRef.current = controller
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

  const handleStopGeneration = () => {
    if (sseControllerRef.current) {
      sseControllerRef.current.abort()
      sseControllerRef.current = null

      // 更新当前流式消息状态
      setConversations(prev => prev.map(conv => {
        if (conv.id === currentConversationId) {
          const messages = [...conv.messages]
          const lastMessage = messages[messages.length - 1]

          if (lastMessage?.streaming) {
            lastMessage.streaming = false
            lastMessage.content += '\n\n*（已停止生成）*'
          }
          return { ...conv, messages }
        }
        return conv
      }))
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
              loading={currentConversation?.messages.some(m => m.streaming) ?? false}
              onStopGeneration={handleStopGeneration}
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
                .flatMap(m => m.thoughtStepsData || m.thoughtSteps || []) || []
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
