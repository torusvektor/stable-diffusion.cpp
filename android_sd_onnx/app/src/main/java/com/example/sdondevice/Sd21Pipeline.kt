package com.example.sdondevice

import ai.onnxruntime.*
import java.nio.FloatBuffer

class Sd21Pipeline(private val mgr: QnnSdSessionManager) {

    data class GenConfig(
        val prompt: String,
        val negativePrompt: String = "",
        val steps: Int = 20,
        val guidanceScale: Float = 7.5f,
        val height: Int = 768,
        val width: Int = 768,
        val seed: Long = 42L
    )

    fun generate(cfg: GenConfig): FloatArray {
        // 1) Tokenizace (placeholder): implementujte CLIP tokenizer kompatibilní se SD2.1
        val condTokens = tokenize(cfg.prompt)
        val uncondTokens = tokenize(cfg.negativePrompt.ifEmpty { "" })

        // 2) Text encoder (conditional + unconditional)
        val condEmbedding = runTextEncoder(condTokens)
        val uncondEmbedding = runTextEncoder(uncondTokens)

        // 3) Inicializace latent šumu [1,4,H/8,W/8]
        var latent = randomLatent(
            batch = 1,
            channels = 4,
            h = cfg.height / 8,
            w = cfg.width / 8,
            seed = cfg.seed
        )

        val scheduler = SimpleEulerScheduler(inferenceSteps = cfg.steps).apply { build() }

        // 4) Denoising loop
        for (i in 0 until cfg.steps) {
            val t = scheduler.timesteps[i]

            val epsUncond = runUnet(latent, t, uncondEmbedding)
            val epsCond = runUnet(latent, t, condEmbedding)

            // classifier-free guidance
            val epsGuided = FloatArray(epsCond.size) { idx ->
                epsUncond[idx] + cfg.guidanceScale * (epsCond[idx] - epsUncond[idx])
            }

            latent = scheduler.step(latent, epsGuided, i)
        }

        // 5) Decode latent -> image (RGB float [0..1])
        return runVaeDecoder(latent)
    }

    private fun runTextEncoder(inputIds: LongArray): FloatArray {
        val shape = longArrayOf(1, inputIds.size.toLong())
        val tensor = OnnxTensor.createTensor(
            mgr.textSession.environment,
            java.nio.LongBuffer.wrap(inputIds),
            shape
        )
        mgr.textSession.run(mapOf("input_ids" to tensor)).use { out ->
            @Suppress("UNCHECKED_CAST")
            val v = out[0].value as Array<Array<FloatArray>>
            return v[0].flatMap { it.asList() }.toFloatArray()
        }
    }

    private fun runUnet(latent: FloatArray, timestep: Int, textEmbedding: FloatArray): FloatArray {
        // Přizpůsobte přesným input jménům/shape vašeho model.onnx
        val latentTensor = OnnxTensor.createTensor(
            mgr.unetSession.environment,
            FloatBuffer.wrap(latent),
            longArrayOf(1, 4, 96, 96) // pro 768x768
        )
        val tTensor = OnnxTensor.createTensor(
            mgr.unetSession.environment,
            java.nio.LongBuffer.wrap(longArrayOf(timestep.toLong())),
            longArrayOf(1)
        )
        val embTensor = OnnxTensor.createTensor(
            mgr.unetSession.environment,
            FloatBuffer.wrap(textEmbedding),
            longArrayOf(1, 77, 1024)
        )

        mgr.unetSession.run(
            mapOf(
                "sample" to latentTensor,
                "timestep" to tTensor,
                "encoder_hidden_states" to embTensor
            )
        ).use { out ->
            @Suppress("UNCHECKED_CAST")
            val v = out[0].value as Array<Array<Array<FloatArray>>>
            return flatten4d(v)
        }
    }

    private fun runVaeDecoder(latent: FloatArray): FloatArray {
        val latentTensor = OnnxTensor.createTensor(
            mgr.vaeSession.environment,
            FloatBuffer.wrap(latent),
            longArrayOf(1, 4, 96, 96)
        )
        mgr.vaeSession.run(mapOf("latent_sample" to latentTensor)).use { out ->
            @Suppress("UNCHECKED_CAST")
            val v = out[0].value as Array<Array<Array<FloatArray>>>
            return flatten4d(v) // [1,3,768,768]
        }
    }

    // --- helpers (placeholders) ---

    private fun tokenize(text: String): LongArray {
        // TODO: CLIP tokenizer + pad/truncate na délku 77
        return LongArray(77) { 0L }
    }

    private fun randomLatent(batch: Int, channels: Int, h: Int, w: Int, seed: Long): FloatArray {
        val rnd = java.util.Random(seed)
        return FloatArray(batch * channels * h * w) { (rnd.nextGaussian() * 1.0).toFloat() }
    }

    private fun flatten4d(v: Array<Array<Array<FloatArray>>>): FloatArray {
        val out = ArrayList<Float>(v.size * v[0].size * v[0][0].size * v[0][0][0].size)
        for (a in v) for (b in a) for (c in b) for (d in c) out.add(d)
        return out.toFloatArray()
    }
}
