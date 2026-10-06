package sk.ainet.examples.smarthome.screenshots

import sk.ainet.examples.smarthome.ui.Destination

/**
 * One screen of the listing. Stores display screenshots in upload order, so [order] is the running order and must
 * match the marketing captions. [skipStores] reflects what a store's layout already shows or its release lacks —
 * a shot never renders UI that is pointless or absent there.
 */
data class Shot(
    val order: Int,
    val name: String,
    val destination: Destination,
    val skipStores: Set<StoreTarget> = emptySet(),
) {
    val fileName: String get() = order.toString().padStart(2, '0') + "_" + name + ".png"
}

object Shots {
    val all: List<Shot> = listOf(
        Shot(1, "home", Destination.HOME),
        // The wide gallery layout shows the pipeline beside the home already, so a separate pipeline shot would
        // duplicate 01_home there.
        Shot(2, "pipeline", Destination.PIPELINE, skipStores = setOf(StoreTarget.GALLERY)),
        Shot(3, "cartridges", Destination.CARTRIDGES),
    )
}
