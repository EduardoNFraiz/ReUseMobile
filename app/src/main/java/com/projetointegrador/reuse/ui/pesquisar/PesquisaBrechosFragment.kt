package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth // 🛑 IMPORT NECESSÁRIO
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.data.model.TipoConta
import com.projetointegrador.reuse.databinding.FragmentPesquisaBrechosBinding
import com.projetointegrador.reuse.ui.adapter.TaskAdapter

class PesquisaBrechosFragment : Fragment() {
    private var _binding: FragmentPesquisaBrechosBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var taskAdapter: TaskAdapter
    private val taskList = mutableListOf<Task>()
    private var searchListener: ValueEventListener? = null

    private val sharedViewModel: SharedSearchViewModel by activityViewModels()
    private var currentUserId: String? = null // 🛑 UID do usuário logado

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesquisaBrechosBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🛑 OBTÉM O UID DO USUÁRIO LOGADO
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        initRecyclerViewTask()
        sharedViewModel.searchText.observe(viewLifecycleOwner) { newText ->
            performSearch(newText)
        }
    }

    private fun initRecyclerViewTask(){
        // 🛑 AJUSTE 1: Instancia o TaskAdapter com o listener de clique
        taskAdapter = TaskAdapter(taskList) { clickedUserUid ->
            navigateToVisualizarBrecho(clickedUserUid)
        }
        binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = taskAdapter
    }

    private fun navigateToVisualizarBrecho(userUid: String) {
        if (!isAdded) return

        try {
            // 🛑 MUDANÇA ESSENCIAL: CHAME A CLASSE DIRECTIONS DO FRAGMENTO PAI
            // Use PesquisaFragmentDirections se a ação estiver dentro de PesquisaFragment.
            val action = PesquisaFragmentDirections.actionPesquisaFragmentToVisualizarPBrechoFragment(userUid)

            // Use findNavController() que resolve para o NavHost principal
            findNavController().navigate(action)

        } catch (e: Exception) {
            // Mantenha o log para diagnosticar se houver falha (ex: nome da ação incorreto)
            Log.e("PesquisaUsuarios", "Erro na navegação: ${e.message}", e)
            Toast.makeText(requireContext(), "Erro ao navegar para o perfil. Verifique o NavGraph.", Toast.LENGTH_SHORT).show()
        }
    }

    fun performSearch(searchText: String) {

        val searchLower = searchText.lowercase()
        searchListener?.let { database.removeEventListener(it) }

        // 🛑 CAMINHO CORRIGIDO: usuarios/pessoaJuridica/brechos
        val baseQuery = database
            .child("usuarios")
            .child("pessoaJuridica")
            .child("brechos")

        val query: Query
        val searchField = "nomeDeUsuario"

        if (searchLower.isNotEmpty() && searchLower.length >= 1) {
            query = baseQuery
                .orderByChild(searchField)
                .startAt(searchLower)
                .endAt(searchLower + "\uf8ff")
                .limitToFirst(50)
        } else {
            query = baseQuery.limitToFirst(50)
        }

        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val newTaskList = mutableListOf<Task>()

                for (brechoSnapshot in snapshot.children) {
                    val brechoUID = brechoSnapshot.key // UID da Pessoa Jurídica

                    // 🛑 FILTRO DE EXCLUSÃO DO PERFIL LOGADO
                    if (brechoUID == currentUserId) {
                        continue // Pula o nó do brechó logado
                    }

                    val map = brechoSnapshot.value as? Map<*, *>

                    if (map != null) {
                        val nomeFantasia = map["nomeFantasia"]?.toString()
                        val nomeDeUsuario = map["nomeDeUsuario"]?.toString()
                        val fotoBase64 = map["fotoBase64"]?.toString()

                        if (!nomeDeUsuario.isNullOrEmpty()) {
                            val taskItem = Task(
                                uid = brechoUID,
                                fotoBase64 = fotoBase64,
                                nomeCompleto = nomeFantasia ?: "Brechó",
                                nomeDeUsuario = nomeDeUsuario,
                                rating = 4.5f,
                                conta = TipoConta.BRECHO
                            )
                            newTaskList.add(taskItem)
                        }
                    }
                }

                taskAdapter.updateList(newTaskList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "ERRO FIREBASE BRECHÓS: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        query.addListenerForSingleValueEvent(newListener)
        searchListener = newListener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchListener?.let { database.removeEventListener(it) }
        _binding = null
    }
}