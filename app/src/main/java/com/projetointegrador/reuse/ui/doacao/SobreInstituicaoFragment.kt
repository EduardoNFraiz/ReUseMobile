package com.projetointegrador.reuse.ui.doacao

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs // 🛑 Import necessário para navArgs
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentSobreInstituicaoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.displayBase64Image // 🛑 Adicionar import do utilitário de imagem (assumindo que existe)

class SobreInstituicaoFragment : Fragment() {
    private var _binding: FragmentSobreInstituicaoBinding? = null
    private val binding get() = _binding!!

    // 🛑 Safe Args: Obtém os argumentos passados, incluindo o 'instituicaoUid'
    private val args: SobreInstituicaoFragmentArgs by navArgs()

    // 🛑 Firebase: Referência ao banco de dados
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSobreInstituicaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)

        // 🛑 NOVO: Inicia a busca e preenchimento dos dados
        loadInstituicaoData(args.instituicaoUID)

        initListeners()
        barraDeNavegacao()
    }

    /**
     * 🛑 Busca e preenche os dados da Instituição.
     * @param instituicaoUid O UID da instituição obtido via Safe Args.
     */
    private fun loadInstituicaoData(instituicaoUid: String) {

        // Caminho para os dados do perfil (Nome, CNPJ, Foto)
        val perfilPath = "usuarios/pessoaJuridica/instituicoes/$instituicaoUid"
        // Caminho para os dados do anúncio (Breve Descrição, Detalhes, Endereço Formatado)
        val anuncioPath = "anuncios/$instituicaoUid"

        // 1. Buscar Dados do Perfil e Anúncio
        database.child(perfilPath).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(requireContext(), "Instituição não encontrada.", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return
                }

                // Perfil (CNPJ, Nome, Foto)
                val nomeCompleto = snapshot.child("nomeCompleto").getValue(String::class.java)
                val cnpj = snapshot.child("cnpj").getValue(String::class.java)
                val fotoBase64 = snapshot.child("fotoBase64").getValue(String::class.java)

                // Preenche Nome e CNPJ
                binding.nomeProjeto.text = nomeCompleto
                binding.cnpj.text = cnpj

                // Preenche a imagem
                if (!fotoBase64.isNullOrEmpty()) {
                    displayBase64Image(fotoBase64, binding.logo)
                } else {
                    binding.logo.setImageResource(R.drawable.exemplo) // Imagem placeholder
                }

                // 2. Buscar dados do Anúncio (que contém a descrição e endereço formatado)
                database.child(anuncioPath).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(anuncioSnapshot: DataSnapshot) {
                        val breveDescricao = anuncioSnapshot.child("breveDescricao").getValue(String::class.java)
                        val detalhes = anuncioSnapshot.child("detalhes").getValue(String::class.java)
                        val endereco = anuncioSnapshot.child("endereco").getValue(String::class.java)

                        // Preenche Descrição, Detalhes e Endereço
                        binding.descricao.text = breveDescricao
                        binding.detalhes.text = detalhes
                        binding.endereco.text = endereco
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("SobreInstituicao", "Erro ao buscar anúncio: ${error.message}")
                        Toast.makeText(requireContext(), "Erro ao carregar detalhes do anúncio.", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SobreInstituicao", "Erro ao buscar perfil: ${error.message}")
                Toast.makeText(requireContext(), "Falha ao carregar dados da instituição.", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        })
    }

    private fun initListeners(){
        binding.btnDoacao.setOnClickListener {
            // Se 'realizarDoacaoFragment' precisar do UID, ele deve ser passado aqui.
            val action = SobreInstituicaoFragmentDirections.actionSobreInstituicaoFragmentToRealizarDoacaoFragment(args.instituicaoUID)
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