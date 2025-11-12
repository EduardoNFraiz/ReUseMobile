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

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var instituicaoListener: ValueEventListener? = null

    private var userLatLng: LatLng? = null
    private var userCep: String? = null
    private var userAddressUid: String? = null

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

        // 🛑 Chamada única para buscar dados do usuário e carregar instituições
        fetchUserAddressUidAndCep()

        setupSearchListener()
        initListeners()
        barraDeNavegacao()
        val doacaoRealizada = arguments?.getBoolean("REALIZEI_DOACAO") ?: false
        if (doacaoRealizada) { mostrardialog() }
    }

    private fun initRecyclerViewInstituicoes(instuicaoList: List<Instituicao>){
        val onInstitutionClick: (String) -> Unit = { instituicaoUid ->
            val action = DoacaoFragmentDirections.actionDoacaoFragmentToSobreInstituicaoFragment(instituicaoUid)
            findNavController().navigate(action)
        }

        instituicaoAdapter = InstituicaoAdapter(instuicaoList, onInstitutionClick)
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
            loadInstituicoes(searchText, userLatLng)
        }
    }

    /**
     * 🛑 REFATORADO: Busca o UID do endereço do usuário logado, independentemente de ser PF ou PJ.
     */
    private fun fetchUserAddressUidAndCep(searchText: String? = null) {
        val currentUserId = auth.currentUser?.uid ?: run {
            showBottomSheet(message = "Usuário não logado. A distância será omitida.")
            fetchUserLocationAndLoadInstituicoes(searchText)
            return
        }

        // Nodos onde o usuário logado pode ter endereço
        val paths = listOf(
            "usuarios/pessoaFisica/$currentUserId",
            "usuarios/pessoaJuridica/brechos/$currentUserId",
            "usuarios/pessoaJuridica/instituicoes/$currentUserId"
        )

        var foundAddress = false

        paths.forEach { path ->
            database.child(path).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Tenta obter o 'enderecoUid' ou 'endereço' (assumindo que PJ usa 'endereço')
                    val addressUidPF = snapshot.child("enderecoUid").getValue(String::class.java)
                    val addressUidPJ = snapshot.child("endereço").getValue(String::class.java)

                    val foundUid = addressUidPF ?: addressUidPJ

                    if (!foundUid.isNullOrEmpty() && !foundAddress) {
                        userAddressUid = foundUid
                        foundAddress = true // Marca que encontrou para evitar re-execução

                        lifecycleScope.launch {
                            userCep = fetchCepByAddressUidRTDB(userAddressUid!!)

                            if (userCep.isNullOrEmpty()) {
                                showBottomSheet(message = "CEP do usuário não encontrado. A distância será omitida.")
                            }
                            // Continua o fluxo para obter LatLng e carregar instituições
                            fetchUserLocationAndLoadInstituicoes(searchText)
                        }
                    } else if (path == paths.last() && !foundAddress) {
                        // Se é o último caminho e não encontrou, mostra erro e prossegue sem localização
                        showBottomSheet(message = "Endereço do usuário não encontrado. A distância será omitida.")
                        fetchUserLocationAndLoadInstituicoes(searchText)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RTDB", "Erro ao buscar endereço do usuário: ${error.message}")
                    if (path == paths.last() && !foundAddress) {
                        showBottomSheet(message = "Erro ao buscar seu endereço. A distância será omitida.")
                        fetchUserLocationAndLoadInstituicoes(searchText)
                    }
                }
            })
        }
    }

    /**
     * Tenta obter as coordenadas do CEP do usuário e, em seguida, carrega e filtra as instituições.
     */
    private fun fetchUserLocationAndLoadInstituicoes(searchText: String? = null) {
        lifecycleScope.launch {
            // Se o CEP foi encontrado, busca as coordenadas
            if (userCep != null && userLatLng == null) {
                userLatLng = getLatLngFromCep(userCep!!, requireContext())
                if (userLatLng == null) {
                    showBottomSheet(message = "Erro ao obter sua localização. A distância será omitida.")
                }
            }
            // Carrega a lista com as coordenadas do usuário (userLatLng pode ser null)
            loadInstituicoes(searchText, userLatLng)
        }
    }

    /**
     * Busca o CEP real no nó 'enderecos' do Realtime Database usando Coroutines.
     * @param addressUid O UID do endereço.
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
                    lifecycleScope.async(Dispatchers.IO) {

                        val uid = instSnapshot.key ?: return@async null

                        // 1. FILTRO DE USUÁRIO: Não lista a própria instituição do usuário (se ele for uma)
                        if (uid == currentUserId) return@async null

                        val contaPJ = instSnapshot.getValue(ContaPessoaJuridica::class.java)
                        if (contaPJ == null) return@async null

                        // Lógica de Distância
                        var distancia: String
                        val addressUid = contaPJ.endereço

                        if (userLocation != null && !addressUid.isNullOrEmpty()) {

                            // 2. BUSCA O CEP REAL DO NÓ 'enderecos'
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
                            name = contaPJ.nomeCompleto,
                            username = contaPJ.nomeDeUsuario,
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
                        it.distancia.substringBefore(" ").toDoubleOrNull() ?: Double.MAX_VALUE
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