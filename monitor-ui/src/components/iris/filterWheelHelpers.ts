import { choiceKey, EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

export const filterPosition = [
  'Z',
  'Y',
  'J',
  'H',
  'K',
  'Ks',
  'H+K notch',
  'CO',
  'BrGamma',
  'PaBeta',
  'H2',
  'FeII',
  'HeI',
  'CaII Trip',
  'J Cont',
  'H Cont',
  'K Cont'
] as const

export type FilterPosition = typeof filterPosition[number]

export type Filter = {
  current: FilterPosition | undefined
  target: FilterPosition | undefined
}

export const filterCurrentPositionKey = choiceKey<FilterPosition>(
  'current',
  filterPosition
)

export const filterDemandPositionKey = choiceKey<FilterPosition>(
  'demand',
  filterPosition
)

export const filterWheelPrefix = new Prefix('IRIS', 'imager.filter')
export const wheelPositionEvent = new EventKey(
  filterWheelPrefix,
  new EventName('Wheel1Position')
)
