package edu.ivytech.tipcalculatorsp22

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import edu.ivytech.tipcalculatorsp22.databinding.ActivityMainBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import kotlin.math.ceil
import kotlin.math.round

enum class Rounding{NOROUND, ROUNDTIP, ROUNDTOTAL}
enum class ContextMenuItems{POUND, YEN, EURO, DOLLAR}
class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var tipPercent = .15
    private var rounding : Rounding = Rounding.NOROUND
    private var split = 1
    private lateinit var savedValues : SharedPreferences
    private val tipKey = "saved_tip"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val items = resources.getStringArray(R.array.split_array)
        val adapter = ArrayAdapter(this, R.layout.list_item, items)
        binding.splitBillDropdown.setAdapter(adapter)
        binding.splitBillDropdown.setText(items[0], false)
        binding.splitBillDropdown.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                for(i in items.indices) {
                    if(items[i] == s.toString()) {
                        split = i + 1
                        break
                    }
                }

                if(split > 1) {
                    binding.splitAmountLayout.visibility = View.VISIBLE
                } else {
                    binding.splitAmountLayout.visibility = View.GONE
                }
                calculateAndDisplay()
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

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

        registerForContextMenu(binding.billAmountEditText)
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        when (pref.getString(getString(R.string.currency_key),"$")) {
            getString(R.string.currency_symbol) -> changeCurrencySymbol(getString(R.string.currency_symbol))
            getString(R.string.pound_symbol) -> changeCurrencySymbol(getString(R.string.pound_symbol))
            getString(R.string.yen_symbol) -> changeCurrencySymbol(getString(R.string.yen_symbol))
            getString(R.string.euro_symbol) -> changeCurrencySymbol(getString(R.string.euro_symbol))
        }

        when (pref.getString(getString(R.string.rounding_key), "0")) {
            Rounding.NOROUND.ordinal.toString() -> {
                binding.radioGroup.check(R.id.noRoundBtn)
                rounding = Rounding.NOROUND
            }
            Rounding.ROUNDTIP.ordinal.toString() -> {
                binding.radioGroup.check(R.id.roundTipBtn)
                rounding = Rounding.ROUNDTIP
            }
            Rounding.ROUNDTOTAL.ordinal.toString() -> {
                binding.radioGroup.check(R.id.roundTotalBtn)
                rounding = Rounding.ROUNDTOTAL
            }
        }
        savedValues = getSharedPreferences("SavedValues", MODE_PRIVATE)

    }

    override fun onResume() {
        super.onResume()
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        if(pref.getBoolean(getString(R.string.remember_tip_key), false)) {
            val defaultTip = pref.getString(getString(R.string.default_tip_key), "15")
            var defaultTipNum = defaultTip!!.filter { it.isDigit() || it == '.' }
            if (defaultTipNum.isNullOrEmpty())
                defaultTipNum = "15"
            try {
                tipPercent = defaultTipNum!!.toInt() / 100.0
                binding.tipPercentSlider.value = defaultTipNum.toFloat()
                binding.tipPercentDisplay.text = defaultTipNum + "%"
            } catch (e: Exception) {
                Log.e("MainActivity.onResume", "There is an error with the default tip percent. check that there are no letters in the value")
            }
        } else {
            tipPercent = savedValues.getFloat(tipKey, 0.15f).toDouble()
            binding.tipPercentSlider.value = tipPercent.toFloat() * 100.0f
            val percentFormat = NumberFormat.getPercentInstance()
            binding.tipPercentDisplay.text = percentFormat.format(tipPercent)
        }

    }

    override fun onPause() {
        val editor = savedValues.edit()
        editor.putFloat(tipKey, tipPercent.toFloat())
        editor.apply()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.settings_menu -> {
                Toast.makeText(this, "Clicked Settings Menu Item", Toast.LENGTH_LONG).show()
                val intent = Intent(this,SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.about_menu -> {
                Toast.makeText(this, "Clicked About Menu Item", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val prefix = binding.billAmountLayout.prefixText
        if (prefix == "£")
            menu?.add(Menu.NONE, ContextMenuItems.DOLLAR.ordinal,
                ContextMenuItems.DOLLAR.ordinal, "Change to $")
        else
            menu?.add(Menu.NONE, ContextMenuItems.POUND.ordinal,
                ContextMenuItems.POUND.ordinal, "Change to £")
        if(prefix == "¥")
            menu?.add(Menu.NONE, ContextMenuItems.DOLLAR.ordinal,
                ContextMenuItems.DOLLAR.ordinal, "Change to $")
        else
            menu?.add(Menu.NONE, ContextMenuItems.YEN.ordinal,
                ContextMenuItems.YEN.ordinal, "Change to ¥")
        if(prefix == "€")
            menu?.add(Menu.NONE, ContextMenuItems.DOLLAR.ordinal,
                ContextMenuItems.DOLLAR.ordinal, "Change to $")
        else
            menu?.add(Menu.NONE, ContextMenuItems.EURO.ordinal,
                ContextMenuItems.EURO.ordinal, "Change to €")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ContextMenuItems.POUND.ordinal -> {
                changeCurrencySymbol(getString(R.string.pound_symbol))
                true
            }
            ContextMenuItems.YEN.ordinal -> {
                changeCurrencySymbol(getString(R.string.yen_symbol))
                true
            }
            ContextMenuItems.EURO.ordinal -> {
                changeCurrencySymbol(getString(R.string.euro_symbol))
                true
            }
            ContextMenuItems.DOLLAR.ordinal -> {
                changeCurrencySymbol(getString(R.string.currency_symbol))
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }
    private fun changeCurrencySymbol(currencySymbol: String) {
        binding.billAmountLayout.prefixText = currencySymbol
        binding.tipAmountLayout.prefixText = currencySymbol
        binding.totalAmountLayout.prefixText = currencySymbol
        binding.splitAmountLayout.prefixText = currencySymbol
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

        if(split > 1) {
            var splitAmount = totalAmount / split
            binding.splitAmountEditText.setText(currencyFormat.format(splitAmount))
        }

        binding.tipAmountEditText.setText(currencyFormat.format(tipAmount))
        binding.totalAmountEditText.setText(currencyFormat.format(totalAmount))
    }
}