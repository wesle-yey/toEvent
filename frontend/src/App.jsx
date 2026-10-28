import './App.css'

import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Premium from './pages/Premium';
import { ProtectedRoute } from './components/ProtectedRoute';
import NotFound from './pages/NotFound';
import Logout from './pages/Logout';

function App() {
  return (
    <Router>
      <Routes>
        {/* Rotas Públicas */}
        <Route path="/login" element={<Login />} />

        {/* Rotas Privadas (Protegidas por JWT) */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/premium"
          element={
            <ProtectedRoute>
              <Premium />
            </ProtectedRoute>
          }
        />
        <Route
          path="/logout"
          element={
            <ProtectedRoute>
              <Logout />
            </ProtectedRoute>
          }
        />
        {/* Rota de erro e rota curinga protegidas:
            - Se não autenticado, ProtectedRoute redireciona diretamente para /login
            - Se autenticado, renderiza a tela 404 personalizada */}
        <Route
          path="/error"
          element={
            <ProtectedRoute>
              <NotFound />
            </ProtectedRoute>
          }
        />
        <Route
          path="*"
          element={
            <ProtectedRoute>
              <NotFound />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;