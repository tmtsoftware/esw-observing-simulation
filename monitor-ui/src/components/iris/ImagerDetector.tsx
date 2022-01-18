import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import {
  getObserveEventName,
  getObserveEventSubscriptionForPattern
} from '../common/helpers'

export const ImagerDetector = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [obsEvent, setObsEvent] = React.useState<string>()

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      setObsEvent(getObserveEventName(event))
    }

    const subscriptions = [
      getObserveEventSubscriptionForPattern(
        eventService,
        onObserveEvent,
        'IRIS',
        'imager.detector.ObserveEvent.*'
      )
    ]

    return () => subscriptions.forEach((s) => s?.cancel())
  }, [eventService])

  const imagerDetectorLabelValueMap: LabelValueMap[] = [
    { label: 'observe event', current: obsEvent }
  ]

  return (
    <Assembly
      name={'Imager Detector'}
      keyValue={imagerDetectorLabelValueMap}
      showDivider={false}
    />
  )
}
