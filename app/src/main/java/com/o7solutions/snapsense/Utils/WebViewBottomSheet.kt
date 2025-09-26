package com.o7solutions.snapsense.Utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.o7solutions.snapsense.R

class WebViewBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_URL = "arg_url"

        fun newInstance(url: String): WebViewBottomSheet {
            val fragment = WebViewBottomSheet()
            val bundle = Bundle()
            bundle.putString(ARG_URL, url)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val url = arguments?.getString(ARG_URL) ?: "https://www.google.com"
        val webView = view.findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true

        webView.settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"



        // THIS IS THE IMPORTANT PART:
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false // Stay inside the WebView
            }


        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
            ): Boolean {
                val newWebView = WebView(requireContext())
                newWebView.webViewClient = webView.webViewClient
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }
        }

        webView.loadUrl(url)
    }
}
