import { setAppName } from '@tmtsoftware/esw-ts'
import React from 'react'
import App from './App'
import { AppConfig } from './config/AppConfig'
import ReactDOM from 'react-dom/client'
import './index.css'

setAppName(AppConfig.applicationName)
const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement)
root.render(<App />)
