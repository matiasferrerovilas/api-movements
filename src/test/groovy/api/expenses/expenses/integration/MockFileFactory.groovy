package api.expenses.expenses.integration

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile

class MockFileFactory {
    static MockMultipartFile resumenGaliciaMock() {
        def doc = new PDDocument()
        def page = new PDPage()
        doc.addPage(page)

        def content = new PDPageContentStream(doc, page)
        content.beginText()

        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12)

        content.newLineAtOffset(50, 750)

        def lines = [
                'Resumen N° VI00000000050818488',
                'Tarjeta Crédito VISA',
                'VILAS MATIAS GABRIEL FERRERO   Consumidor Final CUIT Banco: 30-50000173-5',
                'PJE PERRAULT 1240, CIUDAD AUTONOMA BUEN, C1406EMD  N° Cuenta: 1250815754 Sucursal: 133',
                'Resumen de tarjeta de credito VISA',
                'DETALLE DEL CONSUMO',
                'FECHA REFERENCIA CUOTA COMPROBANTE PESOS DÓLARES',
                '18-03-25 * LAZARO CABALLITO 06/06 000835 16.316,66',
                '14-06-25 * MAMUSCHKA 03/03 506435 5.750,00',
                '04-07-25 * DISCO SM 247 02/03 277396 4.708,33',
                '30-07-25 * MC DONALDS CABALLITO 047153 7.199,00',
                '02-08-25 E CULTO TEMP                USD       14,86 855642 14,86',
                '02-08-25 E AUTOGRILL 7202            EUR        9,50 364413 11,13',
                '02-08-25 E UBER   *TRIP              EUR       41,04 872073 48,08',
                "*USTED DISPONE DE 30 DIAS DESDE LA RECEPCION PARA CUESTIONAR ESTE RESUMEN*"
        ]

        lines.each { line ->
            content.showText(line)
            content.newLineAtOffset(0, -12)
        }

        content.endText()
        content.close()

        def output = new ByteArrayOutputStream()
        doc.save(output)
        doc.close()

        return new MockMultipartFile(
                "file",
                "GALICIA_MOCK_0325_GASTOS_USD.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                output.toByteArray()
        )
    }

}
