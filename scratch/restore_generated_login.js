const fs = require('fs');
const path = require('path');

const brainDir = "C:/Users/ravip/.gemini/antigravity/brain/cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
const srcImage = path.join(brainDir, "login_screen_mockup_v6_1779342135103.png");
const destImage = path.join(brainDir, "artifacts", "login_screen.png");

try {
    if (fs.existsSync(srcImage)) {
        fs.copyFileSync(srcImage, destImage);
        console.log(`Successfully restored login screen mockup: ${destImage}`);
    } else {
        console.log(`Source image not found: ${srcImage}`);
    }
} catch (e) {
    console.log(`Error: ${e.message}`);
}
