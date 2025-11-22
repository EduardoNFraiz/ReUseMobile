package com.projetointegrador.reuse.ui.compra

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.data.model.TransacaoCompra // Importe seu modelo
import com.projetointegrador.reuse.databinding.FragmentConfirmarCompraBinding
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
class ConfirmarCompraFragment : Fragment() {
    private var _binding: FragmentConfirmarCompraBinding? = null
    private val binding get() = _binding!!

    private val args: ConfirmarCompraFragmentArgs by navArgs()

    private lateinit var database: DatabaseReference
    private var currentPeca: PecaCadastro? = null
    private var enderecoCompletoStr: String = "" // Variável para armazenar o endereço completo

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmarCompraBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pecaUid = args.pecaUID

        initListeners(pecaUid)
        initToolbar(binding.toolbar)

        loadPecaData(pecaUid)
        loadEnderecoData()
    }

    // --- LÓGICA DE CARREGAMENTO DE DADOS PRINCIPAL ---

    private fun loadPecaData(pecaUid: String) {
        database.child("pecas").child(pecaUid).get()
            .addOnSuccessListener { snapshot ->
                val peca = snapshot.getValue(PecaCadastro::class.java)
                if (peca != null) {
                    currentPeca = peca
                    updatePecaUI(peca)
                } else {
                    Toast.makeText(requireContext(), "Erro: Peça não encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("ConfirmarCompra", "Erro ao buscar peça: ${it.message}")
                Toast.makeText(requireContext(), "Erro ao carregar dados da peça.", Toast.LENGTH_SHORT).show()
            }
    }

    // 🛑 FUNÇÃO INICIAL PARA CARREGAR ENDEREÇO (INICIA A CADEIA DE BUSCA)
    private fun loadEnderecoData() {
        val userId = currentUserId
        if (userId.isNullOrEmpty()) {
            binding.tvEndereco.text = "Erro: Usuário não autenticado."
            return
        }

        // Tenta o primeiro caminho: Pessoa Física
        tryLoadPFEndereco(userId)
    }

    // --- FUNÇÕES AUXILIARES PARA CADEIA DE BUSCA DE ENDEREÇO ---

    // 1. Tenta Pessoa Física
    private fun tryLoadPFEndereco(userId: String) {
        database.child("usuarios").child("pessoaFisica").child(userId).child("endereço").get()
            .addOnSuccessListener { snapshot ->
                val enderecoUid = snapshot.getValue(String::class.java)
                if (enderecoUid.isNullOrEmpty()) {
                    // Se falhar/nulo, tenta PJ - Instituições
                    tryLoadPJInstEndereco(userId)
                } else {
                    // Sucesso: carrega o endereço
                    fetchFullEndereco(enderecoUid)
                }
            }
            .addOnFailureListener {
                Log.e("ConfirmarCompra", "Falha na busca PF: ${it.message}")
                // Se erro na consulta, tenta PJ - Instituições
                tryLoadPJInstEndereco(userId)
            }
    }

    // 2. Tenta Pessoa Jurídica - Instituições
    private fun tryLoadPJInstEndereco(userId: String) {
        database.child("usuarios").child("pessoaJuridica").child("instituicoes").child(userId).child("endereço").get()
            .addOnSuccessListener { snapshot ->
                val enderecoUid = snapshot.getValue(String::class.java)
                if (enderecoUid.isNullOrEmpty()) {
                    // Se falhar/nulo, tenta PJ - Brechós
                    tryLoadPJBrechoEndereco(userId)
                } else {
                    // Sucesso: carrega o endereço
                    fetchFullEndereco(enderecoUid)
                }
            }
            .addOnFailureListener {
                Log.e("ConfirmarCompra", "Falha na busca PJ Inst: ${it.message}")
                // Se erro na consulta, tenta PJ - Brechós
                tryLoadPJBrechoEndereco(userId)
            }
    }

    // 3. Tenta Pessoa Jurídica - Brechós (última tentativa)
    private fun tryLoadPJBrechoEndereco(userId: String) {
        database.child("usuarios").child("pessoaJuridica").child("brechos").child(userId).child("endereço").get()
            .addOnSuccessListener { snapshot ->
                val enderecoUid = snapshot.getValue(String::class.java)
                if (enderecoUid.isNullOrEmpty()) {
                    // Falha total: nenhum endereço encontrado em nenhum caminho
                    Log.d("ConfirmarCompra", "Endereço não encontrado em nenhum caminho para o usuário $userId")
                    binding.tvEndereco.text = "Endereço de entrega não definido."
                } else {
                    // Sucesso: carrega o endereço
                    fetchFullEndereco(enderecoUid)
                }
            }
            .addOnFailureListener {
                Log.e("ConfirmarCompra", "Falha na busca PJ Brechó: ${it.message}")
                // Falha total, mostra mensagem de erro genérica
                binding.tvEndereco.text = "Erro ao carregar endereço."
            }
    }

    // FUNÇÃO QUE BUSCA OS DETALHES DO ENDEREÇO (mantida)
    private fun fetchFullEndereco(enderecoUid: String) {
        database.child("enderecos").child(enderecoUid).get()
            .addOnSuccessListener { snapshot ->

                val rua = snapshot.child("rua").getValue(String::class.java) ?: "Rua não informada"
                val numero = snapshot.child("numero").getValue(String::class.java) ?: "S/N"
                val cidade = snapshot.child("cidade").getValue(String::class.java) ?: "Cidade"
                val estado = snapshot.child("estado").getValue(String::class.java) ?: "Estado"

                val enderecoCompleto = "$rua, nº $numero\n$cidade - $estado"

                binding.tvEndereco.text = enderecoCompleto
                // 🛑 Armazena o endereço completo na variável de classe para uso na transação
                enderecoCompletoStr = enderecoCompleto
            }
            .addOnFailureListener {
                Log.e("ConfirmarCompra", "Erro ao buscar detalhes do endereço: ${it.message}")
                binding.tvEndereco.text = "Erro ao carregar detalhes do endereço."
            }
    }

    // --- LÓGICA DE ATUALIZAÇÃO E LISTENERS ---

    private fun updatePecaUI(peca: PecaCadastro) {
        binding.tvNomeProduto.text = peca.titulo ?: "Item sem título"
        binding.tvDescricao.text = peca.detalhe ?: "Sem descrição disponível."
        peca.fotoBase64?.let { base64 ->
            displayBase64Image(base64, binding.imgProduto)
        } ?: run {
            binding.imgProduto.setImageResource(R.drawable.closeticon) // Placeholder
        }
    }

    private fun initListeners(pecaUid: String) {
        binding.btnConfirmarPedido.setOnClickListener {
            val selectedPaymentId = binding.radioGroupPagamento.checkedRadioButtonId

            // 1. Validação de Pagamento
            if (selectedPaymentId == -1) {
                Toast.makeText(requireContext(), "Por favor, selecione uma forma de pagamento.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validação de Dados Essenciais
            if (currentPeca == null || currentUserId.isNullOrEmpty() || enderecoCompletoStr.isEmpty()) {
                Toast.makeText(requireContext(), "Erro: Dados essenciais para a compra estão faltando. Tente novamente.", Toast.LENGTH_SHORT).show()
                Log.e("ConfirmarCompra", "Dados faltantes. Peça: ${currentPeca == null}, User: ${currentUserId.isNullOrEmpty()}, Endereço: ${enderecoCompletoStr.isEmpty()}")
                return@setOnClickListener
            }

            // 3. Iniciar a transação
            processarCompra(pecaUid, selectedPaymentId)
        }
    }


    /**
     * Executa a sequência de operações: 1. Cria Avaliação, 2. Atualiza Peça, 3. Cria Transação.
     */
    private fun processarCompra(pecaUid: String, selectedPaymentId: Int) {
        val precoTotal = currentPeca?.preco ?: "0.00"
        val vendedorUid = currentPeca?.ownerUid!!
        val compradorUid = currentUserId!!

        // Obter Forma de Pagamento
        val formaPagamento = when (selectedPaymentId) {
            R.id.rbCartaoCredito -> "Cartão de Crédito"
            R.id.rbPix -> "PIX"
            R.id.rbCartaoDebito -> "Cartão de Débito"
            else -> "Pagamento não selecionado"
        }

        // Assumindo forma de envio simples, pois não há seleção na UI fornecida
        val formaEnvio = "Correios"

        // 1. 🚀 CRIAR AVALIAÇÃO PENDENTE
        val avaliacaoRef = database.child("avaliacoes").push()
        val avaliacaoUid = avaliacaoRef.key!!

        // Cria os dados iniciais da avaliação (pendente)
        val avaliacaoData = mapOf(
            "avaliado" to false, // Começa como pendente
            "avaliadorUID" to compradorUid, // O comprador é quem fará a avaliação
            "avaliadoUID" to vendedorUid, // O vendedor é quem será avaliado
            "dataHoraCriacao" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            "rating" to 0.0,
            "description" to ""
        )

        avaliacaoRef.setValue(avaliacaoData)
            .addOnSuccessListener {
                // 2. 🔄 ATUALIZAR PEÇA
                atualizarPeca(pecaUid, compradorUid) { sucessoPeca ->
                    if (sucessoPeca) {
                        // 3. 📝 CRIAR TRANSAÇÃO
                        criarTransacaoCompra(
                            vendedorUid,
                            compradorUid,
                            pecaUid,
                            precoTotal,
                            formaPagamento,
                            formaEnvio,
                            enderecoCompletoStr,
                            avaliacaoUid
                        )
                    } else {
                        Toast.makeText(requireContext(), "Erro ao atualizar status da peça.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ConfirmarCompra", "Falha ao criar avaliação: ${it.message}")
                Toast.makeText(requireContext(), "Erro na transação. Tente novamente.", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Atualiza o status da peça comprada no banco.
     */
    private fun atualizarPeca(pecaUid: String, novoOwnerUid: String, callback: (Boolean) -> Unit) {

        // 1. Buscar a gaveta 'Recebidos' do novo proprietário
        database.child("gavetas")
            .orderByChild("ownerUid")
            .equalTo(novoOwnerUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var gavetaUid: String? = null

                    // Itera sobre as gavetas encontradas (todas que pertencem ao novoOwnerUid)
                    for (childSnapshot in snapshot.children) {
                        val name = childSnapshot.child("name").getValue(String::class.java)
                        if (name == "Recebidos") {
                            // Encontrou a gaveta correta
                            gavetaUid = childSnapshot.key
                            break
                        }
                    }

                    if (gavetaUid.isNullOrEmpty()) {
                        Log.e("ConfirmarCompra", "Gaveta 'Recebidos' não encontrada para o usuário $novoOwnerUid.")
                        Toast.makeText(requireContext(), "Erro: Gaveta de destino não encontrada. A compra falhou.", Toast.LENGTH_LONG).show()
                        callback(false)
                        return
                    }

                    // 2. Se a gavetaUid foi encontrada, realiza o update da peça
                    val updatePeca = mapOf<String, Any>(
                        "ownerUid" to novoOwnerUid,
                        "finalidade" to "Organizar",
                        "gavetaUid" to gavetaUid
                    )

                    database.child("pecas").child(pecaUid).updateChildren(updatePeca)
                        .addOnSuccessListener {
                            Log.d("ConfirmarCompra", "Peça $pecaUid atualizada com sucesso para o novo dono e gaveta.")
                            callback(true)
                        }
                        .addOnFailureListener {
                            Log.e("ConfirmarCompra", "Erro ao atualizar peça $pecaUid: ${it.message}")
                            callback(false)
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ConfirmarCompra", "Erro ao buscar gaveta: ${error.message}")
                    callback(false)
                }
            })
    }

    /**
     * Cria o registro da transação de compra no banco de dados.
     */
    private fun criarTransacaoCompra(
        vendedorUid: String,
        compradorUid: String,
        pecaUid: String,
        precoTotal: String,
        formaPagamento: String,
        formaEnvio: String,
        enderecoDestino: String,
        avaliacaoUid: String
    ) {
        val transacaoDataHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val novaTransacao = TransacaoCompra(
            vendedorUID = vendedorUid,
            compradorUID = compradorUid,
            dataDaTransacao = transacaoDataHora,
            pecaUID = pecaUid,
            precoTotal = precoTotal,
            formaPagamento = formaPagamento,
            formaEnvio = formaEnvio,
            enderecoDestino = enderecoDestino,
            avaliacaoUID = avaliacaoUid
        )

        database.child("transacoes").child("compra").push().setValue(novaTransacao)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pedido confirmado e transação registrada!", Toast.LENGTH_LONG).show()

                // 4. ✅ SUCESSO FINAL: Retorna para a tela anterior
                setFragmentResult("requestKey", bundleOf("REALIZEI_COMPRA" to true, "PECA_UID_COMPRADA" to pecaUid))
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                Log.e("ConfirmarCompra", "Falha ao registrar transação: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao finalizar transação no banco. ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}