import { choiceKey, EventKey, EventName, Prefix } from '@tmtsoftware/esw-ts'

const resPosition = [
  '4000-Z',
  '4000-Y',
  '4000-J',
  '4000-H',
  '4000-K',
  '4000-H+K',
  '8000-Z',
  '8000-Y',
  '8000-J',
  '8000-H',
  '8000-Kn1-3',
  '8000-Kn4-5',
  '8000-Kbb',
  '10000-Z',
  '10000-Y',
  '10000-J',
  '10000-H',
  '1000-K',
  'Mirror'
] as const

export type ResPosition = typeof resPosition[number]

export type Res = {
  current: ResPosition | undefined
  target: ResPosition | undefined
}

export const resCurrentPositionKey = choiceKey<ResPosition>(
  'current',
  resPosition
)

export const resDemandPositionKey = choiceKey<ResPosition>(
  'target',
  resPosition
)

export const ifsResPrefix = new Prefix('IRIS', 'ifs.res')
export const resPositionEvent = new EventKey(
  ifsResPrefix,
  new EventName('SpectralResolutionPosition')
)
