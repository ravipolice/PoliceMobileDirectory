const fs = require('fs');
const path = require('path');

const brainDir = "C:/Users/ravip/.gemini/antigravity/brain/cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
const artifactsDir = path.join(brainDir, "artifacts");

const loginSrc = path.join(brainDir, "media__1779342048625.jpg");
const regSrc = path.join(brainDir, "media__1779342368587.jpg");

const loginDest = path.join(artifactsDir, "login_screen.png");
const regDest = path.join(artifactsDir, "registration_form.png");

try {
    if (fs.existsSync(loginSrc)) {
        fs.copyFileSync(loginSrc, loginDest);
        console.log(`Successfully replaced login screen with user screenshot: ${loginDest}`);
    } else {
        console.log(`Source login image not found at: ${loginSrc}`);
    }

    if (fs.existsSync(regSrc)) {
        fs.copyFileSync(regSrc, regDest);
        console.log(`Successfully replaced registration form with user screenshot: ${regDest}`);
    } else {
        console.log(`Source registration image not found at: ${regSrc}`);
    }
} catch (e) {
    console.log(`Error: ${e.message}`);
}
