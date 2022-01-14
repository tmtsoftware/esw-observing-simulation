import { Col, Divider, Row } from 'antd'
import * as React from 'react'

export type ValueType =
  | {
      current?: string | boolean | number | undefined
      target?: string | boolean | number | undefined
      error?: string | boolean | number | undefined
      label: string
    }
  | undefined

export const Assembly = ({
  name,
  keyValue
}: {
  name: string
  keyValue: ValueType[]
}): JSX.Element => {
  return (
    <>
      <Divider orientation='center' style={{ paddingTop: '0px' }}>
        {name}
      </Divider>
      {keyValue.map((e, i) => {
        return (
          <Row key={i} style={{ border: '0.5px solid lightgrey' }} gutter={16}>
            <Col span={6}>{e?.label}:</Col>
            <Col span={6}>{e?.current?.toString()}</Col>
            <Col span={6}>{e?.target?.toString()}</Col>
            <Col span={6}>{e?.error?.toString()}</Col>
          </Row>
        )
      })}
    </>
  )
}
