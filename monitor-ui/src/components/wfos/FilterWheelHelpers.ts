import { choiceKey, EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

const blueFilterWheelPosition = ['u-prime', 'g-prime', 'fused-silica'] as const
export type BlueFilterWheelPosition = typeof blueFilterWheelPosition[number]

const redFilterWheelPosition = [
  'r-prime',
  'i-prime',
  'z-prime',
  'fused-silica'
] as const
export type RedFilterWheelPosition = typeof redFilterWheelPosition[number]

export type BlueFilterPosition = {
  current: string | undefined
  target: string | undefined
}

export type RedFilterPosition = {
  current: string | undefined
  target: string | undefined
}

export const blueCurrentPositionKey = choiceKey<BlueFilterWheelPosition>(
  'current',
  blueFilterWheelPosition
)

export const blueDemandPositionKey = choiceKey<BlueFilterWheelPosition>(
  'demand',
  blueFilterWheelPosition
)

export const redCurrentPositionKey = choiceKey<RedFilterWheelPosition>(
  'current',
  redFilterWheelPosition
)

export const redDemandPositionKey = choiceKey<RedFilterWheelPosition>(
  'demand',
  redFilterWheelPosition
)

export const bflPrefix = new Prefix('WFOS', 'blue.filter')
export const blueFilterPositionEvent = new EventKey(
  bflPrefix,
  new EventName('Wheel1Position')
)

export const rflPrefix = new Prefix('WFOS', 'red.filter')
export const redFilterPositionEvent = new EventKey(
  rflPrefix,
  new EventName('Wheel1Position')
)
