import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionHelper, movieService } from '../services/api'
import axios from 'axios'
import {
  BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts'
import MovieCard from '../components/MovieCard'

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
  ratingDist:     ()           => api.get('/admin/stats/rating-distribution'),
  reviewsPerDay:  ()           => api.get('/admin/stats/reviews-per-day'),
  genres:         ()           => api.get('/admin/stats/genres'),
}

const PAGE_SIZE = 10

const PIE_COLORS = ['#7C5CBF','#e040fb','#26c6da','#66bb6a','#ffa726','#ef5350','#ab47bc','#42a5f5']

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
  const [ratingDist,   setRatingDist]   = useState([])
  const [reviewsPerDay,setReviewsPerDay] = useState([])
  const [genres,       setGenres]       = useState([])

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') { navigate('/home'); return }
    Promise.all([
      adminApi.stats(),
      adminApi.popularMovies(),
      adminApi.ratingDist(),
      adminApi.reviewsPerDay(),
      adminApi.genres(),
    ]).then(async ([s, p, rd, rpd, g]) => {
      setRatingDist(rd.data || [])
      setReviewsPerDay(rpd.data || [])
      setGenres(g.data || [])
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


            {/* ── GRÁFICO 1: Distribuição de Notas ─────────────────────── */}
            <div className="card" style={{ padding:'1.5rem', marginBottom:'1.5rem' }}>
              <div style={{ fontWeight:700, fontSize:'0.95rem', marginBottom:'1.2rem' }}>
                📊 Distribuição de Notas
              </div>
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={ratingDist} margin={{ top:5, right:10, left:-10, bottom:5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis
                    dataKey="rating"
                    tick={{ fill:'var(--text-muted)', fontSize:11 }}
                    tickFormatter={v => `${v}★`}
                  />
                  <YAxis tick={{ fill:'var(--text-muted)', fontSize:11 }} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{ background:'var(--bg-elevated)', border:'1px solid var(--border)', borderRadius:8 }}
                    labelFormatter={v => `Nota ${v}★`}
                    formatter={v => [`${v} reviews`, '']}
                    cursor={{ fill:'rgba(124,92,191,0.1)' }}
                  />
                  <Bar dataKey="count" fill="var(--purple-accent)" radius={[4,4,0,0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* ── GRÁFICO 2: Reviews por Dia ────────────────────────────── */}
            <div className="card" style={{ padding:'1.5rem', marginBottom:'1.5rem' }}>
              <div style={{ fontWeight:700, fontSize:'0.95rem', marginBottom:'1.2rem' }}>
                📈 Reviews nos Últimos 7 Dias
              </div>
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={reviewsPerDay} margin={{ top:5, right:10, left:-10, bottom:5 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="date" tick={{ fill:'var(--text-muted)', fontSize:11 }} />
                  <YAxis tick={{ fill:'var(--text-muted)', fontSize:11 }} allowDecimals={false} />
                  <Tooltip
                    contentStyle={{ background:'var(--bg-elevated)', border:'1px solid var(--border)', borderRadius:8 }}
                    formatter={v => [`${v} reviews`, '']}
                    cursor={{ stroke:'var(--purple-accent)', strokeWidth:1 }}
                  />
                  <Line
                    type="monotone" dataKey="count"
                    stroke="var(--purple-bright)" strokeWidth={2.5}
                    dot={{ fill:'var(--purple-accent)', r:4 }}
                    activeDot={{ r:6, fill:'var(--purple-bright)' }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>

            {/* ── GRÁFICO 3: Gêneros Mais Avaliados ────────────────────── */}
            {genres.length > 0 && (
              <div className="card" style={{ padding:'1.5rem', marginBottom:'2rem' }}>
                <div style={{ fontWeight:700, fontSize:'0.95rem', marginBottom:'1.2rem' }}>
                  🎭 Gêneros Mais Avaliados
                </div>
                <div style={{ display:'flex', alignItems:'center', gap:'2rem', flexWrap:'wrap' }}>
                  <ResponsiveContainer width={260} height={260}>
                    <PieChart>
                      <Pie
                        data={genres} dataKey="count" nameKey="genre"
                        cx="50%" cy="50%" outerRadius={110}
                        strokeWidth={2} stroke="var(--bg-base)"
                      >
                        {genres.map((_, i) => (
                          <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{ background:'var(--bg-elevated)', border:'1px solid var(--border)', borderRadius:8 }}
                        formatter={(v, name) => [`${v} reviews`, name]}
                      />
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

            {/* Filmes Populares */}
            <div>
              <div className="feed-header" style={{ marginBottom:'1rem' }}>
                <span className="feed-title">🔥 Filmes Mais Avaliados no CineScore</span>
              </div>

              {popularMovies.length === 0 && (
                <div style={{ color:'var(--text-muted)', fontSize:'0.85rem' }}>Nenhum filme avaliado ainda.</div>
              )}

              <div style={{ display:'grid', gridTemplateColumns:'repeat(5,1fr)', gap:'0.75rem', maxWidth:680 }}>
                {popularMovies.map(movie => (
                  <MovieCard
                    key={movie.movieId}
                    movie={{
                      id:          movie.movieId,
                      title:       movie.movieTitle,
                      poster:      movie.moviePoster,
                      voteAverage: movie.avgRating,
                    }}
                    extraLabel={`${movie.reviewCount} review${movie.reviewCount !== 1 ? 's' : ''}`}
                  />
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
