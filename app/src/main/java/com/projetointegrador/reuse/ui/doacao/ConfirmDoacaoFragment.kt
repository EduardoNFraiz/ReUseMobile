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
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentConfirmDoacaoBinding
import com.projetointegrador.reuse.databinding.CardviewPecaBinding
import com.projetointegrador.reuse.ui.closet.CriarGavetaFragmentDirections
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar


class ConfirmDoacaoFragment : Fragment() {
    private var _binding: FragmentConfirmDoacaoBinding? = null
    private val binding get() = _binding!!

    // 🛑 Safe Args: Obtém o UID da instituição e o Array de UIDs das peças
    private val args: ConfirmDoacaoFragmentArgs by navArgs()

    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    // 🛑 Usa o Adapter de Visualização interno
    private lateinit var pecaAdapter: PecaVisualizacaoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmDoacaoBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)

        val instituicaoUid = args.instituicaoUID
        val pecasUids = args.pecasUIDS.toList() // Array para List

        loadInstituicaoHeader(instituicaoUid)
        initRecyclerViewPecas()
        loadSelectedPecas(pecasUids)

        initListeners(pecasUids)
        barraDeNavegacao()
    }

    private fun initRecyclerViewPecas() {
        pecaAdapter = PecaVisualizacaoAdapter(emptyList())
        binding.recyclerViewTask.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTask.adapter = pecaAdapter
    }

    /**
     * Busca os dados da instituição e preenche o cabeçalho.
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
                Log.e("ConfirmDoacao", "Erro ao carregar cabeçalho da instituição: ${error.message}")
            }
        })
    }

    /**
     * Itera sobre a lista de UIDs selecionados e busca cada peça individualmente.
     */
    private fun loadSelectedPecas(pecasUids: List<String>) {
        val pecasList = mutableListOf<PecaCadastro>()
        val totalPecas = pecasUids.size
        var pecasCarregadas = 0

        if (totalPecas == 0) {
            Toast.makeText(requireContext(),
                getString(R.string.aviso_nenhuma_peca_selecionada_para_doacao), Toast.LENGTH_LONG).show()
            return
        }

        pecasUids.forEach { pecaUid ->
            database.child("pecas").child(pecaUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val peca = snapshot.getValue(PecaCadastro::class.java)
                    peca?.let { pecasList.add(it) }

                    pecasCarregadas++
                    if (pecasCarregadas == totalPecas) {
                        pecaAdapter.updateList(pecasList)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ConfirmDoacao", "Erro ao carregar peça $pecaUid: ${error.message}")
                    pecasCarregadas++
                    if (pecasCarregadas == totalPecas) {
                        pecaAdapter.updateList(pecasList)
                    }
                }
            })
        }
    }

    private fun initListeners(pecasUids: List<String>){
        binding.btnDoacao.setOnClickListener {
            navigateToEnvioDoacao(pecasUids, args.instituicaoUID)
        }
    }

    private fun navigateToEnvioDoacao(pecasUids: List<String>, instituicaoUid: String) {
        val pecasUidsArray = pecasUids.toTypedArray()
        try {
            val action = ConfirmDoacaoFragmentDirections.actionConfirmDoacaoFragmentToEnvioDoacaoFragment(
                instituicaoUID = instituicaoUid,
                pecasUIDS = pecasUidsArray
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            Log.e("Navegacao", "Erro ao navegar com Safe Args: ${e.message}")
            Toast.makeText(requireContext(),
                getString(R.string.error_navegacao_verifique_nav_graph), Toast.LENGTH_LONG).show()
        }
    }


    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener {
            val action = CriarGavetaFragmentDirections.actionGlobalCadRoupaFragment(
                pecaUID = null,
                gavetaUID = null
            )
            findNavController().navigate(action)
        }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class PecaVisualizacaoAdapter (
        private var pecas: List<PecaCadastro>
    ) : RecyclerView.Adapter<PecaVisualizacaoAdapter.PecaViewHolder> () {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PecaViewHolder {
            val view = CardviewPecaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PecaViewHolder(view)
        }

        override fun getItemCount() = pecas.size

        override fun onBindViewHolder(holder: PecaViewHolder, position: Int) {
            holder.bind(pecas[position])
        }

        inner class PecaViewHolder(val binding : CardviewPecaBinding): RecyclerView.ViewHolder(binding.root){

            fun bind(peca: PecaCadastro) {

                // --- Bind dos Dados ---
                if (!peca.fotoBase64.isNullOrEmpty()) {
                    displayBase64Image(peca.fotoBase64!!, binding.imagePeca)
                } else {
                    binding.imagePeca.setImageResource(R.drawable.closeticon)
                }

                binding.root.setOnClickListener(null)
            }
        }

        fun updateList(newList: List<PecaCadastro>) {
            this.pecas = newList
            notifyDataSetChanged()
        }
    }
}