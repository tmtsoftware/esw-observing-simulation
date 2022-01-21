import { EventService, Prefix, TcpConnection } from '@tmtsoftware/esw-ts'
import React, { createContext, useContext, useEffect, useState } from 'react'
import { useLocationService } from './LocationServiceContext'

export interface EventServiceProps {
  children: React.ReactNode
}

const EventServiceContext = createContext<EventService | undefined>(undefined)

const EVENT_SERVER = TcpConnection(new Prefix('CSW', 'EventServer'), 'Service')

const EventServiceProvider = (props: EventServiceProps) => {
  const { children } = props
  const locationService = useLocationService()
  const [eventService, setEventService] = useState<EventService>()

  const resetEventService = async () => {
    setEventService(await EventService())
  }

  useEffect(() => {
    const sub = locationService.track(EVENT_SERVER)((e) => {
      switch (e._type) {
        case 'LocationRemoved':
          setEventService(undefined)
          break
        case 'LocationUpdated':
          resetEventService()
          break
      }
    })
    return () => sub.cancel()
  }, [locationService])

  return (
    <EventServiceContext.Provider value={eventService}>
      {children}
    </EventServiceContext.Provider>
  )
}

export const useEventService = () => {
  return useContext(EventServiceContext)
}

export default EventServiceProvider
