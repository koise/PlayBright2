package com.example.playbright.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.playbright.data.model.MatchingPairResponse
import com.example.playbright.data.network.ApiErrorHandler
import com.example.playbright.data.repository.ApiRepository
import kotlinx.coroutines.launch

class MatchingPairsViewModel : ViewModel() {

    private val repository = ApiRepository()
    private var MODULE_ID = "matching-pairs" // Default module ID
    
    fun setModuleId(moduleId: String) {
        MODULE_ID = moduleId
    }
    
    fun getModuleId(): String = MODULE_ID

    private val _pairs = MutableLiveData<List<MatchingPairResponse>>()
    val pairs: LiveData<List<MatchingPairResponse>> = _pairs

    private val _currentPairs = MutableLiveData<List<MatchingPairResponse>>(emptyList())
    val currentPairs: LiveData<List<MatchingPairResponse>> = _currentPairs

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _matchedPairs = MutableLiveData<Set<String>>(emptySet())
    val matchedPairs: LiveData<Set<String>> = _matchedPairs

    private val _availableCategories = MutableLiveData<List<String>>(emptyList())
    val availableCategories: LiveData<List<String>> = _availableCategories

    private val _selectedCategory = MutableLiveData<String?>()
    val selectedCategory: LiveData<String?> = _selectedCategory

    private val matchedPairsSet = mutableSetOf<String>()

    fun loadPairs() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                android.util.Log.d("MatchingPairsViewModel", "Loading pairs for module: $MODULE_ID")
                val response = repository.getMatchingPairs(
                    moduleId = MODULE_ID,
                    status = "Active"
                )

                android.util.Log.d("MatchingPairsViewModel", "Response status: ${response.isSuccessful}, Code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val pairsList = response.body() ?: emptyList()
                    android.util.Log.d("MatchingPairsViewModel", "Loaded ${pairsList.size} pairs from backend")
                    
                    _pairs.value = pairsList

                    if (pairsList.isNotEmpty()) {
                        // Extract unique categories from pairs
                        val categories = pairsList.mapNotNull { it.pairType }
                            .distinct()
                            .sorted()
                        _availableCategories.value = categories
                        android.util.Log.d("MatchingPairsViewModel", "Found ${categories.size} categories: $categories")
                        android.util.Log.d("MatchingPairsViewModel", "✅ Pairs loaded successfully")
                    } else {
                        android.util.Log.w("MatchingPairsViewModel", "⚠️ No pairs found for module: $MODULE_ID")
                        _error.value = "No matching pairs available for this module. Please add pairs in the admin panel."
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    android.util.Log.e("MatchingPairsViewModel", "❌ Failed to load pairs: ${response.code()} - $errorBody")
                    _error.value = "Failed to load pairs: $errorBody"
                }
            } catch (e: Exception) {
                android.util.Log.e("MatchingPairsViewModel", "❌ Exception loading pairs: ${e.message}", e)
                val errorMessage = ApiErrorHandler.handleError(e)
                _error.value = errorMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        android.util.Log.d("MatchingPairsViewModel", "Category selected: $category")
        loadRandomPairs()
    }

    fun loadRandomPairs() {
        val allPairs = _pairs.value ?: emptyList()
        val selectedCat = _selectedCategory.value
        
        // Filter pairs by selected category
        val filteredPairs = if (selectedCat != null) {
            allPairs.filter { it.pairType == selectedCat }
        } else {
            allPairs
        }
        
        android.util.Log.d("MatchingPairsViewModel", "Loading pairs for category: $selectedCat, filtered count: ${filteredPairs.size}")
        
        if (filteredPairs.size >= 4) {
            // Select 4 random pairs
            val randomPairs = filteredPairs.shuffled().take(4)
            _currentPairs.value = randomPairs
            resetMatches()
        } else if (filteredPairs.isNotEmpty()) {
            // Use all available pairs if less than 4
            _currentPairs.value = filteredPairs.shuffled()
            resetMatches()
        } else {
            android.util.Log.w("MatchingPairsViewModel", "No pairs available for category: $selectedCat")
            _error.value = "No pairs available for selected category."
        }
    }

    fun addMatch(pairId: String) {
        matchedPairsSet.add(pairId)
        _matchedPairs.value = matchedPairsSet.toSet()
    }

    fun removeMatch(pairId: String) {
        matchedPairsSet.remove(pairId)
        _matchedPairs.value = matchedPairsSet.toSet()
    }

    fun isMatched(pairId: String): Boolean {
        return matchedPairsSet.contains(pairId)
    }

    fun resetMatches() {
        matchedPairsSet.clear()
        _matchedPairs.value = emptySet()
    }

    fun areAllMatched(): Boolean {
        val current = _currentPairs.value ?: emptyList()
        return current.isNotEmpty() && matchedPairsSet.size == current.size
    }

    fun getPairById(pairId: String): MatchingPairResponse? {
        return _currentPairs.value?.find { it.id == pairId }
    }
}


