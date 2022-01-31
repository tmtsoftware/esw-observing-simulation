import type { Event } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEffect, useState } from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { AngleP } from './TCSHelpers'
import {
  baseCurrentKey,
  baseDemandKey,
  capCurrentKey,
  capDemandKey,
  currentPositionEventKey,
  round
} from './TCSHelpers'

export const ENCAssembly = (): JSX.Element => {
  const eventService = useEventService()
  const [cap, setCap] = useState<AngleP>()
  const [base, setBase] = useState<AngleP>()

  useEffect(() => {
    const onEvent = (event: Event) => {
      const capCurrent = event.get(capCurrentKey)?.values[0]
      const capDemand = event.get(capDemandKey)?.values[0]
      const baseCurrent = event.get(baseCurrentKey)?.values[0]
      const baseDemand = event.get(baseDemandKey)?.values[0]

      const roundCurrentCap = round(capCurrent)
      const roundTargetCap = round(capDemand)
      setCap({
        current: roundCurrentCap,
        target: roundTargetCap,
        error:
          roundCurrentCap && roundTargetCap
            ? round(roundTargetCap - roundCurrentCap)
            : undefined
      })

      const roundCurrentBase = round(baseCurrent)
      const roundTargetBase = round(baseDemand)
      setBase({
        current: roundCurrentBase,
        target: roundTargetBase,
        error:
          roundCurrentBase && roundTargetBase
            ? round(roundTargetBase - roundCurrentBase)
            : undefined
      })
    }

    const subscription = eventService?.subscribe(
      new Set([currentPositionEventKey]),
      10
    )(onEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const labelValueMap: LabelValueMap[] = [
    { label: 'base', ...base },
    { label: 'cap', ...cap }
  ]

  return <Assembly name={'Enclosure'} keyValue={labelValueMap} />
}
