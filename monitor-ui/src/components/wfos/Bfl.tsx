import type { Event } from '@tmtsoftware/esw-ts'
import { booleanKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { BlueFilterPosition } from './FilterWheelHelpers'
import {
  blueCurrentPositionKey,
  blueDemandPositionKey,
  blueFilterPositionEvent
} from './FilterWheelHelpers'

type DarkSlide = 'In' | 'Out'
export const BFL = (): JSX.Element => {
  const eventService = useEventService()
  const [filter, setFilter] = React.useState<BlueFilterPosition>()
  const [darkSlide, setDarkSlide] = React.useState<DarkSlide>()

  React.useEffect(() => {
    const onBlueFilterPositionEvent = (event: Event) => {
      const current = event.get(blueCurrentPositionKey)?.values[0]
      const target = event.get(blueDemandPositionKey)?.values[0]
      setFilter({ current: current, target: target })

      const darkKey = booleanKey('dark')
      if (event.get(darkKey)?.values[0] === true) setDarkSlide('In')
      else if (event.get(darkKey)?.values[0] === false) setDarkSlide('Out')
    }

    const subscription = eventService?.subscribe(
      new Set([blueFilterPositionEvent]),
      10
    )(onBlueFilterPositionEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const bflLabelValueMap: LabelValueMap[] = [
    { label: 'filter', ...filter },
    { label: 'dark slide', current: darkSlide }
  ]

  return <Assembly name={'Blue Filter Wheel'} keyValue={bflLabelValueMap} />
}
