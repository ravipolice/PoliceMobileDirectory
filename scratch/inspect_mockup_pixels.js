const { Jimp } = require('jimp');
const path = require('path');

const inputPath = path.join(__dirname, 'mockup.jpg');

Jimp.read(inputPath)
    .then(image => {
        const width = image.bitmap.width;
        const height = image.bitmap.height;
        console.log(`Dimensions: ${width}x${height}`);
        
        // Print colors of the 4 corners
        const corners = [
            { name: 'Top-Left', x: 0, y: 0 },
            { name: 'Top-Right', x: width - 1, y: 0 },
            { name: 'Bottom-Left', x: 0, y: height - 1 },
            { name: 'Bottom-Right', x: width - 1, y: height - 1 },
            { name: 'Center', x: Math.floor(width / 2), y: Math.floor(height / 2) }
        ];
        
        corners.forEach(c => {
            const color = image.getPixelColor(c.x, c.y);
            const r = (color >> 24) & 255;
            const g = (color >> 16) & 255;
            const b = (color >> 8) & 255;
            const a = color & 255;
            console.log(`${c.name} (${c.x}, ${c.y}): RGB(${r}, ${g}, ${b}), Alpha: ${a}`);
        });
        
        // Let's print a grid of 5x5 pixels color to see where the white starts
        console.log('\n5x5 Grid Color Sample:');
        for (let j = 0; j < 5; j++) {
            const y = Math.floor(height * j / 4);
            let row = '';
            for (let i = 0; i < 5; i++) {
                const x = Math.floor(width * i / 4);
                const color = image.getPixelColor(x, y);
                const r = (color >> 24) & 255;
                const g = (color >> 16) & 255;
                const b = (color >> 8) & 255;
                row += `| (${x},${y}): RGB(${r},${g},${b}) `;
            }
            console.log(row + '|');
        }
    })
    .catch(err => {
        console.error(err);
    });
