package api.m2.movements.unit.services

import api.m2.movements.services.category.rules.MerchantKeyNormalizer
import spock.lang.Specification

class MerchantKeyNormalizerTest extends Specification {

    MerchantKeyNormalizer normalizer = new MerchantKeyNormalizer()

    def "keyOf - should normalize '#description' to '#expected'"() {
        expect:
        normalizer.keyOf(description) == expected

        where:
        description                                          || expected
        "Mercadona Valencia 1234"                            || "MERCADONA VALENCIA"
        "MERCADONA, VALENCIA"                                || "MERCADONA VALENCIA"
        "Café de la Esquina S.L."                            || "CAFE ESQUINA"
        "COMPRA LIDL 0456 MADRID ES"                         || "LIDL MADRID"
        "Amazon.es*AB12CD"                                   || "AMAZON AB12CD"
        "Bizum De Ana Garcia"                                || "BIZUM ANA GARCIA"
        "Transferencia Inmediata A Favor De Fulano De Tal"   || "TRANSFERENCIA INMEDIATA FAVOR FULANO TAL"
        "Recibo O2 Fibra"                                    || "RECIBO O2 FIBRA"
        "1234 5678"                                          || ""
        "   "                                                || ""
        null                                                 || ""
    }

    def "headOf - should return the first significant word unless the key starts with a generic one"() {
        expect:
        normalizer.headOf("Lidl Valencia") == "LIDL"
        normalizer.headOf("Bizum De Ana") == ""
        normalizer.headOf(null) == ""
    }

    def "keyOf - should never exceed the column length"() {
        given:
        def longWord = "A" * 50

        expect:
        normalizer.keyOf("${longWord} ${longWord}").length() <= MerchantKeyNormalizer.MAX_KEY_LENGTH
    }
}
