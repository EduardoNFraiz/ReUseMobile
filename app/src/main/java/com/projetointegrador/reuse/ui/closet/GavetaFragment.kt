package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
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
import com.projetointegrador.reuse.databinding.FragmentGavetaBinding
import com.projetointegrador.reuse.ui.adapter.PecaClosetAdapter
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet

class GavetaFragment : Fragment() {

    private var _binding: FragmentGavetaBinding? = null
    private val binding get() = _binding!!

    private var gavetaUID: String? = null

    private lateinit var reference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var pecaClosetAdapter: PecaClosetAdapter

    private val loadedPecasWithUids = mutableListOf<Pair<PecaCloset, String>>()


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

        if (gavetaUID.isNullOrEmpty()) {
            showBottomSheet(message = "Erro: ID da gaveta não foi encontrado.")
            findNavController().popBackStack()
        } else {
            loadGavetaAndRoupas(gavetaUID!!)
        }

        initToolbar(binding.toolbar)

        initListeners()
    }

    // --- LÓGICA DE CARREGAMENTO DE DADOS ---

    private fun loadGavetaAndRoupas(uid: String) {
        loadGavetaDetails(uid)
        loadRoupaUidsFromGaveta(uid)
    }

    private fun loadGavetaDetails(uid: String) {
        reference.child("gavetas").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gaveta = snapshot.getValue(Gaveta::class.java)
                    if (gaveta != null) {
                        binding.textViewGaveta.text = gaveta.name
                    } else {
                        showBottomSheet(message = "Detalhes da gaveta não encontrados.")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao carregar detalhes da gaveta: ${error.message}")
                }
            })
    }

    private fun loadRoupaUidsFromGaveta(gavetaUid: String) {
        reference.child("gavetas").child(gavetaUid).child("peças")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val roupaUids = mutableListOf<String>()
                    for (childSnapshot in snapshot.children) {
                        roupaUids.add(childSnapshot.key!!)
                    }
                    if (roupaUids.isNotEmpty()) {
                        fetchRoupaDetails(roupaUids)
                    } else {
                        showBottomSheet(message = "Esta gaveta não possui itens cadastrados.")
                        initRecyclerView(emptyList())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao listar UIDs das roupas: ${error.message}")
                }
            })
    }

    private fun fetchRoupaDetails(roupaUids: List<String>) {
        val totalRoupas = roupaUids.size
        var roupasCarregadas = 0

        loadedPecasWithUids.clear()

        for (uid in roupaUids) {
            reference.child("peças").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val peca = snapshot.getValue(PecaCloset::class.java)
                        if (peca != null) {
                            loadedPecasWithUids.add(Pair(peca, uid))
                        }

                        roupasCarregadas++

                        if (roupasCarregadas == totalRoupas) {
                            initRecyclerView(loadedPecasWithUids)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showBottomSheet(message = "Erro ao buscar detalhes da peça $uid: ${error.message}")
                        roupasCarregadas++
                        if (roupasCarregadas == totalRoupas) {
                            initRecyclerView(loadedPecasWithUids)
                        }
                    }
                })
        }
    }

    private fun initRecyclerView(pecaClosetList: List<Pair<PecaCloset, String>>){
        pecaClosetAdapter = PecaClosetAdapter(pecaClosetList) { clickedRoupaUID ->
            navigateToRoupaDetails(clickedRoupaUID)
        }
        binding.recyclerViewPecaCloset.setHasFixedSize(true)
        binding.recyclerViewPecaCloset.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewPecaCloset.adapter = pecaClosetAdapter
    }


    private fun navigateToRoupaDetails(roupaUID: String) {
        val bundle = Bundle().apply {
            // Ao clicar em uma peça, queremos VISUALIZAR (e potencialmente EDITAR).
            // Passamos a flag como false ou não a incluímos, garantindo o fluxo de visualização/edição
            // dependendo de como CadRoupaFragment lida com o ID da roupa.
            putString("ROUPA_ID", roupaUID)
            putBoolean("VISUALIZAR_INFO", true) // Assume que queremos visualizar
        }
        findNavController().navigate(R.id.action_gavetaFragment_to_cadRoupaFragment, bundle)
    }

    // --- FUNÇÕES DE DELEÇÃO DE GAVETA (CORRIGIDAS PARA EXCLUIR PEÇAS) ---

    private fun confirmAndDeleteGaveta() {
        val gavetaNome = binding.textViewGaveta.text.toString()

        showBottomSheet(
            titleDialog = R.string.text_tile_warning,
            titleButton = R.string.text_button_warning,

            // Mensagem de confirmação forte
            message = "ATENÇÃO: Tem certeza que deseja excluir a gaveta '$gavetaNome'? Esta ação é irreversível e todas as peças de roupa contidas nela serão PERMANENTEMENTE EXCLUÍDAS!",

            // Ação de clique: Inicia a exclusão
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

        // ETAPA 1: Buscar todas as UIDs das peças contidas na gaveta
        reference.child("gavetas").child(gavetaUid).child("peças")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pecaUidsToDelete = mutableListOf<String>()
                    for (childSnapshot in snapshot.children) {
                        pecaUidsToDelete.add(childSnapshot.key!!)
                    }

                    // ETAPA 2: Excluir os detalhes de TODAS as peças do nó principal /peças/
                    deletePecasDetails(gavetaUid, userId, pecaUidsToDelete)
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "ERRO: Falha ao listar peças para exclusão. ${error.message}")
                }
            })
    }

    // NOVO: Função para deletar em lote os detalhes das peças no nó /peças/
    private fun deletePecasDetails(gavetaUid: String, userId: String, pecaUids: List<String>) {
        if (pecaUids.isEmpty()) {
            // Não há peças, pule direto para excluir a gaveta
            deleteGavetaNodeAndReferences(gavetaUid, userId)
            return
        }

        val updates = mutableMapOf<String, Any?>()

        for (uid in pecaUids) {
            // Configura o caminho para deletar o detalhe da peça no nó principal /peças/{uid}
            updates["peças/$uid"] = null
        }

        // Executa a exclusão em lote dos detalhes das peças
        reference.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // ETAPA 3: Se os detalhes das peças foram excluídos, prossiga para excluir a gaveta e suas referências.
                deleteGavetaNodeAndReferences(gavetaUid, userId)
            } else {
                showBottomSheet(message = "ERRO: Falha ao excluir detalhes das peças no nó principal. ${task.exception?.message}")
            }
        }
    }

    // NOVO: Função para deletar o nó da gaveta e as referências do usuário
    private fun deleteGavetaNodeAndReferences(gavetaUid: String, userId: String) {
        // Remove o nó da gaveta (o que remove as referências aninhadas como /gavetas/{uid}/peças)
        reference.child("gavetas").child(gavetaUid).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Remove a referência da gaveta no nó do usuário
                removeGavetaReferenceFromUser(gavetaUid, userId)
            } else {
                showBottomSheet(message = "ERRO: Falha ao excluir o nó da gaveta. ${task.exception?.message}")
            }
        }
    }


    private fun removeGavetaReferenceFromUser(gavetaUid: String, userId: String) {
        // Tenta remover em 'pessoaFisica'
        reference.child("usuarios").child("pessoaFisica").child(userId).child("gavetas").child(gavetaUid).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onGavetaDeletionSuccess()
                    return@addOnCompleteListener
                }

                // Se falhar, tenta buscar nos subtipos de Pessoa Jurídica
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

        binding.buttonCadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putString("GAVETA_ID", gavetaUID)
                putBoolean("CRIANDO_ROUPA", true) // <--- CORREÇÃO AQUI
            }
            findNavController().navigate(R.id.action_gavetaFragment_to_cadRoupaFragment, bundle)
        }

        binding.trash1.setOnClickListener {
            confirmAndDeleteGaveta()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}