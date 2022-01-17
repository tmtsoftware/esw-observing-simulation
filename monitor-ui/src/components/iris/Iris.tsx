import React from 'react'
import { Assembly } from '../common/Assembly'
import { SubsytemCard } from '../common/SubsystemCard'
import { ADC } from './Adc'
import { FilterWheel } from './FilterWheel'
import { IfsRes } from './IfsRes'
import { IfsScale } from './IfsScale'

export const IRIS = (): JSX.Element => {
  return (
    <SubsytemCard subsystem={'IRIS'}>
      <ADC />
      <FilterWheel />
      <IfsScale />
      <IfsRes />
      <Assembly name={'IFS Detector'} keyValue={[]} />
      <Assembly name={'Imager Detector'} keyValue={[]} />
    </SubsytemCard>
  )
}
