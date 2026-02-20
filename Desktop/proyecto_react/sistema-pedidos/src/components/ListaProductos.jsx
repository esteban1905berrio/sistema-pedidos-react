// src/components/ListaProductos.jsx
import { useEffect, useState } from 'react';
import { clienteSupabase } from '../lib/supabase';
import { toast } from 'sonner';

export default function ListaProductos() {
  const [productos, setProductos] = useState([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    obtenerProductos();
  }, []);

  async function obtenerProductos() {
    try {
      // Traemos productos y sus inventarios
      const { data, error } = await clienteSupabase
        .from('productos')
        .select(`
          id,
          nombre,
          precio,
          inventarios (stock_actual)
        `);

      if (error) throw error;
      setProductos(data || []);
    } catch (error) {
      console.error("Error Supabase:", error);
      toast.error("Error al cargar productos");
    } finally {
      setCargando(false);
    }
  }

  if (cargando) return <p className="text-center text-gray-500">Cargando productos...</p>;

  if (productos.length === 0) {
    return (
      <div className="text-center p-10 bg-white rounded-xl border border-dashed border-gray-300">
        <p className="text-gray-500">No hay productos registrados en la base de datos.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
      {productos.map((prod) => {
        // Validamos de forma segura si existe el stock
        // Supabase devuelve un array en las relaciones, tomamos el primer elemento si existe
        const stockActual = prod.inventarios?.[0]?.stock_actual ?? 0;

        return (
          <div key={prod.id} className="bg-white p-6 rounded-xl shadow-md border border-gray-100">
            <h3 className="text-xl font-bold text-gray-800">{prod.nombre}</h3>
            <p className="text-blue-600 font-semibold text-lg">${prod.precio}</p>
            
            <div className="mt-4 flex justify-between items-center">
              <span className={`text-sm font-medium px-2 py-1 rounded ${
                stockActual > 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
              }`}>
                Stock: {stockActual}
              </span>
              
              <button 
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors disabled:bg-gray-300"
                disabled={stockActual <= 0}
              >
                Pedir
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}