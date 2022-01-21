import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { Res } from './IfsResHelpers'
import {
  resCurrentPositionKey,
  resDemandPositionKey,
  resPositionEvent
} from './IfsResHelpers'

export const IfsRes = (): JSX.Element => {
  const eventService = useEventService()
  const [res, setRes] = React.useState<Res>()

  React.useEffect(() => {
    const onResPositionEvent = (event: Event) => {
      const current = event.get(resCurrentPositionKey)?.values[0]
      const target = event.get(resDemandPositionKey)?.values[0]
      setRes({ current: current, target: target })
    }

    const subscription = eventService?.subscribe(
      new Set([resPositionEvent]),
      10
    )(onResPositionEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const resLabelValueMap: LabelValueMap[] = [{ label: 'grating', ...res }]

  return <Assembly name={'IFS Res'} keyValue={resLabelValueMap} />
}
