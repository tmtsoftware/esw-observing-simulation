import {
  booleanKey,
  choiceKey,
  doubleKey,
  EventKey,
  EventName,
  Prefix
} from '@tmtsoftware/esw-ts'

const blueFilterWheelPosition = ['u-prime', 'g-prime', 'fused-silica'] as const
export type BlueFilterWheelPosition = typeof blueFilterWheelPosition[number]

export type FilterPosition = {
  current: number | undefined
  target: number | undefined
}


export const filterCurrentPositionKey = choiceKey<BlueFilterWheelPosition>('current', blueFilterWheelPosition)

export const filterDemandPositionKey = choiceKey<BlueFilterWheelPosition>('demand', blueFilterWheelPosition)

export const bflPrefix = new Prefix('WFOS', 'blue.filter')
export const blueFilterPositionEvent = new EventKey(
  bflPrefix,
  new EventName('Wheel1Position')
)

