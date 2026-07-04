const fs = require('fs');
const path = require('path');

const brainDir = "C:\\Users\\ravip\\.gemini\antigravity\\brain\\cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
const artifactsDir = path.join(brainDir, "artifacts");

const loginSrc = path.join(brainDir, "login_screen_mockup_v5_1779342098933.png");
const loginDest = path.join(artifactsDir, "login_screen.png");

try {
    if (fs.existsSync(loginSrc)) {
        fs.copyFileSync(loginSrc, loginDest);
        console.log(`Successfully updated login screen mockup to v5 in: ${loginDest}`);
    } else {
        console.log(`Source image not found at: ${loginSrc}`);
    }
} catch (e) {
    console.log(`Error: ${e.message}`);
}
