import type { Event, EventKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import {
  exposureIdKey,
  filenameKey,
  getObserveEventName
} from '../common/helpers'
import {
  dataWriteEndEvent,
  dataWriteStartEvent,
  ifsObserveEvents
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
  const eventService = React.useContext(EventServiceContext)
  const [obsEvent, setObsEvent] = React.useState<string>()
  const [exposureId, setExposureId] = React.useState<string>()
  const [filename, setFilename] = React.useState<string>()

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      console.log(JSON.stringify(event))
      setObsEvent(getObserveEventName(event))
      setExposureId(event.get(exposureIdKey)?.values[0])
      switch (event.eventName.name) {
        case dataWriteStartEvent.eventName.name:
        case dataWriteEndEvent.eventName.name:
          setFilename(event.get(filenameKey)?.values[0])
          break
      }
    }

    const subscription = eventService?.subscribe(
      new Set(eventKeys),
      10
    )(onObserveEvent)

    return () => subscription?.cancel()
  }, [eventKeys, eventService])

  const ifsDetectorLabelValueMap: LabelValueMap[] = [
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
