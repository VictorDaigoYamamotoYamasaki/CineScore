import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { StarDisplay, StarRating } from './StarRating'
import CommentModal from './CommentModal'
import { interactionService, movieService, reviewService, sessionHelper } from '../services/api'

// Cache com limite de tamanho para evitar crescimento ilimitado de memória
const MAX_CACHE_SIZE = 200
const movieInfoCache = new Map()

async function fetchMovieInfo(movieId) {
  if (!movieId) return null
  if (movieInfoCache.has(movieId)) return movieInfoCache.get(movieId)

  // Marca como em busca para evitar chamadas duplicadas
  movieInfoCache.set(movieId, null)

  // Remove a entrada mais antiga ao atingir o limite
  if (movieInfoCache.size > MAX_CACHE_SIZE) {
    movieInfoCache.delete(movieInfoCache.keys().next().value)
  }

  try {
    const { data } = await movieService.buscarPorId(movieId)
    movieInfoCache.set(movieId, { title: data.title, poster: data.poster })
  } catch {
    movieInfoCache.set(movieId, null)
  }
  return movieInfoCache.get(movieId)
}

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}
function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day:'2-digit', month:'short', year:'numeric' })
}

const EMOJIS = ['❤️','😂','😮','🔥','👍','😢']

export default function ReviewCard({ review, movieTitle, moviePoster, onClick, style, isOwn, onUpdate, onDelete }) {
  const navigate    = useNavigate()
  const currentUser = sessionHelper.get()

  const [resolvedTitle,  setResolvedTitle]  = useState(movieTitle || review.movieTitle || null)
  const [resolvedPoster, setResolvedPoster] = useState(moviePoster || review.moviePoster || null)

  const [showModal,    setShowModal]    = useState(false)
  const [commentCount, setCommentCount] = useState(null)
  const [reactions,    setReactions]    = useState({ counts:{}, myReactions:[] })
  const [showEmojis,   setShowEmojis]   = useState(false)
  const [pendingEmoji, setPendingEmoji] = useState(null)

  // ── Estado de edição inline ───────────────────────────────────────────────
  const [editMode,      setEditMode]      = useState(false)
  const [editRating,    setEditRating]    = useState(0)
  const [editText,      setEditText]      = useState('')
  const [editWatchedAt, setEditWatchedAt] = useState('')
  const [editLoading,   setEditLoading]   = useState(false)
  const [editError,     setEditError]     = useState('')
  const [deleteMode,    setDeleteMode]    = useState(false)
  const [deleteLoading, setDeleteLoading] = useState(false)

  // Busca informações do filme se não tiver título ou poster
  useEffect(() => {
    const needsFetch = (!resolvedTitle || !resolvedPoster) && review.movieId
    if (!needsFetch) return

    fetchMovieInfo(review.movieId).then(info => {
      if (info) {
        if (!resolvedTitle)  setResolvedTitle(info.title)
        if (!resolvedPoster) setResolvedPoster(info.poster)
      }
    }).catch(() => {})
  }, [review.movieId])

  useEffect(() => {
    interactionService.resumo(review.id)
      .then(({ data }) => {
        setCommentCount(data.commentCount)
        setReactions(data.reactions || { counts: {}, myReactions: [] })
      })
      .catch(() => {})
  }, [review.id])

  const title  = resolvedTitle  || review.movieId
  const poster = resolvedPoster || null

  function abrirEdicao(e) {
    e.stopPropagation()
    const hoje = new Date().toISOString().substring(0, 10)
    setEditRating(review.rating)
    setEditText(review.reviewText || '')
    setEditWatchedAt((review.watchedAt || review.createdAt || hoje).substring(0, 10))
    setEditError('')
    setDeleteMode(false)
    setEditMode(true)
  }

  function cancelarEdicao(e) {
    e?.stopPropagation()
    setEditMode(false)
    setDeleteMode(false)
    setEditError('')
  }

  async function salvarEdicao(e) {
    e.stopPropagation()
    if (!editRating) { setEditError('Selecione uma nota.'); return }
    setEditLoading(true); setEditError('')
    try {
      const { data } = await reviewService.editar(review.id, editRating, editText, editWatchedAt)
      onUpdate?.(data)
      setEditMode(false)
    } catch (err) {
      setEditError(err.response?.data?.message || 'Erro ao salvar.')
    } finally { setEditLoading(false) }
  }

  async function confirmarDelete(e) {
    e.stopPropagation()
    setDeleteLoading(true)
    try {
      await reviewService.deletar(review.id)
      onDelete?.(review.id)
    } catch (err) {
      setEditError(err.response?.data?.message || 'Erro ao excluir.')
      setDeleteMode(false)
    } finally { setDeleteLoading(false) }
  }

  function handleCardClick() {
    if (onClick) { onClick(); return }
    if (review.movieId) navigate(`/movies/${review.movieId}`)
  }


  async function handleReact(e, emoji) {
    e.stopPropagation()
    if (!sessionHelper.isLogged()) { navigate('/login'); return }
    if (pendingEmoji) return
    setPendingEmoji(emoji)
    try {
      const { data } = await interactionService.reagir(review.id, emoji)
      setReactions(data)
    } catch (err) {
      console.error('Erro ao reagir:', err?.response?.data || err)
    } finally {
      setPendingEmoji(null)
    }
  }

  function handleCommentClick(e) {
    e.stopPropagation()
    if (!sessionHelper.isLogged()) { navigate('/login'); return }
    setShowModal(true)
  }

  function handleSummaryChange(_, changes) {
    if (changes.commentCountDelta !== undefined)
      setCommentCount(prev => (prev || 0) + changes.commentCountDelta)
    if (changes.reactions) setReactions(changes.reactions)
  }

  const activeReactions = Object.entries((reactions || {}).counts || {}).filter(([, c]) => c > 0)

  return (
    <>
      {/* ── Formulário de edição inline ── */}
      {editMode && (
        <div onClick={e => e.stopPropagation()}
          style={{ background:'var(--bg-elevated)', borderRadius:8,
            border:'1px solid var(--border)', padding:'1rem 1.2rem',
            marginTop:'-0.5rem', marginBottom:'1.25rem' }}>
          {editError && <div className="msg msg--error" style={{marginBottom:'0.6rem'}}>{editError}</div>}
          <div style={{ display:'flex', gap:'1.5rem', flexWrap:'wrap', marginBottom:'0.75rem' }}>
            <div>
              <label style={{ fontSize:'0.7rem', color:'var(--text-secondary)',
                textTransform:'uppercase', letterSpacing:'0.08em',
                display:'block', marginBottom:'0.3rem' }}>Data assistido</label>
              <input type="date" value={editWatchedAt}
                max={new Date().toISOString().substring(0, 10)}
                onChange={e => setEditWatchedAt(e.target.value)}
                style={{ padding:'0.3rem 0.6rem', borderRadius:6,
                  border:'1px solid var(--border)', background:'var(--bg-input)',
                  color:'var(--text-primary)', fontSize:'0.85rem' }} />
            </div>
            <div>
              <label style={{ fontSize:'0.7rem', color:'var(--text-secondary)',
                textTransform:'uppercase', letterSpacing:'0.08em',
                display:'block', marginBottom:'0.3rem' }}>Nota</label>
              <StarRating value={editRating} onChange={setEditRating} size="md" />
            </div>
          </div>
          <div style={{ marginBottom:'0.75rem' }}>
            <label style={{ fontSize:'0.7rem', color:'var(--text-secondary)',
              textTransform:'uppercase', letterSpacing:'0.08em',
              display:'block', marginBottom:'0.3rem' }}>Comentário</label>
            <textarea value={editText} onChange={e => setEditText(e.target.value)}
              rows={2} style={{ minHeight:60, fontSize:'0.85rem' }}
              placeholder="O que você achou?" />
            <div style={{ textAlign:'right', fontSize:'0.7rem',
              color: editText.length > 1900 ? '#ff6b6b' : 'var(--text-muted)' }}>
              {editText.length}/2000
            </div>
          </div>
          <div style={{ display:'flex', gap:'0.5rem' }}>
            <button className="btn btn--primary btn--sm"
              onClick={salvarEdicao} disabled={editLoading}>
              {editLoading ? <span className="spinner" /> : 'Salvar'}
            </button>
            <button className="btn btn--ghost btn--sm"
              onClick={cancelarEdicao} disabled={editLoading}>Cancelar</button>
          </div>
        </div>
      )}

      {/* ── Confirmação de exclusão ── */}
      {deleteMode && (
        <div onClick={e => e.stopPropagation()}
          style={{ background:'var(--bg-elevated)', borderRadius:8,
            border:'1px solid var(--border)', padding:'0.9rem 1.2rem',
            marginTop:'-0.5rem', marginBottom:'1.25rem',
            display:'flex', alignItems:'center', gap:'1rem', flexWrap:'wrap' }}>
          <span style={{ fontSize:'0.85rem', color:'var(--text-secondary)', flex:1 }}>
            Excluir esta review?
          </span>
          <div style={{ display:'flex', gap:'0.5rem' }}>
            <button className="btn btn--sm"
              style={{ background:'var(--red)', color:'#fff', border:'none' }}
              onClick={confirmarDelete} disabled={deleteLoading}>
              {deleteLoading ? <span className="spinner" /> : 'Excluir'}
            </button>
            <button className="btn btn--ghost btn--sm"
              onClick={cancelarEdicao} disabled={deleteLoading}>Cancelar</button>
          </div>
        </div>
      )}

      {showModal && (
        <CommentModal
          review={{ ...review, movieTitle: title }}
          onClose={() => setShowModal(false)}
          onSummaryChange={handleSummaryChange}
        />
      )}

      <div
        className="review-card"
        onClick={handleCardClick}
        style={{ cursor:'pointer', marginBottom:'1.25rem', display:'flex', gap:'1rem', alignItems:'flex-start', padding:'0.9rem 1rem', ...style }}
      >
        {/* Poster */}
        <div style={{ width:70, height:100, borderRadius:6, flexShrink:0, overflow:'hidden',
          background:'var(--bg-elevated)', boxShadow:'0 3px 10px rgba(0,0,0,0.5)', position:'relative' }}>
          {poster
            ? <img src={poster} alt={title}
                style={{ position:'absolute', inset:0, width:'100%', height:'100%', objectFit:'cover' }}
                onError={e => e.target.style.display='none'} />
            : <div style={{ position:'absolute', inset:0, display:'flex', alignItems:'center', justifyContent:'center', fontSize:'1.6rem' }}>🎬</div>
          }
        </div>

        {/* Conteúdo */}
        <div style={{ flex:1, minWidth:0 }}>
          {/* Cabeçalho: usuário + data + botões de edição (se dono) */}
          <div style={{ display:'flex', alignItems:'center', gap:'0.5rem', marginBottom:'0.3rem' }}>
            <Link
              to={`/profile/${review.userId}`}
              onClick={e => e.stopPropagation()}
              style={{ display:'flex', alignItems:'center', gap:'0.5rem', textDecoration:'none', color:'inherit' }}
            >
              <div className="avatar" style={{ width:24, height:24, fontSize:'0.6rem', flexShrink:0 }}>
                {initials(review.userName)}
              </div>
              <span
                style={{ fontWeight:700, fontSize:'0.88rem', color:'var(--text-primary)', transition:'color 0.15s' }}
                onMouseEnter={e => e.currentTarget.style.color='var(--purple-bright)'}
                onMouseLeave={e => e.currentTarget.style.color='var(--text-primary)'}
              >{review.userName}</span>
            </Link>
            <span style={{ fontSize:'0.72rem', color:'var(--text-muted)' }}>{formatDate(review.createdAt)}</span>
            {isOwn && !editMode && !deleteMode && (
              <div style={{ marginLeft:'auto', display:'flex', gap:'0.4rem' }} onClick={e => e.stopPropagation()}>
                <button onClick={abrirEdicao}
                  style={{ background:'none', border:'none', cursor:'pointer', padding:'0.1rem 0.3rem',
                    color:'var(--text-muted)', fontSize:'0.8rem' }} title="Editar">✎</button>
                <button onClick={e => { e.stopPropagation(); setDeleteMode(true); setEditMode(false) }}
                  style={{ background:'none', border:'none', cursor:'pointer', padding:'0.1rem 0.3rem',
                    color:'var(--text-muted)', fontSize:'0.8rem' }} title="Excluir">✕</button>
              </div>
            )}
          </div>

          {/* Título */}
          <div style={{ fontSize:'0.92rem', fontWeight:600, color:'var(--text-primary)', marginBottom:'0.3rem',
            overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}>
            {title}
          </div>

          {/* Estrelas */}
          <div style={{ marginBottom:'0.4rem' }}>
            <StarDisplay value={review.rating} size="sm" />
          </div>

          {/* Texto */}
          {review.reviewText && (
            <p style={{ fontSize:'0.82rem', color:'var(--text-secondary)', margin:'0 0 0.5rem', lineHeight:1.5,
              display:'-webkit-box', WebkitLineClamp:3, WebkitBoxOrient:'vertical', overflow:'hidden' }}>
              {review.reviewText}
            </p>
          )}

          {/* Barra de interações */}
          <div style={{ display:'flex', alignItems:'center', gap:'0.4rem', flexWrap:'wrap', marginTop:'0.4rem' }}
            onClick={e => e.stopPropagation()}>

            {activeReactions.map(([emoji, count]) => {
              const mine = reactions.myReactions?.includes(emoji)
              return (
                <button key={emoji} onClick={e => handleReact(e, emoji)} title={mine ? 'Remover reação' : 'Reagir'}
                  style={{ padding:'0.15rem 0.5rem', borderRadius:999, fontSize:'0.82rem',
                    border: mine ? '1px solid var(--purple-accent)' : '1px solid var(--border)',
                    background: mine ? 'rgba(124,92,191,0.18)' : 'var(--bg-surface)',
                    cursor:'pointer', display:'flex', alignItems:'center', gap:'0.2rem', transition:'all 0.12s' }}>
                  {emoji}
                  <span style={{ fontSize:'0.72rem', color: mine ? 'var(--purple-bright)' : 'var(--text-muted)' }}>{count}</span>
                </button>
              )
            })}

            <button onClick={e => { e.stopPropagation(); setShowEmojis(v => !v) }} title="Reagir"
              style={{ padding:'0.15rem 0.45rem', borderRadius:999, fontSize:'0.8rem',
                border: showEmojis ? '1px solid var(--purple-accent)' : '1px solid var(--border)',
                background: showEmojis ? 'rgba(124,92,191,0.12)' : 'transparent',
                cursor:'pointer', color:'var(--text-muted)', transition:'all 0.12s' }}>
              😊 +
            </button>

            {showEmojis && (
              <div style={{ display:'flex', gap:'0.25rem', alignItems:'center', background:'var(--bg-elevated)',
                border:'1px solid var(--border)', borderRadius:999, padding:'0.2rem 0.5rem' }}
                onClick={e => e.stopPropagation()}>
                {/* Somente 1 reação por review — clicar em diferente troca */}
                {EMOJIS.map(emoji => {
                  const mine  = reactions.myReactions?.includes(emoji)
                  const count = reactions.counts?.[emoji] || 0
                  return (
                    <button key={emoji} onClick={e => handleReact(e, emoji)} title={mine ? 'Remover' : 'Reagir'}
                      disabled={!!pendingEmoji}
                      style={{ background: mine ? 'rgba(124,92,191,0.25)' : 'transparent',
                        border: mine ? '1px solid var(--purple-accent)' : '1px solid transparent',
                        borderRadius:8, padding:'0.2rem 0.35rem', fontSize:'1rem',
                        cursor: pendingEmoji ? 'wait' : 'pointer',
                        transition:'transform 0.1s, background 0.12s', position:'relative',
                        opacity: pendingEmoji && pendingEmoji !== emoji ? 0.5 : 1 }}
                      onMouseEnter={e => e.currentTarget.style.transform='scale(1.25)'}
                      onMouseLeave={e => e.currentTarget.style.transform='scale(1)'}>
                      {emoji}
                      {count > 0 && (
                        <span style={{ position:'absolute', top:-4, right:-4,
                          background: mine ? 'var(--purple-accent)' : 'var(--bg-card)',
                          border:'1px solid var(--border)', borderRadius:999, fontSize:'0.55rem',
                          padding:'0 3px', color: mine ? '#fff' : 'var(--text-muted)',
                          lineHeight:1.4, minWidth:12, textAlign:'center' }}>{count}</span>
                      )}
                    </button>
                  )
                })}
                <button onClick={e => { e.stopPropagation(); setShowEmojis(false) }}
                  style={{ background:'none', border:'none', color:'var(--text-muted)', cursor:'pointer', fontSize:'0.75rem', padding:'0 0.2rem', marginLeft:2 }}>✕</button>
              </div>
            )}

            <button onClick={handleCommentClick}
              style={{ padding:'0.15rem 0.6rem', borderRadius:999, fontSize:'0.8rem',
                border:'1px solid var(--border)', background:'var(--bg-surface)',
                cursor:'pointer', display:'flex', alignItems:'center', gap:'0.3rem',
                color:'var(--text-muted)', transition:'all 0.15s', marginLeft:'auto' }}
              onMouseEnter={e => { e.currentTarget.style.borderColor='var(--purple-accent)'; e.currentTarget.style.color='var(--text-primary)' }}
              onMouseLeave={e => { e.currentTarget.style.borderColor='var(--border)'; e.currentTarget.style.color='var(--text-muted)' }}>
              💬 <span>{commentCount ?? '·'}</span>
            </button>
          </div>
        </div>
      </div>
    </>
  )
}
