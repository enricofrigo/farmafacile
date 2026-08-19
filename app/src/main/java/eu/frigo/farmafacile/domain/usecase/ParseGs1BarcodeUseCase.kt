package eu.frigo.farmafacile.domain.usecase

import eu.frigo.farmafacile.core.gs1.Gs1BarcodeData
import eu.frigo.farmafacile.core.gs1.Gs1DataMatrixParser
import javax.inject.Inject

class ParseGs1BarcodeUseCase @Inject constructor(
    private val parser: Gs1DataMatrixParser
) {
    operator fun invoke(rawBarcode: String?): Gs1BarcodeData {
        return parser.parse(rawBarcode)
    }
}
