package com.projetointegrador.reuse.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.text.InputType
import androidx.navigation.fragment.findNavController
import com.projetointegrador.reuse.R
import com.projetointegrador.reuse.util.initToolbar
import com.projetointegrador.reuse.databinding.FragmentLoginBinding


class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

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
        initToolbar(binding.toolbar)
        mostrarOcultarSenha()
        initListeners()
    }

    private fun initListeners() {
        binding.buttonLogar.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_closetFragment)
        }
        binding.textViewEsqueceuASenha.setOnClickListener{
            findNavController().navigate(R.id.action_loginFragment_to_receberCodigoFragment)
        }
    }

    private fun mostrarOcultarSenha() {
        binding.ivToggleSenha.setOnClickListener {
            senhaVisivel = !senhaVisivel

            if (senhaVisivel) {
                // Mostrar senha
                binding.etSenha.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivToggleSenha.setImageResource(R.drawable.olhomostrar)
            } else {
                // Ocultar senha
                binding.etSenha.inputType =
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