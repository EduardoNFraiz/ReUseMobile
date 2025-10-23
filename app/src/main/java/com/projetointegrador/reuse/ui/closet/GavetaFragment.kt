package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Gaveta
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.databinding.FragmentGavetaBinding
import com.projetointegrador.reuse.ui.adapter.PecaClosetAdapter
import com.projetointegrador.reuse.util.initToolbar

class GavetaFragment : Fragment() {
    private var _binding: FragmentGavetaBinding? = null
    private val binding get() = _binding!!

    private lateinit var PecaClosetAdapter: PecaClosetAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGavetaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)
        initListeners()
        initRecyclerViewTask(getPecaCloset())
    }

    private fun initRecyclerViewTask(pecaclosetList: List<PecaCloset>){
        PecaClosetAdapter = PecaClosetAdapter(pecaclosetList)
        binding.recyclerViewPecaCloset.setHasFixedSize(true)
        binding. recyclerViewPecaCloset.adapter = PecaClosetAdapter
    }

    private fun getPecaCloset() = listOf(
        PecaCloset(R.drawable.closeticon, "Camisa de time GG", "R$81,00"),
        PecaCloset(R.drawable.closeticon, "Camisa de time GG", "R$81,00"),
        PecaCloset(R.drawable.closeticon, "Camisa de time GG", "R$81,00"),
        PecaCloset(R.drawable.closeticon, "Camisa de time GG", "R$81,00"),
        PecaCloset(R.drawable.closeticon, "Camisa de time GG", "R$81,00"),
        PecaCloset(R.drawable.closeticon, "Camisa de time GG", "R$81,00"),
        )
    private fun initListeners() {
        binding.buttonCadastrarRoupa.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("CRIANDO_ROUPA", true)
            }
            findNavController().navigate(R.id.action_gavetaFragment_to_cadRoupaFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
