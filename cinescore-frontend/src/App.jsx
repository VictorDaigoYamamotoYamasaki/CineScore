import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { sessionHelper } from './services/api'
import LoginPage        from './pages/LoginPage'
import RegisterPage     from './pages/RegisterPage'
import HomePage         from './pages/HomePage'
import MoviePage        from './pages/MoviePage'
import CreateReviewPage from './pages/CreateReviewPage'

function ProtectedRoute({ children }) {
  return sessionHelper.isLogged() ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={
          sessionHelper.isLogged()
            ? <Navigate to="/home" replace />
            : <Navigate to="/login" replace />
        } />

        {}
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {}
        <Route path="/home" element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
        <Route path="/movies/:imdbId" element={<ProtectedRoute><MoviePage /></ProtectedRoute>} />
        <Route path="/reviews/new"    element={<ProtectedRoute><CreateReviewPage /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
