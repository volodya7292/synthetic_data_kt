@file:Suppress("unused")

package synthdata

/**
 * Creates/reverses mapping of list of strings to integers.
 * Each distinct string is associated with a unique index.
 */
class StringTransformer(set: Array<String>) {
    private var uniques = arrayOf<String>()
    private var strToIdx = mapOf<String, Int>()

    init {
        uniques = set.distinct().toTypedArray()
        strToIdx = uniques.withIndex().associate { Pair(it.value, it.index) }
    }

    /** Returns the number of unique items in this transformer */
    fun uniquesSize(): Int {
        return uniques.size
    }

    /** Transforms strings to corresponding integers */
    fun transform(data: Array<String>): IntArray {
        return data.map { strToIdx[it]!! }.toIntArray()
    }

    /** Transforms integers to corresponding strings */
    fun reverseTransform(data: IntArray): Array<String> {
        return data.map { uniques[it] }.toTypedArray()
    }
}
