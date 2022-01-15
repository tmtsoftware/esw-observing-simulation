import type { Subsystem } from '@tmtsoftware/esw-ts'
import { Card, Col, Row } from 'antd'
import React from 'react'

export const SubsytemHeader = ({
  subsystem,
  children
}: {
  subsystem: Subsystem
  children: React.ReactNode
}) => {
  return (
    <Card
      style={{ marginBottom: '1.5rem' }}
      bodyStyle={{ paddingTop: '0', paddingBottom: '0' }}
      title={
        <Row gutter={16}>
          <Col span={6}>{subsystem}</Col>
          {['CURRENT', 'TARGET', 'ERROR'].map((c, i) => (
            <Col
              key={i}
              style={{ fontWeight: 'normal', fontSize: '14px' }}
              span={6}>
              {c}
            </Col>
          ))}
        </Row>
      }
      bordered={true}>
      {children}
    </Card>
  )
}
