package com.projetointegrador.reuse.ui.doacao

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Instituicao
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.data.model.TipoConta
import com.projetointegrador.reuse.databinding.FragmentDoacaoBinding
import com.projetointegrador.reuse.databinding.FragmentPesquisaUsuariosBinding
import com.projetointegrador.reuse.ui.adapter.InstituicaoAdapter
import com.projetointegrador.reuse.ui.adapter.TaskAdapter



class DoacaoFragment : Fragment() {
    private var _binding: FragmentDoacaoBinding? = null
    private val binding get() = _binding!!
    private lateinit var instituicaoAdapter: InstituicaoAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        barraDeNavegacao()
        initRecyclerViewTask(getTask())
        val doacaoRealizada = arguments?.getBoolean("REALIZEI_DOACAO") ?: false
        if (doacaoRealizada) { mostrardialog() }
    }

    private fun mostrardialog() {
        val dialog = DialogDoacaoFragment()
        dialog.show(parentFragmentManager,"doacao concluida")
    }

    private fun initListeners() {

    }

    private fun initRecyclerViewTask(instuicaoList: List<Instituicao>){
        instituicaoAdapter = InstituicaoAdapter(instuicaoList)
        binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTask.setHasFixedSize(true)
        binding. recyclerViewTask.adapter = instituicaoAdapter
    }

    private fun getTask() = listOf(
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.baseline_arrow_circle_right_24, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.baseline_arrow_circle_right_24, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.baseline_arrow_circle_right_24, "Eduardo Neumam", "1,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.person, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        Instituicao(R.drawable.baseline_arrow_circle_right_24, "Eduardo Neumam", "2,0km de distancia", TipoConta.INSTITUICAO),
        )

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