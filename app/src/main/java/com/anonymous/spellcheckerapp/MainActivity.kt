package com.anonymous.spellcheckerapp

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.anonymous.spellcheckerapp.databinding.ActivityMainBinding
import com.anonymous.spellcheckerapp.models.Suggestion

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), SpellCheckerSession.SpellCheckerSessionListener {
    private lateinit var binding: ActivityMainBinding
    private var spellCheckerSession: SpellCheckerSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setUpEventListeners()
        setUpSpellCheckerSession()
    }

    private fun setUpSpellCheckerSession(){
        val textServicesManager = getSystemService(TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        spellCheckerSession = textServicesManager.newSpellCheckerSession(null, null, this, true)
    }

    private fun checkSpelling(inputText: String) {
        if (TextUtils.isEmpty(inputText)) {
            Log.d(TAG, "INPUT TEXT IS EMPTY")
            return
        }

        spellCheckerSession?.getSuggestions(TextInfo(inputText), 100)
    }

    private fun checkSentence(inputText: String) {
        if (inputText.isBlank()) {
            Log.d(TAG, "INPUT TEXT IS EMPTY")

            binding.tvCorrectSentence.text = getString(R.string.no_suggestion_available)
            return
        }

        spellCheckerSession?.getSentenceSuggestions(arrayOf(TextInfo(inputText)), 5)
    }

    private fun replaceWordCaseSensitive(input: String, target: String, replacement: String): String {
        return input.replace(target, replacement)
    }

    private fun setUpEventListeners(){
        binding.apply {
            btnGetSuggestion.setOnClickListener {
//                checkSpelling(edUserInput.text.toString())
                checkSentence(edUserInput.text.toString())
            }
        }
    }

    override fun onGetSuggestions(suggestionsInfoArray: Array<out SuggestionsInfo>?) {
        if (suggestionsInfoArray == null) return

        for (suggestionsInfo in suggestionsInfoArray) {
            val word = suggestionsInfo.suggestionsCount

            if (word > 0) {
                val suggestions = (0 until word).map { index ->
                    suggestionsInfo.getSuggestionAt(index)
                }

                Log.d(TAG, "SUGGESTIONS: $suggestions")
            } else {
                Log.d(TAG, "NO SUGGESTIONS AVAILABLE")
            }
        }
    }

    override fun onGetSentenceSuggestions(suggestionsInfos: Array<out SentenceSuggestionsInfo>?) {
        if (suggestionsInfos == null) return

        val suggestionList = mutableListOf<Suggestion>()

        for (sentenceSuggestionsInfo in suggestionsInfos) {
            for (i in 0 until sentenceSuggestionsInfo.suggestionsCount) {
                val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(i)
                val offset = sentenceSuggestionsInfo.getOffsetAt(i)
                val length = sentenceSuggestionsInfo.getLengthAt(i)
                val misspellWord = binding.edUserInput.text.substring(offset, offset + length)

                if (suggestionsInfo != null && suggestionsInfo.suggestionsCount > 0) {
                    val suggestions = (0 until suggestionsInfo.suggestionsCount).map { index ->
                        suggestionsInfo.getSuggestionAt(index)
                    }

                    suggestionList.add(Suggestion(offset, length, misspellWord, suggestions))
                } else {
                    Log.d(TAG, "NO SUGGESTIONS FOR WORD AT OFFSET: $offset")
                }
            }
        }

//        Log.d(TAG, "SUGGESTION LIST: $suggestionList")

        if(suggestionList.isEmpty()){
            binding.tvCorrectSentence.text = getString(R.string.no_suggestion_available)

            return
        }

        var fullSentence = binding.edUserInput.text.toString()

        for (seg in suggestionList){
            fullSentence = replaceWordCaseSensitive(fullSentence, seg.misspellWord, seg.suggestions[0])
        }

        binding.tvCorrectSentence.text = fullSentence
    }

    override fun onDestroy() {
        super.onDestroy()
        spellCheckerSession?.close()
    }
}