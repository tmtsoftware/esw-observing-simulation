import React from 'react'
import { SubsytemCard } from '../common/SubsystemCard'
import { ADC } from './Adc'
import { IfsDetector, ImagerDetector } from './Detector'
import { FilterWheel } from './FilterWheel'
import { IfsRes } from './IfsRes'
import { IfsScale } from './IfsScale'

export const IRIS = (): JSX.Element => (
  <SubsytemCard subsystem={'IRIS'}>
    <ADC />
    <FilterWheel />
    <IfsScale />
    <IfsRes />
    <IfsDetector />
    <ImagerDetector />
  </SubsytemCard>
)
