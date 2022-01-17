import React from 'react'
import { Assembly } from '../common/Assembly'
import { SubsytemCard } from '../common/SubsystemCard'
import { ADC } from './Adc'

export const IRIS = (): JSX.Element => {
  return (
    <SubsytemCard subsystem={'IRIS'}>
      <ADC />
      <Assembly name={'Filter Wheel'} keyValue={[]} />
      <Assembly name={'IFS Scale'} keyValue={[]} />
      <Assembly name={'IFS Res'} keyValue={[]} />
      <Assembly name={'IFS Detector'} keyValue={[]} />
      <Assembly name={'Imager Detector'} keyValue={[]} />
    </SubsytemCard>
  )
}
