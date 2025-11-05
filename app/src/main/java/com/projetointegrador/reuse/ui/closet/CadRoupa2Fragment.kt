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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
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

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private val gavetaDoar = listOf("Doação")
    private val gavetaVender = listOf("Vendas")
    private val gavetasOrganizarDefault = listOf("Casual", "Trabalho", "Academia", "Festa", "Outro")
    private val gavetasDeTransacao = listOf("Vendas", "Doação", "Carrinho")

    // Esta variável AGORA armazena o UID da gaveta
    private var gavetaSelecionada: String? = null

    private var nameToUidMap: Map<String, String> = emptyMap()
    private var isSavingPeca = false
    private var isLoadingGavetas = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCadRoupa2Binding.inflate(inflater, container, false)
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

    private fun setupViewMode() {
        val isCreating = args.isCreating
        val isEditing = args.isEditing

        binding.btnCadastrarPeca.visibility = if (isCreating) View.VISIBLE else View.GONE
        binding.bttSalvar.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.trash2.visibility = if (!isCreating && !isEditing) View.VISIBLE else View.GONE

        val shouldEnableFields = isCreating || isEditing
        setFieldsEnabled(shouldEnableFields)

        if (!isCreating) {
            loadPecaData(pecaEmAndamento)
        } else {
            binding.radioButton5.isChecked = true
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

    private fun findUserAccountType() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado. Carregando gavetas padrão.", Toast.LENGTH_SHORT).show()
            updateSpinner(emptyList())
            return
        }

        database.child("usuarios").child("pessoaFisica").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        getGavetaUidsFromUser(userId, "pessoaFisica", null)
                    } else {
                        searchPessoaJuridicaForGavetaUids(userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao buscar tipo de conta: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList())
                }
            })
    }

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
                            getGavetaUidsFromUser(userId, "pessoaJuridica", subtipo)
                            return
                        }

                        if (subtiposChecados == totalSubtipos && !found) {
                            Toast.makeText(requireContext(), "Nenhuma gaveta encontrada ou tipo de conta não identificado.", Toast.LENGTH_LONG).show()
                            updateSpinner(emptyList())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        subtiposChecados++
                        Toast.makeText(requireContext(), "Erro ao buscar subtipo: ${error.message}", Toast.LENGTH_LONG).show()
                        if (subtiposChecados == totalSubtipos && !found) {
                            updateSpinner(emptyList())
                        }
                    }
                })
        }
    }

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
                    for (gavetaSnapshot in snapshot.children) {
                        gavetaUids.add(gavetaSnapshot.key!!)
                    }

                    if (gavetaUids.isNotEmpty()) {
                        fetchGavetaNames(gavetaUids)
                    } else {
                        Toast.makeText(requireContext(), "Você ainda não possui gavetas customizadas de organização.", Toast.LENGTH_LONG).show()
                        updateSpinner(emptyList())
                        gavetaSelecionada = null
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao listar UIDs das gavetas: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList())
                }
            })
    }

    private fun fetchGavetaNames(gavetaUids: List<String>) {
        val totalGavetas = gavetaUids.size
        var gavetasCarregadas = 0
        val gavetaNames = mutableListOf<String>()
        val mutableMap = mutableMapOf<String, String>()

        for (uid in gavetaUids) {
            database.child("gavetas").child(uid).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue(String::class.java)
                        if (name != null && !gavetasDeTransacao.contains(name)) {
                            gavetaNames.add(name)
                            mutableMap[name] = uid
                        }
                        gavetasCarregadas++

                        if (gavetasCarregadas == totalGavetas) {
                            val finalGavetaNames = gavetaNames
                            nameToUidMap = mutableMap.toMap()
                            updateSpinner(finalGavetaNames)
                            setupSpinnerNameListener()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        gavetasCarregadas++
                        if (gavetasCarregadas == totalGavetas) {
                            val finalGavetaNames = gavetaNames
                            nameToUidMap = mutableMap.toMap()
                            updateSpinner(finalGavetaNames)
                            setupSpinnerNameListener()
                        }
                    }
                })
        }
    }

    private fun setupSpinnerNameListener() {
        binding.spinner.onItemSelectedListener = null
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val nomeSelecionado = parent?.getItemAtPosition(position).toString()
                gavetaSelecionada = nameToUidMap[nomeSelecionado]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                gavetaSelecionada = null
            }
        }
        if (binding.spinner.adapter != null && binding.spinner.adapter.count > 0) {
            val nomeSelecionado = binding.spinner.adapter.getItem(0).toString()
            gavetaSelecionada = nameToUidMap[nomeSelecionado]
        } else {
            gavetaSelecionada = null
        }
    }

    private fun fetchGavetaUidByName(gavetaName: String, onComplete: (String?) -> Unit) {
        database.child("gavetas")
            .orderByChild("name")
            .equalTo(gavetaName)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val uid = snapshot.children.firstOrNull()?.key
                    onComplete(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Erro ao buscar UID da gaveta '$gavetaName': ${error.message}", Toast.LENGTH_LONG).show()
                    onComplete(null)
                }
            })
    }

    private fun setupListeners() {
        binding.Finalidade.setOnCheckedChangeListener { _, checkedId ->
            val finalidadeSelecionada = when (checkedId) {
                R.id.radioButton5 -> {
                    findUserAccountType()
                    getString(R.string.app_organizar)
                }
                R.id.radioButton6 -> {
                    val name = gavetaDoar.first()
                    updateSpinner(gavetaDoar)
                    binding.spinner.onItemSelectedListener = null
                    fetchGavetaUidByName(name) { uid ->
                        gavetaSelecionada = uid
                        if (uid == null) {
                            Toast.makeText(requireContext(), "Aviso: Gaveta 'Doação' não encontrada no banco.", Toast.LENGTH_LONG).show()
                        }
                    }
                    getString(R.string.app_doar)
                }
                R.id.radioButton7 -> {
                    val name = gavetaVender.first()
                    updateSpinner(gavetaVender)
                    binding.spinner.onItemSelectedListener = null
                    fetchGavetaUidByName(name) { uid ->
                        gavetaSelecionada = uid
                        if (uid == null) {
                            Toast.makeText(requireContext(), "Aviso: Gaveta 'Vendas' não encontrada no banco.", Toast.LENGTH_LONG).show()
                        }
                    }
                    getString(R.string.app_vender)
                }
                else -> null
            }

            pecaEmAndamento.finalidade = finalidadeSelecionada
            if (finalidadeSelecionada != null) {
                updatePrecoFieldsVisibility(finalidadeSelecionada)
            }
        }

        binding.btnCadastrarPeca.setOnClickListener {
            if (!isSavingPeca && validarDados()) {
                isSavingPeca = true
                binding.btnCadastrarPeca.isEnabled = false
                collectFinalData()
                savePecaNoBanco(pecaEmAndamento, gavetaSelecionada)
            }
        }

        binding.bttSalvar.setOnClickListener {
            if (!isSavingPeca && validarDados()) {
                isSavingPeca = true
                binding.bttSalvar.isEnabled = false
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

    private fun savePecaNoBanco(peca: PecaCadastro, gavetaUid: String?) {
        if (gavetaUid.isNullOrBlank()) {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(), "Erro: ID da gaveta de destino é inválido ou nulo.", Toast.LENGTH_SHORT).show()
            return
        }

        val pecaRef = database.child("pecas").push()
        val pecaUid = pecaRef.key

        if (pecaUid != null) {
            pecaRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        database.child("gavetas")
                            .child(gavetaUid)
                            .child("peças")
                            .child(pecaUid)
                            .setValue(true)
                            .addOnCompleteListener { gavetaTask ->
                                isSavingPeca = false
                                binding.btnCadastrarPeca.isEnabled = true
                                if (gavetaTask.isSuccessful) {
                                    Toast.makeText(requireContext(), "Peça cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    // PASSAR O UID DA GAVETA NA NAVEGAÇÃO
                                    val bundle = Bundle().apply {
                                        putString("GAVETA_ID", gavetaUid)
                                    }
                                    findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment, bundle)
                                } else {
                                    Toast.makeText(requireContext(), "Erro ao vincular à gaveta: ${gavetaTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        isSavingPeca = false
                        binding.btnCadastrarPeca.isEnabled = true
                        Toast.makeText(requireContext(), "Erro ao salvar os detalhes da peça: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            isSavingPeca = false
            binding.btnCadastrarPeca.isEnabled = true
            Toast.makeText(requireContext(), "Erro ao gerar ID da peça.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePecaNoBanco(peca: PecaCadastro, novaGavetaUid: String?) {
        val pecaUid = "UID_DA_PECA_EXISTENTE" // Substituir pelo UID real da peça em edição

        if (pecaUid != null && novaGavetaUid != null) {
            val pecaUpdateRef = database.child("pecas").child(pecaUid)
            pecaUpdateRef.setValue(peca)
                .addOnCompleteListener { task ->
                    isSavingPeca = false
                    binding.bttSalvar.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Peça atualizada com sucesso!", Toast.LENGTH_SHORT).show()
                        // PASSAR O UID DA GAVETA NA NAVEGAÇÃO
                        val bundle = Bundle().apply {
                            putString("GAVETA_ID", novaGavetaUid)
                        }
                        findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment, bundle)
                    } else {
                        Toast.makeText(requireContext(), "Erro ao atualizar a peça: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            isSavingPeca = false
            binding.bttSalvar.isEnabled = true
            Toast.makeText(requireContext(), "Erro: ID da peça ou gaveta inválido.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectFinalData() {
        pecaEmAndamento.apply {
            if (finalidade == getString(R.string.app_vender)) {
                preco = binding.editEditText.text?.toString() ?: ""
                titulo = binding.editTextTitulo.text?.toString() ?: ""
                detalhe = binding.editTextDetalhes.text?.toString() ?: ""
            } else {
                preco = ""
                titulo = ""
                detalhe = ""
            }
        }
    }

    private fun validarDados(): Boolean {
        if (binding.Finalidade.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "Por favor, selecione a Finalidade da peça.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (gavetaSelecionada.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Erro de seleção: ID da gaveta não foi encontrado. Por favor, selecione novamente e aguarde o carregamento do UID.", Toast.LENGTH_LONG).show()
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