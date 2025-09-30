# Perfection Amateur ILXL - Web UI

This project provides a web-based user interface for the `stable-diffusion.cpp` project, with a focus on using the "Perfection Amateur" model and its associated LoRAs.

## Features

-   Web-based UI for generating images with `stable-diffusion.cpp`.
-   Support for selecting models and LoRAs.
-   Adjustable generation parameters, including steps, CFG scale, sampler, and scheduler.
-   Hi-Res Fix simulation for upscaling/refining images.
-   Displays the generated image directly in the browser.

## Setup

### 1. Build stable-diffusion.cpp

Before you can run the web UI, you need to build the `stable-diffusion.cpp` project. Follow the instructions in the original `README.md` (which has been renamed to `README.sd-cpp.md`) to build the project. Make sure the compiled executable is located at `./bin/sd`.

### 2. Install Dependencies

The web UI requires Python and Flask. You can install Flask using pip:

```bash
pip install Flask
```

### 3. Download Models

The web UI is configured to use the "Perfection Amateur" model and the DMD2 LoRAs. You will need to download these files and place them in the `models` directory.

-   `perfectionAmateurILXL_30.safetensors`
-   `dmd2_sdxl_4step_lora.safetensors`
-   `dmd2_sdxl_4step_lora_fp16.safetensors`

Create a `models` directory in the root of the project:

```bash
mkdir models
```

Then, place the downloaded files in this directory.

## Running the Server

To start the web UI, run the following command from the root of the project:

```bash
python server.py
```

The server will start on `http://127.0.0.1:8080`.

## Usage

1.  Open your web browser and navigate to `http://127.0.0.1:8080`.
2.  Adjust the generation settings in the left panel.
3.  Enter a positive and negative prompt.
4.  Click the "Generate Image" button.
5.  The generated image will appear in the "Current Generation" panel on the right.

**Note:** The "Hi-Res Fix" is a simplified simulation. For a true hires fix, you would need to implement a two-step txt2img and img2img process. This implementation uses the hires fix settings for the main generation for simplicity.