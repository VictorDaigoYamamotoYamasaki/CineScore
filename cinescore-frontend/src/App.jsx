import { ThemeProvider } from './context/ThemeContext'
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { sessionHelper } from './services/api'
import ErrorBoundary       from './components/ErrorBoundary'
import LoginPage           from './pages/LoginPage'
import ForgotPasswordPage  from './pages/ForgotPasswordPage'
import ResetPasswordPage   from './pages/ResetPasswordPage'
import RegisterPage        from './pages/RegisterPage'
import HomePage            from './pages/HomePage'
import MoviePage           from './pages/MoviePage'
import CreateReviewPage    from './pages/CreateReviewPage'
import AdminPage           from './pages/AdminPage'
import ActorPage           from './pages/ActorPage'
import ProfilePage         from './pages/ProfilePage'
import FollowersPage       from './pages/FollowersPage'
import RecommendationPage  from './pages/RecommendationPage'

function PrivateRoute({ children }) {
  const location = useLocation()
  return sessionHelper.isLogged()
    ? children
    : <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
}

export default function App() {
  return (
    <ThemeProvider>
    <BrowserRouter>
      <ErrorBoundary>
        <Routes>
          <Route path="/" element={<Navigate to="/home" replace />} />

          {/* Autenticação */}
          <Route path="/login"           element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password"  element={<ResetPasswordPage />} />
          <Route path="/register"        element={<RegisterPage />} />

          {/* Rotas públicas */}
          <Route path="/home"                          element={<HomePage />} />
          <Route path="/movies/:movieId"               element={<MoviePage />} />
          <Route path="/actors/:actorId"               element={<ActorPage />} />
          <Route path="/profile/:userId"               element={<ProfilePage />} />
          <Route path="/profile/:userId/network"       element={<FollowersPage />} />

          {/* Rotas privadas */}
          <Route path="/profile"         element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
          <Route path="/reviews/new"     element={<PrivateRoute><CreateReviewPage /></PrivateRoute>} />
          <Route path="/recommendations" element={<PrivateRoute><RecommendationPage /></PrivateRoute>} />
          <Route path="/admin"           element={<PrivateRoute><AdminPage /></PrivateRoute>} />

          <Route path="*" element={<Navigate to="/home" replace />} />
        </Routes>
      </ErrorBoundary>
    </BrowserRouter>
    </ThemeProvider>
  )
}
