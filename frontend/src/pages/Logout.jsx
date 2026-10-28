import { Navigate } from 'react-router-dom';

export default function Logout() {
    window.localStorage.removeItem('token');
    return <Navigate to="/login" replace />
}