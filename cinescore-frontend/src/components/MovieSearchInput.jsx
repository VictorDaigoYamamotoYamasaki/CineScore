import { useState, useEffect, useRef } from 'react'
import { movieService } from '../services/api'

export default function MovieSearchInput({ onSelect, onActorSelect, hideActorToggle = false, autoFocus = false }) {
  const [mode,    setMode]    = useState('movie')
  const [query,   setQuery]   = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [open,    setOpen]    = useState(false)
  const timerRef              = useRef(null)
  const wrapRef               = useRef(null)

  useEffect(() => {
    setQuery('')
    setResults([])
    setOpen(false)
  }, [mode])

  useEffect(() => {
    const q = query.trim()
    if (!q || q.length < 3) { setResults([]); setOpen(false); return }

    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        if (mode === 'movie') {
          const { data } = await movieService.buscarPorTitulo(q.replace(/-/g, ' '))
          setResults(Array.isArray(data) ? data : [])
        } else {
          const { data } = await movieService.buscarAtor(q)
          setResults(Array.isArray(data) ? data : [])
        }
        setOpen(true)
      } catch {
        setResults([])
      } finally {
        setLoading(false)
      }
    }, 400)

    return () => clearTimeout(timerRef.current)
  }, [query, mode])

  useEffect(() => {
    function handler(e) {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  function handleSelect(item) {
    if (mode === 'movie') onSelect && onSelect(item)
    else onActorSelect && onActorSelect(item)
    setQuery('')
    setOpen(false)
    setResults([])
  }

  const toggleBtn = (m, label) => ({
    type: 'button',
    onClick: () => setMode(m),
    style: {
      padding: '0.3rem 1rem',
      borderRadius: '999px',
      fontSize: '0.78rem',
      fontWeight: 600,
      cursor: 'pointer',
      transition: 'all 0.15s',
      background: mode === m ? 'linear-gradient(135deg,#7C5CBF,#9B72FF)' : 'transparent',
      color: mode === m ? '#fff' : 'var(--text-secondary)',
      border: mode === m ? 'none' : '1px solid var(--border)',
    }
  })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:'0.6rem' }}>
      {!hideActorToggle && (
        <div style={{ display:'flex', gap:'0.4rem' }}>
          <button {...toggleBtn('movie')}>Filmes</button>
          <button {...toggleBtn('actor')}>Atores</button>
        </div>
      )}

      <div className="search-wrap" ref={wrapRef}>
        <input
          type="text"
          placeholder={mode === 'movie' ? 'Busque um filme...' : 'Busque um ator ou atriz...'}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => results.length > 0 && setOpen(true)}
          autoComplete="off"
          autoFocus={autoFocus}
        />

        {open && (
          <div className="search-results">
            {loading && <div className="search-empty">Buscando...</div>}

            {!loading && results.length === 0 && (
              <div className="search-empty">
                {mode === 'movie' ? 'Nenhum filme encontrado' : 'Nenhum ator encontrado'}
              </div>
            )}

            {!loading && mode === 'movie' && results.map((movie) => (
              <div key={movie.id} className="search-item" onMouseDown={() => handleSelect(movie)}>
                {movie.poster
                  ? <img src={movie.poster} alt={movie.title} className="search-item-poster" onError={e => e.target.style.display='none'} />
                  : <div className="search-item-poster search-item-poster--empty">🎬</div>
                }
                <div className="search-item-info">
                  <div className="search-item-title">{movie.title}</div>
                  <div className="search-item-year">{movie.year}</div>
                </div>
              </div>
            ))}

            {!loading && mode === 'actor' && results.map((actor) => (
              <div key={actor.id} className="search-item" onMouseDown={() => handleSelect(actor)}>
                {actor.photo
                  ? <img src={actor.photo} alt={actor.name} style={{ width:36,height:36,borderRadius:'50%',objectFit:'cover',flexShrink:0 }} onError={e => e.target.style.display='none'} />
                  : <div style={{ width:36,height:36,borderRadius:'50%',background:'var(--bg-elevated)',display:'flex',alignItems:'center',justifyContent:'center',flexShrink:0,fontSize:'1rem' }}>🎭</div>
                }
                <div className="search-item-info">
                  <div className="search-item-title">{actor.name}</div>
                  {actor.knownForTitles && (
                    <div className="search-item-year" style={{ maxWidth:220,overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap' }}>
                      {actor.knownForTitles}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
