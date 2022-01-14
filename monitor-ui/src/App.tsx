import { LocationService, loadGlobalConfig } from '@tmtsoftware/esw-ts'
import { Spin } from 'antd'
import React from 'react'
import { BrowserRouter as Router } from 'react-router-dom'
import { Main } from './components/Main'
import { AppConfig } from './config/AppConfig'
import EventServiceProvider from './contexts/EventServiceContext'
import { LocationServiceProvider } from './contexts/LocationServiceContext'
import { useQuery } from './hooks/useQuery'

const basename =
  import.meta.env.PROD === 'production' ? `/${AppConfig.applicationName}` : ''

const App = (): JSX.Element => {
  const { data: initialised, error } = useQuery(() =>
    loadGlobalConfig().then(() => true)
  )
  const locationService = LocationService()

  if (error) return <div> Failed to load global config </div>
  return initialised ? (
    <LocationServiceProvider locationService={locationService}>
      <EventServiceProvider>
        <Router basename={basename}>
          <Main />
        </Router>
      </EventServiceProvider>
    </LocationServiceProvider>
  ) : (
    <Spin />
  )
}

export default App
