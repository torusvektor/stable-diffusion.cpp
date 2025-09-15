#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <android/bitmap.h>
#include "stable-diffusion.h"

#define TAG "JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_stablediffusion_MainActivity_generateImageBitmap(
        JNIEnv *env,
        jobject /* this */,
        jstring prompt_java,
        jfloat cfg_scale,
        jint steps,
        jstring sampler_java,
        jstring scheduler_java,
        jfloat denoise,
        jboolean hires_fix,
        jstring lora_dir_java) {

    const char *prompt_chars = env->GetStringUTFChars(prompt_java, nullptr);
    const char *sampler_chars = env->GetStringUTFChars(sampler_java, nullptr);
    const char *scheduler_chars = env->GetStringUTFChars(scheduler_java, nullptr);
    const char *lora_dir_chars = env->GetStringUTFChars(lora_dir_java, nullptr);

    std::string prompt(prompt_chars);
    std::string sampler(sampler_chars);
    std::string scheduler(scheduler_chars);
    std::string lora_dir(lora_dir_chars);

    env->ReleaseStringUTFChars(prompt_java, prompt_chars);
    env->ReleaseStringUTFChars(sampler_java, sampler_chars);
    env->ReleaseStringUTFChars(scheduler_java, scheduler_chars);
    env->ReleaseStringUTFChars(lora_dir_java, lora_dir_chars);

    LOGD("Prompt: %s", prompt.c_str());
    LOGD("Steps: %d", steps);
    LOGD("CFG Scale: %f", cfg_scale);
    LOGD("Sampler: %s", sampler.c_str());
    LOGD("Scheduler: %s", scheduler.c_str());
    LOGD("Denoise: %f", denoise);
    LOGD("Hires Fix: %d", hires_fix);
    LOGD("LoRA Dir: %s", lora_dir.c_str());


    // TODO: Make model paths configurable
    const char* model_path = "/sdcard/stable-diffusion/perfectionAmateurILXL_30.safetensors";

    sd_ctx_params_t ctx_params;
    sd_ctx_params_init(&ctx_params);
    ctx_params.model_path = model_path;
    ctx_params.lora_model_dir = lora_dir.c_str();
    ctx_params.n_threads = 4;
    ctx_params.wtype = SD_TYPE_F16;

    // TODO: Implement Hires Fix
    if (hires_fix) {
        LOGD("Hires Fix is enabled but not yet implemented.");
    }

    sd_ctx_t* sd_ctx = new_sd_ctx(&ctx_params);
    if (sd_ctx == nullptr) {
        LOGD("Failed to create Stable Diffusion context");
        return nullptr;
    }

    sd_img_gen_params_t img_gen_params;
    sd_img_gen_params_init(&img_gen_params);
    img_gen_params.prompt = prompt.c_str();
    img_gen_params.negative_prompt = "";
    img_gen_params.clip_skip = -1;
    img_gen_params.width = 512;
    img_gen_params.height = 512;
    img_gen_params.sample_params.sample_method = str_to_sample_method(sampler.c_str());
    img_gen_params.sample_params.scheduler = str_to_schedule(scheduler.c_str());
    img_gen_params.sample_params.sample_steps = steps;
    img_gen_params.strength = denoise;
    img_gen_params.seed = -1;
    img_gen_params.batch_count = 1;
    img_gen_params.sample_params.guidance.txt_cfg = cfg_scale;

    sd_image_t* results = generate_image(sd_ctx, &img_gen_params);
    if (results == nullptr) {
        LOGD("Failed to generate image");
        free_sd_ctx(sd_ctx);
        return nullptr;
    }

    sd_image_t result_image = results[0];
    int width = result_image.width;
    int height = result_image.height;
    uint8_t* image_data = result_image.data;

    // Create a Bitmap object
    jclass bitmap_class = env->FindClass("android/graphics/Bitmap");
    jmethodID create_bitmap_method = env->GetStaticMethodID(bitmap_class, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring config_name = env->NewStringUTF("ARGB_8888");
    jclass bitmap_config_class = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID value_of_method = env->GetStaticMethodID(bitmap_config_class, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmap_config = env->CallStaticObjectMethod(bitmap_config_class, value_of_method, config_name);
    jobject bitmap = env->CallStaticObjectMethod(bitmap_class, create_bitmap_method, width, height, bitmap_config);

    // Lock the pixels and copy the data
    void* bitmap_pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmap_pixels) < 0) {
        LOGD("Failed to lock bitmap pixels");
        free(results->data);
        free(results);
        free_sd_ctx(sd_ctx);
        return nullptr;
    }

    uint8_t* src_pixels = image_data;
    uint32_t* dst_pixels = (uint32_t*)bitmap_pixels;
    for (int i = 0; i < width * height; ++i) {
        uint8_t r = src_pixels[i * 3];
        uint8_t g = src_pixels[i * 3 + 1];
        uint8_t b = src_pixels[i * 3 + 2];
        dst_pixels[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    free(results->data);
    free(results);
    free_sd_ctx(sd_ctx);

    return bitmap;
}
