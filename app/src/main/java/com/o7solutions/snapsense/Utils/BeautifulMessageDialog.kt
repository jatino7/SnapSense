package com.o7solutions.snapsense.Utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import com.o7solutions.snapsense.R

class BeautifulMessageDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val onOkClick: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var dialogView: View
    private lateinit var tvTitle: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnOk: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove default dialog styling
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Inflate custom layout
        dialogView = LayoutInflater.from(context).inflate(R.layout.beautiful_dialog, null)
        setContentView(dialogView)

        initializeViews()
        setupClickListeners()
        setupWindowProperties()
        startEntranceAnimation()
    }

    private fun initializeViews() {
        tvTitle = dialogView.findViewById(R.id.tvTitle)
        tvMessage = dialogView.findViewById(R.id.tvMessage)
        btnOk = dialogView.findViewById(R.id.btnOk)

        // Set content
        tvTitle.text = title
        tvMessage.text = message
    }

    private fun setupClickListeners() {
        btnOk.setOnClickListener {
            animateButtonPress(btnOk) {
                onOkClick?.invoke()
                dismissWithAnimation()
            }
        }

        // Dismiss on outside touch (optional)
        setCanceledOnTouchOutside(true)
    }

    private fun setupWindowProperties() {
        window?.apply {
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Center the dialog
            attributes = attributes?.apply {
                verticalMargin = 0.1f
            }
        }
    }

    private fun startEntranceAnimation() {
        dialogView.alpha = 0f
        dialogView.scaleX = 0.7f
        dialogView.scaleY = 0.7f
        dialogView.translationY = 100f

        dialogView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun animateButtonPress(button: View, onComplete: () -> Unit) {
        button.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction(onComplete)
            }
    }

    private fun dismissWithAnimation() {
        dialogView.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .translationY(-50f)
            .setDuration(250)
            .withEndAction {
                dismiss()
            }
    }
}

