@file:Suppress("unused")

package synthdata

import com.sun.jna.Memory
import java.io.Closeable

enum class ColumnType(val nativeId: Int) {
    Continuous(RAW_COLUMN_TYPE_CONTINUOUS),
    Discrete(RAW_COLUMN_TYPE_DISCRETE);

    val elementSize: Int
        get() = when (this) {
            Continuous -> Float.SIZE_BYTES
            Discrete -> Int.SIZE_BYTES
        }
}

sealed class ColumnData {
    class Continuous(var data: FloatArray) : ColumnData()
    class Discrete(var data: IntArray) : ColumnData()

    val rowCount
        get() = when (this) {
            is Continuous -> this.data.size
            is Discrete -> this.data.size
        }

    internal val type
        get() = when (this) {
            is Continuous -> ColumnType.Continuous
            is Discrete -> ColumnType.Discrete
        }
}

typealias DoStop = Boolean

class TVAE internal constructor(private val handle: SynthNetHandle, private val columnTypes: Array<ColumnType>) :
    Closeable {

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fit(data: Array<ColumnData>, batchSize: Int, flowControl: (epoch: Int, loss: Double) -> DoStop): TVAE {
            val nRows = data[0].rowCount
            assert(data.all { it.rowCount == nRows })

            val columnTypes = data.map { it.type }
            val rawData = CSynthLib.RawColumnData().toArray(columnTypes.size) as Array<CSynthLib.RawColumnData>

            for ((i, col) in data.withIndex()) {
                val mem = Memory(nRows.toLong() * col.type.elementSize)
                when (col) {
                    is ColumnData.Continuous -> mem.write(0, col.data, 0, col.data.size)
                    is ColumnData.Discrete -> mem.write(0, col.data, 0, col.data.size)
                }
                rawData[i].type = col.type.nativeId
                rawData[i].data = mem
            }

            val handle = CSynthLib.INSTANCE.synth_net_fit(
                rawData,
                data.size,
                nRows,
                CSynthLib.TrainParams(batchSize, object : CSynthLib.FlowControlCallback {
                    override fun invoke(epoch: Int, loss: Double): Boolean {
                        return flowControl(epoch, loss)
                    }
                })
            )

            for (rawCol in rawData) {
                (rawCol.data as Memory).close()
            }

            return TVAE(handle, columnTypes.toTypedArray())
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun sample(sampleCount: Int): Array<ColumnData> {
        val columnCount = columnTypes.size
        val totalRowSize = columnTypes.sumOf { it.elementSize }

        val mem = Memory(sampleCount * totalRowSize.toLong())
        CSynthLib.INSTANCE.synth_net_sample(handle, mem, sampleCount)

        val sampledColumns = mutableListOf<ColumnData>()
        var currColOffset = 0L

        for (i in 0 until columnCount) {
            val data = when (columnTypes[i]) {
                ColumnType.Continuous -> ColumnData.Continuous(mem.getFloatArray(currColOffset, sampleCount))
                ColumnType.Discrete -> ColumnData.Discrete(mem.getIntArray(currColOffset, sampleCount))
            }
            sampledColumns.add(data)
            currColOffset += columnTypes[i].elementSize * sampleCount
        }

        return sampledColumns.toTypedArray()
    }

    override fun close() {
        CSynthLib.INSTANCE.synth_net_destroy(handle)
    }
}

