import { EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

const ifsDetectorPrefix = new Prefix('IRIS', 'ifs.detector')
const exposureStartEvent = new EventKey(
  ifsDetectorPrefix,
  new EventName('ObserveEvent.ExposureStart')
)
const exposureEndEvent = new EventKey(
  ifsDetectorPrefix,
  new EventName('ObserveEvent.ExposureEnd')
)
const exposureAbortedEvent = new EventKey(
  ifsDetectorPrefix,
  new EventName('ObserveEvent.ExposureAborted')
)
const dataWriteStartEvent = new EventKey(
  ifsDetectorPrefix,
  new EventName('ObserveEvent.DataWriteStart')
)
const dataWriteEndEvent = new EventKey(
  ifsDetectorPrefix,
  new EventName('ObserveEvent.DataWriteEnd')
)

export const ifsObserveEvents = [
  exposureStartEvent,
  exposureEndEvent,
  exposureAbortedEvent,
  dataWriteStartEvent,
  dataWriteEndEvent
]
