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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
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
    private var currentUserId: String? = null

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

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        initRecyclerViewTask()
        sharedViewModel.searchText.observe(viewLifecycleOwner) { newText ->
            performSearch(newText)
        }
    }

    private fun initRecyclerViewTask(){
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
            // Assumindo que a ação está configurada no nav_graph do fragmento pai (PesquisaFragment)
            val action = PesquisaFragmentDirections.actionPesquisaFragmentToVisualizarPBrechoFragment(userUid)
            findNavController().navigate(action)

        } catch (e: Exception) {
            Log.e("PesquisaBrechos", "Erro na navegação: ${e.message}", e)
            Toast.makeText(requireContext(),
                getString(R.string.error_navegar_para_perfil_verifique_navgraph), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 🛑 NOVO: Busca e calcula o rating médio de um usuário (Brechó).
     */
    private fun fetchUserRating(userUid: String, callback: (Float) -> Unit) {
        database.child("avaliacoes")
            .orderByChild("avaliadoUid")
            .equalTo(userUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalRating = 0.0
                    var count = 0

                    for (avaliacaoSnapshot in snapshot.children) {
                        val avaliado = avaliacaoSnapshot.child("avaliado").getValue(Boolean::class.java)
                        val rating = avaliacaoSnapshot.child("rating").getValue(Double::class.java)

                        // Aplica as duas condições: avaliado é true E o rating existe
                        if (avaliado == true && rating != null) {
                            totalRating += rating
                            count++
                        }
                    }

                    // Retorna 0.0f se não houver avaliações para evitar divisão por zero
                    val averageRating = if (count > 0) (totalRating / count).toFloat() else 3.0f
                    callback(averageRating)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PesquisaBrechos", "Erro ao buscar rating para $userUid: ${error.message}")
                    callback(0.0f)
                }
            })
    }

    fun performSearch(searchText: String) {

        val searchLower = searchText.lowercase()
        searchListener?.let { database.removeEventListener(it) }

        // CAMINHO CORRIGIDO: usuarios/pessoaJuridica/brechos
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

                val brechosToProcess = snapshot.children.toList()
                if (brechosToProcess.isEmpty()) {
                    taskAdapter.updateList(emptyList())
                    return
                }

                val newTaskList = mutableListOf<Task>()
                var pendingRatings = brechosToProcess.size

                for (brechoSnapshot in brechosToProcess) {
                    val brechoUID = brechoSnapshot.key ?: continue

                    // FILTRO DE EXCLUSÃO DO PERFIL LOGADO
                    if (brechoUID == currentUserId) {
                        pendingRatings--
                        continue
                    }

                    val map = brechoSnapshot.value as? Map<*, *>

                    if (map != null) {
                        // 🛑 CHAMA A BUSCA DE RATING PARA CADA BRECHÓ
                        fetchUserRating(brechoUID) { averageRating ->
                            val nomeFantasia = map["nomeFantasia"]?.toString()
                            val nomeDeUsuario = map["nomeDeUsuario"]?.toString()
                            val fotoBase64 = map["fotoBase64"]?.toString()

                            if (!nomeDeUsuario.isNullOrEmpty()) {
                                val taskItem = Task(
                                    uid = brechoUID,
                                    fotoBase64 = fotoBase64,
                                    nomeCompleto = nomeFantasia ?: "Brechó",
                                    nomeDeUsuario = nomeDeUsuario,
                                    rating = averageRating, // 🛑 USANDO O RATING CALCULADO
                                    conta = TipoConta.BRECHO
                                )
                                newTaskList.add(taskItem)
                            }

                            pendingRatings--

                            // 🏁 VERIFICA SE TODAS AS TAREFAS ASSÍNCRONAS TERMINARAM
                            if (pendingRatings == 0) {
                                // Ordena a lista (opcional)
                                newTaskList.sortByDescending { it.rating }
                                taskAdapter.updateList(newTaskList)
                            }
                        }
                    } else {
                        pendingRatings--
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_firebase_brechos, error.message), Toast.LENGTH_LONG).show()
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