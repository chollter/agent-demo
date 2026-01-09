import { Timeline, Empty } from 'antd'
import {
  BulbOutlined,
  ThunderboltOutlined,
  EyeOutlined,
  LoadingOutlined
} from '@ant-design/icons'

interface ThoughtStep {
  stepType: string
  content: string
}

interface ThoughtStepsProps {
  steps: ThoughtStep[]
}

export default function ThoughtSteps({ steps }: ThoughtStepsProps) {
  const getStepIcon = (stepType: string) => {
    switch (stepType) {
      case 'THOUGHT':
        return <LoadingOutlined style={{ color: '#3b82f6', fontSize: 16 }} />
      case 'ACTION':
        return <ThunderboltOutlined style={{ color: '#10b981', fontSize: 16 }} />
      case 'OBSERVATION':
        return <EyeOutlined style={{ color: '#f59e0b', fontSize: 16 }} />
      default:
        return <BulbOutlined style={{ color: '#8b5cf6', fontSize: 16 }} />
    }
  }

  const getStepColor = (stepType: string) => {
    switch (stepType) {
      case 'THOUGHT':
        return '#3b82f6'
      case 'ACTION':
        return '#10b981'
      case 'OBSERVATION':
        return '#f59e0b'
      default:
        return '#8b5cf6'
    }
  }

  const getStepLabel = (stepType: string) => {
    switch (stepType) {
      case 'THOUGHT':
        return '思考'
      case 'ACTION':
        return '行动'
      case 'OBSERVATION':
        return '观察'
      default:
        return '未知'
    }
  }

  const getStepBgColor = (stepType: string) => {
    switch (stepType) {
      case 'THOUGHT':
        return '#eff6ff'
      case 'ACTION':
        return '#ecfdf5'
      case 'OBSERVATION':
        return '#fffbeb'
      default:
        return '#f5f3ff'
    }
  }

  return (
    <div style={{
      flex: 1,
      overflowY: 'auto',
      padding: '16px'
    }}>
      {steps.length === 0 ? (
        <Empty
          image={
            <div style={{
              width: 60,
              height: 60,
              margin: '0 auto',
              borderRadius: 16,
              background: 'linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <BulbOutlined style={{ color: '#9ca3af', fontSize: 28 }} />
            </div>
          }
          description={
            <div style={{ marginTop: 16 }}>
              <div style={{ fontSize: 13, color: '#6b7280' }}>暂无思考步骤</div>
              <div style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>
                发送消息后可查看思考过程
              </div>
            </div>
          }
          style={{ marginTop: 60 }}
        />
      ) : (
        <Timeline
          mode="left"
          className="thought-timeline"
          items={steps.map((step, index) => ({
            dot: (
              <div style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: getStepBgColor(step.stepType),
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: `2px solid ${getStepColor(step.stepType)}`,
                boxShadow: `0 2px 8px ${getStepColor(step.stepType)}20`
              }}>
                {getStepIcon(step.stepType)}
              </div>
            ),
            color: getStepColor(step.stepType),
            children: (
              <div
                key={index}
                style={{
                  paddingBottom: 20,
                  marginLeft: 8
                }}
              >
                <div style={{ marginBottom: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{
                    padding: '4px 12px',
                    borderRadius: 6,
                    fontSize: 12,
                    fontWeight: 600,
                    color: getStepColor(step.stepType),
                    background: getStepBgColor(step.stepType),
                    border: `1px solid ${getStepColor(step.stepType)}30`,
                    letterSpacing: '0.3px'
                  }}>
                    {getStepLabel(step.stepType)}
                  </span>
                  <span style={{
                    fontSize: 11,
                    color: '#9ca3af',
                    fontWeight: 500,
                    letterSpacing: '0.3px'
                  }}>
                    STEP {index + 1}
                  </span>
                </div>
                <div style={{
                  fontSize: 13,
                  lineHeight: '1.7',
                  color: '#374151',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  padding: '12px 16px',
                  background: '#ffffff',
                  borderRadius: 10,
                  border: '1px solid #e5e7eb',
                  boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)'
                }}>
                  {step.content}
                </div>
              </div>
            )
          }))}
        />
      )}
    </div>
  )
}
