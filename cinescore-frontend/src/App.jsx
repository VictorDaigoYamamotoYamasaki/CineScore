import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { sessionHelper } from './services/api'
import LoginPage         from './pages/LoginPage'
import RegisterPage      from './pages/RegisterPage'
import HomePage          from './pages/HomePage'
import MoviePage         from './pages/MoviePage'
import CreateReviewPage  from './pages/CreateReviewPage'
import AdminPage         from './pages/AdminPage'
import ActorPage         from './pages/ActorPage'
import ProfilePage       from './pages/ProfilePage'
import FollowersPage     from './pages/FollowersPage'
import RecommendationPage from './pages/RecommendationPage'

// Apenas rotas que exigem login (criar review, recomendações, admin)
function PrivateRoute({ children }) {
  return sessionHelper.isLogged() ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Raiz: vai para /home sempre */}
        <Route path="/" element={<Navigate to="/home" replace />} />

        {/* Autenticação */}
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Rotas públicas — qualquer visitante pode ver */}
        <Route path="/home"            element={<HomePage />} />
        <Route path="/movies/:movieId" element={<MoviePage />} />
        <Route path="/actors/:actorId" element={<ActorPage />} />
        <Route path="/profile/:userId" element={<ProfilePage />} />
        <Route path="/profile/:userId/network" element={<FollowersPage />} />

        {/* Rotas privadas — exigem login */}
        <Route path="/profile"         element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
        <Route path="/reviews/new"     element={<PrivateRoute><CreateReviewPage /></PrivateRoute>} />
        <Route path="/recommendations" element={<PrivateRoute><RecommendationPage /></PrivateRoute>} />
        <Route path="/admin"           element={<PrivateRoute><AdminPage /></PrivateRoute>} />

        <Route path="*" element={<Navigate to="/home" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
