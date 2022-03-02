import type { Event } from '@tmtsoftware/esw-ts'
import { Angle } from '@tmtsoftware/esw-ts'
import * as React from 'react'
import { useEffect, useState } from 'react'
import { useEventService } from '../../contexts/EventServiceContext'
import type { LabelValueMap } from '../common/Assembly'
import { Assembly } from '../common/Assembly'
import type { AngleP, EqCoordP } from './TCSHelpers'
import {
  currentAltAzCoordKey,
  currentHourAngleKey,
  currentPosKey,
  demandAltAzCoordKey,
  demandHourAngleKey,
  demandPosKey,
  mountPositionEventKey,
  round,
  siderealTimeKey
} from './TCSHelpers'

const degreeToString = (angleInDegrees?: number) =>
  angleInDegrees
    ? Angle.deToString(Angle.D2R * angleInDegrees, true)
    : undefined

export const MCSAssembly = (): JSX.Element => {
  const eventService = useEventService()
  const [altPosition, setAltPosition] = useState<AngleP>()
  const [azPosition, setAzPosition] = useState<AngleP>()
  const [raValue, setRaValue] = useState<EqCoordP>()
  const [decValue, setDecValue] = useState<EqCoordP>()
  const [siderealTime, setSiderealTime] = useState<{ current?: string }>()
  const [hourAngle, setHourAngle] = useState<AngleP>()

  useEffect(() => {
    const onEvent = (event: Event) => {
      const currentAltAzCoord = event.get(currentAltAzCoordKey)?.values[0]
      const demandAltAzCoord = event.get(demandAltAzCoordKey)?.values[0]
      const currentPos = event.get(currentPosKey)?.values[0]
      const demandPos = event.get(demandPosKey)?.values[0]

      const currentRa = currentPos
        ? Angle.raToString(currentPos?.ra.toRadian(), true)
        : undefined
      const currentDec = currentPos
        ? Angle.deToString(currentPos?.dec.toRadian(), true)
        : undefined
      const demandRa = demandPos
        ? Angle.raToString(demandPos?.ra.toRadian(), true)
        : undefined
      const demandDec = demandPos
        ? Angle.deToString(demandPos?.dec.toRadian(), true)
        : undefined

      setRaValue({
        current: currentRa,
        target: demandRa
      })

      setDecValue({
        current: currentDec,
        target: demandDec
      })

      const currentAlt = round(currentAltAzCoord?.alt.toDegree())
      const targetAlt = round(demandAltAzCoord?.alt.toDegree())

      setAltPosition({
        current: currentAlt,
        target: targetAlt,
        error:
          currentAlt && targetAlt ? round(targetAlt - currentAlt) : undefined
      })

      const currentAz = round(currentAltAzCoord?.az.toDegree())
      const targetAz = round(demandAltAzCoord?.az.toDegree())
      setAzPosition({
        current: currentAz,
        target: targetAz,
        error: currentAz && targetAz ? round(targetAz - currentAz) : undefined
      })

      const siderealTimeP = event.get(siderealTimeKey)?.values[0]
      const currentHourAngle = event.get(currentHourAngleKey)?.values[0]
      const demandHourAngle = event.get(demandHourAngleKey)?.values[0]
      const hourAngleError =
        currentHourAngle &&
        demandHourAngle &&
        round(demandHourAngle - currentHourAngle)

      setSiderealTime({
        current: siderealTimeP
          ? Angle.raToString(Angle.H2R * siderealTimeP, true)
          : undefined
      })
      setHourAngle({
        current: degreeToString(currentHourAngle),
        target: degreeToString(demandHourAngle),
        error: degreeToString(hourAngleError)
      })
    }

    const subscription = eventService?.subscribe(
      new Set([mountPositionEventKey]),
      10
    )(onEvent)

    return () => subscription?.cancel()
  }, [eventService])

  const labelValueMap: LabelValueMap[] = [
    { label: 'sidereal time', ...siderealTime },
    { label: 'hour angle', ...hourAngle },
    { label: 'ra', ...raValue },
    { label: 'dec', ...decValue },
    { label: 'az', ...azPosition },
    { label: 'alt', ...altPosition }
  ]

  return <Assembly name={'Mount Position'} keyValue={labelValueMap} />
}
