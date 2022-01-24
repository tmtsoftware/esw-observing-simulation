import { Col, Row } from 'antd'
import * as React from 'react'
import { Assembly } from './common/Assembly'
import { SubsytemCard } from './common/SubsystemCard'
import { IRIS } from './iris/Iris'
import { WFOS } from './wfos/Wfos'

export const Main = (): JSX.Element => (
  <Row gutter={16}>
    <Col xs={24} md={24} lg={12} xl={8}>
      <IRIS />
    </Col>
    <Col xs={24} md={24} lg={12} xl={8}>
      <WFOS />
      <SubsytemCard subsystem={'TCS'}>
        <Assembly name={'Mount Position'} keyValue={[]} />
        <Assembly name={'Enclosure'} keyValue={[]} showDivider={false} />
      </SubsytemCard>
    </Col>
  </Row>
)
