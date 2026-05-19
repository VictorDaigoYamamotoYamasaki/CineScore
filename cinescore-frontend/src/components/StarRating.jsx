import { useState } from 'react'

export function StarRating({ value, onChange, size = 'md' }) {
  const [hovered, setHovered] = useState(0)

  function handleMouseMove(e, starNum) {
    const rect = e.currentTarget.getBoundingClientRect()
    const x    = e.clientX - rect.left
    const half = x < rect.width / 2
    setHovered(half ? starNum - 0.5 : starNum)
  }

  const display = hovered || value

  return (
    <div className="stars" onMouseLeave={() => setHovered(0)}>
      {[1, 2, 3, 4, 5].map((s) => {

        const fill = Math.min(1, Math.max(0, display - (s - 1)))

        return (
          <span
            key={s}
            className={`star-wrap star-wrap--${size}`}
            onMouseMove={(e) => handleMouseMove(e, s)}
            onClick={() => onChange(hovered || value)}
            role="button"
            aria-label={`${hovered || value} estrelas`}
            style={{ cursor: 'pointer' }}
          >
            {}
            <span className="star-bg">★</span>
            {}
            {fill > 0 && (
              <span
                className="star-fg"
                style={{ width: `${fill * 100}%` }}
              >★</span>
            )}
          </span>
        )
      })}

      {value > 0 && (
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginLeft: '0.4rem', alignSelf: 'center' }}>
          {value}/5
        </span>
      )}
    </div>
  )
}

export function StarDisplay({ value, size = 'sm' }) {
  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map((s) => {
        const fill = Math.min(1, Math.max(0, value - (s - 1)))
        return (
          <span key={s} className={`star-wrap star-wrap--${size}`}>
            <span className="star-bg">★</span>
            {fill > 0 && (
              <span className="star-fg" style={{ width: `${fill * 100}%` }}>★</span>
            )}
          </span>
        )
      })}
    </div>
  )
}
