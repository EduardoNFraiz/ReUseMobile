package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Peca
import com.projetointegrador.reuse.databinding.FragmentPesquisaVendasBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter

class PesquisaVendasFragment : Fragment() {
    private var _binding: FragmentPesquisaVendasBinding? = null
    private val binding get() = _binding!!
    private lateinit var pecaAdapter: PecaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesquisaVendasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initRecyclerViewTask(getTask())
    }

    private fun initListeners() {

    }

    private fun initRecyclerViewTask(pecaList: List<Peca>){
        pecaAdapter = PecaAdapter(pecaList)
        binding.recyclerViewTask.setHasFixedSize(true)
        binding. recyclerViewTask.adapter = pecaAdapter
    }

    private fun getTask() = listOf(
        Peca(R.drawable.person, "Eduardo Neumam"),
        Peca(R.drawable.baseline_arrow_circle_right_24, "Eduardo", "R$60,00"),
        Peca(R.drawable.baseline_image_24, "Neumam", "@neumam"),
        Peca(R.drawable.baseline_image_24, "Eduardo", "@_neumam"),
        Peca(R.drawable.baseline_image_24, "Neumam", "@edu"),
        Peca(R.drawable.baseline_image_24, "Eduardo", "@eduardoneumam"),
        Peca(R.drawable.person, "Eduardo Neumam"),
        Peca(R.drawable.baseline_arrow_circle_right_24, "Eduardo", "R$60,00"),
        Peca(R.drawable.baseline_image_24, "Neumam", "@neumam"),
        Peca(R.drawable.baseline_image_24, "Eduardo", "@_neumam"),
        Peca(R.drawable.baseline_image_24, "Neumam", "@edu"),
        Peca(R.drawable.baseline_image_24, "Eduardo", "@eduardoneumam"),
    )



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}