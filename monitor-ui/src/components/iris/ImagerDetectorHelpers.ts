import { EventKey, Prefix } from '@tmtsoftware/esw-ts'
import {
  dataWriteEndEventKey,
  dataWriteStartEventKey,
  exposureAbortedEventKey,
  exposureEndEventKey,
  exposureStartEventKey,
  irDetectorExposureDataEventKey
} from '../common/helpers'

const imagerDetectorPrefix = new Prefix('IRIS', 'imager.detector')
const exposureStartEvent = new EventKey(
  imagerDetectorPrefix,
  exposureStartEventKey
)
const exposureEndEvent = new EventKey(imagerDetectorPrefix, exposureEndEventKey)
const exposureAbortedEvent = new EventKey(
  imagerDetectorPrefix,
  exposureAbortedEventKey
)
const dataWriteStartEvent = new EventKey(
  imagerDetectorPrefix,
  dataWriteStartEventKey
)
const dataWriteEndEvent = new EventKey(
  imagerDetectorPrefix,
  dataWriteEndEventKey
)

export const imagerDetectorExposureData = new EventKey(
  imagerDetectorPrefix,
  irDetectorExposureDataEventKey
)

export const imagerObserveEvents = [
  exposureStartEvent,
  exposureEndEvent,
  exposureAbortedEvent,
  dataWriteStartEvent,
  dataWriteEndEvent,
  imagerDetectorExposureData
]
