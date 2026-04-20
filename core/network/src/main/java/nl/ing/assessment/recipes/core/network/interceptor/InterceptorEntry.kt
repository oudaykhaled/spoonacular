package nl.ing.assessment.recipes.core.network.interceptor

import okhttp3.Interceptor

enum class InterceptorType { APPLICATION, NETWORK }

data class InterceptorEntry(
    val order: Int,
    val interceptor: Interceptor,
    val type: InterceptorType = InterceptorType.APPLICATION
) : Comparable<InterceptorEntry> {
    override fun compareTo(other: InterceptorEntry): Int = order.compareTo(other.order)
}
