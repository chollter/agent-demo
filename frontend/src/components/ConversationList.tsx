import { List, Typography, Popconfirm, Empty } from 'antd'
import { DeleteOutlined, MessageOutlined, ClockCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const { Text } = Typography

interface Conversation {
  id: string
  title: string
  createdAt: string
  messages: Array<any>
}

interface ConversationListProps {
  conversations: Conversation[]
  currentId: string | null
  onSelect: (id: string) => void
  onDelete: (id: string) => void
}

export default function ConversationList({
  conversations,
  currentId,
  onSelect,
  onDelete
}: ConversationListProps) {
  const itemStyle = {
    padding: '14px 18px',
    cursor: 'pointer',
    borderBottom: '1px solid #f3f4f6',
    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
    position: 'relative' as const,
    background: 'transparent'
  }

  const activeStyle = {
    background: 'linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%)',
    borderLeft: '3px solid #3b82f6'
  }

  const hoverStyle = {
    background: '#f9fafb'
  }

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* 头部 */}
      <div style={{
        padding: '20px 20px 16px',
        borderBottom: '1px solid #e5e7eb'
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
          对话历史
        </div>
      </div>

      {/* 列表 */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
        {conversations.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
              <div style={{ marginTop: 20 }}>
                <Text type="secondary" style={{ fontSize: 13 }}>暂无对话</Text>
                <div style={{ marginTop: 8, fontSize: 12, color: '#9ca3af' }}>
                  开始新对话来创建历史记录
                </div>
              </div>
            }
            style={{ marginTop: 60 }}
          />
        ) : (
          <List
            dataSource={conversations}
            renderItem={(conversation) => (
              <div
                style={{
                  ...itemStyle,
                  ...(conversation.id === currentId ? activeStyle : {})
                }}
                onClick={() => onSelect(conversation.id)}
                onMouseEnter={(e) => {
                  if (conversation.id !== currentId) {
                    Object.assign(e.currentTarget.style, hoverStyle)
                  }
                }}
                onMouseLeave={(e) => {
                  if (conversation.id !== currentId) {
                    e.currentTarget.style.background = 'transparent'
                  }
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{
                      fontSize: 14,
                      fontWeight: conversation.id === currentId ? 600 : 500,
                      marginBottom: 6,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                      color: conversation.id === currentId ? '#1f2937' : '#4b5563',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8
                    }}>
                      <MessageOutlined
                        style={{
                          fontSize: 14,
                          color: conversation.id === currentId ? '#3b82f6' : '#9ca3af',
                          flexShrink: 0
                        }}
                      />
                      <span style={{ fontSize: 13 }}>{conversation.title}</span>
                    </div>
                    <div style={{
                      fontSize: 12,
                      color: '#9ca3af',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4
                    }}>
                      <ClockCircleOutlined style={{ fontSize: 11 }} />
                      {dayjs(conversation.createdAt).fromNow()}
                    </div>
                  </div>

                  <Popconfirm
                    title="确认删除"
                    description="确定要删除此对话吗？"
                    onConfirm={(e) => {
                      e?.stopPropagation()
                      onDelete(conversation.id)
                    }}
                    onCancel={(e) => e?.stopPropagation()}
                    okText="删除"
                    cancelText="取消"
                    okButtonProps={{ danger: true }}
                  >
                    <DeleteOutlined
                      style={{
                        color: '#9ca3af',
                        padding: '6px',
                        borderRadius: 6,
                        transition: 'all 0.2s',
                        fontSize: 14
                      }}
                      onClick={(e) => e.stopPropagation()}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.color = '#ef4444'
                        e.currentTarget.style.background = '#fef2f2'
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.color = '#9ca3af'
                        e.currentTarget.style.background = 'transparent'
                      }}
                    />
                  </Popconfirm>
                </div>
              </div>
            )}
          />
        )}
      </div>
    </div>
  )
}
