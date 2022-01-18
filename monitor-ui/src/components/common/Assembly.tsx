import { Col, Row, Typography } from 'antd'
import * as React from 'react'

export type LabelValueMap =
  | {
      label: string
      current?: string | boolean | number
      target?: string | boolean | number
      error?: string | boolean | number
    }
  | undefined

export const Assembly = ({
  name,
  keyValue
}: {
  name: string
  keyValue: LabelValueMap[]
}): JSX.Element => {
  return (
    <div style={{ paddingBottom: '0.5rem' }}>
      <Typography.Title level={5}>{name}</Typography.Title>
      {keyValue.map((e, i) => {
        return (
          <Row key={i} gutter={16}>
            <Col style={{ textAlign: 'left' }} span={6}>
              <Typography.Text strong type='secondary'>
                {e?.label}:
              </Typography.Text>
            </Col>
            <Col span={6}>{e?.current?.toString()}</Col>
            <Col span={6}>{e?.target?.toString()}</Col>
            <Col span={6}>{e?.error?.toString()}</Col>
          </Row>
        )
      })}
    </div>
  )
}
