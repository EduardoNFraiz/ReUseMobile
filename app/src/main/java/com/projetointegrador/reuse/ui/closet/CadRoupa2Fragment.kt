package com.projetointegrador.reuse.ui.closet

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.data.model.PecaCadastro
import com.projetointegrador.reuse.databinding.FragmentCadRoupa2Binding
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.util.showBottomSheet
// Importações Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database


class CadRoupa2Fragment : Fragment() {
    private var _binding: FragmentCadRoupa2Binding? = null
    private val binding get() = _binding!!

    private val args: CadRoupa2FragmentArgs by navArgs()

    private lateinit var pecaEmAndamento: PecaCadastro

    // Variáveis do Firebase
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Variáveis estáticas de gaveta de Doar/Vender e Default
    private val gavetaDoar = listOf("Doação")
    private val gavetaVender = listOf("Vendas")
    private val gavetasOrganizarDefault = listOf("Casual", "Trabalho", "Academia", "Festa", "Outro")

    private var gavetaSelecionada: String? = null
    private var editando = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadRoupa2Binding.inflate(inflater, container, false)

        // Inicializa Firebase
        database = Firebase.database.reference
        auth = Firebase.auth

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar(binding.toolbar)

        pecaEmAndamento = args.PecaCadastro

        setupViewMode()
        setupListeners()
        barraDeNavegacao()
    }

    // --- (Funções setupViewMode, setFieldsEnabled, loadPecaData, updatePrecoFieldsVisibility, updateSpinner) ---
    // ... (Manter as funções auxiliares de View aqui) ...
    private fun setupViewMode() {
        val isCreating = arguments?.getBoolean("CRIANDO_ROUPA") ?: false
        editando = arguments?.getBoolean("EDITANDO") ?: false

        binding.btnCadastrarPeca.visibility = if (isCreating && !editando) View.VISIBLE else View.GONE
        binding.bttSalvar.visibility = if (editando) View.VISIBLE else View.GONE
        binding.trash2.visibility = if (!isCreating) View.VISIBLE else View.GONE

        val shouldEnableFields = isCreating || editando
        setFieldsEnabled(shouldEnableFields)

        if (!isCreating) {
            loadPecaData(pecaEmAndamento)
        }

        if (isCreating) {
            binding.radioButton5.isChecked = true
            // Carrega Gavetas Dinâmicas ao iniciar no modo Organizar
            findUserAccountType()
            updatePrecoFieldsVisibility(getString(R.string.app_organizar))
        }
    }

    private fun setFieldsEnabled(isEnabled: Boolean) {
        binding.radioButton5.isEnabled = isEnabled
        binding.radioButton6.isEnabled = isEnabled
        binding.radioButton7.isEnabled = isEnabled
        binding.spinner.isEnabled = isEnabled
        binding.editEditText.isEnabled = isEnabled
        binding.editTextTitulo.isEnabled = isEnabled
        binding.editTextDetalhes.isEnabled = isEnabled
    }

    private fun loadPecaData(peca: PecaCadastro) {
        when (peca.finalidade) {
            getString(R.string.app_organizar) -> binding.radioButton5.isChecked = true
            getString(R.string.app_doar) -> binding.radioButton6.isChecked = true
            getString(R.string.app_vender) -> binding.radioButton7.isChecked = true
            else -> {}
        }

        if (peca.finalidade == getString(R.string.app_vender)) {
            binding.editEditText.setText(peca.preco)
            binding.editTextTitulo.setText(peca.titulo)
            binding.editTextDetalhes.setText(peca.detalhe)
        }

        val checkedId = binding.Finalidade.checkedRadioButtonId
        if (checkedId != -1) {
            binding.Finalidade.check(checkedId)
        }
        updatePrecoFieldsVisibility(peca.finalidade ?: "")
    }

    private fun updatePrecoFieldsVisibility(finalidade: String) {
        val isVenda = finalidade == getString(R.string.app_vender)
        binding.editEditText.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.editTextTitulo.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.editTextDetalhes.visibility = if (isVenda) View.VISIBLE else View.GONE
        binding.textview6.visibility = if (isVenda) View.VISIBLE else View.GONE
    }

    private fun updateSpinner(opcoes: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter
    }

    // --- 1. Lógica de Busca de Gavetas Dinâmicas do Firebase (Adaptada do ClosetFragment) ---

    private fun findUserAccountType() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado. Carregando gavetas padrão.", Toast.LENGTH_SHORT).show()
            updateSpinner(gavetasOrganizarDefault)
            return
        }

        // 1. Tentar encontrar o tipo de conta (Pessoa Física)
        database.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Usuário é Pessoa Física
                        getGavetaUidsFromUser(userId, "pessoaFisica", null)
                    } else {
                        // Se não for Pessoa Física, tentar encontrar em 'pessoaJuridica'
                        searchPessoaJuridicaForGavetaUids(userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao buscar tipo de conta: ${error.message}")
                    updateSpinner(gavetasOrganizarDefault)
                }
            })
    }

    // Função auxiliar para buscar subtipos de Pessoa Jurídica
    private fun searchPessoaJuridicaForGavetaUids(userId: String) {
        val subtipos = listOf("brechos", "instituicoes")
        var found = false
        var subtiposChecados = 0
        val totalSubtipos = subtipos.size

        for (subtipo in subtipos) {
            database.child("usuarios").child("pessoaJuridica").child(subtipo).child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        subtiposChecados++
                        if (snapshot.exists() && !found) {
                            found = true
                            // Usuário é Pessoa Jurídica com o subtipo encontrado
                            getGavetaUidsFromUser(userId, "pessoaJuridica", subtipo)
                            // Retorna imediatamente após encontrar para evitar chamadas desnecessárias
                            return
                        }

                        if (subtiposChecados == totalSubtipos && !found) {
                            // Se terminou de buscar em todos e não achou, a lista fica vazia/default
                            Toast.makeText(requireContext(), "Nenhuma gaveta encontrada ou tipo de conta não identificado.", Toast.LENGTH_LONG).show()
                            updateSpinner(gavetasOrganizarDefault)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        subtiposChecados++
                        showBottomSheet(message = "Erro ao buscar subtipo: ${error.message}")
                        if (subtiposChecados == totalSubtipos && !found) {
                            updateSpinner(gavetasOrganizarDefault)
                        }
                    }
                })
        }
    }

    // Obtém os UIDs das gavetas a partir do nó do usuário e ATUALIZA O SPINNER
    private fun getGavetaUidsFromUser(userId: String, tipoConta: String, subtipo: String?) {
        val path = if (tipoConta == "pessoaFisica") {
            "usuarios/pessoaFisica/$userId/gavetas"
        } else {
            "usuarios/pessoaJuridica/$subtipo/$userId/gavetas"
        }

        database.child(path)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gavetaUids = mutableListOf<String>()
                    // O snapshot.children são os UIDs das gavetas
                    for (gavetaSnapshot in snapshot.children) {
                        gavetaUids.add(gavetaSnapshot.key!!)
                    }

                    if (gavetaUids.isNotEmpty()) {
                        // ATUALIZA O SPINNER SOMENTE COM OS UIDS (Nomes das gavetas)
                        updateSpinner(gavetaUids)
                    } else {
                        // Nenhuma gaveta cadastrada
                        Toast.makeText(requireContext(), "Você ainda não possui gavetas cadastradas. Usando padrão.", Toast.LENGTH_LONG).show()
                        updateSpinner(gavetasOrganizarDefault)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showBottomSheet(message = "Erro ao listar UIDs das gavetas: ${error.message}")
                    updateSpinner(gavetasOrganizarDefault)
                }
            })
    }

    // --- 2. Lógica de Listeners ---

    private fun setupListeners() {
        // Listener para o RadioGroup de Finalidade
        binding.Finalidade.setOnCheckedChangeListener { _, checkedId ->
            val finalidadeSelecionada = when (checkedId) {
                R.id.radioButton5 -> {
                    findUserAccountType() // BUSCA E CARREGA GAVETAS DO USUÁRIO
                    getString(R.string.app_organizar)
                }
                R.id.radioButton6 -> {
                    updateSpinner(gavetaDoar)
                    getString(R.string.app_doar)
                }
                R.id.radioButton7 -> {
                    updateSpinner(gavetaVender)
                    getString(R.string.app_vender)
                }
                else -> null
            }

            pecaEmAndamento.finalidade = finalidadeSelecionada
            if (finalidadeSelecionada != null) {
                updatePrecoFieldsVisibility(finalidadeSelecionada)
            }
        }

        // Listener para o Spinner (seleção de Gaveta)
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                gavetaSelecionada = parent?.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { gavetaSelecionada = null }
        }

        // Listener para o botão CADASTRAR PEÇA
        binding.btnCadastrarPeca.setOnClickListener {
            if (validarDados()) {
                collectFinalData()
                // O valor em 'gavetaSelecionada' é o UID da gaveta
                savePecaNoBanco(pecaEmAndamento, gavetaSelecionada)
            }
        }

        // Listener para o botão SALVAR ALTERAÇÕES
        binding.bttSalvar.setOnClickListener {
            if (validarDados()) {
                collectFinalData()
                updatePecaNoBanco(pecaEmAndamento, gavetaSelecionada)
            }
        }

        binding.trash2.setOnClickListener {
            showBottomSheet(
                titleButton = R.string.excluir,
                titleDialog = R.string.deseja_excluir,
                message = getString(R.string.click_para_excluir),
            )
        }
    }

    // --- 3. Lógica de Salvamento (Firebase) ---

    private fun savePecaNoBanco(peca: PecaCadastro, gavetaUid: String?) {
        if (gavetaUid == null) return

        val pecaRef = database.child("pecas").push()
        val pecaUid = pecaRef.key

        if (pecaUid != null) {
            // 1. Salva os detalhes completos da peça em "pecas/{UID da peça}"
            pecaRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 2. Salva a referência da peça na gaveta em "gavetas/{UID da gaveta}/peças/{UID da peça}"
                        database.child("gavetas")
                            .child(gavetaUid) // O UID da gaveta agora é o valor do spinner
                            .child("pecas")
                            .child(pecaUid)
                            .setValue(true)
                            .addOnCompleteListener { gavetaTask ->
                                if (gavetaTask.isSuccessful) {
                                    Toast.makeText(requireContext(), "Peça cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment)
                                } else {
                                    Toast.makeText(requireContext(), "Erro ao vincular à gaveta.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(requireContext(), "Erro ao salvar os detalhes da peça.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updatePecaNoBanco(peca: PecaCadastro, novaGavetaUid: String?) {
        // CORRIGIR: Obter o UID real da peça em edição
        val pecaUid = "UID_DA_PECA_EXISTENTE"

        if (pecaUid != null && novaGavetaUid != null) {
            val pecaUpdateRef = database.child("pecas").child(pecaUid)

            pecaUpdateRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Peça atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment)
                    } else {
                        Toast.makeText(requireContext(), "Erro ao atualizar a peça.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // --- 4. Funções Auxiliares (Mantidas) ---

    private fun collectFinalData() {
        pecaEmAndamento.apply {
            if (finalidade == getString(R.string.app_vender)) {
                preco = binding.editEditText.text?.toString()
                titulo = binding.editTextTitulo.text?.toString()
                detalhe = binding.editTextDetalhes.text?.toString()
            } else {
                preco = null
                titulo = null
                detalhe = null
            }
        }
    }

    private fun validarDados(): Boolean {
        if (binding.Finalidade.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Por favor, selecione a Finalidade da peça.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (gavetaSelecionada.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Por favor, selecione a Gaveta de destino.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (pecaEmAndamento.finalidade == getString(R.string.app_vender)) {
            if (binding.editTextTitulo.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Por favor, insira um Título para a peça.", Toast.LENGTH_SHORT).show()
                return false
            }
            if (binding.editEditText.text.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Por favor, insira o Preço de venda.", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun barraDeNavegacao() {
        binding.closet.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.pesquisar.setOnClickListener { findNavController().navigate(R.id.pesquisar) }
        binding.cadastrarRoupa.setOnClickListener { findNavController().navigate(R.id.closet) }
        binding.doacao.setOnClickListener { findNavController().navigate(R.id.doacao) }
        binding.perfil.setOnClickListener { findNavController().navigate(R.id.perfil) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}