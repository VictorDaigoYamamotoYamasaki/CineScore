import { useState, useEffect, useRef } from 'react'
import { movieService } from '../services/api'

export default function MovieSearchInput({ onSelect }) {
  const [query,   setQuery]   = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [open,    setOpen]    = useState(false)
  const timerRef              = useRef(null)
  const wrapRef               = useRef(null)

  useEffect(() => {
    const q = query.trim()
    if (!q || q.length < 2) { setResults([]); setOpen(false); return }

    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(async () => {
      setLoading(true)
      try {

        const normalized = q.replace(/-/g, ' ')
        const { data } = await movieService.buscarPorTitulo(normalized)
        setResults(Array.isArray(data) ? data : [])
        setOpen(true)
      } catch {
        setResults([])
      } finally {
        setLoading(false)
      }
    }, 400)

    return () => clearTimeout(timerRef.current)
  }, [query])

  useEffect(() => {
    function handler(e) {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  function handleSelect(movie) {
    onSelect(movie)
    setQuery('')
    setOpen(false)
    setResults([])
  }

  return (
    <div className="search-wrap" ref={wrapRef}>
      <input
        type="text"
        placeholder="Buscar filme pelo título..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onFocus={() => results.length > 0 && setOpen(true)}
        autoComplete="off"
      />

      {open && (
        <div className="search-results">
          {loading && <div className="search-empty">Buscando...</div>}

          {!loading && results.length === 0 && (
            <div className="search-empty">Nenhum filme encontrado</div>
          )}

          {!loading && results.map((movie) => (
            <div
              key={movie.imdbId}
              className="search-item"
              onMouseDown={() => handleSelect(movie)}
            >
              {movie.poster && movie.poster !== 'N/A' ? (
                <img
                  src={movie.poster}
                  alt={movie.title}
                  className="search-item-poster"
                  onError={(e) => { e.target.style.display = 'none' }}
                />
              ) : (
                <div className="search-item-poster search-item-poster--empty">🎬</div>
              )}
              <div className="search-item-info">
                <div className="search-item-title">{movie.title}</div>
                <div className="search-item-year">{movie.year}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
