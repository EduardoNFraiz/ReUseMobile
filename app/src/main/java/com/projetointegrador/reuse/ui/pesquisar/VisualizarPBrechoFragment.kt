package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs // Import necessário
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentVisualizarPBrechoBinding
import com.projetointegrador.reuse.ui.adapter.ViewPagerAdapter
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar


class VisualizarPBrechoFragment : Fragment() {
    private var _binding: FragmentVisualizarPBrechoBinding? = null
    private val binding get() = _binding!!

    // 🛑 Assumindo que o argumento Safe Args é 'userUID'
    private val args: VisualizarPBrechoFragmentArgs by navArgs()

    private lateinit var database: DatabaseReference
    private var targetUserUID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisualizarPBrechoBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        targetUserUID = args.userUID

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        barraDeNavegacao()
        initToolbar(binding.toolbar)

        targetUserUID?.let { uid ->
            loadPerfilBrechoData(uid) // 🛑 Carrega os dados do Brechó
            initTabs(uid)             // 🛑 Inicializa as abas com o UID
        } ?: run {
            Toast.makeText(requireContext(), "UID do Brechó não encontrado.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    // 🛑 FUNÇÃO PARA CARREGAR DADOS DO PERFIL DO BRECHÓ
    private fun loadPerfilBrechoData(userId: String) {
        // Caminho do Brechó: usuarios/pessoaJuridica/brechos/{userId}
        database.child("usuarios").child("pessoaJuridica").child("brechos").child(userId).get()
            .addOnSuccessListener { snapshot ->
                // Adapte os nomes das chaves conforme seu Firebase (ex: nomeBrecho, username, foto)
                val nomeBrecho = snapshot.child("nomeCompleto").getValue(String::class.java)
                val username = snapshot.child("nomeDeUsuario").getValue(String::class.java)
                val fotoBase64 = snapshot.child("fotoBase64").getValue(String::class.java)

                // 🛑 ATUALIZA UI COM DADOS DO BRECHÓ
                binding.textViewNome.text = nomeBrecho ?: "Brechó Desconhecido"
                binding.textViewUsername.text = "@${username ?: "desconhecido"}"

                fotoBase64?.let { base64 ->
                    displayBase64Image(base64, binding.imagePerfil)
                } ?: binding.imagePerfil.setImageResource(R.drawable.person)

            }
            .addOnFailureListener {
                Log.e("VisualizarPBrecho", "Erro ao carregar dados do perfil do Brechó: ${it.message}")
                Toast.makeText(requireContext(), "Erro ao carregar perfil do Brechó.", Toast.LENGTH_SHORT).show()
            }
    }

    // 🛑 FUNÇÃO AJUSTADA PARA PASSAR O UID E O TIPO DE PARENT (se usar a Opção 2)
    private fun initTabs(userId: String) {
        val pageAdapter = ViewPagerAdapter(requireActivity())

        // Passa o UID e o flag de PARENT_TYPE="BRECHO"
        val bundle = Bundle().apply {
            putString("TARGET_USER_UID", userId)
            putString("PARENT_TYPE", "BRECHO") // Adicionado para Opção 2
        }

        // O Brechó só precisa da aba "À Venda"
        val aVendaFragment = AVendaFragment().apply { arguments = bundle }
        pageAdapter.addFragment(aVendaFragment, R.string.aba_avenda)

        binding.viewPager.adapter = pageAdapter
        binding.viewPager.offscreenPageLimit = pageAdapter.itemCount

        TabLayoutMediator(binding.tabs, binding.viewPager){tab, position ->
            tab.text = getString(pageAdapter.getTitle(position))
        }.attach()
    }

    private fun initListeners() {

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