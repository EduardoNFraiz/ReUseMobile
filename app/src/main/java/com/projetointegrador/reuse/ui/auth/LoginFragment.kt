package com.projetointegrador.reuse.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.InputType
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.databinding.FragmentLoginBinding
import com.projetointegrador.reuse.util.showBottomSheet


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private var senhaVisivel = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        initToolbar(binding.toolbar)
        mostrarOcultarSenha()
        initListeners()
    }

    private fun initListeners() {
        binding.buttonLogar.setOnClickListener {
            valideData()
        }
        binding.textViewEsqueceuASenha.setOnClickListener{
            findNavController().navigate(R.id.action_loginFragment_to_receberCodigoFragment)
        }
    }

    private fun valideData(){
        val email = binding.editTextEmail.text.toString().trim()
        val senha = binding.editTextSenha.text.toString().trim()
        if(email.isNotBlank()) {
            if (senha.isNotBlank()) {
                loginUser(email,senha)
            } else {
                showBottomSheet(message = getString(R.string.password_empty_login))
            }
        }else{
            showBottomSheet(message = getString(R.string.email_empty_login))
        }
    }

    private fun loginUser(email: String, password: String){
        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        findNavController().navigate(R.id.action_loginFragment_to_closetFragment)
                    }else{
                        Toast.makeText(requireContext(), task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }catch (e: Exception){
            Toast.makeText(requireContext(), e.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarOcultarSenha() {
        binding.ivToggleSenha.setOnClickListener {
            senhaVisivel = !senhaVisivel

            if (senhaVisivel) {
                // Mostrar senha
                binding.editTextSenha.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivToggleSenha.setImageResource(R.drawable.olhomostrar)
            } else {
                // Ocultar senha
                binding.editTextSenha.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivToggleSenha.setImageResource(R.drawable.olhoocultar)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}