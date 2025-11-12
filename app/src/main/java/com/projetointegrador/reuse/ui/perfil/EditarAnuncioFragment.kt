package com.projetointegrador.reuse.ui.perfil

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.projetointegrador.reuse.R // 🛑 Import R (Assumindo que você tem recursos de strings/layouts)
import com.projetointegrador.reuse.databinding.FragmentEditarAnuncioBinding
import com.projetointegrador.reuse.util.initToolbar
import java.util.concurrent.Executors

class EditarAnuncioFragment : Fragment() {

    private var _binding: FragmentEditarAnuncioBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val currentUserUid = auth.currentUser?.uid

    private var isEditing: Boolean = false // 🛑 NOVO: Flag para o modo de edição
    private var enderecoFormatadoParaAnuncio: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarAnuncioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initToolbar(binding.toolbar)

        // 🛑 Inicializa em modo de visualização (Visualização = false)
        toggleEditMode(false)

        loadData()
        initListeners()
    }

    /**
     * Alterna entre os modos de Edição e Visualização.
     */
    private fun toggleEditMode(enable: Boolean) {
        isEditing = enable

        // Habilita/desabilita campos editáveis
        binding.etBreveDescricao.isEnabled = enable
        binding.etDetalhes.isEnabled = enable

        // Campos que permanecem desabilitados/visuais
        binding.etCnpj.isEnabled = false
        binding.etEnderecoCep.isEnabled = false

        // Controla a visibilidade dos botões
        // O botão Salvar só aparece no modo de edição
        binding.bttSalvarAnuncio.visibility = if (enable) View.VISIBLE else View.GONE

        // 🛑 O botão de edição muda para "Cancelar" no modo de edição
        binding.bttEditarAnuncio.text = if (enable) "Cancelar" else "Editar"

        // Se desativarmos a edição, recarregamos os dados originais (caso o usuário cancele)
        if (!enable && isEditing) {
            // Recarrega os dados do Firebase (opcional, mas seguro)
            loadData()
        }
    }


    private fun loadData() {
        if (currentUserUid == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // 1. Puxar CNPJ e UID do Endereço do perfil da instituição
        database.child("usuarios").child("pessoaJuridica").child("instituicoes").child(currentUserUid).get()
            .addOnSuccessListener { snapshot ->
                val cnpj = snapshot.child("cnpj").getValue(String::class.java)
                val enderecoUID = snapshot.child("endereço").getValue(String::class.java)

                binding.etCnpj.setText(cnpj)

                // 2. BUSCAR DADOS DO ANÚNCIO (anuncios/{uid})
                loadAnuncioData(currentUserUid)

                // 3. BUSCAR ENDEREÇO COMPLETO (Se o UID for encontrado)
                if (!enderecoUID.isNullOrEmpty()) {
                    loadEnderecoData(enderecoUID)
                } else {
                    binding.etEnderecoCep.hint = "Endereço não vinculado ao perfil."
                }
            }
            .addOnFailureListener {
                Log.e("EditarAnuncio", "Erro ao carregar dados da instituição: ${it.message}")
                Toast.makeText(requireContext(), "Erro ao carregar dados.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Busca os dados do anúncio e preenche os campos breveDescricao e detalhes.
     */
    private fun loadAnuncioData(anuncioUID: String) {
        database.child("anuncios").child(anuncioUID).get()
            .addOnSuccessListener { snapshot ->
                val breveDescricao = snapshot.child("breveDescricao").getValue(String::class.java)
                val detalhes = snapshot.child("detalhes").getValue(String::class.java)

                binding.etBreveDescricao.setText(breveDescricao)
                binding.etDetalhes.setText(detalhes)

                enderecoFormatadoParaAnuncio = snapshot.child("endereco").getValue(String::class.java)
            }
            .addOnFailureListener {
                Log.w("EditarAnuncio", "Nenhum anúncio existente encontrado.")
            }
    }

    /**
     * Usa o UID do endereço para buscar o objeto completo, formata e salva localmente.
     */
    private fun loadEnderecoData(enderecoUID: String) {
        database.child("enderecos").child(enderecoUID).get()
            .addOnSuccessListener { enderecoSnapshot ->
                val cep = enderecoSnapshot.child("cep").getValue(String::class.java) ?: ""
                val rua = enderecoSnapshot.child("rua").getValue(String::class.java) ?: ""
                val numero = enderecoSnapshot.child("numero").getValue(String::class.java) ?: "S/N"
                val cidade = enderecoSnapshot.child("cidade").getValue(String::class.java) ?: ""
                val estado = enderecoSnapshot.child("estado").getValue(String::class.java) ?: ""

                val enderecoFormatado = "CEP: $cep \n $rua n° $numero, $cidade - $estado"

                enderecoFormatadoParaAnuncio = enderecoFormatado

                binding.etEnderecoCep.setText(enderecoFormatado)
            }
            .addOnFailureListener {
                Log.e("EditarAnuncio", "Erro ao carregar endereço com UID $enderecoUID: ${it.message}")
                Toast.makeText(requireContext(), "Erro ao carregar dados de endereço.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initListeners() {
        // 🛑 NOVO LISTENER: Botão de Edição
        binding.bttEditarAnuncio.setOnClickListener {
            if (isEditing) {
                // Se está editando e clica, significa CANCELAR
                toggleEditMode(false)
            } else {
                // Se não está editando e clica, significa EDITAR
                toggleEditMode(true)
            }
        }

        binding.bttSalvarAnuncio.setOnClickListener {
            saveAnuncio()
        }
    }

    private fun saveAnuncio() {
        val breveDescricao = binding.etBreveDescricao.text.toString().trim()
        val detalhes = binding.etDetalhes.text.toString().trim()

        if (currentUserUid == null) return

        // Verifica se o modo de edição está ativo antes de salvar
        if (!isEditing) {
            Toast.makeText(requireContext(), "Pressione 'Editar' para salvar alterações.", Toast.LENGTH_SHORT).show()
            return
        }

        val enderecoFinal = enderecoFormatadoParaAnuncio
        if (enderecoFinal.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Aguarde o endereço ser carregado ou vincule um endereço.", Toast.LENGTH_LONG).show()
            return
        }

        // Salva os dados do ANÚNCIO (anuncios/{uid})
        val novoAnuncioMap = mapOf(
            "breveDescricao" to breveDescricao,
            "detalhes" to detalhes,
            "endereco" to enderecoFinal
        )

        database.child("anuncios").child(currentUserUid)
            .setValue(novoAnuncioMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Anúncio salvo com sucesso!", Toast.LENGTH_SHORT).show()
                // Após salvar, retorna ao modo de visualização
                toggleEditMode(false)
                // Navega para cima (opcional, dependendo do fluxo)
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                Log.e("EditarAnuncio", "Erro ao salvar anúncio: ${e.message}")
                Toast.makeText(requireContext(), "Falha ao salvar anúncio.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}