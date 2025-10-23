package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.databinding.FragmentClosetBinding
import com.projetointegrador.reuse.ui.adapter.GavetaAdapter

class ClosetFragment : Fragment() {
    private var _binding: FragmentClosetBinding? = null
    private val binding get() = _binding!!
    private lateinit var GavetaAdapter: GavetaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClosetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        barraDeNavegacao()
        initRecyclerViewTask(getGaveta())
    }

    private fun initRecyclerViewTask(gavetaList: List<Gaveta>){
        GavetaAdapter = GavetaAdapter(gavetaList)
        binding.recyclerViewGaveta.setHasFixedSize(true)
        binding. recyclerViewGaveta.adapter = GavetaAdapter
    }

    private fun getGaveta() = listOf(
        Gaveta(R.drawable.avatar_background, "Doação", "4"),
        Gaveta(R.drawable.avatar_background, "Carrinho", "3"),
        Gaveta(R.drawable.avatar_background, "Camisas", "5"),
        Gaveta(R.drawable.avatar_background, "Calças", "10"),
        Gaveta(R.drawable.avatar_background, "Tenis", "1"),
        Gaveta(R.drawable.avatar_background, "Meia", "2"),

        )

    private fun initListeners() {
        //binding.gaveta.setOnClickListener {
          //  findNavController().navigate(R.id.action_closetFragment_to_gavetaFragment)
        //}
        binding.bttHistorico.setOnClickListener {
            findNavController().navigate(R.id.action_closetFragment_to_historicoFragment)
        }

        binding.buttonTeste.setOnClickListener {
            findNavController().navigate(R.id.compra)
        }
        binding.buttonCriarGaveta.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("HIDE_EDIT_BUTTONS", true)
            }
            findNavController().navigate(R.id.action_closetFragment_to_criarGavetaFragment, bundle)
        }
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener {
            findNavController().navigate(R.id.closet)
        }
        binding.pesquisar.setOnClickListener {
            findNavController().navigate(R.id.pesquisar)
        }
        binding.cadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.cadastrarRoupa,bundle)
        }
        binding.doacao.setOnClickListener {
            findNavController().navigate(R.id.doacao)
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