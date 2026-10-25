package com.example.stitch

object DiamondPalette : PaletteProvider {
    // Палитра для алмазной мозаики (распространенные цвета страз AB/DM)
    // Использует расширенную цветовую гамму с более яркими и насыщенными цветами
    private val RAW = """
AB01|Белый кристалл|FFFFFF
AB02|Черный бриллиант|000000
AB03|Кристальный прозрачный|F8F8F8
AB04|Алмазный белый|FAFAFA
AB05|Слоновая кость|FFFFF0
AB06|Шампанское|F7E7CE
AB07|Кремовый|FFFDD0
AB08|Лимонный|FFF44F
AB09|Желтый канареечный|FFEF00
AB10|Золотисто-желтый|FFD700
AB11|Солнечно-желтый|FFD300
AB12|Оранжевый желтый|FFA500
AB13|Мандариновый|FF9966
AB14|Янтарный|FFBF00
AB15|Персиковый|FFCBA4
AB16|Коралловый|FF7F50
AB17|Лососевый|FA8072
AB18|Алый|FF2400
AB19|Рубиновый|E0115F
AB20|Красный|FF0000
AB21|Вишневый|DE3163
AB22|Малиновый|DC143C
AB23|Бордовый|800020
AB24|Розовый фламинго|FC8EAC
AB25|Розовый|FFC0CB
AB26|Фуксия|FF00FF
AB27|Маджента|FF0090
AB28|Горячий розовый|FF69B4
AB29|Розовый кварц|F7CAC9
AB30|Лавандовый|E6E6FA
AB31|Сиреневый|C8A2C8
AB32|Фиолетовый|8B008B
AB33|Аметистовый|9966CC
AB34|Пурпурный|800080
AB35|Индиго|4B0082
AB36|Сливовый|DDA0DD
AB37|Голубой кристалл|89CFF0
AB38|Небесно-голубой|87CEEB
AB39|Лазурный|007FFF
AB40|Королевский синий|4169E1
AB41|Сапфировый|0F52BA
AB42|Кобальтовый|0047AB
AB43|Морской волны|00CED1
AB44|Бирюзовый|40E0D0
AB45|Аквамарин|7FFFD4
AB46|Изумрудный|50C878
AB47|Зеленый|00FF00
AB48|Лаймовый|32CD32
AB49|Шартрез|7FFF00
AB50|Оливковый|808000
AB51|Хаки|C3B091
AB52|Зеленый лес|228B22
AB53|Мятный|98FF98
AB54|Нефритовый|00A86B
AB55|Болотный|4A5D23
AB56|Золотой|FFD700
AB57|Бронзовый|CD7F32
AB58|Медный|B87333
AB59|Латунный|B5A642
AB60|Коричневый шоколад|7B3F00
AB61|Махагон|C04000
AB62|Каштановый|954535
AB63|Сепия|704214
AB64|Бежевый|F5F5DC
AB65|Песочный|C2B280
AB66|Серый серебристый|C0C0C0
AB67|Серый|808080
AB68|Угольный|36454F
AB69|Жемчужный|FDEEF4
AB70|Радужный кристалл|F5EAC7
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
