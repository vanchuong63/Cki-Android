package com.example.bluetooth.subpabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime


val supabase = createSupabaseClient(
    supabaseUrl = "https://onjsgzthfxlvcuolfczb.supabase.co",
    supabaseKey = "sb_publishable_wnKSk4-3-6tEaFhTj3_pJQ_Bcm6oELo"
) {
    install(Postgrest)
    install(Realtime)

}