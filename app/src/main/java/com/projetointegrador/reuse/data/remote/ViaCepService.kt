package com.projetointegrador.reuse.data.remote

import com.projetointegrador.reuse.data.model.CepResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepService {
    @GET("{cep}/json/")
    suspend fun getAddressByCep(@Path("cep") cep: String): CepResponse
}