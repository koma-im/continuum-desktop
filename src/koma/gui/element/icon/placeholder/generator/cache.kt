package koma.gui.element.icon.placeholder.generator

import javafx.scene.image.Image
import javafx.scene.paint.Color
import org.cache2k.Cache
import org.cache2k.Cache2kBuilder
import org.cache2k.configuration.Cache2kConfiguration

class ColoredName(
        val color: Color,
        val name: String
)

object AvatarGeneratorCache {
    private val cache: Cache<ColoredName, Image>

    fun generateAvatar(colorname: ColoredName): Image
            = cache.computeIfAbsent(colorname, { getImageForName(colorname.name, colorname.color) })

    init {
        cache = createCache()
    }

    private fun createCache(): Cache<ColoredName, Image> {
        val conf = Cache2kConfiguration<ColoredName, Image>()
        val cache = Cache2kBuilder.of(conf)
                .entryCapacity(300)
        return cache.build()
    }
}
