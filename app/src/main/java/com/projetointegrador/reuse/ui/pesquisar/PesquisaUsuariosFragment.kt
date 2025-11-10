package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth // 🛑 IMPORT NECESSÁRIO
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.ContaPessoaFisica
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.data.model.TipoConta
import com.projetointegrador.reuse.databinding.FragmentPesquisaUsuariosBinding
import com.projetointegrador.reuse.ui.adapter.TaskAdapter
import androidx.fragment.app.activityViewModels

class PesquisaUsuariosFragment : Fragment() {
    private var _binding: FragmentPesquisaUsuariosBinding? = null
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
        _binding = FragmentPesquisaUsuariosBinding.inflate(inflater, container, false)
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
        taskAdapter = TaskAdapter(taskList)
        binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = taskAdapter
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

                val newTaskList = mutableListOf<Task>()

                for (userSnapshot in snapshot.children) {
                    val userUID = userSnapshot.key // UID é a chave principal

                    // 🛑 FILTRO DE EXCLUSÃO DO PERFIL LOGADO
                    if (userUID == currentUserId) {
                        continue // Pula o nó do usuário logado
                    }

                    val map = userSnapshot.value as? Map<*, *>

                    if (map != null) {
                        val nomeCompleto = map["nomeCompleto"]?.toString()
                        val nomeDeUsuario = map["nomeDeUsuario"]?.toString()
                        val fotoBase64 = map["fotoBase64"]?.toString()

                        if (!nomeDeUsuario.isNullOrEmpty()) {
                            val conta = ContaPessoaFisica(
                                nomeCompleto = nomeCompleto ?: "",
                                nomeDeUsuario = nomeDeUsuario,
                                email = map["email"]?.toString() ?: "",
                                telefone = map["telefone"]?.toString() ?: "",
                                dataNascimento = map["dataNascimento"]?.toString() ?: "",
                                cpf = map["cpf"]?.toString() ?: "",
                                endereço = map["endereço"]?.toString() ?: "",
                                dataCadastro = map["dataCadastro"]?.toString() ?: "",
                                tipoPessoa = map["tipoPessoa"]?.toString() ?: "",
                                tipoUsuario = map["tipoUsuario"]?.toString() ?: "",
                                fotoBase64 = fotoBase64
                            )

                            val taskItem = Task(
                                fotoBase64 = conta.fotoBase64,
                                nomeCompleto = conta.nomeCompleto,
                                nomeDeUsuario = conta.nomeDeUsuario,
                                rating = 4.5f,
                                conta = TipoConta.USUARIO
                            )
                            newTaskList.add(taskItem)
                        }
                    }
                }

                taskAdapter.updateList(newTaskList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "ERRO FIREBASE USUÁRIOS: ${error.message}", Toast.LENGTH_LONG).show()
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