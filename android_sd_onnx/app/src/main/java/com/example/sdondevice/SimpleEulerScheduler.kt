package com.example.sdondevice

import kotlin.math.sqrt

class SimpleEulerScheduler(
    val numTrainSteps: Int = 1000,
    val inferenceSteps: Int = 20
) {
    lateinit var timesteps: IntArray
        private set
    lateinit var sigmas: FloatArray
        private set

    fun build() {
        timesteps = IntArray(inferenceSteps) { i ->
            val t = (numTrainSteps - 1) - (i * (numTrainSteps - 1) / (inferenceSteps - 1))
            t
        }

        // Placeholder sigma schedule; pro produkci použijte přesný scheduler ze SD2.1 pipeline
        sigmas = FloatArray(inferenceSteps + 1) { i ->
            if (i == inferenceSteps) 0f else sqrt((timesteps[i] + 1).toFloat() / numTrainSteps)
        }
    }

    fun step(latent: FloatArray, noisePred: FloatArray, stepIndex: Int): FloatArray {
        val sigma = sigmas[stepIndex]
        val sigmaNext = sigmas[stepIndex + 1]
        val dt = sigmaNext - sigma

        val out = FloatArray(latent.size)
        for (i in latent.indices) {
            out[i] = latent[i] + noisePred[i] * dt
        }
        return out
    }
}
