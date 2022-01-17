import type {
  Event,
  EventKey,
  EventService,
  Subscription
} from '@tmtsoftware/esw-ts'

type EventHandler = (event: Event) => void

export const getSubscriptions = (
  eventService: EventService | undefined,
  keys: [EventKey, EventHandler][]
): Subscription[] =>
  eventService
    ? keys.map(([eventKey, onEvent]) =>
        eventService.subscribe(new Set([eventKey]), 1)(onEvent)
      )
    : []
