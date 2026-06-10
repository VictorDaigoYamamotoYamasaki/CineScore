import { useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { followerService, profileService, sessionHelper } from '../services/api'

function initials(name = '') {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase()
}

export default function FollowersPage() {
  const { userId: profileUserId } = useParams()
  const [params, setParams] = useSearchParams()
  const navigate            = useNavigate()
  const currentUser         = sessionHelper.get()

  const tab = params.get('tab') || 'followers'

  const [followers,      setFollowers]      = useState([])
  const [following,      setFollowing]      = useState([])
  const [loading,        setLoading]        = useState(true)
  const [toggling,       setToggling]       = useState({})
  const [numericUserId,  setNumericUserId]  = useState(null)

  useEffect(() => {
    if (!currentUser) { navigate('/login'); return }
    setLoading(true)

    // Resolve numeric userId from profileUserId via profile endpoint
    profileService.perfilPorId(profileUserId)
      .then(({ data }) => {
        const uid = profileUserId
        setNumericUserId(uid)
        return Promise.all([
          followerService.listarSeguidores(uid),
          followerService.listarSeguindo(uid),
        ])
      })
      .then(([frs, fng]) => {
        setFollowers(frs.data || [])
        setFollowing(fng.data || [])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [profileUserId])

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
      console.error('Erro ao seguir/deixar de seguir:', e)
    } finally {
      setToggling(prev => ({ ...prev, [user.userId]: false }))
    }
  }

  const list        = tab === 'followers' ? followers : following
  const isOwnProfile = profileUserId === currentUser?.profileUserId

  return (
    <>
      <Navbar user={currentUser} />
      <div className="page-main">
        <div className="container" style={{ maxWidth:520, paddingTop:'2rem' }}>

          <button
            className="btn btn--ghost btn--sm"
            style={{ marginBottom:'1.2rem' }}
            onClick={() => navigate(`/profile/${profileUserId}`)}
          >
            ← Voltar ao perfil
          </button>

          <div style={{ display:'flex', gap:'0.5rem', marginBottom:'1.5rem' }}>
            {['followers','following'].map(t => (
              <button key={t}
                className={`btn btn--sm ${tab === t ? 'btn--primary' : 'btn--ghost'}`}
                onClick={() => setParams({ tab: t })}
              >
                {t === 'followers' ? 'Seguidores' : 'Seguindo'}
                <span style={{ marginLeft:'0.4rem', fontSize:'0.7rem', color:'var(--text-muted)' }}>
                  {t === 'followers' ? followers.length : following.length}
                </span>
              </button>
            ))}
          </div>

          {loading && <div className="spinner" style={{ margin:'2rem auto' }} />}

          {!loading && list.length === 0 && (
            <p style={{ color:'var(--text-muted)', fontSize:'0.85rem', textAlign:'center', padding:'2rem 0' }}>
              {tab === 'followers' ? 'Nenhum seguidor ainda.' : 'Não está seguindo ninguém ainda.'}
            </p>
          )}

          {!loading && list.map(user => (
            <div key={user.userId}
              style={{ display:'flex', alignItems:'center', gap:'0.75rem',
                padding:'0.75rem 0', borderBottom:'1px solid var(--border)' }}>
              <div className="avatar" style={{ width:36, height:36, fontSize:'0.85rem',
                cursor:'pointer', flexShrink:0 }}
                onClick={() => navigate(`/profile/${user.profileUserId || user.userId}`)}>
                {initials(user.name)}
              </div>
              <div style={{ flex:1 }}>
                <div style={{ fontWeight:600, fontSize:'0.9rem', color:'var(--text-primary)',
                  cursor:'pointer' }}
                  onClick={() => navigate(`/profile/${user.profileUserId || user.userId}`)}>
                  {user.name}
                </div>
                <div style={{ fontSize:'0.75rem', color:'var(--text-muted)' }}>
                  {user.reviewCount} review{user.reviewCount !== 1 ? 's' : ''}
                </div>
              </div>

              {String(user.userId) !== String(currentUser?.id) && user.name !== 'Usuário Deletado' && (
                <button
                  className={`btn btn--sm ${user.isFollowing ? 'btn--ghost' : 'btn--primary'}`}
                  disabled={toggling[user.userId]}
                  onClick={() => handleToggleFollow(user)}
                >
                  {toggling[user.userId]
                    ? <span className="spinner" style={{ width:12, height:12 }} />
                    : user.isFollowing ? 'Seguindo' : 'Seguir'}
                </button>
              )}
            </div>
          ))}

        </div>
      </div>
    </>
  )
}
