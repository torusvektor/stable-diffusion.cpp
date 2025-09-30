document.addEventListener('DOMContentLoaded', () => {
    const generateBtn = document.getElementById('generate-btn');
    const saveBtn = document.getElementById('save-btn');
    const imageContainer = document.getElementById('image-container');
    const resultGrid = document.querySelector('.result-grid');

    function addHistoryItem(imagePath, prompt) {
        const historyItem = document.createElement('div');
        historyItem.className = 'result-card glass-effect rounded-lg overflow-hidden cursor-pointer';

        const img = new Image();
        img.src = imagePath;
        img.alt = prompt;
        img.className = 'w-full h-48 object-cover';

        const info = document.createElement('div');
        info.className = 'p-3';
        const p = document.createElement('p');
        p.className = 'text-white text-sm truncate';
        p.textContent = prompt;
        info.appendChild(p);

        historyItem.appendChild(img);
        historyItem.appendChild(info);

        historyItem.addEventListener('click', () => {
            const mainImg = new Image();
            mainImg.src = imagePath;
            mainImg.alt = "Generated image";
            mainImg.className = "w-full h-full object-contain";
            imageContainer.innerHTML = '';
            imageContainer.appendChild(mainImg);
        });

        resultGrid.prepend(historyItem);
    }

    generateBtn.addEventListener('click', async () => {
        const model = document.getElementById('model').value;
        const loras = Array.from(document.querySelectorAll('#loras input:checked')).map(el => el.value);
        const loraWeight = document.getElementById('lora-weight').value;
        const positivePrompt = document.getElementById('positive-prompt').value;
        const negativePrompt = document.getElementById('negative-prompt').value;
        const steps = document.getElementById('steps').value;
        const cfgScale = document.getElementById('cfg-scale').value;
        const sampler = document.getElementById('sampler').value;
        const scheduler = document.getElementById('scheduler').value;
        const hiresFix = document.getElementById('hires-fix').checked;
        const hiresSteps = document.getElementById('hires-steps').value;
        const hiresCfg = document.getElementById('hires-cfg').value;
        const denoise = document.getElementById('denoise').value;

        const data = {
            model,
            loras,
            loraWeight,
            positivePrompt,
            negativePrompt,
            steps,
            cfgScale,
            sampler,
            scheduler,
            hiresFix,
            hiresSteps,
            hiresCfg,
            denoise,
        };

        imageContainer.innerHTML = `<div class="text-center text-white opacity-70">Generating...</div>`;

        try {
            const response = await fetch('/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            });

            const result = await response.json();

            if (result.status === 'success') {
                const img = new Image();
                img.src = result.image_path;
                img.alt = "Generated image";
                img.className = "w-full h-full object-contain";
                imageContainer.innerHTML = '';
                imageContainer.appendChild(img);
                addHistoryItem(result.image_path, positivePrompt);
            } else {
                imageContainer.innerHTML = `<div class="text-center text-white opacity-70">${result.message}</div>`;
            }
        } catch (error) {
            console.error('Error:', error);
            imageContainer.innerHTML = `<div class="text-center text-white opacity-70">An error occurred while generating the image.</div>`;
        }
    });

    saveBtn.addEventListener('click', () => {
        const img = imageContainer.querySelector('img');
        if (img) {
            const link = document.createElement('a');
            link.href = img.src;
            link.download = 'generated-image.png';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } else {
            alert('No image to save!');
        }
    });
});