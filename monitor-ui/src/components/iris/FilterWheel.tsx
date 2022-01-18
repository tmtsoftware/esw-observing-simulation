import type { Event } from '@tmtsoftware/esw-ts'
import { booleanKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getSubscriptions } from '../common/helpers'

import type { Filter, FilterPosition } from './filterWheelHelpers'

import {
  filterCurrentPositionKey,
  filterDemandPositionKey,
  wheelPositionEvent
} from './filterWheelHelpers'

export const FilterWheel = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [filter, setFilter] = React.useState<Filter>()
  const [darkSlide, setDarkSlide] = React.useState<string>()

  React.useEffect(() => {
    const onWheelPositionEvent = (event: Event) => {
      const current = event.get(filterCurrentPositionKey)
        ?.values as unknown as FilterPosition[]
      const target = event.get(filterDemandPositionKey)
        ?.values as unknown as FilterPosition[]
      setFilter({ current: current[0], target: target[0] })

      const darkKey = booleanKey('dark')
      if (event.get(darkKey)?.values[0] === true) setDarkSlide('In')
      else if (event.get(darkKey)?.values[0] === false) setDarkSlide('Out')
    }

    const subscriptions = getSubscriptions(eventService, [
      [wheelPositionEvent, onWheelPositionEvent]
    ])

    return () => subscriptions.forEach((s) => s.cancel())
  }, [eventService])

  const filterLabelValueMap: LabelValueMap[] = [
    { label: 'filter', ...filter },
    { label: 'dark slide', current: darkSlide }
  ]

  return <Assembly name={'Filter Wheel'} keyValue={filterLabelValueMap} />
}
