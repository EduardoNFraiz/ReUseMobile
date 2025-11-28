package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.databinding.FragmentClosetBinding
import com.projetointegrador.reuse.ui.adapter.GavetaAdapter
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet


class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!

    private lateinit var gavetaAdapter: GavetaAdapter
    private lateinit var reference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // 🛑 NOVO: Variável para armazenar o mapa de contagem de peças
    private var pecaCountMap: Map<String, Int> = emptyMap()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa Firebase
        reference = Firebase.database.reference
        auth = Firebase.auth

        initListeners()
        barraDeNavegacao()

        setupRecyclerView()

        // 🛑 INICIA A BUSCA COMPLETA SEM FILTRO
        loadUserGavetas(null)

        // 🛑 CONFIGURA O FILTRO DE PESQUISA
        setupSearchListener()
    }


    private fun setupSearchListener() {
        binding.editTextPesquisarGavetas.doAfterTextChanged { editable ->
            val searchText = editable.toString().trim()
            if (searchText.length >= 1) {
                loadUserGavetas(searchText)
            } else if (searchText.isEmpty()) {
                loadUserGavetas(null)
            }
        }
    }

    /**
     * Inicializa a RecyclerView e anexa o Adapter.
     */
    private fun setupRecyclerView() {
        // 🛑 ATUALIZADO: Inicializa o Adapter com lista vazia E mapa de contagem vazio
        gavetaAdapter = GavetaAdapter(emptyList(), pecaCountMap) { clickedGavetaUID ->
            navigateToGavetaFragment(clickedGavetaUID)
        }
        binding.recyclerViewGaveta.adapter = gavetaAdapter
        binding.recyclerViewGaveta.setHasFixedSize(true)
    }

    /**
     * Atualiza a lista do Adapter após os dados serem carregados.
     */
    private fun updateRecyclerViewData(gavetaList: List<Pair<Gaveta, String>>, countMap: Map<String, Int>){
        // 🛑 ATUALIZADO: Chama o updateList do Adapter passando a lista de gavetas e o mapa de contagem
        gavetaAdapter.updateList(gavetaList, countMap)
    }

    // Oculta o loading


    /**
     * Realiza a navegação para o GavetaFragment (listagem de PEÇAS), passando o UID da gaveta via Bundle.
     */
    private fun navigateToGavetaFragment(gavetaUID: String) {
        if (gavetaUID.isEmpty()) {
            showBottomSheet(message = getString(R.string.error_uid_gaveta_nao_encontrado))
            return
        }

        val bundle = Bundle().apply {
            // Passa o UID da gaveta para o Fragment de listagem de peças
            putString("GAVETA_ID", gavetaUID)
        }
        findNavController().navigate(R.id.action_closetFragment_to_gavetaFragment, bundle)
    }


    // --- LÓGICA DE CARREGAMENTO DO FIREBASE ---

    private fun loadUserGavetas(searchText: String?) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showBottomSheet(message = getString(R.string.error_usuario_nao_logado))
            updateRecyclerViewData(emptyList(), emptyMap()) // Atualiza com lista/mapa vazio
            return
        }

        // 1. Tentar encontrar o tipo de conta (Pessoa Física ou Jurídica)
        reference.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        getGavetaUidsFromUser(userId, "pessoaFisica", null, searchText)
                    } else {
                        searchPessoaJuridicaForGavetaUids(userId, searchText)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(
                        R.string.error_buscar_tipo_de_conta,
                        error.message
                    ))
                    updateRecyclerViewData(emptyList(), emptyMap())
                }
            })
    }

    // Função auxiliar para buscar subtipos de Pessoa Jurídica
    private fun searchPessoaJuridicaForGavetaUids(userId: String, searchText: String?) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false
        var checkedCount = 0

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        checkedCount++
                        if (snapshot.exists() && !found) {
                            found = true
                            getGavetaUidsFromUser(userId, "pessoaJuridica", subtipo, searchText)
                            return
                        }

                        if (checkedCount == subtipos.size && !found) {
                            showBottomSheet(message = getString(R.string.error_nenhuma_gaveta_encontrada_ou_tipo_conta_nao_identificado))
                            updateRecyclerViewData(emptyList(), emptyMap())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkedCount++
                        if (checkedCount == subtipos.size) {
                            showBottomSheet(message = getString(
                                R.string.error_buscar_subtipo,
                                error.message
                            ))
                            updateRecyclerViewData(emptyList(), emptyMap())
                        }
                    }
                })
        }
    }

    // 2. Obtém os UIDs das gavetas a partir do nó do usuário
    private fun getGavetaUidsFromUser(userId: String, tipoConta: String, subtipo: String?, searchText: String?) {
        // 🛑 AJUSTE: O caminho agora aponta diretamente para o nó principal 'gavetas'
        // A filtragem será feita pela query 'orderByChild'.
        val gavetasRef = reference.child("gavetas")

        gavetasRef.orderByChild("ownerUid")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaUids = mutableListOf<String>()

                    // O snapshot já contém apenas as gavetas onde ownerUid == userId
                    for (gavetaSnapshot in snapshot.children) {
                        gavetaUids.add(gavetaSnapshot.key!!)
                    }

                    if (gavetaUids.isNotEmpty()) {
                        // O fluxo de busca da contagem de peças e detalhes permanece o mesmo
                        // para aproveitar a estrutura existente.
                        fetchPecaCount(gavetaUids) { pecaCountMap ->
                            fetchGavetaDetails(gavetaUids, searchText, pecaCountMap)
                        }
                    } else {
                        showBottomSheet(message = getString(R.string.aviso_nao_possui_gavetas_cadastradas))
                        updateRecyclerViewData(emptyList(), emptyMap())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(
                        R.string.error_listar_uids_gavetas,
                        error.message
                    ))
                    updateRecyclerViewData(emptyList(), emptyMap())
                }
            })
    }
    // 🛑 NOVO: 3. Busca a contagem de peças para todos os UIDs
    private fun fetchPecaCount(gavetaUids: List<String>, onCountComplete: (Map<String, Int>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onCountComplete(emptyMap())
            return
        }
        // 🛑 QUERY OTIMIZADA: Busca apenas as peças cujo proprietário (ownerUid) é o usuário atual.
        reference.child("pecas")
            .orderByChild("ownerUid")
            .equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pecaCountMap = mutableMapOf<String, Int>()

                    // Inicializa o mapa com 0 para todas as gavetas do usuário
                    gavetaUids.forEach { pecaCountMap[it] = 0 }

                    // Itera sobre TODAS as peças (pode ser lento se o nó 'pecas' for muito grande)
                    // Uma otimização seria usar uma query filtrando pelo ownerUid do usuário.
                    for (pecaSnapshot in snapshot.children) {
                        val gavetaUid = pecaSnapshot.child("gavetaUid").getValue(String::class.java)

                        // Se o gavetaUid pertence a uma das gavetas do usuário, incrementa a contagem
                        if (gavetaUid != null && pecaCountMap.containsKey(gavetaUid)) {
                            pecaCountMap[gavetaUid] = (pecaCountMap[gavetaUid] ?: 0) + 1
                        }
                    }

                    onCountComplete(pecaCountMap)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Em caso de falha na contagem, passa um mapa vazio (ou com 0)
                    showBottomSheet(message = getString(R.string.error_contar_pecas, error.message))
                    onCountComplete(gavetaUids.associateWith { 0 })
                }
            })
    }


    // 4. Busca os dados completos de cada gaveta
    private fun fetchGavetaDetails(gavetaUids: List<String>, searchText: String?, pecaCountMap: Map<String, Int>) {
        val loadedGavetasWithUids = mutableListOf<Pair<Gaveta, String>>()
        val totalGavetas = gavetaUids.size
        var gavetasCarregadas = 0

        val searchLower = searchText?.lowercase() ?: ""

        for (uid in gavetaUids) {
            reference.child("gavetas").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val gaveta = snapshot.getValue(Gaveta::class.java)
                        if (gaveta != null) {
                            val nomeGavetaLower = gaveta.nome?.lowercase() ?: ""
                            if (searchLower.isEmpty() || nomeGavetaLower.contains(searchLower)) {
                                loadedGavetasWithUids.add(Pair(gaveta, uid))
                            }
                        }

                        gavetasCarregadas++

                        // Quando todas as gavetas forem carregadas, atualiza o RecyclerView
                        if (gavetasCarregadas == totalGavetas) {
                            // 🛑 ATUALIZADO: Passa a lista de pares E o mapa de contagem
                            updateRecyclerViewData(loadedGavetasWithUids, pecaCountMap)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = getString(
                            R.string.erro_ao_buscar_detalhes_da_gavetaa,
                            uid,
                            error.message
                        ))
                        gavetasCarregadas++
                        if (gavetasCarregadas == totalGavetas) {
                            // 🛑 ATUALIZADO: Passa o mapa de contagem
                            updateRecyclerViewData(loadedGavetasWithUids, pecaCountMap)
                        }
                    }
                })
        }
    }

    // --- RESTANTE DO CÓDIGO (LISTENERS) ---

    private fun initListeners() {
        binding.bttHistorico.setOnClickListener {
            findNavController().navigate(R.id.action_closetFragment_to_historicoFragment)
        }

        binding.buttonCriarGaveta.setOnClickListener {
            val bundle = Bundle().apply {
                // Ao criar, NÃO passamos VISUALIZAR_INFO e nem GAVETA_ID
            }
            findNavController().navigate(R.id.action_closetFragment_to_criarGavetaFragment, bundle)
        }
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val action = CriarGavetaFragmentDirections.actionGlobalCadRoupaFragment(
                pecaUID = null,
                gavetaUID = null
            )
            findNavController().navigate(action)
        }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}