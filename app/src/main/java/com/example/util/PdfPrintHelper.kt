package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

object PdfPrintHelper {
    fun printHtml(context: Context, htmlContent: String, jobName: String = "MCQ_Builder_Document") {
        val webView = WebView(context)
        
        // Enable settings to render local files and support MathJax
        webView.settings.apply {
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                val builder = PrintAttributes.Builder()
                builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                printManager.print(jobName, printAdapter, builder.build())
            }
        }
        
        // Load content using base URL of file:/// to allow absolute file:/// path resolution
        webView.loadDataWithBaseURL("file:///", htmlContent, "text/html", "UTF-8", null)
    }
}
