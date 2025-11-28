package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentPesquisaBinding
import com.projetointegrador.reuse.ui.adapter.ViewPagerAdapter

// 🛑 NOVOS IMPORTS
import androidx.fragment.app.activityViewModels
import com.projetointegrador.reuse.ui.closet.CriarGavetaFragmentDirections

class PesquisaFragment : Fragment() {
    private var _binding: FragmentPesquisaBinding? = null
    private val binding get() = _binding!!

    // 🛑 INICIALIZAÇÃO DO VIEWMODEL COMPARTILHADO
    // by activityViewModels() garante que todos os fragments obtenham a mesma instância
    private val sharedViewModel: SharedSearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesquisaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTabs()
        initListeners()
        barraDeNavegacao()
    }

    private fun initTabs() {
        // 🛑 Sugestão: Tente usar 'this' para o Fragment Manager do Fragment Pai
        val pageAdapter = ViewPagerAdapter(requireActivity())
        pageAdapter.addFragment(PesquisaUsuariosFragment(), R.string.aba_usuarios)
        pageAdapter.addFragment(PesquisaBrechosFragment(), R.string.aba_brechos)
        pageAdapter.addFragment(PesquisaVendasFragment(), R.string.aba_vendas)

        binding.viewPager.adapter = pageAdapter
        binding.viewPager.offscreenPageLimit = pageAdapter.itemCount

        TabLayoutMediator(binding.tabs, binding.viewPager){tab, position ->
            tab.text = getString(pageAdapter.getTitle(position))
        }.attach()
    }


    private fun initListeners() {
        binding.editTextProcurar.doAfterTextChanged { editable ->
            val searchText = editable.toString().trim()

            // 🛑 ÚNICA AÇÃO NECESSÁRIA: Enviar o texto para o ViewModel.
            // O Fragment filho que está observando será notificado e fará a pesquisa.
            sharedViewModel.updateSearchText(searchText)

            // A lógica de encontrar currentFragment, is PesquisaUsuariosFragment, etc.,
            // foi totalmente removida, pois não é mais necessária.
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
}