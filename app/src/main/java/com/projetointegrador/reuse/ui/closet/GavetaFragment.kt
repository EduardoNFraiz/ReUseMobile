package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.data.model.PecaCarrinho
import com.projetointegrador.reuse.databinding.FragmentGavetaBinding
import com.projetointegrador.reuse.ui.adapter.PecaClosetAdapter
import com.projetointegrador.reuse.ui.adapter.PecaCarrinhoAdapter
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet

class GavetaFragment : Fragment() {

    private var _binding: FragmentGavetaBinding? = null
    private val binding get() = _binding!!

    private var gavetaUID: String? = null
    private var gavetaNome: String? = null

    private lateinit var reference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // pecaAdapter pode ser PecaClosetAdapter ou PecaCarrinhoAdapter
    private lateinit var pecaAdapter: RecyclerView.Adapter<*>

    private val loadedPecasCloset = mutableListOf<Pair<PecaCloset, String>>()
    private val loadedPecasCarrinho = mutableListOf<Pair<PecaCarrinho, String>>()

    // Constantes para gavetas especiais
    private val GAVETA_CARRINHO = "Carrinho"
    private val GAVETA_RECEBIDOS = "Recebidos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            gavetaUID = it.getString("GAVETA_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGavetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reference = Firebase.database.reference
        auth = Firebase.auth
        initToolbar(binding.toolbar)
        initListeners()
        barraDeNavegacao()

        // 🛑 AJUSTE 1: Inicialização padrão do adaptador no onViewCreated (tipo mais comum)
        // Isso garante que 'pecaAdapter' está inicializado antes de qualquer chamada assíncrona.
        initRecyclerView(isCarrinho = false)

        if (gavetaUID.isNullOrEmpty()) {
            showBottomSheet(message = getString(R.string.error_id_gaveta_nao_encontrado))
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("GavetaFragment", "onResume called - Forçando Recarregamento da Gaveta")
        gavetaUID?.let { loadGavetaAndRoupas(it) }
    }

    // --- LÓGICA DE CARREGAMENTO DE DADOS ---

    private fun loadGavetaAndRoupas(uid: String) {
        loadedPecasCloset.clear()
        loadedPecasCarrinho.clear()

        loadGavetaDetails(uid)
    }

    private fun loadGavetaDetails(uid: String) {
        reference.child("gavetas").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gaveta = snapshot.getValue(Gaveta::class.java)
                    if (gaveta != null) {
                        gavetaNome = gaveta.name
                        binding.textViewGaveta.text = gavetaNome

                        // Determina se é uma gaveta especial (Carrinho ou Recebidos)
                        val isSpecialGaveta = gavetaNome == GAVETA_CARRINHO || gavetaNome == GAVETA_RECEBIDOS
                        setupViewVisibility(isSpecialGaveta)

                        // 🛑 AJUSTE 2: Chama initRecyclerView para garantir que o tipo de adaptador está correto
                        initRecyclerView(gavetaNome == GAVETA_CARRINHO)

                        loadRoupaUidsFromGaveta(uid)
                    } else {
                        showBottomSheet(message = getString(R.string.error_detalhes_gaveta_nao_encontrados))
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(
                        R.string.error_carregar_detalhes_da_gaveta,
                        error.message
                    ))
                }
            })
    }

    private fun setupViewVisibility(isSpecialGaveta: Boolean) {
        if (isSpecialGaveta) {
            binding.buttonCadastrarRoupa.visibility = View.GONE
            binding.trash1.visibility = View.GONE
        } else {
            binding.buttonCadastrarRoupa.visibility = View.VISIBLE
            binding.trash1.visibility = View.VISIBLE
        }
    }

    private fun loadRoupaUidsFromGaveta(gavetaUid: String) {
        val isCarrinho = gavetaNome == GAVETA_CARRINHO

        reference.child("pecas")
            .orderByChild("gavetaUid")
            .equalTo(gavetaUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    for (pecaSnapshot in snapshot.children) {
                        val uid = pecaSnapshot.key!!

                        if (isCarrinho) {
                            val pecaCarrinho = pecaSnapshot.getValue(PecaCarrinho::class.java)
                            if (pecaCarrinho != null) {
                                loadedPecasCarrinho.add(Pair(pecaCarrinho, uid))
                            }
                        } else {
                            val pecaCloset = pecaSnapshot.getValue(PecaCloset::class.java)
                            if (pecaCloset != null) {
                                loadedPecasCloset.add(Pair(pecaCloset, uid))
                            }
                        }
                    }

                    // Atualiza a lista dependendo do tipo de gaveta
                    if (isCarrinho) {
                        (pecaAdapter as? PecaCarrinhoAdapter)?.updateList(loadedPecasCarrinho)
                        if (loadedPecasCarrinho.isEmpty()) showBottomSheet(message = getString(R.string.aviso_carrinho_vazio))
                    } else {
                        (pecaAdapter as? PecaClosetAdapter)?.updateList(loadedPecasCloset)
                        val message = if (gavetaNome == GAVETA_RECEBIDOS) {
                            getString(R.string.aviso_recebidos_vazio)
                        } else {
                            getString(R.string.aviso_gaveta_vazia)
                        }
                        if (loadedPecasCloset.isEmpty()) showBottomSheet(message = message)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(
                        R.string.error_buscar_pecas_por_gavetas,
                        error.message
                    ))
                    // Garante que a lista é limpa em caso de erro
                    (pecaAdapter as? PecaClosetAdapter)?.updateList(emptyList())
                }
            })
    }

    // --- SETUP DO RECYCLERVIEW ---

    private fun initRecyclerView(isCarrinho: Boolean) {

        val isAdapterInitialized = ::pecaAdapter.isInitialized

        // 🛑 AJUSTE 3: Só reinicializa se for necessário (não inicializado OU tipo errado)
        // Se o adaptador já estiver inicializado com o tipo CORRETO, retorna (melhora performance no onResume)
        if (isAdapterInitialized &&
            ((isCarrinho && pecaAdapter is PecaCarrinhoAdapter) || (!isCarrinho && pecaAdapter is PecaClosetAdapter))) {
            return
        }

        binding.recyclerViewPecaCloset.adapter = null

        if (isCarrinho) {
            // Inicializa com o Adapter de Carrinho (PecaCarrinho)
            pecaAdapter = PecaCarrinhoAdapter(emptyList())
        } else {
            // Inicializa com o Adapter de Closet Normal (PecaCloset)
            pecaAdapter = PecaClosetAdapter(emptyList()) { clickedRoupaUID ->
                navigateToRoupaDetails(clickedRoupaUID)
            }
        }

        binding.recyclerViewPecaCloset.setHasFixedSize(true)
        binding.recyclerViewPecaCloset.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewPecaCloset.adapter = pecaAdapter
    }


    private fun navigateToRoupaDetails(roupaUID: String) {
        val currentGavetaUID = gavetaUID ?: run {
            showBottomSheet(message = getString(R.string.error_id_gaveta_nao_encontrado))
            return
        }

        // Navega usando Safe Args
        val action = GavetaFragmentDirections.actionGavetaFragmentToCadRoupaFragment(
            pecaUID = roupaUID,
            gavetaUID = currentGavetaUID
        )

        findNavController().navigate(action)
    }

    // --- FUNÇÕES DE DELEÇÃO DE GAVETA (MANTIDAS) ---

    private fun confirmAndDeleteGaveta() {
        val gavetaNome = binding.textViewGaveta.text.toString()

        showBottomSheet(
            titleDialog = R.string.text_tile_warning,
            titleButton = R.string.text_button_warning,
            message = getString(R.string.showbottonsheet_msg_excluir_gaveta, gavetaNome),
            onClick = { deleteGaveta() }
        )
    }

    private fun deleteGaveta() {
        // 1. Verificações de segurança e estado
        val gavetaUid = gavetaUID ?: return
        val userId = auth.currentUser?.uid ?: run {
            showBottomSheet(message = getString(R.string.error_usuario_nao_logado))
            return
        }

        reference.child("pecas")
            .orderByChild("gavetaUid")
            .equalTo(gavetaUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pecaUidsToDelete = mutableListOf<String>()

                    // Itera sobre o resultado da busca filtrada
                    for (childSnapshot in snapshot.children) {
                        // Adiciona o UID da peça encontrada (a chave do nó)
                        pecaUidsToDelete.add(childSnapshot.key!!)
                    }

                    // Chama a função para exclusão dos detalhes e da referência da gaveta
                    deletePecasDetails(gavetaUid, userId, pecaUidsToDelete)
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = getString(
                        R.string.error_falha_listar_pecas_exclusao,
                        error.message
                    ))
                }
            })
    }

    private fun deletePecasDetails(gavetaUid: String, userId: String, pecaUids: List<String>) {
        val updates = mutableMapOf<String, Any?>()

        for (uid in pecaUids) {
            updates["pecas/$uid"] = null
        }
        updates["gavetas/$gavetaUid"] = null

        reference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                removeGavetaReferenceFromUser(gavetaUid, userId)
            } else {
                showBottomSheet(message = getString(
                    R.string.error_falha_excluir_pecas_no_gaveta,
                    task.exception?.message
                ))
            }
        }
    }


    private fun removeGavetaReferenceFromUser(gavetaUid: String, userId: String) {
        reference.child("usuarios").child("pessoaFisica").child(userId).child("gavetas").child(gavetaUid).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onGavetaDeletionSuccess()
                    return@addOnCompleteListener
                }
                searchAndRemoveJuridicaReference(gavetaUid, userId)
            }
    }

    private fun searchAndRemoveJuridicaReference(gavetaUid: String, userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var attempts = 0

        for (subtipo in subtipos) {
            reference.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId).child("gavetas").child(gavetaUid)
                .removeValue().addOnCompleteListener { task ->
                    attempts++
                    if (task.isSuccessful) {
                        onGavetaDeletionSuccess()
                        return@addOnCompleteListener
                    }

                    if (attempts == subtipos.size) {
                        showBottomSheet(message = getString(R.string.aviso_gaveta_pecas_excluidas_falha_limpar_registro_usuario))
                        onGavetaDeletionSuccess(skipNotification = true)
                    }
                }
        }
    }

    private fun onGavetaDeletionSuccess(skipNotification: Boolean = false) {
        if (!skipNotification) {
            showBottomSheet(message = getString(R.string.sucesso_exclusao_gaveta_com_pecas))
        }
        findNavController().popBackStack(R.id.closetFragment, false)
    }


    // --- LISTENERS ---

    private fun initListeners() {

        // 1. LISTENER DO BOTÃO PRINCIPAL DE CADASTRO (Abaixo do Título da Gaveta)
        binding.buttonCadastrarRoupa.setOnClickListener {

            val isSpecialGaveta = gavetaNome == GAVETA_CARRINHO || gavetaNome == GAVETA_RECEBIDOS

            if (isSpecialGaveta) {
                Log.w("GavetaFragment", "Tentativa de cadastro em gaveta de sistema bloqueada: $gavetaNome")
                return@setOnClickListener
            }

            val action = GavetaFragmentDirections.actionGavetaFragmentToCadRoupaFragment(
                pecaUID = null,
                gavetaUID = gavetaUID
            )
            findNavController().navigate(action)
        }

        binding.trash1.setOnClickListener {
            confirmAndDeleteGaveta()
        }
    }

    // 2. LISTENERS DA BARRA DE NAVEGAÇÃO
    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }

        binding.cadastrarRoupa.setOnClickListener {
            val action = GavetaFragmentDirections.actionGavetaFragmentToCadRoupaFragment(
                pecaUID = null,
                gavetaUID = gavetaUID
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