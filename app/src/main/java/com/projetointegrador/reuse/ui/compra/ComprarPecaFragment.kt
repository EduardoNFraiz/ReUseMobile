package com.projetointegrador.reuse.ui.compra

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.data.model.PecaCarrinho
import com.projetointegrador.reuse.databinding.FragmentComprarPecaBinding
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar

class ComprarPecaFragment : Fragment() {
    private var _binding: FragmentComprarPecaBinding? = null
    private val binding get() = _binding!!

    private val args: ComprarPecaFragmentArgs by navArgs()

    private lateinit var database: DatabaseReference
    private var currentPeca: PecaCadastro? = null

    // VARIÁVEIS DE ESTADO DO CARRINHO
    private var uidGavetaCarrinho: String? = null
    private var pecaNoCarrinhoUid: String? = null // UID da cópia da peça na gaveta Carrinho
    private val GAVETA_CARRINHO = "Carrinho"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComprarPecaBinding.inflate(inflater, container, false)
        database = Firebase.database.reference

        // 🟢 Correção 1: Desabilita o botão para evitar cliques antes da verificação assíncrona.
        binding.btnAdicionarCarrinho.isEnabled = false

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pecaUid = args.pecaUID

        // Carrega o estado do carrinho e define o texto/estado do botão
        loadCarrinhoState(pecaUid)

        initListeners(pecaUid)
        initToolbar(binding.toolbar)

        loadPecaData(pecaUid)

        setFragmentResultListener("requestKey") { key, bundle ->
            if (key == "requestKey") {
                mostrardialog()
            }
        }
    }

    // --- Lógica do Carrinho ---

    private fun loadCarrinhoState(pecaOriginalUid: String) {
        val ownerUid = Firebase.auth.currentUser?.uid
        if (ownerUid == null) {
            binding.btnAdicionarCarrinho.isEnabled = false
            return
        }

        // 1. Busca o UID da gaveta Carrinho do usuário logado
        fetchGavetaUidByName(GAVETA_CARRINHO, ownerUid) { uid ->
            uidGavetaCarrinho = uid
            if (uid == null) {
                // Falha na busca da Gaveta. Botão permanece desabilitado.
                Toast.makeText(requireContext(), "Erro: Gaveta 'Carrinho' não encontrada.", Toast.LENGTH_LONG).show()
                binding.btnAdicionarCarrinho.isEnabled = false
                return@fetchGavetaUidByName
            }

            // 2. Verifica se a cópia da peça original já está no carrinho
            checkIfPecaIsInCarrinho(pecaOriginalUid, uid)
        }
    }

    private fun checkIfPecaIsInCarrinho(pecaOriginalUid: String, carrinhoUid: String) {
        database.child("pecas")
            .orderByChild("pecaOriginalUid")
            .equalTo(pecaOriginalUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val pecaNoCarrinho = snapshot.children.firstOrNull {
                    it.child("gavetaUid").getValue(String::class.java) == carrinhoUid
                }
                pecaNoCarrinhoUid = pecaNoCarrinho?.key
                updateCarrinhoButton(pecaNoCarrinhoUid != null)

                // 🟢 Correção 2: Habilita o botão APÓS a verificação do estado.
                binding.btnAdicionarCarrinho.isEnabled = true
            }
            .addOnFailureListener {
                Log.e("Carrinho", "Erro ao verificar peça no carrinho: ${it.message}")
                // Em caso de falha de comunicação, o botão deve permanecer desabilitado ou ser tratado
                binding.btnAdicionarCarrinho.isEnabled = false
            }
    }

    private fun updateCarrinhoButton(isInCarrinho: Boolean) {
        // Lógica de alternância (toggle) do texto
        if (isInCarrinho) {
            binding.btnAdicionarCarrinho.text = getString(R.string.remover_do_carrinho)
        } else {
            binding.btnAdicionarCarrinho.text = getString(R.string.adicionar_ao_carrinho)
        }
    }

    private fun toggleCarrinho(pecaOriginalUid: String) {
        // 🟢 Correção 3: Desabilita o botão durante a operação para evitar cliques duplicados.
        binding.btnAdicionarCarrinho.isEnabled = false

        if (pecaNoCarrinhoUid != null) {
            // SE ESTIVER NO CARRINHO, REMOVE.
            removePecaFromCarrinho(pecaNoCarrinhoUid!!)
        } else {
            // SE NÃO ESTIVER NO CARRINHO, ADICIONA.
            savePecaToCarrinho(pecaOriginalUid)
        }
    }

    private fun savePecaToCarrinho(pecaOriginalUid: String) {
        val peca = currentPeca ?: return
        val carrinhoUid = uidGavetaCarrinho ?: return
        val compradorUid = Firebase.auth.currentUser?.uid ?: return

        val pecaCopia = PecaCarrinho(
            gavetaUid = carrinhoUid,
            ownerUid = compradorUid,
            pecaOriginalUid = pecaOriginalUid,
            titulo = peca.titulo,
            preco = peca.preco,
            fotoBase64 = peca.fotoBase64,
            cores = peca.cores,
            categoria = peca.categoria,
            tamanho = peca.tamanho,
            finalidade = peca.finalidade,
            detalhe = peca.detalhe
        )

        val pecaRef = database.child("pecas").push()
        val novaPecaUid = pecaRef.key

        if (novaPecaUid != null) {
            pecaRef.setValue(pecaCopia)
                .addOnSuccessListener {
                    database.child("gavetas").child(carrinhoUid).child("peças").child(novaPecaUid).setValue(true)
                        .addOnSuccessListener {
                            pecaNoCarrinhoUid = novaPecaUid
                            updateCarrinhoButton(true)
                            binding.btnAdicionarCarrinho.isEnabled = true // Re-habilita
                            Toast.makeText(requireContext(), "Adicionado ao carrinho com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Log.e("Carrinho", "Erro ao vincular gaveta: ${it.message}")
                            binding.btnAdicionarCarrinho.isEnabled = true // Re-habilita em caso de falha
                            Toast.makeText(requireContext(), "Erro ao adicionar ao carrinho.", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Log.e("Carrinho", "Erro ao salvar cópia: ${it.message}")
                    binding.btnAdicionarCarrinho.isEnabled = true // Re-habilita em caso de falha
                    Toast.makeText(requireContext(), "Erro ao salvar peça no banco.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun removePecaFromCarrinho(pecaUid: String) {
        val carrinhoUid = uidGavetaCarrinho ?: return

        // 1. Remove a cópia da gaveta Carrinho
        database.child("gavetas").child(carrinhoUid).child("peças").child(pecaUid).removeValue()
            .addOnSuccessListener {
                // 2. Remove a cópia do nó /pecas
                database.child("pecas").child(pecaUid).removeValue()
                    .addOnSuccessListener {
                        pecaNoCarrinhoUid = null
                        updateCarrinhoButton(false)
                        binding.btnAdicionarCarrinho.isEnabled = true // Re-habilita
                        Toast.makeText(requireContext(), "Removido do carrinho com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Log.e("Carrinho", "Erro ao remover peça do banco: ${it.message}")
                        binding.btnAdicionarCarrinho.isEnabled = true // Re-habilita em caso de falha
                        Toast.makeText(requireContext(), "Erro ao remover peça do banco.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Log.e("Carrinho", "Erro ao desvincular peça da gaveta: ${it.message}")
                binding.btnAdicionarCarrinho.isEnabled = true // Re-habilita em caso de falha
                Toast.makeText(requireContext(), "Erro ao remover do carrinho.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Outras Funções (Inalteradas) ---

    private fun loadPecaData(pecaUid: String) {
        database.child("pecas").child(pecaUid).get()
            .addOnSuccessListener { snapshot ->
                val peca = snapshot.getValue(PecaCadastro::class.java)
                if (peca != null) {
                    currentPeca = peca
                    updateUI(peca)
                } else {
                    Toast.makeText(requireContext(), "Peça não encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("ComprarPeca", "Erro ao buscar peça: ${it.message}")
                Toast.makeText(requireContext(), "Erro ao carregar dados da peça.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(peca: PecaCadastro) {
        binding.textView6.text = "R$${peca.preco ?: "0,00"}"
        binding.textView7.text = peca.titulo ?: "N/A"
        binding.textView8.text = peca.detalhe ?: "Sem descrição."

        peca.fotoBase64?.let { base64 ->
            displayBase64Image(base64, binding.imageView)
        } ?: run {
            binding.imageView.setImageResource(R.drawable.closeticon) // Placeholder
        }
    }

    private fun mostrardialog() {
        val dialog = DialogCompraFragment()
        dialog.show(parentFragmentManager,"compra concluida")
    }

    private fun fetchGavetaUidByName(
        gavetaName: String,
        ownerUid: String,
        onComplete: (String?) -> Unit
    ) {
        database.child("gavetas")
            .orderByChild("ownerUid")
            .equalTo(ownerUid)
            .get()
            .addOnSuccessListener { snapshot ->
                val gavetaEncontrada = snapshot.children.firstOrNull {
                    it.child("name").getValue(String::class.java) == gavetaName
                }
                onComplete(gavetaEncontrada?.key)
            }
            .addOnFailureListener {
                Log.e("Carrinho", "Erro ao buscar UID da gaveta '$gavetaName': ${it.message}")
                onComplete(null)
            }
    }

    private fun initListeners(pecaUid: String) {
        binding.btnComprar.setOnClickListener {
            val action = ComprarPecaFragmentDirections.actionComprarPecaFragmentToConfirmarCompraFragment(pecaUid)
            findNavController().navigate(action)
        }

        binding.btnAdicionarCarrinho.setOnClickListener {
            toggleCarrinho(pecaUid)
        }

        binding.btnVerMais.setOnClickListener {
            val ownerUid = currentPeca?.ownerUid
            if (ownerUid != null) {
                val action = ComprarPecaFragmentDirections.actionComprarPecaFragmentToAvaliacoesFragment(ownerUid)
                findNavController().navigate(action)
            } else {
                Toast.makeText(requireContext(), "Informação do vendedor indisponível.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}