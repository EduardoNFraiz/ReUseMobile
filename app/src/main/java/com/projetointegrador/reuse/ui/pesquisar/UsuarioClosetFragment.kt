package com.projetointegrador.reuse.ui.pesquisar

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.data.model.PecaCloset
import com.projetointegrador.reuse.databinding.FragmentUsuarioClosetBinding
import com.projetointegrador.reuse.ui.adapter.PecaClosetAdapter
import com.projetointegrador.reuse.util.displayBase64Image


class UsuarioClosetFragment : Fragment() {
    private var _binding: FragmentUsuarioClosetBinding? = null
    private val binding get() = _binding!!

    private lateinit var pecaAdapter: PecaClosetAdapter
    private val database = Firebase.database.reference
    private var targetUserUID: String? = null

    // 🛑 NOVO: Armazena a lista de peças mapeada para acesso pelo UID
    // Mapeia o UID da peça (String) para o objeto PecaCloset.
    private var pecasMap: Map<String, PecaCloset> = emptyMap()

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

        targetUserUID = arguments?.getString("TARGET_USER_UID")

        initRecyclerView()
        targetUserUID?.let { loadClosetPecas(it) }
        initListeners()
    }

    private fun initRecyclerView(){

        // 🛑 Implementação do onClick usando a lista armazenada (pecasMap)
        pecaAdapter = PecaClosetAdapter(
            pecas = mutableListOf(),
            onClick = { pecaUid ->
                // 1. Busca a peça no mapa pelo UID
                val peca = pecasMap[pecaUid]

                // 2. Se a peça existir e tiver fotoBase64, abre a visualização ampliada.
                if (peca != null && !peca.fotoBase64.isNullOrEmpty()) {
                    showAmplifiedImage(peca.fotoBase64!!)
                } else {
                    // Se não tiver foto, pode seguir para a tela de detalhes normal da peça
                    Toast.makeText(requireContext(), "Abrir detalhes de: $pecaUid", Toast.LENGTH_SHORT).show()
                    // Exemplo de navegação para detalhes:
                    // findNavController().navigate(R.id.detalheRoupaFragment, bundleOf("pecaUid" to pecaUid))
                }
            }
        )
        binding.recyclerViewTask.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewTask.setHasFixedSize(true)
        binding.recyclerViewTask.adapter = pecaAdapter
    }

    // 🛑 FUNÇÃO PARA MOSTRAR IMAGEM AMPLIADA EM DIALOG (mantida)
    private fun showAmplifiedImage(base64: String) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.BLACK)
            adjustViewBounds = true
        }

        displayBase64Image(base64, imageView)

        AlertDialog.Builder(requireContext())
            .setView(imageView)
            .setPositiveButton("Fechar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun loadClosetPecas(userId: String) {
        database.child("pecas").orderByChild("ownerUid").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pecaListForAdapter = mutableListOf<Pair<PecaCloset, String>>()
                    // 🛑 Inicializa o mapa para a nova lista
                    val tempPecasMap = mutableMapOf<String, PecaCloset>()

                    for (pecaSnapshot in snapshot.children) {
                        val pecaCadastro = pecaSnapshot.getValue(PecaCadastro::class.java)
                        val pecaUid = pecaSnapshot.key

                        if (pecaCadastro != null && pecaUid != null) {
                            val finalidade = pecaCadastro.finalidade?.uppercase()

                            if (finalidade != "VENDER" && finalidade != "DOAR" && finalidade != "CARRINHO") {

                                val pecaCloset = PecaCloset(
                                    titulo = pecaCadastro.titulo,
                                    preco = pecaCadastro.preco,
                                    fotoBase64 = pecaCadastro.fotoBase64
                                )
                                pecaListForAdapter.add(Pair(pecaCloset, pecaUid))

                                // 🛑 Adiciona a peça ao mapa para acesso rápido pelo UID
                                tempPecasMap[pecaUid] = pecaCloset
                            }
                        }
                    }

                    // 🛑 Atualiza a variável de classe com o novo mapa
                    pecasMap = tempPecasMap

                    // Atualiza o adapter com a lista (como antes)
                    pecaAdapter.updateList(pecaListForAdapter)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UsuarioCloset", "Erro ao carregar peças: ${error.message}")
                    Toast.makeText(requireContext(), "Erro ao carregar closet.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun initListeners() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}