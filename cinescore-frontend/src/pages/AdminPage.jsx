import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionHelper, movieService } from '../services/api'
import axios from 'axios'

const api = axios.create({ baseURL: '/api' })
api.interceptors.request.use(cfg => {
  const t = localStorage.getItem('cinescore_token')
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

const adminApi = {
  stats:          ()           => api.get('/admin/stats'),
  popularMovies:  ()           => api.get('/admin/popular-movies'),
  listarUsuarios: (page, size) => api.get(`/admin/users?page=${page}&size=${size}`),
  deletarUsuario: id           => api.delete(`/admin/users/${id}`),
  listarReviews:  (page, size) => api.get(`/admin/reviews?page=${page}&size=${size}`),
  deletarReview:  id           => api.delete(`/admin/reviews/${id}`),
}

const PAGE_SIZE = 10

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day:'2-digit', month:'short', year:'numeric' })
}

function Pagination({ current, total, onChange }) {
  if (total <= 1) return null
  return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:'0.4rem', marginTop:'1rem' }}>
      <button className="btn btn--ghost btn--sm" onClick={() => onChange(current - 1)} disabled={current === 0}>‹</button>
      {Array.from({ length: total }, (_, i) => (
        <button
          key={i}
          className={`btn btn--sm ${i === current ? 'btn--primary' : 'btn--ghost'}`}
          onClick={() => onChange(i)}
          style={{ minWidth:32 }}
        >{i + 1}</button>
      ))}
      <button className="btn btn--ghost btn--sm" onClick={() => onChange(current + 1)} disabled={current === total - 1}>›</button>
    </div>
  )
}

export default function AdminPage() {
  const navigate = useNavigate()
  const user     = sessionHelper.get()

  const [section,      setSection]      = useState('overview')
  const [stats,        setStats]        = useState(null)
  const [popularMovies,setPopularMovies] = useState([])
  const [users,        setUsers]        = useState({ content:[], totalPages:1, currentPage:0 })
  const [reviews,      setReviews]      = useState({ content:[], totalPages:1, currentPage:0 })
  const [userPage,     setUserPage]     = useState(0)
  const [reviewPage,   setReviewPage]   = useState(0)
  const [loading,      setLoading]      = useState(true)

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') { navigate('/home'); return }
    Promise.all([
      adminApi.stats(),
      adminApi.popularMovies(),
    ]).then(async ([s, p]) => {
      setStats(s.data)
      const movies = p.data || []

      // Para filmes sem poster, busca do TMDB no frontend
      const enriched = await Promise.all(movies.map(async movie => {
        if (movie.moviePoster) return movie
        try {
          const { data } = await movieService.buscarPorId(movie.movieId)
          return {
            ...movie,
            movieTitle: movie.movieTitle || data.title,
            moviePoster: data.poster,
          }
        } catch {
          return movie
        }
      }))

      setPopularMovies(enriched)
    }).finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (section === 'users') {
      adminApi.listarUsuarios(userPage, PAGE_SIZE).then(({ data }) => setUsers(data))
    }
  }, [section, userPage])

  useEffect(() => {
    if (section === 'reviews') {
      adminApi.listarReviews(reviewPage, PAGE_SIZE).then(({ data }) => setReviews(data))
    }
  }, [section, reviewPage])

  async function handleDeleteUser(id) {
    if (!confirm('Excluir este usuário?')) return
    try {
      await adminApi.deletarUsuario(id)
      adminApi.listarUsuarios(userPage, PAGE_SIZE).then(({ data }) => setUsers(data))
    } catch (err) { alert(err.response?.data?.message || 'Erro.') }
  }

  async function handleDeleteReview(id) {
    if (!confirm('Excluir esta review?')) return
    try {
      await adminApi.deletarReview(id)
      adminApi.listarReviews(reviewPage, PAGE_SIZE).then(({ data }) => setReviews(data))
    } catch (err) { alert(err.response?.data?.message || 'Erro.') }
  }

  if (loading) return (
    <div className="page-main" style={{ display:'flex', alignItems:'center', justifyContent:'center', paddingTop:'4rem' }}>
      <span className="spinner" />
    </div>
  )

  return (
    <div style={{ display:'flex', minHeight:'100vh', background:'var(--bg-base)' }}>
      <aside className="admin-sidebar">
        <div className="admin-logo" onClick={() => navigate('/home')} style={{ cursor:'pointer' }} title="Voltar ao site">
          <span>Cine</span>Score
          <div style={{ fontSize:'0.7rem', color:'var(--text-muted)', marginTop:'0.2rem' }}>Painel Admin</div>
        </div>
        {[
          { key:'overview', label:'📊 Visão geral' },
          { key:'users',    label:'👥 Usuários' },
          { key:'reviews',  label:'⭐ Reviews' },
        ].map(item => (
          <button key={item.key}
            className={`admin-nav-btn ${section === item.key ? 'active' : ''}`}
            onClick={() => setSection(item.key)}>
            {item.label}
          </button>
        ))}
        <button className="admin-nav-btn" onClick={() => navigate('/home')} style={{ marginTop:'auto' }}>
          ← Voltar ao site
        </button>
      </aside>

      <main style={{ flex:1, padding:'2rem', overflowY:'auto' }}>

        {/* VISÃO GERAL */}
        {section === 'overview' && (
          <div>
            <h2 className="admin-title">Visão Geral</h2>

            {/* Stats */}
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(180px,1fr))', gap:'1rem', marginBottom:'2.5rem' }}>
              {[
                { label:'Usuários', value: stats?.totalUsuarios ?? '—', icon:'👥' },
                { label:'Reviews',  value: stats?.totalReviews  ?? '—', icon:'⭐' },
              ].map(stat => (
                <div key={stat.label} className="card" style={{ padding:'1.2rem', textAlign:'center' }}>
                  <div style={{ fontSize:'1.8rem', marginBottom:'0.4rem' }}>{stat.icon}</div>
                  <div style={{ fontSize:'1.8rem', fontWeight:800, color:'var(--purple-bright)' }}>{stat.value}</div>
                  <div style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{stat.label}</div>
                </div>
              ))}
            </div>

            {/* Trending da semana */}
            <div>
              <div className="feed-header" style={{ marginBottom:'1rem' }}>
                <span className="feed-title">🔥 Filmes Mais Avaliados no CineScore</span>
              </div>

              {popularMovies.length === 0 && (
                <div style={{ color:'var(--text-muted)', fontSize:'0.85rem' }}>Nenhum filme avaliado ainda.</div>
              )}

              <div style={{ display:'grid', gridTemplateColumns:'repeat(5,1fr)', gap:'0.75rem', maxWidth:680 }}>
                {popularMovies.map(movie => (
                  <div
                    key={movie.movieId}
                    onClick={() => navigate(`/movies/${movie.movieId}`)}
                    style={{ cursor:'pointer', borderRadius:8, overflow:'hidden', background:'var(--bg-card)',
                      border:'1px solid var(--border)', transition:'transform 0.15s, border-color 0.15s' }}
                    onMouseEnter={e => { e.currentTarget.style.transform='translateY(-4px)'; e.currentTarget.style.borderColor='var(--purple-accent)' }}
                    onMouseLeave={e => { e.currentTarget.style.transform='translateY(0)'; e.currentTarget.style.borderColor='var(--border)' }}
                  >
                    {movie.poster
                      ? <img src={movie.moviePoster} alt={movie.movieTitle} style={{ width:'100%', aspectRatio:'2/3', objectFit:'cover', display:'block' }} />
                      : <div style={{ width:'100%', aspectRatio:'2/3', background:'var(--bg-elevated)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'2rem' }}>🎬</div>
                    }
                    <div style={{ padding:'0.5rem' }}>
                      <div style={{ fontSize:'0.72rem', fontWeight:600, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{movie.movieTitle}</div>
                      <div style={{ display:'flex', justifyContent:'space-between', marginTop:2 }}>
                        <span style={{ fontSize:'0.65rem', color:'var(--text-muted)' }}>{movie.reviewCount} reviews</span>
                        {movie.avgRating > 0 && <span style={{ fontSize:'0.65rem', color:'var(--green)' }}>★ {movie.avgRating?.toFixed(1)}</span>}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* USUÁRIOS */}
        {section === 'users' && (
          <div>
            <h2 className="admin-title">Usuários <span style={{ fontSize:'0.85rem', color:'var(--text-muted)', fontWeight:400 }}>({users.totalElements} total)</span></h2>
            <table className="admin-table">
              <thead><tr><th>ID</th><th>Nome</th><th>E-mail</th><th>Papel</th><th>Ações</th></tr></thead>
              <tbody>
                {users.content.map(u => (
                  <tr key={u.id}>
                    <td>{u.id}</td>
                    <td style={{ cursor:'pointer', color:'var(--purple-bright)' }} onClick={() => navigate(`/profile/${u.id}`)}>{u.name}</td>
                    <td>{u.email}</td>
                    <td><span style={{ background: u.role==='ADMIN' ? 'rgba(155,114,255,0.2)' : 'var(--bg-elevated)', padding:'0.15rem 0.5rem', borderRadius:4, fontSize:'0.75rem', color: u.role==='ADMIN' ? 'var(--purple-bright)' : 'var(--text-muted)' }}>{u.role}</span></td>
                    <td>{u.role !== 'ADMIN' && <button className="btn btn--sm" style={{ background:'var(--red)', color:'#fff', border:'none' }} onClick={() => handleDeleteUser(u.id)}>Excluir</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination current={userPage} total={users.totalPages} onChange={p => setUserPage(p)} />
          </div>
        )}

        {/* REVIEWS */}
        {section === 'reviews' && (
          <div>
            <h2 className="admin-title">Reviews <span style={{ fontSize:'0.85rem', color:'var(--text-muted)', fontWeight:400 }}>({reviews.totalElements} total)</span></h2>
            <table className="admin-table">
              <thead><tr><th>ID</th><th>Usuário</th><th>Filme</th><th>Nota</th><th>Data</th><th>Ações</th></tr></thead>
              <tbody>
                {reviews.content.map(r => (
                  <tr key={r.id}>
                    <td>{r.id}</td>
                    <td style={{ cursor:'pointer', color:'var(--purple-bright)' }} onClick={() => navigate(`/profile/${r.userId}`)}>{r.userName}</td>
                    <td style={{ maxWidth:200, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>{r.movieTitle || r.movieId}</td>
                    <td style={{ color:'var(--green)', fontWeight:600 }}>{r.rating}/5</td>
                    <td>{formatDate(r.createdAt)}</td>
                    <td><button className="btn btn--sm" style={{ background:'var(--red)', color:'#fff', border:'none' }} onClick={() => handleDeleteReview(r.id)}>Excluir</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination current={reviewPage} total={reviews.totalPages} onChange={p => setReviewPage(p)} />
          </div>
        )}
      </main>
    </div>
  )
}
