import React from 'react'
import { SubsystemCard } from '../common/SubsystemCard'
import { BFL } from './Bfl'
import { BlueDetector, RedDetector } from './Detector'
import { RFL } from './Rfl'

export const WFOS = (): JSX.Element => (
  <SubsystemCard subsystem={'WFOS'}>
    <BFL />
    <RFL />
    <BlueDetector />
    <RedDetector />
  </SubsystemCard>
)
