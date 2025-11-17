package com.projetointegrador.reuse.ui.closet

import HistoricoAdapter
import android.os.Bundle
import android.util.Log
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
import com.projetointegrador.reuse.data.model.AvaliacaoBanco
import com.projetointegrador.reuse.data.model.Historico
import com.projetointegrador.reuse.data.model.Peca
import com.projetointegrador.reuse.data.model.TransacaoCompra
import com.projetointegrador.reuse.data.model.TransacaoDoacao
import com.projetointegrador.reuse.databinding.FragmentHistoricoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.android.gms.tasks.Tasks // Necessário para a busca sequencial
import java.util.concurrent.atomic.AtomicInteger

class HistoricoFragment : Fragment() {
    private var _binding: FragmentHistoricoBinding? = null
    private val binding get() = _binding!!

    private lateinit var historicoAdapter: HistoricoAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Variáveis para rastrear o estado das buscas assíncronas
    private val pendingFetches = AtomicInteger(0)
    private val historicoFinalList = mutableListOf<Historico>()
    private val userId: String by lazy { auth.currentUser?.uid ?: "" }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoricoBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        auth = Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
        setupRecyclerView()
        loadAllHistoricoData()
    }

    private fun initListeners() { /* Nada a fazer aqui por enquanto */ }

    private fun setupRecyclerView(){
        // 🛑 CORREÇÃO: O callback agora aceita apenas o avaliacaoUID como String
        historicoAdapter = HistoricoAdapter(mutableListOf()) { avaliacaoUID ->

            // 1. Busca o item Historico completo na lista que foi carregada, usando o avaliacaoUID
            val historicoItem = historicoFinalList.find { it.avaliacaoUID == avaliacaoUID }

            if (historicoItem != null) {
                // 2. Cria o Bundle com base nos dados do item encontrado
                val bundle = Bundle().apply {
                    putString("AVALIACAO_ID", historicoItem.avaliacaoUID)
                }

                Log.e("Historico", "NAVEGANDO")
                findNavController().navigate(R.id.action_historicoFragment_to_adicionarAvaliacaoFragment, bundle)
            } else {
                Log.e("Historico", "Item Historico não encontrado para AvaliacaoUID: $avaliacaoUID")
                showBottomSheet(message = "Erro ao localizar detalhes da avaliação.")
            }
        }

        binding.recyclerViewHist.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewHist.setHasFixedSize(true)
        binding.recyclerViewHist.adapter = historicoAdapter
    }

    // --- FUNÇÕES DE BUSCA UNIFICADA ---

    private fun loadAllHistoricoData() {
        if (userId.isEmpty()) {
            showBottomSheet(message = "Usuário não autenticado.")
            return
        }

        historicoFinalList.clear()
        pendingFetches.set(0) // Reseta o contador

        // Chamadas assíncronas para buscar os dois tipos de transação
        fetchDoacaoData()
        fetchCompraData()
    }

    private fun fetchDoacaoData() {
        // Incrementa o contador antes de iniciar a busca
        pendingFetches.incrementAndGet()

        database.child("transacoes/doacao")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val doacoesList = snapshot.children.mapNotNull { it.getValue(TransacaoDoacao::class.java) }
                        .filter { it.doadorUID == userId || it.instituicaoUID == userId }

                    processTransacoes(doacoesList)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Historico", "Erro ao buscar doações: ${error.message}")
                    checkIfAllProcessed()
                }
            })
    }

    private fun fetchCompraData() {
        // Incrementa o contador antes de iniciar a busca
        pendingFetches.incrementAndGet()

        database.child("transacoes/compra")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val comprasList = snapshot.children.mapNotNull { it.getValue(TransacaoCompra::class.java) }
                        .filter { it.compradorUID == userId || it.vendedorUID == userId }

                    processTransacoes(comprasList)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Historico", "Erro ao buscar compras: ${error.message}")
                    checkIfAllProcessed()
                }
            })
    }

    private fun processTransacoes(transacoes: List<Any>) {
        val totalTransacoes = transacoes.size
        var transacoesProcessadas = 0

        if (totalTransacoes == 0) {
            checkIfAllProcessed()
            return
        }

        for (transacao in transacoes) {
            fetchPecaAndAvaliacaoDetails(transacao) { historicoItem ->
                if (historicoItem != null) {
                    // Sincronizando o acesso à lista, embora o Firebase já faça na Main Thread
                    synchronized(historicoFinalList) {
                        historicoFinalList.add(historicoItem)
                    }
                }

                transacoesProcessadas++

                // Quando todas as transações daquela lista foram processadas, chama o finalizador
                if (transacoesProcessadas == totalTransacoes) {
                    checkIfAllProcessed()
                }
            }
        }
    }

    /**
     * Verifica se as buscas de Doação e Compra terminaram.
     */
    private fun checkIfAllProcessed() {
        // Decrementa o contador de buscas de nível superior (Doação ou Compra)
        if (pendingFetches.decrementAndGet() <= 0) {
            // ORDENAÇÃO FINAL: Mais atual primeiro
            val sortedList = historicoFinalList.sortedByDescending { parseDataString(it.dataHora) }
            historicoAdapter.updateList(sortedList)

            if (sortedList.isEmpty()) {
                showBottomSheet(message = "Nenhum histórico de transação encontrado.")
            }
        }
    }

    // --- FUNÇÕES DE DETALHE E FORMATAÇÃO ---

    private fun parseDataString(dateString: String): java.util.Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchProfileName(uid: String, callback: (String) -> Unit) {
        // 1. Tenta buscar como Pessoa Física (PF)
        database.child("usuarios/pessoaFisica").child(uid).child("nomeDeUsuario")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nomePessoaFisica = snapshot.getValue(String::class.java)
                    if (!nomePessoaFisica.isNullOrEmpty()) {
                        callback(nomePessoaFisica)
                        return
                    }

                    // 2. Se não for PF, tenta buscar como Instituição (PJ)
                    database.child("usuarios/pessoaJuridica/instituicoes").child(uid).child("nomeDeUsuario")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val nomeInstituicao = snapshot.getValue(String::class.java)
                                if (!nomeInstituicao.isNullOrEmpty()) {
                                    callback(nomeInstituicao)
                                    return
                                }

                                // 3. Se não for Instituição, tenta buscar como Brechó (PJ)
                                database.child("usuarios/pessoaJuridica/brechos").child(uid).child("nomeDeUsuario")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val nomeBrecho = snapshot.getValue(String::class.java)
                                            callback(nomeBrecho ?: "Perfil Desconhecido")
                                        }
                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e("Historico", "Erro ao buscar nome de Brechó: ${error.message}")
                                            callback("Erro na Busca")
                                        }
                                    })
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Historico", "Erro ao buscar nome de Instituição: ${error.message}")
                                // Se a busca por instituição falhar, tenta buscar Brechó
                                database.child("usuarios/pessoaJuridica/brechos").child(uid).child("nomeDeUsuario")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val nomeBrecho = snapshot.getValue(String::class.java)
                                            callback(nomeBrecho ?: "Perfil Desconhecido")
                                        }
                                        override fun onCancelled(error: DatabaseError) {
                                            callback("Erro na Busca")
                                        }
                                    })
                            }
                        })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Historico", "Erro ao buscar nome de Pessoa Física: ${error.message}")
                    callback("Erro na Busca")
                }
            })
    }

    private fun formatarDescricaoHistorico(userId: String, transacao: Any, nomePerfil: String): String {
        return when (transacao) {
            is TransacaoDoacao -> {
                val dataFormatada = transacao.dataDaTransacao.split(" ")[0]
                if (transacao.doadorUID == userId) {
                    "Você doou uma peça para $nomePerfil em $dataFormatada."
                } else if (transacao.instituicaoUID == userId) {
                    "Você recebeu uma doação de $nomePerfil em $dataFormatada."
                } else {
                    "Transação de Doação inválida."
                }
            }
            is TransacaoCompra -> {
                val dataFormatada = transacao.dataDaTransacao.split(" ")[0]
                if (transacao.compradorUID == userId) {
                    "Você comprou uma peça de $nomePerfil em $dataFormatada."
                } else if (transacao.vendedorUID == userId) {
                    "Você vendeu uma peça para $nomePerfil em $dataFormatada."
                } else {
                    "Transação de Compra inválida."
                }
            }
            else -> "Tipo de transação desconhecido."
        }
    }

    private fun fetchPecaAndAvaliacaoDetails(transacao: Any, callback: (Historico?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(null)

        // Extrai UIDs
        val pecaUID = when (transacao) {
            is TransacaoDoacao -> transacao.pecaUID
            is TransacaoCompra -> transacao.pecaUID
            else -> return callback(null)
        }
        val avaliacaoUID = when (transacao) {
            is TransacaoDoacao -> transacao.avaliacaoUID
            is TransacaoCompra -> transacao.avaliacaoUID
            else -> return callback(null)
        }
        val transacaoUID = when (transacao) {
            is TransacaoDoacao -> transacao.dataDaTransacao // Usando dataHora como chave temporária
            is TransacaoCompra -> transacao.dataDaTransacao // Usando dataHora como chave temporária
            else -> ""
        }

        // 1. Determina o UID do perfil oposto
        val perfilOpostoUid = when (transacao) {
            is TransacaoDoacao -> if (transacao.doadorUID == userId) transacao.instituicaoUID else transacao.doadorUID
            is TransacaoCompra -> if (transacao.compradorUID == userId) transacao.vendedorUID else transacao.compradorUID
            else -> ""
        }

        if (perfilOpostoUid.isEmpty()) return callback(null)

        // 2. Busca o nome do perfil oposto
        fetchProfileName(perfilOpostoUid) { nomePerfil ->

            val pecaRef = database.child("pecas").child(pecaUID)
            val avaliacaoRef = database.child("avaliacoes").child(avaliacaoUID)

            pecaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(pecaSnapshot: DataSnapshot) {
                    // Assumindo que Peca tem 'titulo' e 'fotoBase64'
                    val peca = pecaSnapshot.getValue(Peca::class.java)

                    avaliacaoRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(avaliacaoSnapshot: DataSnapshot) {
                            val avaliacao = avaliacaoSnapshot.getValue(AvaliacaoBanco::class.java)

                            if (peca != null && avaliacao != null) {

                                // Lógica de exibição do botão: Avaliação pendente E usuário é o avaliador
                                val avaliacaoPendente = !avaliacao.avaliado
                                val isAvaliador = when (transacao) {
                                    is TransacaoCompra -> transacao.compradorUID == userId
                                    is TransacaoDoacao -> transacao.instituicaoUID == userId
                                    else -> false
                                }
                                val showButton = avaliacaoPendente && isAvaliador

                                val description = formatarDescricaoHistorico(userId, transacao, nomePerfil)

                                val historicoItem = Historico(
                                    fotoBase64 = peca.fotoBase64 ?: "",
                                    name = peca.titulo ?: "Peça Desconhecida",
                                    description = description,
                                    button = showButton,
                                    dataHora = when (transacao) {
                                        is TransacaoDoacao -> transacao.dataDaTransacao
                                        is TransacaoCompra -> transacao.dataDaTransacao
                                        else -> "1970-01-01 00:00:00"
                                    },
                                    pecaUID = pecaUID,
                                    avaliacaoUID = avaliacaoUID
                                )
                                callback(historicoItem)
                            } else {
                                Log.e("Historico", "Falha ao carregar Peça/Avaliação (PecaUID: $pecaUID, AvaliacaoUID: $avaliacaoUID).")
                                callback(null)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) { callback(null) }
                    })
                }
                override fun onCancelled(error: DatabaseError) { callback(null) }
            })
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}