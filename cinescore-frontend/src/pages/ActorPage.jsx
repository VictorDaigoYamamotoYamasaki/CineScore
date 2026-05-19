import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { movieService, sessionHelper } from '../services/api'

function calcAge(birthday) {
  if (!birthday) return null
  const diff = Date.now() - new Date(birthday).getTime()
  return Math.floor(diff / (1000 * 60 * 60 * 24 * 365.25))
}

export default function ActorPage() {
  const { actorId } = useParams()
  const navigate    = useNavigate()
  const user        = sessionHelper.get()

  const [actor,   setActor]   = useState(null)
  const [loading, setLoading] = useState(true)
  const [bioOpen, setBioOpen] = useState(false)

  useEffect(() => {
    movieService.buscarAtorPorId(actorId)
      .then(({ data }) => setActor(data))
      .catch(() => navigate('/home'))
      .finally(() => setLoading(false))
  }, [actorId])

  if (loading) return (
    <>
      <Navbar user={user} />
      <div className="page-main" style={{ display:'flex', alignItems:'center', justifyContent:'center', paddingTop:80 }}>
        <span className="spinner" />
      </div>
    </>
  )

  const age     = calcAge(actor?.birthday)
  const bioText = actor?.biography || ''
  const bioShort = bioText.length > 400

  return (
    <>
      <Navbar user={user} />
      <div className="page-main">

        {/* Hero do ator */}
        <div className="movie-hero">
          <div className="container">
            <div className="movie-hero-inner">

              {actor?.photo ? (
                <img
                  src={actor.photo}
                  alt={actor.name}
                  className="movie-poster"
                  style={{ borderRadius: '50%', width: 160, height: 160, objectFit: 'cover', flexShrink: 0 }}
                />
              ) : (
                <div style={{
                  width: 160, height: 160, borderRadius: '50%',
                  background: 'var(--bg-elevated)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '3rem', flexShrink: 0,
                }}>🎭</div>
              )}

              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--purple-bright)', textTransform: 'uppercase', letterSpacing: '0.12em', marginBottom: '0.4rem' }}>
                  {actor?.knownForDepartment === 'Acting' ? 'Ator / Atriz' : actor?.knownForDepartment}
                </div>
                <h1 className="movie-info-title">{actor?.name}</h1>

                <div className="movie-info-meta">
                  {actor?.birthday && (
                    <span>
                      {new Date(actor.birthday).toLocaleDateString('pt-BR')}
                      {age && ` (${age} anos)`}
                    </span>
                  )}
                  {actor?.placeOfBirth && <span>{actor.placeOfBirth}</span>}
                  {actor?.movies && <span>{actor.movies.length} filmes</span>}
                </div>

                {bioText && (
                  <div style={{ marginTop: '0.8rem' }}>
                    <p className="movie-plot">
                      {bioShort && !bioOpen ? bioText.slice(0, 400) + '…' : bioText}
                    </p>
                    {bioShort && (
                      <button
                        className="btn btn--ghost btn--sm"
                        style={{ marginTop: '0.4rem', padding: '0.2rem 0.7rem', fontSize: '0.72rem' }}
                        onClick={() => setBioOpen(v => !v)}
                      >
                        {bioOpen ? 'Ver menos' : 'Ver mais'}
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Filmografia */}
        <div className="container" style={{ paddingTop: '2rem', paddingBottom: '3rem' }}>
          <div className="feed-header" style={{ marginBottom: '1.5rem' }}>
            <span className="feed-title">🎬 Filmografia</span>
            <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
              {actor?.movies?.length || 0} filmes
            </span>
          </div>

          {(!actor?.movies || actor.movies.length === 0) && (
            <div className="empty-state">
              <div className="empty-state-icon">🎬</div>
              <p>Nenhum filme encontrado.</p>
            </div>
          )}

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
            gap: '1.2rem',
          }}>
            {actor?.movies?.map((movie) => (
              <div
                key={`${movie.id}-${movie.character}`}
                onClick={() => navigate(`/movies/${movie.id}`)}
                style={{
                  cursor: 'pointer',
                  borderRadius: 8,
                  overflow: 'hidden',
                  background: 'var(--bg-card)',
                  border: '1px solid var(--border)',
                  transition: 'transform 0.15s, border-color 0.15s',
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.transform = 'translateY(-4px)'
                  e.currentTarget.style.borderColor = 'var(--purple-accent)'
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.transform = 'translateY(0)'
                  e.currentTarget.style.borderColor = 'var(--border)'
                }}
              >
                {movie.poster ? (
                  <img
                    src={movie.poster}
                    alt={movie.title}
                    style={{ width: '100%', aspectRatio: '2/3', objectFit: 'cover', display: 'block' }}
                    onError={e => { e.target.style.display = 'none' }}
                  />
                ) : (
                  <div style={{
                    width: '100%', aspectRatio: '2/3',
                    background: 'var(--bg-elevated)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '2rem',
                  }}>🎬</div>
                )}
                <div style={{ padding: '0.6rem 0.5rem' }}>
                  <div style={{
                    fontSize: '0.78rem', fontWeight: 600,
                    color: 'var(--text-primary)',
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                  }}>{movie.title}</div>
                  <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)', marginTop: 2 }}>{movie.year}</div>
                  {movie.character && (
                    <div style={{
                      fontSize: '0.65rem', color: 'var(--purple-bright)',
                      marginTop: 2,
                      overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                    }}>{movie.character}</div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  )
}
