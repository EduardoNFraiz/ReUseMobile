package com.projetointegrador.reuse.ui.doacao

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentRealizarDoacaoBinding
// 🛑 IMPORTAÇÃO CORRIGIDA
import com.projetointegrador.reuse.ui.adapter.PecaDoacaoAdapter
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar

// 🛑 Você deve garantir que o PecaCadastro reflete a estrutura dos seus dados de roupa.

class RealizarDoacaoFragment : Fragment() {
    private var _binding: FragmentRealizarDoacaoBinding? = null
    private val binding get() = _binding!!

    // 🛑 Safe Args: Obtém o UID da instituição
    private val args: RealizarDoacaoFragmentArgs by navArgs()

    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    // 🛑 TIPO DA VARIÁVEL CORRIGIDO
    private lateinit var pecaAdapter: PecaDoacaoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealizarDoacaoBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val instituicaoUid = args.instituicaoUID

        loadInstituicaoHeader(instituicaoUid)

        initRecyclerViewPecas()
        loadUserDonationClothes()
        initToolbar(binding.toolbar)
        initListeners()
        barraDeNavegacao()
    }

    private fun initRecyclerViewPecas() {
        // A ação de clique apenas atualiza o estado de seleção no adapter.
        val onItemClick: (String) -> Unit = { pecaUid ->
            Log.d("RealizarDoacao", "Peça selecionada/desselecionada: $pecaUid")
        }

        // 🛑 INSTÂNCIA CORRIGIDA
        pecaAdapter = PecaDoacaoAdapter(emptyList(), onItemClick)

        binding.recyclerViewTask.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTask.adapter = pecaAdapter
    }

    /**
     * Busca os dados da instituição no Firebase e preenche o cabeçalho.
     */
    private fun loadInstituicaoHeader(instituicaoUid: String) {
        val perfilPath = "usuarios/pessoaJuridica/instituicoes/$instituicaoUid"

        database.child(perfilPath).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nomeCompleto = snapshot.child("nomeCompleto").getValue(String::class.java)
                val nomeDeUsuario = snapshot.child("nomeDeUsuario").getValue(String::class.java)
                val fotoBase64 = snapshot.child("fotoBase64").getValue(String::class.java)

                binding.textViewNome.text = nomeCompleto ?: "Instituição"
                binding.textViewUsername.text = "@${nomeDeUsuario ?: "projeto"}"

                if (!fotoBase64.isNullOrEmpty()) {
                    displayBase64Image(fotoBase64, binding.imagePerfil)
                } else {
                    binding.imagePerfil.setImageResource(R.drawable.exemplo)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DoacaoFragment", "Erro ao carregar cabeçalho da instituição: ${error.message}")
            }
        })
    }

    /**
     * Busca todas as roupas do usuário logado que tem a finalidade "Doar".
     */
    private fun loadUserDonationClothes() {
        val currentUserId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuário não logado.", Toast.LENGTH_LONG).show()
            return
        }

        val pecasListWithUid = mutableListOf<Pair<PecaCadastro, String>>()

        // Assumindo que o caminho é "pecas" e o campo de ordenação é "ownerUid"
        database.child("pecas")
            .orderByChild("ownerUid")
            .equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (pecaSnapshot in snapshot.children) {
                        val finalidade = pecaSnapshot.child("finalidade").getValue(String::class.java)
                        val pecaUid = pecaSnapshot.key

                        if (finalidade == "Doar" && pecaUid != null) {
                            val peca = pecaSnapshot.getValue(PecaCadastro::class.java)
                            peca?.let { pecasListWithUid.add(Pair(it, pecaUid)) }
                        }
                    }

                    pecaAdapter.updateList(pecasListWithUid)

                    if (pecasListWithUid.isEmpty()) {
                        Toast.makeText(requireContext(), "Você não tem roupas cadastradas para doação.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DoacaoFragment", "Erro ao carregar roupas: ${error.message}")
                }
            })
    }

    private fun initListeners() {
        binding.btnDoacao.setOnClickListener {
            // 🛑 Obtém o Array<String> com todos os UIDs selecionados usando o método do PecaDoacaoAdapter
            val pecasSelecionadasUids = pecaAdapter.getSelectedPecaUids()

            if (pecasSelecionadasUids.isEmpty()) {
                Toast.makeText(requireContext(), "Selecione pelo menos uma peça para doar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cria a ação do Safe Args, passando o Array<String>
            val action = RealizarDoacaoFragmentDirections.actionRealizarDoacaoFragmentToConfirmDoacaoFragment(
                instituicaoUID = args.instituicaoUID,
                pecasUIDS = pecasSelecionadasUids
            )
            findNavController().navigate(action)
        }
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.cadastrarRoupa,bundle) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}