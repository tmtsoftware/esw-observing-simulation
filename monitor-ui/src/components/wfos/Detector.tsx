import type { Event, EventKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { exposureTimeKey, getObserveEventName } from '../common/helpers'
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
  const [exposureTime, setExposureTime] = React.useState<number>()

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      setObsEvent(getObserveEventName(event))
      switch (event.eventName.name) {
        case dataWriteStartEvent.eventName.name:
          break
        case dataWriteEndEvent.eventName.name:
          break
        case blueDetectorExposureData.eventName.name:
          setExposureTime(event.get(exposureTimeKey)?.values[0])
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
    { label: 'exposure time', current: exposureTime },
    { label: 'observe event', current: obsEvent }
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
