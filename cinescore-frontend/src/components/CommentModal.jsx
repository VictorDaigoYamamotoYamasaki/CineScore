import { useEffect, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { interactionService, sessionHelper } from '../services/api'

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}
function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day:'2-digit', month:'short', year:'numeric' })
}

const EMOJIS = ['❤️','😂','😮','🔥','👍','😢']

export default function CommentModal({ review, onClose, onSummaryChange }) {
  const navigate    = useNavigate()
  const currentUser = sessionHelper.get()
  const inputRef    = useRef(null)

  const [comments,    setComments]    = useState([])
  const [reactions,   setReactions]   = useState({ counts: {}, myReactions: [] })
  const [text,        setText]        = useState('')
  const [sending,     setSending]     = useState(false)
  const [loadingC,    setLoadingC]    = useState(true)
  const [pendingEmoji, setPendingEmoji] = useState(null)

  useEffect(() => {
    Promise.all([
      interactionService.listarComentarios(review.id),
      interactionService.resumo(review.id),
    ]).then(([c, s]) => {
      setComments(c.data)
      setReactions(s.data.reactions)
    }).finally(() => setLoadingC(false))

    setTimeout(() => inputRef.current?.focus(), 100)

    // Fechar ao pressionar Esc
    const onKey = e => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [review.id])

  async function handleSend() {
    const t = text.trim()
    if (!t || sending) return
    setSending(true)
    try {
      const { data } = await interactionService.adicionarComentario(review.id, t)
      setComments(prev => [...prev, data])
      setText('')
      onSummaryChange?.(review.id, { commentCountDelta: 1 })
    } catch (e) {
      alert(e.response?.data?.message || 'Erro ao comentar.')
    } finally { setSending(false) }
  }

  async function handleDelete(commentId) {
    if (!window.confirm('Excluir comentário?')) return
    try {
      await interactionService.deletarComentario(review.id, commentId)
      setComments(prev => prev.filter(c => c.id !== commentId))
      onSummaryChange?.(review.id, { commentCountDelta: -1 })
    } catch (e) {
      alert(e.response?.data?.message || 'Erro ao excluir.')
    }
  }

  async function handleReact(emoji) {
    if (pendingEmoji) return
    setPendingEmoji(emoji)
    try {
      const { data } = await interactionService.reagir(review.id, emoji)
      setReactions(data)
      onSummaryChange?.(review.id, { reactions: data })
    } catch (e) {
      console.error('Erro ao reagir:', e?.response?.data || e)
    } finally {
      setPendingEmoji(null)
    }
  }

  return (
    <div
      style={{
        position:'fixed', inset:0, zIndex:2000,
        background:'rgba(0,0,0,0.75)',
        display:'flex', alignItems:'center', justifyContent:'center',
        padding:'1rem',
      }}
      onClick={onClose}
    >
      <div
        style={{
          background:'var(--bg-card)',
          border:'1px solid var(--border)',
          borderRadius:12,
          width:'100%', maxWidth:560,
          maxHeight:'85vh',
          display:'flex', flexDirection:'column',
          overflow:'hidden',
        }}
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ padding:'1rem 1.2rem', borderBottom:'1px solid var(--border)', display:'flex', justifyContent:'space-between', alignItems:'center' }}>
          <div>
            <div style={{ fontWeight:700, fontSize:'0.95rem' }}>
              Review de <span style={{ color:'var(--purple-bright)' }}>{review.userName}</span>
            </div>
            <div style={{ fontSize:'0.78rem', color:'var(--text-muted)', marginTop:2 }}>
              {review.movieTitle || review.movieId}
            </div>
          </div>
          <button onClick={onClose} style={{ background:'none', border:'none', color:'var(--text-muted)', fontSize:'1.2rem', cursor:'pointer' }}>✕</button>
        </div>

        {/* Reações — 1 por review, clicar em diferente troca automaticamente */}
        <div style={{ padding:'0.7rem 1.2rem', borderBottom:'1px solid var(--border)', display:'flex', alignItems:'center', gap:'0.4rem', flexWrap:'wrap' }}>
          {EMOJIS.map(emoji => {
            const count = reactions.counts?.[emoji] || 0
            const mine  = reactions.myReactions?.includes(emoji)
            return (
              <button
                key={emoji}
                onClick={() => handleReact(emoji)}
                title={mine ? 'Remover reação' : 'Reagir com ' + emoji}
                style={{
                  padding:'0.25rem 0.6rem',
                  borderRadius:999,
                  border: mine ? '1px solid var(--purple-accent)' : '1px solid var(--border)',
                  background: mine ? 'rgba(124,92,191,0.18)' : 'var(--bg-surface)',
                  cursor:'pointer', fontSize:'0.92rem',
                  display:'flex', alignItems:'center', gap:'0.25rem',
                  transition:'all 0.12s',
                  transform: 'scale(1)',
                }}
                disabled={!!pendingEmoji}
                onMouseEnter={e => e.currentTarget.style.transform='scale(1.15)'}
                onMouseLeave={e => e.currentTarget.style.transform='scale(1)'}
              >
                {emoji}
                {count > 0 && (
                  <span style={{ fontSize:'0.75rem', color: mine ? 'var(--purple-bright)' : 'var(--text-muted)', fontWeight: mine ? 700 : 400 }}>
                    {count}
                  </span>
                )}
              </button>
            )
          })}
        </div>

        {/* Lista de comentários */}
        <div style={{ flex:1, overflowY:'auto', padding:'0.8rem 1.2rem' }}>
          {loadingC && <div style={{ color:'var(--text-muted)', fontSize:'0.85rem', textAlign:'center', padding:'1rem' }}>Carregando...</div>}

          {!loadingC && comments.length === 0 && (
            <div style={{ color:'var(--text-muted)', fontSize:'0.85rem', textAlign:'center', padding:'1.5rem' }}>
              Nenhum comentário ainda. Seja o primeiro!
            </div>
          )}

          {comments.map(c => (
            <div key={c.id} style={{ display:'flex', gap:'0.6rem', marginBottom:'1rem', alignItems:'flex-start' }}>
              <div
                className="avatar"
                style={{ width:30, height:30, fontSize:'0.65rem', flexShrink:0, cursor:'pointer' }}
                onClick={() => { navigate(`/profile/${c.userId}`); onClose() }}
              >
                {initials(c.userName)}
              </div>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ display:'flex', alignItems:'center', gap:'0.5rem', marginBottom:'0.2rem' }}>
                  <span
                    style={{ fontWeight:600, fontSize:'0.82rem', cursor:'pointer' }}
                    onClick={() => { navigate(`/profile/${c.userId}`); onClose() }}
                    onMouseEnter={e => e.currentTarget.style.color='var(--purple-bright)'}
                    onMouseLeave={e => e.currentTarget.style.color=''}
                  >{c.userName}</span>
                  <span style={{ fontSize:'0.7rem', color:'var(--text-muted)' }}>{formatDate(c.createdAt)}</span>
                  {c.userId === currentUser?.id && (
                    <button
                      onClick={() => handleDelete(c.id)}
                      style={{ marginLeft:'auto', background:'none', border:'none', color:'var(--text-muted)', cursor:'pointer', fontSize:'0.75rem', padding:'0 0.2rem' }}
                      title="Excluir"
                    >✕</button>
                  )}
                </div>
                <p style={{ fontSize:'0.85rem', color:'var(--text-secondary)', margin:0, lineHeight:1.5, wordBreak:'break-word' }}>
                  {c.commentText}
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* Input de comentário */}
        <div style={{ padding:'0.8rem 1.2rem', borderTop:'1px solid var(--border)', display:'flex', gap:'0.6rem' }}>
          <input
            ref={inputRef}
            type="text"
            placeholder="Adicione um comentário..."
            value={text}
            onChange={e => setText(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSend()}
            style={{ flex:1 }}
          />
          <button
            className="btn btn--primary btn--sm"
            onClick={handleSend}
            disabled={sending || !text.trim()}
            style={{ flexShrink:0 }}
          >
            {sending ? <span className="spinner" style={{ width:14, height:14 }} /> : 'Enviar'}
          </button>
        </div>
      </div>
    </div>
  )
}
