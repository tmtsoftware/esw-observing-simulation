import type { Event } from '@tmtsoftware/esw-ts'
import { booleanKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { Filter } from './filterWheelHelpers'
import {
  filterCurrentPositionKey,
  filterDemandPositionKey,
  wheelPositionEvent
} from './filterWheelHelpers'

type DarkSlide = 'In' | 'Out'
export const FilterWheel = (): JSX.Element => {
  const eventService = useEventService()
  const [filter, setFilter] = React.useState<Filter>()
  const [darkSlide, setDarkSlide] = React.useState<DarkSlide>()

  React.useEffect(() => {
    const onWheelPositionEvent = (event: Event) => {
      const current = event.get(filterCurrentPositionKey)?.values[0]
      const target = event.get(filterDemandPositionKey)?.values[0]
      setFilter({ current: current, target: target })

      const darkKey = booleanKey('dark')
      if (event.get(darkKey)?.values[0] === true) setDarkSlide('In')
      else if (event.get(darkKey)?.values[0] === false) setDarkSlide('Out')
    }

    const subscription = eventService?.subscribe(
      new Set([wheelPositionEvent]),
      10
    )(onWheelPositionEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const filterLabelValueMap: LabelValueMap[] = [
    { label: 'filter', ...filter },
    { label: 'dark slide', current: darkSlide }
  ]

  return <Assembly name={'Filter Wheel'} keyValue={filterLabelValueMap} />
}
