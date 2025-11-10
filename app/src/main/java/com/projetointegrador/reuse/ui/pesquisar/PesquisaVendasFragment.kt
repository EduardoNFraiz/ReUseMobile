package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth // Import necessário
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.PecaCadastro // 🛑 MODELO CORRIGIDO
import com.projetointegrador.reuse.databinding.FragmentPesquisaVendasBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter // Adaptação necessária
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Peca

// Nota: Seu PecaAdapter precisará aceitar List<Pair<PecaCadastro, String>>
// Se o PecaAdapter estiver usando Peca, você pode fazer o cast ou ajuste localmente.
// Assumindo que você ajustará o PecaAdapter para aceitar PecaCadastro.

class PesquisaVendasFragment : Fragment() {
    private var _binding: FragmentPesquisaVendasBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    // Adaptador deve ser compatível com PecaCadastro, ou você fará o cast no mapeamento
    private lateinit var pecaAdapter: PecaAdapter
    private var searchListener: ValueEventListener? = null

    private val sharedViewModel: SharedSearchViewModel by activityViewModels()
    private var currentUserId: String? = null // UID do usuário logado

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesquisaVendasBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🛑 OBTÉM O UID DO USUÁRIO LOGADO
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        initRecyclerViewPecas()
        sharedViewModel.searchText.observe(viewLifecycleOwner) { newText ->
            performVendasSearch(newText)
        }
    }

    private fun initRecyclerViewPecas(){
        // 🛑 Inicialização: O Adapter agora espera PecaCadastro
        pecaAdapter = PecaAdapter(emptyList()) { pecaUid ->
            Toast.makeText(requireContext(), "Clicou na peça: $pecaUid", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewTask.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = pecaAdapter
    }

    fun performVendasSearch(searchText: String) {

        val searchLower = searchText.lowercase()
        searchListener?.let { database.removeEventListener(it) }

        // 1. QUERY INICIAL OBRIGATÓRIA: Filtra pelo campo 'finalidade' = "Vender"
        val baseQuery: Query = database.child("pecas")
            .orderByChild("finalidade")
            .equalTo("Vender")

        searchListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Usamos Pair<PecaCadastro, String> na lista temporária
                val fullVendasListWithUids = mutableListOf<Pair<PecaCadastro, String>>()

                for (pecaSnapshot in snapshot.children) {
                    // 🛑 Mapeando para PecaCadastro
                    val peca = pecaSnapshot.getValue(PecaCadastro::class.java)
                    val pecaUid = pecaSnapshot.key

                    if (peca != null && pecaUid != null) {

                        // 🛑 FILTRO DE EXCLUSÃO DE PEÇA PRÓPRIA USANDO 'ownerUid'
                        if (peca.ownerUid == currentUserId) {
                            continue // Pula a peça se for do usuário logado
                        }

                        fullVendasListWithUids.add(Pair(peca, pecaUid))
                    }
                }

                // 2. APLICAÇÃO DO FILTRO DE PESQUISA (LOCAL): Filtra o resultado já excluído
                val filteredList = if (searchLower.isNotEmpty() && searchLower.length >= 1) {
                    fullVendasListWithUids.filter { (peca, _) ->
                        // Filtra pelo campo 'titulo'
                        peca.titulo?.lowercase()?.contains(searchLower) == true
                    }.toMutableList()
                } else {
                    fullVendasListWithUids
                }

                Toast.makeText(requireContext(), "DEBUG VENDAS: ${filteredList.size} peças encontradas (Excluindo próprias).", Toast.LENGTH_LONG).show()

                // O PecaAdapter deve estar pronto para receber List<Pair<PecaCadastro, String>>
                // Se seu adapter usa Peca, você pode precisar mapear PecaCadastro para Peca antes de updateList.
                pecaAdapter.updateList(filteredList) // 🛑 ATENÇÃO AQUI: Cast forçado se o Adapter for PecaAdapter<Peca>
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "ERRO FIREBASE VENDAS: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        baseQuery.addListenerForSingleValueEvent(searchListener as ValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchListener?.let { database.removeEventListener(it) }
        _binding = null
    }
}