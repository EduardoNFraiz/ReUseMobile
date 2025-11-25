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
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.database
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentPesquisaVendasBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter
import com.projetointegrador.reuse.R

class PesquisaVendasFragment : Fragment() {
    private var _binding: FragmentPesquisaVendasBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    // O Adapter agora está ajustado para List<Pair<PecaCadastro, String>>
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

        // OBTÉM O UID DO USUÁRIO LOGADO
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        initRecyclerViewPecas()
        sharedViewModel.searchText.observe(viewLifecycleOwner) { newText ->
            performVendasSearch(newText)
        }
    }

    private fun initRecyclerViewPecas(){
        // Inicializa o adapter e passa a função de navegação (lambda)
        pecaAdapter = PecaAdapter(mutableListOf()) { pecaUid ->
            navigateToComprarPeca(pecaUid)
        }

        binding.recyclerViewTask.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = pecaAdapter
    }

    /**
     * Executa a navegação para o Fragmento de compra da peça.
     * Utiliza o NavController do Fragmento Pai para lidar com a hierarquia ViewPager.
     */
    private fun navigateToComprarPeca(pecaUid: String) {
        if (!isAdded) return

        try {
            // CRUCIAL: Utiliza PesquisaFragmentDirections, assumindo que a ação está
            // definida no Fragmento Pai (PesquisaFragment)
            val action = PesquisaFragmentDirections.actionPesquisaFragmentToComprarPecaFragment(pecaUid)

            // Usa findNavController() que resolverá o NavController do NavHost
            findNavController().navigate(action)

        } catch (e: Exception) {
            // Logs de diagnóstico
            Log.e("PesquisaVendas", "Erro na navegação para ComprarPeca. Verifique o NavGraph e o ID da ação.", e)
            Toast.makeText(requireContext(),
                getString(R.string.error_navegar_para_peca_verifique_navgraph), Toast.LENGTH_LONG).show()
        }
    }

    fun performVendasSearch(searchText: String) {

        val searchLower = searchText.lowercase()
        searchListener?.let { database.removeEventListener(it) }

        // 1. QUERY INICIAL OBRIGATÓRIA: Filtra pelo campo 'finalidade' = "Vender"
        val baseQuery: Query = database.child("pecas")
            .orderByChild("finalidade")
            .equalTo("Vender")

        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Usamos Pair<PecaCadastro, String> na lista temporária (Peca + UID)
                val fullVendasListWithUids = mutableListOf<Pair<PecaCadastro, String>>()

                for (pecaSnapshot in snapshot.children) {
                    val peca = pecaSnapshot.getValue(PecaCadastro::class.java)
                    val pecaUid = pecaSnapshot.key

                    if (peca != null && pecaUid != null) {

                        // FILTRO DE EXCLUSÃO DE PEÇA PRÓPRIA USANDO 'ownerUid'
                        if (peca.ownerUid == currentUserId) {
                            continue
                        }

                        fullVendasListWithUids.add(Pair(peca, pecaUid))
                    }
                }

                // 2. APLICAÇÃO DO FILTRO DE PESQUISA (LOCAL)
                val filteredList = if (searchLower.isNotEmpty() && searchLower.length >= 1) {
                    fullVendasListWithUids.filter { (peca, _) ->
                        // Filtra pelo campo 'titulo'
                        peca.titulo?.lowercase()?.contains(searchLower) == true
                    }.toMutableList()
                } else {
                    fullVendasListWithUids
                }

                // Exemplo de Toast de debug removido ou ajustado
                pecaAdapter.updateList(filteredList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),
                    getString(R.string.error_firebase_vendas, error.message), Toast.LENGTH_LONG).show()
            }
        }

        baseQuery.addListenerForSingleValueEvent(newListener)
        searchListener = newListener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchListener?.let { database.removeEventListener(it) }
        _binding = null
    }
}