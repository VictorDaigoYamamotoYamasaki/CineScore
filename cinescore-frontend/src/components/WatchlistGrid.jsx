import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import MovieCard from './MovieCard'
import MovieSearchInput from './MovieSearchInput'
import { watchlistService } from '../services/api'

const SORTS = [
  { key: 'date',  label: 'Adicionado'   },
  { key: 'vote',  label: 'Popularidade' },
  { key: 'year',  label: 'Ano'          },
]

function sortItems(items, sortKey, sortDir) {
  return [...items].sort((a, b) => {
    let cmp = 0
    if (sortKey === 'date') {
      cmp = new Date(a.createdAt) - new Date(b.createdAt)
    } else if (sortKey === 'vote') {
      cmp = (a.movieVoteAverage || 0) - (b.movieVoteAverage || 0)
    } else {
      cmp = parseInt(a.movieYear || 0) - parseInt(b.movieYear || 0)
    }
    return sortDir === 'desc' ? -cmp : cmp
  })
}

export default function WatchlistGrid({ items = [], isOwn, onAdd, onRemove }) {
  const navigate = useNavigate()

  const [sortKey,  setSortKey]  = useState('date')
  const [sortDir,  setSortDir]  = useState('asc')
  const [showAdd,  setShowAdd]  = useState(false)
  const [adding,   setAdding]   = useState(false)
  const [error,    setError]    = useState('')

  const sorted = useMemo(() => sortItems(items, sortKey, sortDir), [items, sortKey, sortDir])

  function toggleSort(key) {
    if (sortKey === key) {
      setSortDir(d => d === 'desc' ? 'asc' : 'desc')
    } else {
      setSortKey(key)
      setSortDir('desc')
    }
  }

  async function handleAdd(movie) {
    if (!movie?.id) return
    setAdding(true); setError('')
    try {
      const { data } = await watchlistService.adicionar({
        movieId:          String(movie.id),
        movieTitle:       movie.title        || '',
        moviePoster:      movie.poster       || null,
        movieYear:        movie.year         || null,
        movieVoteAverage: movie.voteAverage  || null,
      })
      onAdd?.(data)
      setShowAdd(false)
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao adicionar filme.')
    } finally {
      setAdding(false)
    }
  }

  async function handleRemove(movieId) {
    setError('')
    try {
      await watchlistService.remover(movieId)
      onRemove?.(movieId)
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao remover filme.')
    }
  }

  return (
    <div style={{ marginTop: '0.5rem' }}>
      {/* Sort controls */}
      <div style={{ display:'flex', alignItems:'center', gap:'0.4rem',
        marginBottom:'1.2rem', justifyContent:'flex-end', flexWrap:'wrap' }}>
        <span style={{ fontSize:'0.7rem', color:'var(--text-muted)',
          textTransform:'uppercase', letterSpacing:'0.08em', marginRight:'0.2rem' }}>
          Ordenar por
        </span>
        {SORTS.map(s => {
          const isActive = sortKey === s.key
          return (
            <button key={s.key} onClick={() => toggleSort(s.key)} style={{
              padding:'0.25rem 0.7rem', borderRadius:20, fontSize:'0.75rem',
              fontWeight: isActive ? 700 : 400,
              border: isActive ? '1px solid var(--purple-accent)' : '1px solid var(--border)',
              background: isActive ? 'rgba(124,92,191,0.15)' : 'transparent',
              color: isActive ? 'var(--purple-bright)' : 'var(--text-muted)',
              cursor:'pointer', transition:'all 0.15s',
            }}>
              {s.label}
              {isActive && (
                <span style={{ marginLeft:'0.3rem' }}>
                  {sortDir === 'desc' ? '↓' : '↑'}
                </span>
              )}
            </button>
          )
        })}
      </div>

      {error && (
        <div className="msg msg--error" style={{ marginBottom:'1rem' }}>{error}</div>
      )}

      {/* Grid de filmes */}
      <div style={{
        display:'grid',
        gridTemplateColumns:'repeat(5, 1fr)',
        gap:'1rem',
        alignItems:'start',
      }}>
        {sorted.map(item => (
          <div key={item.id} style={{ position:'relative' }}>
            <MovieCard
              movie={{
                id:          item.movieId,
                title:       item.movieTitle,
                poster:      item.moviePoster,
                year:        item.movieYear,
                voteAverage: 0,  // oculto visualmente; usado apenas na ordenação
              }}
              onClick={() => navigate(`/movies/${item.movieId}`)}
            />
            {isOwn && (
              <button
                onClick={e => { e.stopPropagation(); handleRemove(item.movieId) }}
                title="Remover da watchlist"
                style={{
                  position:'absolute', top:4, right:4,
                  width:22, height:22, borderRadius:'50%',
                  background:'rgba(0,0,0,0.72)', border:'none',
                  color:'#fff', fontSize:'11px', cursor:'pointer',
                  display:'flex', alignItems:'center', justifyContent:'center',
                  lineHeight:1,
                }}>
                ✕
              </button>
            )}
          </div>
        ))}

        {/* Card "+ Adicionar" */}
        {isOwn && (
          <div
            onClick={() => setShowAdd(v => !v)}
            title="Adicionar filme à watchlist"
            style={{
              aspectRatio:'2/3',
              background:'var(--bg-elevated)',
              borderRadius:8,
              border:`1.5px dashed ${showAdd ? 'var(--purple-bright)' : 'var(--border)'}`,
              display:'flex', flexDirection:'column',
              alignItems:'center', justifyContent:'center',
              cursor:'pointer', color:'var(--text-muted)',
              transition:'all 0.15s', gap:'0.4rem',
            }}>
            <span style={{ fontSize:'1.8rem', lineHeight:1, fontWeight:300 }}>+</span>
            <span style={{ fontSize:'0.7rem', letterSpacing:'0.05em' }}>Adicionar</span>
          </div>
        )}
      </div>

      {/* Busca de filmes inline */}
      {showAdd && isOwn && (
        <div style={{ marginTop:'1.2rem' }}>
          {adding && <div className="spinner" style={{ margin:'0 0 0.5rem' }} />}
          <MovieSearchInput
            onSelect={handleAdd}
            hideActorToggle
            autoFocus
          />
        </div>
      )}

      {!isOwn && sorted.length === 0 && (
        <p style={{ color:'var(--text-muted)', fontSize:'0.85rem', textAlign:'center',
          padding:'2rem 0' }}>
          Nenhum filme na watchlist.
        </p>
      )}
    </div>
  )
}
