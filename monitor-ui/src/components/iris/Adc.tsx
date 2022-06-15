import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { Prism, PrismState, Retract } from './adcHelpers'
import {
  angleErrorKey,
  currentAngleKey,
  followingKey,
  prismEvent,
  prismRetractEvent,
  prismStateEvent,
  retractPositionKey,
  targetAngleKey,
  onTargetKey
} from './adcHelpers'

export const ADC = (): JSX.Element => {
  const eventService = useEventService()
  const [state, setState] = React.useState<PrismState>()
  const [onTarget, setOnTarget] = React.useState<boolean>()
  const [retractState, setRetractState] = React.useState<Retract>()
  const [prism, setPrism] = React.useState<Prism>()

  React.useEffect(() => {
    const onPrismEvent = (event: Event) => {
      switch (event.eventName.name) {
        case prismStateEvent.eventName.name:
          setState(event.get(followingKey)?.values[0])
          setOnTarget(event.get(onTargetKey)?.values[0])
          break
        case prismRetractEvent.eventName.name:
          setRetractState(event.get(retractPositionKey)?.values[0])
          break
        case prismEvent.eventName.name:
          const current = event.get(currentAngleKey)?.values[0]
          const target = event.get(targetAngleKey)?.values[0]
          const error = event.get(angleErrorKey)?.values[0]
          setPrism({ current, target, error })
          break
      }
    }

    const subscription = eventService?.subscribe(
      new Set([prismStateEvent, prismRetractEvent, prismEvent])
    )(onPrismEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const adcLabelValueMap: LabelValueMap[] = [
    { label: 'state', current: state },
    { label: 'onTarget', current: onTarget },
    { label: 'stage', current: retractState },
    { label: 'prism', ...prism }
  ]

  return <Assembly name={'ADC'} keyValue={adcLabelValueMap} />
}
