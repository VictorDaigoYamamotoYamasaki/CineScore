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
  criar:          (movieImdbId, rating, reviewText) => api.post('/reviews',        { movieImdbId, rating, reviewText }),
  editar:         (id, rating, reviewText)          => api.put(`/reviews/${id}`,   { rating, reviewText }),
  deletar:        (id)                              => api.delete(`/reviews/${id}`),
  listarTodos:    ()                                => api.get('/reviews'),
  listarPorFilme: (imdbId)                          => api.get(`/reviews/movie/${imdbId}`),
  listarPorUser:  (userId)                          => api.get(`/reviews/user/${userId}`),
}

export const movieService = {
  buscarPorTitulo: (title)  => api.get(`/movies/search?title=${encodeURIComponent(title)}`),
  buscarPorId:     (imdbId) => api.get(`/movies/${imdbId}`),
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
