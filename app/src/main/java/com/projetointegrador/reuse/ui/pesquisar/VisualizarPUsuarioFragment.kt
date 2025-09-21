package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.databinding.FragmentVisualizarPUsuarioBinding
import com.projetointegrador.reuse.ui.adapter.ViewPagerAdapter
import com.projetointegrador.reuse.util.initToolbar


class VisualizarPUsuarioFragment : Fragment() {
    private var _binding: FragmentVisualizarPUsuarioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisualizarPUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        barraDeNavegacao()
        initTabs()
        initToolbar(binding.toolbar)
    }
    private fun initTabs() {
        val pageAdapter = ViewPagerAdapter(requireActivity())
        binding.viewPager.adapter = pageAdapter
        pageAdapter.addFragment(UsuarioClosetFragment(),R.string.aba_closet_usuario)
        pageAdapter.addFragment(AVendaFragment(), R.string.aba_avenda)

        binding.viewPager.offscreenPageLimit = pageAdapter.itemCount

        TabLayoutMediator(binding.tabs, binding.viewPager){tab, position ->
            tab.text = getString(pageAdapter.getTitle(position))
        }.attach()
    }

    private fun initListeners() {

    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.pesquisar.setOnClickListener {
            findNavController().navigate(R.id.pesquisar)
        }
        binding.cadastrarRoupa.setOnClickListener {
            //findNavController().navigate(R.id.cadastrarpeca)
        }
        binding.doacao.setOnClickListener {

        }
        binding.perfil.setOnClickListener {
            findNavController().navigate(R.id.perfil)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}