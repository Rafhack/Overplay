package com.example.overplay

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.overplay.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        viewModel.dispatch(MainUserAction.ViewScreen)

        lifecycleScope.launch {
            viewModel.mainStateFlow.collect(::renderViewState)
        }
    }

    private fun renderViewState(viewState: MainViewState) {
        Toast.makeText(this, if (viewState.isLoading) {
            "Loading..."
        } else {
            "Ready!"
        }, Toast.LENGTH_SHORT).show()
    }
}