import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { StarDisplay } from '../components/StarRating'
import ReviewCard from '../components/ReviewCard'
import DiaryView      from '../components/DiaryView'
import WatchlistGrid  from '../components/WatchlistGrid'
import PosterFrame from '../components/PosterFrame'
import ProfileSettingsModal from '../components/ProfileSettingsModal'
import { profileService, movieService, followerService, recommendationService, watchlistService, sessionHelper } from '../services/api'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

// Modal de busca de filme para adicionar ao favorito
function MoviePickerModal({ position, onSelect, onClose }) {
  const [query,   setQuery]   = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const timerRef = useState(null)

  useEffect(() => {
    const q = query.trim()
    if (!q || q.length < 3) { setResults([]); return }
    clearTimeout(timerRef[0])
    timerRef[0] = setTimeout(async () => {
      setLoading(true)
      try {
        const { data } = await movieService.buscarPorTitulo(q)
        setResults(Array.isArray(data) ? data.slice(0, 6) : [])
      } catch { setResults([]) }
      finally { setLoading(false) }
    }, 400)
    return () => clearTimeout(timerRef[0])
  }, [query])

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.75)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }} onClick={onClose}>
      <div style={{
        background: 'var(--bg-card)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: '1.5rem',
        width: '100%', maxWidth: 480,
        maxHeight: '80vh', overflowY: 'auto',
      }} onClick={e => e.stopPropagation()}>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1rem' }}>
          <h3 style={{ fontSize:'1rem', fontWeight:700 }}>Escolher filme favorito #{position}</h3>
          <button onClick={onClose} style={{ background:'none', border:'none', color:'var(--text-secondary)', fontSize:'1.2rem', cursor:'pointer' }}>✕</button>
        </div>

        <input
          autoFocus
          type="text"
          placeholder="Digite o nome do filme..."
          value={query}
          onChange={e => setQuery(e.target.value)}
          style={{ width:'100%', marginBottom:'1rem' }}
        />

        {loading && <div style={{ textAlign:'center', color:'var(--text-muted)', fontSize:'0.85rem' }}>Buscando...</div>}

        {results.map(movie => (
          <div
            key={movie.id}
            onClick={() => onSelect(position, movie)}
            style={{
              display:'flex', alignItems:'center', gap:'0.75rem',
              padding:'0.6rem', borderRadius:8, cursor:'pointer',
              marginBottom:'0.4rem',
              transition:'background 0.15s',
            }}
            onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-surface)'}
            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
          >
            {movie.poster
              ? <img src={movie.poster} alt={movie.title} style={{ width:40, height:56, objectFit:'cover', borderRadius:4, flexShrink:0 }} />
              : <div style={{ width:40, height:56, background:'var(--bg-elevated)', borderRadius:4, display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>🎬</div>
            }
            <div>
              <div style={{ fontSize:'0.88rem', fontWeight:600 }}>{movie.title}</div>
              <div style={{ fontSize:'0.75rem', color:'var(--text-muted)' }}>{movie.year}</div>
            </div>
          </div>
        ))}

        {!loading && query.length >= 3 && results.length === 0 && (
          <div style={{ textAlign:'center', color:'var(--text-muted)', fontSize:'0.85rem' }}>Nenhum filme encontrado</div>
        )}
      </div>
    </div>
  )
}

export default function ProfilePage() {
  const { userId: profileUserId } = useParams()
  const navigate   = useNavigate()
  const currentUser = sessionHelper.get()

  const [profile,  setProfile]  = useState(null)
  const [activeTab,       setActiveTab]       = useState('reviews')
  const [showWatchlist,   setShowWatchlist]   = useState(false)
  const [watchlistItems,  setWatchlistItems]  = useState([])
  const [watchlistLoaded, setWatchlistLoaded] = useState(false)
  const [loading,  setLoading]  = useState(true)
  const [picker,   setPicker]   = useState(null)   // posição do slot aberto
  const [movieMap, setMovieMap] = useState({})     // movieId → {title, poster, year}
  const [saving,   setSaving]   = useState(false)
  const [following, setFollowing] = useState(false)
  const [followLoading, setFollowLoading] = useState(false)
  const [recs,        setRecs]        = useState([])
  const [recsLoaded,  setRecsLoaded]  = useState(false)
  const [showSettings, setShowSettings] = useState(false)
  const [deleteModal, setDeleteModal] = useState(false)  // false | 'choose' | 'confirm'
  const [deleteOption, setDeleteOption] = useState(null) // 'account' | 'all'
  const [deleteLoading, setDeleteLoading] = useState(false)

  const targetId  = profileUserId || currentUser?.id
  const isOwn     = !profileUserId || profileUserId === currentUser?.id

  useEffect(() => {
    if (!currentUser && !profileUserId) { navigate('/login'); return }
    profileService.perfilPorId(targetId)
      .then(async ({ data }) => {
        setProfile(data)
        setFollowing(data.following)
        if (!profileUserId || profileUserId === currentUser?.id) {
          recommendationService.minhasRecomendacoes([])
            .then(r => setRecs(r.data || []))
            .catch(() => {})
            .finally(() => setRecsLoaded(true))
        }
        // Buscar dados TMDB dos favoritos que têm movieId
        const favIds = data.favorites.filter(f => f.movieId).map(f => f.movieId)
        if (favIds.length > 0) {
          const results = await Promise.allSettled(favIds.map(id => movieService.buscarPorId(id)))
          const map = {}
          results.forEach((res, i) => {
            if (res.status === 'fulfilled') {
              const m = res.value.data
              map[favIds[i]] = { title: m.title, poster: m.poster, year: m.year }
            }
          })
          setMovieMap(map)
        }

        // Prefetch de posters das reviews sem poster (para DiaryView e ReviewCards)
        const semPoster = [...new Set(
          (data.reviews || [])
            .filter(r => !r.moviePoster && r.movieId)
            .map(r => r.movieId)
        )]
        if (semPoster.length > 0) {
          const posterResults = await Promise.allSettled(
            semPoster.map(id =>
              movieService.buscarPorId(id)
                .then(({ data: m }) => ({ id, poster: m.poster, title: m.title }))
                .catch(() => ({ id, poster: null, title: null }))
            )
          )
          const posterMap = {}
          posterResults.forEach(r => {
            if (r.status === 'fulfilled' && r.value?.poster) {
              posterMap[r.value.id] = r.value
            }
          })
          if (Object.keys(posterMap).length > 0) {
            setProfile(prev => ({
              ...prev,
              reviews: (prev.reviews || []).map(r => ({
                ...r,
                moviePoster: r.moviePoster || posterMap[r.movieId]?.poster || null,
                movieTitle:  r.movieTitle  || posterMap[r.movieId]?.title  || r.movieId,
              }))
            }))
          }
        }
      })
      .catch(() => navigate('/home'))
      .finally(() => setLoading(false))
  }, [targetId])

  async function handleToggleWatchlist() {
    setShowWatchlist(v => !v)
    if (!watchlistLoaded) {
      try {
        const carregarWatchlist = isOwn
          ? watchlistService.listar()
          : watchlistService.listarPorUsuario(profile?.userId)
        const { data } = await carregarWatchlist
        setWatchlistItems(data || [])
        setWatchlistLoaded(true)
      } catch { setWatchlistItems([]) }
    }
  }

  function handleReviewUpdate(updated) {
    setProfile(prev => ({
      ...prev,
      reviews: prev.reviews.map(r => r.id === updated.id ? { ...r, ...updated } : r)
    }))
  }

    async function handleSelectMovie(position, movie) {
    setSaving(true)
    try {
      await profileService.salvarFavorito(position, String(movie.id), movie.title, movie.poster || '', movie.year || '')
      setMovieMap(prev => ({ ...prev, [String(movie.id)]: { title: movie.title, poster: movie.poster, year: movie.year } }))
      setProfile(prev => ({
        ...prev,
        favorites: prev.favorites.map(f =>
          f.position === position ? { ...f, movieId: String(movie.id) } : f
        )
      }))
    } catch (e) {
      alert(e.response?.data?.message || 'Erro ao salvar favorito.')
    } finally {
      setSaving(false)
      setPicker(null)
    }
  }

  async function handleRemoveFavorite(position) {
    try {
      await profileService.removerFavorito(position)
      setProfile(prev => ({
        ...prev,
        favorites: prev.favorites.map(f =>
          f.position === position ? { ...f, movieId: null } : f
        )
      }))
    } catch (e) {
      alert(e.response?.data?.message || 'Erro ao remover favorito.')
    }
  }

  async function handleToggleFollow() {
    if (!currentUser) {
      navigate('/login', { state: { from: window.location.pathname } })
      return
    }
    setFollowLoading(true)
    try {
      if (following) {
        await followerService.unfollow(profile?.userId)
        setFollowing(false)
        setProfile(prev => ({ ...prev, followerCount: (prev.followerCount || 1) - 1 }))
      } else {
        await followerService.follow(profile?.userId)
        setFollowing(true)
        setProfile(prev => ({ ...prev, followerCount: (prev.followerCount || 0) + 1 }))
      }
    } catch (e) {
      alert(e.response?.data?.message || 'Erro.')
    } finally {
      setFollowLoading(false)
    }
  }

  async function handleDeleteAccount() {
    setDeleteLoading(true)
    try {
      await profileService.deletarConta(deleteOption === 'all')
      sessionHelper.clear()
      navigate('/login')
    } catch (e) {
      alert(e.response?.data?.message || 'Erro ao excluir conta.')
      setDeleteLoading(false)
      setDeleteModal(false)
    }
  }

  if (loading) return (
    <>
      <Navbar user={currentUser} />
      <div className="page-main" style={{ display:'flex', alignItems:'center', justifyContent:'center', paddingTop:80 }}>
        <span className="spinner" />
      </div>
    </>
  )

  return (
    <>
      <Navbar user={currentUser} />

      {showSettings && profile && (
        <ProfileSettingsModal
          user={{ name: profile.name, email: profile.email }}
          onClose={() => setShowSettings(false)}
          onUpdated={updated => {
            setProfile(prev => ({ ...prev, name: updated.name, email: updated.email }))
            setShowSettings(false)
          }}
        />
      )}

      {picker && (
        <MoviePickerModal
          position={picker}
          onSelect={handleSelectMovie}
          onClose={() => setPicker(null)}
        />
      )}

      <div className="page-main">

        {/* Hero do perfil */}
        <div className="movie-hero">
          <div className="container">
            <div style={{ display:'flex', alignItems:'center', gap:'1.5rem', flexWrap:'wrap', width:'100%' }}>
              {/* Avatar */}
              <div style={{
                width: 80, height: 80, borderRadius:'50%',
                background: 'linear-gradient(135deg, var(--purple-mid), var(--purple-accent))',
                display:'flex', alignItems:'center', justifyContent:'center',
                fontSize:'1.8rem', fontWeight:700, color:'#fff', flexShrink:0,
              }}>
                {initials(profile?.name)}
              </div>

              {/* Info + follow button */}
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ display:'flex', alignItems:'center', gap:'1rem', flexWrap:'wrap' }}>
                  <h1 style={{ fontSize:'1.6rem', fontWeight:700, margin:0 }}>{profile?.name}</h1>
                  {isOwn && currentUser && (
                    <button
                      onClick={() => setShowSettings(true)}
                      title="Editar perfil"
                      style={{
                        background:'none', border:'1px solid var(--border)',
                        borderRadius:999, padding:'0.2rem 0.6rem',
                        cursor:'pointer', color:'var(--text-muted)',
                        fontSize:'0.75rem', transition:'all 0.15s',
                      }}
                      onMouseEnter={e => { e.currentTarget.style.borderColor='var(--purple-accent)'; e.currentTarget.style.color='var(--purple-bright)' }}
                      onMouseLeave={e => { e.currentTarget.style.borderColor='var(--border)'; e.currentTarget.style.color='var(--text-muted)' }}
                    >✏️ Editar</button>
                  )}
                  {!isOwn && profile?.name !== 'Usuário Deletado' && (
                    <button
                      className={`btn btn--sm ${following ? 'btn--ghost' : 'btn--primary'}`}
                      onClick={handleToggleFollow}
                      disabled={followLoading}
                      style={{ flexShrink:0 }}
                    >
                      {followLoading
                        ? <span className="spinner" />
                        : following ? '✓ Seguindo' : '+ Seguir'}
                    </button>
                  )}
                </div>

              </div>

              {/* Contadores — lado direito */}
              <div style={{ display:'flex', gap:'2rem', flexShrink:0 }}>
                <div style={{ textAlign:'center', cursor:'pointer' }} onClick={() => navigate(`/profile/${targetId}/network?tab=followers`)}>
                  <div style={{ fontSize:'1.3rem', fontWeight:700, color:'var(--text-primary)' }}>
                    {profile?.followerCount || 0}
                  </div>
                  <div style={{ fontSize:'0.72rem', color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.08em' }}>
                    Seguidores
                  </div>
                </div>
                <div style={{ textAlign:'center', cursor:'pointer' }} onClick={() => navigate(`/profile/${targetId}/network?tab=following`)}>
                  <div style={{ fontSize:'1.3rem', fontWeight:700, color:'var(--text-primary)' }}>
                    {profile?.followingCount || 0}
                  </div>
                  <div style={{ fontSize:'0.72rem', color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.08em' }}>
                    Seguindo
                  </div>
                </div>
                <div style={{ textAlign:'center' }}>
                  <div style={{ fontSize:'1.3rem', fontWeight:700, color:'var(--text-primary)' }}>
                    {profile?.reviews?.length || 0}
                  </div>
                  <div style={{ fontSize:'0.72rem', color:'var(--text-muted)', textTransform:'uppercase', letterSpacing:'0.08em' }}>
                    Reviews
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="container" style={{ paddingTop:'2rem', paddingBottom:'3rem' }}>

          {/* Filmes Favoritos */}
          <div style={{ marginBottom:'3rem' }}>
            <div className="feed-header" style={{ marginBottom:'1.2rem' }}>
              <span className="feed-title">Filmes Favoritos</span>
              {isOwn && (
                <span style={{ fontSize:'0.75rem', color:'var(--text-muted)' }}>
                  Clique em + para adicionar
                </span>
              )}
            </div>

            <div style={{
              display:'grid',
              gridTemplateColumns:'repeat(5, 1fr)',
              gap:'0.75rem',
            }}>
              {profile?.favorites?.map(fav => {
                const info = fav.movieId ? movieMap[fav.movieId] : null
                return (
                  <div key={fav.position} style={{ position:'relative' }}>
                    {fav.movieId && info ? (
                      // Slot preenchido
                      <div style={{ position:'relative' }}>
                        <PosterFrame
                          src={info.poster}
                          alt={info.title}
                          onClick={() => navigate(`/movies/${fav.movieId}`)}
                          style={{ transition:'transform 0.15s, border-color 0.15s' }}
                        >
                          {/* Botão remover (só para o dono) */}
                          {isOwn && (
                            <button
                              onClick={e => { e.stopPropagation(); handleRemoveFavorite(fav.position) }}
                              title="Remover"
                              style={{
                                position:'absolute', top:4, right:4, zIndex:2,
                                background:'rgba(0,0,0,0.7)', border:'none',
                                color:'#fff', borderRadius:'50%',
                                width:22, height:22, fontSize:'0.7rem',
                                cursor:'pointer', display:'flex',
                                alignItems:'center', justifyContent:'center',
                              }}
                            >✕</button>
                          )}
                          {/* Botão trocar (só para o dono) */}
                          {isOwn && (
                            <button
                              onClick={e => { e.stopPropagation(); setPicker(fav.position) }}
                              title="Trocar filme"
                              style={{
                                position:'absolute', bottom:4, right:4, zIndex:2,
                                background:'rgba(124,92,191,0.85)', border:'none',
                                color:'#fff', borderRadius:4,
                                padding:'2px 6px', fontSize:'0.62rem',
                                cursor:'pointer',
                              }}
                            >trocar</button>
                          )}
                        </PosterFrame>
                        <div style={{ marginTop:'0.4rem', fontSize:'0.72rem', color:'var(--text-secondary)', textAlign:'center', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
                          {info.title}
                        </div>
                      </div>
                    ) : (
                      // Slot vazio
                      <div>
                        <div
                          onClick={() => isOwn && setPicker(fav.position)}
                          style={{
                            aspectRatio:'2/3',
                            borderRadius:8,
                            border:`2px dashed ${isOwn ? 'var(--purple-accent)' : 'var(--border)'}`,
                            display:'flex', flexDirection:'column',
                            alignItems:'center', justifyContent:'center',
                            cursor: isOwn ? 'pointer' : 'default',
                            background:'var(--bg-card)',
                            transition:'background 0.15s, border-color 0.15s',
                            color: isOwn ? 'var(--purple-bright)' : 'var(--text-muted)',
                          }}
                          onMouseEnter={e => { if(isOwn) e.currentTarget.style.background = 'var(--bg-surface)' }}
                          onMouseLeave={e => { if(isOwn) e.currentTarget.style.background = 'var(--bg-card)' }}
                        >
                          {isOwn && <span style={{ fontSize:'1.8rem', lineHeight:1 }}>+</span>}
                          {isOwn && <span style={{ fontSize:'0.65rem', marginTop:'0.3rem', color:'var(--text-muted)' }}>Favorito {fav.position}</span>}
                          {!isOwn && <span style={{ fontSize:'1.5rem' }}>🎬</span>}
                        </div>
                        <div style={{ marginTop:'0.4rem', fontSize:'0.72rem', color:'var(--text-muted)', textAlign:'center' }}>
                          {isOwn ? 'Adicionar' : 'Vazio'}
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </div>

          {/* Abas Avaliações / Diário */}
          <div style={{ marginBottom:'5rem' }}>
            {/* Tab bar */}
            <div style={{ display:'flex', gap:'0.25rem', marginBottom:'1.5rem',
              borderBottom:'1px solid var(--border)', paddingBottom:'0' }}>
              {[
                { key:'reviews', label:'Avaliações' },
                { key:'diary',   label:'Diário'     },
              ].map(tab => (
                <button key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  style={{
                    padding:'0.5rem 1.1rem', fontSize:'0.85rem', fontWeight:600,
                    border:'none', background:'none', cursor:'pointer',
                    color: activeTab === tab.key ? 'var(--purple-bright)' : 'var(--text-muted)',
                    borderBottom: activeTab === tab.key
                      ? '2px solid var(--purple-bright)' : '2px solid transparent',
                    marginBottom:'-1px', transition:'color 0.15s',
                  }}>
                  {tab.label}
                  <span style={{ marginLeft:'0.4rem', fontSize:'0.72rem' }}>
                    {profile?.reviews?.length || 0}
                  </span>
                </button>
              ))}
            </div>

            {/* Aba: Avaliações (cards) */}
            {activeTab === 'reviews' && (
              <div style={{ display:'flex', flexDirection:'column', gap:'0.5rem' }}>
                {(!profile?.reviews || profile.reviews.length === 0) && (
                  <div className="empty-state">
                    <p>Nenhuma avaliação ainda.</p>
                  </div>
                )}
                {profile?.reviews?.map(review => (
                  <ReviewCard
                    key={review.id}
                    review={review}
                    movieTitle={review.movieTitle || review.movieId}
                    moviePoster={review.moviePoster}
                    onClick={() => navigate(`/movies/${review.movieId}`)}
                    isOwn={isOwn}
                    onUpdate={updated => setProfile(prev => ({
                      ...prev,
                      reviews: prev.reviews.map(r => r.id === updated.id ? { ...r, ...updated } : r)
                    }))}
                    onDelete={id => setProfile(prev => ({
                      ...prev,
                      reviews: prev.reviews.filter(r => r.id !== id)
                    }))}
                  />
                ))}
              </div>
            )}

            {/* Aba: Diário */}
            {activeTab === 'diary' && (
              <DiaryView
                reviews={profile?.reviews || []}
                isOwn={isOwn}
                onReviewUpdate={handleReviewUpdate}
              />
            )}
          </div>



          {/* Watchlist — só no próprio perfil */}
          <div style={{ marginBottom:'5rem' }}>
              <div className="feed-header" style={{ marginBottom: showWatchlist ? '1.2rem' : 0 }}>
                <span className="feed-title" style={{ cursor:'pointer' }}
                  onClick={handleToggleWatchlist}>
                  Watchlist
                  {watchlistLoaded && (
                    <span style={{ marginLeft:'0.5rem', fontSize:'0.72rem',
                      color:'var(--text-muted)', fontWeight:400 }}>
                      {watchlistItems.length}
                    </span>
                  )}
                </span>
                <button
                  className="btn btn--ghost btn--sm"
                  onClick={handleToggleWatchlist}
                  style={{ fontSize:'0.75rem' }}
                >
                  {showWatchlist ? 'Fechar ▲' : 'Ver ▼'}
                </button>
              </div>

              {showWatchlist && (
                <WatchlistGrid
                  items={watchlistItems}
                  isOwn={isOwn}
                  onAdd={item => setWatchlistItems(prev => [item, ...prev])}
                  onRemove={movieId => setWatchlistItems(prev => prev.filter(i => i.movieId !== movieId))}
                />
              )}
          </div>

          {/* Recomendações — só no próprio perfil */}
          {isOwn && (
          <div style={{ marginBottom:'5rem' }}>
              <div className="feed-header" style={{ marginBottom:'1.2rem' }}>
                <span className="feed-title">Para você</span>
                <button
                  className="btn btn--ghost btn--sm"
                  onClick={() => navigate('/recommendations')}
                  style={{ fontSize:'0.75rem' }}
                >
                  Ver todas →
                </button>
              </div>

              {!recsLoaded && (
                <div style={{ color:'var(--text-muted)', fontSize:'0.85rem' }}>Carregando...</div>
              )}

              {recsLoaded && recs.length === 0 && (
                <div style={{ fontSize:'0.85rem', color:'var(--text-muted)' }}>
                  Adicione filmes favoritos acima para receber recomendações.
                </div>
              )}

              {recsLoaded && recs.length > 0 && (
                <div style={{
                  display:'grid',
                  gridTemplateColumns:'repeat(auto-fill, minmax(100px, 1fr))',
                  gap:'0.75rem',
                }}>
                  {recs.slice(0, 6).map(movie => (
                    <div
                      key={movie.id}
                      onClick={() => navigate(`/movies/${movie.id}`)}
                      style={{
                        cursor:'pointer', borderRadius:7, overflow:'hidden',
                        background:'var(--bg-card)', border:'1px solid var(--border)',
                        transition:'transform 0.15s, border-color 0.15s',
                      }}
                      onMouseEnter={e => { e.currentTarget.style.transform='translateY(-4px)'; e.currentTarget.style.borderColor='var(--purple-accent)' }}
                      onMouseLeave={e => { e.currentTarget.style.transform='translateY(0)'; e.currentTarget.style.borderColor='var(--border)' }}
                    >
                      {movie.poster
                        ? <img src={movie.poster} alt={movie.title} style={{ width:'100%', aspectRatio:'2/3', objectFit:'cover', display:'block' }} onError={e => e.target.style.display='none'} />
                        : <div style={{ width:'100%', aspectRatio:'2/3', background:'var(--bg-elevated)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:'1.5rem' }}>🎬</div>
                      }
                      <div style={{ padding:'0.4rem 0.4rem 0.5rem', fontSize:'0.68rem', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap', color:'var(--text-secondary)' }}>
                        {movie.title}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Zona de Perigo — só para o próprio usuário e nunca para admin */}
          {isOwn && currentUser?.role !== 'ADMIN' && (
            <div style={{
              marginTop: '3rem',
              padding: '1.25rem 1.5rem',
              borderRadius: 10,
              border: '1px solid var(--red)',
              background: 'rgba(224,92,92,0.05)',
            }}>
              <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--red)', textTransform: 'uppercase', letterSpacing: '0.1em', marginBottom: '0.5rem' }}>
                ⚠ Zona de Perigo
              </div>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', margin: '0 0 1rem' }}>
                A exclusão de conta é permanente e não pode ser desfeita. Conforme a LGPD, você pode escolher o que acontece com suas avaliações.
              </p>
              <button
                className="btn btn--sm"
                style={{ background: 'var(--red)', color: '#fff', border: 'none' }}
                onClick={() => setDeleteModal('choose')}
              >
                Excluir minha conta
              </button>
            </div>
          )}

          {/* Modal de exclusão */}
          {deleteModal && (
            <div style={{
              position: 'fixed', inset: 0, zIndex: 1000,
              background: 'rgba(0,0,0,0.75)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              padding: '1rem',
            }}>
              <div style={{
                background: 'var(--bg-card)',
                border: '1px solid var(--red)',
                borderRadius: 12,
                padding: '2rem',
                width: '100%', maxWidth: 440,
              }}>
                {deleteModal === 'choose' && (
                  <>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem' }}>Excluir conta</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                      O que você quer fazer com suas avaliações?
                    </p>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.5rem' }}>
                      <div
                        onClick={() => setDeleteOption('account')}
                        style={{
                          padding: '1rem',
                          borderRadius: 8,
                          border: `2px solid ${deleteOption === 'account' ? 'var(--purple-accent)' : 'var(--border)'}`,
                          cursor: 'pointer',
                          background: deleteOption === 'account' ? 'rgba(124,92,191,0.1)' : 'var(--bg-surface)',
                          transition: 'all 0.15s',
                        }}
                      >
                        <div style={{ fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.25rem' }}>
                          🗃 Excluir somente a conta
                        </div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                          Seus dados pessoais são removidos, mas suas avaliações ficam como "Usuário Deletado"
                        </div>
                      </div>

                      <div
                        onClick={() => setDeleteOption('all')}
                        style={{
                          padding: '1rem',
                          borderRadius: 8,
                          border: `2px solid ${deleteOption === 'all' ? 'var(--red)' : 'var(--border)'}`,
                          cursor: 'pointer',
                          background: deleteOption === 'all' ? 'rgba(224,92,92,0.08)' : 'var(--bg-surface)',
                          transition: 'all 0.15s',
                        }}
                      >
                        <div style={{ fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.25rem' }}>
                          🗑 Excluir conta e todas as avaliações
                        </div>
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                          Tudo é removido permanentemente, incluindo todas as suas avaliações
                        </div>
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                      <button className="btn btn--ghost btn--sm" onClick={() => { setDeleteModal(false); setDeleteOption(null) }}>
                        Cancelar
                      </button>
                      <button
                        className="btn btn--sm"
                        style={{ background: 'var(--red)', color: '#fff', border: 'none', opacity: deleteOption ? 1 : 0.5 }}
                        disabled={!deleteOption}
                        onClick={() => deleteOption && setDeleteModal('confirm')}
                      >
                        Continuar
                      </button>
                    </div>
                  </>
                )}

                {deleteModal === 'confirm' && (
                  <>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--red)' }}>
                      Tem certeza absoluta?
                    </h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                      {deleteOption === 'all'
                        ? 'Sua conta e TODAS as suas avaliações serão excluídas permanentemente.'
                        : 'Seus dados pessoais serão removidos. Suas avaliações ficarão como "Usuário Deletado".'}
                    </p>
                    <p style={{ fontSize: '0.85rem', color: 'var(--red)', marginBottom: '1.5rem', fontWeight: 600 }}>
                      Esta ação não pode ser desfeita.
                    </p>
                    <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                      <button className="btn btn--ghost btn--sm" onClick={() => setDeleteModal('choose')} disabled={deleteLoading}>
                        Voltar
                      </button>
                      <button
                        className="btn btn--sm"
                        style={{ background: 'var(--red)', color: '#fff', border: 'none' }}
                        onClick={handleDeleteAccount}
                        disabled={deleteLoading}
                      >
                        {deleteLoading ? <span className="spinner" /> : 'Sim, excluir minha conta'}
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

        </div>
      </div>
    </>
  )
}
