import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { sessionHelper } from './services/api'
import LoginPage        from './pages/LoginPage'
import RegisterPage     from './pages/RegisterPage'
import HomePage         from './pages/HomePage'
import MoviePage        from './pages/MoviePage'
import CreateReviewPage from './pages/CreateReviewPage'
import AdminPage        from './pages/AdminPage'
import ActorPage        from './pages/ActorPage'
import ProfilePage      from './pages/ProfilePage'
import FollowersPage       from './pages/FollowersPage'
import RecommendationPage  from './pages/RecommendationPage'

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

        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route path="/home"            element={<ProtectedRoute><HomePage /></ProtectedRoute>} />
        <Route path="/movies/:movieId" element={<ProtectedRoute><MoviePage /></ProtectedRoute>} />
        <Route path="/reviews/new"     element={<ProtectedRoute><CreateReviewPage /></ProtectedRoute>} />
        <Route path="/actors/:actorId" element={<ProtectedRoute><ActorPage /></ProtectedRoute>} />
        <Route path="/profile"         element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/profile/:userId" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/profile/:userId/network" element={<ProtectedRoute><FollowersPage /></ProtectedRoute>} />
        <Route path="/recommendations" element={<ProtectedRoute><RecommendationPage /></ProtectedRoute>} />
        <Route path="/admin"           element={<ProtectedRoute><AdminPage /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
