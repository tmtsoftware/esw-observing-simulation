import React from 'react'
import { SubsytemCard } from '../common/SubsystemCard'
import { BFL } from './Bfl'
import { BlueDetector, RedDetector } from './Detector'
//import { FilterWheel } from './FilterWheel'
import { RFL } from './Rfl'

export const WFOS = (): JSX.Element => (
  <SubsytemCard subsystem={'WFOS'}>
    <BFL />
    <RFL />
    <BlueDetector />
    <RedDetector />
  </SubsytemCard>
)
