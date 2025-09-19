package com.o7solutions.snapsense.Utils

object Chatbot {

    private val KEYWORDS = listOf(
        "O7",
        "web",
        "mobile",
        "app",
        "cloud",
        "digital marketing", // phrase (two words)
        "angular",
        "react",
        "python",
        "ml",
        "machine learning",
        "artificial intelligence",
        "deep learning",
        "ai",
        "datascience",       // single word form
        "data science" ,
        "ui/ux",
        "ui",
        "ux",
        "graphic designing",
        "designing",
        "graphics",
        "full stack",
        "cyber",
        "networking",
        "mean",
        "mern",
        "data analytics",
        "node",
        "seo",
        // spaced form if user writes it separated
    )


    private val KEYWORD_DATA = mapOf(
        "web" to "Web Development",
        "mobile" to "Mobile Development",
        "app" to "Application Solutions",
        "cloud" to "Cloud Computing",
        "digital marketing" to "Digital Marketing Services",
        "angular" to "Angular Framework",
        "react" to "React Framework",
        "python" to "Python Programming",
        "ml" to "Machine Learning",
        "ai" to "Artificial Intelligence",
        "seo" to "Search Engine Optimization"
        // Add more as needed
    )

    // Function that returns all values for keywords found in text
    fun getKeywordValues(text: String): List<String> {
        val lowerText = text.lowercase()
        return KEYWORD_DATA.filter { (keyword, _) ->
            val regex = Regex("\\b${Regex.escape(keyword.lowercase())}\\b", RegexOption.IGNORE_CASE)
            regex.containsMatchIn(lowerText)
        }.values.toList()
    }

    fun findKeywords(text: String): List<String> {
        val lowerText = text.lowercase()
        return KEYWORDS.filter { keyword ->
            val lowerKeyword = keyword.lowercase()
            // Escape regex special chars to handle things like "ui/ux"
            val regex = Regex("\\b${Regex.escape(lowerKeyword)}\\b", RegexOption.IGNORE_CASE)
            regex.containsMatchIn(lowerText)
        }
    }
}