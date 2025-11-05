package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import android.util.Log
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

        // Inicializa Toolbar com a ação de voltar
        initToolbar(binding.toolbar)

        initListeners()
        // Removido: loadGavetaAndRoupas(gavetaUID!!) — carregamento vai para onResume
        if (gavetaUID.isNullOrEmpty()) {
            showBottomSheet(message = "Erro: ID da gaveta não foi encontrado.")
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("GavetaFragment", "onResume called")
        gavetaUID?.let { loadGavetaAndRoupas(it) }
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
        // Busca os UIDs no nó 'peças' (com ç) da gaveta
        reference.child("gavetas").child(gavetaUid).child("peças")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val roupaUids = mutableListOf<String>()
                    for (childSnapshot in snapshot.children) {
                        val isReferenced = childSnapshot.getValue(Boolean::class.java)
                        if (isReferenced == true) {
                            roupaUids.add(childSnapshot.key!!)
                        }
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
                    initRecyclerView(emptyList())
                }
            })
    }

    private fun fetchRoupaDetails(roupaUids: List<String>) {
        val totalRoupas = roupaUids.size
        var roupasCarregadas = 0

        loadedPecasWithUids.clear()

        for (uid in roupaUids) {
            // CORREÇÃO: Busca os detalhes no nó 'pecas' (sem ç)
            reference.child("pecas").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val peca = snapshot.getValue(PecaCloset::class.java)
                        if (peca != null) {
                            loadedPecasWithUids.add(Pair(peca, uid))
                        }

                        roupasCarregadas++

                        // Atualiza o RecyclerView somente após carregar tudo
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

    // --- SETUP DO RECYCLERVIEW E NAVEGAÇÃO DE DETALHES ---

    private fun initRecyclerView(pecaClosetList: List<Pair<PecaCloset, String>>) {
        if (!::pecaClosetAdapter.isInitialized) {
            pecaClosetAdapter = PecaClosetAdapter(pecaClosetList) { clickedRoupaUID ->
                navigateToRoupaDetails(clickedRoupaUID)
            }
            binding.recyclerViewPecaCloset.setHasFixedSize(true)
            binding.recyclerViewPecaCloset.layoutManager = GridLayoutManager(requireContext(), 2)
            binding.recyclerViewPecaCloset.adapter = pecaClosetAdapter
        } else {
            pecaClosetAdapter.updateList(pecaClosetList)
        }
    }


    private fun navigateToRoupaDetails(roupaUID: String) {
        // 1. Garante que o UID da gaveta esteja disponível
        val currentGavetaUID = gavetaUID ?: run {
            showBottomSheet(message = "Erro de contexto: ID da gaveta atual não encontrado.")
            return
        }

        val bundle = Bundle().apply {
            // ID da peça que será editada/visualizada
            putString("pecaUID", roupaUID)

            // 🌟 NOVO: UID da gaveta original (necessário para o CadRoupa2) 🌟
            putString("gavetaUid", currentGavetaUID)

            // O seu nav graph pode usar um argumento diferente como "ROUPA_ID",
            // mas estou padronizando para 'pecaUID' e 'gavetaUid' para consistência com o CadRoupa2

            // Se a ação leva ao CadRoupa1 (seu 'cadRoupaFragment'), o CadRoupa1 deve estar esperando esses argumentos.
        }

        // ATENÇÃO: Verifique se R.id.action_gavetaFragment_to_cadRoupaFragment é o CadRoupa1
        findNavController().navigate(R.id.action_gavetaFragment_to_cadRoupaFragment, bundle)
    }

    // --- FUNÇÕES DE DELEÇÃO DE GAVETA ---

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

        // Usa 'pecas' (sem ç) para exclusão em cascata
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

        binding.buttonCadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                // Passa o ID da gaveta para que a peça já seja associada a ela
                putString("GAVETA_ID", gavetaUID)
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.action_gavetaFragment_to_cadRoupaFragment, bundle)
        }

        binding.trash1.setOnClickListener {
            confirmAndDeleteGaveta()
        }

        binding.closet.setOnClickListener { findNavController().navigate(R.id.closetFragment) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("CRIANDO_ROUPA", true) }
            findNavController().navigate(R.id.cadastrarRoupa, bundle)
        }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacaoFragment) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}