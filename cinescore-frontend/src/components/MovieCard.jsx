import { useNavigate } from 'react-router-dom'

export default function MovieCard({ movie, extraLabel }) {
  const navigate = useNavigate()

  return (
    <div
      onClick={() => navigate(`/movies/${movie.id}`)}
      style={{
        cursor:'pointer', borderRadius:8, overflow:'hidden',
        background:'var(--bg-card)', border:'1px solid var(--border)',
        transition:'transform 0.15s, border-color 0.15s',
      }}
      onMouseEnter={e => {
        e.currentTarget.style.transform = 'translateY(-5px)'
        e.currentTarget.style.borderColor = 'var(--purple-accent)'
      }}
      onMouseLeave={e => {
        e.currentTarget.style.transform = 'translateY(0)'
        e.currentTarget.style.borderColor = 'var(--border)'
      }}
    >
      <div style={{ aspectRatio:'2/3', position:'relative', overflow:'hidden', background:'var(--bg-elevated)' }}>
        {movie.poster
          ? <img src={movie.poster} alt={movie.title}
              style={{ position:'absolute', inset:0, width:'100%', height:'100%', objectFit:'cover', display:'block' }}
              onError={e => e.target.style.display='none'} />
          : <div style={{ position:'absolute', inset:0, display:'flex', alignItems:'center', justifyContent:'center', fontSize:'2rem' }}>🎬</div>
        }
      </div>
      <div style={{ padding:'0.5rem' }}>
        <div style={{
          fontSize:'0.75rem', fontWeight:600, lineHeight:1.3,
          overflow:'hidden', display:'-webkit-box',
          WebkitLineClamp:2, WebkitBoxOrient:'vertical',
          color:'var(--text-primary)', marginBottom:3,
        }}>{movie.title}</div>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
          <span style={{ fontSize:'0.65rem', color:'var(--text-muted)' }}>
            {extraLabel || movie.year}
          </span>
          {movie.voteAverage > 0 && (
            <span style={{ fontSize:'0.65rem', color:'var(--green)' }}>
              ★ {movie.voteAverage?.toFixed(1)}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
