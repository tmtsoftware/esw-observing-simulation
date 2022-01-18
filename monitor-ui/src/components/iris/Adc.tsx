import type { Event } from '@tmtsoftware/esw-ts'
import { booleanKey } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { EventServiceContext } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import { getSubscriptions } from '../common/helpers'
import type { Prism, PrismState, Retract } from './adcHelpers'
import {
  angleErrorKey,
  currentAngleKey,
  followingKey,
  prismEvent,
  prismRetractEvent,
  prismStateEvent,
  retractPositionKey,
  targetAngleKey
} from './adcHelpers'

export const ADC = (): JSX.Element => {
  const eventService = React.useContext(EventServiceContext)
  const [state, setState] = React.useState<PrismState>()
  const [onTarget, setOnTarget] = React.useState<boolean>()
  const [retractState, setRetractState] = React.useState<Retract>()
  const [prism, setPrism] = React.useState<Prism>()

  React.useEffect(() => {
    const onPrismStateEvent = (event: Event) => {
      const values = event.get(followingKey)?.values as unknown as PrismState[]
      setState(values[0])

      const onTargetKey = booleanKey('onTarget')
      setOnTarget(event.get(onTargetKey)?.values[0])
    }

    const onPrismRetractEvent = (event: Event) => {
      const values = event.get(retractPositionKey)
        ?.values as unknown as Retract[]
      setRetractState(values[0])
    }

    const onPrismEvent = (event: Event) => {
      const current = event.get(currentAngleKey)?.values[0]
      const target = event.get(targetAngleKey)?.values[0]
      const error = event.get(angleErrorKey)?.values[0]
      setPrism({ current, target, error })
    }

    const subscriptions = getSubscriptions(eventService, [
      [prismStateEvent, onPrismStateEvent],
      [prismRetractEvent, onPrismRetractEvent],
      [prismEvent, onPrismEvent]
    ])

    return () => subscriptions.forEach((s) => s.cancel())
  }, [eventService])

  const adcLabelValueMap: LabelValueMap[] = [
    { label: 'state', current: state },
    { label: 'onTarget', current: onTarget },
    { label: 'stage', current: retractState },
    { label: 'prism', ...prism }
  ]

  return <Assembly name={'ADC'} keyValue={adcLabelValueMap} />
}
