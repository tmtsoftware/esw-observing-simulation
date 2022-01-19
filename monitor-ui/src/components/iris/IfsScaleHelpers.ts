import { choiceKey, EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

const scaleLevel = ['4', '9', '25', '50'] as const
export type ScaleLevel = typeof scaleLevel[number]

export type Scale = {
  current: ScaleLevel | undefined
  target: ScaleLevel | undefined
}

export const scaleCurrentLevelKey = choiceKey<ScaleLevel>('current', scaleLevel)

export const scaleDemandLevelKey = choiceKey<ScaleLevel>('target', scaleLevel)

export const ifsScalePrefix = new Prefix('IRIS', 'ifs.scale')
export const scaleLevelEvent = new EventKey(
  ifsScalePrefix,
  new EventName('TargetScale')
)
