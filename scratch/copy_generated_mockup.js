const fs = require('fs');
const path = require('path');

const brainDir = "C:/Users/ravip/.gemini/antigravity/brain/cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
const srcImage = path.join(brainDir, "registration_form_mockup_v2_1779342663064.png");
const destImage = path.join(brainDir, "artifacts", "registration_form.png");

try {
    if (fs.existsSync(srcImage)) {
        fs.copyFileSync(srcImage, destImage);
        console.log(`Successfully replaced registration_form.png with the new mockup: ${destImage}`);
    } else {
        console.log(`Source image not found: ${srcImage}`);
    }
} catch (e) {
    console.log(`Error: ${e.message}`);
}
