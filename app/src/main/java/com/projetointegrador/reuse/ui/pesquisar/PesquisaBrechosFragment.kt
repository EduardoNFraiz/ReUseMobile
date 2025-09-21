package com.projetointegrador.reuse.ui.pesquisar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.Task
import com.projetointegrador.reuse.data.model.TipoConta
import com.projetointegrador.reuse.databinding.FragmentPesquisaBinding
import com.projetointegrador.reuse.databinding.FragmentPesquisaBrechosBinding
import com.projetointegrador.reuse.ui.adapter.TaskAdapter

class PesquisaBrechosFragment : Fragment() {
    private var _binding: FragmentPesquisaBrechosBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPesquisaBrechosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
        initRecyclerViewTask(getTask())
    }

    private fun initListeners() {

    }

    private fun initRecyclerViewTask(taskList: List<Task>){
        taskAdapter = TaskAdapter(taskList)
        binding.recyclerViewTask.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTask.setHasFixedSize(true)
        binding. recyclerViewTask.adapter = taskAdapter
    }

    private fun getTask() = listOf(
        Task(R.drawable.baseline_arrow_circle_right_24, "Eduardo Neumam", "@eduardo_neumam", 1.0F, TipoConta.BRECHO),
        Task(R.drawable.baseline_arrow_circle_right_24, "Eduardo", "@eduardo", 2.0F, TipoConta.BRECHO),
        Task(R.drawable.baseline_image_24, "Neumam", "@neumam", 3.0F, TipoConta.BRECHO),
        Task(R.drawable.baseline_image_24, "Eduardo", "@_neumam", 4.0F, TipoConta.BRECHO),
        Task(R.drawable.baseline_image_24, "Neumam", "@edu", 5.0F, TipoConta.BRECHO),
        Task(R.drawable.baseline_image_24, "Eduardo", "@eduardoneumam", 3.5F, TipoConta.BRECHO),

        )


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}