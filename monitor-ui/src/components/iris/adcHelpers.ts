import {
  choiceKey,
  doubleKey,
  EventKey,
  EventName,
  Prefix
} from '@tmtsoftware/esw-ts'

export type PrismState = 'FOLLOWING' | 'STOPPED'
export const prismState: PrismState[] = ['FOLLOWING', 'STOPPED']

export type Retract = 'IN' | 'OUT'
export const retract: Retract[] = ['IN', 'OUT']

export type Prism = {
  current: number | undefined
  target: number | undefined
  error: number | undefined
}

export const followingKey = choiceKey<PrismState>('following', prismState)
export const retractPositionKey = choiceKey<Retract>('position', retract)
export const currentAngleKey = doubleKey('currentAngle')
export const angleErrorKey = doubleKey('errorAngle')
export const targetAngleKey = doubleKey('targetAngle')

export const adcPrefix = new Prefix('IRIS', 'imager.adc')
export const prismStateEvent = new EventKey(
  adcPrefix,
  new EventName('prism_state')
)
export const prismRetractEvent = new EventKey(
  adcPrefix,
  new EventName('prism_position')
)
export const prismEvent = new EventKey(
  adcPrefix,
  new EventName('prism_current')
)
