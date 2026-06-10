import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('[CineScore] Erro não tratado:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          minHeight: '60vh', display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center', padding: '2rem', textAlign: 'center'
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>⚠️</div>
          <h2 style={{ color: 'var(--text-primary)', marginBottom: '0.5rem' }}>Algo deu errado</h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', maxWidth: 400 }}>
            Ocorreu um erro inesperado nesta página.
          </p>
          <button className="btn btn--primary"
            onClick={() => { this.setState({ hasError: false }); window.location.href = '/home' }}>
            Voltar ao início
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
