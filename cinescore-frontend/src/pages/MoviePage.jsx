import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { StarDisplay, StarRating } from '../components/StarRating'
import { movieService, reviewService, watchlistService, sessionHelper } from '../services/api'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

function contarAvaliacoes(n) {
  return n === 1 ? '1 avaliação' : `${n} avaliações`
}


function certColor(cert) {
  if (cert === 'L')                 return '#4caf76'
  if (cert === '10' || cert === '12') return '#e6b800'
  if (cert === '14')                return '#e67e22'
  return '#e74c3c'
}

export default function MoviePage() {
  const { movieId } = useParams()
  const navigate    = useNavigate()
  const user        = sessionHelper.get()

  const [movie,      setMovie]      = useState(null)
  const [movieError, setMovieError] = useState(false)
  const [reviews,    setReviews]    = useState([])
  const [loading,    setLoading]    = useState(true)

  const [editingId,     setEditingId]     = useState(null)
  const [editRating,    setEditRating]    = useState(0)
  const [editText,      setEditText]      = useState('')
  const [editWatchedAt, setEditWatchedAt] = useState('')
  const [editLoading,   setEditLoading]   = useState(false)
  const [editError,     setEditError]     = useState('')
  const [deletingId,    setDeletingId]    = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)
  const [deleteError,    setDeleteError]   = useState('')
  const [inWatchlist,    setInWatchlist]   = useState(false)
  const [watchlistLoading, setWatchlistLoading] = useState(false)

  useEffect(() => {
    setMovie(null)
    setMovieError(false)
    setReviews([])
    setLoading(true)

    // Promise.allSettled garante que um erro em um não cancela o outro
    if (user) {
      watchlistService.listar()
        .then(({ data }) => setInWatchlist((data || []).some(i => i.movieId === movieId)))
        .catch(() => {})
    }

    Promise.allSettled([
      movieService.buscarPorId(movieId),
      reviewService.listarPorFilme(movieId),
    ]).then(([movieResult, reviewsResult]) => {
      if (movieResult.status === 'fulfilled') {
        setMovie(movieResult.value.data)
      } else {
        setMovieError(true)
      }
      if (reviewsResult.status === 'fulfilled') {
        setReviews(reviewsResult.value.data || [])
      }
    }).finally(() => setLoading(false))
  }, [movieId])

  const avg = reviews.length > 0
    ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
    : null

  async function handleWatchlist() {
    if (!user) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    setWatchlistLoading(true)
    try {
      if (inWatchlist) {
        await watchlistService.remover(movieId)
        setInWatchlist(false)
      } else {
        await watchlistService.adicionar({
          movieId,
          movieTitle:       movie?.title  || '',
          moviePoster:      movie?.poster || null,
          movieYear:        movie?.year   || null,
          movieVoteAverage: movie?.voteAverage || null,
        })
        setInWatchlist(true)
      }
    } catch { /* silently ignore */ }
    finally { setWatchlistLoading(false) }
  }

  function handleEditOpen(review) {
    const hoje = new Date().toISOString().substring(0, 10)
    setEditingId(review.id)
    setEditRating(review.rating)
    setEditText(review.reviewText || '')
    setEditWatchedAt((review.watchedAt || review.createdAt || hoje).substring(0, 10))
    setEditError('')
  }

  function handleEditCancel() {
    setEditingId(null)
    setEditRating(0)
    setEditText('')
    setEditError('')
  }

  async function handleEditSave(reviewId) {
    if (editRating === 0) { setEditError('Selecione uma nota.'); return }
    if (!editText.trim()) { setEditError('Escreva um comentário.'); return }
    setEditLoading(true)
    setEditError('')
    try {
      const { data } = await reviewService.editar(reviewId, editRating, editText, editWatchedAt)
      setReviews(prev => prev.map(r => r.id === reviewId ? data : r))
      setEditingId(null)
    } catch (err) {
      setEditError(err.response?.data?.message || 'Erro ao salvar edição.')
    } finally {
      setEditLoading(false)
    }
  }

  async function handleDelete(reviewId) {
    setDeleteLoading(true)
    setDeleteError('')
    try {
      await reviewService.deletar(reviewId)
      setReviews(prev => prev.filter(r => r.id !== reviewId))
      setDeletingId(null)
    } catch (err) {
      setDeleteError(err.response?.data?.message || 'Erro ao excluir review.')
    } finally {
      setDeleteLoading(false)
    }
  }

  if (loading) return (
    <>
      <Navbar user={user} />
      <div className="page-main" style={{display:'flex',alignItems:'center',justifyContent:'center',paddingTop:'80px'}}>
        <span className="spinner" />
      </div>
    </>
  )

  return (
    <>
      <Navbar user={user} />
      <div className="page-main">

        <div className="movie-hero">
          <div className="container">
            {movieError ? (
              <div style={{ padding:'2rem 0', color:'var(--text-muted)', display:'flex', flexDirection:'column', gap:'1rem' }}>
                <p style={{ fontSize:'1rem' }}>⚠️ Não foi possível carregar as informações deste filme.</p>
                <p style={{ fontSize:'0.85rem' }}>
                  O ID do filme pode estar incorreto ou o serviço está temporariamente indisponível.
                </p>
                <button className="btn btn--ghost btn--sm" style={{ width:'fit-content' }}
                  onClick={() => navigate(-1)}>← Voltar</button>
              </div>
            ) : (
              <div className="movie-hero-inner">
                {movie?.poster && (
                  <img src={movie.poster} alt={movie.title} className="movie-poster" />
                )}
                <div>
                  <h1 className="movie-info-title">{movie?.title}</h1>

                  {movie?.certification && (
                    <div style={{ margin: '6px 0 2px' }}>
                      <span style={{
                        padding: '2px 10px',
                        borderRadius: 4,
                        border: `1.5px solid ${certColor(movie.certification)}`,
                        color: certColor(movie.certification),
                        fontSize: '0.8rem',
                        fontWeight: 700,
                        letterSpacing: '0.03em',
                      }}>
                        {movie.certification === 'L' ? 'Livre' : `${movie.certification} anos`}
                      </span>
                    </div>
                  )}

                  <div className="movie-info-meta">
                    <span>{movie?.year}</span>
                    <span>{movie?.genre}</span>
                    {movie?.runtime && <span>{movie.runtime} min</span>}
                    {movie?.director && <span>Dir. {movie.director}</span>}
                  </div>

                  {avg && (
                    <div className="movie-avg">
                      <StarDisplay value={Math.round(avg)} size="sm" />
                      <span className="movie-avg-score">{avg}</span>
                      <span style={{color:'var(--text-muted)', fontSize:'0.75rem'}}>
                        ({contarAvaliacoes(reviews.length)})
                      </span>
                    </div>
                  )}

                  {movie?.overview && (
                    <p className="movie-plot">{movie.overview}</p>
                  )}

                  {movie?.actors && (
                    <p style={{fontSize:'0.8rem', color:'var(--text-muted)', marginTop:'0.5rem'}}>
                      {movie.actors}
                    </p>
                  )}

                  <div style={{ marginTop: '1.2rem', display:'flex', gap:'0.75rem', flexWrap:'wrap' }}>
                    <button
                      className="btn btn--green btn--sm"
                      onClick={() => user
                        ? navigate(`/reviews/new?movieId=${movieId}&title=${encodeURIComponent(movie?.title || '')}&poster=${encodeURIComponent(movie?.poster || '')}&year=${movie?.year || ''}`)
                        : navigate('/login', { state: { from: `/movies/${movieId}` } })
                      }
                    >
                      ★ Avaliar este filme
                    </button>
                    <button
                        className={`btn btn--sm ${inWatchlist ? 'btn--primary' : 'btn--ghost'}`}
                        onClick={handleWatchlist}
                        disabled={watchlistLoading}
                      >
                        {watchlistLoading
                          ? <span className="spinner" style={{ width:12, height:12 }} />
                          : inWatchlist ? '✓ Na Watchlist' : '+ Watchlist'}
                      </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="container">
          <div className="timeline">
            <div className="feed-header">
              <span className="feed-title">📋 Linha do tempo de reviews</span>
              <span style={{fontSize:'0.78rem', color:'var(--text-muted)'}}>
                {contarAvaliacoes(reviews.length)}
              </span>
            </div>

            {deleteError && (
              <div className="msg msg--error" style={{ marginBottom:'1rem' }}>{deleteError}</div>
            )}

            {reviews.length === 0 && (
              <div className="empty-state">
                <div className="empty-state-icon">🎬</div>
                <p>Ainda sem reviews para este filme.</p>
                <button
                  className="btn btn--primary btn--sm"
                  style={{marginTop:'1rem'}}
                  onClick={() => navigate(`/reviews/new?movieId=${movieId}`)}
                >
                  Seja o primeiro a avaliar
                </button>
              </div>
            )}

            {reviews.map((review, i) => {
              const isOwner    = user && review.userId === user.id
              const isEditing  = editingId === review.id
              const isDeleting = deletingId === review.id

              return (
                <div key={review.id} className="timeline-item" style={{animationDelay:`${i * 0.06}s`}}>
                  <div className="timeline-line">
                    <div className="timeline-dot" />
                  </div>
                  <div className="timeline-content" style={{flex:1}}>
                    <div className="timeline-meta">
                      <div className="avatar" style={{width:26,height:26,fontSize:'0.68rem',cursor:'pointer'}}
                        onClick={() => navigate(`/profile/${review.userId}`)} title="Ver perfil">
                        {initials(review.userName)}
                      </div>
                      <span className="timeline-user" style={{cursor:'pointer'}}
                        onClick={() => navigate(`/profile/${review.userId}`)} title="Ver perfil">
                        {review.userName}
                      </span>
                      <StarDisplay value={review.rating} size="sm" />
                      <span className="timeline-date">{formatDate(review.createdAt)}</span>

                      {isOwner && !isEditing && !isDeleting && (
                        <div className="review-actions">
                          <button className="review-action-btn" onClick={() => handleEditOpen(review)} title="Editar">✏️</button>
                          <button className="review-action-btn review-action-btn--delete" onClick={() => setDeletingId(review.id)} title="Excluir">🗑️</button>
                        </div>
                      )}
                    </div>

                    {!isEditing && !isDeleting && review.reviewText && (
                      <p className="timeline-text">{review.reviewText}</p>
                    )}

                    {isEditing && (
                      <div className="review-edit-form">
                        {editError && <div className="msg msg--error" style={{marginBottom:'0.6rem'}}>{editError}</div>}
                        <div style={{marginBottom:'0.6rem'}}>
                          <label style={{fontSize:'0.72rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.1em', display:'block', marginBottom:'0.35rem'}}>Data assistido</label>
                          <input type="date" value={editWatchedAt}
                            max={new Date().toISOString().substring(0, 10)}
                            onChange={e => setEditWatchedAt(e.target.value)}
                            style={{padding:'0.3rem 0.6rem', borderRadius:6,
                              border:'1px solid var(--border)', background:'var(--bg-input)',
                              color:'var(--text-primary)', fontSize:'0.85rem'}} />
                        </div>
                        <div style={{marginBottom:'0.6rem'}}>
                          <label style={{fontSize:'0.72rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.1em', display:'block', marginBottom:'0.35rem'}}>Nova nota</label>
                          <StarRating value={editRating} onChange={setEditRating} size="md" />
                        </div>
                        <div style={{marginBottom:'0.75rem'}}>
                          <label style={{fontSize:'0.72rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.1em', display:'block', marginBottom:'0.35rem'}}>Comentário</label>
                          <textarea value={editText} onChange={e => { setEditText(e.target.value); setEditError('') }} rows={3} style={{minHeight:'70px'}} />
                        </div>
                        <div className="review-edit-actions">
                          <button className="btn btn--primary btn--sm" onClick={() => handleEditSave(review.id)} disabled={editLoading}>
                            {editLoading ? <span className="spinner" /> : 'Salvar'}
                          </button>
                          <button className="btn btn--ghost btn--sm" onClick={handleEditCancel} disabled={editLoading}>Cancelar</button>
                        </div>
                      </div>
                    )}

                    {isDeleting && (
                      <div className="review-delete-confirm">
                        <p>Tem certeza que deseja excluir esta review?</p>
                        <div className="review-edit-actions">
                          <button className="btn btn--sm" style={{background:'var(--red)', color:'#fff'}}
                            onClick={() => handleDelete(review.id)} disabled={deleteLoading}>
                            {deleteLoading ? <span className="spinner" /> : 'Excluir'}
                          </button>
                          <button className="btn btn--ghost btn--sm" onClick={() => setDeletingId(null)} disabled={deleteLoading}>Cancelar</button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    </>
  )
}
