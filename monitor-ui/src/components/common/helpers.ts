import type {
  Event,
  EventKey,
  EventService,
  Subscription,
  Subsystem
} from '@tmtsoftware/esw-ts'

type EventHandler = (event: Event) => void

export const getSubscriptions = (
  eventService: EventService | undefined,
  keys: [EventKey, EventHandler][]
): Subscription[] =>
  eventService
    ? keys.map(([eventKey, onEvent]) =>
        eventService.subscribe(new Set([eventKey]), 10)(onEvent)
      )
    : []

export const getObserveEventName = (event: Event): string =>
  event.eventName.name.split('.')[1]

export const getObserveEventSubscriptionForPattern = (
  eventService: EventService | undefined,
  eventHandler: EventHandler,
  subsystem: Subsystem,
  pattern: string
): Subscription | undefined =>
  eventService?.pSubscribe(subsystem, 10, pattern)(eventHandler)
