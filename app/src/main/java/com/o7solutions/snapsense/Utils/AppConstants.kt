package com.o7solutions.snapsense.Utils

object AppConstants {

    val prompt2 = """
Analyze the product in the image and provide the following information:

1. **Company Identification** – Identify the company that manufactures this product.
2. **Product Details** – Provide all available details about this product, including whether it is a new release or an existing product in the market.
3. **Alternative Recommendations** – If this product or company is not suitable, suggest other comparable products or companies in the same category.
4. **Product Health & Market Insights** – Evaluate the product’s market health, including popularity, customer reception, and potential risks.
5. **Suggestions & Improvements** – Provide actionable suggestions for improvement, usage, or alternatives.

Focus on delivering detailed, insightful, and actionable information for both the product and the company.
""".trimIndent()

    val prompt = """
You are working as a personal assistant. Analyze the image and give me detailed information about image.Detect the product in image than i want you to give me proper information of the product like company,
 model details. You have to give company details anyway. if not possible suggest me other similar companies. Give me condition details of object, suggestions to improve the condition and also give an random
  fact or funny fact about the analysis like any famous quote by some famous person, 
  suggest when it is going to be outdated.
 
""".trimIndent()


    var KEY_API = "apiKey"
    var PREFS_NAME = "App_prefs"
    var keyCol = "keys"

    var unsplashKey = "accessKey"


}