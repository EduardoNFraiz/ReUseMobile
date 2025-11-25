package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs // 🛑 Import necessário
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentVisualizarPUsuarioBinding
import com.projetointegrador.reuse.ui.adapter.ViewPagerAdapter
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar


class VisualizarPUsuarioFragment : Fragment() {
    private var _binding: FragmentVisualizarPUsuarioBinding? = null
    private val binding get() = _binding!!

    // 🛑 1. OBTÉM ARGUMENTOS
    private val args: VisualizarPUsuarioFragmentArgs by navArgs()

    private lateinit var database: DatabaseReference
    private var targetUserUID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisualizarPUsuarioBinding.inflate(inflater, container, false)
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
            loadPerfilData(uid)
            initTabs(uid)
        } ?: run {
            Toast.makeText(requireContext(),
                getString(R.string.error_id_usuario_nao_encontrado), Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    // 🛑 FUNÇÃO PARA CARREGAR DADOS DO PERFIL
    private fun loadPerfilData(userId: String) {
        // Assumindo que dados de nome, username e foto estão em usuarios/pessoaFisica/{userId}
        database.child("usuarios").child("pessoaFisica").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val nome = snapshot.child("nomeCompleto").getValue(String::class.java)
                val username = snapshot.child("nomeDeUsuario").getValue(String::class.java)
                val fotoBase64 = snapshot.child("fotoBase64").getValue(String::class.java)

                binding.textViewNome.text = nome ?: "Usuário Desconhecido"
                binding.textViewUsername.text = "@${username ?: "desconhecido"}"

                fotoBase64?.let { base64 ->
                    displayBase64Image(base64, binding.imagePerfil)
                } ?: binding.imagePerfil.setImageResource(R.drawable.person)

            }
            .addOnFailureListener {
                Log.e("VisualizarP", "Erro ao carregar dados do perfil: ${it.message}")
                Toast.makeText(requireContext(),
                    getString(R.string.error_carregar_perfil), Toast.LENGTH_SHORT).show()
            }
    }

    // 🛑 FUNÇÃO AJUSTADA PARA PASSAR O UID PARA AS ABAS FILHAS
    private fun initTabs(userId: String) {
        val pageAdapter = ViewPagerAdapter(requireActivity())

        // Cria um Bundle para passar o UID
        val bundle = Bundle().apply { putString("TARGET_USER_UID", userId)
            putString("PARENT_TYPE", "USUARIO")
        }

        // 1. Aba Closet (Peças que não são Venda/Doação/Carrinho)
        val closetFragment = UsuarioClosetFragment().apply { arguments = bundle }
        pageAdapter.addFragment(closetFragment, R.string.aba_closet_usuario)

        // 2. Aba A Venda (Somente peças marcadas para Venda)
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