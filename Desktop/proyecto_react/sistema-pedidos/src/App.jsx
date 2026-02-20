// src/App.jsx
import { useState, useEffect } from 'react';
import { Toaster } from 'sonner';
import { clienteSupabase } from './lib/supabase';
import Login from './pages/Login';
import ListaProductos from './components/ListaProductos';

function App() {
  const [sesion, setSesion] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    // 1. Verificar sesión inicial
    const verificarSesion = async () => {
      const { data: { session } } = await clienteSupabase.auth.getSession();
      console.log("Sesión inicial detectada:", session);
      setSesion(session);
      setCargando(false);
    };

    verificarSesion();

    // 2. Escuchar cambios de estado (Login/Logout)
    const { data: { subscription } } = clienteSupabase.auth.onAuthStateChange((event, session) => {
      console.log("Evento de Auth:", event, session);
      setSesion(session);
    });

    return () => subscription.unsubscribe();
  }, []);

  if (cargando) return <div className="h-screen flex items-center justify-center">Cargando...</div>;

  return (
    <>
      <Toaster position="top-right" richColors closeButton />
      
      <main className="min-h-screen bg-gray-50">
        {!sesion ? (
          <Login />
        ) : (
          <div className="p-8">
            <header className="flex justify-between items-center bg-white p-6 rounded-lg shadow-md mb-8">
              <div>
                <h1 className="text-2xl font-bold text-blue-800">Panel de Pedidos</h1>
                <p className="text-gray-500">Sesión activa: {sesion.user.email}</p>
              </div>
              <button 
                onClick={() => clienteSupabase.auth.signOut()}
                className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600"
              >
                Cerrar Sesión
              </button>
            </header>

            <ListaProductos />
          </div>
        )}
      </main>
    </>
  );
}

export default App;