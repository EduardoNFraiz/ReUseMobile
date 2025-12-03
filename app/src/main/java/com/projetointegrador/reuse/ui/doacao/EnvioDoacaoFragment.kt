package com.projetointegrador.reuse.ui.doacao

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.AvaliacaoBanco // Importe o AvaliacaoBanco
import com.projetointegrador.reuse.data.model.TransacaoDoacao
import com.projetointegrador.reuse.databinding.FragmentEnvioDoacaoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume


class EnvioDoacaoFragment : Fragment() {
    private var _binding: FragmentEnvioDoacaoBinding? = null
    private val binding get() = _binding!!

    // Variáveis do Firebase e Safe Args
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val args: EnvioDoacaoFragmentArgs by navArgs()

    // Dados da Instituição (apenas para a transação, sem exibição)
    private var instituicaoEnderecoCompleto: String? = null
    private var instituicaoCep: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = Firebase.database.reference
        auth = Firebase.auth

        // Inicia a busca pelo endereço da instituição (necessário para a transação)
        fetchInstituicaoAddress()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEnvioDoacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)

        binding.btnDoacao.isEnabled = false

        initListeners()

        binding.radioGroupOpcoes.setOnCheckedChangeListener { group, checkedId ->
            // HABILITA O BOTÃO SEMPRE QUE QUALQUER OPÇÃO FOR SELECIONADA
            binding.btnDoacao.isEnabled = checkedId != -1

            for (i in 0 until group.childCount) {
                val radio = group.getChildAt(i) as RadioButton
                if (radio.id == checkedId) {
                    radio.setTypeface(null, Typeface.BOLD)
                } else {
                    radio.setTypeface(null, Typeface.NORMAL)
                }
            }
        }
    }

    private fun initListeners(){
        binding.btnDoacao.setOnClickListener {
            val selectedId = binding.radioGroupOpcoes.checkedRadioButtonId

            if (selectedId != -1) {
                val selectedOption = view?.findViewById<RadioButton>(selectedId)?.text.toString()

                // INICIA O PROCESSO DA TRANSAÇÃO (Que inclui a Avaliação)
                processDoacaoTransaction(
                    pecasUids = args.pecasUIDS.toList(),
                    instituicaoUid = args.instituicaoUID,
                    formaEnvio = selectedOption
                )
            }
        }
    }

    /**
     * BUSCA O ENDEREÇO DA INSTITUIÇÃO (necessário para o campo enderecoDestino da transação).
     */
    private fun fetchInstituicaoAddress() {
        val instituicaoUid = args.instituicaoUID

        database.child("usuarios/pessoaJuridica/instituicoes").child(instituicaoUid).child("endereco")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val addressUid = snapshot.getValue(String::class.java)
                    if (addressUid.isNullOrEmpty()) {
                        Log.e("Doacao", "UID de Endereço da instituição não encontrado.")
                        return
                    }

                    lifecycleScope.launch {
                        val (cep, endereco) = fetchCepAndFullAddress(addressUid)
                        instituicaoCep = cep
                        instituicaoEnderecoCompleto = endereco
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Doacao", "Erro ao buscar addressUid da instituição: ${error.message}")
                }
            })
    }

    /**
     * Função de suspensão para buscar CEP e endereço completo.
     */
    private suspend fun fetchCepAndFullAddress(addressUid: String): Pair<String?, String?> =
        suspendCancellableCoroutine { continuation ->

            database.child("enderecos").child(addressUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val cep = snapshot.child("cep").getValue(String::class.java)
                        val rua = snapshot.child("rua").getValue(String::class.java) ?: ""
                        val numero = snapshot.child("numero").getValue(String::class.java) ?: ""
                        val bairro = snapshot.child("bairro").getValue(String::class.java) ?: ""
                        val cidade = snapshot.child("cidade").getValue(String::class.java) ?: ""
                        val estado = snapshot.child("estado").getValue(String::class.java) ?: ""

                        // Formato: Rua, Número - Bairro, Cidade/Estado (sem CEP)
                        val enderecoCompleto = "$rua, $numero - $bairro, $cidade/$estado"
                        continuation.resume(Pair(cep, enderecoCompleto))
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(Pair(null, null))
                    }
                })
        }

    /**
     * Função de suspensão para encontrar a gaveta "Recebidos" da instituição.
     */
    private suspend fun findRecipientDefaultDrawerUid(instituicaoUid: String): String? =
        suspendCancellableCoroutine { continuation ->

            // Busca a gaveta que tem ownerUid igual a instituicaoUid E o nome ("name") igual a "Recebidos".
            database.child("gavetas")
                .orderByChild("ownerUid")
                .equalTo(instituicaoUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val targetName = "Recebidos" // O nome da gaveta de destino

                        snapshot.children.forEach { gavetaSnapshot ->
                            val gavetaName = gavetaSnapshot.child("nome").getValue(String::class.java)

                            if (gavetaName == targetName) {
                                continuation.resume(gavetaSnapshot.key)
                                return // Retorna o UID da gaveta "Recebidos"
                            }
                        }
                        continuation.resume(null) // Gaveta "Recebidos" não encontrada
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Erro ao buscar gaveta 'Recebidos' do destinatário: ${error.message}")
                        continuation.resume(null)
                    }
                })
        }

    /**
     * Processa a transação em lote (Avaliação Padrão + Peças + Transações) e navega.
     */
    private fun processDoacaoTransaction(pecasUids: List<String>, instituicaoUid: String, formaEnvio: String) {
        val doadorUid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), R.string.error_usuario_nao_logado, Toast.LENGTH_LONG).show()
            return
        }

        if (instituicaoEnderecoCompleto.isNullOrEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.aviso_espere_carregamento_tente_novamente), Toast.LENGTH_LONG).show()
            return
        }



        lifecycleScope.launch(Dispatchers.IO) {

            val targetGavetaUid = findRecipientDefaultDrawerUid(instituicaoUid)

            if (targetGavetaUid.isNullOrEmpty()) {
                // Este lançamento DEVE ser mantido, pois showBottomSheet é UI.
                launch(Dispatchers.Main) {
                    showBottomSheet(message = getString(R.string.error_nao_foi_possivel_encontrar_recebidos_instituicao))
                }
                return@launch
            }

            val updates = mutableMapOf<String, Any>()
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // 1. CRIAR A AVALIAÇÃO PADRÃO (PENDENTE)
            val avaliacaoRef = database.child("avaliacoes").push()
            val avaliacaoUid = avaliacaoRef.key ?: run {
                launch(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_gerar_uid_avaliacao), Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // Cria o objeto AvaliacaoBanco com valores padrão/pendentes
            val avaliacaoData = AvaliacaoBanco(
                avaliadorUid = doadorUid,
                avaliadoUid = instituicaoUid,
                transacaoUid = "",
                description = "",
                rating = 5f,
                avaliado = false
            )

            updates["avaliacoes/$avaliacaoUid"] = avaliacaoData.copy(transacaoUid = "")

            // 2. ATUALIZAR PEÇAS E CRIAR TRANSAÇÕES
            pecasUids.forEach { pecaUid ->
                updates["pecas/$pecaUid/finalidade"] = "Organizar"
                updates["pecas/$pecaUid/gavetaUid"] = targetGavetaUid
                updates["pecas/$pecaUid/ownerUid"] = instituicaoUid

                val transacaoUid = database.child("transacoes/doacao").push().key ?: return@forEach

                val transacaoData = TransacaoDoacao(
                    doadorUID = doadorUid,
                    instituicaoUID = instituicaoUid,
                    dataDaTransacao = currentTime,
                    pecaUID = pecaUid,
                    enderecoDestino = instituicaoEnderecoCompleto!!,
                    formaEnvio = formaEnvio,
                    avaliacaoUID = avaliacaoUid
                )
                updates["transacoes/doacao/$transacaoUid"] = transacaoData

                if (updates["avaliacoes/$avaliacaoUid"] is AvaliacaoBanco && (updates["avaliacoes/$avaliacaoUid"] as AvaliacaoBanco).transacaoUid.isEmpty()) {
                    updates["avaliacoes/$avaliacaoUid"] = avaliacaoData.copy(transacaoUid = transacaoUid)
                }
            }

            // 3. Executar o Batch Update
            database.updateChildren(updates)
                .addOnSuccessListener {
                    // 🛑 launch(Dispatchers.Main) removido aqui. Confiando que o Firebase chama na UI Thread.
                    Toast.makeText(requireContext(),
                        getString(R.string.sucesso_doacao_registrada_avalie), Toast.LENGTH_LONG).show()

                    val bundle = Bundle().apply {
                        putBoolean("REALIZEI_DOACAO", true)
                    }

                    // 🛑 A navegação é o ponto de falha mais provável.
                    findNavController().navigate(R.id.action_envioDoacaoFragment_to_doacaoFragment, bundle)
                }
                .addOnFailureListener { e ->
                    // 🛑 launch(Dispatchers.Main) removido aqui. Confiando que o Firebase chama na UI Thread.
                    Log.e("Doacao", "Erro ao realizar transação de doação: ${e.message}")
                    Toast.makeText(requireContext(),
                        getString(R.string.error_finalizar_doacao, e.message), Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}