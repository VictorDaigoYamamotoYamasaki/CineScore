import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import MovieSearchInput from '../components/MovieSearchInput'
import { StarRating } from '../components/StarRating'
import { reviewService, sessionHelper } from '../services/api'

function ErroInline({ msg }) {
  if (!msg) return null
  return <span style={{ color:'#ff6b6b', fontSize:'0.76rem', marginTop:'4px', display:'block' }}>{msg}</span>
}

export default function CreateReviewPage() {
  const navigate      = useNavigate()
  const [params]      = useSearchParams()
  const user          = sessionHelper.get()

  const [movie,   setMovie]   = useState(null)
  const [rating,  setRating]  = useState(0)
  const [text,    setText]    = useState('')
  const [watchedAt, setWatchedAt] = useState(new Date().toISOString().substring(0, 10))
  const [erros,   setErros]   = useState({})
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const movieId = params.get('movieId')
    const title   = params.get('title')
    const poster  = params.get('poster')
    const year    = params.get('year')
    if (movieId && title) {
      setMovie({ id: movieId, title, poster: poster || null, year: year || '' })
    }
  }, [])

  function handleMovieSelect(m) {
    setMovie(m)
    setErros(p => ({ ...p, movie: '' }))
  }

  function handleRatingChange(v) {
    setRating(v)
    setErros(p => ({ ...p, rating: '' }))
  }

  function handleTextChange(e) {
    setText(e.target.value)
    setErros(p => ({ ...p, text: '' }))
  }

  function validar() {
    const e = {}
    if (!movie)
      e.movie = 'Selecione um filme.'
    if (rating === 0)
      e.rating = 'Selecione uma nota de 0,5 a 5 estrelas.'
    if (!text.trim())
      e.text = 'Escreva um comentário.'
    else if (text.trim().length > 2000)
      e.text = `Review deve ter no máximo 2000 caracteres (atual: ${text.trim().length}).`
    else if (text.trim().length < 3)
      e.text = 'Comentário deve ter pelo menos 3 caracteres.'
    return e
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSuccess('')
    const e2 = validar()
    if (Object.keys(e2).length > 0) { setErros(e2); return }

    setLoading(true)
    try {
      await reviewService.criar(String(movie.id), rating, text, movie.title || '', movie.poster || '', watchedAt)
      setSuccess('Review publicada com sucesso!')
      setRating(0); setText('')
      setTimeout(() => navigate(`/movies/${movie.id}`), 1200)
    } catch (err) {
      setErros({ api: err.response?.data?.message || 'Erro ao publicar review.' })
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
              <div className="logo-title">
                Cine<span style={{color:'var(--purple-bright)'}}>Score</span>
              </div>
              <div className="logo-tagline">Registrar avaliação</div>
            </div>

            {erros.api  && <div className="msg msg--error">{erros.api}</div>}
            {success    && <div className="msg msg--success">✓ {success}</div>}

            <form onSubmit={handleSubmit} noValidate>
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
                      <div className="movie-selected-meta">{movie.year} · ID: {movie.id}</div>
                    </div>
                    <button type="button" className="movie-selected-clear"
                      onClick={() => setMovie(null)}>✕</button>
                  </div>
                ) : (
                  <MovieSearchInput onSelect={handleMovieSelect} hideActorToggle />
                )}
                <ErroInline msg={erros.movie} />
              </div>

              <div className="form-group">
                <label>Data assistido</label>
                <input
                  type="date"
                  value={watchedAt}
                  max={new Date().toISOString().substring(0, 10)}
                  onChange={e => setWatchedAt(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>Nota</label>
                <StarRating value={rating} onChange={handleRatingChange} size="md" />
                <ErroInline msg={erros.rating} />
              </div>

              <div className="form-group">
                <label>Comentário</label>
                <textarea
                  placeholder="O que você achou do filme?"
                  value={text}
                  onChange={handleTextChange}
                  style={erros.text ? { borderColor:'#ff6b6b' } : {}}
                />
                <div style={{ textAlign:'right', fontSize:'0.72rem', marginTop:'2px',
                  color: text.length > 1900 ? '#ff6b6b' : text.length > 1600 ? 'var(--warning, #f5a623)' : 'var(--text-muted)' }}>
                  {text.length}/2000
                </div>
                <ErroInline msg={erros.text} />
              </div>

              <button type="submit" className="btn btn--primary btn--full" disabled={loading}>
                {loading ? <span className="spinner" /> : '★ Publicar Review'}
              </button>
            </form>

            <div className="form-footer" style={{marginTop:'1rem'}}>
              <button className="btn btn--ghost btn--sm" onClick={() => navigate('/home')} style={{width:'auto'}}>
                ← Voltar ao início
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
