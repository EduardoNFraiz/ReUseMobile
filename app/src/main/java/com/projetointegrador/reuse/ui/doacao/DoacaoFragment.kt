package com.projetointegrador.reuse.ui.doacao

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.ContaPessoaJuridica
import com.projetointegrador.reuse.data.model.Instituicao
import com.projetointegrador.reuse.data.model.TipoConta
import com.projetointegrador.reuse.databinding.FragmentDoacaoBinding
import com.projetointegrador.reuse.ui.adapter.InstituicaoAdapter
import com.projetointegrador.reuse.util.LatLng
import com.projetointegrador.reuse.util.getLatLngFromCep
import com.projetointegrador.reuse.util.showBottomSheet
import com.projetointegrador.reuse.util.calculateHaversineDistance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.DecimalFormat
import kotlin.coroutines.resume

class DoacaoFragment : Fragment() {
    private var _binding: FragmentDoacaoBinding? = null
    private val binding get() = _binding!!
    private lateinit var instituicaoAdapter: InstituicaoAdapter

    // Variáveis do Firebase
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var instituicaoListener: ValueEventListener? = null

    // Variáveis de Localização
    private var userLatLng: LatLng? = null
    // Substitua pelo CEP REAL do usuário (deve vir do Firebase ou Localização)
    private val userCep = "29101010"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoacaoBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        auth = Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerViewInstituicoes(emptyList())

        // Inicia a busca de localização do usuário e carrega a lista
        fetchUserLocationAndLoadInstituicoes()

        setupSearchListener()
        initListeners()
        barraDeNavegacao()
        val doacaoRealizada = arguments?.getBoolean("REALIZEI_DOACAO") ?: false
        if (doacaoRealizada) { mostrardialog() }
    }

    private fun initRecyclerViewInstituicoes(instuicaoList: List<Instituicao>){
        instituicaoAdapter = InstituicaoAdapter(instuicaoList)
        binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = instituicaoAdapter
    }

    /**
     * Configura o listener para o campo de pesquisa.
     */
    private fun setupSearchListener() {
        binding.editTextProcurar.doAfterTextChanged { editable ->
            val searchText = editable.toString().trim()
            // Recarrega a lista aplicando o filtro
            fetchUserLocationAndLoadInstituicoes(searchText)
        }
    }

    /**
     * Tenta obter as coordenadas do CEP do usuário e, em seguida, carrega e filtra as instituições.
     */
    private fun fetchUserLocationAndLoadInstituicoes(searchText: String? = null) {
        lifecycleScope.launch {
            // Se as coordenadas do usuário ainda não foram obtidas, busca
            if (userLatLng == null) {
                userLatLng = getLatLngFromCep(userCep, requireContext())
                if (userLatLng == null) {
                    showBottomSheet(message = "Erro ao obter sua localização. A distância será omitida.")
                }
            }
            // Carrega a lista com as coordenadas do usuário (userLatLng pode ser null)
            loadInstituicoes(searchText, userLatLng)
        }
    }

    /**
     * NOVO MÉTODO: Busca o CEP real no nó 'enderecos' do Realtime Database usando Coroutines.
     * @param addressUid O UID do endereço que está salvo na ContaPessoaJuridica.
     */
    private suspend fun fetchCepByAddressUidRTDB(addressUid: String): String? =
        suspendCancellableCoroutine { continuation ->

            // Caminha até enderecos/UID_ENDERECO/cep
            database.child("enderecos").child(addressUid).child("cep")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cep = snapshot.getValue(String::class.java)
                        continuation.resume(cep)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RTDB", "Erro ao buscar CEP para UID $addressUid: ${error.message}")
                        continuation.resume(null)
                    }
                })
        }


    /**
     * Carrega, filtra e calcula a distância das instituições.
     */
    private fun loadInstituicoes(searchText: String?, userLocation: LatLng?) {
        val path = "usuarios/pessoaJuridica/instituicoes"
        val currentUserId = auth.currentUser?.uid
        val searchLower = searchText?.lowercase() ?: ""

        // Remove listeners antigos
        instituicaoListener?.let { database.child(path).removeEventListener(it) }

        // Define o novo listener
        instituicaoListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                // Mapeia e calcula a distância em paralelo
                val deferredInstituicoes = snapshot.children.mapNotNull { instSnapshot ->
                    // Usando Dispatchers.IO para operações de rede/DB assíncronas
                    lifecycleScope.async(Dispatchers.IO) {

                        val uid = instSnapshot.key ?: return@async null

                        // 1. FILTRO DE USUÁRIO
                        if (uid == currentUserId) return@async null

                        val contaPJ = instSnapshot.getValue(ContaPessoaJuridica::class.java)
                        if (contaPJ == null) return@async null

                        // Lógica de Distância
                        var distancia: String
                        val addressUid = contaPJ.endereço

                        if (userLocation != null && !addressUid.isNullOrEmpty()) {

                            // 2. BUSCA O CEP REAL DO NÓ 'enderecos' (CORREÇÃO)
                            val cepReal = fetchCepByAddressUidRTDB(addressUid)

                            if (!cepReal.isNullOrEmpty()) {
                                // 3. Agora geolocaliza o CEP REAL
                                val instLocation = getLatLngFromCep(cepReal, requireContext())

                                if (instLocation != null) {
                                    val distanceKm = calculateHaversineDistance(
                                        userLocation.latitude, userLocation.longitude,
                                        instLocation.latitude, instLocation.longitude
                                    )
                                    // Formata para 1 casa decimal
                                    distancia = "${DecimalFormat("#.#").format(distanceKm)} km de distância"
                                } else {
                                    distancia = "Localização não encontrada"
                                }
                            } else {
                                distancia = "Distância indisponível" // CEP não encontrado no RTDB
                            }

                        } else {
                            distancia = "Distância indisponível" // User Location ou Address UID vazio
                        }

                        // Mapeamento
                        val instituicaoDisplay = Instituicao(
                            fotoBase64 = contaPJ.fotoBase64,
                            uid = uid,
                            name = contaPJ.nomeDeUsuario,
                            distancia = distancia,
                            conta = TipoConta.INSTITUICAO
                        )

                        // 4. FILTRO DE PESQUISA
                        val nomeLower = instituicaoDisplay.name.lowercase()
                        if (searchLower.isEmpty() || nomeLower.contains(searchLower)) {
                            return@async instituicaoDisplay
                        }
                        return@async null
                    }
                }

                // Aguarda todas as coroutines e atualiza o UI na Main Thread
                lifecycleScope.launch(Dispatchers.Main) {
                    val instituicaoListForDisplay = deferredInstituicoes.mapNotNull { it.await() }

                    // Ordena pela distância (extrai o número para ordenar, ignora texto)
                    instituicaoAdapter.updateList(instituicaoListForDisplay.sortedBy {
                        it.distancia.substringBefore(" ")?.toDoubleOrNull() ?: Double.MAX_VALUE
                    })

                    if (instituicaoListForDisplay.isEmpty()) {
                        val message = if (searchLower.isNotEmpty()) "Nenhuma instituição encontrada com o nome \"$searchText\"." else "Nenhuma instituição de doação encontrada."
                        showBottomSheet(message = message)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Erro ao carregar instituições: ${error.message}", Toast.LENGTH_LONG).show()
                instituicaoAdapter.updateList(emptyList())
            }
        }

        database.child(path).addValueEventListener(instituicaoListener as ValueEventListener)
    }


    private fun mostrardialog() {
        val dialog = DialogDoacaoFragment()
        dialog.show(parentFragmentManager,"doacao concluida")
    }

    private fun initListeners() {
        // Se houver outros listeners, adicione aqui
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.cadastrarRoupa,bundle) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove o listener ao destruir a View
        instituicaoListener?.let {
            database.child("usuarios/pessoaJuridica/instituicoes").removeEventListener(it)
        }
        _binding = null
    }
}