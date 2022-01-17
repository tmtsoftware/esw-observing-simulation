import { choiceKey, EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

export type ScaleLevel = '4' | '9' | '25' | '50'
export const scaleLevel: ScaleLevel[] = ['4', '9', '25', '50']

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
