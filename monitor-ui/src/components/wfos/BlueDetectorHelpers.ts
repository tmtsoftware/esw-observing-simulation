import { EventKey, Prefix } from '@tmtsoftware/esw-ts'
import {
  exposureStartEventKey,
  exposureEndEventKey,
  exposureAbortedEventKey,
  dataWriteStartEventKey,
  dataWriteEndEventKey,
  OpticalDetectorExposureDataEventKey
} from '../common/helpers'

const blueDetectorPrefix = new Prefix('WFOS', 'blue.detector')
const exposureStartEvent = new EventKey(
  blueDetectorPrefix,
  exposureStartEventKey
)
const exposureEndEvent = new EventKey(blueDetectorPrefix, exposureEndEventKey)
const exposureAbortedEvent = new EventKey(
  blueDetectorPrefix,
  exposureAbortedEventKey
)
export const dataWriteStartEvent = new EventKey(
  blueDetectorPrefix,
  dataWriteStartEventKey
)
export const dataWriteEndEvent = new EventKey(
  blueDetectorPrefix,
  dataWriteEndEventKey
)

export const blueDetectorExposureData = new EventKey(
  blueDetectorPrefix,
  OpticalDetectorExposureDataEventKey
)

export const blueDetectorObserveEvents = [
  exposureStartEvent,
  exposureEndEvent,
  exposureAbortedEvent,
  dataWriteStartEvent,
  dataWriteEndEvent,
  blueDetectorExposureData
]
