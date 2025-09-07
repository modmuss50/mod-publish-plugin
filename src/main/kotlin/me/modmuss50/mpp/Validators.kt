package me.modmuss50.mpp

import org.gradle.api.provider.ListProperty

object Validators {
    fun <T : Any> validateUnique(name: String, listProp: ListProperty<T>) {
        val duplicates = listProp.get().groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw IllegalArgumentException("$name contains duplicate values: $duplicates")
        }
    }
}
