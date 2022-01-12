import React from 'react'
import { LocationService, loadGlobalConfig } from '@tmtsoftware/esw-ts'
import { BrowserRouter as Router } from 'react-router-dom'
import { AppConfig } from './config/AppConfig'
import { LocationServiceProvider } from './contexts/LocationServiceContext'
import { useQuery } from './hooks/useQuery'
import { Main} from './components/Main'

const basename = import.meta.env.PROD === 'production' ? `/${AppConfig.applicationName}` : ''

const App = (): JSX.Element => {
  const { data: initialised, error } = useQuery(() => loadGlobalConfig().then(() => true))
  const locationService = LocationService()

  if (error) return <div> Failed to load global config </div>
  return initialised ? (
    <LocationServiceProvider locationService={locationService}>
      <Router basename={basename}>
        <Main />
      </Router>
    </LocationServiceProvider>
  ) : (
    <div>Loading....</div>
  )
}

export default App
