// src/lib/supabase.js
import { createClient } from '@supabase/supabase-js'

const urlSupabase = import.meta.env.VITE_SUPABASE_URL
const llaveAnonimaSupabase = import.meta.env.VITE_SUPABASE_ANON_KEY

if (!urlSupabase || !llaveAnonimaSupabase) {
  console.error("⚠️ Error: No se encontraron las credenciales en el archivo .env")
}

export const clienteSupabase = createClient(urlSupabase, llaveAnonimaSupabase)