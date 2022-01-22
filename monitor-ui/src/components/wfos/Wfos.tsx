import React from 'react'
import { SubsytemCard } from '../common/SubsystemCard'
import { BFL } from './Bfl'

export const WFOS = (): JSX.Element => (
  <SubsytemCard subsystem={'WFOS'}>
    <BFL />
  </SubsytemCard>
)
