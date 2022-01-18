import type { Subsystem } from '@tmtsoftware/esw-ts'
import { Card, Col, Row, Typography } from 'antd'
import React from 'react'

export const SubsytemCard = ({
  subsystem,
  children
}: {
  subsystem: Subsystem
  children: React.ReactNode
}): JSX.Element => {
  return (
    <Card
      style={{ marginBottom: '1.5rem' }}
      headStyle={{ background: 'rgb(240,240,240)' }}
      title={
        <Row gutter={16} style={{ display: 'flex', alignItems: 'center' }}>
          <Col span={6}>
            <Typography.Title level={4} style={{ marginBottom: '0' }}>
              {subsystem}
            </Typography.Title>
          </Col>
          {['CURRENT', 'TARGET', 'ERROR'].map((columnName, i) => (
            <Col key={i} span={6}>
              {columnName}
            </Col>
          ))}
        </Row>
      }>
      {children}
    </Card>
  )
}
