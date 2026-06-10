import { useState } from 'react'
import { Link } from 'react-router-dom'
import { authService } from '../services/api'

const REGEX_EMAIL = /\S+@\S+\.\S+/

export default function ForgotPasswordPage() {
  const [email,   setEmail]   = useState('')
  const [erroEmail, setErroEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [enviado, setEnviado] = useState(false)
  const [apiError, setApiError] = useState('')

  function handleEmailChange(e) {
    setEmail(e.target.value)
    setErroEmail('')
    setApiError('')
  }

  function validar() {
    if (!email.trim()) return 'E-mail obrigatório.'
    if (!REGEX_EMAIL.test(email)) return 'Informe um e-mail válido.'
    return ''
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const erro = validar()
    if (erro) { setErroEmail(erro); return }
    setLoading(true)
    try {
      await authService.forgotPassword(email.trim())
      setEnviado(true)
    } catch (err) {
      setApiError(err.response?.data?.message || 'Erro ao enviar e-mail.')
    } finally { setLoading(false) }
  }

  return (
    <div style={{ minHeight:'100vh', display:'flex', alignItems:'center', justifyContent:'center', padding:'1rem' }}>
      <div className="card" style={{ width:'100%', maxWidth:400, padding:'2rem' }}>

        <div style={{ textAlign:'center', marginBottom:'1.5rem' }}>
          <div className="logo-text" style={{ fontSize:'1.6rem', fontWeight:800, marginBottom:'0.5rem' }}>
            Cine<span>Score</span>
          </div>
          <h2 style={{ margin:0, fontSize:'1.1rem', fontWeight:600 }}>Esqueceu sua senha?</h2>
          <p style={{ color:'var(--text-muted)', fontSize:'0.85rem', marginTop:'0.4rem' }}>
            Informe seu e-mail e enviaremos um link para redefinir sua senha.
          </p>
        </div>

        {enviado ? (
          <div style={{ textAlign:'center' }}>
            <div style={{ fontSize:'2.5rem', marginBottom:'1rem' }}>📬</div>
            <p style={{ color:'var(--text-secondary)', fontSize:'0.9rem', lineHeight:1.6 }}>
              Se o e-mail <strong>{email}</strong> estiver cadastrado, você receberá as instruções em instantes.
            </p>
            <p style={{ color:'var(--text-muted)', fontSize:'0.8rem' }}>
              Verifique também a caixa de spam.
            </p>
            <Link to="/login">
              <button className="btn btn--primary" style={{ width:'100%', marginTop:'1rem' }}>
                Voltar para o login
              </button>
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            {apiError && (
              <div style={{ background:'rgba(220,53,69,0.15)', border:'1px solid rgba(220,53,69,0.4)',
                borderRadius:8, padding:'0.6rem 0.8rem', marginBottom:'1rem',
                fontSize:'0.82rem', color:'#ff6b6b' }}>
                {apiError}
              </div>
            )}
            <div style={{ marginBottom:'1rem' }}>
              <label style={{ fontSize:'0.8rem', color:'var(--text-muted)', display:'block', marginBottom:'0.4rem' }}>
                E-mail
              </label>
              <input type="email" value={email} onChange={handleEmailChange}
                placeholder="seu@email.com"
                style={{ width:'100%', ...(erroEmail ? { borderColor:'#ff6b6b' } : {}) }}
                autoFocus />
              {erroEmail && <span style={{ color:'#ff6b6b', fontSize:'0.76rem', marginTop:'4px', display:'block' }}>{erroEmail}</span>}
            </div>
            <button type="submit" className="btn btn--primary" style={{ width:'100%' }} disabled={loading}>
              {loading ? 'Enviando...' : 'Enviar link de redefinição'}
            </button>
            <div style={{ textAlign:'center', marginTop:'1rem' }}>
              <Link to="/login" style={{ color:'var(--text-muted)', fontSize:'0.82rem', textDecoration:'none' }}>
                Voltar para o login
              </Link>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
