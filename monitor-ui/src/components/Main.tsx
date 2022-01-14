import { Spin } from 'antd'
import * as React from 'react'
import { EventServiceContext } from '../contexts/EventServiceContext'
import { ADC } from './iris/Adc'

export const Main = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  console.log(eventService)
  return eventService ? <ADC eventService={eventService} /> : <Spin />
}
