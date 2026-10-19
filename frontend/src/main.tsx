import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import 'bootstrap/dist/css/bootstrap.min.css'
import './styles.css'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import { VigenciaProvider } from './context/VigenciaContext'
import App from './App'

createRoot(document.getElementById('root')!).render(<React.StrictMode><BrowserRouter><AuthProvider><ToastProvider><VigenciaProvider><App /></VigenciaProvider></ToastProvider></AuthProvider></BrowserRouter></React.StrictMode>)
