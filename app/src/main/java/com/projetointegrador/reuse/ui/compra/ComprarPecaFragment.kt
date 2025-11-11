package com.projetointegrador.reuse.ui.compra

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs // 🛑 Import necessário para Safe Args
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro // Modelo da peça
import com.projetointegrador.reuse.databinding.FragmentComprarPecaBinding
import com.projetointegrador.reuse.util.displayBase64Image
import com.projetointegrador.reuse.util.initToolbar

class ComprarPecaFragment : Fragment() {
    private var _binding: FragmentComprarPecaBinding? = null
    private val binding get() = _binding!!

    // 🛑 1. OBTÉM OS ARGUMENTOS PASSADOS PELO SAFE ARGS
    private val args: ComprarPecaFragmentArgs by navArgs()

    private lateinit var database: DatabaseReference
    private var currentPeca: PecaCadastro? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComprarPecaBinding.inflate(inflater, container, false)
        database = Firebase.database.reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🛑 UID da peça é acessado diretamente
        val pecaUid = args.pecaUID

        initListeners(pecaUid) // Passa o UID para o listener
        initToolbar(binding.toolbar)

        // 🛑 CARREGA OS DADOS DA PEÇA
        loadPecaData(pecaUid)

        // Mantido o setFragmentResultListener, mas a lógica de navegação foi movida para initListeners
        setFragmentResultListener("requestKey") { key, bundle ->
            if (key == "requestKey") {
                mostrardialog()
            }
        }
    }

    // 🛑 FUNÇÃO PARA CARREGAR DADOS DO FIREBASE
    private fun loadPecaData(pecaUid: String) {
        database.child("pecas").child(pecaUid).get()
            .addOnSuccessListener { snapshot ->
                val peca = snapshot.getValue(PecaCadastro::class.java)
                if (peca != null) {
                    currentPeca = peca
                    updateUI(peca)
                } else {
                    Toast.makeText(requireContext(), "Peça não encontrada.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Log.e("ComprarPeca", "Erro ao buscar peça: ${it.message}")
                Toast.makeText(requireContext(), "Erro ao carregar dados da peça.", Toast.LENGTH_SHORT).show()
            }
    }

    // 🛑 FUNÇÃO PARA ATUALIZAR VIEWS COM OS DADOS DA PEÇA
    private fun updateUI(peca: PecaCadastro) {
        // Exemplo de atualização de views. Adapte conforme seu layout XML.
        binding.textView6.text = peca.titulo ?: "N/A"
        binding.textView7.text = "R$${peca.preco ?: "0,00"}"
        binding.textView8.text = peca.detalhe ?: "Sem descrição."

        peca.fotoBase64?.let { base64 ->
            displayBase64Image(base64, binding.imageView)
        } ?: run {
            binding.imageView.setImageResource(R.drawable.closeticon) // Placeholder
        }
    }

    private fun mostrardialog() {
        val dialog = DialogCompraFragment()
        dialog.show(parentFragmentManager,"compra concluida")
    }

    // 🛑 LISTENERS AJUSTADOS
    private fun initListeners(pecaUid: String) {
        binding.btnComprar.setOnClickListener {
            val action = ComprarPecaFragmentDirections.actionComprarPecaFragmentToConfirmarCompraFragment(pecaUid)
            findNavController().navigate(action)
        }


        binding.btnVerMais.setOnClickListener {
            val ownerUid = currentPeca?.ownerUid
            if (ownerUid != null) {
                val action = ComprarPecaFragmentDirections.actionComprarPecaFragmentToAvaliacoesFragment(ownerUid)
                findNavController().navigate(action)
            } else {
                Toast.makeText(requireContext(), "Informação do vendedor indisponível.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}