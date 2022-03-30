import { DeleteOutlined } from '@ant-design/icons'
import type { Event, EventName } from '@tmtsoftware/esw-ts'
import { ObserveEventNames } from '@tmtsoftware/esw-ts'
import { Button, Card, Col, Collapse, Row, Space, Typography } from 'antd'
import * as React from 'react'
import { useRef } from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import { formatParameters } from './ParamFormatter'
import './styles.module.css'

export const extractEventName = (eventName: EventName) => {
  return eventName.name.split('ObserveEvent.')[1]
}

const randomInThousands = Math.floor(1000 + Math.random() * 9000)

const dateFormatter = new Intl.DateTimeFormat('en', {
  hour: 'numeric',
  minute: 'numeric',
  second: 'numeric',
  fractionalSecondDigits: 3
})

const getTime = (dateStr: string) => dateFormatter.format(new Date(dateStr))

const paramSet = (event: Event): JSX.Element[] =>
  event.paramSet.map((parameter) => (
    <Row key={parameter.keyName} gutter={8}>
      <Col style={{ textAlign: 'right' }} span={8}>
        <Typography.Text type='secondary' strong>
          {parameter.keyName + ': '}
        </Typography.Text>
      </Col>
      <Col>
        <Space direction='horizontal'>
          {formatParameters(parameter, event)}
          {parameter.units.name !== 'none' && parameter.units.name}
        </Space>
      </Col>
    </Row>
  ))

const { Panel } = Collapse

const ignoreList = [
  ObserveEventNames.IRDetectorExposureData.name,
  ObserveEventNames.OpticalDetectorExposureData.name
]

export const ObserveEvents = () => {
  const eventService = useEventService()
  const [observeEvents, setObserveEvents] = React.useState<Event[]>([])
  const elem = useRef<HTMLDivElement | null>(null)
  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      if (!ignoreList.includes(event.eventName.name)) {
        //prepend new event & append recent 19 event to the list
        setObserveEvents((preList) => [...preList.slice(0, 1000), event])
        elem.current?.scrollIntoView({
          behavior: 'smooth'
        })
      }
    }
    const subscription = eventService?.subscribeObserveEvents()(onObserveEvent)
    return () => subscription?.cancel()
  }, [eventService])

  return (
    <Card
      style={{ marginBottom: '0.25rem' }}
      headStyle={{ display: 'none' }}
      bodyStyle={{ padding: '6px' }}>
      <Row
        gutter={16}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          backgroundColor: 'rgb(240,240,240)',
          marginBottom: '8px'
        }}>
        <Col span={18}>
          <Typography.Title level={5} style={{ marginBottom: '0' }}>
            {'Observe Events'}
          </Typography.Title>
        </Col>
        <Col>
          <Button
            type='text'
            icon={<DeleteOutlined />}
            onClick={() => setObserveEvents([])}
          />
        </Col>
      </Row>
      <Collapse
        accordion
        style={{
          backgroundColor: 'white',
          maxHeight: '24rem',
          overflow: 'scroll'
        }}
        ghost>
        {observeEvents.map((event, index) => (
          <Panel
            header={
              <div key={index} ref={(el) => (elem.current = el)}>
                <Typography.Text>
                  {getTime(event.eventTime)}: {event.source.toJSON()}:{' '}
                  <Typography.Text strong>
                    {extractEventName(event.eventName)}
                  </Typography.Text>
                </Typography.Text>
              </div>
            }
            key={event.eventId}>
            {paramSet(event)}
          </Panel>
        ))}
        <Panel header={''} disabled key={randomInThousands} />
      </Collapse>
    </Card>
  )
}
