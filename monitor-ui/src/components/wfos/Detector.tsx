import type { Event, EventKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import {
  exposureIdKey,
  filenameKey,
  exposureTimeKey,
  remainingExposureTimeKey,
  coaddsDoneKey,
  coaddsKey,
  getObserveEventName
} from '../common/helpers'
import {
  blueDetectorObserveEvents,
  dataWriteEndEvent,
  dataWriteStartEvent,
  blueDetectorExposureData
} from './BlueDetectorHelpers'
import { redDetectorObserveEvents } from './RedDetectorHelpers'

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
  const [exposureTime, setExposureTime] = React.useState<number>()
  const [remainingExposureTime, setRemExposureTime] = React.useState<number>()
  const [coadds, setCoadds] = React.useState<number>()
  const [coaddsDone, setCoaddsDone] = React.useState<number>()

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
        case blueDetectorExposureData.eventName.name:
          setCoadds(event.get(coaddsKey)?.values[0])
          setCoaddsDone(event.get(coaddsDoneKey)?.values[0])
          setExposureTime(event.get(exposureTimeKey)?.values[0])
          setRemExposureTime(event.get(remainingExposureTimeKey)?.values[0])
          break
      }
    }

    const subscription = eventService?.subscribe(
      new Set(eventKeys),
      10
    )(onObserveEvent)

    return () => subscription?.cancel()
  }, [eventKeys, eventService])

  const blueDetectorLabelValueMap: LabelValueMap[] = [
    { label: 'coadds', current: coadds },
    { label: 'coadds done', current: coaddsDone },
    { label: 'exposure time', current: exposureTime },
    { label: 'remaining time', current: remainingExposureTime },
    { label: 'observe event', current: obsEvent },
    { label: 'exposure id', current: exposureId },
    { label: 'filename', current: filename }
  ]

  return (
    <Assembly
      name={name}
      keyValue={blueDetectorLabelValueMap}
      singleColumn
      showDivider={showDivider}
    />
  )
}

export const BlueDetector = () => (
  <Detector
    name={'Blue Detector'}
    eventKeys={blueDetectorObserveEvents}
    showDivider
  />
)

export const RedDetector = () => (
  <Detector
    name={'Red Detector'}
    eventKeys={redDetectorObserveEvents}
    showDivider={false}
  />
)
