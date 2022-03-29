import { Col, Row } from 'antd'
import * as React from 'react'
import { IRIS } from './iris/Iris'
import { ObserveEvents } from './observeEvents/ObserveEvents'
import { TCS } from './tcs/TCS'
import { WFOS } from './wfos/Wfos'

export const Main = (): JSX.Element => (
  <Row gutter={16}>
    <Col xs={24} md={24} lg={12} xl={8}>
      <IRIS />
      <TCS />
    </Col>
    <Col xs={24} md={24} lg={12} xl={8}>
      <WFOS />
      <ObserveEvents />
    </Col>
  </Row>
)
