import { useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { authService } from '../services/api'

const REGEX_SENHA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$/

function ErroInline({ msg }) {
  if (!msg) return null
  return <span style={{ color:'#ff6b6b', fontSize:'0.76rem', marginTop:'4px', display:'block' }}>{msg}</span>
}

export default function ResetPasswordPage() {
  const navigate       = useNavigate()
  const [searchParams] = useSearchParams()
  const token          = searchParams.get('token') || ''

  const [novaSenha, setNovaSenha] = useState('')
  const [confirmar, setConfirmar] = useState('')
  const [erros,     setErros]     = useState({})
  const [loading,   setLoading]   = useState(false)
  const [concluido, setConcluido] = useState(false)
  const [apiError,  setApiError]  = useState('')

  function handleNovaSenhaChange(e) {
    setNovaSenha(e.target.value)
    setErros(p => ({ ...p, novaSenha: '' }))
    setApiError('')
  }

  function handleConfirmarChange(e) {
    setConfirmar(e.target.value)
    setErros(p => ({ ...p, confirmar: '' }))
  }

  function validar() {
    const e = {}
    if (!novaSenha)
      e.novaSenha = 'Nova senha obrigatória.'
    else if (!REGEX_SENHA.test(novaSenha))
      e.novaSenha = 'Mín. 6 caracteres, 1 maiúscula, 1 minúscula e 1 caractere especial.'
    if (!confirmar)
      e.confirmar = 'Confirmação obrigatória.'
    else if (novaSenha && confirmar !== novaSenha)
      e.confirmar = 'As senhas não coincidem.'
    return e
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const e2 = validar()
    if (Object.keys(e2).length > 0) { setErros(e2); return }
    setLoading(true)
    try {
      await authService.resetPassword(token, novaSenha)
      setConcluido(true)
    } catch (err) {
      setApiError(err.response?.data?.message || 'Link inválido ou expirado.')
    } finally { setLoading(false) }
  }

  if (!token) {
    return (
      <div style={{ minHeight:'100vh', display:'flex', alignItems:'center', justifyContent:'center' }}>
        <div className="card" style={{ padding:'2rem', textAlign:'center', maxWidth:380 }}>
          <p style={{ color:'#ff6b6b' }}>Link inválido ou expirado.</p>
          <Link to="/forgot-password">
            <button className="btn btn--primary" style={{ marginTop:'1rem', width:'100%' }}>
              Solicitar novo link
            </button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div style={{ minHeight:'100vh', display:'flex', alignItems:'center', justifyContent:'center', padding:'1rem' }}>
      <div className="card" style={{ width:'100%', maxWidth:400, padding:'2rem' }}>

        <div style={{ textAlign:'center', marginBottom:'1.5rem' }}>
          <div className="logo-text" style={{ fontSize:'1.6rem', fontWeight:800, marginBottom:'0.5rem' }}>
            Cine<span>Score</span>
          </div>
          <h2 style={{ margin:0, fontSize:'1.1rem', fontWeight:600 }}>Nova senha</h2>
          <p style={{ color:'var(--text-muted)', fontSize:'0.85rem', marginTop:'0.4rem' }}>
            Escolha uma nova senha para sua conta.
          </p>
        </div>

        {concluido ? (
          <div style={{ textAlign:'center' }}>
            <div style={{ fontSize:'2.5rem', marginBottom:'1rem' }}>✅</div>
            <p style={{ color:'var(--text-secondary)', fontSize:'0.9rem' }}>
              Senha redefinida com sucesso!
            </p>
            <button className="btn btn--primary" style={{ width:'100%', marginTop:'1rem' }}
              onClick={() => navigate('/login')}>
              Fazer login
            </button>
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
                Nova senha
              </label>
              <input type="password" value={novaSenha} onChange={handleNovaSenhaChange}
                placeholder="Mín. 6 chars, 1 maiúscula, 1 minúscula, 1 especial"
                style={{ width:'100%', ...(erros.novaSenha ? { borderColor:'#ff6b6b' } : {}) }}
                autoFocus />
              <ErroInline msg={erros.novaSenha} />
            </div>
            <div style={{ marginBottom:'1.5rem' }}>
              <label style={{ fontSize:'0.8rem', color:'var(--text-muted)', display:'block', marginBottom:'0.4rem' }}>
                Confirmar nova senha
              </label>
              <input type="password" value={confirmar} onChange={handleConfirmarChange}
                placeholder="Repita a senha"
                style={{ width:'100%', ...(erros.confirmar ? { borderColor:'#ff6b6b' } : {}) }} />
              <ErroInline msg={erros.confirmar} />
            </div>
            <button type="submit" className="btn btn--primary" style={{ width:'100%' }} disabled={loading}>
              {loading ? 'Salvando...' : 'Redefinir senha'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
