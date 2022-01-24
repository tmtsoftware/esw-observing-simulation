import { EventKey, Prefix } from '@tmtsoftware/esw-ts'
import {
  dataWriteEndEventKey,
  dataWriteStartEventKey,
  exposureAbortedEventKey,
  exposureEndEventKey,
  exposureStartEventKey,
  irDetectorExposureDataEventKey
} from '../common/helpers'

const redDetectorPrefix = new Prefix('WFOS', 'red.detector')
const exposureStartEvent = new EventKey(
  redDetectorPrefix,
  exposureStartEventKey
)
const exposureEndEvent = new EventKey(redDetectorPrefix, exposureEndEventKey)
const exposureAbortedEvent = new EventKey(
  redDetectorPrefix,
  exposureAbortedEventKey
)
const dataWriteStartEvent = new EventKey(
  redDetectorPrefix,
  dataWriteStartEventKey
)
const dataWriteEndEvent = new EventKey(redDetectorPrefix, dataWriteEndEventKey)

export const redDetectorExposureData = new EventKey(
  redDetectorPrefix,
  irDetectorExposureDataEventKey
)

export const redDetectorObserveEvents = [
  exposureStartEvent,
  exposureEndEvent,
  exposureAbortedEvent,
  dataWriteStartEvent,
  dataWriteEndEvent,
  redDetectorExposureData
]
