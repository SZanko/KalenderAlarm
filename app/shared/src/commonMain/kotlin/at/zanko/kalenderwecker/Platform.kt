package at.zanko.kalenderwecker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform