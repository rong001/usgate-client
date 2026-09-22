package com.usgate.client.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.usgate.client.UsGateApp
import com.usgate.client.databinding.FragmentNodesBinding
import com.usgate.client.databinding.ItemNodeBinding
import com.usgate.client.subscription.ProxyNode

class NodeListFragment : Fragment() {
    private var _binding: FragmentNodesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val nodes = UsGateApp.instance.prefs.loadNodes()
        if (nodes.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvNodes.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvNodes.visibility = View.VISIBLE
            binding.rvNodes.layoutManager = LinearLayoutManager(requireContext())
            binding.rvNodes.adapter = NodeAdapter(nodes) { node ->
                UsGateApp.instance.prefs.selectedNodeId = node.id
                Toast.makeText(requireContext(), "已选择：${node.name}", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private class NodeAdapter(
        private val items: List<ProxyNode>,
        private val onClick: (ProxyNode) -> Unit
    ) : RecyclerView.Adapter<NodeAdapter.VH>() {

        class VH(val binding: ItemNodeBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemNodeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val node = items[position]
            holder.binding.tvName.text = node.name
            holder.binding.tvMeta.text = node.displayMeta()
            holder.binding.root.setOnClickListener { onClick(node) }
        }
    }
}
