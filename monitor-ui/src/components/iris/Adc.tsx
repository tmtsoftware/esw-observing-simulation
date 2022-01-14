import {
  booleanKey,
  choiceKey,
  doubleKey,
  EventKey,
  EventName,
  Prefix
} from '@tmtsoftware/esw-ts'
import type { Event, Subscription, EventService } from '@tmtsoftware/esw-ts'
import { Card, Col, Divider, Row } from 'antd'
import * as React from 'react'
import { Assembly } from '../common/Assembly'
import type { ValueType } from '../common/Assembly'

type PrismState = 'FOLLOWING' | 'STOPPED'
const prismState: PrismState[] = ['FOLLOWING', 'STOPPED']
type Retract = 'IN' | 'OUT'
const retract: Retract[] = ['IN', 'OUT']
type Prism = {
  current: number | undefined
  target: number | undefined
  error: number | undefined
}

const followingKey = choiceKey<PrismState>('following', prismState)
const retractPositionKey = choiceKey<Retract>('position', retract)
const currentAngleKey = doubleKey('currentAngle')
const angleErrorKey = doubleKey('errorAngle')
const targetAngleKey = doubleKey('targetAngle')

const adcPrefix = new Prefix('IRIS', 'imager.adc')
const prismStateEvent = new EventKey(adcPrefix, new EventName('prism_state'))
const prismRetractEvent = new EventKey(
  adcPrefix,
  new EventName('prism_position')
)
const prismEvent = new EventKey(adcPrefix, new EventName('prism_current'))

export const ADC = ({
  eventService
}: {
  eventService: EventService
}): JSX.Element => {
  const [state, setState] = React.useState<PrismState>()
  const [onTarget, setOnTarget] = React.useState<boolean>()
  const [retractState, setRetractState] = React.useState<Retract>()
  const [prism, setPrism] = React.useState<Prism | undefined>(undefined)

  React.useEffect(() => {
    const subscriptions: Subscription[] = []

    const onPrismStateEvent = (event: Event) => {
      const values = event.get(followingKey)?.values as unknown as PrismState[]
      setState(values[0])

      const onTargetKey = booleanKey('onTarget')
      setOnTarget(event.get(onTargetKey)?.values[0])
    }

    const onPrismRetractEvent = (event: Event) => {
      const values = event.get(retractPositionKey)
        ?.values as unknown as Retract[]
      setRetractState(values[0])
    }

    const onPrismEvent = (event: Event) => {
      const current = event.get(currentAngleKey)?.values[0]
      const target = event.get(targetAngleKey)?.values[0]
      const error = event.get(angleErrorKey)?.values[0]
      setPrism({ current, target, error })
    }

    subscriptions.push(
      eventService.subscribe(new Set([prismStateEvent]), 1)(onPrismStateEvent)
    )
    subscriptions.push(
      eventService.subscribe(
        new Set([prismRetractEvent]),
        1
      )(onPrismRetractEvent)
    )
    subscriptions.push(
      eventService.subscribe(new Set([prismEvent]), 1)(onPrismEvent)
    )
    return () => subscriptions.forEach((s) => s.cancel())
  }, [eventService])

  const values: ValueType[] = [
    { label: 'state', current: state },
    { label: 'onTarget', current: onTarget },
    { label: 'stage', current: retractState },
    { label: 'prism', ...prism }
  ]

  return (
    <Card
      bodyStyle={{ paddingTop: '0', paddingBottom: '0' }}
      title={
        <Row gutter={16}>
          <Col span={6}>IRIS</Col>
          <Col style={{ fontWeight: 'normal', fontSize: '14px' }} span={6}>
            CURRENT
          </Col>
          <Col style={{ fontWeight: 'normal', fontSize: '14px' }} span={6}>
            TARGET
          </Col>
          <Col style={{ fontWeight: 'normal', fontSize: '14px' }} span={6}>
            ERROR
          </Col>
        </Row>
      }
      bordered={true}
      style={{ width: '30%' }}>
      <Assembly name={'ADC'} keyValue={values} />
      <Divider orientation='center'>Imager</Divider>
      <p>Card content</p>
    </Card>
  )
}
