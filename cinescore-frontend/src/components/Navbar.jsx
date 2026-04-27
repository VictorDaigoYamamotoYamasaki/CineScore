import { useNavigate, Link } from 'react-router-dom'
import { sessionHelper } from '../services/api'

export default function Navbar({ user }) {
  const navigate = useNavigate()

  function logout() {
    sessionHelper.clear()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <Link to="/home" className="navbar-logo">
        Cine<span>Score</span>
      </Link>

      <div className="navbar-right">
        <Link to="/reviews/new">
          <button className="btn btn--primary btn--sm">+ Log a film</button>
        </Link>
        <span className="navbar-user">{user?.name?.split(' ')[0]}</span>
        <button className="navbar-logout" onClick={logout}>Sair</button>
      </div>
    </nav>
  )
}
