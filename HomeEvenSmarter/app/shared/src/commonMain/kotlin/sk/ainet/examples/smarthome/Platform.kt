package sk.ainet.examples.smarthome

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform