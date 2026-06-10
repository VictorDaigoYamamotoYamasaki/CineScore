import { Fragment, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { StarDisplay, StarRating } from './StarRating'
import { reviewService } from '../services/api'

const MESES = ['JAN','FEV','MAR','ABR','MAI','JUN','JUL','AGO','SET','OUT','NOV','DEZ']

// Aceita tanto "2026-06-03" (LocalDate) quanto "2026-06-03T11:39:03" (LocalDateTime)
function formatarData(dateStr) {
  if (!dateStr) return null
  const somenteData = typeof dateStr === 'string'
    ? dateStr.substring(0, 10)
    : String(dateStr).substring(0, 10)
  const d = new Date(somenteData + 'T12:00:00')
  if (isNaN(d.getTime())) return null
  return {
    dia:  String(d.getDate()).padStart(2, '0'),
    mes:  MESES[d.getMonth()],
    ano:  String(d.getFullYear()),
    chave: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
  }
}

function agruparPorMes(reviews) {
  const mapa   = new Map()   // chave → grupo
  const ordem  = []          // mantém ordem cronológica

  reviews.forEach(review => {
    const dateStr = review.watchedAt || review.createdAt
    const fmt     = formatarData(dateStr)
    const chave   = fmt?.chave || 'sem-data'

    if (!mapa.has(chave)) {
      const grupo = { chave, mes: fmt?.mes || '—', ano: fmt?.ano || '—', reviews: [] }
      mapa.set(chave, grupo)
      ordem.push(grupo)
    }
    mapa.get(chave).reviews.push(review)
  })

  return ordem
}

function CalendarIcon({ mes, ano }) {
  return (
    <div style={{ width:54, height:52, borderRadius:6, overflow:'hidden',
      border:'1px solid var(--border)', flexShrink:0, background:'var(--bg-elevated)' }}>
      <div style={{ background:'var(--purple-accent)', textAlign:'center',
        fontSize:'0.55rem', fontWeight:800, color:'#fff', padding:'2px 0',
        letterSpacing:'0.08em' }}>
        {mes}
      </div>
      <div style={{ textAlign:'center', fontSize:'0.68rem', fontWeight:600,
        color:'var(--text-secondary)', padding:'4px 0', lineHeight:1 }}>
        {ano}
      </div>
    </div>
  )
}

export default function DiaryView({ reviews, isOwn, onReviewUpdate }) {
  const navigate = useNavigate()
  const hoje     = new Date().toISOString().substring(0, 10)

  const [sortKey,     setSortKey]     = useState('date')   // 'date' | 'rating'
  const [sortDir,     setSortDir]     = useState('desc')   // 'asc' | 'desc'
  const [editingId,   setEditingId]   = useState(null)
  const [editDate,    setEditDate]    = useState('')
  const [editRating,  setEditRating]  = useState(0)
  const [editText,    setEditText]    = useState('')
  const [editLoading, setEditLoading] = useState(false)
  const [editError,   setEditError]   = useState('')

  // Ordena dinamicamente por data ou nota, em ordem crescente ou decrescente
  const ordenadas = [...reviews].sort((a, b) => {
    let cmp = 0
    if (sortKey === 'date') {
      const da = (a.watchedAt || a.createdAt || '').substring(0, 10)
      const db = (b.watchedAt || b.createdAt || '').substring(0, 10)
      cmp = da < db ? -1 : da > db ? 1 : (a.id || 0) - (b.id || 0)
    } else {
      cmp = (a.rating || 0) - (b.rating || 0)
    }
    return sortDir === 'desc' ? -cmp : cmp
  })

  const grupos = agruparPorMes(ordenadas)

  function abrirEdicao(review) {
    const data = (review.watchedAt || review.createdAt || hoje).substring(0, 10)
    setEditingId(review.id)
    setEditDate(data)
    setEditRating(review.rating || 0)
    setEditText(review.reviewText || '')
    setEditError('')
  }

  function cancelarEdicao() { setEditingId(null); setEditError('') }

  async function salvarEdicao(review) {
    if (!editRating) { setEditError('Selecione uma nota.'); return }
    setEditLoading(true); setEditError('')
    try {
      const { data } = await reviewService.editar(review.id, editRating, editText, editDate)
      onReviewUpdate?.(data)
      setEditingId(null)
    } catch (err) {
      setEditError(err.response?.data?.message || 'Erro ao salvar.')
    } finally {
      setEditLoading(false)
    }
  }

  if (ordenadas.length === 0) {
    return (
      <div className="empty-state" style={{ padding:'3rem 0' }}>
        <p style={{ color:'var(--text-muted)' }}>Nenhum filme no diário ainda.</p>
      </div>
    )
  }

  function toggleSort(key) {
    if (sortKey === key) setSortDir(d => d === 'desc' ? 'asc' : 'desc')
    else { setSortKey(key); setSortDir('desc') }
  }

  const SORT_LABEL = {
    date: { desc: 'Data ↓', asc: 'Data ↑' },
    rating: { desc: 'Nota ↓', asc: 'Nota ↑' },
  }

  return (
    <div>
      {/* Controle de ordenação */}
      <div style={{ display:'flex', alignItems:'center', gap:'0.4rem',
        marginBottom:'1rem', justifyContent:'flex-end' }}>
        <span style={{ fontSize:'0.7rem', color:'var(--text-muted)',
          textTransform:'uppercase', letterSpacing:'0.08em', marginRight:'0.2rem' }}>
          Ordenar por
        </span>
        {['date','rating'].map(key => (
          <button key={key}
            onClick={() => toggleSort(key)}
            style={{
              padding:'0.25rem 0.7rem', borderRadius:20, fontSize:'0.75rem',
              fontWeight: sortKey === key ? 700 : 400,
              border: sortKey === key ? '1px solid var(--purple-accent)' : '1px solid var(--border)',
              background: sortKey === key ? 'rgba(124,92,191,0.15)' : 'transparent',
              color: sortKey === key ? 'var(--purple-bright)' : 'var(--text-muted)',
              cursor:'pointer', transition:'all 0.15s',
            }}>
            {SORT_LABEL[key][sortKey === key ? sortDir : 'desc'].replace(/[↑↓]/,'').trim()}
            {sortKey === key && (
              <span style={{ marginLeft:'0.3rem' }}>{sortDir === 'desc' ? '↓' : '↑'}</span>
            )}
          </button>
        ))}
      </div>

      {/* Cabeçalho */}
      <div style={{ display:'grid',
        gridTemplateColumns:'70px 44px 44px 1fr 120px 32px',
        gap:'0 0.5rem', padding:'0 0 0.5rem',
        borderBottom:'2px solid var(--border)',
        fontSize:'0.6rem', fontWeight:700,
        color:'var(--text-muted)', letterSpacing:'0.1em',
        textTransform:'uppercase' }}>
        <span>Mês</span><span>Dia</span><span></span>
        <span>Filme</span><span>Nota</span><span></span>
      </div>

      {grupos.map(({ chave, mes, ano, reviews: rvsDoMes }) => (
        <Fragment key={chave}>
          {rvsDoMes.map((review, idx) => {
            const fmt     = formatarData(review.watchedAt || review.createdAt)
            const editing = editingId === review.id

            return (
              <Fragment key={review.id}>
                {/* Linha da entry */}
                <div style={{
                  display:'grid',
                  gridTemplateColumns:'70px 44px 44px 1fr 120px 32px',
                  gap:'0 0.5rem', alignItems:'center',
                  padding:'1rem 0',
                  borderBottom:'1px solid var(--border)',
                  background: editing ? 'var(--bg-elevated)' : 'transparent',
                  transition:'background 0.15s',
                }}>
                  {/* Calendário — só no 1º do mês */}
                  <div style={{ display:'flex', alignItems:'center' }}>
                    {idx === 0
                      ? <CalendarIcon mes={mes} ano={ano} />
                      : <div style={{ width:54 }} />}
                  </div>

                  {/* Dia */}
                  <span style={{ fontSize:'1.4rem', fontWeight:300,
                    color:'var(--text-secondary)', textAlign:'center', lineHeight:1 }}>
                    {fmt?.dia || '--'}
                  </span>

                  {/* Pôster */}
                  <div style={{ width:36, height:52, borderRadius:3, overflow:'hidden',
                    background:'var(--bg-elevated)', cursor:'pointer', flexShrink:0 }}
                    onClick={() => navigate(`/movies/${review.movieId}`)}>
                    {review.moviePoster
                      ? <img src={review.moviePoster} alt={review.movieTitle}
                          style={{ width:'100%', height:'100%', objectFit:'cover', display:'block' }}
                          onError={e => { e.target.style.display = 'none' }} />
                      : <div style={{ width:'100%', height:'100%', display:'flex',
                          alignItems:'center', justifyContent:'center',
                          fontSize:'0.5rem', color:'var(--text-muted)' }}>
                          sem capa
                        </div>
                    }
                  </div>

                  {/* Título */}
                  <span style={{ fontWeight:700, fontSize:'0.88rem',
                    color:'var(--text-primary)', cursor:'pointer',
                    overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' }}
                    onClick={() => navigate(`/movies/${review.movieId}`)}>
                    {review.movieTitle || review.movieId}
                  </span>

                  {/* Nota */}
                  <StarDisplay value={review.rating} size="sm" />

                  {/* Botão editar (só dono) */}
                  {isOwn && (
                    <button
                      onClick={() => editing ? cancelarEdicao() : abrirEdicao(review)}
                      style={{ background:'none', border:'none', cursor:'pointer', padding:'0.2rem',
                        fontSize:'0.85rem',
                        color: editing ? 'var(--purple-bright)' : 'var(--text-muted)' }}
                      title={editing ? 'Cancelar' : 'Editar'}>
                      {editing ? '✕' : '✎'}
                    </button>
                  )}
                </div>

                {/* Formulário inline de edição */}
                {editing && (
                  <div style={{ padding:'1rem 1.2rem', background:'var(--bg-elevated)',
                    borderBottom:'1px solid var(--border)',
                    display:'flex', flexDirection:'column', gap:'0.75rem' }}>
                    {editError && <div className="msg msg--error">{editError}</div>}

                    <div style={{ display:'flex', gap:'1.5rem', flexWrap:'wrap' }}>
                      <div>
                        <label style={{ fontSize:'0.7rem', color:'var(--text-secondary)',
                          textTransform:'uppercase', letterSpacing:'0.08em',
                          display:'block', marginBottom:'0.3rem' }}>
                          Data assistido
                        </label>
                        <input type="date" value={editDate} max={hoje}
                          onChange={e => setEditDate(e.target.value)}
                          style={{ padding:'0.3rem 0.6rem', borderRadius:6,
                            border:'1px solid var(--border)', background:'var(--bg-input)',
                            color:'var(--text-primary)', fontSize:'0.85rem' }} />
                      </div>
                      <div>
                        <label style={{ fontSize:'0.7rem', color:'var(--text-secondary)',
                          textTransform:'uppercase', letterSpacing:'0.08em',
                          display:'block', marginBottom:'0.3rem' }}>
                          Nota
                        </label>
                        <StarRating value={editRating} onChange={setEditRating} size="md" />
                      </div>
                    </div>

                    <div>
                      <label style={{ fontSize:'0.7rem', color:'var(--text-secondary)',
                        textTransform:'uppercase', letterSpacing:'0.08em',
                        display:'block', marginBottom:'0.3rem' }}>
                        Comentário (opcional)
                      </label>
                      <textarea value={editText}
                        onChange={e => setEditText(e.target.value)}
                        rows={2} placeholder="O que você achou?"
                        style={{ minHeight:56, fontSize:'0.85rem' }} />
                      <div style={{ textAlign:'right', fontSize:'0.7rem',
                        color: editText.length > 1900 ? '#ff6b6b' : 'var(--text-muted)' }}>
                        {editText.length}/2000
                      </div>
                    </div>

                    <div style={{ display:'flex', gap:'0.5rem' }}>
                      <button className="btn btn--primary btn--sm"
                        onClick={() => salvarEdicao(review)} disabled={editLoading}>
                        {editLoading ? <span className="spinner" /> : 'Salvar'}
                      </button>
                      <button className="btn btn--ghost btn--sm"
                        onClick={cancelarEdicao} disabled={editLoading}>
                        Cancelar
                      </button>
                    </div>
                  </div>
                )}
              </Fragment>
            )
          })}
        </Fragment>
      ))}
    </div>
  )
}
