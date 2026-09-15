import { Component, StrictMode, createElement, Fragment } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';

// The project uses Vite's classic JSX transform, which emits React.createElement.
// React is exposed explicitly to keep that transform working with the current CJS package.
const React = { createElement, Fragment };

class ErrorBoundary extends Component {
  constructor(props) { super(props); this.state = { error: null }; }
  static getDerivedStateFromError(error) { return { error }; }
  render() {
    if (this.state.error) return <main className="render-error"><h1>Khong the hien thi giao dien</h1><p>{this.state.error.message}</p><button onClick={() => window.location.reload()}>Tai lai trang</button></main>;
    return this.props.children;
  }
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary><App /></ErrorBoundary>
  </StrictMode>,
);
