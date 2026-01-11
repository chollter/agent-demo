import { useEffect, useRef, useState } from 'react'
import { Input, Button, Avatar, Empty } from 'antd'
import { SendOutlined, StopOutlined, UserOutlined, RobotOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism'
import dayjs from 'dayjs'

const { TextArea } = Input

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  thoughtSteps?: any[]
  thoughtStepsData?: any[]
  toolCalls?: string[]
  success?: boolean
  streaming?: boolean
  tokenStats?: {
    totalTokens: number
    inputTokens: number
    outputTokens: number
  }
}

interface ChatPanelProps {
  messages: Message[]
  onSendMessage: (content: string) => void
  loading: boolean
  onStopGeneration?: () => void
}

export default function ChatPanel({ messages, onSendMessage, loading, onStopGeneration }: ChatPanelProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const [inputValue, setInputValue] = useState('')

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSend = () => {
    if (inputValue.trim()) {
      onSendMessage(inputValue.trim())
      setInputValue('')
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const exampleQuestions = [
    '帮我计算 25 × 36',
    '北京今天天气怎么样？',
    '现在几点了？',
    '搜索 AI 相关内容'
  ]

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: '#f5f7fa' }}>
      {/* 消息列表 */}
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: 20
      }}>
        {messages.length === 0 ? (
          <Empty
            style={{ marginTop: 60 }}
            description={
              <div style={{ textAlign: 'center' }}>
                <div style={{
                  width: 80,
                  height: 80,
                  margin: '0 auto 20px',
                  borderRadius: 20,
                  background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 8px 24px rgba(59, 130, 246, 0.3)'
                }}>
                  <RobotOutlined style={{ color: 'white', fontSize: 40 }} />
                </div>
                <div style={{ fontSize: 18, marginBottom: 8, color: '#1f2937', fontWeight: 600 }}>
                  欢迎使用 AI Agent Demo
                </div>
                <div style={{ fontSize: 14, color: '#6b7280', marginBottom: 28 }}>
                  基于 ReAct 模式的智能助手，帮你处理各种任务
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'center' }}>
                  {exampleQuestions.map((example, i) => (
                    <button
                      key={i}
                      onClick={() => setInputValue(example)}
                      style={{
                        padding: '10px 20px',
                        border: '1px solid #e5e7eb',
                        borderRadius: 12,
                        background: 'white',
                        cursor: 'pointer',
                        fontSize: 13,
                        color: '#4b5563',
                        transition: 'all 0.2s',
                        width: 'fit-content',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.borderColor = '#3b82f6'
                        e.currentTarget.style.color = '#3b82f6'
                        e.currentTarget.style.transform = 'translateX(4px)'
                        e.currentTarget.style.boxShadow = '0 2px 8px rgba(59, 130, 246, 0.15)'
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.borderColor = '#e5e7eb'
                        e.currentTarget.style.color = '#4b5563'
                        e.currentTarget.style.transform = 'translateX(0)'
                        e.currentTarget.style.boxShadow = 'none'
                      }}
                    >
                      <ThunderboltOutlined style={{ fontSize: 12, color: '#f59e0b' }} />
                      {example}
                    </button>
                  ))}
                </div>
              </div>
            }
          />
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className="message-animate"
              style={{
                display: 'flex',
                gap: 12,
                justifyContent: message.role === 'user' ? 'flex-end' : 'flex-start'
              }}
            >
              {message.role === 'assistant' && (
                <Avatar
                  icon={<RobotOutlined />}
                  style={{
                    background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                    flexShrink: 0,
                    boxShadow: '0 2px 8px rgba(59, 130, 246, 0.2)'
                  }}
                />
              )}

              <div style={{
                maxWidth: '72%',
                display: 'flex',
                flexDirection: 'column',
                gap: 6
              }}>
                <div style={{
                  padding: '14px 18px',
                  borderRadius: message.role === 'user' ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
                  background: message.role === 'user'
                    ? 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)'
                    : '#ffffff',
                  color: message.role === 'user' ? '#fff' : '#1f2937',
                  wordBreak: 'break-word',
                  boxShadow: message.role === 'user'
                    ? '0 2px 8px rgba(59, 130, 246, 0.25)'
                    : '0 1px 3px rgba(0, 0, 0, 0.05)',
                  fontSize: 14,
                  lineHeight: '1.6'
                }}>
                  {message.role === 'assistant' ? (
                    <>
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={{
                          code({ node, inline, className, children, ...props }: any) {
                            const match = /language-(\w+)/.exec(className || '')
                            return !inline && match ? (
                              <SyntaxHighlighter
                                style={oneDark}
                                language={match[1]}
                                PreTag="div"
                                {...props}
                              >
                                {String(children).replace(/\n$/, '')}
                              </SyntaxHighlighter>
                            ) : (
                              <code
                                style={{
                                  background: '#f1f5f9',
                                  padding: '2px 6px',
                                  borderRadius: 4,
                                  fontSize: '0.9em',
                                  color: '#e11d48'
                                }}
                                {...props}
                              >
                                {children}
                              </code>
                            )
                          }
                        }}
                      >
                        {message.content}
                      </ReactMarkdown>
                      {/* 流式输出指示器 */}
                      {message.streaming && (
                        <span className="streaming-cursor"></span>
                      )}
                    </>
                  ) : (
                    <div>{message.content}</div>
                  )}
                </div>

                <div style={{
                  fontSize: 11,
                  color: '#9ca3af',
                  paddingLeft: 4,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6
                }}>
                  {dayjs(message.timestamp).format('HH:mm:ss')}
                  {message.role === 'assistant' && message.tokenStats && (
                    <span style={{
                      color: '#6b7280',
                      background: '#f3f4f6',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 10,
                      fontWeight: 500,
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4
                    }}>
                      🔢 {message.tokenStats.totalTokens.toLocaleString()} tokens
                      <span style={{ color: '#9ca3af' }}>
                        (↑{message.tokenStats.inputTokens.toLocaleString()} ↓{message.tokenStats.outputTokens.toLocaleString()})
                      </span>
                    </span>
                  )}
                  {message.role === 'assistant' && message.success === false && (
                    <span style={{
                      color: '#ef4444',
                      background: '#fef2f2',
                      padding: '2px 8px',
                      borderRadius: 4,
                      fontSize: 10,
                      fontWeight: 500
                    }}>
                      ✗ 失败
                    </span>
                  )}
                </div>
              </div>

              {message.role === 'user' && (
                <Avatar
                  icon={<UserOutlined />}
                  style={{
                    background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                    flexShrink: 0,
                    boxShadow: '0 2px 8px rgba(16, 185, 129, 0.2)'
                  }}
                />
              )}
            </div>
          ))
        )}

        {loading && (
          <div style={{ display: 'flex', gap: 12 }} className="message-animate">
            <Avatar
              icon={<RobotOutlined />}
              style={{
                background: 'linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)',
                boxShadow: '0 2px 8px rgba(59, 130, 246, 0.2)'
              }}
            />
            <div style={{
              padding: '14px 18px',
              background: '#ffffff',
              borderRadius: '18px 18px 18px 4px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.05)'
            }}>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <span className="typing-dot"></span>
                <span className="typing-dot"></span>
                <span className="typing-dot"></span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* 输入框 */}
      <div style={{
        padding: '16px 24px 20px',
        background: 'white',
        borderTop: '1px solid #e5e7eb'
      }}>
        <div style={{
          display: 'flex',
          gap: 12,
          alignItems: 'flex-end',
          background: '#f9fafb',
          border: '1px solid #e5e7eb',
          borderRadius: 14,
          padding: '8px 8px 8px 16px',
          transition: 'all 0.2s',
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor = '#3b82f6'
          e.currentTarget.style.background = 'white'
          e.currentTarget.style.boxShadow = '0 0 0 3px rgba(59, 130, 246, 0.1)'
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor = '#e5e7eb'
          e.currentTarget.style.background = '#f9fafb'
          e.currentTarget.style.boxShadow = '0 1px 2px rgba(0, 0, 0, 0.05)'
        }}>
          <TextArea
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyPress}
            placeholder="输入消息... (Enter 发送，Shift+Enter 换行)"
            autoSize={{ minRows: 1, maxRows: 6 }}
            disabled={loading}
            style={{
              fontSize: 14,
              border: 'none',
              background: 'transparent',
              resize: 'none',
              padding: 0,
              boxShadow: 'none'
            }}
          />
          {loading ? (
            <Button
              type="primary"
              danger
              icon={<StopOutlined />}
              onClick={onStopGeneration}
              style={{
                height: 40,
                borderRadius: 10,
                border: 'none',
                boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)',
                flexShrink: 0
              }}
            >
              停止
            </Button>
          ) : (
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSend}
              disabled={!inputValue.trim()}
              style={{
                height: 40,
                borderRadius: 10,
                background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
                border: 'none',
                boxShadow: '0 2px 8px rgba(59, 130, 246, 0.3)',
                flexShrink: 0
              }}
            >
              发送
            </Button>
          )}
        </div>
        <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 8, textAlign: 'center' }}>
          AI Agent 基于 ReAct 模式，可以理解并执行复杂任务
        </div>
      </div>
    </div>
  )
}
