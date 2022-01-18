import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getSubscriptions } from '../common/helpers'
import type { Scale } from './IfsScaleHelpers'
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
      const current = event.get(scaleCurrentLevelKey)?.values[0]
      const target = event.get(scaleDemandLevelKey)?.values[0]
      setScale({ current: current, target: target })
    }

    const subscriptions = getSubscriptions(eventService, [
      [scaleLevelEvent, onscaleLevelEvent]
    ])

    return () => subscriptions.forEach((s) => s.cancel())
  }, [eventService])

  const scaleLabelValueMap: LabelValueMap[] = [{ label: 'scale', ...scale }]

  return <Assembly name={'IFS Scale'} keyValue={scaleLabelValueMap} />
}
