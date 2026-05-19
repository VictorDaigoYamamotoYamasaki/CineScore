import { useState, useRef, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { sessionHelper, userSearchService } from '../services/api'

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

export default function Navbar({ user }) {
  const navigate  = useNavigate()
  const isLogged  = sessionHelper.isLogged()

  const [query,   setQuery]   = useState('')
  const [results, setResults] = useState([])
  const [open,    setOpen]    = useState(false)
  const [loading, setLoading] = useState(false)
  const wrapRef  = useRef(null)
  const timerRef = useRef(null)

  useEffect(() => {
    const q = query.trim()
    if (!q || q.length < 2) { setResults([]); setOpen(false); return }
    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        const { data } = await userSearchService.buscarPorNome(q)
        setResults(data || [])
        setOpen(true)
      } catch { setResults([]) }
      finally { setLoading(false) }
    }, 350)
    return () => clearTimeout(timerRef.current)
  }, [query])

  useEffect(() => {
    const handler = e => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  function goToProfile(userId) {
    setQuery(''); setOpen(false); setResults([])
    navigate(`/profile/${userId}`)
  }

  function logout() {
    sessionHelper.clear()
    navigate('/home')
  }

  return (
    <nav className="navbar">
      <Link to="/home" className="navbar-logo">Cine<span>Score</span></Link>

      {/* Busca de usuários */}
      <div ref={wrapRef} style={{ position:'relative', flex:'0 1 280px' }}>
        <input type="text" placeholder="🔍 Buscar usuário..." value={query}
          onChange={e => setQuery(e.target.value)}
          onFocus={() => results.length > 0 && setOpen(true)}
          autoComplete="off"
          style={{ width:'100%', padding:'0.35rem 0.75rem', borderRadius:999,
            border:'1px solid var(--border)', background:'var(--bg-input)',
            color:'var(--text-primary)', fontSize:'0.82rem' }}
        />
        {open && (
          <div style={{ position:'absolute', top:'calc(100% + 6px)', left:0, right:0,
            background:'var(--bg-card)', border:'1px solid var(--border)',
            borderRadius:10, boxShadow:'0 6px 20px rgba(0,0,0,0.5)', zIndex:500, overflow:'hidden' }}>
            {loading && <div style={{ padding:'0.7rem 1rem', fontSize:'0.82rem', color:'var(--text-muted)' }}>Buscando...</div>}
            {!loading && results.length === 0 && <div style={{ padding:'0.7rem 1rem', fontSize:'0.82rem', color:'var(--text-muted)' }}>Nenhum usuário encontrado</div>}
            {!loading && results.map(u => (
              <div key={u.id} onMouseDown={() => goToProfile(u.id)}
                style={{ display:'flex', alignItems:'center', gap:'0.65rem', padding:'0.55rem 1rem', cursor:'pointer', transition:'background 0.12s' }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                <div className="avatar" style={{ width:28, height:28, fontSize:'0.62rem', flexShrink:0 }}>{initials(u.name)}</div>
                <div style={{ fontSize:'0.85rem', fontWeight:600 }}>{u.name}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="navbar-right">
        {isLogged ? (
          /* Menu para usuário logado */
          <>
            {user?.role === 'ADMIN' && (
              <Link to="/admin"><button className="btn btn--ghost btn--sm">⚙ Admin</button></Link>
            )}
            <Link to="/recommendations">
              <button className="btn btn--sm" style={{ background:'linear-gradient(135deg,#7C5CBF,#e040fb)',
                color:'#fff', border:'none', fontWeight:700, boxShadow:'0 0 12px rgba(224,64,251,0.35)' }}>
                ✨ Para você
              </button>
            </Link>
            <Link to="/reviews/new">
              <button className="btn btn--primary btn--sm">+ Log a film</button>
            </Link>
            <Link to="/profile" style={{ textDecoration:'none' }}>
              <span className="navbar-user" style={{ cursor:'pointer' }} title="Meu perfil">
                {user?.name?.split(' ')[0]}
              </span>
            </Link>
            <button className="navbar-logout" onClick={logout}>Sair</button>
          </>
        ) : (
          /* Menu para visitante não logado */
          <>
            <Link to="/login">
              <button className="btn btn--ghost btn--sm">Entrar</button>
            </Link>
            <Link to="/register">
              <button className="btn btn--primary btn--sm">Criar conta</button>
            </Link>
          </>
        )}
      </div>
    </nav>
  )
}
