package com.usgate.client.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.usgate.client.R
import com.usgate.client.UsGateApp
import com.usgate.client.databinding.FragmentHomeBinding
import com.usgate.client.vpn.UsGateVpnService
import com.usgate.client.vpn.VpnController

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var connected = false

    private val vpnPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                VpnController.start(requireContext())
                setStatus(connecting = true)
            } else {
                Toast.makeText(requireContext(), R.string.vpn_permission_required, Toast.LENGTH_SHORT).show()
            }
        }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(UsGateVpnService.EXTRA_STATE)) {
                UsGateVpnService.STATE_CONNECTED -> {
                    connected = true
                    setStatus(connected = true)
                }
                UsGateVpnService.STATE_DISCONNECTED -> {
                    connected = false
                    setStatus(connected = false)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        refreshNodeLabel()
        binding.btnConnect.setOnClickListener { toggleConnect() }
        binding.btnSelectNode.setOnClickListener {
            (activity as? MainActivity)?.openNodes()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNodeLabel()
        val filter = IntentFilter(UsGateVpnService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= 33) {
            requireContext().registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                requireContext(),
                stateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onPause() {
        requireContext().unregisterReceiver(stateReceiver)
        super.onPause()
    }

    private fun refreshNodeLabel() {
        val node = UsGateApp.instance.prefs.selectedNode()
        binding.tvNode.text = node?.let { "${it.name}\n${it.displayMeta()}" }
            ?: getString(R.string.no_node_selected)
    }

    private fun toggleConnect() {
        if (connected) {
            VpnController.stop(requireContext())
            setStatus(connected = false)
            connected = false
            return
        }
        val node = UsGateApp.instance.prefs.selectedNode()
        if (node == null) {
            Toast.makeText(requireContext(), R.string.need_node, Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.openNodes()
            return
        }
        setStatus(connecting = true)
        VpnController.ensurePermissionThenStart(requireActivity(), vpnPermission) {
            setStatus(connecting = true)
        }
    }

    private fun setStatus(connected: Boolean = false, connecting: Boolean = false) {
        val state = ConnectionUiState.fromFlags(connected = connected, connecting = connecting)
        binding.tvStatus.text = getString(state.statusLabelRes())
        binding.btnConnect.text = getString(state.primaryButtonRes())
        val colorRes = when (state) {
            ConnectionUiState.CONNECTING -> R.color.usgate_muted
            ConnectionUiState.CONNECTED -> R.color.usgate_connected
            ConnectionUiState.ERROR -> R.color.usgate_danger
            ConnectionUiState.DISCONNECTED -> R.color.usgate_text
        }
        binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
