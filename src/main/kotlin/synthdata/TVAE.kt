package synthdata

import com.sun.jna.Memory
import com.sun.jna.Pointer
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
        fun fit(data: Array<ColumnData>, batchSize: Int, flowControl: (epoch: Int, loss: Double) -> DoStop): TVAE {
            val nRows = data[0].rowCount
            assert(data.all { it.rowCount == nRows })

            val columnTypes = data.map { it.type }

            val rawData = data.map {
                val mem = Memory(nRows.toLong() * it.type.elementSize)
                when (it) {
                    is ColumnData.Continuous -> mem.write(0, it.data, 0, it.data.size)
                    is ColumnData.Discrete -> mem.write(0, it.data, 0, it.data.size)
                }
                CSynthLib.RawColumnData(it.type.nativeId, mem)
            }

            val handle = CSynthLib.INSTANCE.synth_net_fit(
                rawData.toTypedArray(),
                data.size,
                nRows,
                CSynthLib.TrainParams(batchSize, object : CSynthLib.FlowControlCallback {
                    override fun invoke(epoch: Int, loss: Double): Boolean {
                        return flowControl(epoch, loss)
                    }
                })
            )

            for (col in rawData) {
                (col.data as Memory).close()
            }

            return TVAE(handle, columnTypes.toTypedArray())
        }
    }

    fun sample(sampleCount: Int): Array<ColumnData> {
        val columnCount = columnTypes.size
        val totalRowSize = columnTypes.sumOf { it.elementSize }


        val mem = Memory(sampleCount * totalRowSize.toLong())

        val rawDataColumns = mutableListOf<Pointer>()
        var currRowOffset = 0

        for (i in 0 until columnCount) {
            rawDataColumns.add(mem.share(currRowOffset * sampleCount.toLong()))
            currRowOffset += columnTypes[i].elementSize
        }

        CSynthLib.INSTANCE.synth_net_sample(handle, rawDataColumns.toTypedArray(), sampleCount)

        val sampledColumns = rawDataColumns.zip(columnTypes).map { (ptr, type) ->
            when (type) {
                ColumnType.Continuous -> ColumnData.Continuous(ptr.getFloatArray(0, sampleCount))
                ColumnType.Discrete -> ColumnData.Discrete(ptr.getIntArray(0, sampleCount))
            }
        }
        return sampledColumns.toTypedArray()
    }

    override fun close() {
        CSynthLib.INSTANCE.synth_net_destroy(handle)
    }
}

