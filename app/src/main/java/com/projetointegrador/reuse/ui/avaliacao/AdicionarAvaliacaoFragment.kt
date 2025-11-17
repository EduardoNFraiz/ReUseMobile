package com.projetointegrador.reuse.ui.avaliacao

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.AvaliacaoBanco // Assumindo este modelo
import com.projetointegrador.reuse.databinding.FragmentAdicionarAvaliacaoBinding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet

class AdicionarAvaliacaoFragment : Fragment() {

    private var _binding: FragmentAdicionarAvaliacaoBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private var avaliacaoId: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionarAvaliacaoBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Receber UIDs do Bundle
        avaliacaoId = arguments?.getString("AVALIACAO_ID")

        if (avaliacaoId.isNullOrEmpty()) {
            showBottomSheet(message = "Erro: ID da avaliação não encontrado.")
            findNavController().popBackStack() // Volta para a tela anterior
            return
        }
        initToolbar(binding.toolbar)
        initListeners()
    }

    private fun initListeners() {
        // Configura a ação de voltar
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Configura o clique do botão Avaliar
        binding.btnAvaliar.setOnClickListener {
            performAvaliacaoUpdate()
        }
    }

    private fun performAvaliacaoUpdate() {
        val ratingValue = binding.ratingBar.rating
        val descriptionText = binding.editTextDetalhes.text.toString().trim()

        if (descriptionText.isEmpty()) {
            showBottomSheet(message = "Por favor, adicione uma descrição da sua experiência.")
            return
        }

        // 2. Criar o mapa de atualização
        val updateMap = HashMap<String, Any>()
        updateMap["rating"] = ratingValue.toDouble() // Armazenar como Double/Float
        updateMap["description"] = descriptionText
        updateMap["avaliado"] = true // 🛑 Define como avaliado

        // 3. Executar a atualização no Firebase
        database.child("avaliacoes")
            .child(avaliacaoId!!)
            .updateChildren(updateMap)
            .addOnSuccessListener {
                showBottomSheet(message = "Avaliação adicionada com sucesso!")
                findNavController().popBackStack(R.id.historicoFragment, false) // Volta para o histórico
            }
            .addOnFailureListener { e ->
                Log.e("Avaliacao", "Falha ao atualizar a avaliação: ${e.message}")
                showBottomSheet(message = "Erro ao enviar avaliação: ${e.message}")
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}