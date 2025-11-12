package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentAVendaBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter



class AVendaFragment : Fragment() {
    private var _binding: FragmentAVendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var pecaAdapter: PecaAdapter
    private val database = Firebase.database.reference
    private var targetUserUID: String? = null
    private var parentType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAVendaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserUID = arguments?.getString("TARGET_USER_UID")
        parentType = arguments?.getString("PARENT_TYPE") ?: "BRECHO"


        initRecyclerView()
        targetUserUID?.let { loadAVendaPecas(it) }
        initListeners()
    }

    private fun initRecyclerView(){
        // 🛑 INICIALIZAÇÃO AJUSTADA COM O LISTENER DE CLIQUE
        pecaAdapter = PecaAdapter(mutableListOf()) { pecaUid ->

            // 🛑 Variável para a ação de navegação (tipo NavDirections)
            val action: NavDirections

            // 🛑 LÓGICA DE NAVEGAÇÃO CONDICIONAL
            if (parentType == "BRECHO") {
                // Navegação para o fragmento pai de Brechó
                action = VisualizarPBrechoFragmentDirections.actionVisualizarPBrechoFragmentToComprarPecaFragment(pecaUID = pecaUid)
            } else {
                // Navegação para o fragmento pai de Pessoa Física (Usuário Padrão)
                action = VisualizarPUsuarioFragmentDirections.actionVisualizarPUsuarioFragmentToComprarPecaFragment(pecaUID = pecaUid)
            }

            // 2. Navega usando o objeto action
            findNavController().navigate(action)
        }

        binding.recyclerViewTask.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = pecaAdapter
    }

    private fun loadAVendaPecas(userId: String) {
        database.child("pecas").orderByChild("ownerUid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pecaList = mutableListOf<Pair<PecaCadastro, String>>()
                    for (pecaSnapshot in snapshot.children) {
                        val peca = pecaSnapshot.getValue(PecaCadastro::class.java)
                        val pecaUid = pecaSnapshot.key

                        if (peca != null && pecaUid != null && peca.finalidade?.uppercase() == "VENDER") {
                            pecaList.add(Pair(peca, pecaUid))
                        }
                    }
                    pecaAdapter.updateList(pecaList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AVendaFragment", "Erro ao carregar peças à venda: ${error.message}")
                    Toast.makeText(requireContext(), "Erro ao carregar itens à venda.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun initListeners() {
        // ...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}