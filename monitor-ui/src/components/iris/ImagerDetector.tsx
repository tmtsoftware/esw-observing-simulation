import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getObserveEventName } from '../common/helpers'
import { imagerObserveEvents } from './ImagerDetectorHelpers'

export const ImagerDetector = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [obsEvent, setObsEvent] = React.useState<string>()

  React.useEffect(() => {
    const onObserveEvent = (event: Event) => {
      setObsEvent(getObserveEventName(event))
    }

    const subscription = eventService?.subscribe(
      new Set(imagerObserveEvents),
      10
    )(onObserveEvent)

    return () => subscription?.cancel()
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
