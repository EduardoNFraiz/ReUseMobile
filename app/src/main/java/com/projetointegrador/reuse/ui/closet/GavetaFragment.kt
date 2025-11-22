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

    private lateinit var pecaAdapter: RecyclerView.Adapter<*>

    private val loadedPecasCloset = mutableListOf<Pair<PecaCloset, String>>()
    private val loadedPecasCarrinho = mutableListOf<Pair<PecaCarrinho, String>>()

    // Constantes para gavetas especiais
    private val GAVETA_CARRINHO = "Carrinho"
    private val GAVETA_RECEBIDOS = "Recebidos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // A chave "GAVETA_ID" é usada no retorno do CadRoupaFragment (handleBackNavigation)
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
        // 🛑 Importante: Chame initListeners antes de barraDeNavegacao se houver sobreposição
        initListeners()
        barraDeNavegacao()

        if (gavetaUID.isNullOrEmpty()) {
            showBottomSheet(message = "Erro: ID da gaveta não foi encontrado.")
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

        if (::pecaAdapter.isInitialized) {
            pecaAdapter.notifyDataSetChanged()
        }

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

                        // O adaptador de carrinho só é usado se o nome for Carrinho
                        initRecyclerView(gavetaNome == GAVETA_CARRINHO)

                        loadRoupaUidsFromGaveta(uid)
                    } else {
                        showBottomSheet(message = "Detalhes da gaveta não encontrados.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao carregar detalhes da gaveta: ${error.message}")
                }
            })
    }

    // Função para controlar a visibilidade de botões de edição/exclusão
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

                        // Lógica para usar o modelo correto (PecaCarrinho ou PecaCloset)
                        if (isCarrinho) {
                            val pecaCarrinho = pecaSnapshot.getValue(PecaCarrinho::class.java)
                            if (pecaCarrinho != null) {
                                loadedPecasCarrinho.add(Pair(pecaCarrinho, uid))
                            }
                        } else {
                            // Usado para Gaveta Normal e Gaveta Recebidos
                            val pecaCloset = pecaSnapshot.getValue(PecaCloset::class.java)
                            if (pecaCloset != null) {
                                loadedPecasCloset.add(Pair(pecaCloset, uid))
                            }
                        }
                    }

                    // Atualiza a lista dependendo do tipo de gaveta
                    if (isCarrinho) {
                        (pecaAdapter as? PecaCarrinhoAdapter)?.updateList(loadedPecasCarrinho)
                        if (loadedPecasCarrinho.isEmpty()) showBottomSheet(message = "Seu carrinho está vazio.")
                    } else {
                        (pecaAdapter as? PecaClosetAdapter)?.updateList(loadedPecasCloset)
                        val message = if (gavetaNome == GAVETA_RECEBIDOS) {
                            "Nenhuma peça em Recebidos."
                        } else {
                            "Esta gaveta não possui itens cadastrados."
                        }
                        if (loadedPecasCloset.isEmpty()) showBottomSheet(message = message)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao buscar peças por gaveta: ${error.message}")
                    (pecaAdapter as? PecaClosetAdapter)?.updateList(emptyList())
                }
            })
    }

    // --- SETUP DO RECYCLERVIEW ---

    private fun initRecyclerView(isCarrinho: Boolean) {

        // Verifica se o adaptador já foi inicializado com o tipo correto
        if (::pecaAdapter.isInitialized &&
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
            showBottomSheet(message = "Erro de contexto: ID da gaveta atual não encontrado.")
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
            titleDialog = R.string.atencao,
            titleButton = R.string.entendi,
            message = "ATENÇÃO: Tem certeza que deseja excluir a gaveta '$gavetaNome'? Esta ação é irreversível e todas as peças de roupa contidas nela serão PERMANENTEMENTE EXCLUÍDAS!",
            onClick = { deleteGaveta() }
        )
    }

    private fun deleteGaveta() {
        val gavetaUid = gavetaUID ?: return
        val userId = auth.currentUser?.uid ?: run {
            showBottomSheet(message = "Usuário não autenticado. Impossível deletar.")
            return
        }

        showBottomSheet(message = "Iniciando a exclusão da gaveta e suas peças...")

        reference.child("gavetas").child(gavetaUid).child("peças")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pecaUidsToDelete = mutableListOf<String>()
                    for (childSnapshot in snapshot.children) {
                        pecaUidsToDelete.add(childSnapshot.key!!)
                    }
                    deletePecasDetails(gavetaUid, userId, pecaUidsToDelete)
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "ERRO: Falha ao listar peças para exclusão. ${error.message}")
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
                showBottomSheet(message = "ERRO: Falha ao excluir peças ou nó da gaveta. ${task.exception?.message}")
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
                        showBottomSheet(message = "AVISO: A gaveta e peças foram excluídas, mas houve falha ao limpar o registro do seu usuário.")
                        onGavetaDeletionSuccess(skipNotification = true)
                    }
                }
        }
    }

    private fun onGavetaDeletionSuccess(skipNotification: Boolean = false) {
        if (!skipNotification) {
            showBottomSheet(message = "Gaveta e todas as peças contidas foram excluídas com sucesso!")
        }
        findNavController().popBackStack(R.id.closetFragment, false)
    }


    // --- LISTENERS ---

    private fun initListeners() {

        // 1. LISTENER DO BOTÃO PRINCIPAL DE CADASTRO (Abaixo do Título da Gaveta)
        binding.buttonCadastrarRoupa.setOnClickListener {

            val isSpecialGaveta = gavetaNome == GAVETA_CARRINHO || gavetaNome == GAVETA_RECEBIDOS

            if (isSpecialGaveta) {
                // A visibilidade já está GONE, mas este é um fallback de segurança
                Log.w("GavetaFragment", "Tentativa de cadastro em gaveta de sistema bloqueada: $gavetaNome")
                return@setOnClickListener
            }

            // 🟢 CORREÇÃO 1: Usa Safe Args para passar o gavetaUID e pecaUID=null (criação)
            val action = GavetaFragmentDirections.actionGavetaFragmentToCadRoupaFragment(
                pecaUID = null,
                gavetaUID = gavetaUID // 👈 Passa o UID da gaveta atual
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
            // 🟢 CORREÇÃO 2: Usa Safe Args e passa o gavetaUID também para a navegação da barra.
            // Isso garante que o CadRoupaFragment saiba para qual gaveta a peça será associada.
            val action = GavetaFragmentDirections.actionGavetaFragmentToCadRoupaFragment(
                pecaUID = null, // Nulo, pois é criação
                gavetaUID = gavetaUID // 👈 Passa o UID da gaveta atual
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