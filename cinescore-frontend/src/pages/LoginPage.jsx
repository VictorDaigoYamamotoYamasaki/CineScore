import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { authService, sessionHelper } from '../services/api'

const REGEX_EMAIL = /\S+@\S+\.\S+/

function ErroInline({ msg }) {
  if (!msg) return null
  return <span style={{ color:'#ff6b6b', fontSize:'0.76rem', marginTop:'4px', display:'block' }}>{msg}</span>
}

export default function LoginPage() {
  const navigate = useNavigate()
  const location  = useLocation()
  const [form, setForm]     = useState({ email: '', password: '' })
  const [erros, setErros]   = useState({})
  const [apiError, setApiError] = useState('')
  const [loading, setLoading]   = useState(false)

  function handleChange(e) {
    const { name, value } = e.target
    setForm(p => ({ ...p, [name]: value }))
    if (erros[name]) setErros(p => ({ ...p, [name]: '' }))
    setApiError('')
  }

  function validar() {
    const e = {}
    if (!form.email.trim())
      e.email = 'E-mail obrigatório.'
    else if (!REGEX_EMAIL.test(form.email))
      e.email = 'Informe um e-mail válido.'
    if (!form.password)
      e.password = 'Senha obrigatória.'
    return e
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const e2 = validar()
    if (Object.keys(e2).length > 0) { setErros(e2); return }
    setLoading(true)
    try {
      const { data } = await authService.login(form.email, form.password)
      sessionHelper.save(data)
      navigate(location.state?.from || '/home', { replace: true })
    } catch (err) {
      setApiError(err.response?.data?.message || 'E-mail ou senha incorretos.')
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

        {apiError && <div className="msg msg--error">{apiError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label htmlFor="email">E-mail</label>
            <input id="email" name="email" type="email" placeholder="seu@email.com"
              value={form.email} onChange={handleChange} autoComplete="email"
              style={erros.email ? { borderColor:'#ff6b6b' } : {}} />
            <ErroInline msg={erros.email} />
          </div>
          <div className="form-group">
            <label htmlFor="password">Senha</label>
            <input id="password" name="password" type="password" placeholder="••••••••"
              value={form.password} onChange={handleChange} autoComplete="current-password"
              style={erros.password ? { borderColor:'#ff6b6b' } : {}} />
            <ErroInline msg={erros.password} />
          </div>
          <div style={{ textAlign:'right', marginBottom:'0.8rem' }}>
            <Link to="/forgot-password" style={{ color:'var(--text-muted)', fontSize:'0.8rem', textDecoration:'none' }}>
              Esqueci minha senha
            </Link>
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
