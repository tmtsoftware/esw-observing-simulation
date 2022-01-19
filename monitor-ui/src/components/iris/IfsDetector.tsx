import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getObserveEventName } from '../common/helpers'
import { ifsObserveEvents } from './IfsDetectorHelpers'

export const IfsDetector = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [obsEvent, setObsEvent] = React.useState<string>()

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      setObsEvent(getObserveEventName(event))
    }

    const subscription = eventService?.subscribe(
      new Set(ifsObserveEvents),
      10
    )(onObserveEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const ifsDetectorLabelValueMap: LabelValueMap[] = [
    { label: 'observe event', current: obsEvent }
  ]

  return <Assembly name={'IFS Detector'} keyValue={ifsDetectorLabelValueMap} />
}
