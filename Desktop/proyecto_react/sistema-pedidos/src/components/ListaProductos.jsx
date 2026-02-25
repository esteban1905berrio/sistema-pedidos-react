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
      console.error("Error al cargar productos:", error);
      toast.error("Error al conectar con la base de datos");
    } finally {
      setCargando(false);
    }
  }

  // --- FUNCIÓN PARA PROCESAR EL PEDIDO ---
  async function hacerPedido(productoId, stockActual) {
    console.log("Iniciando proceso de pedido...");
    console.log("Producto ID:", productoId, "Stock actual:", stockActual);

    try {
      // 1. Verificar sesión del usuario
      const { data: { user }, error: userError } = await clienteSupabase.auth.getUser();
      
      if (userError || !user) {
        console.error("Error de usuario:", userError);
        toast.error("Debes estar logueado para realizar un pedido");
        return;
      }

      console.log("Usuario autenticado:", user.id);

      // 2. Insertar el registro en la tabla 'pedidos'
      const { error: errorPedido } = await clienteSupabase
        .from('pedidos')
        .insert([
          { 
            id_usuario: user.id, 
            id_producto: productoId, 
            cantidad: 1 
          }
        ]);

      if (errorPedido) {
        console.error("Error al insertar en tabla PEDIDOS:", errorPedido);
        throw new Error(`Error en pedidos: ${errorPedido.message}`);
      }

      console.log("Registro de pedido creado con éxito.");

      // 3. Descontar el stock en la tabla 'inventarios'
      // Importante: Usamos el ID del producto para encontrar su fila de inventario
      const nuevoStock = stockActual - 1;
      const { error: errorStock } = await clienteSupabase
        .from('inventarios')
        .update({ stock_actual: nuevoStock })
        .eq('id_producto', productoId);

      if (errorStock) {
        console.error("Error al actualizar tabla INVENTARIOS:", errorStock);
        throw new Error(`Error en inventario: ${errorStock.message}`);
      }

      console.log("Stock actualizado a:", nuevoStock);

      // 4. Éxito y recarga de datos
      toast.success("¡Pedido realizado con éxito!");
      await obtenerProductos(); // Refresca la interfaz

    } catch (error) {
      console.error("FALLO CRÍTICO:", error.message);
      toast.error(error.message || "No se pudo completar el pedido");
    }
  }

  if (cargando) return <p className="text-center text-gray-500 mt-10">Cargando catálogo...</p>;

  if (productos.length === 0) {
    return (
      <div className="text-center p-10 bg-white rounded-xl border border-dashed border-gray-300 mt-6">
        <p className="text-gray-500">No hay productos disponibles actualmente.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
      {productos.map((prod) => {
        // Extraemos el stock de la relación (asumiendo que es un array de 1 elemento)
        const stockActual = prod.inventarios?.[0]?.stock_actual ?? 0;

        return (
          <div key={prod.id} className="bg-white p-6 rounded-xl shadow-md border border-gray-100 flex flex-col justify-between">
            <div>
              <h3 className="text-xl font-bold text-gray-800">{prod.nombre}</h3>
              <p className="text-blue-600 font-bold text-2xl mt-2">${prod.precio}</p>
            </div>
            
            <div className="mt-6 flex justify-between items-center">
              <span className={`text-xs font-bold px-2 py-1 rounded-full uppercase ${
                stockActual > 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
              }`}>
                {stockActual > 0 ? `Stock: ${stockActual}` : 'Agotado'}
              </span>
              
              <button 
                onClick={() => {
                  console.log("Click en el producto:", prod.nombre);
                  hacerPedido(prod.id, stockActual);
                }}
                className="bg-blue-600 text-white px-5 py-2 rounded-lg font-medium hover:bg-blue-700 transition-all active:scale-95 disabled:bg-gray-300 disabled:cursor-not-allowed"
                disabled={stockActual <= 0}
              >
                Pedir ahora
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}