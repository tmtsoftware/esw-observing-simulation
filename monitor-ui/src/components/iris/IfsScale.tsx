import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { Scale } from './IfsScaleHelpers'
import {
  scaleCurrentLevelKey,
  scaleDemandLevelKey,
  scaleLevelEvent
} from './IfsScaleHelpers'

export const IfsScale = (): JSX.Element => {
  const eventService = useEventService()
  const [scale, setScale] = React.useState<Scale>()

  React.useEffect(() => {
    const onscaleLevelEvent = (event: Event) => {
      const current = event.get(scaleCurrentLevelKey)?.values[0]
      const target = event.get(scaleDemandLevelKey)?.values[0]
      setScale({ current: current, target: target })
    }

    const subscription = eventService?.subscribe(
      new Set([scaleLevelEvent]),
      10
    )(onscaleLevelEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const scaleLabelValueMap: LabelValueMap[] = [{ label: 'scale', ...scale }]

  return <Assembly name={'IFS Scale'} keyValue={scaleLabelValueMap} />
}
