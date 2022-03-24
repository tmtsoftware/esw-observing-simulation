import type { Event, EventName } from '@tmtsoftware/esw-ts'
import { Card, Col, Collapse, List, Row, Space, Typography } from 'antd'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import { formatParameters } from './ParamFormatter'
import './styles.module.css'

export const extractEventName = (eventName: EventName) => {
  return eventName.name.split('ObserveEvent.')[1]
}

const createDataSource = (event: Event): JSX.Element[] => {
  return event.paramSet.map((parameter) => (
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
}

const { Panel } = Collapse
export const ObserveEvents = () => {
  const eventService = useEventService()
  const [observeEvents, setObserveEvents] = React.useState<Event[]>([])

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      //prepend new event & append recent 19 event to the list
      setObserveEvents((preList) => [event, ...preList.slice(0, 19)])
    }
    const subscription = eventService?.subscribeObserveEvents(1)(onObserveEvent)
    return () => subscription?.cancel()
  }, [eventService])

  return (
    <Card
      style={{ marginBottom: '1.5rem' }}
      headStyle={{ display: 'none' }}
      bodyStyle={{ padding: '12px' }}>
      <Row
        gutter={16}
        style={{
          display: 'flex',
          alignItems: 'center',
          backgroundColor: 'rgb(240,240,240)',
          marginBottom: '8px'
        }}>
        <Col span={24}>
          <Typography.Title level={5} style={{ marginBottom: '0' }}>
            {'Observe events'}
          </Typography.Title>
        </Col>
      </Row>
      <Collapse accordion style={{ backgroundColor: 'white' }}>
        {observeEvents.map((event) => {
          return (
            <Panel
              header={
                <>
                  <Typography.Text>
                    {event.source.toJSON()}:{' '}
                    <Typography.Text strong>
                      {extractEventName(event.eventName)}
                    </Typography.Text>
                  </Typography.Text>
                </>
              }
              key={event.eventId}>
              {createDataSource(event)}
            </Panel>
          )
        })}
      </Collapse>
    </Card>
  )
}
