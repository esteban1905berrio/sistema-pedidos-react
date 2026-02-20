// src/pages/Login.jsx
import { useState } from 'react';
import { clienteSupabase } from '../lib/supabase'; // Importamos la conexión que creamos antes
import { toast } from 'sonner'; // Para las notificaciones tipo "Toast" solicitadas

export default function Login() {
  const [correo, setCorreo] = useState('');
  const [clave, setClave] = useState('');
  const [cargando, setCargando] = useState(false);

  // Función para Iniciar Sesión
  const manejarIngreso = async (e) => {
    e.preventDefault();
    setCargando(true);
    
    const { error } = await clienteSupabase.auth.signInWithPassword({
      email: correo,
      password: clave,
    });

    if (error) {
      toast.error("Error al entrar: " + error.message);
    } else {
      toast.success("¡Bienvenido al Sistema!");
    }
    setCargando(false);
  };

  // Función para Registrarse
  const manejarRegistro = async (e) => {
    e.preventDefault();
    setCargando(true);
    
    const { error } = await clienteSupabase.auth.signUp({
      email: correo,
      password: clave,
    });

    if (error) {
      toast.error("Error al registrar: " + error.message);
    } else {
      toast.success("Te enviamos un correo de confirmación.");
    }
    setCargando(false);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100 p-4">
      <div className="w-full max-w-md p-8 space-y-6 bg-white rounded-xl shadow-lg border border-gray-200">
        <div className="text-center">
          <h2 className="text-3xl font-extrabold text-blue-700">Sistema de Pedidos</h2>
          <p className="text-gray-500 mt-2 text-sm">Ingresa tus credenciales para continuar</p>
        </div>

        <form className="space-y-4" onSubmit={(e) => e.preventDefault()}>
          <div>
            <label className="block text-sm font-semibold text-gray-700">Correo Electrónico</label>
            <input 
              type="email" 
              placeholder="tu@correo.com"
              value={correo} 
              onChange={(e) => setCorreo(e.target.value)}
              className="mt-1 w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none" 
              required 
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700">Contraseña</label>
            <input 
              type="password" 
              placeholder="••••••••"
              value={clave} 
              onChange={(e) => setClave(e.target.value)}
              className="mt-1 w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none" 
              required 
            />
          </div>

          <div className="flex flex-col gap-3 pt-2">
            <button 
              type="button"
              onClick={manejarIngreso}
              disabled={cargando}
              className="w-full py-2.5 text-white bg-blue-600 rounded-lg font-bold hover:bg-blue-700 transition-colors disabled:bg-gray-400"
            >
              {cargando ? 'Verificando...' : 'Iniciar Sesión'}
            </button>
            
            <button 
              type="button"
              onClick={manejarRegistro}
              disabled={cargando}
              className="w-full py-2.5 text-blue-600 border-2 border-blue-600 rounded-lg font-bold hover:bg-blue-50 transition-colors"
            >
              Crear Cuenta
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}