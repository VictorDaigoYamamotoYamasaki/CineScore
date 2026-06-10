import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import MovieCard from '../components/MovieCard'
import Navbar from '../components/Navbar'
import { recommendationService, sessionHelper } from '../services/api'

export default function RecommendationPage() {
  const navigate = useNavigate()
  const user     = sessionHelper.get()

  const [recs,       setRecs]       = useState([])
  const [shownIds,   setShownIds]   = useState([])   // IDs já exibidos
  const [loading,    setLoading]    = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [empty,      setEmpty]      = useState(false)
  const [exhausted,  setExhausted]  = useState(false) // sem mais filmes novos

  const fetchRecs = useCallback(async (exclude, isRefresh = false) => {
    isRefresh ? setRefreshing(true) : setLoading(true)
    setEmpty(false)
    setExhausted(false)
    try {
      const { data } = await recommendationService.minhasRecomendacoes(exclude)
      if (!data || data.length === 0) {
        if (isRefresh) setExhausted(true)
        else setEmpty(true)
      } else {
        setRecs(data)
        // Acumula os IDs já exibidos para não repetir no próximo refresh
        const newIds = data.map(r => r.id)
        setShownIds(prev => [...new Set([...prev, ...newIds])])
      }
    } catch {
      setEmpty(true)
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    if (!user) { navigate('/login'); return }
    fetchRecs([])
  }, [])

  function handleRefresh() {
    fetchRecs(shownIds, true)
  }

  function handleReset() {
    setShownIds([])
    setExhausted(false)
    fetchRecs([])
  }

  // Agrupa por seção
  const genreSection  = recs.filter(r => r.reason?.startsWith('Baseado em'))
  const actorSections = recs
    .filter(r => r.reason?.startsWith('Com '))
    .reduce((acc, rec) => {
      if (!acc[rec.reason]) acc[rec.reason] = []
      acc[rec.reason].push(rec)
      return acc
    }, {})

  return (
    <>
      <Navbar user={user} />
      <div className="page-main">

        {/* Hero */}
        <div className="movie-hero" style={{ minHeight:'auto', paddingBottom:'1.5rem' }}>
          <div className="container">
            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', flexWrap:'wrap', gap:'1rem' }}>
              <div style={{ display:'flex', alignItems:'center', gap:'1rem' }}>
                <div style={{ fontSize:'2.5rem', lineHeight:1 }}></div>
                <div>
                  <h1 style={{ fontSize:'1.8rem', fontWeight:800, margin:0 }}>Para você</h1>
                  <p style={{ fontSize:'0.88rem', color:'var(--text-secondary)', margin:'0.3rem 0 0' }}>
                    Baseado nos seus filmes favoritos
                  </p>
                </div>
              </div>

              {!loading && !empty && !exhausted && (
                <button
                  onClick={handleRefresh}
                  disabled={refreshing}
                  className="btn btn--ghost btn--sm"
                  style={{ display:'flex', alignItems:'center', gap:'0.4rem', minWidth:120 }}
                >
                  {refreshing
                    ? <><span className="spinner" style={{ width:14, height:14 }} /> Buscando...</>
                    : <>🔄 Ver outros filmes</>}
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="container" style={{ paddingTop:'2rem', paddingBottom:'3rem' }}>

          {loading && (
            <div className="empty-state" style={{ paddingTop:'3rem' }}>
              <div className="spinner" style={{ margin:'0 auto 1rem', width:32, height:32 }} />
              <p style={{ color:'var(--text-muted)' }}>Analisando seus favoritos...</p>
            </div>
          )}

          {!loading && empty && (
            <div className="empty-state" style={{ paddingTop:'3rem' }}>
              <div className="empty-state-icon">🎬</div>
              <p style={{ marginBottom:'1rem' }}>
                Adicione filmes favoritos no seu perfil para receber recomendações personalizadas.
              </p>
              <button className="btn btn--primary btn--sm" onClick={() => navigate('/profile')}>
                Ir para meu perfil
              </button>
            </div>
          )}

          {/* Esgotou as recomendações disponíveis */}
          {!loading && exhausted && (
            <div className="empty-state" style={{ paddingTop:'2rem' }}>
              <div className="empty-state-icon">🎉</div>
              <p style={{ marginBottom:'0.5rem', fontWeight:600 }}>Você viu todas as recomendações disponíveis!</p>
              <p style={{ fontSize:'0.85rem', color:'var(--text-muted)', marginBottom:'1.2rem' }}>
                Adicione mais filmes favoritos ou recomece do início.
              </p>
              <button className="btn btn--primary btn--sm" onClick={handleReset}>
                🔄 Recomeçar
              </button>
            </div>
          )}

          {/* Recomendações */}
          {!loading && !empty && !exhausted && (
            <div style={{ opacity: refreshing ? 0.4 : 1, transition:'opacity 0.3s' }}>

              {genreSection.length > 0 && (
                <Section
                  title={genreSection[0].reason}
                  movies={genreSection}
                  />
              )}

              {Object.entries(actorSections).map(([reason, movies]) => (
                <Section key={reason} title={reason} movies={movies} />
              ))}

            </div>
          )}

        </div>
      </div>
    </>
  )
}

function Section({ title, movies }) {
  return (
    <div style={{ marginBottom:'2.5rem' }}>
      <div className="feed-header" style={{ marginBottom:'1.2rem' }}>
        <span className="feed-title" style={{ fontSize:'1rem' }}>{title}</span>
        <span style={{ fontSize:'0.75rem', color:'var(--text-muted)' }}>
          {movies.length} filme{movies.length !== 1 ? 's' : ''}
        </span>
      </div>

      <div style={{
        display:'grid',
        gridTemplateColumns:'repeat(auto-fill, minmax(130px, 1fr))',
        gap:'1rem',
      }}>
        {movies.map(movie => (
          <MovieCard key={movie.id} movie={{ ...movie, voteAverage: 0 }} />
        ))}
      </div>
    </div>
  )
}
