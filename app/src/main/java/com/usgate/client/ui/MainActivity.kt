package com.usgate.client.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.usgate.client.R
import com.usgate.client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Replace splash theme with main brand theme before inflate
        setTheme(R.style.Theme_USGate)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_settings -> {
                    openSettings()
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragment_container, HomeFragment())
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val atRoot = supportFragmentManager.backStackEntryCount == 0
            supportActionBar?.setDisplayHomeAsUpEnabled(!atRoot)
            binding.toolbar.title = when {
                atRoot -> getString(R.string.app_name)
                else -> supportFragmentManager.fragments.lastOrNull()?.let { frag ->
                    when (frag) {
                        is SettingsFragment -> getString(R.string.settings_title)
                        is NodeListFragment -> getString(R.string.nodes_title)
                        else -> getString(R.string.app_name)
                    }
                } ?: getString(R.string.app_name)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    fun openSettings() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, SettingsFragment())
            addToBackStack("settings")
        }
    }

    fun openNodes() {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, NodeListFragment())
            addToBackStack("nodes")
        }
    }
}
