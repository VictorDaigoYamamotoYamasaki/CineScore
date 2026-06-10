import { useState, useRef, useEffect } from 'react'
import { useNavigate, Link }           from 'react-router-dom'
import { sessionHelper, userSearchService } from '../services/api'
import { useTheme } from '../context/ThemeContext'

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

// ── Toggle switch component ──────────────────────────────────────────────────
function ThemeToggle({ theme, onToggle }) {
  const isLight = theme === 'light'
  return (
    <div
      onClick={onToggle}
      title={isLight ? 'Mudar para modo escuro' : 'Mudar para modo claro'}
      style={{
        display: 'flex', alignItems: 'center', gap: '0.5rem',
        cursor: 'pointer', userSelect: 'none',
        width: '100%', padding: '0.55rem 1rem',
        transition: 'background 0.12s',
        borderRadius: 6,
      }}
      onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
      onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
    >
      {/* Sun icon */}
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
        stroke="var(--text-secondary)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="5"/>
        <line x1="12" y1="1" x2="12" y2="3"/>
        <line x1="12" y1="21" x2="12" y2="23"/>
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
        <line x1="1" y1="12" x2="3" y2="12"/>
        <line x1="21" y1="12" x2="23" y2="12"/>
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
      </svg>

      <span style={{ fontSize: '0.84rem', fontWeight: 500, color: 'var(--text-primary)', flex: 1 }}>
        Modo claro
      </span>

      {/* Toggle pill */}
      <div style={{
        width: 36, height: 20, borderRadius: 10, flexShrink: 0,
        background: isLight ? 'var(--purple-accent)' : 'var(--bg-elevated)',
        border: '1.5px solid var(--border)',
        position: 'relative', transition: 'background 0.2s',
      }}>
        <div style={{
          position: 'absolute', top: 2,
          left: isLight ? 16 : 2,
          width: 12, height: 12, borderRadius: '50%',
          background: isLight ? '#fff' : 'var(--text-muted)',
          transition: 'left 0.2s',
          boxShadow: '0 1px 3px rgba(0,0,0,0.25)',
        }} />
      </div>
    </div>
  )
}

export default function Navbar({ user }) {
  const navigate = useNavigate()
  const isLogged = sessionHelper.isLogged()
  const { theme, toggleTheme } = useTheme()

  const [query,    setQuery]    = useState('')
  const [results,  setResults]  = useState([])
  const [open,     setOpen]     = useState(false)
  const [loading,  setLoading]  = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  const searchRef = useRef(null)
  const menuRef   = useRef(null)
  const timerRef  = useRef(null)

  useEffect(() => {
    const handler = e => {
      if (searchRef.current && !searchRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  useEffect(() => {
    const handler = e => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

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

  function goToProfile(userId) {
    setQuery(''); setOpen(false); setResults([])
    navigate(`/profile/${userId}`)
  }

  function logout() {
    setMenuOpen(false)
    sessionHelper.clear()
    navigate('/home')
  }

  const menuItemStyle = {
    display: 'flex', alignItems: 'center', gap: '0.6rem',
    width: '100%', padding: '0.55rem 1rem',
    background: 'none', border: 'none', cursor: 'pointer',
    color: 'var(--text-primary)', fontSize: '0.84rem', fontWeight: 500,
    textAlign: 'left', transition: 'background 0.12s',
    whiteSpace: 'nowrap', borderRadius: 6,
  }

  return (
    <nav className="navbar">
      <Link to="/home" className="navbar-logo">Cine<span>Score</span></Link>

      {/* Busca de usuários */}
      <div ref={searchRef} style={{ position: 'relative', flex: '0 1 280px' }}>
        <input
          type="text" placeholder="🔍 Buscar usuário..." value={query}
          onChange={e => setQuery(e.target.value)}
          onFocus={() => results.length > 0 && setOpen(true)}
          autoComplete="off"
          style={{
            width: '100%', padding: '0.35rem 0.75rem', borderRadius: 999,
            border: '1px solid var(--border)', background: 'var(--bg-input)',
            color: 'var(--text-primary)', fontSize: '0.82rem',
          }}
        />
        {open && (
          <div style={{
            position: 'absolute', top: 'calc(100% + 6px)', left: 0, right: 0,
            background: 'var(--bg-card)', border: '1px solid var(--border)',
            borderRadius: 10, boxShadow: '0 6px 20px rgba(0,0,0,0.25)',
            zIndex: 500, overflow: 'hidden',
          }}>
            {loading && (
              <div style={{ padding: '0.7rem 1rem', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                Buscando...
              </div>
            )}
            {!loading && results.length === 0 && (
              <div style={{ padding: '0.7rem 1rem', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                Nenhum usuário encontrado
              </div>
            )}
            {!loading && results.map(u => (
              <div
                key={u.id}
                onMouseDown={() => goToProfile(u.id)}
                style={{
                  display: 'flex', alignItems: 'center', gap: '0.65rem',
                  padding: '0.55rem 1rem', cursor: 'pointer', transition: 'background 0.12s',
                }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              >
                <div className="avatar" style={{ width: 28, height: 28, fontSize: '0.62rem', flexShrink: 0 }}>
                  {initials(u.name)}
                </div>
                <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{u.name}</div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Ações principais + menu */}
      <div className="navbar-right">
        {isLogged ? (
          <>
            {user?.role === 'ADMIN' && (
              <Link to="/admin">
                <button className="btn btn--ghost btn--sm">⚙ Admin</button>
              </Link>
            )}
            <Link to="/recommendations">
              <button className="btn btn--sm" style={{
                background: 'linear-gradient(135deg,#7C5CBF,#e040fb)',
                color: '#fff', border: 'none', fontWeight: 700,
                boxShadow: '0 0 12px rgba(224,64,251,0.35)',
              }}>
                Para você
              </button>
            </Link>
            <Link to="/reviews/new">
              <button className="btn btn--primary btn--sm">+ Nova review</button>
            </Link>
            <Link to="/profile" style={{ textDecoration: 'none' }}>
              <span className="navbar-user" style={{ cursor: 'pointer' }} title="Meu perfil">
                {user?.name?.split(' ')[0]}
              </span>
            </Link>
          </>
        ) : (
          <>
            <Link to="/login">
              <button className="btn btn--ghost btn--sm">Entrar</button>
            </Link>
            <Link to="/register">
              <button className="btn btn--primary btn--sm">Criar conta</button>
            </Link>
          </>
        )}

        {/* Botão hambúrguer estilo browser */}
        <div ref={menuRef} style={{ position: 'relative' }}>
          <button
            onClick={() => setMenuOpen(v => !v)}
            title="Menu"
            style={{
              width: 34, height: 34, borderRadius: 6,
              border: `1px solid ${menuOpen ? 'var(--purple-accent)' : 'var(--border)'}`,
              background: menuOpen ? 'var(--bg-surface)' : 'transparent',
              color: 'var(--text-secondary)', cursor: 'pointer',
              display: 'flex', flexDirection: 'column',
              alignItems: 'center', justifyContent: 'center', gap: '4px',
              transition: 'all 0.15s', flexShrink: 0, padding: 0,
            }}
          >
            {/* Três linhas — estilo Chrome/Brave */}
            {[0,1,2].map(i => (
              <span key={i} style={{
                display: 'block', width: 16, height: 1.5,
                borderRadius: 2, background: 'var(--text-secondary)',
                transition: 'all 0.15s',
              }} />
            ))}
          </button>

          {menuOpen && (
            <div style={{
              position: 'absolute', top: 'calc(100% + 6px)', right: 0,
              background: 'var(--bg-card)', border: '1px solid var(--border)',
              borderRadius: 10, boxShadow: '0 6px 24px rgba(0,0,0,0.18)',
              minWidth: 200, zIndex: 600, overflow: 'hidden',
              padding: '4px',
            }}>
              {/* Toggle de tema */}
              <ThemeToggle theme={theme} onToggle={() => { toggleTheme(); setMenuOpen(false) }} />

              {/* Sair — só para logados */}
              {isLogged && (
                <>
                  <div style={{ height: 1, background: 'var(--border)', margin: '4px 0' }} />
                  <button
                    onClick={logout}
                    style={{ ...menuItemStyle, color: 'var(--red)' }}
                    onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
                    onMouseLeave={e => e.currentTarget.style.background = 'none'}
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                      stroke="var(--red)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                      <polyline points="16 17 21 12 16 7"/>
                      <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                    <span>Sair</span>
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      </div>
    </nav>
  )
}
