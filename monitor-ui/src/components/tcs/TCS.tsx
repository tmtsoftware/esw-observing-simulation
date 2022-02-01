import * as React from 'react'
import { SubsystemCard } from '../common/SubsystemCard'
import { ENCAssembly } from './ENCAssembly'
import { MCSAssembly } from './MCSAssembly'

export const TCS = () => (
  <SubsystemCard subsystem={'TCS'}>
    <MCSAssembly />
    <ENCAssembly />
  </SubsystemCard>
)
