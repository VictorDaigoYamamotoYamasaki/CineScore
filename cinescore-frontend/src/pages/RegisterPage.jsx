import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { authService, sessionHelper } from '../services/api'

const REGEX_EMAIL = /\S+@\S+\.\S+/
const REGEX_SENHA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$/

function ErroInline({ msg }) {
  if (!msg) return null
  return <span style={{ color:'#ff6b6b', fontSize:'0.76rem', marginTop:'4px', display:'block' }}>{msg}</span>
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm]       = useState({ name: '', email: '', password: '', confirm: '' })
  const [erros, setErros]     = useState({})
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
    if (!form.name.trim())
      e.name = 'Nome obrigatório.'
    else if (form.name.trim().length < 2)
      e.name = 'Nome deve ter pelo menos 2 caracteres.'
    else if (form.name.trim().length > 25)
      e.name = 'Nome deve ter no máximo 25 caracteres.'

    if (!form.email.trim())
      e.email = 'E-mail obrigatório.'
    else if (!REGEX_EMAIL.test(form.email))
      e.email = 'Informe um e-mail válido.'

    if (!form.password)
      e.password = 'Senha obrigatória.'
    else if (!REGEX_SENHA.test(form.password))
      e.password = 'Mín. 6 caracteres, 1 maiúscula, 1 minúscula e 1 caractere especial.'

    if (!form.confirm)
      e.confirm = 'Confirme sua senha.'
    else if (form.password && form.confirm !== form.password)
      e.confirm = 'As senhas não coincidem.'

    return e
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const e2 = validar()
    if (Object.keys(e2).length > 0) { setErros(e2); return }
    setLoading(true)
    try {
      const { data } = await authService.register(form.name, form.email, form.password)
      sessionHelper.save(data)
      navigate('/home')
    } catch (err) {
      setApiError(err.response?.data?.message || 'Erro ao criar conta.')
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

        <div className="form-heading">Criar conta</div>
        <div className="form-sub">Junte-se à comunidade CineScore</div>

        {apiError && <div className="msg msg--error">{apiError}</div>}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label>Nome</label>
            <input name="name" type="text" placeholder="Seu nome completo" maxLength={25}
              value={form.name} onChange={handleChange}
              style={erros.name ? { borderColor:'#ff6b6b' } : {}} />
            <div style={{ textAlign:'right', fontSize:'0.72rem', color: form.name.length > 22 ? '#ff6b6b' : 'var(--text-muted)' }}>{form.name.length}/25</div>
            <ErroInline msg={erros.name} />
          </div>
          <div className="form-group">
            <label>E-mail</label>
            <input name="email" type="email" placeholder="seu@email.com"
              value={form.email} onChange={handleChange}
              style={erros.email ? { borderColor:'#ff6b6b' } : {}} />
            <ErroInline msg={erros.email} />
          </div>
          <div className="form-group">
            <label>Senha</label>
            <input name="password" type="password"
              placeholder="Mín. 6 chars, 1 maiúscula, 1 minúscula, 1 especial"
              value={form.password} onChange={handleChange}
              style={erros.password ? { borderColor:'#ff6b6b' } : {}} />
            <ErroInline msg={erros.password} />
          </div>
          <div className="form-group">
            <label>Confirmar senha</label>
            <input name="confirm" type="password" placeholder="Repita a senha"
              value={form.confirm} onChange={handleChange}
              style={erros.confirm ? { borderColor:'#ff6b6b' } : {}} />
            <ErroInline msg={erros.confirm} />
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
