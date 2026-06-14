package com.example.sdondevice

import ai.onnxruntime.*
import android.content.Context
import java.io.File
import java.nio.FloatBuffer
import java.util.EnumSet

class QnnSdSessionManager(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    lateinit var textSession: OrtSession
        private set
    lateinit var unetSession: OrtSession
        private set
    lateinit var vaeSession: OrtSession
        private set

    fun initSessions(modelRoot: File) {
        val so = OrtSession.SessionOptions()

        // Důležité: QNN EP konfigurace (klíče se mohou lišit dle ORT/QNN verze)
        // Ověřte proti vašemu ORT buildu.
        val providerOptions = hashMapOf(
            "backend_path" to "libQnnHtp.so",
            "htp_performance_mode" to "burst",
            "htp_graph_finalization_optimization_mode" to "3",
            "qnn_context_priority" to "normal",
            "profiling_level" to "off"
        )
        so.addQnn(providerOptions)

        so.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        so.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)

        // V některých ORT verzích pomůže menší počet vláken kvůli CPU overheadu
        so.setIntraOpNumThreads(2)
        so.setInterOpNumThreads(1)

        textSession = env.createSession(
            File(modelRoot, "text_encoder/model.onnx").absolutePath,
            so
        )
        unetSession = env.createSession(
            File(modelRoot, "unet/model.onnx").absolutePath,
            so
        )
        vaeSession = env.createSession(
            File(modelRoot, "vae_decoder/model.onnx").absolutePath,
            so
        )
    }

    fun close() {
        if (::textSession.isInitialized) textSession.close()
        if (::unetSession.isInitialized) unetSession.close()
        if (::vaeSession.isInitialized) vaeSession.close()
        env.close()
    }
}
