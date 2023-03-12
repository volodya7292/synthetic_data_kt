@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package synthdata

/**
 * Creates/reverses mapping of list of strings to integers.
 * Each distinct string is associated with a unique index.
 */
class StringTransformer(uniques: Array<String>) {
    /** Unique strings */
    val uniques: Array<String>
    private var strToIdx = mapOf<String, Int>()

    init {
        this.uniques = uniques.map { it.trim() }.distinct().toTypedArray()
        strToIdx = this.uniques.withIndex().associate { Pair(it.value, it.index) }
    }

    /** Transforms strings to corresponding integers */
    fun transform(data: Array<String>): IntArray {
        return data.map { strToIdx[it.trim()]!! }.toIntArray()
    }

    /** Transforms integers to corresponding strings */
    fun reverseTransform(data: IntArray): Array<String> {
        return data.map { uniques[it] }.toTypedArray()
    }
}
