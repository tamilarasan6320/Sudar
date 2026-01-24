package com.sudar.tnpscapp.nativeapp.feature.tests

/**
 * Singleton holder for passing test result data between screens
 * This avoids complex serialization for navigation
 */
object TestResultHolder {
    var resultData: TestResultData? = null
    
    fun set(data: TestResultData) {
        resultData = data
    }
    
    fun get(): TestResultData? = resultData
    
    fun clear() {
        resultData = null
    }
}
