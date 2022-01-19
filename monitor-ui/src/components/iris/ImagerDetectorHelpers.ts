import { EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

const imagerDetectorPrefix = new Prefix('IRIS', 'imager.detector')
const exposureStartEvent = new EventKey(
  imagerDetectorPrefix,
  new EventName('ObserveEvent.ExposureStart')
)
const exposureEndEvent = new EventKey(
  imagerDetectorPrefix,
  new EventName('ObserveEvent.ExposureEnd')
)
const exposureAbortedEvent = new EventKey(
  imagerDetectorPrefix,
  new EventName('ObserveEvent.ExposureAborted')
)
const dataWriteStartEvent = new EventKey(
  imagerDetectorPrefix,
  new EventName('ObserveEvent.DataWriteStart')
)
const dataWriteEndEvent = new EventKey(
  imagerDetectorPrefix,
  new EventName('ObserveEvent.DataWriteEnd')
)

export const imagerObserveEvents = [
  exposureStartEvent,
  exposureEndEvent,
  exposureAbortedEvent,
  dataWriteStartEvent,
  dataWriteEndEvent
]
