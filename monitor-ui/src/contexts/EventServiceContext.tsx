import { EventService, setAppName } from '@tmtsoftware/esw-ts'
import React, { createContext, useEffect, useState } from 'react'
import { AppConfig } from '../config/AppConfig'

export interface EventServiceProps {
  children: React.ReactNode
}

setAppName(AppConfig.applicationName)

// eslint-disable-next-line import/no-mutable-exports
export let defaultEventServiceState: EventService
// eslint-disable-next-line import/no-mutable-exports
export let EventServiceContext = createContext<EventService | undefined>(
  undefined
)

const setDefault = async () => {
  defaultEventServiceState = await EventService()
  EventServiceContext = createContext<EventService | undefined>(
    defaultEventServiceState
  )
}

setDefault()

const EventServiceProvider = (props: EventServiceProps) => {
  const { children } = props
  const [eventService, setEventService] = useState<EventService>(
    defaultEventServiceState
  )

  const resetEventService = async () => {
    //Authenticating config service
    const service = await EventService()
    setEventService(service)
  }

  useEffect(() => {
    resetEventService().catch(() => ({}))
    // window.alert('event server is not available')
  }, [])

  return (
    <EventServiceContext.Provider value={eventService}>
      {children}
    </EventServiceContext.Provider>
  )
}

export default EventServiceProvider
