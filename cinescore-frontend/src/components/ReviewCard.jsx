import { useNavigate } from 'react-router-dom'
import { StarDisplay } from './StarRating'

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

export default function ReviewCard({ review, movieTitle }) {
  const navigate = useNavigate()

  return (
    <div
      className="review-card"
      onClick={() => navigate(`/movies/${review.movieImdbId}`)}
      style={{ cursor: 'pointer' }}
    >
      <div className="review-card-header">
        <div className="review-card-user">
          <div className="avatar">{initials(review.userName)}</div>
          <div>
            <div className="review-card-username">{review.userName}</div>
            <div className="review-card-date">{formatDate(review.createdAt)}</div>
          </div>
        </div>
        <StarDisplay value={review.rating} size="sm" />
      </div>

      {review.reviewText && (
        <p className="review-card-text">
          {review.reviewText.length > 200
            ? review.reviewText.slice(0, 200) + '...'
            : review.reviewText}
        </p>
      )}

      {}
      <div style={{ marginTop: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
        🎬
        <span>{movieTitle || review.movieImdbId}</span>
      </div>
    </div>
  )
}
