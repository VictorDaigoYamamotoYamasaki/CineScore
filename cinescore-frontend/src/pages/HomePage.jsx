import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import ReviewCard from '../components/ReviewCard'
import MovieSearchInput from '../components/MovieSearchInput'
import { reviewService, sessionHelper } from '../services/api'

export default function HomePage() {
  const navigate = useNavigate()
  const user     = sessionHelper.get()

  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    reviewService.listarTodos()
      .then(({ data }) => setReviews(data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  function handleMovieSelect(movie) { navigate(`/movies/${movie.id}`) }
  function handleActorSelect(actor) { navigate(`/actors/${actor.id}`) }

  if (!user) { navigate('/login'); return null }

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
              <span className="feed-title">⚡ Atividade recente</span>
              <button className="btn btn--ghost btn--sm" onClick={() => navigate('/reviews/new')}>
                + Nova review
              </button>
            </div>

            {loading && (
              <div className="empty-state">
                <div className="spinner" style={{ margin: '0 auto' }} />
              </div>
            )}

            {!loading && reviews.length === 0 && (
              <div className="empty-state">
                <div className="empty-state-icon">🎬</div>
                <p>Nenhuma review ainda. Seja o primeiro!</p>
              </div>
            )}

            {!loading && reviews.map((review, i) => (
              <ReviewCard
                key={review.id}
                review={review}
                movieTitle={review.movieTitle || review.movieId}
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
