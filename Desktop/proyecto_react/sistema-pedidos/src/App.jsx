// src/App.jsx
import { Toaster } from 'sonner';
import Login from './pages/Login';

function App() {
  return (
    <>
      {/* Toaster: Configuración para los mensajes de éxito/error (Requisito UX) */}
      <Toaster position="top-right" richColors closeButton />
      
      {/* El main con fondo gris suave para que resalte el formulario */}
      <main className="min-h-screen bg-gray-50">
        <Login />
      </main>
    </>
  );
}

export default App;