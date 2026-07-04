const fs = require('fs');
const path = require('path');

const brainDir = "C:\\Users\\ravip\\.gemini\\antigravity\\brain\\cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
const artifactsDir = path.join(brainDir, "artifacts");

if (!fs.existsSync(artifactsDir)) {
    fs.mkdirSync(artifactsDir, { recursive: true });
    console.log(`Created artifacts directory: ${artifactsDir}`);
}

const loginSrc = path.join(brainDir, "login_screen_mockup_1779339749075.png");
const loginDest = path.join(artifactsDir, "login_screen.png");

const regSrc = path.join(brainDir, "registration_form_mockup_1779339765644.png");
const regDest = path.join(artifactsDir, "registration_form.png");

try {
    if (fs.existsSync(loginSrc)) {
        fs.copyFileSync(loginSrc, loginDest);
        console.log(`Copied login image to: ${loginDest}`);
    } else {
        console.log(`Login source image not found at: ${loginSrc}`);
    }

    if (fs.existsSync(regSrc)) {
        fs.copyFileSync(regSrc, regDest);
        console.log(`Copied registration image to: ${regDest}`);
    } else {
        console.log(`Registration source image not found at: ${regSrc}`);
    }
} catch (e) {
    console.log(`Error copying files: ${e.message}`);
}
