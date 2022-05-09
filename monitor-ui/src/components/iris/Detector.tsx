import type { Event, EventKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import {
  exposureIdKey,
  exposureTimeKey,
  filenameKey,
  getObserveEventName,
  rampsCompleteKey,
  rampsKey,
  remainingExposureTimeKey
} from '../common/helpers'
import {
  dataWriteEndEvent,
  dataWriteStartEvent,
  ifsObserveEvents,
  irDetectorExposureData
} from './IfsDetectorHelpers'
import { imagerObserveEvents } from './ImagerDetectorHelpers'

const Detector = ({
  name,
  eventKeys,
  showDivider
}: {
  name: string
  eventKeys: EventKey[]
  showDivider: boolean
}): JSX.Element => {
  const eventService = useEventService()
  const [obsEvent, setObsEvent] = React.useState<string>()
  const [exposureId, setExposureId] = React.useState<string>()
  const [filename, setFilename] = React.useState<string>()
  const [ramps, setRamps] = React.useState<number>()
  const [rampsComplete, setRampsComplete] = React.useState<number>()
  const [exposureTime, setExposureTime] = React.useState<number>()
  const [remainingExposureTime, setRemainingExposureTime] =
    React.useState<number>()

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      setObsEvent(getObserveEventName(event))
      setExposureId(event.get(exposureIdKey)?.values[0])
      // Note: See tsconfig.json:     "noFallthroughCasesInSwitch": true
      switch (event.eventName.name) {
        case dataWriteStartEvent.eventName.name:
          setFilename(event.get(filenameKey)?.values[0])
          break
        case dataWriteEndEvent.eventName.name:
          setFilename(event.get(filenameKey)?.values[0])
          break
        case irDetectorExposureData.eventName.name:
          setRamps(event.get(rampsKey)?.values[0])
          setRampsComplete(event.get(rampsCompleteKey)?.values[0])
          setExposureTime(event.get(exposureTimeKey)?.values[0])
          setRemainingExposureTime(
            event.get(remainingExposureTimeKey)?.values[0]
          )
          break
      }
    }

    const subscription = eventService?.subscribe(
      new Set(eventKeys)
    )(onObserveEvent)

    return () => subscription?.cancel()
  }, [eventKeys, eventService])

  const ifsDetectorLabelValueMap: LabelValueMap[] = [
    { label: 'ramps', current: ramps },
    { label: 'ramps complete', current: rampsComplete },
    { label: 'exposure time', current: exposureTime },
    { label: 'remaining time', current: remainingExposureTime },
    { label: 'observe event', current: obsEvent },
    { label: 'exposure id', current: exposureId },
    { label: 'filename', current: filename }
  ]

  return (
    <Assembly
      name={name}
      keyValue={ifsDetectorLabelValueMap}
      singleColumn
      showDivider={showDivider}
    />
  )
}

export const IfsDetector = () => (
  <Detector name={'Ifs Detector'} eventKeys={ifsObserveEvents} showDivider />
)

export const ImagerDetector = () => (
  <Detector
    name={'Imager Detector'}
    eventKeys={imagerObserveEvents}
    showDivider={false}
  />
)
