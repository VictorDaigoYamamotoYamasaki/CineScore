import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import MovieSearchInput from '../components/MovieSearchInput'
import { StarRating } from '../components/StarRating'
import { reviewService, sessionHelper } from '../services/api'

export default function CreateReviewPage() {
  const navigate      = useNavigate()
  const [params]      = useSearchParams()
  const user          = sessionHelper.get()

  const [movie,   setMovie]   = useState(null)
  const [rating,  setRating]  = useState(0)
  const [text,    setText]    = useState('')
  const [error,   setError]   = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const imdbId = params.get('imdbId')
    const title  = params.get('title')
    const poster = params.get('poster')
    const year   = params.get('year')
    if (imdbId && title) {
      setMovie({ imdbId, title, poster: poster || null, year: year || '' })
    }
  }, [])

  function handleMovieSelect(m) {
    setMovie(m)
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(''); setSuccess('')
    if (!movie)      { setError('Selecione um filme.'); return }
    if (rating === 0) { setError('Selecione uma nota de 1 a 5 estrelas.'); return }
    if (!text.trim()) { setError('Escreva um comentário.'); return }

    setLoading(true)
    try {
      await reviewService.criar(movie.imdbId, rating, text)
      setSuccess('Review publicada com sucesso!')
      setRating(0); setText('')
      setTimeout(() => navigate(`/movies/${movie.imdbId}`), 1200)
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao publicar review.')
    } finally { setLoading(false) }
  }

  if (!user) { navigate('/login'); return null }

  return (
    <>
      <Navbar user={user} />
      <div className="page-main">
        <div className="container--narrow">
          <div className="form-card">

            <div className="logo" style={{marginBottom:'1.5rem'}}>
              <div className="logo-title" style={{fontSize:'1.3rem'}}>
                Cine<span style={{color:'var(--purple-bright)'}}>Score</span>
              </div>
              <div className="logo-tagline">Registrar avaliação</div>
            </div>

            {error   && <div className="msg msg--error">{error}</div>}
            {success && <div className="msg msg--success">✓ {success}</div>}

            <form onSubmit={handleSubmit} noValidate>

              {}
              <div className="form-group">
                <label>Filme</label>
                {movie ? (
                  <div className="movie-selected">
                    {movie.poster && movie.poster !== 'N/A'
                      ? <img src={movie.poster} alt={movie.title} className="movie-selected-poster" />
                      : <div className="movie-selected-poster" style={{display:'flex',alignItems:'center',justifyContent:'center',color:'var(--text-muted)'}}>🎬</div>
                    }
                    <div className="movie-selected-info">
                      <div className="movie-selected-title">{movie.title}</div>
                      <div className="movie-selected-meta">{movie.year} · {movie.imdbId}</div>
                    </div>
                    <button
                      type="button"
                      className="movie-selected-clear"
                      onClick={() => setMovie(null)}
                      aria-label="Remover filme selecionado"
                    >✕</button>
                  </div>
                ) : (
                  <MovieSearchInput onSelect={handleMovieSelect} />
                )}
              </div>

              {}
              <div className="form-group">
                <label>Nota</label>
                <StarRating value={rating} onChange={setRating} size="md" />
              </div>

              {}
              <div className="form-group">
                <label>Comentário</label>
                <textarea
                  placeholder="O que você achou do filme?"
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
              </div>

              <button type="submit" className="btn btn--primary btn--full" disabled={loading}>
                {loading ? <span className="spinner" /> : '★ Publicar Review'}
              </button>
            </form>

            <div className="form-footer" style={{marginTop:'1rem'}}>
              <button
                className="btn btn--ghost btn--sm"
                onClick={() => navigate('/home')}
                style={{width:'auto'}}
              >
                ← Voltar ao início
              </button>
            </div>

          </div>
        </div>
      </div>
    </>
  )
}
