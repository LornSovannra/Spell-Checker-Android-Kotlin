package com.anonymous.spellcheckerapp.models

data class Suggestion(
    val offset: Int,
    val length: Int,
    val misspellWord: String,
    val suggestions: List<String>,
)