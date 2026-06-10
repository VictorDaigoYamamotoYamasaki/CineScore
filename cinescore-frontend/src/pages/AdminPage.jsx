import { useEffect, useState } from 'react'
import { useTheme } from '../context/ThemeContext'
import { useNavigate } from 'react-router-dom'
import { sessionHelper, movieService } from '../services/api'
import axios from 'axios'
import {
  BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts'
import MovieCard from '../components/MovieCard'

const api = axios.create({ baseURL: '/api' })
api.interceptors.request.use(cfg => {
  const t = localStorage.getItem('cinescore_token')
  if (t) cfg.headers.Authorization = `Bearer ${t}`
  return cfg
})

const adminApi = {
  stats:          ()                    => api.get('/admin/stats'),
  popularMovies:  ()                    => api.get('/admin/popular-movies'),
  ratingExtremes: ()                    => api.get('/admin/stats/rating-extremes'),
  listarUsuarios: (page, size, sortDir) => api.get(`/admin/users?page=${page}&size=${size}&sortDir=${sortDir}`),
  deletarUsuario: id                    => api.delete(`/admin/users/${id}`),
  listarReviews:  (page, size, sortDir) => api.get(`/admin/reviews?page=${page}&size=${size}&sortDir=${sortDir}`),
  deletarReview:  id                    => api.delete(`/admin/reviews/${id}`),
  ratingDist:     ()                    => api.get('/admin/stats/rating-distribution'),
  reviewsPerDay:  ()                    => api.get('/admin/stats/reviews-per-day'),
  genres:         ()                    => api.get('/admin/stats/genres'),
}

const PAGE_SIZE    = 30
const PIE_COLORS   = ['#7C5CBF','#e040fb','#26c6da','#66bb6a','#ffa726','#ef5350','#ab47bc','#42a5f5']
const PAGES_VISIB  = 10

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day:'2-digit', month:'short', year:'numeric' })
}

function Pagination({ current, total, onChange }) {
  if (total <= 1) return null
  const bloco       = Math.floor(current / PAGES_VISIB)
  const inicioBloco = bloco * PAGES_VISIB
  const fimBloco    = Math.min(inicioBloco + PAGES_VISIB, total)
  const temMais     = fimBloco < total
  const temAnterior = bloco > 0
  return (
    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:'0.3rem', marginTop:'1.2rem', flexWrap:'wrap' }}>
      <button className="btn btn--ghost btn--sm" onClick={() => onChange(current - 1)} disabled={current === 0}>‹</button>
      {temAnterior && <button className="btn btn--ghost btn--sm" onClick={() => onChange(inicioBloco - 1)} style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>‹ anterior</button>}
      {Array.from({ length: fimBloco - inicioBloco }, (_, i) => {
        const p = inicioBloco + i
        return (
          <button key={p} onClick={() => onChange(p)} style={{
            minWidth:32, height:32, borderRadius:4, fontSize:'0.85rem', fontWeight: p===current ? 700 : 400,
            border: p===current ? '1px solid var(--purple-accent)' : '1px solid transparent',
            background: p===current ? 'rgba(124,92,191,0.2)' : 'transparent',
            color: p===current ? 'var(--purple-bright)' : 'var(--text-secondary)', cursor:'pointer',
          }}>{p + 1}</button>
        )
      })}
      {temMais && <button className="btn btn--ghost btn--sm" onClick={() => onChange(fimBloco)} style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>Mais ›</button>}
      <button className="btn btn--ghost btn--sm" onClick={() => onChange(current + 1)} disabled={current === total - 1}>›</button>
    </div>
  )
}

function SortToggle({ dir, onChange }) {
  return (
    <button
      onClick={() => onChange(dir === 'asc' ? 'desc' : 'asc')}
      title={dir === 'asc' ? 'Ordem crescente (clique para inverter)' : 'Ordem decrescente (clique para inverter)'}
      style={{ background:'none', border:'1px solid var(--border)', borderRadius:4, padding:'0.2rem 0.5rem',
        cursor:'pointer', color:'var(--text-muted)', fontSize:'0.8rem', marginLeft:'0.5rem' }}>
      {dir === 'asc' ? 'ID ↑' : 'ID ↓'}
    </button>
  )
}

export default function AdminPage() {
  const navigate = useNavigate()
  const user        = sessionHelper.get()
  const { theme, toggleTheme } = useTheme()

  const [section,       setSection]       = useState('overview')
  const [stats,         setStats]         = useState(null)
  const [popularMovies, setPopularMovies] = useState([])
  const [extremes,      setExtremes]      = useState(null)
  const [users,         setUsers]         = useState({ content:[], totalPages:1, currentPage:0 })
  const [reviews,       setReviews]       = useState({ content:[], totalPages:1, currentPage:0 })
  const [userPage,      setUserPage]      = useState(0)
  const [reviewPage,    setReviewPage]    = useState(0)
  const [userSortDir,   setUserSortDir]   = useState('asc')
  const [reviewSortDir, setReviewSortDir] = useState('asc')
  const [loading,       setLoading]       = useState(true)
  const [ratingDist,    setRatingDist]    = useState([])
  const [reviewsPerDay, setReviewsPerDay] = useState([])
  const [genres,        setGenres]        = useState([])

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') { navigate('/home'); return }
    Promise.all([
      adminApi.stats(), adminApi.popularMovies(), adminApi.ratingExtremes(),
      adminApi.ratingDist(), adminApi.reviewsPerDay(), adminApi.genres(),
    ]).then(async ([s, p, ext, rd, rpd, g]) => {
      setRatingDist(rd.data || [])
      setReviewsPerDay(rpd.data || [])
      setGenres(g.data || [])
      setStats(s.data)
      setExtremes(ext.data)

      const movies = p.data || []
      const enriched = await Promise.all(movies.map(async movie => {
        if (movie.moviePoster) return movie
        try {
          const { data } = await movieService.buscarPorId(movie.movieId)
          return { ...movie, movieTitle: movie.movieTitle || data.title, moviePoster: data.poster }
        } catch { return movie }
      }))
      setPopularMovies(enriched)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (section === 'users') {
      adminApi.listarUsuarios(userPage, PAGE_SIZE, userSortDir).then(({ data }) => setUsers(data)).catch(() => {})
    }
  }, [section, userPage, userSortDir])

  useEffect(() => {
    if (section === 'reviews') {
      adminApi.listarReviews(reviewPage, PAGE_SIZE, reviewSortDir).then(({ data }) => setReviews(data))
    }
  }, [section, reviewPage, reviewSortDir])

  async function handleDeleteUser(id) {
    if (!confirm('Excluir este usuário?')) return
    try {
      await adminApi.deletarUsuario(id)
      adminApi.listarUsuarios(userPage, PAGE_SIZE, userSortDir).then(({ data }) => setUsers(data)).catch(() => {})
    } catch (err) { alert(err.response?.data?.message || 'Erro.') }
  }

  async function handleDeleteReview(id) {
    if (!confirm('Excluir esta review?')) return
    try {
      await adminApi.deletarReview(id)
      adminApi.listarReviews(reviewPage, PAGE_SIZE, reviewSortDir).then(({ data }) => setReviews(data))
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
          { key:'overview', label:'Visão geral' },
          { key:'users',    label:'Usuários' },
          { key:'reviews',  label:'Reviews' },
        ].map(item => (
          <button key={item.key}
            className={`admin-nav-btn ${section === item.key ? 'active' : ''}`}
            onClick={() => setSection(item.key)}>
            {item.label}
          </button>
        ))}
        <div style={{ marginTop:'auto' }}>
          <div
            onClick={toggleTheme}
            style={{
              display:'flex', alignItems:'center', gap:'0.5rem', padding:'0.5rem 0.75rem',
              borderRadius:6, cursor:'pointer', transition:'background 0.12s',
            }}
            onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
          >
            <span style={{ fontSize:'0.84rem', color:'var(--text-secondary)', flex:1 }}>Modo claro</span>
            <div style={{
              width:32, height:18, borderRadius:9, flexShrink:0,
              background: theme === 'light' ? 'var(--purple-accent)' : 'var(--bg-elevated)',
              border:'1.5px solid var(--border)', position:'relative', transition:'background 0.2s',
            }}>
              <div style={{
                position:'absolute', top:2,
                left: theme === 'light' ? 13 : 2,
                width:10, height:10, borderRadius:'50%',
                background: theme === 'light' ? '#fff' : 'var(--text-muted)',
                transition:'left 0.2s',
              }} />
            </div>
          </div>
        </div>
        <button className="admin-nav-btn" onClick={() => navigate('/home')}>
          ← Voltar ao site
        </button>
      </aside>

      <main style={{ flex:1, padding:'2rem', overflowY:'auto' }}>

        {/* VISÃO GERAL */}
        {section === 'overview' && (
          <div>
            <h2 className="admin-title">Visão Geral</h2>

            {/* Stats cards */}
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(180px,1fr))', gap:'1rem', marginBottom:'2rem' }}>
              {[
                { label:'Usuários', value: stats?.totalUsuarios ?? '—', icon:'' },
                { label:'Reviews',  value: stats?.totalReviews  ?? '—', icon:'' },
              ].map(stat => (
                <div key={stat.label} className="card" style={{ padding:'1.2rem', textAlign:'center' }}>
                  <div style={{ fontSize:'1.8rem', marginBottom:'0.4rem' }}>{stat.icon}</div>
                  <div style={{ fontSize:'1.8rem', fontWeight:800, color:'var(--purple-bright)' }}>{stat.value}</div>
                  <div style={{ fontSize:'0.8rem', color:'var(--text-muted)' }}>{stat.label}</div>
                </div>
              ))}
            </div>

            {/* Extremos de nota */}
            {(extremes?.highest || extremes?.lowest) && (
              <div style={{ marginBottom:'2rem' }}>
                <div className="feed-header" style={{ marginBottom:'1rem' }}>
                  <span className="feed-title"> Extremos de Avaliação</span>
                </div>
                <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'1.5rem', maxWidth:340 }}>
                  {extremes?.highest && (
                    <div>
                      <div style={{ fontSize:'0.72rem', fontWeight:700, color:'var(--green)',
                        textTransform:'uppercase', letterSpacing:'0.05em', marginBottom:'0.4rem' }}>
                        Melhor avaliado
                      </div>
                      <MovieCard
                        movie={{
                          id:          extremes.highest.movieId,
                          title:       extremes.highest.movieTitle,
                          poster:      extremes.highest.moviePoster,
                          voteAverage: extremes.highest.avgRating,
                        }}
                        extraLabel={`${extremes.highest.reviewCount} review${extremes.highest.reviewCount !== 1 ? 's' : ''}`}
                      />
                    </div>
                  )}
                  {extremes?.lowest && (
                    <div>
                      <div style={{ fontSize:'0.72rem', fontWeight:700, color:'#ef5350',
                        textTransform:'uppercase', letterSpacing:'0.05em', marginBottom:'0.4rem' }}>
                        Pior avaliado
                      </div>
                      <MovieCard
                        movie={{
                          id:          extremes.lowest.movieId,
                          title:       extremes.lowest.movieTitle,
                          poster:      extremes.lowest.moviePoster,
                          voteAverage: extremes.lowest.avgRating,
                        }}
                        extraLabel={`${extremes.lowest.reviewCount} review${extremes.lowest.reviewCount !== 1 ? 's' : ''}`}
                      />
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Filmes mais avaliados */}
            <div>
              <div className="feed-header" style={{ marginBottom:'1rem' }}>
                <span className="feed-title">Filmes Mais Avaliados no CineScore</span>
              </div>
              {popularMovies.length === 0 && <div style={{ color:'var(--text-muted)', fontSize:'0.85rem' }}>Nenhum filme avaliado ainda.</div>}
              <div style={{ display:'grid', gridTemplateColumns:'repeat(5,1fr)', gap:'0.75rem', maxWidth:680 }}>
                {popularMovies.map(movie => (
                  <MovieCard key={movie.movieId} movie={{ id:movie.movieId, title:movie.movieTitle, poster:movie.moviePoster, voteAverage:movie.avgRating }}
                    extraLabel={`${movie.reviewCount} review${movie.reviewCount !== 1 ? 's' : ''}`} />
                ))}
              </div>
            </div>

            {/* Gráfico: Reviews por Dia */}
            <div className="card" style={{ padding:'1.5rem', marginBottom:'1.5rem' }}>
              <div style={{ fontWeight:700, fontSize:'0.95rem', marginBottom:'1.2rem' }}> Reviews nos Últimos 7 Dias</div>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={reviewsPerDay} margin={{ top:5, right:10, left:-10, bottom:5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="date" tick={{ fill:'var(--text-muted)', fontSize:11 }} />
                  <YAxis tick={{ fill:'var(--text-muted)', fontSize:11 }} allowDecimals={false} />
                  <Tooltip contentStyle={{ background:'var(--bg-elevated)', border:'1px solid var(--border)', borderRadius:8 }}
                    formatter={v => [`${v} reviews`, '']} cursor={{ stroke:'var(--purple-accent)', strokeWidth:1 }} />
                  <Line type="monotone" dataKey="count" stroke="var(--purple-bright)" strokeWidth={2.5}
                    dot={{ fill:'var(--purple-accent)', r:4 }} activeDot={{ r:6, fill:'var(--purple-bright)' }} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            {/* Gráfico: Distribuição de Notas */}
            <div className="card" style={{ padding:'1.5rem', marginBottom:'1.5rem' }}>
              <div style={{ fontWeight:700, fontSize:'0.95rem', marginBottom:'1.2rem' }}> Distribuição de Notas</div>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={ratingDist} margin={{ top:5, right:10, left:-10, bottom:5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="rating" tick={{ fill:'var(--text-muted)', fontSize:11 }} tickFormatter={v => `${v}★`} />
                  <YAxis tick={{ fill:'var(--text-muted)', fontSize:11 }} allowDecimals={false} />
                  <Tooltip contentStyle={{ background:'var(--bg-elevated)', border:'1px solid var(--border)', borderRadius:8 }}
                    labelFormatter={v => `Nota ${v}★`} formatter={v => [`${v} reviews`, '']}
                    cursor={{ fill:'rgba(124,92,191,0.1)' }} />
                  <Bar dataKey="count" fill="var(--purple-accent)" radius={[4,4,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* Gráfico: Gêneros */}
            {genres.length > 0 && (
              <div className="card" style={{ padding:'1.5rem', marginBottom:'2rem' }}>
                <div style={{ fontWeight:700, fontSize:'0.95rem', marginBottom:'1.2rem' }}> Gêneros Mais Avaliados</div>
                <div style={{ display:'flex', alignItems:'center', gap:'2rem', flexWrap:'wrap' }}>
                  <ResponsiveContainer width={260} height={260}>
                    <PieChart>
                      <Pie data={genres} dataKey="count" nameKey="genre" cx="50%" cy="50%" outerRadius={110} strokeWidth={2} stroke="var(--bg-base)">
                        {genres.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                      </Pie>
                      <Tooltip contentStyle={{ background:'var(--bg-elevated)', border:'1px solid var(--border)', borderRadius:8 }}
                        formatter={(v, name) => [`${v} reviews`, name]} />
                    </PieChart>
                  </ResponsiveContainer>
                  <div style={{ flex:1, minWidth:160 }}>
                    {genres.map((g, i) => (
                      <div key={g.genre} style={{ display:'flex', alignItems:'center', gap:'0.5rem', marginBottom:'0.5rem' }}>
                        <div style={{ width:10, height:10, borderRadius:2, background:PIE_COLORS[i % PIE_COLORS.length], flexShrink:0 }} />
                        <span style={{ fontSize:'0.8rem', color:'var(--text-secondary)', flex:1 }}>{g.genre}</span>
                        <span style={{ fontSize:'0.8rem', color:'var(--text-muted)', fontWeight:600 }}>{g.count}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* USUÁRIOS */}
        {section === 'users' && (
          <div>
            <h2 className="admin-title">
              Usuários
              <span style={{ fontSize:'0.85rem', color:'var(--text-muted)', fontWeight:400 }}> ({users.totalElements} total)</span>
              <SortToggle dir={userSortDir} onChange={d => { setUserSortDir(d); setUserPage(0) }} />
            </h2>
            <table className="admin-table">
              <thead><tr><th>ID</th><th>Nome</th><th>E-mail</th><th>Papel</th><th>Ações</th></tr></thead>
              <tbody>
                {users.content.map(u => (
                  <tr key={u.id}>
                    <td style={{ fontFamily:'monospace', fontSize:'0.78rem', color:'var(--text-muted)', letterSpacing:0 }} title={u.id}>{u.id ? u.id.substring(0,8) + '…' : '—'}</td>
                    <td style={{ cursor:'pointer', color:'var(--purple-bright)' }} onClick={() => navigate(`/profile/${u.id}`)}>{u.name}</td>
                    <td>{u.emailMascarado}</td>
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
            <h2 className="admin-title">
              Reviews
              <span style={{ fontSize:'0.85rem', color:'var(--text-muted)', fontWeight:400 }}> ({reviews.totalElements} total)</span>
              <SortToggle dir={reviewSortDir} onChange={d => { setReviewSortDir(d); setReviewPage(0) }} />
            </h2>
            <table className="admin-table">
              <thead><tr><th>ID</th><th>Usuário</th><th>Filme</th><th>Nota</th><th>Data</th><th>Ações</th></tr></thead>
              <tbody>
                {reviews.content.map(r => (
                  <tr key={r.id}>
                    <td style={{ fontFamily:'monospace', fontSize:'0.78rem', color:'var(--text-muted)', letterSpacing:0 }} title={r.id}>{r.id ? r.id.substring(0,8) + '…' : '—'}</td>
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
