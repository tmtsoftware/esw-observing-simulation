import type { Event } from '@tmtsoftware/esw-ts'
import { booleanKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { RedFilterPosition } from './FilterWheelHelpers'
import {
  redCurrentPositionKey,
  redDemandPositionKey,
  redFilterPositionEvent
} from './FilterWheelHelpers'

type DarkSlide = 'In' | 'Out'
export const RFL = (): JSX.Element => {
  const eventService = useEventService()
  const [filter, setFilter] = React.useState<RedFilterPosition>()
  const [darkSlide, setDarkSlide] = React.useState<DarkSlide>()

  React.useEffect(() => {
    const onRedFilterPositionEvent = (event: Event) => {
      const current = event.get(redCurrentPositionKey)?.values[0]
      const target = event.get(redDemandPositionKey)?.values[0]
      setFilter({ current: current, target: target })

      const darkKey = booleanKey('dark')
      if (event.get(darkKey)?.values[0] === true) setDarkSlide('In')
      else if (event.get(darkKey)?.values[0] === false) setDarkSlide('Out')
    }

    const subscription = eventService?.subscribe(
      new Set([redFilterPositionEvent]),
      10
    )(onRedFilterPositionEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const rflLabelValueMap: LabelValueMap[] = [
    { label: 'filter', ...filter },
    { label: 'dark slide', current: darkSlide }
  ]

  return <Assembly name={'Red Filter Wheel'} keyValue={rflLabelValueMap} />
}
