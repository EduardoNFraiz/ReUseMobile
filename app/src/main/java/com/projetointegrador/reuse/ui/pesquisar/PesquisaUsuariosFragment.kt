package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import com.projetointegrador.reuse.R
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.ContaPessoaFisica
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.data.model.TipoConta
import com.projetointegrador.reuse.databinding.FragmentPesquisaUsuariosBinding
import com.projetointegrador.reuse.ui.adapter.TaskAdapter
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

class PesquisaUsuariosFragment : Fragment() {
    private var _binding: FragmentPesquisaUsuariosBinding? = null
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
        _binding = FragmentPesquisaUsuariosBinding.inflate(inflater, container, false)
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
            navigateToVisualizarUsuario(clickedUserUid)
        }
        binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = taskAdapter
    }

    private fun navigateToVisualizarUsuario(userUid: String) {
        if (!isAdded) return

        try {
            // 🛑 MUDANÇA ESSENCIAL: CHAME A CLASSE DIRECTIONS DO FRAGMENTO PAI
            // Use PesquisaFragmentDirections se a ação estiver dentro de PesquisaFragment.
            val action = PesquisaFragmentDirections.actionPesquisaFragmentToVisualizarPUsuarioFragment(userUid)

            // Use findNavController() que resolve para o NavHost principal
            findNavController().navigate(action)

        } catch (e: Exception) {
            // Mantenha o log para diagnosticar se houver falha (ex: nome da ação incorreto)
            Log.e("PesquisaUsuarios", "Erro na navegação: ${e.message}", e)
            Toast.makeText(requireContext(), getString(R.string.error_ao_navegar_para_visualizar_usuario), Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserRating(userUid: String, callback: (Float) -> Unit) {
        database.child("avaliacoes")
            .orderByChild("avaliadoUID")
            .equalTo(userUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalRating = 0.0
                    var count = 0

                    for (avaliacaoSnapshot in snapshot.children) {
                        val avaliado = avaliacaoSnapshot.child("avaliado").getValue(Boolean::class.java)
                        val rating = avaliacaoSnapshot.child("rating").getValue(Double::class.java)

                        // 🛑 Aplica as duas condições: avaliado é true E o rating existe
                        if (avaliado == true && rating != null) {
                            totalRating += rating
                            count++
                        }
                    }

                    val averageRating = if (count > 0) (totalRating / count).toFloat() else 3.0f
                    callback(averageRating)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PesquisaUsuarios", "Erro ao buscar rating para $userUid: ${error.message}")
                    callback(0.0f)
                }
            })
    }

    fun performSearch(searchText: String) {

        val searchLower = searchText.lowercase()
        searchListener?.let { database.removeEventListener(it) }

        val baseQuery = database.child("usuarios").child("pessoaFisica")
        val query: Query

        if (searchLower.isNotEmpty() && searchLower.length >= 1) {
            query = baseQuery
                .orderByChild("nomeDeUsuario")
                .startAt(searchLower)
                .endAt(searchLower + "\uf8ff")
                .limitToFirst(50)
        } else {
            query = baseQuery
                .limitToFirst(50)
        }

        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val usersToProcess = snapshot.children.toList()
                if (usersToProcess.isEmpty()) {
                    taskAdapter.updateList(emptyList())
                    return
                }

                val newTaskList = mutableListOf<Task>()
                var pendingRatings = usersToProcess.size

                for (userSnapshot in usersToProcess) {
                    val userUID = userSnapshot.key ?: continue

                    if (userUID == currentUserId) {
                        pendingRatings--
                        continue
                    }

                    val conta = userSnapshot.getValue(ContaPessoaFisica::class.java)

                    if (conta != null && conta.nomeDeUsuario.isNotEmpty()) {

                        fetchUserRating(userUID) { averageRating ->

                            val taskItem = Task(
                                uid = userUID,
                                fotoBase64 = conta.fotoBase64,
                                nomeCompleto = conta.nomeCompleto,
                                nomeDeUsuario = conta.nomeDeUsuario,
                                rating = averageRating,
                                conta = TipoConta.USUARIO
                            )
                            newTaskList.add(taskItem)

                            pendingRatings--

                            if (pendingRatings == 0) {
                                newTaskList.sortByDescending { it.rating }
                                taskAdapter.updateList(newTaskList)
                            }
                        }
                    } else {
                        pendingRatings-- // Se a conta for nula ou nomeDeUsuario for vazio, decrementa o contador
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_firebase_usuarios, error.message), Toast.LENGTH_LONG).show()
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