import { useState } from 'react'
import { profileService } from '../services/api'

const REGEX_EMAIL = /\S+@\S+\.\S+/
const REGEX_SENHA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$/

function ErroInline({ msg }) {
  if (!msg) return null
  return <span style={{ color:'#ff6b6b', fontSize:'0.76rem', marginTop:'4px', display:'block' }}>{msg}</span>
}

export default function ProfileSettingsModal({ user, onClose, onUpdated }) {
  const [name,     setName]     = useState(user.name  || '')
  const [email,    setEmail]    = useState(user.email || '')
  const [password, setPassword] = useState('')
  const [erros,    setErros]    = useState({})
  const [saving,   setSaving]   = useState(false)
  const [apiError, setApiError] = useState('')

  function handleNameChange(e) {
    setName(e.target.value)
    setErros(p => ({ ...p, name: '' }))
    setApiError('')
  }

  function handleEmailChange(e) {
    setEmail(e.target.value)
    setErros(p => ({ ...p, email: '' }))
    setApiError('')
  }

  function handlePasswordChange(e) {
    setPassword(e.target.value)
    setErros(p => ({ ...p, password: '' }))
  }

  function validar() {
    const e = {}
    if (!name.trim())
      e.name = 'Nome obrigatório.'
    else if (name.trim().length < 2)
      e.name = 'Nome deve ter pelo menos 2 caracteres.'
    else if (name.trim().length > 25)
      e.name = 'Nome deve ter no máximo 25 caracteres.'

    if (!email.trim())
      e.email = 'E-mail obrigatório.'
    else if (!REGEX_EMAIL.test(email))
      e.email = 'Informe um e-mail válido.'

    if (password && !REGEX_SENHA.test(password))
      e.password = 'Mín. 6 caracteres, 1 maiúscula, 1 minúscula e 1 caractere especial.'

    return e
  }

  async function handleSave() {
    const e2 = validar()
    if (Object.keys(e2).length > 0) { setErros(e2); return }
    setSaving(true)
    setApiError('')
    try {
      const { data } = await profileService.atualizarPerfil({
        name:     name.trim(),
        email:    email.trim(),
        password: password || undefined,
      })
      onUpdated(data)
      onClose()
    } catch (err) {
      setApiError(err.response?.data?.message || 'Erro ao atualizar perfil.')
    } finally { setSaving(false) }
  }

  return (
    <div style={{ position:'fixed', inset:0, zIndex:2000, background:'rgba(0,0,0,0.75)',
      display:'flex', alignItems:'center', justifyContent:'center', padding:'1rem' }}
      onClick={onClose}>
      <div style={{ background:'var(--bg-card)', border:'1px solid var(--border)',
        borderRadius:12, width:'100%', maxWidth:440, padding:'1.5rem' }}
        onClick={e => e.stopPropagation()}>

        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:'1.2rem' }}>
          <h3 style={{ margin:0, fontSize:'1rem', fontWeight:700 }}>Editar Perfil</h3>
          <button onClick={onClose} style={{ background:'none', border:'none', color:'var(--text-muted)', fontSize:'1.2rem', cursor:'pointer' }}>✕</button>
        </div>

        {apiError && (
          <div style={{ background:'rgba(220,53,69,0.15)', border:'1px solid rgba(220,53,69,0.4)',
            borderRadius:8, padding:'0.6rem 0.8rem', marginBottom:'1rem',
            fontSize:'0.82rem', color:'#ff6b6b' }}>
            {apiError}
          </div>
        )}

        <div style={{ display:'flex', flexDirection:'column', gap:'0.8rem' }}>
          <div>
            <label style={{ fontSize:'0.78rem', color:'var(--text-muted)', display:'block', marginBottom:'0.3rem' }}>Nome de usuário</label>
            <input type="text" value={name} onChange={handleNameChange}
              placeholder="Seu nome" maxLength={25}
              style={{ width:'100%', ...(erros.name ? { borderColor:'#ff6b6b' } : {}) }} />
            <ErroInline msg={erros.name} />
          </div>
          <div>
            <label style={{ fontSize:'0.78rem', color:'var(--text-muted)', display:'block', marginBottom:'0.3rem' }}>E-mail</label>
            <input type="email" value={email} onChange={handleEmailChange}
              placeholder="Seu e-mail"
              style={{ width:'100%', ...(erros.email ? { borderColor:'#ff6b6b' } : {}) }} />
            <ErroInline msg={erros.email} />
          </div>
          <div>
            <label style={{ fontSize:'0.78rem', color:'var(--text-muted)', display:'block', marginBottom:'0.3rem' }}>
              Nova senha <span style={{ color:'var(--text-muted)', fontSize:'0.72rem' }}>(deixe vazio para não alterar)</span>
            </label>
            <input type="password" value={password} onChange={handlePasswordChange}
              placeholder="••••••••"
              style={{ width:'100%', ...(erros.password ? { borderColor:'#ff6b6b' } : {}) }} />
            <ErroInline msg={erros.password} />
          </div>
        </div>

        <div style={{ display:'flex', gap:'0.6rem', marginTop:'1.2rem', justifyContent:'flex-end' }}>
          <button className="btn btn--ghost btn--sm" onClick={onClose}>Cancelar</button>
          <button className="btn btn--primary btn--sm" onClick={handleSave} disabled={saving}>
            {saving ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </div>
      </div>
    </div>
  )
}
