import { Col, Row } from 'antd'
import * as React from 'react'
import { SubsystemCard } from './common/SubsystemCard'
import { IRIS } from './iris/Iris'
import { ENCAssembly } from './tcs/ENCAssembly'
import { MCSAssembly } from './tcs/MCSAssembly'
import { WFOS } from './wfos/Wfos'

export const Main = (): JSX.Element => (
  <Row gutter={16}>
    <Col xs={24} md={24} lg={12} xl={8}>
      <IRIS />
    </Col>
    <Col xs={24} md={24} lg={12} xl={8}>
      <WFOS />
      <SubsystemCard subsystem={'TCS'}>
        <MCSAssembly />
        <ENCAssembly />
      </SubsystemCard>
    </Col>
  </Row>
)
