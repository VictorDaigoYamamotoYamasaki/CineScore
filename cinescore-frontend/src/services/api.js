import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('cinescore_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('cinescore_token')
      localStorage.removeItem('cinescore_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// ── Auth ──────────────────────────────────────────────────────────────────────
export const authService = {
  login:          (email, password)       => api.post('/auth/login',          { email, password }),
  register:       (name, email, password) => api.post('/auth/register',       { name, email, password }),
  forgotPassword: (email)                 => api.post('/auth/forgot-password', { email }),
  resetPassword:  (token, newPassword)   => api.post('/auth/reset-password',  { token, newPassword }),
}

// ── Reviews ───────────────────────────────────────────────────────────────────
export const reviewService = {
  criar:          (movieId, rating, reviewText, movieTitle, moviePoster, watchedAt) =>
                    api.post('/reviews', { movieId, rating, reviewText, movieTitle, moviePoster, watchedAt }),
  editar:         (id, rating, reviewText, watchedAt) => api.put(`/reviews/${id}`, { rating, reviewText, watchedAt }),
  atualizarData:  (id, watchedAt)            => api.patch(`/reviews/${id}/watched-date`, { watchedAt }),
  deletar:        (id)                     => api.delete(`/reviews/${id}`),
  listarTodos:    ()                       => api.get('/reviews'),
  listarPorFilme: (movieId)               => api.get(`/reviews/movie/${movieId}`),
  listarPorUser:  (userId)                => api.get(`/reviews/user/${userId}`),
}

// ── Movies ────────────────────────────────────────────────────────────────────
export const movieService = {
  buscarPorTitulo: (title)   => api.get(`/movies/search?title=${encodeURIComponent(title)}`),
  buscarPorId:     (movieId) => api.get(`/movies/${movieId}`),
  buscarAtor:      (name)    => api.get(`/movies/actors/search?name=${encodeURIComponent(name)}`),
  buscarAtorPorId: (id)      => api.get(`/movies/actors/${id}`),
  trending:        ()        => api.get('/movies/trending'),
}

// ── Admin ─────────────────────────────────────────────────────────────────────
export const adminService = {
  stats:          ()                         => api.get('/admin/stats'),
  popularMovies:  ()                         => api.get('/admin/popular-movies'),
  listarUsuarios: (page = 0, size = 30, sortDir = 'asc') => api.get(`/admin/users?page=${page}&size=${size}&sortDir=${sortDir}`),
  deletarUsuario: (id)                       => api.delete(`/admin/users/${id}`),
  listarReviews:  (page = 0, size = 30, sortDir = 'asc') => api.get(`/admin/reviews?page=${page}&size=${size}&sortDir=${sortDir}`),
  deletarReview:  (id)                       => api.delete(`/admin/reviews/${id}`),
  ratingDistribution: ()                     => api.get('/admin/stats/rating-distribution'),
  reviewsPerDay:      ()                     => api.get('/admin/stats/reviews-per-day'),
  genres:             ()                     => api.get('/admin/stats/genres'),
  ratingExtremes:     ()                     => api.get('/admin/stats/rating-extremes'),
}

// ── Profile ───────────────────────────────────────────────────────────────────
export const profileService = {
  meuPerfil:       ()                                    => api.get('/profile/me'),
  perfilPorId:     (userId)                              => api.get(`/profile/${userId}`),
  salvarFavorito:  (position, movieId, title, poster, year) =>
                     api.put(`/profile/favorites/${position}`, { movieId, title, poster, year }),
  removerFavorito: (position)       => api.delete(`/profile/favorites/${position}`),
  deletarConta:    (deletarReviews) => api.delete(`/profile/me?deletarReviews=${deletarReviews}`),
  atualizarPerfil: (data)           => api.put('/profile/me/settings', data),
}

// ── Followers ─────────────────────────────────────────────────────────────────
export const followerService = {
  follow:           (userId) => api.post(`/follow/${userId}`),
  unfollow:         (userId) => api.delete(`/follow/${userId}`),
  status:           (userId) => api.get(`/follow/${userId}/status`),
  listarSeguidores: (userId) => api.get(`/follow/${userId}/followers`),
  listarSeguindo:   (userId) => api.get(`/follow/${userId}/following`),
}

// ── Recommendations ───────────────────────────────────────────────────────────
export const recommendationService = {
  minhasRecomendacoes: (exclude = []) =>
    api.get(`/recommendations/me?exclude=${exclude.join(',')}`),
}

// ── Interactions ──────────────────────────────────────────────────────────────
export const interactionService = {
  resumo:             (reviewId)          => api.get(`/reviews/${reviewId}/summary`),
  listarComentarios:  (reviewId)          => api.get(`/reviews/${reviewId}/comments`),
  adicionarComentario:(reviewId, text)    => api.post(`/reviews/${reviewId}/comments`, { text }),
  deletarComentario:  (reviewId, id)      => api.delete(`/reviews/${reviewId}/comments/${id}`),
  reagir:             (reviewId, emoji)   => api.post(`/reviews/${reviewId}/reactions`, { emoji }),
}

// ── User search ───────────────────────────────────────────────────────────────
export const userSearchService = {
  buscarPorNome: (name) => api.get(`/users/search?name=${encodeURIComponent(name)}`),
}

// ── Session ───────────────────────────────────────────────────────────────────
export const sessionHelper = {
  save: (data) => {
    localStorage.setItem('cinescore_token', data.token)
    localStorage.setItem('cinescore_user', JSON.stringify({
      id: data.userId, name: data.name, email: data.email, role: data.role,
    }))
  },
  get:      () => { const r = localStorage.getItem('cinescore_user'); return r ? JSON.parse(r) : null },
  isLogged: () => !!localStorage.getItem('cinescore_token'),
  clear:    () => {
    localStorage.removeItem('cinescore_token')
    localStorage.removeItem('cinescore_user')
  },
}

export default api

export const watchlistService = {
  listar:            ()       => api.get('/watchlist'),
  listarPorUsuario:  (userId) => api.get(`/watchlist/user/${userId}`),
  adicionar:         (dto)    => api.post('/watchlist', dto),
  remover:           (movieId)=> api.delete(`/watchlist/${movieId}`),
}
