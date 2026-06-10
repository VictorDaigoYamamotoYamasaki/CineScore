import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import ReviewCard from '../components/ReviewCard'
import MovieSearchInput from '../components/MovieSearchInput'
import { reviewService, movieService, sessionHelper } from '../services/api'

export default function HomePage() {
  const navigate = useNavigate()
  const user     = sessionHelper.get()

  const [reviews,   setReviews]   = useState([])
  const [loading,   setLoading]   = useState(true)
  const [feedError, setFeedError] = useState(false)

  useEffect(() => {
    carregarFeed()
  }, [])

  async function carregarFeed() {
    setLoading(true)
    setFeedError(false)
    try {
      const { data } = await reviewService.listarTodos()
      const lista = (data || []).slice(0, 20)

      // IDs de filmes sem poster armazenado (deduplica para não chamar o mesmo filme 2x)
      const semPoster = [...new Set(
        lista
          .filter(r => !r.moviePoster && r.movieId)
          .map(r => r.movieId)
      )]

      // Busca todos os posters faltando em paralelo (com fallback silencioso)
      const posterMap = {}
      if (semPoster.length > 0) {
        const fetches = semPoster.map(id =>
          movieService.buscarPorId(id)
            .then(({ data: m }) => ({ id, poster: m.poster, title: m.title }))
            .catch(() => ({ id, poster: null, title: null }))
        )
        const resultados = await Promise.allSettled(fetches)
        resultados.forEach(r => {
          if (r.status === 'fulfilled' && r.value) {
            posterMap[r.value.id] = r.value
          }
        })
      }

      // Enriquece reviews com poster/título do TMDB onde faltava
      const enriquecidas = lista.map(review => ({
        ...review,
        moviePoster: review.moviePoster || posterMap[review.movieId]?.poster  || null,
        movieTitle:  review.movieTitle  || posterMap[review.movieId]?.title   || review.movieId,
      }))

      setReviews(enriquecidas)
    } catch {
      setFeedError(true)
    } finally {
      setLoading(false)
    }
  }

  function handleMovieSelect(movie) { navigate(`/movies/${movie.id}`) }
  function handleActorSelect(actor) { navigate(`/actors/${actor.id}`) }

  return (
    <>
      <Navbar user={user} />
      <div className="page-main">
        <div className="hero">
          <div className="container">
            <h1 className="hero-title">O que você <span>assistiu</span> hoje?</h1>
            <p className="hero-sub">Busque um filme e registre sua avaliação</p>
            <div style={{ maxWidth: 480 }}>
              <MovieSearchInput onSelect={handleMovieSelect} onActorSelect={handleActorSelect} />
            </div>
          </div>
        </div>

        <div className="container">
          <div className="feed">
            <div className="feed-header">
              <span className="feed-title">Atividade recente</span>
              {user && (
                <button className="btn btn--ghost btn--sm" onClick={() => navigate('/reviews/new')}>
                  + Nova review
                </button>
              )}
            </div>

            {loading && (
              <div className="empty-state">
                <div className="spinner" style={{ margin: '0 auto 1rem' }} />
                <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                  Carregando reviews...
                </p>
              </div>
            )}

            {!loading && feedError && (
              <div className="empty-state">
                <p>Não foi possível carregar o feed. Tente novamente.</p>
                <button className="btn btn--ghost btn--sm" style={{ marginTop: '0.75rem' }}
                  onClick={carregarFeed}>Recarregar</button>
              </div>
            )}

            {!loading && !feedError && reviews.length === 0 && (
              <div className="empty-state">
                <p>Nenhuma review ainda. Seja o primeiro!</p>
              </div>
            )}

            {!loading && !feedError && reviews.map((review, i) => (
              <ReviewCard
                key={review.id}
                review={review}
                movieTitle={review.movieTitle}
                moviePoster={review.moviePoster}
                onClick={() => navigate(`/movies/${review.movieId}`)}
                style={{ animationDelay: `${i * 0.05}s` }}
              />
            ))}
          </div>
        </div>
      </div>
    </>
  )
}
