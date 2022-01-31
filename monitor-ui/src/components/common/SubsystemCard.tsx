import type { Subsystem } from '@tmtsoftware/esw-ts'
import { Card, Col, Row, Typography } from 'antd'
import React from 'react'

export const SubsystemCard = ({
  subsystem,
  children
}: {
  subsystem: Subsystem
  children: React.ReactNode
}): JSX.Element => {
  return (
    <Card
      style={{ marginBottom: '1.5rem' }}
      headStyle={{ display: 'none' }}
      bodyStyle={{ padding: '12px' }}>
      <>
        <Row
          gutter={16}
          style={{
            display: 'flex',
            alignItems: 'center',
            backgroundColor: 'rgb(240,240,240)',
            marginBottom: '8px'
          }}>
          <Col span={6}>
            <Typography.Title level={5} style={{ marginBottom: '0' }}>
              {subsystem}
            </Typography.Title>
          </Col>
          {['Current', 'Target', 'Error'].map((columnName, i) => (
            <Col key={i} span={6}>
              {columnName}
            </Col>
          ))}
        </Row>
        {children}
      </>
    </Card>
  )
}
