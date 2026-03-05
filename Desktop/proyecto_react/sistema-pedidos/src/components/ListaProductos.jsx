import { useEffect, useState } from 'react';
import { clienteSupabase } from '../lib/supabase';
import { toast } from 'sonner';

export default function ListaProductos({ onAgregar }) {
  const [productos, setProductos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [productoSeleccionado, setProductoSeleccionado] = useState(null);

  useEffect(() => {
    obtenerProductos();

    // Suscripción Realtime a la tabla productos
    const channel = clienteSupabase
      .channel('public:productos')
      .on(
        'postgres_changes',
        { event: '*', schema: 'public', table: 'productos' },
        (payload) => {
          setProductos((productosActuales) => {
            const ordenar = (lista) =>
              [...lista].sort((a, b) =>
                a.nombre.localeCompare(b.nombre, 'es', { sensitivity: 'base' })
              );

            if (payload.eventType === 'INSERT') {
              return ordenar([...productosActuales, payload.new]);
            }

            if (payload.eventType === 'UPDATE') {
              return ordenar(
                productosActuales.map((p) =>
                  p.id === payload.new.id ? payload.new : p
                )
              );
            }

            if (payload.eventType === 'DELETE') {
              return ordenar(
                productosActuales.filter((p) => p.id !== payload.old.id)
              );
            }

            return productosActuales;
          });
        }
      )
      .subscribe();

    return () => {
      clienteSupabase.removeChannel(channel);
    };
  }, []);

  async function obtenerProductos() {
    try {
      const { data, error } = await clienteSupabase
        .from('productos')
        .select('*')
        .order('nombre', { ascending: true });
      if (error) throw error;
      setProductos(data || []);
    } catch (e) {
      toast.error('Error cargando base de datos');
    } finally {
      setCargando(false);
    }
  }

  if (cargando) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map(n => (
          <div key={n} className="h-64 bg-slate-100 animate-pulse rounded-2xl" />
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {productos.map((prod) => (
        <div 
          key={prod.id} 
          onClick={() => setProductoSeleccionado(prod)}
          className="group bg-white rounded-2xl p-2.5 border border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 cursor-pointer flex flex-col"
        >
          <div className="relative h-44 w-full overflow-hidden rounded-xl bg-slate-50">
            <img 
              src={prod.imagen_url} 
              alt={prod.nombre} 
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" 
            />
            <div className="absolute top-2 right-2">
              <span className="bg-white/90 backdrop-blur px-2.5 py-0.5 rounded-full text-[9px] font-extrabold uppercase tracking-wider text-slate-700 shadow-sm">
                {prod.categoria}
              </span>
            </div>
          </div>
          <div className="p-3 flex-grow flex flex-col justify-between">
            <h3 className="text-sm font-semibold text-slate-800 line-clamp-2 group-hover:text-cyan-600 transition-colors leading-tight">
              {prod.nombre}
            </h3>
            <div className="mt-3 flex items-center justify-between gap-1">
              <span className="text-lg font-extrabold text-slate-950">
                ${Number(prod.precio).toLocaleString('es-CO')}
              </span>
              <div className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 group-hover:bg-cyan-500 group-hover:text-white transition-colors duration-300">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M5 12h14m-7-7 7 7-7 7"/></svg>
              </div>
            </div>
          </div>
        </div>
      ))}

      {/* --- MODAL DE DETALLES --- */}
      {productoSeleccionado && (
        <div className="modal-overlay" onClick={() => setProductoSeleccionado(null)}>
          <div 
            className="modal-content flex flex-col md:flex-row shadow-2xl" 
            onClick={e => e.stopPropagation()}
          >
            <div className="md:w-1/2 bg-slate-50 p-8 flex items-center justify-center relative">
              <img 
                src={productoSeleccionado.imagen_url} 
                className="w-full max-h-64 object-contain drop-shadow-xl" 
                alt={productoSeleccionado.nombre}
              />
            </div>
            
            <div className="md:w-1/2 p-8 flex flex-col justify-center">
              <div className="flex justify-between items-start mb-3">
                <span className="text-cyan-600 font-bold text-[10px] uppercase tracking-[0.15em]">
                  {productoSeleccionado.categoria}
                </span>
                <button 
                  onClick={() => setProductoSeleccionado(null)} 
                  className="text-slate-400 hover:text-slate-950 transition-colors p-1 bg-slate-100 rounded-full"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
                </button>
              </div>
              
              <h2 className="text-2xl font-extrabold text-slate-950 leading-tight">
                {productoSeleccionado.nombre}
              </h2>
              <p className="mt-3 text-sm text-slate-600 leading-relaxed font-medium">
                {productoSeleccionado.description || productoSeleccionado.descripcion}
              </p>
              
              <div className="mt-5 flex items-end gap-2">
                <span className="text-3xl font-extrabold text-slate-950">
                  ${Number(productoSeleccionado.precio).toLocaleString('es-CO')}
                </span>
              </div>
              
              <div className="mt-7 flex flex-col gap-3">
                <button 
                  disabled={productoSeleccionado.stock <= 0}
                  onClick={() => {
                    onAgregar(productoSeleccionado);
                    setProductoSeleccionado(null);
                  }}
                  className="w-full bg-slate-950 text-white py-3.5 rounded-xl font-bold text-base hover:bg-cyan-500 transition-all active:scale-[0.98] disabled:bg-slate-200 shadow-md"
                >
                  {productoSeleccionado.stock > 0 ? 'Añadir al Carrito' : 'Agotado'}
                </button>
                <p className="text-center text-[10px] font-semibold text-slate-500">
                  Stock disponible: {productoSeleccionado.stock} unidades
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}