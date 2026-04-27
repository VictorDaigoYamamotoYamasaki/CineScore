import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService, sessionHelper } from '../services/api'

export default function LoginPage() {
  const navigate = useNavigate()
  const [form, setForm]     = useState({ email: '', password: '' })
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)

  function handleChange(e) {
    setForm(p => ({ ...p, [e.target.name]: e.target.value }))
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!form.email || !form.password) { setError('Preencha todos os campos.'); return }
    setLoading(true)
    try {
      const { data } = await authService.login(form.email, form.password)
      sessionHelper.save(data)
      navigate('/home')
    } catch (err) {
      setError(err.response?.data?.message || 'Email ou senha incorretos.')
    } finally { setLoading(false) }
  }

  return (
    <div className="page-auth">
      <div className="auth-card">
        <div className="logo">
          <div className="logo-mark">
            <div className="logo-icon">🎬</div>
            <div className="logo-title">Cine<span>Score</span></div>
          </div>
          <div className="logo-tagline">Avalie. Descubra. Compartilhe.</div>
        </div>

        <div className="form-heading">Entrar</div>
        <div className="form-sub">Acesse sua conta para continuar</div>

        {error && <div className="msg msg--error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="email">E-mail</label>
            <input id="email" name="email" type="email" placeholder="seu@email.com"
              value={form.email} onChange={handleChange} autoComplete="email" />
          </div>
          <div className="form-group">
            <label htmlFor="password">Senha</label>
            <input id="password" name="password" type="password" placeholder="••••••••"
              value={form.password} onChange={handleChange} autoComplete="current-password" />
          </div>
          <button type="submit" className="btn btn--primary btn--full" disabled={loading}>
            {loading ? <span className="spinner" /> : 'Entrar'}
          </button>
        </form>

        <div className="form-footer">
          Não tem conta? <Link to="/register">Criar conta</Link>
        </div>
      </div>
    </div>
  )
}
