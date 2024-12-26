package com.unifydream.currencyconverter.ui.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.unifydream.currencyconverter.ui.adapter.CurrencyAdapter
import com.unifydream.currencyconverter.databinding.ActivityCurrencyConverterBinding
import com.unifydream.currencyconverter.ui.viewmodel.CurrencyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CurrencyConverterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCurrencyConverterBinding
    private lateinit var currencyAdapter: CurrencyAdapter
    private val viewModel: CurrencyViewModel by viewModels()

    private var selectedCurrencyPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        subscribeLiveData()

        binding.convertButton.setOnClickListener {
            try {
                val amountText = binding.amountEditText.text.toString()
                if (amountText.isNotEmpty()) {
                    selectedCurrencyPosition = binding.currencySpinner.selectedItemPosition
                    val amount = amountText.toDouble()
                    val selectedCurrency = binding.currencySpinner.selectedItem?.toString()
                    if (selectedCurrency != null) {
                        viewModel.refreshDataIfNeeded()
                        viewModel.calculateConversion(amount, selectedCurrency)
                    } else {
                        Toast.makeText(this, "Please select a currency",
                            Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter an amount",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid amount",
                    Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setupRecyclerView() {
        currencyAdapter = CurrencyAdapter()
        binding.currencyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CurrencyConverterActivity)
            adapter = currencyAdapter
        }
    }

    private fun subscribeLiveData() {
        viewModel.exchangeRates.observe(this) { rates ->
            val currencyCodes = rates.map { it.code }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                currencyCodes)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.currencySpinner.adapter = adapter
            binding.currencySpinner.setSelection(selectedCurrencyPosition)
        }


        viewModel.conversionResults.observe(this) { results ->
            currencyAdapter.updateData(results)
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }
}