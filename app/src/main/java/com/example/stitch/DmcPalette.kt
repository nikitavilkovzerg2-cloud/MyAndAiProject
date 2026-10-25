package com.example.stitch

import kotlin.math.pow

data class DmcColor(
    val code: String, val name: String, val hex: String,
    val r: Int, val g: Int, val b: Int, val lab: FloatArray
)

fun srgb2lin(c: Float): Float {
    val n = c / 255f
    return if (n <= 0.04045f) n / 12.92f else ((n + 0.055f) / 1.055f).pow(2.4f)
}

fun rgb2lab(r: Int, g: Int, b: Int): FloatArray {
    val R = srgb2lin(r.toFloat()); val G = srgb2lin(g.toFloat()); val B = srgb2lin(b.toFloat())
    var X = (R*0.4124f + G*0.3576f + B*0.1805f) / 0.95047f
    var Y = (R*0.2126f + G*0.7152f + B*0.0722f) / 1.00000f
    var Z = (R*0.0193f + G*0.1192f + B*0.9505f) / 1.08883f
    X = fx(X); Y = fx(Y); Z = fx(Z)
    return floatArrayOf(116f*Y - 16f, 500f*(X - Y), 200f*(Y - Z))
}

private fun fx(t: Float): Float =
    if (t > 0.008856f) t.pow(1f/3f) else (7.787f*t + 16f/116f)

object DmcPalette : PaletteProvider {
    private val RAW = """
310|Black|000000
Blanc|White|FFFFFF
Ecru|Ecru|F0EADA
3865|Winter White|F9F2E7
746|Off White|FCF6E8
307|Lemon|FDFD63
744|Pale Yellow|FFE9A6
445|Lemon Lt|FFFDB8
973|Canary Bright|FFE300
444|Lemon Dk|FFD600
726|Topaz Lt|FFD67E
725|Topaz Med Lt|FFC840
972|Canary Deep|FFB515
971|Pumpkin|FF7D22
947|Burnt Orange|FF5500
900|Burnt Orange Dk|D55A00
606|Bright Orange Red|FF3300
666|Bright Red|E31D42
321|Red|C72B3B
304|Red Med|B71F35
816|Garnet|9B2B2B
814|Garnet Dk|701414
777|Raspberry Vy Dk|7B0E28
347|Salmon Vy Dk|BF2E34
350|Salmon Med|E0484E
351|Salmon|F5736F
352|Salmon Lt|FF9C8C
353|Peach|FFC9BE
754|Peach Lt|FFE0CE
948|Peach Vy Lt|FEEEE5
758|Terra Cotta Vy Lt|EEB39B
3778|Terra Cotta Lt|DB8B7D
356|Terra Cotta Med|C46C5E
355|Terra Cotta Dk|A65548
3777|Terra Cotta Vy Dk|8C3B31
3721|Shell Pink Dk|A64C4C
3722|Shell Pink Med|BC7070
224|Shell Pink Vy Lt|EFC7C2
223|Shell Pink Lt|CC8C8C
315|Antique Mauve Med|A96B6B
601|Cranberry Dk|D1286E
602|Cranberry Med|E24874
603|Cranberry|FF6D8E
604|Cranberry Lt|FF9CB5
605|Cranberry Vy Lt|FFB8C8
3607|Plum Lt|C54B8C
718|Plum|9E2A6E
917|Plum Med Dk|9B1B5A
3685|Mauve Vy Dk|7B1B42
3687|Mauve|C34A6B
3688|Mauve Med|E07B9B
3803|Mauve Dk|9B2B55
553|Violet|A3639B
552|Violet Med|803A6B
550|Violet Vy Dk|4B1B3B
3746|Blue Violet Dk|6B4A9B
340|Blue Violet Med|ADA7D6
341|Blue Violet Lt|C7C1E8
333|Blue Violet Dk|5B4A9B
208|Lavender Vy Dk|83538C
209|Lavender Dk|A87BB0
210|Lavender Med|C7A3CC
211|Lavender Lt|E3CBE6
3750|Antique Blue Vy Dk|26394D
3753|Antique Blue Ultra Lt|DBE5EE
932|Antique Blue Lt|A8BBD0
931|Antique Blue Med|7B94B0
930|Antique Blue Dk|4B6580
3325|Baby Blue Lt|B8D0E8
775|Baby Blue Vy Lt|D6E8F5
800|Delft Blue Pale|C7D8F0
809|Delft Blue|7B9BD0
799|Delft Blue Med|5B7BC0
798|Delft Blue Dk|4A5FA8
797|Royal Blue|2B4A9B
796|Royal Blue Dk|1B3B7B
820|Royal Blue Vy Dk|1B2B6B
826|Blue Med|4B6BA8
825|Blue Dk|3B5B8C
827|Blue Vy Lt|A8CBE8
828|Blue Ultra Vy Lt|CBE8F5
519|Sky Blue|A8D8E8
518|Wedgewood Lt|4B9BC0
517|Wedgewood Med|3B8BB0
3765|Peacock Blue Vy Dk|1B6B8C
995|Electric Blue Dk|1B7BC0
996|Electric Blue Med|3B9BE0
3843|Electric Blue|1B8BC8
3846|Turquoise Bright Lt|3BCBE8
3845|Turquoise Bright Med|1BBBD8
3844|Turquoise Bright Dk|1B9BB8
943|Aquamarine Med|2BBBA8
959|Sea Green Med|5BCBB8
958|Sea Green Dk|3BBBA8
3812|Sea Green Vy Dk|1B9B8B
500|Blue Green Vy Dk|1B3B2B
501|Blue Green Dk|2B4B3B
502|Blue Green|4B7B6B
503|Blue Green Med|7BA89B
3814|Celadon Green Dk|2B7B6B
3816|Celadon Green|6BA88B
3817|Celadon Green Lt|9BCBA8
3345|Hunter Green Dk|1B5B3B
3346|Hunter Green|3B8B5B
3347|Yellow Green Med|6BAB6B
3348|Yellow Green Lt|C7DB8B
986|Forest Green Vy Dk|2B4B2B
987|Forest Green Dk|4B7B3B
988|Forest Green Med|7BA85B
989|Forest Green|8BB87B
703|Chartreuse|7BB83B
704|Chartreuse Bright|9BCB4B
702|Kelly Green|3B9B3B
700|Bright Green|2B8B3B
701|Green Lt|4BAB3B
907|Parrot Green Lt|C7E87B
906|Parrot Green Med|8BCB3B
905|Parrot Green Dk|5B9B1B
904|Parrot Green Vy Dk|3B7B1B
471|Avocado Green Vy Lt|A8CB7B
470|Avocado Green Lt|7BAB3B
469|Avocado Green|5B8B2B
936|Avocado Green Vy Dk|3B5B1B
935|Avocado Green Dk|3B6B2B
580|Moss Green Dk|3B6B2B
581|Moss Green|6B8B2B
733|Olive Green Med|9B9B3B
732|Olive Green|7B7B2B
731|Olive Green Dk|4B4B1B
730|Olive Green Vy Dk|3B3B1B
734|Olive Green Lt|A8A85B
3051|Dark Olive Green|4B4B2B
3053|Olive Green|9BA87B
3781|Mocha Brown Dk|5B4B3B
3860|Cocoa|8B6B5B
3861|Cocoa Lt|B79B8B
3864|Mocha Beige Lt|CBBFA8
3866|Mocha Brown Ultra Vy Lt|F0E4D8
644|Beige Gray Lt|DDD8CB
640|Beige Gray Vy Dk|A8A088
3787|Brown Gray Dk|7B6B5B
3022|Brown Gray Med|9B8B7B
3024|Brown Gray Vy Lt|E0D8CC
453|Shell Gray Lt|D8CBC0
451|Shell Gray Dk|9B8B85
839|Beige Brown Dk|6B5B4B
841|Beige Brown Lt|C7B39B
842|Beige Brown Vy Lt|E0D3C0
3799|Pewter Gray Vy Dk|3B3B3B
413|Pewter Gray Dk|565656
414|Pewter Gray Dk|8C8C8C
415|Pearl Gray|9E9E9E
318|Steel Gray Lt|ABABAB
317|Pewter Gray|6B6B6B
762|Pearl Gray Vy Lt|ECECEC
645|Beaver Gray Vy Dk|5B5B5B
646|Beaver Gray Dk|7B7B7B
647|Beaver Gray Med|9B9B9B
648|Beaver Gray Lt|B7B7B7
3371|Black Brown|1B0F0B
434|Brown Lt|B8865B
433|Brown Med|8B5B3B
898|Coffee Brown Vy Dk|4B2B1B
938|Coffee Brown Ultra Dk|3B2318
437|Tan Lt|E0B88B
739|Tan Ultra Vy Lt|F0E3CC
3776|Mahogany Lt|C46B4B
301|Mahogany Med|B35B3B
400|Mahogany Dk|8B4B2B
300|Mahogany Vy Dk|6B3B1B
3826|Golden Brown|B88B4B
3828|Hazelnut Brown|B88B5B
680|Old Gold Dk|B88B2B
729|Old Gold Med|D8B85B
677|Old Gold Vy Lt|F0E0B8
422|Hazelnut Brown Lt|C7A88B
420|Hazelnut Brown Dk|A8805B
832|Golden Olive|A89B4B
834|Golden Olive Vy Lt|DBCB8B
3809|Turquoise Vy Dk|1B6B7B
3810|Turquoise Dk|1B8B9B
3811|Turquoise Vy Lt|B7E3E8
927|Sea Green Lt|B7CFC7
926|Sea Green Med Lt|8BAFA8
3849|Turquoise Lt|4BCBB8
3850|Turquoise Dk|2B9B8B
909|Emerald Green Vy Dk|1B6B3B
910|Emerald Green Dk|1B7B4B
911|Emerald Green Med|2B8B5B
912|Emerald Green Lt|4BAB7B
913|Nile Green Med Lt|6BCB9B
955|Nile Green Lt|B7EBCB
3820|Straw Dk|DBA83B
3822|Straw Lt|F0DBA8
3823|Ultra Pale Yellow|FFF8D6
743|Yellow Med|FFE04B
742|Tangerine Lt|FFC04B
741|Tangerine Med|FF9B2B
740|Tangerine|FF7B1B
721|Orange Spice Med|F0682B
720|Orange Spice Dk|D85B1B
977|Golden Brown Lt|DB9B4B
976|Golden Brown Med|C78B3B
817|Coral Red Vy Dk|BB2B2B
891|Coral Red Dk|E04B4B
892|Coral Red Med|E86B6B
893|Coral Red Lt|F08B8B
818|Baby Pink|FFD8D8
819|Baby Pink Lt|FFEBEB
3716|Baby Pink Med Vy Lt|FFC7C7
962|Baby Pink Med|F58B8B
961|Dusty Rose Dk|D85B6B
3774|Desert Sand Vy Lt|F0DBC8
3773|Desert Sand Med|E0BBA0
3772|Desert Sand Vy Dk|B88B6B
3771|Terra Cotta Ultra Vy Dk|8B4B3B
"""

    override val all: List<DmcColor> by lazy {
        val list = mutableListOf<DmcColor>()
        val seen = mutableSetOf<String>()
        for (line in RAW.trim().split("\n")) {
            val p = line.trim().split("|")
            if (p.size < 3) continue
            val code = p[0].trim()
            val name = p[1].trim()
            val hex = p[2].trim().uppercase()
            if (!seen.add("$code|$hex")) continue
            val r = hex.substring(0,2).toInt(16)
            val g = hex.substring(2,4).toInt(16)
            val b = hex.substring(4,6).toInt(16)
            list.add(DmcColor(code, name, "#$hex", r, g, b, rgb2lab(r,g,b)))
        }
        list
    }

    override fun nearest(lab: FloatArray): DmcColor {
        var best = all[0]; var bd = Float.MAX_VALUE
        for (c in all) {
            val d = sq(c.lab[0]-lab[0]) + sq(c.lab[1]-lab[1]) + sq(c.lab[2]-lab[2])
            if (d < bd) { bd = d; best = c }
        }
        return best
    }

    override fun nearestIndex(lab: FloatArray, list: List<DmcColor>): Int {
        var bi = 0; var bd = Float.MAX_VALUE
        for (i in list.indices) {
            val c = list[i]
            val d = sq(c.lab[0]-lab[0]) + sq(c.lab[1]-lab[1]) + sq(c.lab[2]-lab[2])
            if (d < bd) { bd = d; bi = i }
        }
        return bi
    }

    private fun sq(x: Float) = x*x
}
