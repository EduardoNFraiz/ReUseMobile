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
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database


class CadRoupa2Fragment : Fragment() {
    private var _binding: FragmentCadRoupa2Binding? = null
    private val binding get() = _binding!!

    // Usando Safe Args para receber os argumentos da navegação
    private val args: CadRoupa2FragmentArgs by navArgs()

    private lateinit var pecaEmAndamento: PecaCadastro

    // Variáveis do Firebase
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // Variáveis estáticas de gaveta de transação (NOME)
    private val gavetaDoar = listOf("Doação")
    private val gavetaVender = listOf("Vendas")
    // Gavetas Padrão de Organização (Não será mais usada na lista do Spinner)
    private val gavetasOrganizarDefault = listOf("Casual", "Trabalho", "Academia", "Festa", "Outro")
    // Gavetas a serem EXCLUÍDAS da lista "Organizar"
    private val gavetasDeTransacao = listOf("Vendas", "Doação", "Carrinho")

    // Esta variável AGORA armazena o UID da gaveta
    private var gavetaSelecionada: String? = null

    // Mapa para traduzir o NOME da gaveta para o UID (usado apenas no modo Organizar/Dinâmico)
    private var nameToUidMap: Map<String, String> = emptyMap()

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

        // Atribui o objeto PecaCadastro recebido via Safe Args
        pecaEmAndamento = args.PecaCadastro

        setupViewMode()
        setupListeners()
        barraDeNavegacao()
    }

    // --- Funções setupViewMode, setFieldsEnabled, loadPecaData, updatePrecoFieldsVisibility, updateSpinner ---
    private fun setupViewMode() {
        val isCreating = args.isCreating // Lê dos Safe Args
        val isEditing = args.isEditing   // Lê dos Safe Args

        // Lógica de Visibilidade dos Botões
        binding.btnCadastrarPeca.visibility = if (isCreating) View.VISIBLE else View.GONE
        binding.bttSalvar.visibility = if (isEditing) View.VISIBLE else View.GONE
        // A lixeira só aparece se for uma peça existente (não criando) e não estiver em modo de edição
        binding.trash2.visibility = if (!isCreating && !isEditing) View.VISIBLE else View.GONE

        val shouldEnableFields = isCreating || isEditing
        setFieldsEnabled(shouldEnableFields)

        if (!isCreating) {
            // Se não estiver criando (visualizando ou editando), carrega os dados
            loadPecaData(pecaEmAndamento)
        } else {
            // Se estiver criando, define a finalidade padrão e carrega as gavetas
            binding.radioButton5.isChecked = true // Organizar como padrão
            // Dispara o fluxo de busca de nomes de gaveta para o spinner
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

    // --- 1. Lógica de Busca de Gavetas Dinâmicas do Firebase ---

    private fun findUserAccountType() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado. Carregando gavetas padrão.", Toast.LENGTH_SHORT).show()
            updateSpinner(emptyList()) // <-- Listagem vazia se não autenticado
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
                    // Usando Toast para evitar o showBottomSheet confuso
                    Toast.makeText(requireContext(), "Erro ao buscar tipo de conta: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList()) // <-- Listagem vazia em caso de erro
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
                            // Se terminou de buscar em todos e não achou, a lista fica vazia
                            Toast.makeText(requireContext(), "Nenhuma gaveta encontrada ou tipo de conta não identificado.", Toast.LENGTH_LONG).show()
                            updateSpinner(emptyList()) // <-- Listagem vazia
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        subtiposChecados++
                        // Usando Toast
                        Toast.makeText(requireContext(), "Erro ao buscar subtipo: ${error.message}", Toast.LENGTH_LONG).show()
                        if (subtiposChecados == totalSubtipos && !found) {
                            updateSpinner(emptyList()) // <-- Listagem vazia em caso de erro
                        }
                    }
                })
        }
    }

    // Obtém os UIDs das gavetas a partir do nó do usuário
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
                        // Buscar os nomes reais das gavetas usando os UIDs (e filtrá-los)
                        fetchGavetaNames(gavetaUids)
                    } else {
                        // Nenhuma gaveta customizada cadastrada.
                        Toast.makeText(requireContext(), "Você ainda não possui gavetas customizadas de organização.", Toast.LENGTH_LONG).show()

                        updateSpinner(emptyList()) // <-- Define lista vazia
                        gavetaSelecionada = null // Garante que o campo de seleção esteja nulo
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Usando Toast
                    Toast.makeText(requireContext(), "Erro ao listar UIDs das gavetas: ${error.message}", Toast.LENGTH_LONG).show()
                    updateSpinner(emptyList()) // <-- Listagem vazia em caso de erro
                }
            })
    }

    /**
     * Busca o nome de cada gaveta a partir do nó /gavetas/{uid} e atualiza o Spinner
     * APENAS com gavetas customizadas, excluindo as gavetas de transação (Vendas, Doação, Carrinho).
     */
    private fun fetchGavetaNames(gavetaUids: List<String>) {
        val totalGavetas = gavetaUids.size
        var gavetasCarregadas = 0
        val gavetaNames = mutableListOf<String>()

        // Mapa para manter a relação Nome -> UID. Essencial para salvar a peça depois.
        val mutableMap = mutableMapOf<String, String>()

        for (uid in gavetaUids) {
            // Busca apenas o campo 'name' da gaveta
            database.child("gavetas").child(uid).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue(String::class.java)

                        // --- LÓGICA DE FILTRAGEM ---
                        // Apenas adiciona se o nome não for uma gaveta de transação (Vendas, Doação, Carrinho)
                        if (name != null && !gavetasDeTransacao.contains(name)) {
                            gavetaNames.add(name)
                            mutableMap[name] = uid // Mapeia o nome para o UID
                        }
                        // ----------------------------------

                        gavetasCarregadas++

                        if (gavetasCarregadas == totalGavetas) {
                            // 1. Usa APENAS as gavetas customizadas filtradas
                            val finalGavetaNames = gavetaNames

                            // 2. Atualiza o mapa de tradução global (apenas para as gavetas customizadas com UID)
                            nameToUidMap = mutableMap.toMap()

                            // 3. ATUALIZA O SPINNER SOMENTE COM OS NOMES FILTRADOS
                            updateSpinner(finalGavetaNames)

                            // 4. CONFIGURA O LISTENER QUE VAI USAR O MAPA
                            setupSpinnerNameListener()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Ignorar gaveta em caso de erro, mas continuar o loop
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

    /**
     * Ajusta o Listener do Spinner para usar o mapa Nome -> UID
     */
    private fun setupSpinnerNameListener() {

        // Remove listeners antigos para evitar duplicação ou conflitos
        binding.spinner.onItemSelectedListener = null

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val nomeSelecionado = parent?.getItemAtPosition(position).toString()

                // O valor salvo em gavetaSelecionada é o UID real (valor do mapa)
                gavetaSelecionada = nameToUidMap[nomeSelecionado]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                gavetaSelecionada = null
            }
        }

        // Inicializa o valor da gaveta com base no primeiro item (se houver)
        if (binding.spinner.adapter != null && binding.spinner.adapter.count > 0) {
            val nomeSelecionado = binding.spinner.adapter.getItem(0).toString()
            gavetaSelecionada = nameToUidMap[nomeSelecionado]
        } else {
            // Se o adapter estiver vazio (sem gavetas customizadas), setamos como nulo
            gavetaSelecionada = null
        }
    }

    /**
     * CORREÇÃO: Busca o UID de uma gaveta pelo seu nome (Usado para 'Vendas' e 'Doação').
     * Isso garante que gavetaSelecionada tenha o UID real, conforme o banco exige.
     */
    private fun fetchGavetaUidByName(gavetaName: String, onComplete: (String?) -> Unit) {
        database.child("gavetas")
            .orderByChild("name")
            .equalTo(gavetaName)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Pega a chave (UID) do primeiro resultado encontrado
                    val uid = snapshot.children.firstOrNull()?.key
                    onComplete(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Usando Toast
                    Toast.makeText(requireContext(), "Erro ao buscar UID da gaveta '$gavetaName': ${error.message}", Toast.LENGTH_LONG).show()
                    onComplete(null)
                }
            })
    }

    // --- 2. Lógica de Listeners ---

    private fun setupListeners() {
        // Listener para o RadioGroup de Finalidade
        binding.Finalidade.setOnCheckedChangeListener { _, checkedId ->
            val finalidadeSelecionada = when (checkedId) {
                R.id.radioButton5 -> {
                    findUserAccountType() // BUSCA E CARREGA APENAS GAVETAS CUSTOMIZADAS
                    getString(R.string.app_organizar)
                }
                R.id.radioButton6 -> {
                    val name = gavetaDoar.first()
                    updateSpinner(gavetaDoar)
                    binding.spinner.onItemSelectedListener = null // Desabilita listener de mapeamento

                    // BUSCA ASSÍNCRONA DO UID REAL para "Doação"
                    fetchGavetaUidByName(name) { uid ->
                        gavetaSelecionada = uid // Seta o UID real
                        if (uid == null) {
                            Toast.makeText(requireContext(), "Aviso: Gaveta 'Doação' não encontrada no banco.", Toast.LENGTH_LONG).show()
                        }
                    }
                    getString(R.string.app_doar)
                }
                R.id.radioButton7 -> {
                    val name = gavetaVender.first()
                    updateSpinner(gavetaVender)
                    binding.spinner.onItemSelectedListener = null // Desabilita listener de mapeamento

                    // BUSCA ASSÍNCRONA DO UID REAL para "Vendas"
                    fetchGavetaUidByName(name) { uid ->
                        gavetaSelecionada = uid // Seta o UID real
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

        // Listener para o botão CADASTRAR PEÇA
        binding.btnCadastrarPeca.setOnClickListener {
            if (validarDados()) {
                collectFinalData()
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
        if (gavetaUid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Erro: ID da gaveta de destino é inválido ou nulo.", Toast.LENGTH_SHORT).show()
            return
        }

        val pecaRef = database.child("pecas").push()
        val pecaUid = pecaRef.key

        if (pecaUid != null) {
            // 1. Salva os detalhes completos da peça em "pecas/{UID da peça}"
            pecaRef.setValue(peca)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 2. Salva a referência da peça na gaveta usando o UID real
                        database.child("gavetas")
                            .child(gavetaUid) // Usa o UID real buscado
                            .child("peças")
                            .child(pecaUid)
                            .setValue(true)
                            .addOnCompleteListener { gavetaTask ->
                                if (gavetaTask.isSuccessful) {
                                    Toast.makeText(requireContext(), "Peça cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_cadRoupa2Fragment_to_gavetaFragment)
                                } else {
                                    // FALHA NA VINCULAÇÃO
                                    Toast.makeText(requireContext(), "Erro ao vincular à gaveta.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        // FALHA AO SALVAR PEÇA PRINCIPAL
                        Toast.makeText(requireContext(), "Erro ao salvar os detalhes da peça.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updatePecaNoBanco(peca: PecaCadastro, novaGavetaUid: String?) {
        // LÓGICA DE EDIÇÃO/ATUALIZAÇÃO (necessita do UID da peça original)
        val pecaUid = "UID_DA_PECA_EXISTENTE" // Substituir pelo UID real da peça em edição

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

    // --- 4. Funções Auxiliares ---

    private fun collectFinalData() {
        pecaEmAndamento.apply {
            if (finalidade == getString(R.string.app_vender)) {
                // Se for venda, pega os dados e usa "" se forem nulos
                preco = binding.editEditText.text?.toString() ?: ""
                titulo = binding.editTextTitulo.text?.toString() ?: ""
                detalhe = binding.editTextDetalhes.text?.toString() ?: ""
            } else {
                // Se não for venda (Organizar ou Doar), define explicitamente como string vazia
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

        // Verifica se gavetaSelecionada é nula. (PONTO CRÍTICO)
        if (gavetaSelecionada.isNullOrBlank()) {
            // SUBSTITUI A CHAMADA DE ERRO CONFUZA PELO TOAST:
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