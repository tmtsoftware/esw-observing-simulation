import { Col, Row } from 'antd'
import * as React from 'react'
import { Assembly } from './common/Assembly'
import { SubsytemHeader } from './common/Subsystem'
import { IRIS } from './iris/Iris'

export const Main = (): JSX.Element => {
  return (
    <Row gutter={16}>
      <Col span={6}>
        <IRIS />
      </Col>
      <Col span={6}>
        <SubsytemHeader subsystem={'WFOS'}>
          <Assembly name={'Red Filter Wheel'} keyValue={[]} />
          <Assembly name={'Blue Filter Wheel'} keyValue={[]} />
          <Assembly name={'Blue Detector'} keyValue={[]} />
          <Assembly name={'Red Detector'} keyValue={[]} />
        </SubsytemHeader>
        <SubsytemHeader subsystem={'TCS'}>
          <Assembly name={'Mount Position'} keyValue={[]} />
          <Assembly name={'Enclosure'} keyValue={[]} />
        </SubsytemHeader>
      </Col>
    </Row>
  )
}
