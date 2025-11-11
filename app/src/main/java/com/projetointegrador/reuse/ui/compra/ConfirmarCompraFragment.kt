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
import com.projetointegrador.reuse.databinding.FragmentConfirmarCompraBinding
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar

class ConfirmarCompraFragment : Fragment() {
    private var _binding: FragmentConfirmarCompraBinding? = null
    private val binding get() = _binding!!

    private val args: ConfirmarCompraFragmentArgs by navArgs()

    private lateinit var database: DatabaseReference
    private var currentPeca: PecaCadastro? = null

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
        loadEnderecoData() // Inicia a busca multi-caminho
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
            // Validação de Pagamento
            if (binding.radioGroupPagamento.checkedRadioButtonId == -1) {
                Toast.makeText(requireContext(), "Por favor, selecione uma forma de pagamento.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simulação de retorno de compra
            val resultadoBundle = bundleOf(
                "REALIZEI_COMPRA" to true,
                "PECA_UID_COMPRADA" to pecaUid
            )

            setFragmentResult("requestKey", resultadoBundle)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}