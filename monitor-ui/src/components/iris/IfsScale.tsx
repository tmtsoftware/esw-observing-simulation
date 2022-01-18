import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getSubscriptions } from '../common/helpers'
import type { Scale, ScaleLevel } from './IfsScaleHelpers'
import {
  scaleCurrentLevelKey,
  scaleDemandLevelKey,
  scaleLevelEvent
} from './IfsScaleHelpers'

export const IfsScale = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [scale, setScale] = React.useState<Scale>()

  React.useEffect(() => {
    const onscaleLevelEvent = (event: Event) => {
      const current = event.get(scaleCurrentLevelKey)
        ?.values as unknown as ScaleLevel[]
      const target = event.get(scaleDemandLevelKey)
        ?.values as unknown as ScaleLevel[]
      setScale({ current: current[0], target: target[0] })
    }

    const subscriptions = getSubscriptions(eventService, [
      [scaleLevelEvent, onscaleLevelEvent]
    ])

    return () => subscriptions.forEach((s) => s.cancel())
  }, [eventService])

  const scaleLabelValueMap: LabelValueMap[] = [{ label: 'scale', ...scale }]

  return <Assembly name={'IFS Scale'} keyValue={scaleLabelValueMap} />
}
