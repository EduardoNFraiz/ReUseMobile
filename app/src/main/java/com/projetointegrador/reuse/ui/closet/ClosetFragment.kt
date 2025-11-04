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
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet


class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!
    private lateinit var GavetaAdapter: GavetaAdapter

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

        // Inicia o carregamento das gavetas
        loadUserGavetas()
    }

    /**
     * Inicializa o RecyclerView. A lista agora é de Pair<Gaveta, String> (Objeto Gaveta e seu UID).
     */
    private fun initRecyclerViewTask(gavetaList: List<Pair<Gaveta, String>>){
        // Configura o Adapter, passando a lista de pares e a lambda de clique que recebe o UID
        GavetaAdapter = GavetaAdapter(gavetaList) { clickedGavetaUID ->
            // Ação a ser executada quando um item é clicado
            navigateToGavetaFragment(clickedGavetaUID)
        }
        binding.recyclerViewGaveta.setHasFixedSize(true)
        binding.recyclerViewGaveta.adapter = GavetaAdapter
    }

    /**
     * Realiza a navegação para o GavetaFragment, passando o UID da gaveta via Bundle.
     */
    private fun navigateToGavetaFragment(gavetaUID: String) {
        if (gavetaUID.isEmpty()) {
            showBottomSheet(message = "ID da gaveta não encontrado. Não é possível navegar.")
            return
        }

        val bundle = Bundle().apply {
            // Passa o UID da gaveta via Bundle
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
                }
            })
    }

    // Função auxiliar para buscar subtipos de Pessoa Jurídica
    private fun searchPessoaJuridicaForGavetaUids(userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && !found) {
                            found = true
                            // Usuário é Pessoa Jurídica com o subtipo encontrado
                            getGavetaUidsFromUser(userId, "pessoaJuridica", subtipo)
                        }

                        if (subtipo == subtipos.last() && !found) {
                            // Se terminou de buscar em todos e não achou, a lista fica vazia.
                            showBottomSheet(message = "Nenhuma gaveta encontrada ou tipo de conta não identificado.")
                            initRecyclerViewTask(emptyList())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = "Erro ao buscar subtipo: ${error.message}")
                        initRecyclerViewTask(emptyList())
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
                        initRecyclerViewTask(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao listar UIDs das gavetas: ${error.message}")
                    initRecyclerViewTask(emptyList())
                }
            })
    }

    // 4. Busca os dados completos de cada gaveta
    private fun fetchGavetaDetails(gavetaUids: List<String>) {
        // A lista agora é de pares (Gaveta sem modificação, e o UID)
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
                            // Adiciona o PAR (Gaveta, UID) à lista, mantendo Gaveta intocada.
                            loadedGavetasWithUids.add(Pair(gaveta, uid))
                        }

                        gavetasCarregadas++

                        // Quando todas as gavetas forem carregadas, atualiza o RecyclerView
                        if (gavetasCarregadas == totalGavetas) {
                            // Passa a lista de pares para o Adapter
                            initRecyclerViewTask(loadedGavetasWithUids)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = "Erro ao buscar detalhes da gaveta $uid: ${error.message}")
                        gavetasCarregadas++
                        if (gavetasCarregadas == totalGavetas) {
                            initRecyclerViewTask(loadedGavetasWithUids)
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
                putBoolean("HIDE_EDIT_BUTTONS", true)
            }
            findNavController().navigate(R.id.action_closetFragment_to_criarGavetaFragment, bundle)
        }
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.pesquisar.setOnClickListener {
            findNavController().navigate(R.id.pesquisar)
        }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.cadastrarRoupa,bundle)
        }
        binding.doacao.setOnClickListener {
            findNavController().navigate(R.id.doacao)
        }
        binding.perfil.setOnClickListener {
            findNavController().navigate(R.id.perfil)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}