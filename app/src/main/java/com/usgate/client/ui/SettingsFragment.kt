package com.usgate.client.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.usgate.client.R
import com.usgate.client.UsGateApp
import com.usgate.client.databinding.FragmentSettingsBinding
import com.usgate.client.subscription.SubscriptionImporter
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = UsGateApp.instance.prefs
        binding.etSubscription.setText(prefs.subscriptionUrl)

        binding.btnSave.setOnClickListener {
            prefs.subscriptionUrl = binding.etSubscription.text?.toString().orEmpty().trim()
            Toast.makeText(requireContext(), R.string.save_ok, Toast.LENGTH_SHORT).show()
        }

        binding.btnImport.setOnClickListener {
            val input = binding.etSubscription.text?.toString().orEmpty().trim()
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), R.string.subscription_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.subscriptionUrl = input
            binding.tvImportResult.text = "导入中…"
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val nodes = if (input.startsWith("http://") || input.startsWith("https://")) {
                        SubscriptionImporter.importFromUrl(input)
                    } else {
                        SubscriptionImporter.importFromText(input)
                    }
                    prefs.saveNodes(nodes)
                    if (nodes.isNotEmpty() && prefs.selectedNodeId == null) {
                        prefs.selectedNodeId = nodes.first().id
                    }
                    binding.tvImportResult.text = getString(R.string.import_ok, nodes.size)
                } catch (e: Exception) {
                    binding.tvImportResult.text = getString(R.string.import_fail, e.message ?: "unknown")
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
