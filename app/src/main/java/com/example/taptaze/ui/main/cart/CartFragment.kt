package com.example.taptaze.ui.main.cart

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.taptaze.R
import com.razorpay.PaymentResultWithDataListener
import com.example.taptaze.common.invisible
import com.example.taptaze.common.visible
import com.example.taptaze.data.model.request.ClearCartRequest
import com.example.taptaze.databinding.FragmentCartBinding
import com.example.taptaze.ui.login.AuthViewModel
import com.example.taptaze.ui.main.MainActivity
import com.razorpay.Checkout
import com.razorpay.PaymentData
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart), CartAdapter.CartListener, PaymentResultWithDataListener {

    private lateinit var binding: FragmentCartBinding
    private val cartAdapter by lazy { CartAdapter(this) }
    private val viewModelFirebase by viewModels<AuthViewModel>()
    private val viewModel by viewModels<CartViewModel>()
    private var globalTotal = 0.00

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        with(binding) {

            rvCart.adapter = cartAdapter

            ivDelete.setOnClickListener {

                val alertDialog = AlertDialog.Builder(requireContext())

                alertDialog.setTitle("Deleting Cart")
                alertDialog.setMessage("Do you want to delete all items ?")
                alertDialog.setPositiveButton("Yes") { _, _ ->
                    val clearCartRequest = ClearCartRequest(viewModelFirebase.currentUser!!.uid)
                    viewModel.clearCart(clearCartRequest)
                    viewModel.getCartProducts(viewModelFirebase.currentUser!!.uid)
                }
                alertDialog.setNegativeButton("No") { _, _ ->
                    alertDialog.setCancelable(true)
                }
                alertDialog.show()
            }

            buttonOrder.setOnClickListener {
                initPayment()
//                findNavController().navigate(CartFragmentDirections.cartToPayment())
            }

        }

        initAdapter()
        observeData()

    }


    private fun initPayment() {
        val activity: Activity = requireActivity()
        val co = Checkout()

        try {
            val options = JSONObject()
            options.put("name","Karan Gupta Coder")
            options.put("description","Demoing Charges")
            //You can omit the image option to fetch the image from the dashboard
            options.put("image","https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg")
            options.put("theme.color", "#3399cc");
            options.put("currency","INR");
//            options.put("order_id", "order_DBJOWzybf0sJbb");
            options.put("amount","${globalTotal*100}")//pass amount in currency subunits

            val retryObj = JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 4);
            options.put("retry", retryObj);

            val prefill = JSONObject()
            prefill.put("email","karansuggu@gmail.com")
            prefill.put("contact","7307990967")

            options.put("prefill",prefill)
            co.open(activity,options)
        }catch (e: Exception){
            Toast.makeText(activity,"Error in payment: "+ e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(p0: String?, p1: PaymentData?) {
        Toast.makeText(requireContext(), "Payment succeed", Toast.LENGTH_SHORT).show()
    }

    override fun onPaymentError(p0: Int, p1: String?, p2: PaymentData?) {
        Toast.makeText(requireContext(), "${p1.toString()}", Toast.LENGTH_SHORT).show()
    }


    private fun initAdapter() {
        with(viewModel) {
            getCartProducts(viewModelFirebase.currentUser!!.uid)
            totalAmount.observe(viewLifecycleOwner) { total ->
                globalTotal = total
                binding.tvTotal.text = String.format("₹%.2f", total)
            }
        }
    }


    private fun observeData() {
        viewModel.cartState.observe(viewLifecycleOwner) { state ->

            when (state) {

                CartState.Loading -> {
                    binding.progressBarCart.visible()
                }

                is CartState.CartList -> {
                    binding.rvCart.visible()
                    cartAdapter.submitList(state.products)
                    binding.progressBarCart.invisible()
                }

                is CartState.PostResponse -> {
                    binding.progressBarCart.invisible()
                    Toast.makeText(
                        requireContext(),
                        state.crud.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is CartState.Error -> {
                    binding.rvCart.invisible()
                    Toast.makeText(
                        requireContext(),
                        state.throwable.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBarCart.invisible()
                }

            }

        }
    }

    override fun onCartClick(id: Int) {
        val action = CartFragmentDirections.cartToDetail(id)
        findNavController().navigate(action)
    }

    override fun onDeleteItemClick(id: Int) {
        viewModel.deleteFromCart(id)
        viewModel.getCartProducts(viewModelFirebase.currentUser!!.uid)
    }

    override fun onIncreaseItemClick(price: Double) {
        viewModel.increase(price)
    }

    override fun onDecreaseItemClick(price: Double) {
        viewModel.decrease(price)
    }


}