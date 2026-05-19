import { useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { followerService, sessionHelper } from '../services/api'

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

export default function FollowersPage() {
  const { userId }         = useParams()
  const [params, setParams] = useSearchParams()
  const navigate            = useNavigate()
  const currentUser         = sessionHelper.get()

  const tab = params.get('tab') || 'followers'

  const [followers, setFollowers] = useState([])
  const [following, setFollowing] = useState([])
  const [loading,   setLoading]   = useState(true)
  const [toggling,  setToggling]  = useState({})

  useEffect(() => {
    if (!currentUser) { navigate('/login'); return }
    setLoading(true)
    Promise.all([
      followerService.listarSeguidores(userId),
      followerService.listarSeguindo(userId),
    ]).then(([frs, fng]) => {
      setFollowers(frs.data)
      setFollowing(fng.data)
    }).catch(() => {})
    .finally(() => setLoading(false))
  }, [userId])

  async function handleToggleFollow(user) {
    setToggling(prev => ({ ...prev, [user.userId]: true }))
    try {
      if (user.isFollowing) {
        await followerService.unfollow(user.userId)
      } else {
        await followerService.follow(user.userId)
      }
      const update = list => list.map(u =>
        u.userId === user.userId ? { ...u, isFollowing: !u.isFollowing } : u
      )
      setFollowers(update)
      setFollowing(update)
    } catch (e) {
      alert(e.response?.data?.message || 'Erro.')
    } finally {
      setToggling(prev => ({ ...prev, [user.userId]: false }))
    }
  }

  const list = tab === 'followers' ? followers : following
  const isOwnProfile = String(userId) === String(currentUser?.id)

  return (
    <>
      <Navbar user={currentUser} />
      <div className="page-main">
        <div className="container" style={{ maxWidth: 640, paddingTop: '2rem', paddingBottom: '3rem' }}>

          {/* Voltar */}
          <button
            className="btn btn--ghost btn--sm"
            style={{ marginBottom: '1.5rem' }}
            onClick={() => navigate(`/profile/${userId}`)}
          >
            ← Voltar ao perfil
          </button>

          {/* Tabs */}
          <div style={{ display: 'flex', borderBottom: '1px solid var(--border)', marginBottom: '1.5rem' }}>
            {['followers', 'following'].map(t => (
              <button
                key={t}
                onClick={() => setParams({ tab: t })}
                style={{
                  padding: '0.6rem 1.5rem',
                  background: 'none',
                  border: 'none',
                  borderBottom: tab === t ? '2px solid var(--purple-bright)' : '2px solid transparent',
                  color: tab === t ? 'var(--text-primary)' : 'var(--text-muted)',
                  fontWeight: tab === t ? 700 : 400,
                  fontSize: '0.9rem',
                  cursor: 'pointer',
                  marginBottom: -1,
                  transition: 'all 0.15s',
                }}
              >
                {t === 'followers'
                  ? `Seguidores (${followers.length})`
                  : `Seguindo (${following.length})`}
              </button>
            ))}
          </div>

          {loading && (
            <div className="empty-state">
              <div className="spinner" style={{ margin: '0 auto' }} />
            </div>
          )}

          {!loading && list.length === 0 && (
            <div className="empty-state">
              <div className="empty-state-icon">👥</div>
              <p>{tab === 'followers' ? 'Nenhum seguidor ainda.' : 'Não está seguindo ninguém.'}</p>
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
            {list.map(user => (
              <div
                key={user.userId}
                className="card"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  padding: '0.9rem 1rem',
                  cursor: 'pointer',
                  transition: 'border-color 0.15s',
                }}
                onClick={() => navigate(`/profile/${user.userId}`)}
                onMouseEnter={e => e.currentTarget.style.borderColor = 'var(--purple-accent)'}
                onMouseLeave={e => e.currentTarget.style.borderColor = 'var(--border)'}
              >
                {/* Avatar */}
                <div style={{
                  width: 44, height: 44, borderRadius: '50%', flexShrink: 0,
                  background: 'linear-gradient(135deg, var(--purple-mid), var(--purple-accent))',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontWeight: 700, fontSize: '1rem', color: '#fff',
                }}>
                  {initials(user.name)}
                </div>

                {/* Info */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{user.name}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 2 }}>
                    {user.reviewCount} avaliação{user.reviewCount !== 1 ? 'ões' : ''}
                  </div>
                </div>

                {/* Follow button — não mostrar para si mesmo nem para deletados */}
                {String(user.userId) !== String(currentUser?.id) && user.name !== 'Usuário Deletado' && (
                  <button
                    className={`btn btn--sm ${user.isFollowing ? 'btn--ghost' : 'btn--primary'}`}
                    style={{ flexShrink: 0 }}
                    disabled={toggling[user.userId]}
                    onClick={e => { e.stopPropagation(); handleToggleFollow(user) }}
                  >
                    {toggling[user.userId]
                      ? <span className="spinner" />
                      : user.isFollowing ? '✓ Seguindo' : '+ Seguir'}
                  </button>
                )}
              </div>
            ))}
          </div>

        </div>
      </div>
    </>
  )
}
