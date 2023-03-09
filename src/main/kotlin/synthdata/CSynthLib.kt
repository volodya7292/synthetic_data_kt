package synthdata

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native.load
import com.sun.jna.Pointer
import com.sun.jna.Structure

internal typealias SynthNetHandle = Long

internal const val RAW_COLUMN_TYPE_CONTINUOUS = 0
internal const val RAW_COLUMN_TYPE_DISCRETE = 1

@Suppress("FunctionName", "LocalVariableName", "PropertyName", "unused")
internal interface CSynthLib : Library {
    companion object {
        val INSTANCE: CSynthLib by lazy {
            load("synthetic_data", CSynthLib::class.java)
        }
    }

    @Structure.FieldOrder("type", "data")
    class RawColumnData(@JvmField var type: Int = 0, @JvmField var data: Pointer? = null) : Structure()

    @Structure.FieldOrder("batch_size", "flow_control_callback")
    class TrainParams(@JvmField var batch_size: Int, @JvmField var flow_control_callback: FlowControlCallback) :
        Structure()

    interface FlowControlCallback : Callback {
        fun invoke(epoch: Int, loss: Double): Boolean
    }

    fun synth_net_fit(
        columns: Array<RawColumnData>,
        n_columns: Int,
        n_rows: Int,
        train_params: TrainParams
    ): SynthNetHandle

    fun synth_net_sample(handle: SynthNetHandle, columns: Pointer, n_samples: Int)

    fun synth_net_destroy(handle: SynthNetHandle)
}
