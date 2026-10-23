package app.lokey0905.location.catalog

/** Stable identities shared by the Pikmin list and supplementary game-page cards. */
enum class CatalogApp(val section: CatalogSection?) {
    PIKMIN(null),
    JOYSTICK(null),
    DEFIT(CatalogSection.STEPS),
    FITNESS(CatalogSection.STEPS),
    GOOGLE_FIT(CatalogSection.HEALTH),
    HEALTH_CONNECT(CatalogSection.HEALTH),
    APKMIRROR_INSTALLER(CatalogSection.INSTALLATION)
}

enum class CatalogSection {
    STEPS,
    HEALTH,
    INSTALLATION
}
