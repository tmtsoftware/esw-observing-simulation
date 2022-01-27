import type { Event } from '@tmtsoftware/esw-ts'
import { EventName, intKey, longKey, stringKey } from '@tmtsoftware/esw-ts'

export const getObserveEventName = (event: Event): string =>
  event.eventName.name.split('.')[1]

export const filenameKey = stringKey('filename')
export const exposureIdKey = stringKey('exposureId')
export const rampsKey = intKey('rampsInExposure')
export const rampsCompleteKey = intKey('rampsComplete')
export const exposureTimeKey = longKey('exposureTime')
export const remainingExposureTimeKey = longKey('remainingExposureTime')
export const coaddsKey = intKey('coaddsInExposure')
export const coaddsDoneKey = intKey('coaddsDone')

export const exposureEndEventKey = new EventName('ObserveEvent.ExposureEnd')
export const exposureStartEventKey = new EventName('ObserveEvent.ExposureStart')
export const exposureAbortedEventKey = new EventName(
  'ObserveEvent.ExposureAborted'
)
export const dataWriteStartEventKey = new EventName(
  'ObserveEvent.DataWriteStart'
)
export const dataWriteEndEventKey = new EventName('ObserveEvent.DataWriteEnd')
export const irDetectorExposureDataEventKey = new EventName(
  'ObserveEvent.IRDetectorExposureData'
)

export const OpticalDetectorExposureDataEventKey = new EventName(
  'ObserveEvent.OpticalDetectorExposureData'
)
