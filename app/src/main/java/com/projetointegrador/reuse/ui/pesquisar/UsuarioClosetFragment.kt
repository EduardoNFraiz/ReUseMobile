package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Peca
import com.projetointegrador.reuse.databinding.FragmentUsuarioClosetBinding
import com.projetointegrador.reuse.ui.adapter.PecaAdapter


class UsuarioClosetFragment : Fragment() {
    private var _binding: FragmentUsuarioClosetBinding? = null
    private val binding get() = _binding!!
    private lateinit var pecaAdapter: PecaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsuarioClosetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        //initRecyclerViewTask(getTask())
    }

    private fun initListeners() {

    }

    //private fun initRecyclerViewTask(pecaList: List<Peca>){
        //pecaAdapter = PecaAdapter(pecaList)
        //binding.recyclerViewTask.setHasFixedSize(true)
        //binding. recyclerViewTask.adapter = pecaAdapter
    //}



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}