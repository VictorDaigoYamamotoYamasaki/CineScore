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

export const authService = {
  login:    (email, password)       => api.post('/auth/login',    { email, password }),
  register: (name, email, password) => api.post('/auth/register', { name, email, password }),
}

export const reviewService = {
  criar: (movieId, rating, reviewText, movieTitle, moviePoster) => api.post('/reviews', { movieId, rating, reviewText, movieTitle, moviePoster }),
  editar:         (id, rating, reviewText)      => api.put(`/reviews/${id}`,   { rating, reviewText }),
  deletar:        (id)                          => api.delete(`/reviews/${id}`),
  listarTodos:    ()                            => api.get('/reviews'),
  listarPorFilme: (movieId)                     => api.get(`/reviews/movie/${movieId}`),
  listarPorUser:  (userId)                      => api.get(`/reviews/user/${userId}`),
}

export const movieService = {
  buscarPorTitulo:  (title)   => api.get(`/movies/search?title=${encodeURIComponent(title)}`),
  buscarPorId:      (movieId) => api.get(`/movies/${movieId}`),
  buscarAtor:       (name)    => api.get(`/movies/actors/search?name=${encodeURIComponent(name)}`),
  buscarAtorPorId:  (id)      => api.get(`/movies/actors/${id}`),
  trending:         ()         => api.get('/movies/trending'),
}

export const adminService = {
  stats:          ()   => api.get('/admin/stats'),
  listarUsuarios: ()   => api.get('/admin/users'),
  deletarUsuario: (id) => api.delete(`/admin/users/${id}`),
  listarReviews:  ()   => api.get('/admin/reviews'),
  deletarReview:  (id) => api.delete(`/admin/reviews/${id}`),
}

export const sessionHelper = {
  save: (data) => {
    localStorage.setItem('cinescore_token', data.token)
    localStorage.setItem('cinescore_user', JSON.stringify({
      id: data.userId, name: data.name, email: data.email, role: data.role,
    }))
  },
  get:      () => { const r = localStorage.getItem('cinescore_user'); return r ? JSON.parse(r) : null },
  isLogged: () => !!localStorage.getItem('cinescore_token'),
  clear:    () => { localStorage.removeItem('cinescore_token'); localStorage.removeItem('cinescore_user') },
}

export default api

export const profileService = {
  meuPerfil:       ()                          => api.get('/profile/me'),
  perfilPorId:     (userId)                    => api.get(`/profile/${userId}`),
  salvarFavorito:  (position, movieId, title, poster, year) =>
    api.put(`/profile/favorites/${position}`, { movieId, title, poster, year }),
  removerFavorito:  (position)                  => api.delete(`/profile/favorites/${position}`),
  deletarConta:     (deletarReviews)            => api.delete(`/profile/me?deletarReviews=${deletarReviews}`),
}

export const followerService = {
  follow:           (userId) => api.post(`/follow/${userId}`),
  unfollow:         (userId) => api.delete(`/follow/${userId}`),
  status:           (userId) => api.get(`/follow/${userId}/status`),
  listarSeguidores: (userId) => api.get(`/follow/${userId}/followers`),
  listarSeguindo:   (userId) => api.get(`/follow/${userId}/following`),
}

export const recommendationService = {
  minhasRecomendacoes: (exclude = []) => api.get(`/recommendations/me?exclude=${exclude.join(',')}`),
}

export const interactionService = {
  resumo:            (reviewId)        => api.get(`/reviews/${reviewId}/summary`),
  listarComentarios: (reviewId)        => api.get(`/reviews/${reviewId}/comments`),
  adicionarComentario:(reviewId, text) => api.post(`/reviews/${reviewId}/comments`, { text }),
  deletarComentario: (reviewId, id)    => api.delete(`/reviews/${reviewId}/comments/${id}`),
  reagir:            (reviewId, emoji) => api.post(`/reviews/${reviewId}/reactions`, { emoji }),
}

export const userSearchService = {
  buscarPorNome: (name) => api.get(`/users/search?name=${encodeURIComponent(name)}`),
}
