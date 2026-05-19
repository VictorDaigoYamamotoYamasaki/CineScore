import { useState } from 'react'
import api from '../services/api'

export default function ProfileSettingsModal({ user, onClose, onUpdated }) {
  const [name,     setName]     = useState(user.name || '')
  const [email,    setEmail]    = useState(user.email || '')
  const [password, setPassword] = useState('')
  const [saving,   setSaving]   = useState(false)
  const [error,    setError]    = useState('')

  async function handleSave() {
    if (!name.trim() || !email.trim()) {
      setError('Nome e e-mail são obrigatórios.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const { data } = await api.put('/profile/me/settings', {
        name: name.trim(),
        email: email.trim(),
        password: password || undefined,
      })
      onUpdated(data)
      onClose()
    } catch (err) {
      setError(err.response?.data?.message || 'Erro ao atualizar perfil.')
    } finally {
      setSaving(false)
    }
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

        {error && (
          <div style={{ background:'rgba(220,53,69,0.15)', border:'1px solid rgba(220,53,69,0.4)',
            borderRadius:8, padding:'0.6rem 0.8rem', marginBottom:'1rem',
            fontSize:'0.82rem', color:'#ff6b6b' }}>
            {error}
          </div>
        )}

        <div style={{ display:'flex', flexDirection:'column', gap:'0.8rem' }}>
          <div>
            <label style={{ fontSize:'0.78rem', color:'var(--text-muted)', display:'block', marginBottom:'0.3rem' }}>Nome de usuário</label>
            <input type="text" value={name} onChange={e => setName(e.target.value)}
              placeholder="Seu nome" style={{ width:'100%' }} />
          </div>
          <div>
            <label style={{ fontSize:'0.78rem', color:'var(--text-muted)', display:'block', marginBottom:'0.3rem' }}>E-mail</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)}
              placeholder="Seu e-mail" style={{ width:'100%' }} />
          </div>
          <div>
            <label style={{ fontSize:'0.78rem', color:'var(--text-muted)', display:'block', marginBottom:'0.3rem' }}>
              Nova senha <span style={{ color:'var(--text-muted)', fontSize:'0.72rem' }}>(deixe vazio para não alterar)</span>
            </label>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)}
              placeholder="••••••••" style={{ width:'100%' }} />
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
