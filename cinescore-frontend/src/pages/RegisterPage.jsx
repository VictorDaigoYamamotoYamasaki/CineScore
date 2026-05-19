import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService, sessionHelper } from '../services/api'

const senhaRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$/

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm]       = useState({ name: '', email: '', password: '', confirm: '' })
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  function handleChange(e) {
    setForm(p => ({ ...p, [e.target.name]: e.target.value }))
    setError('')
  }

  async function handleSubmit(e) {
    e.preventDefault()

    if (!form.name || !form.email || !form.password || !form.confirm) {
      setError('Preencha todos os campos.')
      return
    }

    if (!senhaRegex.test(form.password)) {
      setError('Senha deve ter mínimo 6 caracteres, 1 maiúscula, 1 minúscula e 1 caractere especial (ex: !@#$%).')
      return
    }

    if (form.password !== form.confirm) {
      setError('As senhas não coincidem.')
      return
    }

    setLoading(true)
    try {
      const { data } = await authService.register(form.name, form.email, form.password)
      sessionHelper.save(data)
      navigate('/home')
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao criar conta.')
    } finally {
      setLoading(false)
    }
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

        <div className="form-heading">Criar conta</div>
        <div className="form-sub">Junte-se à comunidade CineScore</div>

        {error && <div className="msg msg--error">{error}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label>Nome</label>
            <input name="name" type="text" placeholder="Seu nome completo"
              value={form.name} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label>E-mail</label>
            <input name="email" type="email" placeholder="seu@email.com"
              value={form.email} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input name="password" type="password"
              placeholder="Mín. 6 chars, 1 maiúscula, 1 minúscula, 1 especial"
              value={form.password} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label>Confirmar senha</label>
            <input name="confirm" type="password" placeholder="Repita a senha"
              value={form.confirm} onChange={handleChange} />
          </div>
          <button type="submit" className="btn btn--primary btn--full" disabled={loading}>
            {loading ? <span className="spinner" /> : 'Criar conta'}
          </button>
        </form>

        <div className="form-footer">
          Já tem conta? <Link to="/login">Entrar</Link>
        </div>
      </div>
    </div>
  )
}
