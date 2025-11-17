package com.projetointegrador.reuse.ui.avaliacao

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.Avaliacao
import com.projetointegrador.reuse.data.model.AvaliacaoBanco
import com.projetointegrador.reuse.databinding.FragmentAvaliacoesBinding
import com.projetointegrador.reuse.ui.adapter.AvaliacaoAdapter
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import kotlin.getValue

class AvaliacoesFragment : Fragment() {
    private var _binding: FragmentAvaliacoesBinding? = null
    private val binding get() = _binding!!
    private lateinit var avaliacaoAdapter: AvaliacaoAdapter // Corrigido o nome da variável
    private lateinit var database: DatabaseReference
    private var targetUserUid: String? = null // UID do usuário que queremos ver as avaliações
    private val args: AvaliacoesFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvaliacoesBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserUid = "do5jehRnBZMsUUGYNx93Ol7zQdE3"

        if (targetUserUid.isNullOrEmpty()) {
            showBottomSheet(message = "Erro: ID do usuário alvo não fornecido.")
            findNavController().popBackStack()
            return
        }

        initListeners()
        initToolbar(binding.toolbar)
        setupRecyclerView()
        loadAvaliacoes()
    }
    private fun initListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView(){
        avaliacaoAdapter = AvaliacaoAdapter(emptyList())
        binding.recyclerViewAvaliacao.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAvaliacao.setHasFixedSize(true)
        binding.recyclerViewAvaliacao.adapter = avaliacaoAdapter
    }

    /**
     * Busca todas as Avaliações onde o campo 'avaliadoUID' é igual ao targetUserUid.
     * 🛑 Requer que o seu modelo AvaliacaoBanco tenha o campo 'avaliadoUID'.
     */
    private fun loadAvaliacoes() {
        database.child("avaliacoes")
            .orderByChild("avaliadoUid")
            .equalTo(targetUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // 1. Mapeia todos os resultados encontrados (podem incluir avaliações pendentes)
                    val todasAvaliacoes = snapshot.children.mapNotNull { it.getValue(AvaliacaoBanco::class.java) }

                    // 🛑 2. FILTRO ESSENCIAL: Filtra localmente a lista para incluir apenas aquelas onde 'avaliado' é true
                    // Assumindo que o campo 'avaliado' existe e é um Boolean no seu modelo AvaliacaoBanco.
                    val avaliacoesConcluidas = todasAvaliacoes.filter { it.avaliado == true }

                    if (avaliacoesConcluidas.isEmpty()) {
                        showBottomSheet(message = "Este usuário ainda não recebeu avaliações.")
                        return
                    }

                    // 3. Processa apenas a lista filtrada de avaliações concluídas
                    processAvaliacoes(avaliacoesConcluidas)
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao carregar avaliações: ${error.message}")
                    Log.e("AvaliacoesFragment", "Erro Firebase: ${error.message}")
                }
            })
    }

    /**
     * Busca o nome e a foto do perfil que fez a avaliação (o 'avaliadorUID').
     */
    private fun processAvaliacoes(avaliacoesBanco: List<AvaliacaoBanco>) {
        val avaliacoesFinalList = mutableListOf<Avaliacao>()
        val totalAvaliacoes = avaliacoesBanco.size
        var avaliacoesProcessadas = 0

        for (avaliacaoBanco in avaliacoesBanco) {
            // 🛑 Assumindo que AvaliacaoBanco tem o campo 'avaliadorUID'
            val avaliadorUID = avaliacaoBanco.avaliadorUid

            fetchProfileDetails(avaliadorUID) { name, photoBase64 ->
                // Cria o objeto Avaliacao para o RecyclerView
                val avaliacaoItem = Avaliacao(
                    fotoBase64 = photoBase64,
                    name = name,
                    description = avaliacaoBanco.description,
                    rating = avaliacaoBanco.rating
                )
                avaliacoesFinalList.add(avaliacaoItem)

                avaliacoesProcessadas++

                if (avaliacoesProcessadas == totalAvaliacoes) {
                    // Atualiza a lista quando todas as buscas secundárias estiverem completas
                    avaliacaoAdapter.updateList(avaliacoesFinalList)
                }
            }
        }
    }

    /**
     * Busca o nome e a foto (Base64) de um perfil (PF, Instituição ou Brechó).
     */
    private fun fetchProfileDetails(uid: String, callback: (String, String?) -> Unit) {
        // Funções aninhadas para evitar repetição de código
        fun fetchPessoaJuridica(tipo: String, next: () -> Unit) {
            database.child("usuarios/pessoaJuridica/$tipo").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val nome = snapshot.child("nomeDeUsuario").getValue(String::class.java)
                        val foto = snapshot.child("fotoBase64").getValue(String::class.java)
                        if (!nome.isNullOrEmpty()) {
                            callback(nome, foto)
                        } else {
                            next()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        next()
                    }
                })
        }

        // Tenta PF
        database.child("usuarios/pessoaFisica").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomePF = snapshot.child("nomeDeUsuario").getValue(String::class.java)
                    val fotoPF = snapshot.child("fotoBase64").getValue(String::class.java)
                    if (!nomePF.isNullOrEmpty()) {
                        callback(nomePF, fotoPF)
                        return
                    }

                    // Tenta Instituição
                    fetchPessoaJuridica("instituicoes") {
                        // Tenta Brechó
                        fetchPessoaJuridica("brechos") {
                            // Se falhar em todos
                            callback("Perfil Desconhecido", null)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    callback("Erro na Busca", null)
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}