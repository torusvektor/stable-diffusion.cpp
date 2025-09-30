from flask import Flask, render_template, request, send_from_directory, jsonify
import subprocess
import os
import uuid

app = Flask(__name__, template_folder='web', static_folder='web/static')

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/generated/<path:filename>')
def generated(filename):
    return send_from_directory(os.path.join(app.root_path, 'web/static/generated'), filename)

@app.route('/generate', methods=['POST'])
def generate():
    data = request.get_json()

    executable = "./bin/sd"
    model_path = os.path.join("models", data['model'])

    prompt = data['positivePrompt']
    for lora in data['loras']:
        prompt += f" <lora:{os.path.splitext(lora)[0]}:{data['loraWeight']}>"

    # --- First Pass: txt2img ---
    base_output_filename = f"base_{uuid.uuid4()}.png"
    base_output_path = os.path.join(app.root_path, "web/static/generated", base_output_filename)

    cmd_txt2img = [
        executable,
        "-m", model_path,
        "-p", prompt,
        "-n", data['negativePrompt'],
        "--steps", str(data['steps']),
        "--cfg-scale", str(data['cfgScale']),
        "--sampling-method", data['sampler'],
        "--scheduler", data['scheduler'],
        "-o", base_output_path,
        "--lora-model-dir", "models"
    ]

    try:
        subprocess.run(cmd_txt2img, check=True)

        if not data['hiresFix']:
            image_path = f"/generated/{base_output_filename}"
            return jsonify({"status": "success", "image_path": image_path})

        # --- Second Pass: img2img (Hi-Res Fix) ---
        hires_output_filename = f"hires_{uuid.uuid4()}.png"
        hires_output_path = os.path.join(app.root_path, "web/static/generated", hires_output_filename)

        cmd_img2img = [
            executable,
            "-m", model_path,
            "-p", prompt,
            "-n", data['negativePrompt'],
            "--steps", str(data['hiresSteps']),
            "--cfg-scale", str(data['hiresCfg']),
            "--sampling-method", data['sampler'],
            "--scheduler", data['scheduler'],
            "-o", hires_output_path,
            "--lora-model-dir", "models",
            "-i", base_output_path,
            "--strength", str(data.get('denoise', 0.35)) # Use .get for safety
        ]

        subprocess.run(cmd_img2img, check=True)

        # Clean up the intermediate base image
        os.remove(base_output_path)

        image_path = f"/generated/{hires_output_filename}"
        return jsonify({"status": "success", "image_path": image_path})

    except subprocess.CalledProcessError as e:
        print(f"Error executing stable-diffusion.cpp: {e}")
        # Clean up intermediate file if it exists and we failed
        if os.path.exists(base_output_path):
            os.remove(base_output_path)
        return jsonify({"status": "error", "message": "Failed to generate image."})
    except FileNotFoundError:
        print("Error: stable-diffusion.cpp executable not found.")
        return jsonify({"status": "error", "message": "Executable not found. Have you built the project?"})


if __name__ == '__main__':
    app.run(debug=True, port=8080)