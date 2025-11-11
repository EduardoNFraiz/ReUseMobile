package com.projetointegrador.reuse.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log // 🛑 NOVO IMPORT PARA DEBUG
import com.projetointegrador.reuse.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*

// Raio da Terra em Quilômetros
private const val EARTH_RADIUS_KM = 6371.0

data class LatLng(val latitude: Double, val longitude: Double)

fun calculateHaversineDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = Math.pow(Math.sin(dLat / 2), 2.0) +
            Math.pow(Math.sin(dLon / 2), 2.0) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return EARTH_RADIUS_KM * c
}

/**
 * Converte um CEP para coordenadas LatLng, com validação de formato.
 * @param cep O CEP da instituição (pode conter formatação).
 * @param context Contexto para inicializar o Geocoder.
 * @return LatLng se for bem-sucedido, ou null em caso de erro.
 */
suspend fun getLatLngFromCep(cep: String, context: Context): LatLng? {
    return withContext(Dispatchers.IO) {

        // 🛑 1. Limpeza e Validação do CEP antes de chamar a API
        val cepLimpo = cep.replace(Regex("[^0-9]"), "")

        if (cepLimpo.length != 8) {
            Log.e("LocationUtils", "CEP inválido ou incompleto: $cepLimpo")
            return@withContext null
        }

        // 2. Obter endereço completo (ViaCEP)
        val cepResponse = try {
            // Usa o CEP limpo garantido com 8 dígitos
            RetrofitClient.viaCepService.getAddressByCep(cepLimpo)
        } catch (e: Exception) {
            // Este catch pega o HTTP 400 Bad Request
            Log.e("LocationUtils", "Erro HTTP 400/Rede ao buscar ViaCEP para $cepLimpo: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }

        if (cepResponse.erro || cepResponse.localidade.isEmpty()) {
            Log.e("LocationUtils", "CEP $cepLimpo não encontrado no ViaCEP.")
            return@withContext null // CEP válido, mas não encontrado no ViaCEP
        }

        // 3. Converter endereço para LatLng (Android Geocoder)
        val fullAddress = "${cepResponse.logradouro}, ${cepResponse.bairro}, ${cepResponse.localidade}, ${cepResponse.uf}, Brasil"
        val geocoder = Geocoder(context, Locale("pt", "BR"))

        val addresses: List<Address>? = try {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(fullAddress, 1)
        } catch (e: Exception) {
            Log.e("LocationUtils", "Erro no Geocoder para o endereço: $fullAddress", e)
            null
        }

        // 4. Retorna a primeira coordenada encontrada
        return@withContext if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            LatLng(address.latitude, address.longitude)
        } else {
            Log.w("LocationUtils", "Geocoder não encontrou LatLng para $fullAddress. (Problema de Play Services ou Endereço)")
            null
        }
    }
}