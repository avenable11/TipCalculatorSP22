package edu.ivytech.tipcalculatorsp22

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import edu.ivytech.tipcalculatorsp22.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.round

enum class Rounding{NOROUND, ROUNDTIP, ROUNDTOTAL}

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var tipPercent = .15
    private var rounding : Rounding = Rounding.NOROUND
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val items = resources.getStringArray(R.array.split_array)
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        binding.splitBillDropdown.setAdapter(adapter)
        binding.splitBillDropdown.setText(items[0], false)

        binding.calculateBtn.setOnClickListener { calculateAndDisplay() }

        binding.tipPercentSlider.addOnChangeListener { slider, value, fromUser ->
            tipPercent = value / 100.0
            val percentFormat = NumberFormat.getPercentInstance()
            binding.tipPercentDisplay.text = percentFormat.format(tipPercent)
        }
        binding.radioGroup.setOnCheckedChangeListener { group : RadioGroup, checkedId : Int ->
            when (checkedId) {
                R.id.noRoundBtn -> rounding = Rounding.NOROUND
                R.id.roundTipBtn -> rounding = Rounding.ROUNDTIP
                R.id.roundTotalBtn -> rounding = Rounding.ROUNDTOTAL
            }
        }
    }

    private fun calculateAndDisplay() {
        val billAmtStr = binding.billAmountEditText.text.toString()
        var billAmount = 0.0
        if(billAmtStr.isNotEmpty()) {
            billAmount = billAmtStr!!.toDouble()
        }

        var tipAmount = billAmount * tipPercent
        var totalAmount = billAmount + tipAmount

        if(rounding == Rounding.ROUNDTIP) {
            tipAmount = ceil(tipAmount)
            totalAmount = billAmount + tipAmount
        } else if(rounding == Rounding.ROUNDTOTAL) {
            totalAmount = ceil(totalAmount)
            tipAmount = totalAmount - billAmount
        }


        val currencyFormat = NumberFormat.getCurrencyInstance()
        val symbol = DecimalFormatSymbols()
        symbol.currencySymbol = ""
        (currencyFormat as DecimalFormat).decimalFormatSymbols = symbol

        binding.tipAmountEditText.setText(currencyFormat.format(tipAmount))
        binding.totalAmountEditText.setText(currencyFormat.format(totalAmount))
    }
}