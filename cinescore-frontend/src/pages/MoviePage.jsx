import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { StarDisplay, StarRating } from '../components/StarRating'
import { movieService, reviewService, sessionHelper } from '../services/api'

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

export default function MoviePage() {
  const { imdbId } = useParams()
  const navigate   = useNavigate()
  const user       = sessionHelper.get()

  const [movie,   setMovie]   = useState(null)
  const [reviews, setReviews] = useState([])
  const [loading, setLoading] = useState(true)

  const [editingId,   setEditingId]   = useState(null)
  const [editRating,  setEditRating]  = useState(0)
  const [editText,    setEditText]    = useState('')
  const [editLoading, setEditLoading] = useState(false)
  const [editError,   setEditError]   = useState('')

  const [deletingId,   setDeletingId]   = useState(null)
  const [deleteLoading, setDeleteLoading] = useState(false)

  useEffect(() => {
    if (!user) { navigate('/login'); return }

    Promise.all([
      movieService.buscarPorId(imdbId),
      reviewService.listarPorFilme(imdbId),
    ])
      .then(([m, r]) => { setMovie(m.data); setReviews(r.data) })
      .catch(() => navigate('/home'))
      .finally(() => setLoading(false))
  }, [imdbId])

  const avg = reviews.length > 0
    ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1)
    : null

  function handleEditOpen(review) {
    setEditingId(review.id)
    setEditRating(review.rating)
    setEditText(review.reviewText || '')
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
      const { data } = await reviewService.editar(reviewId, editRating, editText)

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
    try {
      await reviewService.deletar(reviewId)

      setReviews(prev => prev.filter(r => r.id !== reviewId))
      setDeletingId(null)
    } catch (err) {
      alert(err.response?.data?.message || 'Erro ao excluir review.')
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

        {}
        <div className="movie-hero">
          <div className="container">
            <div className="movie-hero-inner">
              {movie?.poster && movie.poster !== 'N/A' && (
                <img src={movie.poster} alt={movie.title} className="movie-poster" />
              )}
              <div>
                <h1 className="movie-info-title">{movie?.title}</h1>
                <div className="movie-info-meta">
                  <span>{movie?.year}</span>
                  <span>{movie?.genre}</span>
                  <span>{movie?.runtime}</span>
                  {movie?.director && movie.director !== 'N/A' && <span>Dir. {movie.director}</span>}
                </div>

                {avg && (
                  <div className="movie-avg">
                    <StarDisplay value={Math.round(avg)} size="sm" />
                    <span className="movie-avg-score">{avg}</span>
                    <span style={{color:'var(--text-muted)', fontSize:'0.75rem'}}>
                      ({reviews.length} review{reviews.length !== 1 ? 's' : ''})
                    </span>
                  </div>
                )}

                {movie?.plot && movie.plot !== 'N/A' && (
                  <p className="movie-plot">{movie.plot}</p>
                )}

                <div style={{ marginTop: '1.2rem' }}>
                  <button
                    className="btn btn--green btn--sm"
                    onClick={() => navigate(`/reviews/new?imdbId=${imdbId}&title=${encodeURIComponent(movie?.title || '')}&poster=${encodeURIComponent(movie?.poster || '')}&year=${movie?.year || ''}`)}
                  >
                    ★ Avaliar este filme
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {}
        <div className="container">
          <div className="timeline">
            <div className="feed-header">
              <span className="feed-title">📋 Linha do tempo de reviews</span>
              <span style={{fontSize:'0.78rem', color:'var(--text-muted)'}}>
                {reviews.length} avaliação{reviews.length !== 1 ? 'ões' : ''}
              </span>
            </div>

            {reviews.length === 0 && (
              <div className="empty-state">
                <div className="empty-state-icon">🎬</div>
                <p>Ainda sem reviews para este filme.</p>
                <button
                  className="btn btn--primary btn--sm"
                  style={{marginTop:'1rem'}}
                  onClick={() => navigate(`/reviews/new?imdbId=${imdbId}`)}
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

                    {}
                    <div className="timeline-meta">
                      <div className="avatar" style={{width:26,height:26,fontSize:'0.68rem'}}>
                        {initials(review.userName)}
                      </div>
                      <span className="timeline-user">{review.userName}</span>
                      <StarDisplay value={review.rating} size="sm" />
                      <span className="timeline-date">{formatDate(review.createdAt)}</span>

                      {}
                      {isOwner && !isEditing && !isDeleting && (
                        <div className="review-actions">
                          <button
                            className="review-action-btn"
                            onClick={() => handleEditOpen(review)}
                            title="Editar review"
                          >
                            ✏️
                          </button>
                          <button
                            className="review-action-btn review-action-btn--delete"
                            onClick={() => setDeletingId(review.id)}
                            title="Excluir review"
                          >
                            🗑️
                          </button>
                        </div>
                      )}
                    </div>

                    {}
                    {!isEditing && !isDeleting && review.reviewText && (
                      <p className="timeline-text">{review.reviewText}</p>
                    )}

                    {}
                    {isEditing && (
                      <div className="review-edit-form">
                        {editError && <div className="msg msg--error" style={{marginBottom:'0.6rem'}}>{editError}</div>}
                        <div style={{marginBottom:'0.6rem'}}>
                          <label style={{fontSize:'0.72rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.1em', display:'block', marginBottom:'0.35rem'}}>Nova nota</label>
                          <StarRating value={editRating} onChange={setEditRating} size="md" />
                        </div>
                        <div style={{marginBottom:'0.75rem'}}>
                          <label style={{fontSize:'0.72rem', color:'var(--text-secondary)', textTransform:'uppercase', letterSpacing:'0.1em', display:'block', marginBottom:'0.35rem'}}>Comentário</label>
                          <textarea
                            value={editText}
                            onChange={e => { setEditText(e.target.value); setEditError('') }}
                            rows={3}
                            style={{minHeight:'70px'}}
                          />
                        </div>
                        <div className="review-edit-actions">
                          <button
                            className="btn btn--primary btn--sm"
                            onClick={() => handleEditSave(review.id)}
                            disabled={editLoading}
                          >
                            {editLoading ? <span className="spinner" /> : 'Salvar'}
                          </button>
                          <button
                            className="btn btn--ghost btn--sm"
                            onClick={handleEditCancel}
                            disabled={editLoading}
                          >
                            Cancelar
                          </button>
                        </div>
                      </div>
                    )}

                    {}
                    {isDeleting && (
                      <div className="review-delete-confirm">
                        <p>Tem certeza que deseja excluir esta review?</p>
                        <div className="review-edit-actions">
                          <button
                            className="btn btn--sm"
                            style={{background:'var(--red)', color:'#fff'}}
                            onClick={() => handleDelete(review.id)}
                            disabled={deleteLoading}
                          >
                            {deleteLoading ? <span className="spinner" /> : 'Excluir'}
                          </button>
                          <button
                            className="btn btn--ghost btn--sm"
                            onClick={() => setDeletingId(null)}
                            disabled={deleteLoading}
                          >
                            Cancelar
                          </button>
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
