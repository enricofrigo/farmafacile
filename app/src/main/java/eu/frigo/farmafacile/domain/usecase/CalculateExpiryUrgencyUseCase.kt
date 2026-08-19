package eu.frigo.farmafacile.domain.usecase

import eu.frigo.farmafacile.core.utils.ExpiryStatus
import eu.frigo.farmafacile.core.utils.ExpiryUrgencyCalculator
import java.time.LocalDate
import javax.inject.Inject

class CalculateExpiryUrgencyUseCase @Inject constructor() {
    operator fun invoke(expiryDate: LocalDate?, referenceDate: LocalDate = LocalDate.now()): ExpiryStatus {
        return ExpiryUrgencyCalculator.calculate(expiryDate, referenceDate)
    }
}
