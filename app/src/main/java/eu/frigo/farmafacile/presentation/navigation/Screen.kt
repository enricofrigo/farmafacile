package eu.frigo.farmafacile.presentation.navigation

sealed class Screen(val route: String) {
    data object Lists : Screen("lists")
    data object ListDetail : Screen("list_detail/{listId}") {
        fun createRoute(listId: String) = "list_detail/$listId"
    }
    data object Scanner : Screen("scanner/{listId}") {
        fun createRoute(listId: String) = "scanner/$listId"
    }
    data object AddEditMedicine : Screen("add_edit/{listId}?medicineId={medicineId}&aic={aic}&expiry={expiry}&lot={lot}&serial={serial}") {
        fun createRoute(
            listId: String,
            medicineId: String? = null,
            aic: String? = null,
            expiry: String? = null,
            lot: String? = null,
            serial: String? = null
        ): String {
            val builder = StringBuilder("add_edit/$listId?")
            medicineId?.let { builder.append("medicineId=$it&") }
            aic?.let { builder.append("aic=$it&") }
            expiry?.let { builder.append("expiry=$it&") }
            lot?.let { builder.append("lot=$it&") }
            serial?.let { builder.append("serial=$it&") }
            return builder.toString().trimEnd('&', '?')
        }
    }
    data object Dosage : Screen("dosage")
    data object Settings : Screen("settings")
    data object SyncLogs : Screen("sync_logs/{listId}") {
        fun createRoute(listId: String) = "sync_logs/$listId"
    }
}
