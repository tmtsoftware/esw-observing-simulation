import { EventKey, Prefix } from '@tmtsoftware/esw-ts'
import {
  exposureStartEventKey,
  exposureEndEventKey,
  exposureAbortedEventKey,
  dataWriteStartEventKey,
  dataWriteEndEventKey,
  irDetectorExposureDataEventKey
} from '../common/helpers'

const ifsDetectorPrefix = new Prefix('IRIS', 'ifs.detector')
const exposureStartEvent = new EventKey(
  ifsDetectorPrefix,
  exposureStartEventKey
)
const exposureEndEvent = new EventKey(ifsDetectorPrefix, exposureEndEventKey)
const exposureAbortedEvent = new EventKey(
  ifsDetectorPrefix,
  exposureAbortedEventKey
)
export const dataWriteStartEvent = new EventKey(
  ifsDetectorPrefix,
  dataWriteStartEventKey
)
export const dataWriteEndEvent = new EventKey(
  ifsDetectorPrefix,
  dataWriteEndEventKey
)

export const irDetectorExposureData = new EventKey(
  ifsDetectorPrefix,
  irDetectorExposureDataEventKey
)

export const ifsObserveEvents = [
  exposureStartEvent,
  exposureEndEvent,
  exposureAbortedEvent,
  dataWriteStartEvent,
  dataWriteEndEvent,
  irDetectorExposureData
]
