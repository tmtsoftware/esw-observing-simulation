import { Col, Row } from 'antd'
import * as React from 'react'
import { Assembly } from './common/Assembly'
import { SubsytemCard } from './common/SubsystemCard'
import { IRIS } from './iris/Iris'

export const Main = (): JSX.Element => {
  return (
    <Row gutter={16}>
      <Col span={9}>
        <IRIS />
      </Col>
      <Col span={9}>
        <SubsytemCard subsystem={'WFOS'}>
          <Assembly name={'Red Filter Wheel'} keyValue={[]} />
          <Assembly name={'Blue Filter Wheel'} keyValue={[]} />
          <Assembly name={'Blue Detector'} keyValue={[]} />
          <Assembly name={'Red Detector'} keyValue={[]} />
        </SubsytemCard>
        <SubsytemCard subsystem={'TCS'}>
          <Assembly name={'Mount Position'} keyValue={[]} />
          <Assembly name={'Enclosure'} keyValue={[]} />
        </SubsytemCard>
      </Col>
    </Row>
  )
}
