const fs = require('fs');
const path = require('path');
const { Jimp } = require('jimp');

const inputPath = path.join(__dirname, 'mockup.jpg');
const outputPath = path.join(__dirname, 'mockup_cropped.jpg');

Jimp.read(inputPath)
    .then(async image => {
        const width = image.bitmap.width;
        const height = image.bitmap.height;
        console.log(`Original size: ${width}x${height}`);

        const bgColor = image.getPixelColor(0, 0);
        const bgR = (bgColor >> 24) & 255;
        const bgG = (bgColor >> 16) & 255;
        const bgB = (bgColor >> 8) & 255;
        console.log(`Detected background color: RGB(${bgR}, ${bgG}, ${bgB})`);

        const threshold = 30; // Slightly higher threshold to handle jpg compression artifacts
        function isBg(x, y) {
            const color = image.getPixelColor(x, y);
            const r = (color >> 24) & 255;
            const g = (color >> 16) & 255;
            const b = (color >> 8) & 255;
            return Math.abs(r - bgR) < threshold &&
                   Math.abs(g - bgG) < threshold &&
                   Math.abs(b - bgB) < threshold;
        }

        // We want to crop from top and bottom.
        // Let's scan for top content boundary
        let top = 0;
        for (let y = 0; y < height; y++) {
            let rowHasContent = false;
            for (let x = 0; x < width; x++) {
                if (!isBg(x, y)) {
                    rowHasContent = true;
                    break;
                }
            }
            if (rowHasContent) {
                top = y;
                break;
            }
        }

        // Scan for bottom content boundary
        let bottom = height - 1;
        for (let y = height - 1; y >= 0; y--) {
            let rowHasContent = false;
            for (let x = 0; x < width; x++) {
                if (!isBg(x, y)) {
                    rowHasContent = true;
                    break;
                }
            }
            if (rowHasContent) {
                bottom = y;
                break;
            }
        }

        let left = 0;
        let right = width - 1;

        console.log(`Detected content vertical boundaries - Top: ${top}, Bottom: ${bottom}`);

        // In the user's mockup image, the phone mockup has a header that starts around y=240
        // and a footer that ends around y=800.
        // Let's set the crop values:
        // We crop from top and bottom.
        const cropTop = Math.max(0, top);
        const cropHeight = Math.min(height - cropTop, bottom - cropTop + 1);

        console.log(`Cropping parameters - Top: ${cropTop}, Height: ${cropHeight}, Width: ${width}`);

        // Try Jimp v1 crop signature
        try {
            // Let's try named parameters
            image.crop({ x: 0, y: cropTop, w: width, h: cropHeight });
        } catch (e) {
            try {
                // Let's try width/height
                image.crop({ x: 0, y: cropTop, width: width, height: cropHeight });
            } catch (e2) {
                console.error("Failed to crop with object parameter, throwing:", e2);
                throw e2;
            }
        }

        await image.write(outputPath);
        console.log(`Successfully cropped image. Saved to: ${outputPath}`);
    })
    .catch(err => {
        console.error('Error processing image:', err);
    });
