package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.projetointegrador.reuse.util.initToolbar // Mantido se for usado
import com.projetointegrador.reuse.util.showBottomSheet


class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!

    // O Adapter deve ser inicializado imediatamente, mas como está lateinit,
    // faremos a inicialização no novo método setupRecyclerView
    private lateinit var gavetaAdapter: GavetaAdapter // Renomeado para seguir convenção Kotlin

    private lateinit var reference: DatabaseReference
    private lateinit var auth: FirebaseAuth


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

        // 1. Configura o RecyclerView ANTES de buscar os dados.
        // Isso garante que o Adapter esteja anexado na primeira passagem de layout.
        setupRecyclerView()

        // 2. Inicia o carregamento das gavetas
        loadUserGavetas()
    }

    /**
     * Inicializa a RecyclerView e anexa o Adapter com uma lista vazia.
     */
    private fun setupRecyclerView() {
        // Inicializa a Adapter com uma lista vazia (inicial)
        gavetaAdapter = GavetaAdapter(emptyList()) { clickedGavetaUID ->
            // Ação a ser executada quando um item é clicado (Navegação para listagem de PEÇAS)
            navigateToGavetaFragment(clickedGavetaUID)
        }
        // Anexa o Adapter imediatamente
        binding.recyclerViewGaveta.adapter = gavetaAdapter
        binding.recyclerViewGaveta.setHasFixedSize(true)
    }

    /**
     * Atualiza a lista do Adapter após os dados serem carregados.
     */
    private fun updateRecyclerViewData(gavetaList: List<Pair<Gaveta, String>>){
        // Chama o novo método de atualização no Adapter
        gavetaAdapter.updateList(gavetaList)
    }

    /**
     * Realiza a navegação para o GavetaFragment (listagem de PEÇAS), passando o UID da gaveta via Bundle.
     */
    private fun navigateToGavetaFragment(gavetaUID: String) {
        if (gavetaUID.isEmpty()) {
            showBottomSheet(message = "ID da gaveta não encontrado. Não é possível navegar.")
            return
        }

        val bundle = Bundle().apply {
            // Passa o UID da gaveta para o Fragment de listagem de peças
            putString("GAVETA_ID", gavetaUID)
        }
        findNavController().navigate(R.id.action_closetFragment_to_gavetaFragment, bundle)
    }


    // --- LÓGICA DE CARREGAMENTO DO FIREBASE ---

    private fun loadUserGavetas() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showBottomSheet(message = "Usuário não autenticado.")
            return
        }

        // 1. Tentar encontrar o tipo de conta (Pessoa Física ou Jurídica)
        reference.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Usuário é Pessoa Física
                        getGavetaUidsFromUser(userId, "pessoaFisica", null)
                    } else {
                        // Se não for Pessoa Física, tentar encontrar em 'pessoaJuridica'
                        searchPessoaJuridicaForGavetaUids(userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao buscar tipo de conta: ${error.message}")
                    updateRecyclerViewData(emptyList()) // Atualiza com lista vazia em caso de erro
                }
            })
    }

    // Função auxiliar para buscar subtipos de Pessoa Jurídica
    private fun searchPessoaJuridicaForGavetaUids(userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false
        var checkedCount = 0 // Contador para verificar se todas as buscas terminaram

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        checkedCount++
                        if (snapshot.exists() && !found) {
                            found = true
                            // Usuário é Pessoa Jurídica com o subtipo encontrado
                            getGavetaUidsFromUser(userId, "pessoaJuridica", subtipo)
                            return // Sai após encontrar o tipo de conta
                        }

                        if (checkedCount == subtipos.size && !found) {
                            // Se terminou de buscar em todos e não achou, a lista fica vazia.
                            showBottomSheet(message = "Nenhuma gaveta encontrada ou tipo de conta não identificado.")
                            updateRecyclerViewData(emptyList())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        checkedCount++
                        if (checkedCount == subtipos.size) {
                            showBottomSheet(message = "Erro ao buscar subtipo: ${error.message}")
                            updateRecyclerViewData(emptyList())
                        }
                    }
                })
        }
    }

    // 2. Obtém os UIDs das gavetas a partir do nó do usuário
    private fun getGavetaUidsFromUser(userId: String, tipoConta: String, subtipo: String?) {
        val path = if (tipoConta == "pessoaFisica") {
            "usuarios/pessoaFisica/$userId/gavetas"
        } else {
            "usuarios/pessoaJuridica/$subtipo/$userId/gavetas"
        }

        reference.child(path)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaUids = mutableListOf<String>()
                    // O snapshot.children são os UIDs das gavetas
                    for (gavetaSnapshot in snapshot.children) {
                        gavetaUids.add(gavetaSnapshot.key!!)
                    }

                    if (gavetaUids.isNotEmpty()) {
                        // 3. Buscar os dados completos
                        fetchGavetaDetails(gavetaUids)
                    } else {
                        // Nenhuma gaveta cadastrada
                        showBottomSheet(message = "Você ainda não possui gavetas cadastradas.")
                        updateRecyclerViewData(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao listar UIDs das gavetas: ${error.message}")
                    updateRecyclerViewData(emptyList())
                }
            })
    }

    // 4. Busca os dados completos de cada gaveta
    private fun fetchGavetaDetails(gavetaUids: List<String>) {
        val loadedGavetasWithUids = mutableListOf<Pair<Gaveta, String>>()
        val totalGavetas = gavetaUids.size
        var gavetasCarregadas = 0

        for (uid in gavetaUids) {
            reference.child("gavetas").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Converte o DataSnapshot para o objeto Gaveta (sem modificar o UID)
                        val gaveta = snapshot.getValue(Gaveta::class.java)
                        if (gaveta != null) {
                            // Adiciona o PAR (Gaveta, UID) à lista
                            loadedGavetasWithUids.add(Pair(gaveta, uid))
                        }

                        gavetasCarregadas++

                        // Quando todas as gavetas forem carregadas, atualiza o RecyclerView
                        if (gavetasCarregadas == totalGavetas) {
                            // Passa a lista de pares para o Adapter
                            updateRecyclerViewData(loadedGavetasWithUids)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = "Erro ao buscar detalhes da gaveta $uid: ${error.message}")
                        gavetasCarregadas++
                        if (gavetasCarregadas == totalGavetas) {
                            // Atualiza mesmo com falha em algumas gavetas
                            updateRecyclerViewData(loadedGavetasWithUids)
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

        binding.buttonTeste.setOnClickListener {
            findNavController().navigate(R.id.compra)
        }
        binding.buttonCriarGaveta.setOnClickListener {
            val bundle = Bundle().apply {
                // Ao criar, NÃO passamos VISUALIZAR_INFO e nem GAVETA_ID
                // Deixando o gavetaId nulo, o CriarGavetaFragment entra no modo Criação.
                // removemos a flag "HIDE_EDIT_BUTTONS" pois ela não é mais necessária no novo fluxo
            }
            findNavController().navigate(R.id.action_closetFragment_to_criarGavetaFragment, bundle)
        }
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
        _binding = null
    }
}