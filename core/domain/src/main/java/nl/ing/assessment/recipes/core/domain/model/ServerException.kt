package nl.ing.assessment.recipes.core.domain.model

class ServerException(
    val code: Int,
    override val message: String?
) : RuntimeException(message)
