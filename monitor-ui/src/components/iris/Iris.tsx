import React from 'react'
import { SubsytemCard } from '../common/SubsystemCard'
import { ADC } from './Adc'
import { FilterWheel } from './FilterWheel'
import { IfsDetector } from './IfsDetector'
import { IfsRes } from './IfsRes'
import { IfsScale } from './IfsScale'
import { ImagerDetector } from './ImagerDetector'

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
