import { useState, useEffect } from 'react';
import { Toaster, toast } from 'sonner';
import { clienteSupabase } from './lib/supabase';
import Login from './pages/Login';
import ListaProductos from './components/ListaProductos';

function App() {
  const [sesion, setSesion] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [carrito, setCarrito] = useState([]);
  const [carritoAbierto, setCarritoAbierto] = useState(false);

  useEffect(() => {
    clienteSupabase.auth.getSession().then(({ data: { session } }) => {
      setSesion(session);
      setCargando(false);
    });
    const {
      data: { subscription },
    } = clienteSupabase.auth.onAuthStateChange((_event, session) => {
      setSesion(session);
    });
    return () => subscription.unsubscribe();
  }, []);

  const agregarAlCarrito = (producto) => {
    setCarrito((actual) => [...actual, producto]);
    toast.success(`Añadido: ${producto.nombre}`);
  };

  const eliminarDelCarrito = (index) => {
    setCarrito((actual) => actual.filter((_, i) => i !== index));
  };

  const finalizarCompra = async () => {
    if (!sesion || carrito.length === 0) return;

    // Optimistic update: vaciamos carrito y cerramos, guardando estado anterior por si toca rollback
    const carritoAnterior = [...carrito];
    const carritoAbiertoAnterior = carritoAbierto;

    setCarrito([]);
    setCarritoAbierto(false);

    const loadingId = toast.loading('Procesando tu pedido en SAMBER...');

    try {
      const operaciones = [];

      for (const prod of carritoAnterior) {
        operaciones.push(
          clienteSupabase.from('pedidos').insert([
            { id_usuario: sesion.user.id, id_producto: prod.id, cantidad: 1 },
          ])
        );
        operaciones.push(
          clienteSupabase
            .from('productos')
            .update({ stock: prod.stock - 1 })
            .eq('id', prod.id)
        );
      }

      const resultados = await Promise.all(operaciones);
      const error = resultados.find((r) => r?.error);

      toast.dismiss(loadingId);

      if (error) {
        // Rollback: restaurar carrito y estado visual
        setCarrito(carritoAnterior);
        setCarritoAbierto(carritoAbiertoAnterior);
        toast.error('Hubo un error al procesar la compra', {
          description: error.error.message,
        });
        return;
      }

      toast.success('¡Compra exitosa! Gracias por confiar en SAMBER.');
      // El stock se actualizará en la UI gracias a Realtime en ListaProductos
    } catch (error) {
      // Rollback ante error inesperado
      toast.dismiss(loadingId);
      setCarrito(carritoAnterior);
      setCarritoAbierto(carritoAbiertoAnterior);
      toast.error('Hubo un error al procesar la compra');
    }
  };

  const total = carrito.reduce((sum, item) => sum + item.precio, 0);

  if (cargando) return <div className="h-screen flex items-center justify-center">...</div>;

  return (
    <>
      <Toaster position="bottom-right" richColors />
      
      <main className="min-h-screen bg-[#fcfcfd]">
        {!sesion ? (
          <Login />
        ) : (
          <div className="pb-20">
            {/* --- NAV BAR --- */}
            <nav className="sticky top-0 z-40 bg-white/90 backdrop-blur-md border-b border-slate-100">
              <div className="max-w-7xl mx-auto px-4 h-16 flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <div className="w-9 h-9 bg-gradient-to-br from-cyan-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-cyan-100">
                    <span className="text-white font-black text-xl">S</span>
                  </div>
                  <span className="text-xl font-black tracking-tighter text-slate-900">SAMBER</span>
                </div>
                
                <div className="flex items-center gap-4">
                  {/* BOTÓN CARRITO */}
                  <button 
                    onClick={() => setCarritoAbierto(true)}
                    className="relative p-2 bg-slate-50 rounded-xl hover:bg-slate-100 transition-colors"
                  >
                  
<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-slate-800 group-hover:text-cyan-600 transition-colors">
  <circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/>
  <path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.56-7.43H5.05"/>
</svg>
                    {carrito.length > 0 && (
                      <span className="absolute -top-1 -right-1 bg-cyan-500 text-white text-[10px] font-bold w-5 h-5 flex items-center justify-center rounded-full border-2 border-white animate-bounce">
                        {carrito.length}
                      </span>
                    )}
                  </button>

                  <button 
                    onClick={() => clienteSupabase.auth.signOut()}
                    className="text-xs font-bold text-red-500 hover:bg-red-50 px-3 py-2 rounded-lg"
                  >
                    Salir
                  </button>
                </div>
              </div>
            </nav>

            {/* --- HERO SECTION --- */}
            {/* --- HERO SECTION --- */}
{/* --- HERO SECTION --- */}
<header className="max-w-7xl mx-auto px-4 pt-12 pb-16">
  <div className="flex flex-col md:flex-row items-center justify-between gap-12">
    
    {/* Lado Izquierdo: Textos y Botón */}
    <div className="md:w-1/2 space-y-6">
      <h1 className="text-5xl md:text-7xl font-black text-slate-900 tracking-tighter leading-tight">
        SAMBER <br />
        <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-500 to-blue-600">
          Tech Evolution.
        </span>
      </h1>
      <p className="text-lg text-slate-500 max-w-md leading-relaxed font-medium">
       Donde el diseño sofisticado se encuentra con la potencia absoluta. Prepárate para la evolución tecnológica que tu espacio de trabajo merece.
      </p>
      
      {/* BOTÓN REPARADO */}
      <div className="pt-4">
        <button 
          onClick={() => document.getElementById('catalogo')?.scrollIntoView({ behavior: 'smooth' })}
          className="bg-slate-900 text-white px-8 py-4 rounded-2xl font-bold hover:bg-cyan-600 transition-all shadow-lg shadow-slate-200 active:scale-95"
        >
          Explorar Catálogo
        </button>
      </div>
    </div> {/* <-- Aquí cierra el div de textos (Línea 114 aprox) */}

    {/* Lado Derecho: Imagen */}
    <div className="md:w-1/2 relative group">
      <div className="absolute -inset-4 bg-gradient-to-r from-cyan-100 to-blue-100 rounded-full blur-3xl opacity-30"></div>
      <div className="relative">
        <img 
          src="/imagen_productos/Captura.PNG" 
          alt="SAMBER Showcase" 
          className="w-full h-auto object-contain drop-shadow-2xl hover:scale-105 transition-transform duration-700"
        />
      </div>
    </div>

  </div>
</header>
{/* --- SECCIÓN DE PRODUCTOS --- */}
<section id="catalogo" className="max-w-7xl mx-auto px-4 mt-12 mb-8">
  <div className="flex items-center gap-4 mb-10">
    <h2 className="text-2xl font-black text-slate-800 tracking-tight">
      Nuevos Ingresos
    </h2>
    {/* Línea decorativa que da el toque profesional */}
    <div className="h-[2px] flex-grow bg-gradient-to-r from-slate-200 to-transparent rounded-full"></div>
    
    {/* Pequeño indicador de stock */}
    <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest bg-slate-100 px-3 py-1 rounded-full">
      Samber Oficial Store
    </span>
  </div>

  {/* Tu componente de lista */}
  <ListaProductos onAgregar={agregarAlCarrito} />
</section>

            <section className="max-w-7xl mx-auto px-4">
               <ListaProductos onAgregar={agregarAlCarrito} />
            </section>

            {/* --- CARRITO LATERAL (DRAWER) --- */}
            {carritoAbierto && (
              <div className="fixed inset-0 z-50 overflow-hidden">
                <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm" onClick={() => setCarritoAbierto(false)} />
                <div className="absolute inset-y-0 right-0 max-w-full flex">
                  <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
                    <div className="p-6 border-b flex justify-between items-center">
                      <h2 className="text-xl font-black text-slate-900">Tu Carrito</h2>
                      <button onClick={() => setCarritoAbierto(false)} className="text-slate-400 hover:text-slate-900">✕</button>
                    </div>

                    <div className="flex-grow overflow-y-auto p-6 space-y-4">
                      {carrito.length === 0 ? (
                        <p className="text-center text-slate-400 mt-10">El carrito está vacío</p>
                      ) : (
                        carrito.map((item, index) => (
                          <div key={index} className="flex gap-4 bg-slate-50 p-3 rounded-2xl border border-slate-100">
                            <img src={item.imagen_url} className="w-16 h-16 object-cover rounded-xl" />
                            <div className="flex-grow">
                              <h4 className="text-sm font-bold text-slate-800">{item.nombre}</h4>
                              <p className="text-cyan-600 font-bold text-sm">${item.precio.toLocaleString()}</p>
                            </div>
                            <button onClick={() => eliminarDelCarrito(index)} className="text-red-400 hover:text-red-600 text-xs">Quitar</button>
                          </div>
                        ))
                      )}
                    </div>

                    <div className="p-6 border-t bg-slate-50">
                      <div className="flex justify-between text-lg font-black mb-4">
                        <span>Total:</span>
                        <span className="text-cyan-600">${total.toLocaleString()}</span>
                      </div>
                      <button 
                        onClick={finalizarCompra}
                        disabled={carrito.length === 0}
                        className="w-full bg-slate-900 text-white py-4 rounded-2xl font-bold hover:bg-blue-600 transition-all active:scale-95 disabled:bg-slate-300"
                      >
                        Finalizar Compra
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </>
  );
}

export default App;