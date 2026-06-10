/**
 * PosterFrame — container que força proporção 2:3 para qualquer imagem de poster.
 * Usa a técnica padding-bottom em um div separado do conteúdo clicável,
 * garantindo que a proporção não depende do tamanho da imagem original.
 */
export default function PosterFrame({ src, alt, onClick, children, style = {} }) {
  return (
    <div
      onClick={onClick}
      style={{
        borderRadius: 8,
        overflow: 'hidden',
        cursor: onClick ? 'pointer' : 'default',
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border)',
        ...style,
      }}
    >
      {/* Div que define a proporção 2:3 via padding-bottom */}
      <div style={{ position: 'relative', width: '100%', paddingBottom: '150%' }}>
        {src ? (
          <img
            src={src}
            alt={alt}
            style={{
              position: 'absolute',
              inset: 0,
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              display: 'block',
            }}
          />
        ) : (
          <div style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '2rem',
          }}>
            🎬
          </div>
        )}
        {/* Slot para botões sobrepostos (remover, trocar, etc.) */}
        {children}
      </div>
    </div>
  )
}
