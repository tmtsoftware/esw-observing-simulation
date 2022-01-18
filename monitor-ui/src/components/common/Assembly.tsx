import { Col, Divider, Row, Typography } from 'antd'
import * as React from 'react'

export type LabelValueMap =
  | {
      label: string
      current?: string | boolean | number
      target?: string | boolean | number
      error?: string | boolean | number
    }
  | undefined

export const Line = () => (
  <Divider
    style={{
      borderTopColor: 'lightgrey',
      margin: '1rem'
    }}
  />
)

export const Assembly = ({
  name,
  keyValue,
  showDivider = true
}: {
  name: string
  keyValue: LabelValueMap[]
  showDivider?: boolean
}): JSX.Element => {
  return (
    <div>
      <Row>
        <Col style={{ textAlign: 'right', paddingRight: '1rem' }} span={6}>
          <Typography.Title level={5}>{name}</Typography.Title>
        </Col>
      </Row>
      {keyValue.map((data, i) => {
        return (
          <Row key={i} gutter={16}>
            <Col style={{ textAlign: 'right' }} span={6}>
              <Typography.Text strong type='secondary'>
                {data?.label}:
              </Typography.Text>
            </Col>
            <Col span={6}>{data?.current?.toString()}</Col>
            <Col span={6}>{data?.target?.toString()}</Col>
            <Col span={6}>{data?.error?.toString()}</Col>
          </Row>
        )
      })}
      {showDivider && <Line />}
    </div>
  )
}
