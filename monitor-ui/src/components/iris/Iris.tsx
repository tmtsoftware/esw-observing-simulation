import React from 'react'
import { Assembly } from '../common/Assembly'
import { SubsytemHeader } from '../common/Subsystem'
import { ADC } from './Adc'

export const IRIS = () => {
  return (
    <SubsytemHeader subsystem={'IRIS'}>
      <ADC />
      <Assembly name={'Imager'} keyValue={[]} />
      <Assembly name={'Filter Wheel'} keyValue={[]} />
      <Assembly name={'IFS Scale'} keyValue={[]} />
      <Assembly name={'IFS Res'} keyValue={[]} />
      <Assembly name={'IFS Detector'} keyValue={[]} />
      <Assembly name={'Imager Detector'} keyValue={[]} />
    </SubsytemHeader>
  )
}
