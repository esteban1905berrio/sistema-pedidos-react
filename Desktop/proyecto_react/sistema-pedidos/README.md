# 📦 Sistema de Pedidos Real-Time

Este es un sistema de gestión de pedidos y productos moderno, construido con **React** para la interfaz y **Supabase** como infraestructura de base de datos y autenticación.

## 🚀 Tecnologías Utilizadas

* **Frontend:** React 18 + Vite.
* **Estilos:** [Tailwind CSS v4](https://tailwindcss.com/) (Última versión).
* **Backend:** Supabase (PostgreSQL).
* **Notificaciones:** Sonner (Toasts elegantes).
* **Iconos:** Lucide React.

## ✨ Características Principales

* **Catálogo Dinámico:** Visualización de productos en tiempo real desde PostgreSQL.
* **Gestión de Inventario:** El stock se actualiza automáticamente al realizar un pedido mediante políticas de seguridad (RLS).
* **Autenticación:** Sistema de inicio de sesión integrado con Supabase Auth.
* **Diseño Responsive:** Totalmente adaptado a dispositivos móviles y escritorio.

## 🛠️ Configuración de la Base de Datos

Para que el proyecto funcione correctamente, se requieren las siguientes tablas en Supabase:

1.  **productos:** `id, nombre, precio, imagen_url`.
2.  **inventarios:** `id, id_producto, stock_actual`.
3.  **pedidos:** `id, id_usuario, id_producto, cantidad, creado_en`.

> **Nota:** Es necesario configurar las políticas RLS en Supabase para permitir el `UPDATE` en la tabla de inventarios.

---
Desarrollado con por Esteban.