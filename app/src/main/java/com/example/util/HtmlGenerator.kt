package com.example.util

import com.example.data.model.Question
import com.example.data.model.Topic
import com.example.data.prefs.SettingsManager

object HtmlGenerator {

    private val markers = listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ")

    private fun toBengaliNum(num: Int): String {
        val engToBng = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return num.toString().map { engToBng[it] ?: it }.joinToString("")
    }

    private fun escapeHtml(str: String?): String {
        if (str == null) return ""
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    fun generateMcqHtml(
        questions: List<Question>,
        topics: List<Topic>,
        settings: SettingsManager,
        startRange: Int? = null,
        endRange: Int? = null
    ): String {
        val startNum = settings.startNum
        val brandName = settings.brandName
        val fontSize = settings.fontSize
        val watermarkPath = settings.watermarkPath
        val hfEnabled = settings.isHeaderFooterEnabled

        val sb = java.lang.StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html lang="bn">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MCQ Printout</title>
                <script src="https://cdn.tailwindcss.com"></script>
                <link href="https://fonts.googleapis.com/css2?family=Noto+Serif+Bengali:wght@400;500;600;700&display=swap" rel="stylesheet">
                <script>
                    window.MathJax = {
                        tex: {
                            inlineMath: [['$', '$'], ['\\(', '\\)']],
                            displayMath: [['$$', '$$'], ['\\[', '\\]']],
                            processEscapes: true
                        },
                        chtml: {
                            displayAlign: 'left',
                            displayIndent: '0',
                            scale: 1,
                            matchFontHeight: true
                        },
                        options: {
                            enableMenu: false, 
                            skipHtmlTags: ['script', 'noscript', 'style', 'textarea', 'pre']
                        }
                    };
                </script>
                <script id="MathJax-script" async src="https://cdn.jsdelivr.net/npm/mathjax@3.2.2/es5/tex-chtml.js"></script>
                <style>
                    :root {
                        --main-font-size: ${fontSize}pt; 
                        --opt-circle-move: 0px;  
                        --ans-circle-move: 0px;  
                    }
                    * { box-sizing: border-box !important; }
                    body {
                        font-family: 'Noto Serif Bengali', serif;
                        background-color: white;
                        color: black;
                        position: relative; 
                        margin: 0;
                        padding: 0;
                    }
                    .pdf-item, .question-text, .print-option, .answer-box-print, 
                    .opt-marker, .answer-omr-marker, .math-content, .exp-row, .topic-header-box {
                        font-size: var(--main-font-size) !important;
                    }
                    .math-content {
                        width: 100% !important;
                        max-width: 100% !important;
                        overflow-wrap: anywhere !important; 
                        word-wrap: break-word !important; 
                        word-break: break-word !important; 
                        white-space: normal !important; 
                    }
                    .opt-marker {
                        display: inline-block !important;
                        margin-right: 6px !important;
                        flex-shrink: 0 !important;
                        line-height: 1 !important;
                    }
                    .answer-omr-marker {
                        display: inline-block !important;
                        margin-left: 6px !important;
                        line-height: 1 !important;
                    }
                    .view-img {
                        max-width: 100%; 
                        object-fit: contain; object-position: left top;
                        margin-top: 6px; margin-bottom: 6px; display: block; border-radius: 4px;
                    }
                    .topic-header-box {
                        background-color: #e5e7eb !important; 
                        border: 1px solid #000 !important;
                        text-align: center !important;
                        font-weight: bold !important;
                        padding: 4px 8px !important;
                        margin-bottom: 12px !important;
                        width: 100% !important;
                        color: #000 !important;
                        -webkit-print-color-adjust: exact !important;
                        print-color-adjust: exact !important;
                    }
                    #watermark-layer {
                        position: fixed; 
                        top: 50%; left: 50%; transform: translate(-50%, -50%);
                        z-index: 0; pointer-events: none; opacity: 0.08; 
                        display: block !important;
                    }
                    #watermark-layer img { max-width: 500px; max-height: 500px; object-fit: contain; }
                    
                    /* Header & Footer Layout */
                    .custom-hf-wrapper {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    .header-design {
                        display: flex;
                        align-items: stretch;
                        justify-content: space-between;
                        border-bottom: 5px solid #4b5563; 
                        margin-bottom: 12px;
                        width: 100%;
                        background-color: white;
                    }
                    .hd-left-wrap { display: flex; align-items: stretch; }
                    .hd-deco-left {
                        width: 16px;
                        background-color: #4b5563 !important; 
                        clip-path: polygon(0 0, 100% 0, 100% 100%, 30% 100%);
                        margin-right: 4px;
                    }
                    .hd-left {
                        background-color: #d1d5db !important; 
                        color: #000 !important;
                        padding: 6px 30px 6px 15px;
                        clip-path: polygon(0 0, 100% 0, calc(100% - 15px) 100%, 0 100%);
                        font-weight: bold;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        font-size: 14pt !important;
                    }
                    .hd-right {
                        background-color: #4b5563 !important; 
                        color: #fff !important;
                        padding: 6px 15px 6px 25px;
                        clip-path: polygon(15px 0, 100% 0, 100% 100%, 0 100%);
                        font-weight: normal;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        font-size: 12pt !important;
                    }
                    .footer-design {
                        display: flex;
                        align-items: stretch;
                        justify-content: space-between;
                        border-top: 5px solid #4b5563;
                        margin-top: 10px;
                        width: 100%;
                        background-color: white;
                    }
                    .fd-left-wrap { display: flex; align-items: stretch; }
                    .fd-deco-left {
                        width: 16px;
                        background-color: #4b5563 !important; 
                        clip-path: polygon(0 0, 100% 0, 100% 100%, 30% 100%);
                        margin-right: 4px;
                    }
                    .fd-left {
                        background-color: #4b5563 !important; 
                        color: #fff !important;
                        padding: 6px 30px 6px 15px;
                        clip-path: polygon(0 0, 100% 0, calc(100% - 15px) 100%, 0 100%);
                        font-weight: bold;
                        display: flex;
                        align-items: center;
                        gap: 8px;
                        font-size: 10pt !important;
                    }
                    .fd-right {
                        color: #000;
                        font-weight: bold;
                        font-size: 9pt !important;
                        padding-right: 15px;
                        display: flex;
                        align-items: center;
                    }

                    @media print {
                        @page { margin: 8mm 0mm 8mm 0mm; size: A4 portrait; }
                        body { background-color: white !important; color: #000 !important; }
                        
                        .print-fixed-header { position: fixed; top: 0; left: 0; width: 100%; z-index: 1000; }
                        .print-fixed-footer { position: fixed; bottom: 0; left: 0; width: 100%; z-index: 1000; }
                        
                        .spacer-header { display: block !important; height: 16mm; }
                        .spacer-footer { display: block !important; height: 16mm; }

                        #mcq-list { 
                            column-count: 2; 
                            column-gap: 8mm; 
                            column-rule: 1px solid #d1d5db; 
                            display: block !important; 
                            width: 100% !important; 
                            padding: 0 4mm !important; 
                        }
                        .pdf-item { 
                            break-inside: avoid; 
                            page-break-inside: avoid; 
                            margin-top: 0 !important; 
                            margin-bottom: 12px !important; 
                            border: none !important; 
                            padding: 0 !important; 
                            display: block; 
                            overflow: hidden; 
                            width: 100% !important; 
                            padding-right: 4px !important; 
                            max-width: 100% !important; 
                        }
                        .question-text { font-weight: normal !important; line-height: 1.3 !important; margin-bottom: 5px !important; }
                        .print-options-container { display: flex !important; flex-direction: column !important; gap: 3px !important; padding-left: 8px !important; width: 100% !important; }
                        .print-option { display: flex !important; flex-direction: row !important; align-items: flex-start !important; border: none !important; padding: 0 !important; line-height: 1.3 !important; width: 100% !important; }
                        .opt-content { flex: 1 !important; display: flex !important; flex-direction: column !important; min-width: 0 !important; }
                        
                        .answer-box-print { display: block !important; border: 1px solid #000 !important; border-radius: 4px !important; margin-top: 5px !important; margin-left: 8px !important; break-inside: avoid !important; color: #000 !important; width: calc(100% - 8px) !important; max-width: calc(100% - 8px) !important; }
                        .ans-top-row { display: flex !important; flex-direction: row !important; align-items: stretch !important; width: 100% !important; }
                        .ans-top-row.has-exp { border-bottom: 1px solid #000 !important; }
                        .brand-side-print { display: flex !important; align-items: center !important; justify-content: center !important; padding: 3px 8px !important; border-right: 1px solid #000 !important; font-weight: normal !important; color: #000 !important; white-space: nowrap !important; }
                        .ans-col { display: block !important; flex: 1 !important; padding: 3px 8px !important; min-width: 0 !important; width: 100% !important; }
                        .answer-title-line { display: block !important; font-weight: normal !important; color: #000 !important; }
                        .exp-row { display: block !important; padding: 4px 10px 4px 8px !important; font-weight: normal !important; color: #000 !important; width: 100% !important; }
                    }
                    
                    /* Standard Screen preview style */
                    #mcq-list { 
                        column-count: 2; 
                        column-gap: 8mm; 
                        column-rule: 1px solid #d1d5db; 
                        padding: 10px; 
                    }
                    .pdf-item {
                        margin-bottom: 15px;
                        border-bottom: 1px solid #e5e7eb;
                        padding-bottom: 10px;
                        break-inside: avoid;
                    }
                    .print-options-container {
                        padding-left: 10px;
                    }
                    .print-option {
                        display: flex;
                        align-items: flex-start;
                        padding: 2px 0;
                    }
                    .answer-box-print {
                        border: 1px solid #000;
                        border-radius: 4px;
                        margin-top: 5px;
                        margin-left: 8px;
                    }
                    .ans-top-row {
                        display: flex;
                        align-items: stretch;
                    }
                    .ans-top-row.has-exp {
                        border-bottom: 1px solid #000;
                    }
                    .brand-side-print {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 3px 8px;
                        border-right: 1px solid #000;
                        font-weight: bold;
                    }
                    .ans-col {
                        padding: 3px 8px;
                        flex: 1;
                    }
                    .exp-row {
                        padding: 4px 10px 4px 8px;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        // Watermark layer
        if (!watermarkPath.isNullOrEmpty()) {
            sb.append("""
                <div id="watermark-layer">
                    <img src="file://$watermarkPath" alt="Watermark">
                </div>
            """.trimIndent())
        }

        // Table Wrapper for repeating Header/Footer on every printed page
        sb.append("<table class=\"custom-hf-wrapper\">")

        // Printed Header
        if (hfEnabled) {
            sb.append("""
                <thead>
                    <tr>
                        <td>
                            <div class="print-fixed-header">
                                <div class="header-design">
                                    <div class="hd-left-wrap">
                                        <div class="hd-deco-left"></div>
                                        <div class="hd-left">
                                            <span id="lbl-header-left">${escapeHtml(settings.headerLeft)}</span>
                                        </div>
                                    </div>
                                    <div class="hd-right">
                                        <span id="lbl-header-right">${escapeHtml(settings.headerRight)}</span>
                                    </div>
                                </div>
                            </div>
                            <div class="spacer-header"></div>
                        </td>
                    </tr>
                </thead>
            """.trimIndent())
        }

        // Body with MCQ Content
        sb.append("<tbody><tr><td><div id=\"mcq-list\">")

        questions.forEachIndexed { index, item ->
            val currentQuestionNum = startNum + index
            
            // Apply range filters if present
            if (startRange != null && currentQuestionNum < startRange) return@forEachIndexed
            if (endRange != null && currentQuestionNum > endRange) return@forEachIndexed

            val bengaliNum = toBengaliNum(currentQuestionNum)

            // Look for matching topic
            val matchedTopic = topics.find { t -> t.questionNum == currentQuestionNum }
            if (matchedTopic != null) {
                sb.append("<div class=\"topic-header-box font-serif\">${escapeHtml(matchedTopic.title)}</div>")
            }

            val qImgStyle = "max-height: ${item.qImgSize}px;"
            val optImgStyle = "max-height: ${item.optImgSize}px;"
            val expImgStyle = "max-height: ${item.expImgSize}px;"

            sb.append("""
                <div class="pdf-item">
                    <div class="question-text flex flex-col items-start font-normal">
                        <div class="flex items-start w-full">
                            <span class="mr-1 flex-shrink-0">$bengaliNum.</span>
                            <div class="math-content flex-1">${item.questionText.replace("\n", "<br>")}</div>
                        </div>
                        ${if (!item.qImage.isNullOrEmpty()) "<img src=\"file://${item.qImage}\" class=\"view-img\" style=\"$qImgStyle\">" else ""}
                    </div>
                    <div class="print-options-container">
            """.trimIndent())

            val correctMarkersList = mutableListOf<String>()
            item.options.forEachIndexed { oIndex, opt ->
                val isCorrect = item.correctIndices.contains(oIndex)
                val rawMarker = markers.getOrNull(oIndex) ?: "${oIndex + 1}"
                if (isCorrect && !item.isNoAnswer) {
                    correctMarkersList.add(rawMarker)
                }

                sb.append("""
                    <div class="print-option">
                        <span class="opt-marker font-bold">$rawMarker</span> 
                        <div class="opt-content">
                            ${if (opt.text.isNotEmpty()) "<div class=\"math-content\">${escapeHtml(opt.text)}</div>" else ""}
                            ${if (!opt.image.isNullOrEmpty()) "<img src=\"file://${opt.image}\" class=\"view-img\" style=\"$optImgStyle\">" else ""}
                        </div>
                    </div>
                """.trimIndent())
            }

            sb.append("</div>") // Close print-options-container

            val correctMarkerText = if (item.isNoAnswer) "উত্তর নেই" else correctMarkersList.joinToString(", ")
            val hasExp = !item.explanation.isNullOrEmpty() || !item.expImage.isNullOrEmpty()

            val brandSectionPrint = if (brandName.isNotEmpty()) {
                "<div class=\"brand-side-print\">${escapeHtml(brandName)}</div>"
            } else ""

            sb.append("""
                <div class="answer-box-print">
                    <div class="ans-top-row ${if (hasExp) "has-exp" else ""}">
                        $brandSectionPrint
                        <div class="ans-col">
                            <div class="answer-title-line">
                                <span>উত্তর:</span>
                                <span class="answer-omr-marker ml-1 font-bold">$correctMarkerText</span>
                            </div>
                        </div>
                    </div>
            """.trimIndent())

            if (hasExp) {
                sb.append("<div class=\"exp-row\">")
                if (!item.explanation.isNullOrEmpty()) {
                    sb.append("<span class=\"math-content\">${item.explanation.replace("\n", "<br>")}</span>")
                }
                if (!item.expImage.isNullOrEmpty()) {
                    sb.append("<img src=\"file://${item.expImage}\" class=\"view-img\" style=\"$expImgStyle\">")
                }
                sb.append("</div>")
            }

            sb.append("</div>") // Close answer-box-print
            sb.append("</div>") // Close pdf-item
        }

        sb.append("</div></td></tr></tbody>")

        // Printed Footer
        if (hfEnabled) {
            sb.append("""
                <tfoot>
                    <tr>
                        <td>
                            <div class="spacer-footer"></div>
                            <div class="print-fixed-footer">
                                <div class="footer-design">
                                    <div class="fd-left-wrap">
                                        <div class="fd-deco-left"></div>
                                        <div class="fd-left">
                                            <span>${escapeHtml(settings.footerLeft)}</span>
                                        </div>
                                    </div>
                                    <div class="fd-right">
                                        <span>${escapeHtml(settings.footerRight)}</span>
                                    </div>
                                </div>
                            </div>
                        </td>
                    </tr>
                </tfoot>
            """.trimIndent())
        }

        sb.append("</table>")
        sb.append("</body></html>")

        return sb.toString()
    }
}
