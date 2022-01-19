import { Col, Row } from 'antd'
import * as React from 'react'
import { Assembly } from './common/Assembly'
import { SubsytemCard } from './common/SubsystemCard'
import { IRIS } from './iris/Iris'

export const Main = (): JSX.Element => (
  <Row gutter={16}>
    <Col xs={24} sm={24} md={12} xl={8}>
      <IRIS />
    </Col>
    <Col xs={24} sm={24} md={12} xl={8}>
      <SubsytemCard subsystem={'WFOS'}>
        <Assembly name={'Red Filter Wheel'} keyValue={[]} />
        <Assembly name={'Blue Filter Wheel'} keyValue={[]} />
        <Assembly name={'Blue Detector'} keyValue={[]} />
        <Assembly name={'Red Detector'} keyValue={[]} showDivider={false} />
      </SubsytemCard>
      <SubsytemCard subsystem={'TCS'}>
        <Assembly name={'Mount Position'} keyValue={[]} />
        <Assembly name={'Enclosure'} keyValue={[]} showDivider={false} />
      </SubsytemCard>
    </Col>
  </Row>
)
