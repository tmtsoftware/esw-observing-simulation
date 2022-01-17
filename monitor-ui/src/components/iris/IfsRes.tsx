import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getSubscriptions } from '../common/helpers'
import type { Res, ResPosition } from './IfsResHelpers'
import {
  resDemandPositionKey,
  resCurrentPositionKey,
  resPositionEvent
} from './IfsResHelpers'

export const IfsRes = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [res, setRes] = React.useState<Res | undefined>(undefined)

  React.useEffect(() => {
    const onResPositionEvent = (event: Event) => {
      const current = event.get(resCurrentPositionKey)
        ?.values as unknown as ResPosition[]
      const target = event.get(resDemandPositionKey)
        ?.values as unknown as ResPosition[]
      setRes({ current: current[0], target: target[0] })
    }

    const subscriptions = getSubscriptions(eventService, [
      [resPositionEvent, onResPositionEvent]
    ])

    return () => subscriptions.forEach((s) => s.cancel())
  }, [eventService])

  const resLabelValueMap: LabelValueMap[] = [{ label: 'grating', ...res }]

  return <Assembly name={'IFS Res'} keyValue={resLabelValueMap} />
}
